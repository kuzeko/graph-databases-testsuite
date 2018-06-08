package main

import (
	"bufio"
	"flag"
	"fmt"
	"io"
	"log"
	"os"
	"regexp"
	"strings"
)

var (
	inF  = flag.String("i", "dataset.json", "input file")
	rF   = flag.String("r", "replacements.txt", "replacements file")
	outF = flag.String("o", "out.json", "output file")
)

var (
	lblre = regexp.MustCompile(`"_label":"([^"]*)"`)
	rMap  = map[string]string{}
)

func main() {
	flag.Parse()
	gen()
}

func gen() {
	fin, err := os.Open(*inF)
	if err != nil {
		log.Fatal(err)
	}
	defer fin.Close()

	fr, err := os.Open(*rF)
	if err != nil {
		log.Fatal(err)
	}
	defer fr.Close()

	fout, err := os.Create(*outF)
	if err != nil {
		log.Fatal(err)
	}
	defer fout.Close()

	// Create reader and writer
	bfin := bufio.NewReader(fin)
	br := bufio.NewReader(fr)
	out := bufio.NewWriter(fout)
	defer out.Flush()

	fmt.Println("loading replacement map")
	populateMap(br)
	fmt.Println("done loading replacement map")

	fmt.Println("replacing")
	// Remove header
	if header, err := bfin.ReadBytes('['); err == nil {
		out.Write(header)
	} else {
		panic(err)
	}

	// Read input
	onEdges := false
	for {

		line, err := readEscaped(bfin, '}')
		if err == io.EOF {
			break
		} else if err != nil {
			log.Fatal(err)
		}

		// If on nodes, just copy the output
		if !onEdges {
			out.WriteString(line)
		} else {
			out.WriteString(replaceHash(line))
		}

		if r, _, err := bfin.ReadRune(); err == io.EOF {
			// Done
			break
		} else if r == ',' {
			// Next line
			out.WriteRune(',')
			continue
		} else if r == ']' {
			// Done with nodes?
			out.WriteRune(']')
			mid, err := bfin.ReadString('[')
			out.WriteString(mid)
			if err == nil {
				onEdges = true
			} else if err != io.EOF {
				panic(err)
			} else {
				// We have done (early exit)
				break
			}
		}
	}
}

func readEscaped(stream *bufio.Reader, delim byte) (string, error) {
	var (
		buff       []string
		quoteCount int
		err        error
	)

	for {
		line, e := stream.ReadString(delim)
		buff = append(buff, line)
		if e != nil {
			err = e
			break
		}
		if quoteCount += countEscaped(line, '"'); quoteCount%2 == 0 {
			// It's NOT escaped terminate
			break
		}
	}
	return strings.Join(buff, ""), err
}

func parseLine(str string) []string {
	var (
		token []string

		start int

		open   bool
		escape bool
	)

	for i, c := range str {
		if c == '\\' {
			escape = !escape
		} else {
			if c == '"' && !escape {
				open = !open
			} else if (c == ':' || c == ',') && !open {
				token = append(token, strings.Trim(str[start:i], ":{},"))
				start = i
			}
			escape = false
		}
	}
	token = append(token, strings.Trim(str[start:len(str)], ":{},"))
	return token
}

func countEscaped(str string, c rune) int {
	var (
		count  int
		escape bool
	)
	for _, r := range str {
		if r == '\\' {
			escape = !escape
		} else {
			if r == c && !escape {
				count++
			}
			escape = false
		}
	}
	return count
}

func replaceHash(line string) string {
	label := lblre.FindString(line)
	if v, exists := rMap[label]; exists {
		return strings.Replace(line, label, v, 1)
	}
	return line
}

func populateMap(reader *bufio.Reader) {
	for {
		if line, _, err := reader.ReadLine(); err != nil && err != io.EOF {
			panic(err)
		} else if err == io.EOF {
			break
		} else {
			ts := strings.Split(string(line), "@")
			rMap[ts[1]] = ts[2]
		}
	}
}
