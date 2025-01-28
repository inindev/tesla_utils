// Copyright (c) 2025, John Clark <inindev@gmail.com>
//
// Licensed under the MIT License. See LICENSE file in the project root for full license information.
package main

import (
	"context"
	"fmt"
	"log"
	"os"
	"strings"
	"time"

	"github.com/inindev/tesla_utils/auth"
	"github.com/teslamotors/vehicle-command/pkg/account"
	"github.com/teslamotors/vehicle-command/pkg/protocol"
	"github.com/teslamotors/vehicle-command/pkg/vehicle"
)

// exit codes
const (
	exitSuccess           = 0
	exitError             = 1
	exitAuthError         = 2
	exitVehicleSetupError = 3
	exitExecCommandError  = 4
)

// command and parameters
type Command struct {
	Name   string
	Params []string
}

// function type for car operations
type CarOperation func(ctx context.Context) error

// command function map
var CarCommand = map[string]func(*vehicle.Vehicle, context.Context) error{
	"lock":            func(v *vehicle.Vehicle, ctx context.Context) error { return v.Lock(ctx) },
	"unlock":          func(v *vehicle.Vehicle, ctx context.Context) error { return v.Unlock(ctx) },
	"opentrunk":       func(v *vehicle.Vehicle, ctx context.Context) error { return v.OpenTrunk(ctx) },
	"closetrunk":      func(v *vehicle.Vehicle, ctx context.Context) error { return v.CloseTrunk(ctx) },
	"openfrunk":       func(v *vehicle.Vehicle, ctx context.Context) error { return v.OpenFrunk(ctx) },
	"honkhorn":        func(v *vehicle.Vehicle, ctx context.Context) error { return v.HonkHorn(ctx) },
	"flashlights":     func(v *vehicle.Vehicle, ctx context.Context) error { return v.FlashLights(ctx) },
	"closewindows":    func(v *vehicle.Vehicle, ctx context.Context) error { return v.CloseWindows(ctx) },
	"ventwindows":     func(v *vehicle.Vehicle, ctx context.Context) error { return v.VentWindows(ctx) },
	"chargeportclose": func(v *vehicle.Vehicle, ctx context.Context) error { return v.ChargePortClose(ctx) },
	"chargeportopen":  func(v *vehicle.Vehicle, ctx context.Context) error { return v.ChargePortOpen(ctx) },
	"climate-on":      func(v *vehicle.Vehicle, ctx context.Context) error { return v.ClimateOn(ctx) },
	"climate-off":     func(v *vehicle.Vehicle, ctx context.Context) error { return v.ClimateOff(ctx) },
}

// executes the specified vehicle command
func executeCommand(ctx context.Context, car *vehicle.Vehicle, cmd Command) error {
	if err := car.Connect(ctx); err != nil {
		return fmt.Errorf("failed to connect to vehicle: %w", err)
	}
	if err := car.StartSession(ctx, nil); err != nil {
		return fmt.Errorf("failed to perform handshake with vehicle: %w", err)
	}

	cmd.Name = strings.ToLower(cmd.Name) // case insensitive

	// handle the climate command with parameters
	if cmd.Name == "climate" {
		if len(cmd.Params) == 0 {
			return fmt.Errorf("climate command requires a parameter (on/off/true/false)")
		}
		switch cmd.Params[0] {
		case "on", "true":
			cmd.Name = "climate-on"
		case "off", "false":
			cmd.Name = "climate-off"
		default:
			return fmt.Errorf("invalid parameter for climate command: %s", cmd.Params[0])
		}
	}

	op, ok := CarCommand[cmd.Name]
	if !ok {
		return fmt.Errorf("unknown command: %s", cmd.Name)
	}

	fmt.Printf("executing %s command...\n", cmd.Name)
	if err := op(car, ctx); err != nil {
		if protocol.MayHaveSucceeded(err) {
			log.Printf("%s command sent, but client could not confirm receipt: %s", cmd.Name, err)
		} else {
			return fmt.Errorf("failed to execute command %s: %w", cmd.Name, err)
		}
	}
	fmt.Printf("%s command executed successfully\n", cmd.Name)
	return nil
}

