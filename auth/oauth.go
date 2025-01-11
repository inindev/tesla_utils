
package auth

import (
    "fmt"
    "net/url"
)


// generate the oauth url for initiating the authentication flow
func GenOauthUrl(state, clientId string) string {
    params := url.Values{
        "client_id":     {clientId},
        "locale":        {"en-US"},
        "prompt":        {"login"},
        "redirect_uri":  {RedirectUri},
        "response_type": {"code"},
        "scope":         {Scope},
        "state":         {state},
    }
    return fmt.Sprintf("%s?%s", AuthEp, params.Encode())
}

