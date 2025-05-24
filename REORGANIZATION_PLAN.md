# Repository Reorganization Plan

## Current Problems
- Files scattered across root directory without clear organization
- Mixed source code, data files, documentation, and outputs
- Multiple similar analysis files with unclear differences
- Inconsistent naming conventions
- Hard to understand project workflow

## Proposed New Structure

```
pyq_analysis/
├── README.md                          # Main project documentation
├── docs/                              # All documentation
│   ├── CONVERSION_SUMMARY.md
│   ├── query_to_markdown_guide.md     # Renamed from README_query_to_markdown.md
│   └── workflow.md                    # New: comprehensive workflow guide
├── src/                               # Source code and scripts
│   ├── clojure/                       # ClojureScript source files
│   │   ├── extract_and_group_posted.clj
│   │   ├── extract_content.clj
│   │   ├── convert_to_markdown.cljs
│   │   └── logseq/                    # Logseq-specific modules
│   │       ├── extract_recursive.cljs
│   │       ├── extract_blocks_recursive.cljs
│   │       ├── extract_recursive_ordered.cljs
│   │       └── simple_query.cljs
│   ├── python/                        # Python analysis scripts
│   │   ├── parse_moments.py
│   │   ├── filter_long_moments.py
│   │   ├── analyze_json_schema.py
│   │   └── simple_schema_analyzer.py
│   └── shell/                         # Shell scripts
│       ├── query_to_markdown.sh
│       └── extract_recursive          # Main extraction script
├── data/                              # Raw and processed data
│   ├── raw/                           # Original data files
│   │   ├── pyq_backup.json           # Renamed from "pyq backup.json"
│   │   └── pyq_backup.js             # Archive if needed
│   ├── processed/                     # Intermediate processed data
│   │   ├── extracted_content.edn
│   │   ├── parsed_moments.json
│   │   ├── long_moments.json
│   │   └── schema_analysis.json
│   └── queries/                       # Query definitions and results
│       ├── definitions/               # Query definitions (.edn files)
│       │   ├── pyq_posted_references_query.edn
│       │   ├── pyq_extended_query.edn
│       │   ├── wuxian_game_query.edn
│       │   ├── schema_exploration.edn
│       │   ├── social_media_analysis.edn
│       │   └── block_attributes/      # Block attribute queries
│       │       ├── simple.edn
│       │       ├── limited.edn
│       │       └── sample.edn
│       └── results/                   # Query execution results
│           ├── pyq_posted/           # PyQ posted analysis results
│           │   ├── raw_output.edn
│           │   ├── optimized_output.edn
│           │   ├── final_ordered.edn
│           │   └── grouped_content.edn
│           ├── wuxian_game/          # Wuxian game analysis results
│           │   ├── raw_output.edn
│           │   ├── optimized_output.edn
│           │   └── grouped_content.edn
│           └── schema_exploration/   # Schema analysis results
│               ├── results.edn
│               └── ordered.edn
├── analysis/                          # Final analysis outputs
│   ├── reports/                       # Markdown analysis reports
│   │   ├── pyq_posted_analysis.md    # Consolidated from multiple similar files
│   │   ├── wuxian_game_analysis.md
│   │   ├── schema_exploration_analysis.md
│   │   ├── moments_summary.md
│   │   └── gemini_analysis.md        # Renamed from analysis_gemni_raw.md
│   └── summaries/                     # Quick summaries and overviews
├── tools/                             # Logseq query tools (reorganized from query_ls/)
│   ├── package.json
│   ├── package-lock.json
│   ├── deps.edn
│   ├── index.mjs
│   ├── lq                            # Query execution script
│   ├── example_query.edn
│   ├── ls_query_samples.md
│   └── README.md                     # Tool-specific documentation
└── archive/                          # Archived/backup files
    ├── extract_and_group_posted.clj.bak
    └── deprecated/                   # Old versions of files

```

## Migration Steps

### Phase 1: Create New Directory Structure
1. Create new directories: `docs/`, `src/`, `data/`, `analysis/`, `tools/`, `archive/`
2. Create subdirectories as outlined above

### Phase 2: Move and Rename Files

#### Documentation
- Move `CONVERSION_SUMMARY.md` → `docs/CONVERSION_SUMMARY.md`
- Move `README_query_to_markdown.md` → `docs/query_to_markdown_guide.md`

#### Source Code
- Move `extract_and_group_posted.clj` → `src/clojure/extract_and_group_posted.clj`
- Move `extract_content.clj` → `src/clojure/extract_content.clj`
- Move `convert_to_markdown.cljs` → `src/clojure/convert_to_markdown.cljs`
- Move `query_ls/src/*.cljs` → `src/clojure/logseq/`
- Move `parse_moments.py` → `src/python/parse_moments.py`
- Move `filter_long_moments.py` → `src/python/filter_long_moments.py`
- Move `pyqs/analyze_json_schema.py` → `src/python/analyze_json_schema.py`
- Move `pyqs/simple_schema_analyzer.py` → `src/python/simple_schema_analyzer.py`
- Move `query_to_markdown.sh` → `src/shell/query_to_markdown.sh`
- Move `extract_recursive` → `src/shell/extract_recursive`

#### Data Files
- Move `pyqs/pyq backup.json` → `data/raw/pyq_backup.json` (rename to remove spaces)
- Move `pyq backup.js` → `data/raw/pyq_backup.js`
- Move `extracted_content.edn` → `data/processed/extracted_content.edn`
- Move `parsed_moments.json` → `data/processed/parsed_moments.json`
- Move `long_moments.json` → `data/processed/long_moments.json`
- Move `pyqs/schema_analysis.json` → `data/processed/schema_analysis.json`

#### Query Organization
- Move `query_ls/queries/*.edn` → `data/queries/definitions/`
- Organize by category (pyq, wuxian_game, schema, block_attributes)
- Move `query_ls/results/*.edn` → `data/queries/results/`
- Organize by analysis type

#### Analysis Reports
- Consolidate similar analysis files into `analysis/reports/`
- Rename `analysis_gemni_raw.md` → `analysis/reports/gemini_analysis.md`

#### Tools
- Move `query_ls/` (excluding queries/ and results/) → `tools/`

#### Archive
- Move `extract_and_group_posted.clj.bak` → `archive/extract_and_group_posted.clj.bak`

### Phase 3: Update References
1. Update script paths in shell scripts
2. Update import paths in ClojureScript files
3. Update file references in documentation
4. Update README.md with new structure

### Phase 4: Clean Up
1. Remove duplicate/redundant files
2. Standardize naming conventions
3. Update .gitignore if needed

## Benefits of New Structure

1. **Clear Separation**: Source code, data, documentation, and outputs are clearly separated
2. **Logical Grouping**: Related files are grouped together (e.g., all Python scripts, all query definitions)
3. **Scalability**: Easy to add new analysis types or data sources
4. **Maintainability**: Easier to find and update specific components
5. **Professional**: Follows standard project organization patterns
6. **Documentation**: Clear docs/ directory for all documentation
7. **Data Management**: Organized data pipeline from raw → processed → results → analysis

## Notes

- The `tools/` directory maintains the Logseq query functionality as a separate module
- Raw data is preserved but organized
- Analysis outputs are clearly separated from intermediate processing files
- Archive directory preserves old files without cluttering the main structure
- Consistent naming conventions (underscores, no spaces in filenames) 