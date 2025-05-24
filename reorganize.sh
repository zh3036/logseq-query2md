#!/bin/bash

# Repository Reorganization Script
# This script reorganizes the pyq_analysis repository according to the reorganization plan

set -e  # Exit on any error

echo "Starting repository reorganization..."

# Phase 1: Create new directory structure
echo "Creating new directory structure..."

mkdir -p docs
mkdir -p src/{clojure/logseq,python,shell}
mkdir -p data/{raw,processed,queries/{definitions/block_attributes,results/{pyq_posted,wuxian_game,schema_exploration}}}
mkdir -p analysis/{reports,summaries}
mkdir -p tools
mkdir -p archive/deprecated

# Phase 2: Move and rename files

echo "Moving documentation files..."
[ -f "CONVERSION_SUMMARY.md" ] && mv "CONVERSION_SUMMARY.md" docs/
[ -f "README_query_to_markdown.md" ] && mv "README_query_to_markdown.md" docs/query_to_markdown_guide.md

echo "Moving source code files..."
[ -f "extract_and_group_posted.clj" ] && mv "extract_and_group_posted.clj" src/clojure/
[ -f "extract_content.clj" ] && mv "extract_content.clj" src/clojure/
[ -f "convert_to_markdown.cljs" ] && mv "convert_to_markdown.cljs" src/clojure/

