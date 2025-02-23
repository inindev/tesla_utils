#
# Copyright (c) 2025, John Clark <inindev@gmail.com>
#
# Licensed under the MIT License. See LICENSE file in the project root for full license information.
#

import argparse
import json
import logging
import re
import ssl
import sys
import certifi
from urllib.parse import urlencode, urlparse
import http.client
from tesla_auth.storage import SecureStorage
from tesla_auth.oauth2 import OAuth2Client

FLEET_API_URL = "https://fleet-api.prd.na.vn.cloud.tesla.com"

logger = logging.getLogger("tesla_request")

def make_request(method, url, data=None):
    """Make an HTTP GET or POST request to the given URL with optional data."""
    storage = SecureStorage()
    client = OAuth2Client(storage)
    proxy_url = storage.retrieve_proxy_url() or ""
    vin = storage.retrieve_vin()
    access_token = client.get_access_token()

    # url pre-processing: prioritize user-provided full url, then vin-specific, else fleet api
    if not url.lower().startswith("http"):
        url_base = proxy_url if "{vin}" in url.lower() else FLEET_API_URL
        url = f"{url_base.rstrip('/')}/{url.lstrip('/')}"
    url = re.sub(r"{vin}", vin or "", url, flags=re.IGNORECASE)

    # setup connection with tls 1.3 and certifi ca bundle
    parsed_url = urlparse(url)
    context = ssl.SSLContext(ssl.PROTOCOL_TLS_CLIENT)
    context.minimum_version = ssl.TLSVersion.TLSv1_3
    context.load_verify_locations(cafile=certifi.where())
    conn = http.client.HTTPSConnection(parsed_url.netloc, context=context)

    # prepare headers and body
    headers = {"Authorization": f"Bearer {access_token}"} if access_token else { }
    body = urlencode(data).encode("utf-8") if method == "POST" and data else None
    if body:
        headers["Content-Type"] = "application/x-www-form-urlencoded"

    try:
        conn.request(method, parsed_url.path + (f"?{parsed_url.query}" if parsed_url.query else ""), body, headers)
        response = conn.getresponse()
        if response.status != 200:
            raise Exception(f"HTTP {response.status}: {response.reason}")

        response_body = response.read().decode("utf-8")
        content_type = response.getheader("Content-Type", "").lower()
        conn.close()

        print(json.dumps(json.loads(response_body), indent=4, sort_keys=True) if "application/json" in content_type else response_body)

    except Exception as e:
        logger.error(f"request failed: {e}", exc_info=True)
        print(f"Error: {e}", file=sys.stderr)
        sys.exit(1)

def main():
    """Handle HTTP GET or POST requests with optional data and VIN substitution."""
    print()
    parser = argparse.ArgumentParser(description="Make HTTP requests with Tesla config VIN substitution")
    parser.add_argument("method", choices=["GET", "get", "POST", "post"], help="HTTP method: GET or POST (case insensitive)", type=str.upper)
    parser.add_argument("url", help="URL to request (relative paths prepend proxy_url if {vin} present, else fleet API; use {vin} or {VIN} for VIN substitution)")
    parser.add_argument("-d", "--data", nargs="*", help="Key-value pairs for POST data (e.g., key1=value1 key2=value2)")
    parser.add_argument("-v", "--verbose", action="store_true", help="Enable verbose (DEBUG) logging")
    args = parser.parse_args()

    logging.basicConfig(level=logging.DEBUG if args.verbose else logging.WARNING)
    data = dict(item.split("=", 1) for item in args.data) if args.data else None
    if data and any("=" not in item for item in args.data):
        parser.error("Data must be in key=value format (e.g., -d key1=value1 key2=value2)")

    make_request(args.method, args.url, data)

if __name__ == "__main__":
    try:
        main()
    except SystemExit:
        pass
    print()
