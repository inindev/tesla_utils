
package auth

import (
    "encoding/json"
    "fmt"
    "log"
    "net/http"
    "net/url"
    "time"
)


// fetch authentication tokens from server
func GetAuthToken(code string) (AuthData, error) {
    clientId, err := GetClientId()
    if err != nil {
        return AuthData{}, err
    }

    clientSecret, err := GetClientSecret()
    if err != nil {
        return AuthData{}, err
    }

    redirectUri, err := GetRedirectUri()
    if err != nil {
        return AuthData{}, err
    }

    data := url.Values{
        "grant_type":    {"authorization_code"},
        "client_id":     {clientId},
        "client_secret": {clientSecret},
        "code":          {code},
        "audience":      {Audience},
        "redirect_uri":  {redirectUri},
    }

    log.Printf("Sending request to: %s", TokenEp)
    log.Printf("With data: %v", data.Encode())

    resp, err := http.PostForm(TokenEp, data)
    if err != nil {
        return AuthData{}, fmt.Errorf("failed to get auth token: %v", err)
    }
    defer resp.Body.Close()

    if resp.StatusCode != http.StatusOK {
        return AuthData{}, fmt.Errorf("request failed with status %d", resp.StatusCode)
    }

    var authData AuthData
    if err := json.NewDecoder(resp.Body).Decode(&authData); err != nil {
        return AuthData{}, fmt.Errorf("error decoding json: %v", err)
    }

    authData.CapturedAt = time.Now().Format(time.RFC3339)
    return authData, nil
}

// refresh the auth token using the provided token
func RefreshAuthToken(refreshToken string) (AuthData, error) {
    clientId, err := GetClientId()
    if err != nil {
        return AuthData{}, err
    }

    data := url.Values{}
    data.Set("grant_type", "refresh_token")
    data.Set("client_id", clientId)
    data.Set("refresh_token", refreshToken)

    resp, err := http.PostForm(TokenEp, data)
    if err != nil {
        return AuthData{}, fmt.Errorf("failed to refresh auth token: %v", err)
    }
    defer resp.Body.Close()

    var authData AuthData
    if err := json.NewDecoder(resp.Body).Decode(&authData); err != nil {
        return AuthData{}, fmt.Errorf("error decoding json: %v", err)
    }

    authData.CapturedAt = time.Now().Format(time.RFC3339)
    return authData, nil
}

// calculates the remaining life of the token with fallbacks
func CalculateTokenLifePercentage(authData AuthData) int {
    if authData.CapturedAt == "" {
        log.Println("captured_at is missing, assuming token needs refresh")
        return 0
    }

    capturedTime, err := time.Parse(time.RFC3339, authData.CapturedAt)
    if err != nil {
        log.Printf("failed to parse captured time: %v, assuming token needs refresh", err)
        return 0
    }

    ageSec := time.Now().Sub(capturedTime)
    durationSec := time.Duration(authData.ExpiresIn) * time.Second

    if durationSec < 1 {
        log.Println("expires_in is zero, assuming token needs immediate refresh")
        return 0
    }

    percentage := 100 - int(ageSec*100/durationSec)
    if percentage < 0 {
        return 0
    }
    if percentage > 100 {
        return 100
    }
    return percentage
}

// check if the token needs refreshing and refreshes if necessary
func ManageToken() error {
    authData, err := LoadAuthData()
    if err != nil {
        return fmt.Errorf("failed to load auth data: %v", err)
    }

    lifePercentage := CalculateTokenLifePercentage(authData)
    if lifePercentage > 20 {
        log.Printf("using cached token, life remaining: %d%%", lifePercentage)
        log.Printf("access token: %s", authData.AccessToken)
        return nil
    }

    log.Printf("token needs refreshing, life remaining: %d%%", lifePercentage)
    log.Printf("ori authData: %v", authData)
    newAuth, err := RefreshAuthToken(authData.RefreshToken)
    if err != nil {
        return fmt.Errorf("failed to refresh token: %v", err)
    }

    if err := SaveAuthData(newAuth); err != nil {
        return fmt.Errorf("error saving new auth data to file cache: %v", err)
    }

    log.Printf("successfully generated new access token")
    log.Printf("access token: %s", newAuth.AccessToken)
    return nil
}