# Move ClojureScript files from query_ls/src/
if [ -d "query_ls/src" ]; then
    mv query_ls/src/*.cljs src/clojure/logseq/ 2>/dev/null || true
fi

# Move Python files
[ -f "parse_moments.py" ] && mv "parse_moments.py" src/python/
[ -f "filter_long_moments.py" ] && mv "filter_long_moments.py" src/python/
[ -f "pyqs/analyze_json_schema.py" ] && mv "pyqs/analyze_json_schema.py" src/python/
[ -f "pyqs/simple_schema_analyzer.py" ] && mv "pyqs/simple_schema_analyzer.py" src/python/

# Move shell scripts and preserve executable permissions
[ -f "query_to_markdown.sh" ] && mv "query_to_markdown.sh" src/shell/ && chmod +x src/shell/query_to_markdown.sh
[ -f "extract_recursive" ] && mv "extract_recursive" src/shell/ && chmod +x src/shell/extract_recursive

echo "Moving data files..."
# Move raw data files (handle spaces in filenames)
[ -f "pyqs/pyq backup.json" ] && mv "pyqs/pyq backup.json" data/raw/pyq_backup.json
[ -f "pyq backup.js" ] && mv "pyq backup.js" data/raw/

# Move processed data files
[ -f "extracted_content.edn" ] && mv "extracted_content.edn" data/processed/
[ -f "parsed_moments.json" ] && mv "parsed_moments.json" data/processed/
[ -f "long_moments.json" ] && mv "long_moments.json" data/processed/
[ -f "pyqs/schema_analysis.json" ] && mv "pyqs/schema_analysis.json" data/processed/

echo "Organizing queries and results..."
# Move query definitions
if [ -d "query_ls/queries" ]; then
    # Move block attribute queries
    mv query_ls/queries/block_attributes_*.edn data/queries/definitions/block_attributes/ 2>/dev/null || true
    
    # Move other query definitions
    mv query_ls/queries/*.edn data/queries/definitions/ 2>/dev/null || true
    
    # Note: Some .edn files in queries/ are actually results, we'll handle them separately
    mv data/queries/definitions/pyq_references_result.edn data/queries/results/pyq_posted/ 2>/dev/null || true
fi

# Move query results and organize by type
if [ -d "query_ls/results" ]; then
    # PyQ posted results
    mv query_ls/results/pyq_posted_*.edn data/queries/results/pyq_posted/ 2>/dev/null || true
    mv query_ls/results/pyq_content_only.edn data/queries/results/pyq_posted/ 2>/dev/null || true
    mv query_ls/results/pyq_extended_query_result.edn data/queries/results/pyq_posted/ 2>/dev/null || true
    
    # Wuxian game results
    mv query_ls/results/wuxian_game_*.edn data/queries/results/wuxian_game/ 2>/dev/null || true
    
    # Schema exploration results
    mv query_ls/results/schema_exploration*.edn data/queries/results/schema_exploration/ 2>/dev/null || true
    
    # Block attributes results
    mv query_ls/results/block_attributes_*.edn data/queries/results/schema_exploration/ 2>/dev/null || true
fi

echo "Moving analysis reports..."
[ -f "pyq_analysis_readable.md" ] && mv "pyq_analysis_readable.md" analysis/reports/pyq_posted_analysis.md
[ -f "pyq_posted_ordered_analysis.md" ] && mv "pyq_posted_ordered_analysis.md" analysis/reports/
[ -f "wuxian_game_analysis.md" ] && mv "wuxian_game_analysis.md" analysis/reports/
[ -f "schema_exploration_analysis.md" ] && mv "schema_exploration_analysis.md" analysis/reports/
[ -f "moments_summary.md" ] && mv "moments_summary.md" analysis/reports/
[ -f "analysis_gemni_raw.md" ] && mv "analysis_gemni_raw.md" analysis/reports/gemini_analysis.md
[ -f "analysis.md" ] && mv "analysis.md" analysis/reports/

echo "Moving tools..."
# Move query_ls tools (excluding already moved directories)
if [ -d "query_ls" ]; then
    # Copy remaining files to tools/
    [ -f "query_ls/package.json" ] && mv "query_ls/package.json" tools/
    [ -f "query_ls/package-lock.json" ] && mv "query_ls/package-lock.json" tools/
    [ -f "query_ls/deps.edn" ] && mv "query_ls/deps.edn" tools/
    [ -f "query_ls/index.mjs" ] && mv "query_ls/index.mjs" tools/ && chmod +x tools/index.mjs
    [ -f "query_ls/lq" ] && mv "query_ls/lq" tools/ && chmod +x tools/lq
    [ -f "query_ls/example_query.edn" ] && mv "query_ls/example_query.edn" tools/
    [ -f "query_ls/ls_query_samples.md" ] && mv "query_ls/ls_query_samples.md" tools/
    [ -f "query_ls/README.md" ] && mv "query_ls/README.md" tools/
    
    # Move node_modules if it exists
    [ -d "query_ls/node_modules" ] && mv "query_ls/node_modules" tools/
fi

echo "Moving archive files..."
[ -f "extract_and_group_posted.clj.bak" ] && mv "extract_and_group_posted.clj.bak" archive/

# Phase 3: Update script permissions and replace with updated versions
echo "Updating scripts with new paths..."

# The updated scripts are already created in the new locations, so we need to overwrite the moved ones
# Scripts that were moved will be replaced by the updated versions

echo "Cleaning up empty directories..."
# Remove empty directories
rmdir query_ls/queries 2>/dev/null || true
rmdir query_ls/results 2>/dev/null || true
rmdir query_ls/src 2>/dev/null || true
rmdir query_ls 2>/dev/null || true
rmdir pyqs 2>/dev/null || true

echo "Repository reorganization completed!"
echo ""
echo "✅ New structure created with updated scripts."
echo ""
echo "📁 Key directories:"
echo "   src/shell/         - Updated executable scripts"
echo "   data/queries/      - Query definitions and results"  
echo "   analysis/reports/  - Final analysis outputs"
echo "   tools/             - Logseq query tools"
echo "   docs/              - All documentation"
echo ""
echo "🚀 Updated scripts ready to use:"
echo "   ./src/shell/query_to_markdown.sh data/queries/definitions/[query].edn"
echo "   ./src/shell/extract_recursive [args]"
echo "   ./tools/lq [args]"
echo ""
echo "📖 Next steps:"
echo "1. Test the updated scripts work correctly"
echo "2. Update any additional references in other files"
echo "3. Remove any remaining duplicate files"
echo ""
echo "See REORGANIZATION_PLAN.md for detailed information about the new structure." 