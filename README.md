# Query to Markdown Pipeline

This script automates the process of extracting data from Logseq using DataScript queries and converting the results to well-formatted markdown.

## Features

✅ **Ordered Extraction**: Uses the enhanced `extract_recursive_ordered.cljs` to maintain proper block ordering  
✅ **Automatic Conversion**: Converts results to markdown with table of contents and hierarchical structure  
✅ **Error Handling**: Graceful error handling with informative messages  
✅ **Flexible Naming**: Auto-generates output names or uses custom names  
✅ **Help System**: Shows usage and lists available queries  

## Usage

```bash
./query_to_markdown.sh <query_file> [output_name]
```

### Examples

```bash
# Use automatic naming (creates pyq_posted_references_query_analysis.md)
./query_to_markdown.sh query_ls/queries/pyq_posted_references_query.edn

# Use custom name (creates my_analysis.md)
./query_to_markdown.sh query_ls/queries/pyq_posted_references_query.edn my_analysis

# Show help and available queries
./query_to_markdown.sh
```

## Output Files

The script creates two files:

1. **EDN file**: `query_ls/results/{name}_ordered.edn` - Raw extracted data with proper ordering
2. **Markdown file**: `{name}_analysis.md` - Human-readable formatted analysis

## Pipeline Steps

1. **🔍 Ordered Extraction**: Runs DataScript query with proper block ordering
2. **📝 Markdown Conversion**: Converts hierarchical data to formatted markdown
3. **✅ Validation**: Checks that both steps completed successfully

## Features of Generated Markdown

- **Table of Contents** with clickable anchors
- **Hierarchical bullet lists** preserving parent-child relationships  
- **Proper markdown escaping** of special characters
- **Clean formatting** with appropriate spacing
- **Numbered sequences** exactly as they appear in Logseq

## Requirements

- `nbb-logseq` (for running ClojureScript queries)
- `nbb` (for markdown conversion)
- Logseq graph accessible at the default location

## Error Handling

The script will:
- Check if the query file exists
- Validate successful extraction
- Validate successful markdown conversion
- Provide informative error messages
- Exit with appropriate codes

## Example Output Structure

```markdown
# PyQ Posted Content Analysis

## Table of Contents
1. [Block Title](#block-1)
2. [Another Block](#block-2)

---

# Block Title {#block-1}
Content here...
- Child block 1
- Child block 2
  * Nested child

# Another Block {#block-2}
More content...
```

Happy querying! 🚀 