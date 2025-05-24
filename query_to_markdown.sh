#!/bin/bash

# Query to Markdown Pipeline
# Usage: ./query_to_markdown.sh <query_file> [output_name]
# Example: ./query_to_markdown.sh query_ls/queries/pyq_posted_references_query.edn pyq_analysis

set -e  # Exit on any error

# Check if query file is provided
if [ $# -lt 1 ]; then
    echo "Usage: $0 <query_file> [output_name]"
    echo "Example: $0 query_ls/queries/pyq_posted_references_query.edn pyq_analysis"
    echo ""
    echo "This script will:"
    echo "1. Run the ordered extraction on the query"
    echo "2. Convert the results to markdown"
    echo ""
    echo "Available queries:"
    find query_ls/queries -name "*.edn" -type f | sed 's/^/  /'
    exit 1
fi

QUERY_FILE="$1"
GRAPH_NAME="yihan_main_LOGSEQ"  # Default graph name

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

# Define intermediate and final file paths
EDN_OUTPUT="query_ls/results/${OUTPUT_NAME}_ordered.edn"
MD_OUTPUT="${OUTPUT_NAME}_analysis.md"

echo "🚀 Starting Query to Markdown Pipeline"
echo "📋 Query file: $QUERY_FILE"
echo "🎯 Graph: $GRAPH_NAME"
echo "📄 EDN output: $EDN_OUTPUT"
echo "📝 Markdown output: $MD_OUTPUT"
echo ""

# Step 1: Run ordered extraction
echo "⚙️  Step 1: Running ordered extraction..."
cd query_ls
npx nbb-logseq src/extract_recursive_ordered.cljs "$GRAPH_NAME" "../$QUERY_FILE" "results/$(basename "$EDN_OUTPUT")"

# Check if extraction was successful
if [ ! -f "results/$(basename "$EDN_OUTPUT")" ]; then
    echo "❌ Error: Extraction failed - output file not created"
    exit 1
fi

echo "✅ Extraction completed successfully"

# Step 2: Convert to markdown
echo "⚙️  Step 2: Converting to markdown..."
cd ..
nbb convert_to_markdown.cljs -- "$EDN_OUTPUT" "$MD_OUTPUT"

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