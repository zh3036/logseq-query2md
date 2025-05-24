# AI Assistant Guide: Logseq Query to Markdown System

This guide explains how to use the lq2md (Logseq to Markdown) system for extracting and analyzing Logseq data.

## Quick Command Reference

### Running Queries
```bash
# From project root, run:
src/shell/query_to_markdown.sh <query_file> [output_name]

# Examples:
src/shell/query_to_markdown.sh data/queries/definitions/my_query.edn
src/shell/query_to_markdown.sh data/queries/definitions/my_query.edn custom_analysis_name
```

### Getting Help
```bash
# Show usage and list available queries:
src/shell/query_to_markdown.sh
```

## File Locations

### Input Files (Queries)
- **Location**: `data/queries/definitions/`
- **Format**: `.edn` files containing DataScript queries
- **Example**: `data/queries/definitions/pyq_posted_references_query.edn`

### Output Files
- **EDN Results**: `data/queries/results/{name}_ordered.edn`
- **Markdown Reports**: `analysis/reports/{name}_analysis.md`

### Existing Query Examples
Check `data/queries/definitions/` for existing query templates you can reference or modify.

## Writing DataScript Queries

### Basic Query Structure
```clojure
;; File: data/queries/definitions/my_query.edn
{:query
 [:find (pull ?b [*])
  :where
  [?b :block/content ?content]
  ;; Add your conditions here
  ]
}
```

### Common Query Patterns

#### Find blocks with specific content
```clojure
{:query
 [:find (pull ?b [*])
  :where
  [?b :block/content ?content]
  [(clojure.string/includes? ?content "your-search-term")]
]}
```

#### Find blocks with specific tags
```clojure
{:query
 [:find (pull ?b [*])
  :where
  [?b :block/refs ?ref]
  [?ref :block/name "your-tag"]
]}
```

#### Find pages with specific properties
```clojure
{:query
 [:find (pull ?p [*])
  :where
  [?p :block/name ?name]
  [?p :block/properties ?props]
  [(get ?props :your-property) ?value]
]}
```

## What the System Does

1. **Extracts**: Runs your DataScript query against the Logseq database
2. **Orders**: Maintains proper hierarchical block ordering (parent-child relationships)
3. **Converts**: Transforms the raw data into readable markdown with:
   - Table of contents
   - Hierarchical bullet lists
   - Proper markdown formatting
   - Clickable anchors

## Reading Results

### EDN Files
Raw extracted data with complete block information. Useful for:
- Debugging queries
- Further programmatic processing
- Understanding the data structure

### Markdown Files
Human-readable analysis reports with:
- Automatic table of contents
- Hierarchical structure preserved
- Clean formatting
- Easy sharing and viewing

## Tips for AI Assistants

1. **Always check existing queries first**: Look in `data/queries/definitions/` for similar patterns
2. **Use descriptive output names**: This helps organize results in `analysis/reports/`
3. **Test queries incrementally**: Start simple, then add complexity
4. **Check both outputs**: EDN for debugging, Markdown for human review
5. **Follow DataScript syntax**: This is Datomic-style query language, not SQL

## Common Use Cases

- **Content Analysis**: Find all blocks containing specific keywords
- **Tag Exploration**: Extract all content related to specific tags
- **Property Queries**: Find pages/blocks with specific properties
- **Relationship Mapping**: Discover connections between different concepts
- **Content Organization**: Extract structured data for reports

## Error Troubleshooting

- **Query file not found**: Check the path in `data/queries/definitions/`
- **Empty results**: Verify your query syntax and that matching data exists
- **Missing output**: Check permissions and that directories exist
- **Syntax errors**: Validate your EDN query structure

## Example Workflow

1. Create query file: `data/queries/definitions/my_analysis.edn`
2. Run extraction: `src/shell/query_to_markdown.sh data/queries/definitions/my_analysis.edn`
3. Check results:
   - EDN: `data/queries/results/my_analysis_ordered.edn`
   - Report: `analysis/reports/my_analysis_analysis.md`
4. Iterate and refine as needed

Remember: The system preserves the exact hierarchical structure from Logseq, making it perfect for maintaining context and relationships in your extracted data. 