// setup the vehicle class
func setupVehicle(ctx context.Context, authData auth.AuthData) (*vehicle.Vehicle, error) {
	privateKeyFile, err := auth.GetKeyFile()
	if err != nil {
		return nil, fmt.Errorf("tesla_key_file environment variable not set: %w", err)
	}

	vin, err := auth.GetVin()
	if err != nil {
		return nil, fmt.Errorf("tesla_vin environment variable not set: %w", err)
	}

	privateKey, err := protocol.LoadPrivateKey(privateKeyFile)
	if err != nil {
		return nil, fmt.Errorf("failed to load private key: %w", err)
	}

	acct, err := account.New(authData.AccessToken, "tesla-vehicle-control/1.0.0")
	if err != nil {
		return nil, fmt.Errorf("authentication error: %w", err)
	}

	vehicle, err := acct.GetVehicle(ctx, vin, privateKey, nil)
	if err != nil {
		return nil, fmt.Errorf("failed to fetch vehicle info from account: %w", err)
	}
	return vehicle, nil
}

// handles the authentication process
func authenticate() (auth.AuthData, error) {
	authData, err := loadAuthData()
	if err != nil {
		log.Println("authentication token not found or expired, initiating new authentication sequence")

		state := auth.GetRandomCodeword()
		oauthURL, err := auth.GenOauthUrl(state)
		if err != nil {
			return auth.AuthData{}, fmt.Errorf("failed to generate OAuth URL: %w", err)
		}

		fmt.Printf("\n%s\n%s\n%s\n\n", state, strings.Repeat("~", len(state)), oauthURL)

		var authCode string
		fmt.Print("enter the authorization code from the Tesla authentication page: ")
		fmt.Scanln(&authCode)

		authData, err = auth.GetAuthToken(authCode)
		if err != nil {
			return auth.AuthData{}, fmt.Errorf("failed to get authentication tokens: %w", err)
		}

		if err := auth.SaveAuthData(authData); err != nil {
			return auth.AuthData{}, fmt.Errorf("failed to save auth data: %w", err)
		}
	}
	return authData, nil
}

// retrieve or refresh authentication token
func loadAuthData() (auth.AuthData, error) {
	authData, err := auth.LoadAuthData()
	if err != nil {
		return auth.AuthData{}, fmt.Errorf("failed to load auth data: %w", err)
	}

	lifePercentage := auth.CalculateTokenLifePercentage(authData)
	if lifePercentage <= 20 {
		return refreshToken(authData.RefreshToken)
	}
	return authData, nil
}

// refresh authentication token
func refreshToken(refreshToken string) (auth.AuthData, error) {
	newAuth, err := auth.RefreshAuthToken(refreshToken)
	if err != nil {
		return auth.AuthData{}, fmt.Errorf("failed to refresh token: %w", err)
	}
	if err := auth.SaveAuthData(newAuth); err != nil {
		return auth.AuthData{}, fmt.Errorf("failed to save refreshed auth data: %w", err)
	}
	return newAuth, nil
}

func main() {
	ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
	defer cancel()

	logger := log.New(os.Stderr, "", log.LstdFlags)

	if len(os.Args) < 2 {
		logger.Println("usage: program <command> [param1] [param2]")
		os.Exit(exitError)
	}

	cmd := Command{
		Name:   os.Args[1],
		Params: os.Args[2:],
	}

	authData, err := authenticate()
	if err != nil {
		logger.Println(err)
		os.Exit(exitAuthError)
	}

	vehicle, err := setupVehicle(ctx, authData)
	if err != nil {
		logger.Println(err)
		os.Exit(exitVehicleSetupError)
	}

	if err := executeCommand(ctx, vehicle, cmd); err != nil {
		logger.Println(err)
		os.Exit(exitExecCommandError)
	}

	os.Exit(exitSuccess)
}
