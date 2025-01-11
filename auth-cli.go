
package main

import (
    "fmt"
    "log"
    "os"
    "regexp"
    "strings"

    "tesla_utils/auth"
)


func handleAuthCommand(code string) {
    clientId := os.Getenv("TESLA_CLIENT_ID")
    clientSecret := os.Getenv("TESLA_CLIENT_SECRET")
    authResponse, err := auth.GetAuthToken(code, clientId, clientSecret)
    if err != nil {
        log.Printf("failed to get authentication tokens: %v", err)
        os.Exit(2)
    }
    log.Printf("access token: %s", authResponse.AccessToken)
    if err := auth.SaveAuthData(authResponse); err != nil {
        log.Printf("failed to save auth data: %v", err)
        os.Exit(3)
    }
    log.Println("successfully stored authentication tokens")
}

func handleGenCommand() {
    clientId := os.Getenv("TESLA_CLIENT_ID")
    if clientId == "" {
        log.Fatal("tesla_client_id environment variable not set")
        os.Exit(1)
    }
    state := auth.GetRandomCodeword()
    fmt.Printf("\n%s\n%s\n%s\n\n", state, strings.Repeat("~", len(state)), auth.GenOauthUrl(state, clientId))
}

func main() {
    if len(os.Args) > 1 {
        const tokenPattern = "^NA_[a-fA-F0-9]{60}$"
        tokenRegex := regexp.MustCompile(tokenPattern)
        token := os.Args[1]
        if tokenRegex.MatchString(token) {
            handleAuthCommand(token)
        } else {
            log.Printf("token '%s' appears corrupt\n", token)
        }
        return
    }

    // check if the auth cache file exists
    if _, err := os.Stat(auth.AuthCacheFile); err == nil {
        if err = auth.ManageToken(); err == nil {
            return
        }
    }

    // generate new auth
    fmt.Printf("\npaste into browser to generate a new NA_xxx auth token\n")
    handleGenCommand()
}

