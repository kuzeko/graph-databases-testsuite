package main

import (
	"bufio"
	"flag"
	"fmt"
	"io"
	"log"
	"os"
	"strings"
	"strconv"
    "math"
)

var (
	in     = flag.String("i", "dataset.json", "input file")
	props  = flag.String("p", "props.txt", "props output file")
	labels = flag.String("l", "labels.txt", "labels output file")

    prop_map   = map[string]int{}
    labels_set = map[string]interface{}{}

    progress = 0
)

const (
    STRING      = iota
    DOUBLE
    FLOAT
    LONG
    INTEGER
)

func main() {
	flag.Parse()
	gen()
}

func gen() {
	fin, err := os.Open(*in)
	if err != nil {
		log.Fatal(err)
	}

	fprops, err := os.Create(*props)
	if err != nil {
		log.Fatal(err)
	}

	flabels, err := os.Create(*labels)
	if err != nil {
		log.Fatal(err)
	}

	// Create reader and writer
	bfin := bufio.NewReader(fin)
	bv := bufio.NewWriter(fprops)
	be := bufio.NewWriter(flabels)

	// Remove header
	bfin.ReadString('[')

	// Read input
	onEdges := false
	for {

		line, err := readEscaped(bfin, '}')
		if err == io.EOF {
			break
		} else if err != nil {
			log.Fatal(err)
		}

        progress += 1
        if progress == 100000 {
            progress = 0
            fmt.Print(".")
        }

		extract(line, onEdges)

		if r, _, err := bfin.ReadRune(); err == io.EOF {
			// Done
			break
		} else if r == ',' {
			// Next line
			continue
		} else if r == ']' {
			// Done with props?
			if !onEdges {
				// trim header
				bfin.ReadString('[')
				onEdges = true
			} else {
				// We have done (early exit)
				break
			}
		}
	}

    for k, t := range prop_map {
        bv.Write([]byte(fmt.Sprintf("%s,%s\n", k, toType(t))))
    }

    for l, _ := range labels_set {
        be.Write([]byte(l))
        be.Write([]byte("\n"))
    }

	bv.Flush()
	be.Flush()
	fin.Close()
	fprops.Close()
	flabels.Close()
}

func toType(i int) string {
    switch (i) {
        case STRING:
            return "string"
        case DOUBLE:
            return "double"
        case FLOAT:
            return "float"
        case LONG:
            return "long"
        case INTEGER:
            return "integer"
        default:
            return ""
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
		// fmt.Println("Line:", line)

		if e != nil {
			err = e
			break
		}

		if quoteCount += countEscaped(line, '"'); quoteCount%2 == 0 {
			// It's NOT escaped terminate
			break
		}

		// It's escaped go ahead reading
		// fmt.Println("Continuing:", line, quoteCount)
	}
	//fmt.Println("Stop:", strings.Join(buff, ""))
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


func extract(line string, isEdge bool) {
	// Parse row
	dict := make(map[string]string)
	tokens := parseLine(strings.Trim(line, " "))
	for i := 0; i < len(tokens); i += 2 {
		dict[strings.Trim(tokens[i], "\"")] = tokens[i+1]
	}

    if (isEdge) {
        if label, ok := dict["_label"]; ok {
            labels_set[strings.Trim(label, "\"")] = nil
        }
    }

    for k, v := range dict {
        if len(k) > 0 && k[0] != '_' {
            t := STRING
            if i, err := strconv.ParseInt(v, 10, 64); err == nil {
                if (math.MinInt32 <= i && i <= math.MaxInt32) {
                    t = INTEGER
                } else {
                    t = LONG
                }
            } else if f, err := strconv.ParseFloat(v, 64); err == nil {
                if ((math.SmallestNonzeroFloat32 <= f && f <= math.MaxFloat32) || 
                    (- math.MaxFloat32 <= f && f <= - math.SmallestNonzeroFloat32)) { 
                    t = FLOAT 
                } else {
                    t = DOUBLE
                }
            }
            if curr, ok := prop_map[k]; !ok || t < curr {
                prop_map[k] = t
            }
        }
    }
}
