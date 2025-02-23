#
# Copyright (c) 2025, John Clark <inindev@gmail.com>
#
# Licensed under the MIT License. See LICENSE file in the project root for full license information.
#

import logging
import argparse
import time
from tesla_auth.init_flow import init_auth, AuthenticationError
from tesla_auth.oauth2 import OAuth2Client, AuthResult
from tesla_auth.storage import SecureStorage

logger = logging.getLogger("tesla_auth")

def config_setup():
    """Prompt the user for configuration values and store them in SecureStorage."""
    storage = SecureStorage()

    vin = input("Enter Vehicle Identification Number (VIN): ")
    storage.store_vin(vin)
    logger.debug(f"Stored VIN: '{vin}'")

    base_url = input("Enter Base URL: ")
    storage.store_base_url(base_url)
    logger.debug(f"Stored Base URL: '{base_url}'")

    client_id = input("Enter Client ID: ")
    storage.store_client_id(client_id)
    logger.debug(f"Stored Client ID: '{client_id}'")

    client_secret = input("Enter Client Secret: ")
    storage.store_client_secret(client_secret)
    logger.debug(f"Stored Client Secret: '{client_secret}'")

    print("Configuration stored successfully.")

def main():
    """Aggregate script to handle Tesla OAuth2 configuration, authentication, and token management."""
    print()
    parser = argparse.ArgumentParser(description="Tesla OAuth2 Authentication and Configuration Tool")
    parser.add_argument("--config", action="store_true", help="Store configuration values")
    parser.add_argument("--init", action="store_true", help="Initiate OAuth2 authentication")
    parser.add_argument("--refresh", action="store_true", help="Refresh the existing access token")
    parser.add_argument("--status", action="store_true", help="Report token expiration time and life remaining")
    parser.add_argument("-v", "--verbose", action="store_true", help="Enable verbose (DEBUG) logging")
    args = parser.parse_args()

    # configure logging: WARNING by default
    logging.basicConfig(level=logging.DEBUG if args.verbose else logging.WARNING)

    actions = sum([args.config, args.init, args.refresh, args.status])
    if actions > 1:
        parser.error("Only one of --config, --init, --refresh, or --status can be specified at a time.")
    elif actions == 0:
        parser.print_help()
        return

    storage = SecureStorage()
    client = OAuth2Client(storage)

    try:
        if args.config:
            print("Running configuration storage...")
            config_setup()
        elif args.init:
            print("Running authentication flow...")
            init_auth()
        elif args.refresh:
            print("Refreshing access token...")
            result = client.refresh_access_token()
            if not isinstance(result, AuthResult.Success):
                logger.error(f"Failed to refresh token: {result.error_message}")
                return
            print("Access token refreshed successfully!")
            # get and print new token info
            exp_info = client.get_jwt_exp_info()
            if exp_info:
                exp_time, life_remain = exp_info
                exp_datetime = time.strftime("%Y-%m-%d %H:%M:%S UTC", time.gmtime(exp_time))
                print(f"New token expiration: {exp_datetime}")
                print(f"Life remaining: {life_remain}%")
        elif args.status:
            print("Checking token status...")
            exp_info = client.get_jwt_exp_info()
            if exp_info is None:
                print("No valid access token found.")
                return
            exp_time, life_remain = exp_info
            exp_datetime = time.strftime("%Y-%m-%d %H:%M:%S UTC", time.gmtime(exp_time))
            print(f"Expiration Time: {exp_datetime}")
            print(f"Life Remaining: {life_remain}%")
    except AuthenticationError as e:
        logger.error(f"Authentication error: {e}")
        print(f"Error: {e}")
    except Exception as e:
        logger.error(f"Unexpected error: {e}", exc_info=True)
        print(f"An unexpected error occurred: {e}")

if __name__ == "__main__":
    try:
        main()
    except SystemExit:
        pass
    print()
