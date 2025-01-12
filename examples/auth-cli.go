//
// Copyright (c) 2025, John Clark <inindev@gmail.com>
//
// Licensed under the MIT License. See LICENSE file in the project root for full license information.
//
package main

import (
    "fmt"
    "log"
    "os"
    "regexp"
    "strings"

    "tesla_utils/auth"
)

// exit codes
//  2 - corrupt token
//  3 - failed to get authentication tokens
//  4 - failed to save auth data
//  5 - failed to manage the token
//  6 - for any error other than "file does not exist" when checking the auth cache file
//  7 - failed to generate the oauth url

func handleAuthCommand(code string) {
    authResponse, err := auth.GetAuthToken(code)
    if err != nil {
        log.Printf("failed to get authentication tokens: %v", err)
        os.Exit(3)
    }
    log.Printf("access token: %s", authResponse.AccessToken)

    if err := auth.SaveAuthData(authResponse); err != nil {
        log.Printf("failed to save auth data: %v", err)
        os.Exit(4)
    }
    log.Println("successfully stored authentication tokens")
}

func main() {
    if len(os.Args) > 1 {
        const tokenPattern = "^NA_[a-fA-F0-9]{60}$"
        tokenRegex := regexp.MustCompile(tokenPattern)
        token := os.Args[1]
        if !tokenRegex.MatchString(token) {
            log.Printf("token '%s' appears corrupt\n", token)
            os.Exit(2) // Exit with a specific code for corrupt token
        }
        handleAuthCommand(token)
        return
    }

    // check if the auth cache file exists
    if _, statErr := os.Stat(auth.CacheFilePath()); statErr == nil {
        if err := auth.ManageToken(); err != nil {
            log.Printf("failed to manage token: %v", err)
            os.Exit(5)
        }
        return
    } else if !os.IsNotExist(statErr) {
        log.Printf("error checking auth cache file: %v", statErr)
        os.Exit(6)
    }

    // generate new auth
    fmt.Printf("\npaste into browser to generate a new NA_xxx auth token\n")
    state := auth.GetRandomCodeword()
    oauthUrl, err := auth.GenOauthUrl(state)
    if err != nil {
        log.Printf("failed to generate oauth url: %v", err)
        os.Exit(7)
    }
    fmt.Printf("\n%s\n%s\n%s\n\n", state, strings.Repeat("~", len(state)), oauthUrl)
}

