//
// Copyright (c) 2025, John Clark <inindev@gmail.com>
//
// Licensed under the MIT License. See LICENSE file in the project root for full license information.
//
package auth

import (
    "embed"
    "fmt"
    "math/rand"
    "strings"
    "time"
)

//go:embed words.txt
var wordsFile embed.FS

func init() {
    rand.Seed(time.Now().UnixNano())
}

// generate a random state key by combining two words
func GetRandomCodeword() string {
    data, err := wordsFile.ReadFile("words.txt")
    if err != nil {
        fmt.Println("error reading embedded file:", err)
        return ""
    }

    words := strings.Fields(string(data))
    if len(words) < 2 {
        fmt.Println("not enough words to generate a key")
        return ""
    }

    // shuffle the slice of words in place
    rand.Shuffle(len(words), func(i, j int) {
        words[i], words[j] = words[j], words[i]
    })

    return strings.Join([]string{words[0], "_", words[1]}, "")
}

