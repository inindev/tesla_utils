#
# Copyright (c) 2025, John Clark <inindev@gmail.com>
#
# Licensed under the MIT License. See LICENSE file in the project root for full license information.
#

import logging
import webbrowser
import http.server
import socketserver
import time
from typing import Optional, Tuple
from urllib.parse import urlparse, parse_qs
from .oauth2 import OAuth2Client, AuthResult
from .storage import SecureStorage

logger = logging.getLogger(__name__)

class AuthConfig:
    """Configuration constants for OAuth2 authentication."""
    DEFAULT_CALLBACK = "http://localhost:8888/auth/callback"
    CALLBACK_PATH = "/auth/callback"  # Expected path for callback
    PROMPT_LAUNCH = "Would you like to launch the URL in your browser? [Y/n]: "
    PROMPT_CALLBACK = "Enter a custom callback URL or press Enter to use the default: "
    SERVER_TIMEOUT = 60  # Seconds to wait for callback

class AuthenticationError(Exception):
    """Custom exception for authentication failures."""
    pass

def prompt_user(message: str, default: Optional[str] = None) -> str:
    """Prompt the user for input with an optional default value."""
    full_message = f"{message} ({default})" if default else message
    response = input(full_message).strip()
    return response if response else default or ""

def get_yes_no_response(prompt: str) -> bool:
    """Get a yes/no response from the user, defaulting to yes."""
    response = input(prompt).strip().lower()
    return response in ('', 'y', 'yes')

def handle_manual_callback(client: OAuth2Client, storage: SecureStorage) -> None:
    """Handle manual callback by prompting for state and code."""
    print(f"\nRedirect example: {client.CALLBACK_URI}?code=abc123&state=xyz789")
    state = prompt_user("Enter the 'state' from the redirect URL: ")
    code = prompt_user("Enter the 'code' from the redirect URL: ")

    if not validate_state(state, storage.retrieve_state()):
        raise AuthenticationError("State mismatch. Please try again.")

    redirect_uri = f"{client.CALLBACK_URI}?code={code}&state={state}"
    process_auth_result(client.exchange_code_for_tokens(redirect_uri))

def handle_local_callback(client: OAuth2Client, storage: SecureStorage, port: int) -> None:
    """Handle local server callback on the specified port with one-shot requests."""
    class OAuthHandler(http.server.SimpleHTTPRequestHandler):
        def __init__(self, request, client_address, server, *, client: OAuth2Client, storage: SecureStorage):
            self.client = client
            self.storage = storage
            super().__init__(request, client_address, server)

        def do_GET(self) -> None:
            self.send_response(200)
            self.send_header("Content-type", "text/html")
            self.end_headers()
            self.wfile.write(b"Authentication complete. Close this window.")

            # check if this is the expected callback path
            if self.path.startswith(AuthConfig.CALLBACK_PATH):
                params = parse_qs(urlparse(self.path).query)
                code, state = params.get("code", [None])[0], params.get("state", [None])[0]
                stored_state = self.storage.retrieve_state()

                if not validate_state(state, stored_state):
                    self.server.auth_result = AuthResult.Failure("State mismatch")
                elif not code:
                    self.server.auth_result = AuthResult.Failure("No code received")
                else:
                    redirect_uri = f"{self.client.CALLBACK_URI}?code={code}&state={state}"
                    self.server.auth_result = self.client.exchange_code_for_tokens(redirect_uri)
                self.server.is_callback_received = True
            else:
                logger.debug(f"Ignoring request to {self.path}, waiting for {AuthConfig.CALLBACK_PATH}")

        def log_message(self, format: str, *args) -> None:
            pass  # suppress default logging

    handler = lambda request, client_address, server: OAuthHandler(
        request, client_address, server, client=client, storage=storage
    )
    with socketserver.TCPServer(("", port), handler) as server:
        server.auth_result = None
        server.is_callback_received = False
        server.timeout = 1  # Short timeout per request to check overall timeout

        logger.debug(f"Starting server on port {port}")
        print(f"Waiting for callback on {client.CALLBACK_URI} (timeout in {AuthConfig.SERVER_TIMEOUT}s)...")

        start_time = time.time()
        while not server.is_callback_received:
            if time.time() - start_time > AuthConfig.SERVER_TIMEOUT:
                logger.error("No callback received within timeout period")
                raise AuthenticationError("Timeout waiting for callback")
            server.handle_request()  # Process one request

        process_auth_result(server.auth_result)

def validate_state(received: str, expected: str) -> bool:
    """Validate the received state against the expected state."""
    if received != expected:
        logger.error(f"State mismatch: expected {expected}, got {received}")
        return False
    return True

def process_auth_result(result: Optional[AuthResult]) -> None:
    """Process and display the authentication result."""
    if isinstance(result, AuthResult.Success):
        logger.debug("Successfully exchanged code for tokens")
        print("Authentication successful! Tokens stored.")
    else:
        error_msg = getattr(result, "error_message", "Unknown error")
        logger.error(f"Token exchange failed: {error_msg}")
        raise AuthenticationError(f"Authentication failed: {error_msg}")

def init_auth() -> None:
    """Perform OAuth2 authentication with user-configurable callback."""
    storage = SecureStorage()

    # prompt for callback URL and configure client
    print(f"Default callback: {AuthConfig.DEFAULT_CALLBACK} (local server)")
    callback_url = prompt_user(AuthConfig.PROMPT_CALLBACK, AuthConfig.DEFAULT_CALLBACK)
    OAuth2Client.CALLBACK_URI = callback_url
    client = OAuth2Client(storage)

    # initiate oauth2 flow
    auth_url = client.initiate_auth_flow()
    print(f"Authorization URL: {auth_url}")

    # launch browser if requested
    if get_yes_no_response(AuthConfig.PROMPT_LAUNCH):
        logger.debug("Launching browser")
        webbrowser.open(auth_url)
        print("Authenticate in your browser and return here if using a custom callback.")

    # handle callback based on URL
    if callback_url == AuthConfig.DEFAULT_CALLBACK:
        port = urlparse(callback_url).port or 80
        handle_local_callback(client, storage, port)
    else:
        handle_manual_callback(client, storage)

if __name__ == "__main__":
    # parse command-line arguments
    parser = argparse.ArgumentParser(description="Perform Tesla OAuth2 authentication.")
    parser.add_argument("-v", "--verbose", action="store_true", help="Enable verbose (DEBUG) logging")
    args = parser.parse_args()

    # configure logging based on the verbose flag
    logging.basicConfig(level=logging.DEBUG if args.verbose else logging.WARNING)

    try:
        init_auth()
    except AuthenticationError as e:
        print(f"Error: {e}")
    except Exception as e:
        logger.error(f"Unexpected error: {e}", exc_info=True)
        print(f"An unexpected error occurred: {e}")
