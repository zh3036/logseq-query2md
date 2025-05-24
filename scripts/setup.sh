#!/bin/bash

# Setup script for logseq-query-to-markdown
set -e

echo "🚀 Setting up Logseq Query to Markdown..."

# Check if Node.js is installed
if ! command -v node &> /dev/null; then
    echo "❌ Node.js is required but not installed."
    echo "Please install Node.js from https://nodejs.org/"
    exit 1
fi

# Check Node.js version
NODE_VERSION=$(node -v | cut -d'v' -f2)
REQUIRED_VERSION="16.0.0"

if [ "$(printf '%s\n' "$REQUIRED_VERSION" "$NODE_VERSION" | sort -V | head -n1)" != "$REQUIRED_VERSION" ]; then
    echo "❌ Node.js version $REQUIRED_VERSION or higher is required. You have $NODE_VERSION"
    exit 1
fi

echo "✅ Node.js version: $NODE_VERSION"

# Install dependencies
echo "📦 Installing dependencies..."
npm install

# Make CLI executable
chmod +x bin/logseq-query.js

# Create global symlink if requested
if [ "$1" = "--global" ]; then
    echo "🔗 Creating global symlink..."
    npm link
    echo "✅ logseq-query command is now available globally!"
else
    echo "💡 To install globally, run: npm install -g logseq-query-to-markdown"
    echo "💡 Or run locally with: npx logseq-query"
fi

echo ""
echo "🎉 Setup complete!"
echo ""
echo "📖 Quick start:"
echo "  1. Initialize a project: logseq-query init"
echo "  2. Create a query: logseq-query create my-query"
echo "  3. Run a query: logseq-query run my-query"
echo ""
echo "📚 For more help: logseq-query --help" 