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
    "io/ioutil"
    "encoding/json"
)

const (
    AuthEp        = "https://auth.tesla.com/oauth2/v3/authorize"
    Scope         = "openid user_data vehicle_device_data vehicle_cmds vehicle_charging_cmds energy_device_data energy_cmds offline_access"
    TokenEp       = "https://auth.tesla.com/oauth2/v3/token"
    Audience      = "https://fleet-api.prd.na.vn.cloud.tesla.com"

    teslaCfgDir   = ".tesla"           // $HOME/.tesla
    authCacheFile = "auth_cache.json"  // $HOME/.tesla/auth_cache.json
    configFile    = "config.json"      // $HOME/.tesla/config.json

    // tesla environment variable names
    teslaClientId     = "TESLA_CLIENT_ID"      // 00000000-0000-0000-0000-000000000000
    teslaClientSecret = "TESLA_CLIENT_SECRET"  //
    teslaKeyFile      = "TESLA_KEY_FILE"       // $HOME/.tesla/private.key
    teslaVin          = "TESLA_VIN"            // 5YJ00000000000000
    teslaRedirectUri  = "TESLA_REDIRECT_URI"   // https://auth.<yourdomain>.com/auth/callback
)

var (
    teslaCfgDirPath   string
    authCacheFilePath string
    configFilePath    string
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

    authCacheFilePath = filepath.Join(teslaCfgDirPath, authCacheFile)
    configFilePath = filepath.Join(teslaCfgDirPath, configFile)
}

// path to the Tesla configuration directory
func TeslaCfgDirPath() string {
    return teslaCfgDirPath
}

// path to the auth cache file
func AuthCacheFilePath() string {
    return authCacheFilePath
}

// path to the config file
func ConfigFilePath() string {
    return configFilePath
}

// prefer environment variables over config file
func getConfigValue(varName string) (string, error) {
    // environment has precedence
    if envVal := os.Getenv(varName); envVal != "" {
        return envVal, nil
    }

    // not found in environment, read from config file
    config, err := readConfig()
    if err == nil {
        if val, ok := config[varName]; ok {
            return val, nil
        }
    }

    return "", fmt.Errorf("%s not found in environment variables or config file", varName)
}

// return the content of the config file as a map
func readConfig() (map[string]string, error) {
    if _, err := os.Stat(configFilePath); os.IsNotExist(err) {
        return map[string]string{}, nil
    }

    data, err := ioutil.ReadFile(configFilePath)
    if err != nil {
        return nil, fmt.Errorf("failed to read config file: %v", err)
    }

    var config map[string]string
    if err := json.Unmarshal(data, &config); err != nil {
        return nil, fmt.Errorf("failed to parse config file: %v", err)
    }
    return config, nil
}

// tesla_client_id environment variable
func GetClientId() (string, error) {
    return getConfigValue(teslaClientId)
}

// tesla_client_secret environment variable
func GetClientSecret() (string, error) {
    return getConfigValue(teslaClientSecret)
}

// tesla_key_file path environment variable
func GetKeyFile() (string, error) {
    return getConfigValue(teslaKeyFile)
}

// tesla_vin environment variable
func GetVin() (string, error) {
    return getConfigValue(teslaVin)
}

// tesla_redirect_uri environment variable
func GetRedirectUri() (string, error) {
    return getConfigValue(teslaRedirectUri)
}

