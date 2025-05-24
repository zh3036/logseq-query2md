# EDN to Markdown Conversion Summary

## Overview

Successfully used [nbb](https://github.com/babashka/nbb) to convert the Logseq EDN data (`pyq_posted_optimized_output.edn`) to a readable markdown format with proper hierarchy.

## What was accomplished

1. **Created an nbb script** (`convert_to_markdown.cljs`) that:
   - Reads EDN data from Logseq block extraction
   - Generates a table of contents from top-level blocks
   - Converts top-level blocks to markdown headers
   - Converts child blocks to nested markdown lists
   - Escapes markdown special characters
   - Extracts meaningful titles from content

2. **Generated structured output** (`pyq_analysis_readable.md`) with:
   - 156 lines of organized content
   - Table of contents with clickable navigation
   - Top-level headers for main blocks  
   - Nested lists for child content
   - Clean formatting
   - Hierarchical structure showing parent-child relationships

## Key Features of the Conversion

- **Table of Contents**: Automatically generated from top-level blocks with clickable links
- **Hierarchical Structure**: Top-level blocks become markdown headers, children become nested lists  
- **Title Extraction**: Automatically generates titles from content (first 50 characters)
- **Content Escaping**: Properly escapes markdown special characters
- **Nested Lists**: Child blocks use markdown lists with alternating bullet styles (- and *)

## Usage

```bash
# Run the conversion script
nbb convert_to_markdown.cljs -- input.edn output.md

# Example
nbb convert_to_markdown.cljs -- query_ls/results/pyq_posted_optimized_output.edn pyq_analysis_readable.md
```

## Input Data Structure

The input EDN contains Logseq blocks with:
- `:content` - The block text content
- `:uuid` - Unique identifier
- `:page` - Source page name
- `:children` - Nested child blocks (recursive structure)

## Output Structure

The markdown output includes:
- Main title: "PyQ Posted Content Analysis"
- Table of contents with numbered entries and anchor links
- Top-level block headers (# Block Title {#anchor})
- Child blocks as nested markdown lists
- Escaped content with proper formatting

## Technologies Used

- **nbb**: Node.js ClojureScript scripting environment
- **ClojureScript**: For data processing and transformation
- **Node.js fs module**: For file I/O operations
- **EDN**: Clojure data format for input
- **Markdown**: Human-readable output format

## Files Created

1. `convert_to_markdown.cljs` - The nbb conversion script
2. `pyq_analysis_readable.md` - The final readable output
3. `CONVERSION_SUMMARY.md` - This summary document

The conversion successfully transforms the structured Logseq data into a readable format while preserving the hierarchical relationships from the original blocks. 