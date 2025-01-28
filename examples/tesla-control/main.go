package main

import (
    "context"
    "errors"
    "flag"
    "fmt"
    "log"
    "os"
    "sort"
    "strings"
    "time"

    "github.com/inindev/tesla_utils/auth"
    "github.com/teslamotors/vehicle-command/pkg/account"
    "github.com/teslamotors/vehicle-command/pkg/protocol"
    "github.com/teslamotors/vehicle-command/pkg/vehicle"
)

func writeErr(format string, a ...interface{}) {
    fmt.Fprintf(os.Stderr, format, a...)
    fmt.Fprintf(os.Stderr, "\n")
}

const usage = `
 * Commands sent to a vehicle over the internet require a VIN and a token.
 * Commands sent to a vehicle over BLE require a VIN.
 * Account-management commands require a token.`

func Usage() {
    fmt.Printf("Usage: %s [OPTION...] COMMAND [ARG...]\n", os.Args[0])
    fmt.Printf("\nRun %s help COMMAND for more information. Valid COMMANDs are listed below.", os.Args[0])
    fmt.Println("")
    fmt.Println(usage)
    fmt.Println("")

    fmt.Printf("Available OPTIONs:\n")
    flag.PrintDefaults()
    fmt.Println("")
    fmt.Printf("Available COMMANDs:\n")
    maxLength := 0
    var labels []string
    for command := range commands {
        labels = append(labels, command)
        if len(command) > maxLength {
            maxLength = len(command)
        }
    }
    sort.Strings(labels)
    for _, command := range labels {
        info := commands[command]
        fmt.Printf("  %s%s %s\n", command, strings.Repeat(" ", maxLength-len(command)), info.help)
    }
}

func runCommand(acct *account.Account, car *vehicle.Vehicle, args []string, timeout time.Duration) int {
    ctx, cancel := context.WithTimeout(context.Background(), timeout)
    defer cancel()

    if err := execute(ctx, acct, car, args); err != nil {
        if protocol.MayHaveSucceeded(err) {
            log.Printf("Couldn't verify success: %s", err)
        } else if errors.Is(err, protocol.ErrNoSession) {
            log.Printf("You must provide a private key with -key-name or -key-file to execute this command")
        } else {
            log.Printf("Failed to execute command: %s", err)
        }
        return 1
    }
    return 0
}

func runInteractiveShell(acct *account.Account, car *vehicle.Vehicle, timeout time.Duration) int {
    InteractiveCommandBuilding(acct, car)
    return 0
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
    status := 1
    defer func() {
        os.Exit(status)
    }()

    var (
        debug          bool
        forceBLE       bool
        commandTimeout time.Duration
        connTimeout    time.Duration
    )

    flag.Usage = Usage
    flag.BoolVar(&debug, "debug", false, "Enable verbose debugging messages")
    flag.BoolVar(&forceBLE, "ble", false, "Force BLE connection even if OAuth environment variables are defined")
    flag.DurationVar(&commandTimeout, "command-timeout", 5*time.Second, "Set timeout for commands sent to the vehicle.")
    flag.DurationVar(&connTimeout, "connect-timeout", 45*time.Second, "Set timeout for establishing initial connection.")

    flag.Parse()
    if !debug {
        if debugEnv, ok := os.LookupEnv("TESLA_VERBOSE"); ok {
            debug = debugEnv != "false" && debugEnv != "0"
        }
    }
    if debug {
        log.SetFlags(log.LstdFlags | log.Lshortfile)
    } else {
        log.SetFlags(log.LstdFlags)
    }

    args := flag.Args()
    if len(args) > 0 {
        if args[0] == "help" {
            if len(args) == 1 {
                Usage()
                return
            }
            info, ok := commands[args[1]]
            if !ok {
                log.Printf("Unrecognized command: %s", args[1])
                return
            }
            info.Usage(args[1])
            status = 0
            return
        }
    }

    // load authentication data
    authData, err := loadAuthData()
    if err != nil {
        log.Printf("Error loading credentials: %v", err)
        return
    }

    ctx, cancel := context.WithTimeout(context.Background(), connTimeout)
    defer cancel()

    acct, err := account.New(authData.AccessToken, "")
    if err != nil {
        log.Printf("Error creating account: %v", err)
        return
    }

    // Load the private key for vehicle commands if needed
    keyFile, err := auth.GetKeyFile()
    if err != nil {
        log.Printf("Error getting key file path: %v", err)
        return
    }

    privateKey, err := protocol.LoadPrivateKey(keyFile)
    if err != nil {
        log.Printf("Error loading private key: %v", err)
        return
    }

    var car *vehicle.Vehicle
    vin, err := auth.GetVin()
    if err == nil && vin != "" {
        car, err = acct.GetVehicle(ctx, vin, privateKey, nil)
        if err != nil {
            log.Printf("Error: GetVehicle failed: %v", err)
            return
        }

        if err := car.Connect(ctx); err != nil {
            log.Printf("Error: Connect failed: %v", err)
            return
        }

        backoff := time.Second
        for attempt := 0; attempt < 3; attempt++ {
            if ctx.Err() != nil {
                log.Printf("Context timed out or canceled during Wakeup retries: %v", ctx.Err())
                return
            }
            if err := car.Wakeup(ctx); err == nil {
                log.Printf("Wakeup succeeded on attempt %d", attempt+1)
                break
            } else {
                if strings.Contains(err.Error(), "offline") || strings.Contains(err.Error(), "asleep") {
                    log.Printf("Wakeup failed on attempt %d - vehicle offline or asleep: %v", attempt+1, err)
                } else {
                    log.Printf("Wakeup failed on attempt %d: %v", attempt+1, err)
                }
                if attempt == 2 {
                    log.Println("Failed to wake up vehicle after multiple attempts")
                    return
                }
                time.Sleep(backoff)
                backoff *= 2
            }
        }

        backoff = time.Second
        for attempt := 0; attempt < 5; attempt++ {
            if ctx.Err() != nil {
                log.Printf("Context timed out or canceled during StartSession retries: %v", ctx.Err())
                return
            }
            if err := car.StartSession(ctx, nil); err == nil {
                log.Printf("StartSession succeeded on attempt %d", attempt+1)
                break
            } else {
                log.Printf("Error: StartSession failed on attempt %d: %v", attempt+1, err)
                if attempt == 4 {
                    log.Println("Error: Failed to start session after multiple attempts")
                    return
                }
                time.Sleep(backoff)
                backoff *= 2 // double the wait time for each retry
            }
        }

        // before command execution, refresh session if possible
        //if err := car.RefreshSession(ctx); err != nil {
        //    log.Printf("Failed to refresh session: %v", err)
        //    return
        //}

        if car != nil {
            defer car.Disconnect()
        }
    }

    if flag.NArg() > 0 {
        log.Println("attempting to execute command...")
        status = runCommand(acct, car, flag.Args(), commandTimeout)
    } else {
        log.Println("entering interactive shell...")
        status = runInteractiveShell(acct, car, commandTimeout)
    }
}

