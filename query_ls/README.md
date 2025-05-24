# Simple Logseq Query Tool

A minimalist ClojureScript tool for running DataScript queries against a Logseq graph database.

## Features

- Simple command-line interface to query your Logseq database
- Uses EDN files for queries
- Written in ClojureScript with minimal dependencies
- Easy to modify and extend

## Installation

```bash
# Install dependencies
npm install

# Make the script executable
chmod +x index.mjs
```

## Usage

Run a query from an EDN file against your Logseq graph:

```bash
./index.mjs GRAPH_NAME QUERY_FILE
```

Where:
- `GRAPH_NAME` is the name of your Logseq graph
- `QUERY_FILE` is the path to an EDN file containing a DataScript query

## Example

```bash
# List available graphs
ls ~/.logseq/graphs

# Run a query
./index.mjs your-graph-name ./example_query.edn
```

## Example Queries

### Find all TODO tasks
```clojure
[:find (pull ?b [*])
 :where
 [?b :block/marker "TODO"]]
```

### Find blocks with specific content
```clojure
[:find (pull ?b [*])
 :where
 [?b :block/content ?c]
 [(clojure.string/includes? ?c "search term")]]
```

### Find tasks with priority A
```clojure
[:find (pull ?b [*])
 :where
 [?b :block/priority "A"]]
```

## Extending

This tool is designed to be simple and easy to modify. Some ideas for extensions:

- Add support for command-line queries
- Add result formatting options
- Implement caching for better performance
- Add support for query rules
