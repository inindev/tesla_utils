#
# Copyright (c) 2025, John Clark <inindev@gmail.com>
#
# Licensed under the MIT License. See LICENSE file in the project root for full license information.
#

import base64
import json
import logging
import math
import time
import uuid
from typing import Tuple, Optional, Union
from urllib.parse import urlencode, parse_qs, urlparse
import http.client

logger = logging.getLogger("OAuth2Client")

class OAuth2Client:
    AUTH_EP = "https://auth.tesla.com/oauth2/v3/authorize"
    TOKEN_EP = "https://auth.tesla.com/oauth2/v3/token"
    AUDIENCE = "https://fleet-api.prd.na.vn.cloud.tesla.com"
    CALLBACK_URI = "http://localhost:8888/auth/callback"
    SCOPE = "openid user_data vehicle_device_data vehicle_cmds vehicle_charging_cmds energy_device_data energy_cmds offline_access"
    LOCALE = "en-US"

    def __init__(self, secure_storage):
        self.secure_storage = secure_storage

    def initiate_auth_flow(self) -> str:
        """Generates the authentication URL for the OAuth2 flow."""
        client_id = self.secure_storage.retrieve_client_id()
        if not client_id:
            logger.debug("Client ID is missing")
            raise Exception("Client ID is missing")

        state = self._generate_state()
        self.secure_storage.store_state(state)

        params = {
            "client_id": client_id,
            "locale": self.LOCALE,
            "prompt": "login",
            "redirect_uri": self.CALLBACK_URI,
            "response_type": "code",
            "scope": self.SCOPE,
            "state": state
        }
        auth_url = f"{self.AUTH_EP}?{urlencode(params)}"
        logger.debug(f"Generated Auth URI: {auth_url}")
        return auth_url

    def exchange_code_for_tokens(self, uri: str) -> "AuthResult":
        """Exchanges the authorization code for access and refresh tokens."""
        try:
            logger.debug(f"Callback URI: {uri}")
            parsed_uri = urlparse(uri)
            params = parse_qs(parsed_uri.query)

            code = params.get("code", [None])[0]
            if not code:
                logger.error("Authorization code missing from callback URI")
                return AuthResult.Failure("Authorization code missing from callback URI")

            state = params.get("state", [None])[0]
            if not state:
                logger.error("Authorization state missing from callback URI")
                return AuthResult.Failure("Authorization state missing from callback URI")

            stored_state = self.secure_storage.retrieve_state()
            if state != stored_state:
                logger.error("State mismatch in OAuth callback")
                return AuthResult.Failure("State mismatch in OAuth callback")
            self.secure_storage.store_state(None)

            client_id = self.secure_storage.retrieve_client_id()
            if not client_id:
                return AuthResult.Failure("Client ID is missing or null")

            client_secret = self.secure_storage.retrieve_client_secret()
            if not client_secret:
                return AuthResult.Failure("Client Secret is missing or null")

            self.secure_storage.store_access_token(None)
            self.secure_storage.store_refresh_token(None)

            data = {
                "grant_type": "authorization_code",
                "client_id": client_id,
                "client_secret": client_secret,
                "code": code,
                "audience": self.AUDIENCE,
                "redirect_uri": self.CALLBACK_URI
            }
            return self._fetch_and_store_token_data(data)

        except Exception as e:
            logger.error(f"Error during token exchange: {e}", exc_info=True)
            return AuthResult.Failure(f"Token exchange failed: {str(e) or 'Unknown error'}")

    def get_access_token(self) -> Optional[str]:
        """Retrieves or refreshes an authentication token."""
        try:
            access_token = self.secure_storage.retrieve_access_token()
            if not access_token:
                logger.debug("Access token is missing")
                return None

            refresh_token = self.secure_storage.retrieve_refresh_token()
            if not refresh_token:
                logger.debug("Refresh token is missing")
                return None

            exp_info = self.get_jwt_exp_info(access_token)
            if exp_info is None or exp_info[1] < 20:
                logger.debug("Token life below 20%, refreshing")
                result = self.refresh_access_token()
                if isinstance(result, AuthResult.Success):
                    new_access_token = self.secure_storage.retrieve_access_token()
                    new_refresh_token = self.secure_storage.retrieve_refresh_token()
                    if not new_access_token or not new_refresh_token:
                        logger.debug("New access or refresh token is missing")
                        return None
                    return new_access_token
                else:
                    logger.error(f"Token refresh failed: {result.error_message}")
                    return None

            return access_token
        except Exception as e:
            logger.error(f"Error retrieving access token: {e}", exc_info=True)
            return None

    def refresh_access_token(self) -> "AuthResult":
        """Refreshes the access token using the stored refresh token."""
        try:
            client_id = self.secure_storage.retrieve_client_id()
            if not client_id:
                logger.debug("Client ID is missing")
                return AuthResult.Failure("Client ID is missing")

            refresh_token = self.secure_storage.retrieve_refresh_token()
            if not refresh_token:
                logger.debug("Refresh token is missing")
                return AuthResult.Failure("Refresh token is missing")

            data = {
                "grant_type": "refresh_token",
                "client_id": client_id,
                "refresh_token": refresh_token
            }
            result = self._fetch_and_store_token_data(data)
            if isinstance(result, AuthResult.Success):
                logger.debug("Access token refreshed successfully")
            return result
        except Exception as e:
            logger.error(f"Error while refreshing token: {e}", exc_info=True)
            return AuthResult.Failure(f"Token refresh failed: {str(e) or 'Unknown error'}")

    def _fetch_and_store_token_data(self, data: dict) -> "AuthResult":
        """Fetches and stores token data from the OAuth2 token endpoint."""
        try:
            # parse the token endpoint url
            parsed_url = urlparse(self.TOKEN_EP)
            conn = http.client.HTTPSConnection(parsed_url.netloc)

            # prepare headers and body
            headers = {"Content-Type": "application/x-www-form-urlencoded"}
            body = urlencode(data).encode("utf-8")

            # make the request
            conn.request("POST", parsed_url.path, body=body, headers=headers)
            response = conn.getresponse()

            # check status
            if response.status != 200:
                raise Exception(f"HTTP {response.status}: {response.reason}")

            response_body = response.read().decode("utf-8")
            conn.close()

            if not response_body:
                return AuthResult.Failure("Empty response body")

            logger.debug(f"Response body: {response_body}")
            json_data = json.loads(response_body)

            access_token = json_data.get("access_token", "")
            if not access_token:
                return AuthResult.Failure("Access token is missing from response")

            refresh_token = json_data.get("refresh_token", "")
            if not refresh_token:
                return AuthResult.Failure("Refresh token is missing from response")

            token_type = json_data.get("token_type", "")
            if not token_type:
                return AuthResult.Failure("Token type is missing from response")

            expires_in = json_data.get("expires_in", 0)
            if expires_in < 1:
                return AuthResult.Failure("Access token is expired")

            self.secure_storage.store_access_token(access_token)
            self.secure_storage.store_refresh_token(refresh_token)
            logger.debug("Access and refresh tokens stored successfully")
            return AuthResult.Success()

        except Exception as e:
            logger.error(f"Error fetching and storing token data: {e}", exc_info=True)
            return AuthResult.Failure(f"Token data fetch failed: {str(e) or 'Unknown error'}")

    def get_jwt_exp_info(self, jwt_token: Optional[str] = None) -> Optional[Tuple[int, int]]:
        """Decodes a JWT token to get expiration information."""
        token = jwt_token or self.secure_storage.retrieve_access_token()
        if not token:
            logger.debug("JWT is null or empty")
            return None

        parts = token.split(".")
        if len(parts) != 3:
            logger.debug(f"Invalid JWT format: expected 3 parts, but got {len(parts)}")
            return None

        try:
            payload = base64.urlsafe_b64decode(parts[1] + "==").decode("utf-8")
        except Exception as e:
            logger.debug(f"Failed to decode JWT payload: {e}", exc_info=True)
            return None

        try:
            json_data = json.loads(payload)
        except json.JSONDecodeError as e:
            logger.debug(f"Failed to json parse JWT payload: {e}", exc_info=True)
            return None

        now = int(time.time())
        exp = json_data.get("exp")
        iat = json_data.get("iat")

        if not exp or not iat or exp <= iat:
            logger.debug("Invalid expiration or issued-at time in JWT")
            life_remain = 0
        else:
            life_remain = max(math.floor((exp - now) / (exp - iat) * 100), 0)
        logger.debug(f"Token life remaining: {life_remain}%")
        return (exp, life_remain)

    def _generate_state(self) -> str:
        """Generates a secure state for OAuth2 flow to prevent CSRF attacks."""
        uuid_obj = uuid.uuid4()
        uuid_bytes = uuid_obj.bytes
        return base64.urlsafe_b64encode(uuid_bytes).decode("utf-8").rstrip("=")

class AuthResult:
    class Success:
        pass

    class Failure:
        def __init__(self, error_message: str):
            self.error_message = error_message
