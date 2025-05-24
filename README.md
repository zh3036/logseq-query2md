# Logseq Block Extractor

A toolkit for extracting and structuring Logseq blocks and their children based on page references.

## Overview

This project provides tools to:

1. Find all blocks in a Logseq graph that reference a specific page/tag (e.g., `[[pyq/posted]]`)
2. Extract the content, UUID, and page of each block
3. Recursively fetch all child blocks, maintaining the hierarchy
4. Output a clean, structured EDN format for further processing

## Features

- Query blocks by reference tag (e.g., `[[pyq/posted]]`, `[[无限游戏]]`)
- Preserve parent-child relationships in the extracted data
- Support for multiple implementation methods (Babashka or nbb-logseq)
- Detailed progress reporting during extraction
- Error handling with helpful messages

## Implementation Approaches

We've developed two different implementations to extract and structure Logseq data:

### 1. Babashka Script (`extract_and_group_posted.clj`)

This approach uses a Babashka script that:
- Processes an initial query output from Logseq
- Makes shell calls to the `lq` script for each child block query
- Recursively builds the nested structure

**Usage:**
```bash
./extract_and_group_posted.clj <graph_name> <input_edn_file> <output_edn_file>
```

**Workflow:**
1. Run the Logseq query to find blocks with specific references: 
   ```bash
   ./query_ls/lq <graph_name> <query_file> > <output_file>
   ```
2. Process the results and fetch children:
   ```bash
   ./extract_and_group_posted.clj <graph_name> <output_from_step_1> <final_output_file>
   ```

### 2. Optimized nbb-logseq Implementation (`extract_recursive.cljs`)

This improved approach uses ClojureScript with nbb-logseq to:
- Load the Logseq graph database only once
- Run all queries within the same process
- Recursively fetch and structure blocks more efficiently

**Usage:**
```bash
./extract_recursive <graph_name> <query_file> <output_file>
```

**Advantages:**
- Much faster processing (no process spawning for each child query)
- Only loads the Logseq graph database once
- Better error handling with detailed messages
- Progress reporting during execution

## Query Format

Queries should be written in EDN format and follow the DataScript query syntax. Example:

```clojure
[:find (pull ?b [:db/id :block/content :block/uuid {:block/page [:block/original-name :db/id]}])
 :where
  [?r :block/name "pyq/posted"]
  [?b :block/refs ?r]]
```

## Output Format

The output is a vector of maps, where each map represents a block and its children:

```clojure
[{:content "Block content here",
  :uuid #uuid "668a36c7-89db-4779-8c42-52b5de011369",
  :page "page-name",
  :children [{:content "Child block content",
              :uuid #uuid "child-uuid",
              :page "page-name",
              :children [...]}]}
 {...}]
```

## Markdown Conversion

For better readability, you can convert the structured EDN output to markdown format using the included nbb script.

### Features

- **Hierarchical Structure**: Uses markdown headers (# ## ### ####) to show block hierarchy
- **Title Extraction**: Automatically generates meaningful titles from content 
- **Metadata Preservation**: Includes page names and UUIDs for reference
- **Content Escaping**: Properly escapes markdown special characters
- **Readable Formatting**: Indented children blocks for better readability

### Usage

```bash
# Convert EDN to readable markdown
nbb convert_to_markdown.cljs -- <input-edn-file> <output-markdown-file>

# Example
nbb convert_to_markdown.cljs -- query_ls/results/pyq_posted_optimized_output.edn pyq_analysis_readable.md
```

### Requirements

- Node.js
- nbb (install with `npm install -g nbb`)

### Output Example

The markdown conversion creates a hierarchical document with:
- Main title and introduction
- Block headers based on content or page name
- Metadata sections showing page and UUID
- Properly formatted content with escaped special characters
- Recursive children sections maintaining the original hierarchy

## Examples

### Finding blocks referencing `[[无限游戏]]`:

1. Create a query file:
```clojure
[:find (pull ?b [:db/id :block/content :block/uuid {:block/page [:block/original-name :db/id]}])
 :where
  [?r :block/name "无限游戏"]
  [?b :block/refs ?r]]
```

2. Run the extraction:
```bash
./extract_recursive yihan_main_LOGSEQ query_ls/queries/wuxian_game_query.edn query_ls/results/wuxian_game_output.edn
```

## Recommendations

- The nbb-logseq implementation (`extract_recursive`) is recommended for better performance and simpler workflow
- For very large graphs or complex queries, consider using more specific queries to limit the result set

## Dependencies

- Logseq graph database
- Babashka (for the Babashka implementation)
- nbb-logseq (for the optimized implementation)
- Node.js and nbb (for markdown conversion: `npm install -g nbb`)
