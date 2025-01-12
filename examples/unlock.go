//
// Copyright (c) 2025, John Clark <inindev@gmail.com>
//
// Licensed under the MIT License. See LICENSE file in the project root for full license information.
//
package main

import (
    "context"
    "fmt"
    "log"
    "os"
    "strings"
    "time"

    "github.com/teslamotors/vehicle-command/pkg/account"
    "github.com/teslamotors/vehicle-command/pkg/protocol"
    "github.com/teslamotors/vehicle-command/pkg/vehicle"

    "tesla_utils/auth"
)

// exit codes
//  1 - general error (environment variables, private key loading, vehicle connection issues, etc.)
//  3 - failed to get authentication tokens
//  4 - failed to save auth data
//  7 - failed to generate the oauth url

// get auth data from cache or refresh if necessary
func getAuthData() (auth.AuthData, error) {
    authData, err := auth.LoadAuthData()
    if err != nil {
        return auth.AuthData{}, err
    }

    lifePercentage := auth.CalculateTokenLifePercentage(authData)
    if lifePercentage <= 20 {
        return refreshToken(authData.RefreshToken)
    }
    return authData, nil
}

// refresh token using the refresh token
func refreshToken(refreshToken string) (auth.AuthData, error) {
    newAuth, err := auth.RefreshAuthToken(refreshToken)
    if err != nil {
        return auth.AuthData{}, err
    }
    if err := auth.SaveAuthData(newAuth); err != nil {
        return auth.AuthData{}, err
    }
    return newAuth, nil
}

// lock or unlock car based on the lock parameter
func lockUnlockCar(ctx context.Context, car *vehicle.Vehicle, lock bool) error {
    if err := car.Connect(ctx); err != nil {
        log.Printf("failed to connect to vehicle: %s\n", err)
        return err
    }

    if err := car.StartSession(ctx, nil); err != nil {
        log.Printf("failed to perform handshake with vehicle: %s\n", err)
        return err
    }

    var action string
    var operation func(ctx context.Context) error

    if lock {
        action = "lock"
        operation = car.Lock
    } else {
        action = "unlock"
        operation = car.Unlock
    }

    fmt.Printf("%s car...\n", action)
    if err := operation(ctx); err != nil {
        if protocol.MayHaveSucceeded(err) {
            log.Printf("%s command sent, but client could not confirm receipt: %s\n", action, err)
        } else {
            log.Printf("failed to %s vehicle: %s\n", action, err)
        }
        return err
    }
    fmt.Printf("vehicle %sed!\n", action)
    return nil
}

func main() {
    logger := log.New(os.Stderr, "", 0)
    status := 1 // exit code
    defer func() {
        os.Exit(status)
    }()

    lock := false
    if len(os.Args) > 1 && strings.EqualFold(os.Args[1], "false") {
        lock = true
    }

    privateKeyFile, err := auth.GetKeyFile()
    if err != nil {
        logger.Printf("tesla_key_file environment variable not set")
        return
    }

    vin, err := auth.GetVin()
    if err != nil {
        logger.Printf("tesla_vin environment variable not set")
        return
    }

    privateKey, err := protocol.LoadPrivateKey(privateKeyFile)
    if err != nil {
        logger.Printf("failed to load private key: %s", err)
        return
    }

    // Check if auth cache file exists
    authData, err := getAuthData()
    if err != nil {
        // If auth data not found or expired, initiate new auth
        logger.Println("authentication token not found or expired. initiating new authentication sequence.")

        state := auth.GetRandomCodeword()
        oauthUrl, err := auth.GenOauthUrl(state)
        if err != nil {
            logger.Printf("failed to generate oauth url: %v", err)
            status = 7
            return
        }
        fmt.Printf("\n%s\n%s\n%s\n\n", state, strings.Repeat("~", len(state)), oauthUrl)

        // Wait for user to input the code after visiting the auth url
        var authCode string
        fmt.Print("enter the authorization code from the tesla authentication page: ")
        fmt.Scanln(&authCode)

        authData, err = auth.GetAuthToken(authCode)
        if err != nil {
            logger.Printf("failed to get authentication tokens: %v", err)
            status = 3
            return
        }

        if err := auth.SaveAuthData(authData); err != nil {
            logger.Printf("failed to save auth data: %v", err)
            status = 4
            return
        }
    }

    userAgent := "example-unlock/1.0.0"

    ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
    defer cancel()

    acct, err := account.New(authData.AccessToken, userAgent)
    if err != nil {
        logger.Printf("authentication error: %s", err)
        return
    }

    car, err := acct.GetVehicle(ctx, vin, privateKey, nil)
    if err != nil {
        logger.Printf("failed to fetch vehicle info from account: %s", err)
        return
    }

    if err := lockUnlockCar(ctx, car, lock); err != nil {
        return
    }
    status = 0
}
