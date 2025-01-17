//
// Copyright (c) 2025, John Clark <inindev@gmail.com>
//
// Licensed under the MIT License. See LICENSE file in the project root for full license information.
//
package main

import (
    "bufio"
    "fmt"
    "log"
    "os"
    "strings"

    "tesla_utils/auth"
)


func main() {
    reader := bufio.NewReader(os.Stdin)

    // Get client ID
    fmt.Print("Enter Tesla Client ID: ")
    clientId, _ := reader.ReadString('\n')
    clientId = strings.TrimSpace(clientId)

    // Get client secret
    fmt.Print("Enter Tesla Client Secret: ")
    clientSecret, _ := reader.ReadString('\n')
    clientSecret = strings.TrimSpace(clientSecret)

    // Get key file path
    fmt.Print("Enter Tesla Key File Path: ")
    keyFile, _ := reader.ReadString('\n')
    keyFile = strings.TrimSpace(keyFile)

    // Get VIN
    fmt.Print("Enter Tesla VIN: ")
    vin, _ := reader.ReadString('\n')
    vin = strings.TrimSpace(vin)

    // Get redirect URI
    fmt.Print("Enter Tesla Redirect URI: ")
    redirectURI, _ := reader.ReadString('\n')
    redirectURI = strings.TrimSpace(redirectURI)

    // Write configuration
    err := auth.WriteConfig(clientId, clientSecret, keyFile, vin, redirectURI)
    if err != nil {
        log.Fatalf("Failed to write config: %v", err)
    }

    fmt.Println("Configuration successfully written to config.json.")
}

