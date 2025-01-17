//
// Copyright (c) 2025, John Clark <inindev@gmail.com>
//
// Licensed under the MIT License. See LICENSE file in the project root for full license information.
//
package auth

import (
    "encoding/json"
    "fmt"
    "io/ioutil"
    "os"
)

// authentication cache file data
type AuthData struct {
    AccessToken  string `json:"access_token"`
    RefreshToken string `json:"refresh_token"`
    ExpiresIn    int    `json:"expires_in"`
    CapturedAt   string `json:"captured_at"`
    IdToken      string `json:"id_token"`
    TokenType    string `json:"token_type"`
}


// read and return authentication data from a json file
func LoadAuthData() (AuthData, error) {
    file, err := os.Open(AuthCacheFilePath())
    if err != nil {
        return AuthData{}, fmt.Errorf("auth cache file not found or cannot be opened: %v", err)
    }
    defer file.Close()

    var auth AuthData
    if err := json.NewDecoder(file).Decode(&auth); err != nil {
        return AuthData{}, fmt.Errorf("error decoding json from auth cache file: %v", err)
    }
    return auth, nil
}

// ensureFilePermissions checks if the file exists and has correct permissions, adjusts if necessary
func ensureFilePermissions(filePath string, desiredPerm os.FileMode) error {
    info, err := os.Stat(filePath)
    if err != nil {
        if os.IsNotExist(err) {
            return nil // file does not exist, permissions will be set by write operation
        }
        return fmt.Errorf("error checking file status: %v", err)
    }

    if info.Mode().Perm() != desiredPerm {
        if err := os.Chmod(filePath, desiredPerm); err != nil {
            return fmt.Errorf("error setting file permissions: %v", err)
        }
    }
    return nil
}

// write the authentication data to a json file with restricted permissions
func SaveAuthData(auth AuthData) error {
    authBytes, err := json.MarshalIndent(auth, "", "  ")
    if err != nil {
        return fmt.Errorf("error marshalling json: %v", err)
    }

    // Ensure correct permissions before writing
    if err := ensureFilePermissions(AuthCacheFilePath(), 0600); err != nil {
        return err
    }

    if err := ioutil.WriteFile(AuthCacheFilePath(), authBytes, 0600); err != nil {
        return fmt.Errorf("failed to save auth data: %v", err)
    }

    return nil
}

