package main

import (
	"bufio"
	"flag"
	"fmt"
	"io"
	"log"
	"os"
	"strings"
)

var (
	in    = flag.String("i", "dataset.json", "input file")
	nodes = flag.String("n", "nodes.json", "nodes output file")
	edges = flag.String("e", "edges.json", "edges output file")
)

const (
	NODE_COLLECTION = "V"
	EDGE_COLLECTION = "E"
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

	fnodes, err := os.Create(*nodes)
	if err != nil {
		log.Fatal(err)
	}

	fedges, err := os.Create(*edges)
	if err != nil {
		log.Fatal(err)
	}

	// Create reader and writer
	bfin := bufio.NewReader(fin)
	bv := bufio.NewWriter(fnodes)
	be := bufio.NewWriter(fedges)
	out := bv

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

		out.Write(serialize(processRow(line, onEdges)))

		if r, _, err := bfin.ReadRune(); err == io.EOF {
			// Done
			break
		} else if r == ',' {
			// Next line
			continue
		} else if r == ']' {
			// Done with nodes?
			if !onEdges {
				// trim header
				bfin.ReadString('[')
				onEdges = true
				out = be
			} else {
				// We have done (early exit)
				break
			}
		}
	}

	bv.Flush()
	be.Flush()
	fin.Close()
	fnodes.Close()
	fedges.Close()
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

func processRow(line string, isEdge bool) map[string]string {
	// Parse row
	dict := make(map[string]string)
	tokens := parseLine(strings.Trim(line, " "))
	for i := 0; i < len(tokens); i += 2 {
		dict[strings.Trim(tokens[i], "\"")] = tokens[i+1]
	}

	// Copy key (if present)
	if _, ok := dict["_key"]; !ok {
		if _, ok := dict["_id"]; ok {
			collection := NODE_COLLECTION
			_id := strings.Trim(dict["_id"], "\"")

			if isEdge {
				collection = EDGE_COLLECTION
				dict["_from"] = toID(NODE_COLLECTION, dict["_outV"])
				dict["_to"] = toID(NODE_COLLECTION, dict["_inV"])
				dict["label"] = dict["_label"]
				delete(dict, "_label")
				delete(dict, "_inV")
				delete(dict, "_outV")
			}

			dict["_key"] = "\"" + _id + "\""
			dict["_id"] = toID(collection, _id)
		}
	}

	return dict
}

func toID(collection, key string) string {
	// _id = collection/_key
	return fmt.Sprintf("\"%s/%s\"", collection, strings.Trim(key, "\""))
}

func serialize(dict map[string]string) []byte {
	o := make([]string, len(dict))
	i := 0
	for k, v := range dict {
		o[i] = fmt.Sprintf("\"%s\":%s", k, v)
		i++
	}
	return []byte("{" + strings.Join(o, ",") + "}\n")
}
