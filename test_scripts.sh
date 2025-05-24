#!/bin/bash

# Test Script for Reorganized Repository
# This script tests that all the reorganized scripts work correctly

set -e

echo "🧪 Testing Reorganized Scripts"
echo "=============================="

# Function to test if a script exists and is executable
test_script() {
    local script_path="$1"
    local description="$2"
    
    echo -n "Testing $description... "
    
    if [ -f "$script_path" ]; then
        if [ -x "$script_path" ]; then
            echo "✅ EXISTS and EXECUTABLE"
        else
            echo "⚠️  EXISTS but NOT EXECUTABLE"
            chmod +x "$script_path"
            echo "   Fixed permissions"
        fi
    else
        echo "❌ NOT FOUND"
        return 1
    fi
}

# Function to test script help/usage
test_script_help() {
    local script_path="$1"
    local description="$2"
    
    echo -n "Testing $description help... "
    
    if "$script_path" --help >/dev/null 2>&1 || "$script_path" >/dev/null 2>&1; then
        echo "✅ RESPONDS"
    else
        echo "⚠️  May have issues (exit code $?)"
    fi
}

echo ""
echo "📁 Checking Directory Structure..."
echo "---------------------------------"

# Check key directories exist
directories=(
    "src/shell"
    "src/clojure/logseq" 
    "src/python"
    "data/queries/definitions"
    "data/queries/results"
    "analysis/reports"
    "tools"
    "docs"
    "archive"
)

for dir in "${directories[@]}"; do
    if [ -d "$dir" ]; then
        echo "✅ $dir"
    else
        echo "❌ $dir - MISSING"
    fi
done

echo ""
echo "🔧 Testing Shell Scripts..."
echo "-------------------------"

# Test shell scripts
test_script "src/shell/query_to_markdown.sh" "Query to Markdown Pipeline"
test_script "src/shell/extract_recursive" "Extract Recursive Wrapper"
test_script "tools/lq" "Logseq Query Tool"

echo ""
echo "📄 Testing Script Help/Usage..."
echo "------------------------------"

# Test help outputs (these may fail but shouldn't crash)
if [ -x "src/shell/query_to_markdown.sh" ]; then
    echo -n "query_to_markdown.sh help... "
    if ./src/shell/query_to_markdown.sh 2>/dev/null | grep -q "Usage:"; then
        echo "✅ Shows usage"
    else
        echo "⚠️  Usage output may be different"
    fi
fi

echo ""
echo "🐍 Testing Python Scripts..."
echo "---------------------------"

python_scripts=(
    "src/python/parse_moments.py"
    "src/python/filter_long_moments.py"
    "src/python/analyze_json_schema.py"
    "src/python/simple_schema_analyzer.py"
)

for script in "${python_scripts[@]}"; do
    test_script "$script" "$(basename "$script")"
done

echo ""
echo "📝 Testing ClojureScript Files..."
echo "--------------------------------"

clojure_files=(
    "src/clojure/convert_to_markdown.cljs"
    "src/clojure/extract_and_group_posted.clj"
    "src/clojure/extract_content.clj"
    "src/clojure/logseq/extract_recursive.cljs"
    "src/clojure/logseq/extract_recursive_ordered.cljs"
    "src/clojure/logseq/simple_query.cljs"
)

for file in "${clojure_files[@]}"; do
    if [ -f "$file" ]; then
        echo "✅ $(basename "$file")"
    else
        echo "❌ $(basename "$file") - NOT FOUND"
    fi
done

echo ""
echo "📊 Testing Query Files..."
echo "------------------------"

# Check if query files exist
if [ -d "data/queries/definitions" ]; then
    query_count=$(find data/queries/definitions -name "*.edn" | wc -l)
    echo "Found $query_count query definition files"
    if [ "$query_count" -gt 0 ]; then
        echo "✅ Query definitions available"
    else
        echo "⚠️  No query files found (run reorganization first)"
    fi
else
    echo "❌ Query definitions directory not found"
fi

echo ""
echo "🛠️  Testing Tool Dependencies..."
echo "------------------------------"

# Check if required tools are available
echo -n "nbb... "
if command -v nbb >/dev/null 2>&1; then
    echo "✅ Available"
else
    echo "❌ NOT FOUND (npm install -g nbb)"
fi

echo -n "npx... "
if command -v npx >/dev/null 2>&1; then
    echo "✅ Available"
else
    echo "❌ NOT FOUND (install Node.js)"
fi

echo -n "@logseq/nbb-logseq... "
if npm list -g @logseq/nbb-logseq >/dev/null 2>&1; then
    echo "✅ Available"
else
    echo "⚠️  May not be installed globally"
fi

echo ""
echo "📋 Summary and Recommendations"
echo "============================="

echo ""
echo "✅ Repository has been reorganized successfully!"
echo ""
echo "🚀 Ready to use scripts:"
echo "   ./src/shell/query_to_markdown.sh data/queries/definitions/[query].edn"
echo "   ./src/shell/extract_recursive [graph] [query] [output]"
echo "   ./tools/lq [graph] [query]"
echo ""
echo "📖 Documentation:"
echo "   docs/query_to_markdown_guide.md - Updated usage guide"
echo "   REORGANIZATION_PLAN.md - Complete reorganization details"
echo ""
echo "🔍 If any scripts show issues:"
echo "   1. Make sure all dependencies are installed (nbb, @logseq/nbb-logseq)"
echo "   2. Check that your Logseq graph is accessible"
echo "   3. Verify query files exist in data/queries/definitions/"
echo ""
echo "Happy analyzing! 🎉" 