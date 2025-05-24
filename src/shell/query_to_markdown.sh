#!/bin/bash

# Query to Markdown Pipeline
# Usage: ./query_to_markdown.sh <query_file> [output_name]
# Example: ./query_to_markdown.sh query_ls/queries/pyq_posted_references_query.edn pyq_analysis

set -e  # Exit on any error

# Get the directory where this script is located and set project root
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# Check if query file is provided
if [ $# -lt 1 ]; then
    echo "Usage: $0 <query_file> [output_name]"
    echo "Example: $0 data/queries/definitions/pyq_posted_references_query.edn pyq_analysis"
    echo ""
    echo "This script will:"
    echo "1. Run the ordered extraction on the query"
    echo "2. Convert the results to markdown"
    echo ""
    echo "Available queries:"
    find "$PROJECT_ROOT/data/queries/definitions" -name "*.edn" -type f 2>/dev/null | sed "s|$PROJECT_ROOT/||" | sed 's/^/  /' || echo "  No queries found (run after reorganization)"
    exit 1
fi

QUERY_FILE="$1"
GRAPH_NAME="yihan_main_LOGSEQ"  # Default graph name

# Convert relative paths to absolute if needed
if [[ ! "$QUERY_FILE" = /* ]]; then
    QUERY_FILE="$PROJECT_ROOT/$QUERY_FILE"
fi

# Check if query file exists
if [ ! -f "$QUERY_FILE" ]; then
    echo "Error: Query file '$QUERY_FILE' not found!"
    exit 1
fi

# Generate output names based on query file or provided name
if [ $# -ge 2 ]; then
    OUTPUT_NAME="$2"
else
    # Extract name from query file (remove path and .edn extension)
    OUTPUT_NAME=$(basename "$QUERY_FILE" .edn)
fi

# Define intermediate and final file paths (updated for new structure)
EDN_OUTPUT="$PROJECT_ROOT/data/queries/results/$(basename "$OUTPUT_NAME")_ordered.edn"
MD_OUTPUT="$PROJECT_ROOT/analysis/reports/${OUTPUT_NAME}_analysis.md"

echo "🚀 Starting Query to Markdown Pipeline"
echo "📋 Query file: $QUERY_FILE"
echo "🎯 Graph: $GRAPH_NAME"
echo "📄 EDN output: $EDN_OUTPUT"
echo "📝 Markdown output: $MD_OUTPUT"
echo ""

# Step 1: Run ordered extraction
echo "⚙️  Step 1: Running ordered extraction..."
cd "$PROJECT_ROOT/tools"

# Create results directory if it doesn't exist
mkdir -p "$(dirname "$EDN_OUTPUT")"

npx @logseq/nbb-logseq "../src/clojure/logseq/extract_recursive_ordered.cljs" "$GRAPH_NAME" "$QUERY_FILE" "$EDN_OUTPUT"

# Check if extraction was successful
if [ ! -f "$EDN_OUTPUT" ]; then
    echo "❌ Error: Extraction failed - output file not created"
    exit 1
fi

echo "✅ Extraction completed successfully"

# Step 2: Convert to markdown
echo "⚙️  Step 2: Converting to markdown..."
cd "$PROJECT_ROOT"

# Create analysis directory if it doesn't exist
mkdir -p "$(dirname "$MD_OUTPUT")"

npx nbb src/clojure/convert_to_markdown.cljs -- "$EDN_OUTPUT" "$MD_OUTPUT"

# Check if conversion was successful
if [ ! -f "$MD_OUTPUT" ]; then
    echo "❌ Error: Markdown conversion failed - output file not created"
    exit 1
fi

echo "✅ Markdown conversion completed successfully"
echo ""
echo "🎉 Pipeline completed!"
echo "📊 Results:"
echo "  📄 EDN file: $EDN_OUTPUT"
echo "  📝 Markdown file: $MD_OUTPUT"
echo ""
echo "📖 View the markdown file:"
echo "  cat '$MD_OUTPUT'"
echo "  or open it in your preferred editor" 