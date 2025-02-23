#
# Copyright (c) 2025, John Clark <inindev@gmail.com>
#
# Licensed under the MIT License. See LICENSE file in the project root for full license information.
#

import os
import json
import logging
from pathlib import Path

logger = logging.getLogger("SecureStorage")

class SecureStorage:
    """Secure storage for Tesla OAuth2 credentials using a JSON file with filesystem security."""
    def __init__(self):
        # Define file paths
        self.home_dir = Path.home()
        self.tesla_dir = self.home_dir / ".tesla"
        self.auth_file = self.tesla_dir / "auth_data.json"

        # Initialize storage
        self._initialize_storage()

    def _initialize_storage(self):
        """Initialize or adjust the .tesla directory and auth_data.json with proper permissions."""
        # ensure .tesla directory exists with 700 permissions
        if not self.tesla_dir.exists():
            self.tesla_dir.mkdir(mode=0o700)
            logger.debug(f"Created directory {self.tesla_dir} with permissions 700")
        elif oct(self.tesla_dir.stat().st_mode & 0o777)[-3:] != "700":
            os.chmod(self.tesla_dir, 0o700)
            logger.debug(f"Adjusted permissions of {self.tesla_dir} to 700")
        else:
            logger.debug(f"Directory {self.tesla_dir} already has correct permissions 700")

        # ensure auth_data.json exists with 600 permissions
        if not self.auth_file.exists():
            with self.auth_file.open("w") as f:
                json.dump({}, f, indent=4, sort_keys=True)  # Initialize with empty dict, pretty format
            os.chmod(self.auth_file, 0o600)
            logger.debug(f"Created file {self.auth_file} with permissions 600")
        elif oct(self.auth_file.stat().st_mode & 0o777)[-3:] != "600":
            os.chmod(self.auth_file, 0o600)
            logger.debug(f"Adjusted permissions of {self.auth_file} to 600")
        else:
            logger.debug(f"File {self.auth_file} already has correct permissions 600")

    def _read_data(self) -> dict:
        """Read the data from the JSON file."""
        try:
            with self.auth_file.open("r") as f:
                return json.load(f)
        except (json.JSONDecodeError, FileNotFoundError) as e:
            logger.error(f"Failed to read {self.auth_file}: {e}", exc_info=True)
            return {}

    def _write_data(self, data: dict):
        """Write data to the JSON file with pretty formatting."""
        try:
            with self.auth_file.open("w") as f:
                json.dump(data, f, indent=4, sort_keys=True)  # Pretty print with indent=4, sorted keys
        except Exception as e:
            logger.error(f"Failed to write to {self.auth_file}: {e}", exc_info=True)
            raise

    def store_access_token(self, access_token: str):
        """Store the access token, removing it if blank."""
        data = self._read_data()
        if not access_token:
            data.pop("access_token", None)
        else:
            data["access_token"] = access_token
        self._write_data(data)

    def retrieve_access_token(self) -> str:
        """Retrieve the access token, returning an empty string if not found."""
        return self._read_data().get("access_token", "")

    def store_refresh_token(self, refresh_token: str):
        """Store the refresh token, removing it if blank."""
        data = self._read_data()
        if not refresh_token:
            data.pop("refresh_token", None)
        else:
            data["refresh_token"] = refresh_token
        self._write_data(data)

    def retrieve_refresh_token(self) -> str:
        """Retrieve the refresh token, returning an empty string if not found."""
        return self._read_data().get("refresh_token", "")

    def store_client_id(self, client_id: str):
        """Store the client ID, removing it if blank."""
        data = self._read_data()
        if not client_id:
            data.pop("client_id", None)
        else:
            data["client_id"] = client_id
        self._write_data(data)

    def retrieve_client_id(self) -> str:
        """Retrieve the client ID, returning an empty string if not found."""
        return self._read_data().get("client_id", "")

    def store_client_secret(self, client_secret: str):
        """Store the client secret, removing it if blank."""
        data = self._read_data()
        if not client_secret:
            data.pop("client_secret", None)
        else:
            data["client_secret"] = client_secret
        self._write_data(data)

    def retrieve_client_secret(self) -> str:
        """Retrieve the client secret, returning an empty string if not found."""
        return self._read_data().get("client_secret", "")

    def clear_client_credentials(self):
        """Clear client credentials from storage."""
        data = self._read_data()
        data.pop("client_id", None)
        data.pop("client_secret", None)
        self._write_data(data)

    def store_state(self, state: str):
        """Store the OAuth state, removing it if blank."""
        data = self._read_data()
        if not state:
            data.pop("oauth_state", None)
        else:
            data["oauth_state"] = state
        self._write_data(data)

    def retrieve_state(self) -> str:
        """Retrieve the OAuth state, returning an empty string if not found."""
        return self._read_data().get("oauth_state", "")

    def store_vin(self, vin: str):
        """Store the VIN, removing it if blank."""
        data = self._read_data()
        if not vin:
            data.pop("vin", None)
        else:
            data["vin"] = vin
        self._write_data(data)

    def retrieve_vin(self) -> str:
        """Retrieve the VIN, returning an empty string if not found."""
        return self._read_data().get("vin", "")

    def store_proxy_url(self, proxy_url: str):
        """store the proxy url, removing it if blank. strip trailing '/' if present."""
        data = self._read_data()
        if not proxy_url:
            data.pop("proxy_url", None)
        else:
            cleaned_proxy_url = proxy_url.rstrip('/')
            data["proxy_url"] = cleaned_proxy_url
        self._write_data(data)

    def retrieve_proxy_url(self) -> str:
        """retrieve the proxy url, returning an empty string if not found."""
        return self._read_data().get("proxy_url", "")

    def clear_secure_storage(self):
        """Clear all data from storage."""
        self._write_data({})
