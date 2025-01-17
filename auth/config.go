//
// Copyright (c) 2025, John Clark <inindev@gmail.com>
//
// Licensed under the MIT License. See LICENSE file in the project root for full license information.
//
package auth

import (
    "fmt"
    "os"
    "path/filepath"
)

const (
    AuthEp        = "https://auth.tesla.com/oauth2/v3/authorize"
    Scope         = "openid user_data vehicle_device_data vehicle_cmds vehicle_charging_cmds energy_device_data energy_cmds offline_access"
    TokenEp       = "https://auth.tesla.com/oauth2/v3/token"
    Audience      = "https://fleet-api.prd.na.vn.cloud.tesla.com"

    teslaCfgDir   = ".tesla"  // $HOME/.tesla
    authCacheFile = "auth_cache.json"

    // tesla environment variable names
    teslaClientId     = "TESLA_CLIENT_ID"      // 00000000-0000-0000-0000-000000000000
    teslaClientSecret = "TESLA_CLIENT_SECRET"  //
    teslaKeyFile      = "TESLA_KEY_FILE"       // $HOME/.tesla/private.key
    teslaVin          = "TESLA_VIN"            // 5YJ00000000000000
    teslaRedirectUri  = "TESLA_REDIRECT_URI"   // https://auth.<yourdomain>.com/auth/callback
)

var (
    teslaCfgDirPath string
    cacheFilePath   string
)


func init() {
    homeDir, err := os.UserHomeDir()
    if err != nil {
        panic(err)
    }

    // Create the .tesla directory if it doesn't exist
    teslaCfgDirPath = filepath.Join(homeDir, teslaCfgDir)
    if err := os.MkdirAll(teslaCfgDirPath, 0700); err != nil {
        panic(err)
    }

    cacheFilePath = filepath.Join(teslaCfgDirPath, authCacheFile)
}

// path to the Tesla configuration directory
func TeslaCfgDirPath() string {
    return teslaCfgDirPath
}

// path to the auth cache file
func CacheFilePath() string {
    return cacheFilePath
}

// getEnvVar is a helper function to reduce code duplication for environment variable retrieval.
func getEnvVar(varName string) (string, error) {
    value := os.Getenv(varName)
    if value == "" {
        return "", fmt.Errorf("%s environment variable not set", varName)
    }
    return value, nil
}

// tesla_client_id environment variable
func GetClientId() (string, error) {
    return getEnvVar(teslaClientId)
}

// tesla_client_secret environment variable
func GetClientSecret() (string, error) {
    return getEnvVar(teslaClientSecret)
}

// tesla_key_file path environment variable
func GetKeyFile() (string, error) {
    return getEnvVar(teslaKeyFile)
}

// tesla_vin environment variable
func GetVin() (string, error) {
    return getEnvVar(teslaVin)
}

// tesla_redirect_uri environment variable
func GetRedirectUri() (string, error) {
    return getEnvVar(teslaRedirectUri)
}

