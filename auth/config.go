// Copyright (c) 2025, John Clark <inindev@gmail.com>
//
// Licensed under the MIT License. See LICENSE file in the project root for full license information.
package auth

import (
	"encoding/json"
	"fmt"
	"io/ioutil"
	"os"
	"path/filepath"
	"strings"
)

const (
	AuthEp   = "https://auth.tesla.com/oauth2/v3/authorize"
	Scope    = "openid user_data vehicle_device_data vehicle_cmds vehicle_charging_cmds energy_device_data energy_cmds offline_access"
	TokenEp  = "https://auth.tesla.com/oauth2/v3/token"
	Audience = "https://fleet-api.prd.na.vn.cloud.tesla.com"

	teslaCfgDir   = ".tesla"          // $HOME/.tesla
	authCacheFile = "auth_cache.json" // $HOME/.tesla/auth_cache.json
	configFile    = "config.json"     // $HOME/.tesla/config.json

	// tesla environment variable names
	teslaClientId     = "TESLA_CLIENT_ID"     // 00000000-0000-0000-0000-000000000000
	teslaClientSecret = "TESLA_CLIENT_SECRET" //
	teslaKeyFile      = "TESLA_KEY_FILE"      // $HOME/.tesla/private.key
	teslaVin          = "TESLA_VIN"           // 5YJ00000000000000
	teslaRedirectUri  = "TESLA_REDIRECT_URI"  // https://auth.<yourdomain>.com/auth/callback
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
		if val, ok := config[strings.ToLower(varName)]; ok {
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

// create config file from params
func WriteConfig(clientID, clientSecret, keyFile, vin, redirectURI string) error {
	config := map[string]string{
		strings.ToLower(teslaClientId):     clientID,
		strings.ToLower(teslaClientSecret): clientSecret,
		strings.ToLower(teslaKeyFile):      keyFile,
		strings.ToLower(teslaVin):          vin,
		strings.ToLower(teslaRedirectUri):  redirectURI,
	}

	data, err := json.MarshalIndent(config, "", "  ")
	if err != nil {
		return fmt.Errorf("failed to marshal config to JSON: %v", err)
	}

	// ensure correct file permissions before writing
	if err := EnsureFilePermissions(configFilePath, 0600); err != nil {
		return err
	}

	if err := ioutil.WriteFile(configFilePath, data, 0600); err != nil {
		return fmt.Errorf("failed to write config file: %v", err)
	}

	return nil
}

// check if the file exists and has correct permissions, adjusts if necessary
func EnsureFilePermissions(filePath string, desiredPerm os.FileMode) error {
	info, err := os.Stat(filePath)
	if err != nil {
		if os.IsNotExist(err) {
			return nil // file does not exist
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
