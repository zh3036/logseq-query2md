# Query to Markdown Pipeline

This script automates the process of extracting data from Logseq using DataScript queries and converting the results to well-formatted markdown.

## Features

✅ **Ordered Extraction**: Uses the enhanced `extract_recursive_ordered.cljs` to maintain proper block ordering  
✅ **Automatic Conversion**: Converts results to markdown with table of contents and hierarchical structure  
✅ **Error Handling**: Graceful error handling with informative messages  
✅ **Flexible Naming**: Auto-generates output names or uses custom names  
✅ **Help System**: Shows usage and lists available queries  
✅ **Updated Paths**: Works with the reorganized directory structure

## Usage

```bash
./src/shell/query_to_markdown.sh <query_file> [output_name]
```

### Examples

```bash
# Use automatic naming (creates pyq_posted_references_query_analysis.md in analysis/reports/)
./src/shell/query_to_markdown.sh data/queries/definitions/pyq_posted_references_query.edn

# Use custom name (creates my_analysis.md in analysis/reports/)
./src/shell/query_to_markdown.sh data/queries/definitions/pyq_posted_references_query.edn my_analysis

# Show help and available queries
./src/shell/query_to_markdown.sh
```

## Output Files

The script creates two files in the organized directory structure:

1. **EDN file**: `data/queries/results/{name}_ordered.edn` - Raw extracted data with proper ordering
2. **Markdown file**: `analysis/reports/{name}_analysis.md` - Human-readable formatted analysis

## Pipeline Steps

1. **🔍 Ordered Extraction**: Runs DataScript query with proper block ordering from `tools/` directory
2. **📝 Markdown Conversion**: Converts hierarchical data to formatted markdown using `src/clojure/convert_to_markdown.cljs`
3. **✅ Validation**: Checks that both steps completed successfully

## Directory Structure Integration

The script works seamlessly with the reorganized structure:

- **Queries**: Located in `data/queries/definitions/`
- **Results**: Stored in `data/queries/results/`
- **Analysis**: Final markdown reports in `analysis/reports/`
- **Source**: ClojureScript modules in `src/clojure/logseq/`
- **Tools**: Logseq tools and dependencies in `tools/`

## Features of Generated Markdown

- **Table of Contents** with clickable anchors
- **Hierarchical bullet lists** preserving parent-child relationships  
- **Proper markdown escaping** of special characters
- **Clean formatting** with appropriate spacing
- **Numbered sequences** exactly as they appear in Logseq

## Requirements

- `@logseq/nbb-logseq` (for running ClojureScript queries)
- `nbb` (for markdown conversion)
- Logseq graph accessible at the default location
- Proper directory structure after reorganization

## Error Handling

The script will:
- Check if the query file exists
- Validate successful extraction
- Validate successful markdown conversion
- Provide informative error messages
- Exit with appropriate codes
- Create necessary directories automatically

## Migration from Old Structure

If you're updating from the old structure:

**Old usage:**
```bash
./query_to_markdown.sh query_ls/queries/pyq_posted_references_query.edn
```

**New usage:**
```bash
./src/shell/query_to_markdown.sh data/queries/definitions/pyq_posted_references_query.edn
```

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

## Other Extraction Tools

For direct extraction without markdown conversion:

```bash
# Use the extract_recursive wrapper
./src/shell/extract_recursive GRAPH_NAME QUERY_FILE OUTPUT_FILE

# Use the lq tool for simple queries
./tools/lq GRAPH_NAME QUERY_FILE
```

Happy querying! 🚀 