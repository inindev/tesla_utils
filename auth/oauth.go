//
// Copyright (c) 2025, John Clark <inindev@gmail.com>
//
// Licensed under the MIT License. See LICENSE file in the project root for full license information.
//
package auth

import (
    "fmt"
    "net/url"
)

// generate the oauth url for initiating the authentication flow
func GenOauthUrl(state string) (string, error) {
    clientId, err := GetClientId()
    if err != nil {
        return "", fmt.Errorf("failed to get client ID: %w", err)
    }

    redirectUri, err := GetRedirectUri()
    if err != nil {
        return "", fmt.Errorf("failed to get redirect URI: %w", err)
    }

    params := url.Values{
        "client_id":     {clientId},
        "locale":        {"en-US"},
        "prompt":        {"login"},
        "redirect_uri":  {redirectUri},
        "response_type": {"code"},
        "scope":         {Scope},
        "state":         {state},
    }
    return fmt.Sprintf("%s?%s", AuthEp, params.Encode()), nil
}

