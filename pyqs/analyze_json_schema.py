#!/usr/bin/env python3
"""
JSON Schema Discovery Tool

This script analyzes a JSON file and discovers its schema structure,
including data types, nested objects, arrays, and field statistics.
"""

import json
import sys
from typing import Any, Dict, List, Set, Union
from collections import defaultdict, Counter
import argparse


class JSONSchemaAnalyzer:
    def __init__(self):
        self.schema = {}
        self.stats = defaultdict(Counter)
        self.field_examples = defaultdict(set)
        
    def analyze_value(self, value: Any, path: str = "root") -> Dict[str, Any]:
        """Analyze a value and return its schema information."""
        if value is None:
            return {"type": "null", "nullable": True}
        
        value_type = type(value).__name__
        
        if isinstance(value, bool):
            return {"type": "boolean"}
        elif isinstance(value, int):
            return {"type": "integer", "min": value, "max": value}
        elif isinstance(value, float):
            return {"type": "number", "min": value, "max": value}
        elif isinstance(value, str):
            # Store a few examples for each string field
            if len(self.field_examples[path]) < 5:
                self.field_examples[path].add(value[:100] if len(value) > 100 else value)
            return {
                "type": "string", 
                "min_length": len(value), 
                "max_length": len(value),
                "examples": list(self.field_examples[path])
            }
        elif isinstance(value, list):
            if not value:
                return {"type": "array", "items": {}, "min_items": 0, "max_items": 0}
            
            # Analyze all items in the array
            item_schemas = []
            for i, item in enumerate(value):
                item_schema = self.analyze_value(item, f"{path}[{i}]")
                item_schemas.append(item_schema)
            
            # Try to determine if all items have the same schema
            unique_schemas = self.merge_schemas(item_schemas)
            
            return {
                "type": "array",
                "items": unique_schemas,
                "min_items": len(value),
                "max_items": len(value)
            }
        elif isinstance(value, dict):
            properties = {}
            required = []
            
            for key, val in value.items():
                properties[key] = self.analyze_value(val, f"{path}.{key}")
                required.append(key)
            
            return {
                "type": "object",
                "properties": properties,
                "required": required
            }
        else:
            return {"type": "unknown", "python_type": value_type}
    
    def merge_schemas(self, schemas: List[Dict[str, Any]]) -> Dict[str, Any]:
        """Merge multiple schemas into one, handling variations."""
        if not schemas:
            return {}
        
        if len(schemas) == 1:
            return schemas[0]
        
        # Group schemas by type
        type_groups = defaultdict(list)
        for schema in schemas:
            schema_type = schema.get("type", "unknown")
            type_groups[schema_type].append(schema)
        
        if len(type_groups) == 1:
            # All items have the same type
            schema_type = list(type_groups.keys())[0]
            merged = {"type": schema_type}
            
            if schema_type == "string":
                all_examples = set()
                min_length = float('inf')
                max_length = 0
                for s in schemas:
                    if "examples" in s:
                        all_examples.update(s["examples"])
                    min_length = min(min_length, s.get("min_length", 0))
                    max_length = max(max_length, s.get("max_length", 0))
                merged.update({
                    "min_length": min_length if min_length != float('inf') else 0,
                    "max_length": max_length,
                    "examples": list(all_examples)[:10]  # Limit examples
                })
            elif schema_type in ["integer", "number"]:
                min_val = min(s.get("min", 0) for s in schemas)
                max_val = max(s.get("max", 0) for s in schemas)
                merged.update({"min": min_val, "max": max_val})
            elif schema_type == "array":
                min_items = min(s.get("min_items", 0) for s in schemas)
                max_items = max(s.get("max_items", 0) for s in schemas)
                # Merge item schemas
                all_item_schemas = []
                for s in schemas:
                    if "items" in s:
                        if isinstance(s["items"], list):
                            all_item_schemas.extend(s["items"])
                        else:
                            all_item_schemas.append(s["items"])
                merged_items = self.merge_schemas(all_item_schemas) if all_item_schemas else {}
                merged.update({
                    "min_items": min_items,
                    "max_items": max_items,
                    "items": merged_items
                })
            elif schema_type == "object":
                # Merge object properties
                all_properties = {}
                all_required = set()
                
                for s in schemas:
                    if "properties" in s:
                        for prop, prop_schema in s["properties"].items():
                            if prop in all_properties:
                                all_properties[prop] = self.merge_schemas([all_properties[prop], prop_schema])
                            else:
                                all_properties[prop] = prop_schema
                    
                    if "required" in s:
                        all_required.update(s["required"])
                
                merged.update({
                    "properties": all_properties,
                    "required": list(all_required)
                })
            
            return merged
        else:
            # Mixed types - create a union
            return {
                "type": "union",
                "types": list(type_groups.keys()),
                "schemas": {t: self.merge_schemas(schemas) for t, schemas in type_groups.items()}
            }
    
    def analyze_file(self, filepath: str) -> Dict[str, Any]:
        """Analyze a JSON file and return its schema."""
        try:
            with open(filepath, 'r', encoding='utf-8') as f:
                data = json.load(f)
            
            schema = self.analyze_value(data, "root")
            
            return {
                "file_info": {
                    "path": filepath,
                    "type": "JSON"
                },
                "schema": schema,
                "statistics": self.generate_statistics(data, schema)
            }
        
        except json.JSONDecodeError as e:
            return {"error": f"Invalid JSON: {e}"}
        except FileNotFoundError:
            return {"error": f"File not found: {filepath}"}
        except Exception as e:
            return {"error": f"Error analyzing file: {e}"}
    
    def generate_statistics(self, data: Any, schema: Dict[str, Any]) -> Dict[str, Any]:
        """Generate statistics about the data."""
        stats = {}
        
        def count_elements(obj, path=""):
            if isinstance(obj, dict):
                stats[f"{path}_object_count"] = stats.get(f"{path}_object_count", 0) + 1
                for key, value in obj.items():
                    count_elements(value, f"{path}.{key}" if path else key)
            elif isinstance(obj, list):
                stats[f"{path}_array_count"] = stats.get(f"{path}_array_count", 0) + 1
                stats[f"{path}_array_length"] = len(obj)
                for i, item in enumerate(obj):
                    count_elements(item, f"{path}[{i}]")
            elif isinstance(obj, str):
                stats[f"{path}_string_count"] = stats.get(f"{path}_string_count", 0) + 1
            elif isinstance(obj, (int, float)):
                stats[f"{path}_number_count"] = stats.get(f"{path}_number_count", 0) + 1
        
        count_elements(data)
        return stats
    
    def print_schema(self, schema_info: Dict[str, Any], max_depth: int = 10):
        """Print the schema in a readable format."""
        def print_schema_recursive(schema: Dict[str, Any], indent: int = 0, depth: int = 0):
            if depth > max_depth:
                print("  " * indent + "... (max depth reached)")
                return
            
            schema_type = schema.get("type", "unknown")
            indent_str = "  " * indent
            
            if schema_type == "object":
                print(f"{indent_str}Object:")
                properties = schema.get("properties", {})
                required = schema.get("required", [])
                
                for prop, prop_schema in properties.items():
                    required_marker = " (required)" if prop in required else " (optional)"
                    print(f"{indent_str}  {prop}{required_marker}:")
                    print_schema_recursive(prop_schema, indent + 2, depth + 1)
            
            elif schema_type == "array":
                min_items = schema.get("min_items", 0)
                max_items = schema.get("max_items", 0)
                print(f"{indent_str}Array[{min_items}-{max_items} items]:")
                items_schema = schema.get("items", {})
                if items_schema:
                    print_schema_recursive(items_schema, indent + 1, depth + 1)
            
            elif schema_type == "string":
                min_len = schema.get("min_length", 0)
                max_len = schema.get("max_length", 0)
                examples = schema.get("examples", [])
                print(f"{indent_str}String[{min_len}-{max_len} chars]")
                if examples:
                    print(f"{indent_str}  Examples: {examples[:3]}")
            
            elif schema_type in ["integer", "number"]:
                min_val = schema.get("min", "")
                max_val = schema.get("max", "")
                print(f"{indent_str}{schema_type.title()}[{min_val}-{max_val}]")
            
            elif schema_type == "union":
                types = schema.get("types", [])
                print(f"{indent_str}Union of: {', '.join(types)}")
                
            else:
                print(f"{indent_str}{schema_type.title()}")
        
        print("File Info:")
        file_info = schema_info.get("file_info", {})
        for key, value in file_info.items():
            print(f"  {key}: {value}")
        
        print("\nSchema:")
        schema = schema_info.get("schema", {})
        print_schema_recursive(schema)
        
        print("\nStatistics:")
        stats = schema_info.get("statistics", {})
        for key, value in sorted(stats.items()):
            print(f"  {key}: {value}")


def main():
    parser = argparse.ArgumentParser(description="Analyze JSON file schema")
    parser.add_argument("file", help="Path to JSON file to analyze")
    parser.add_argument("--max-depth", type=int, default=10, help="Maximum depth to analyze")
    parser.add_argument("--output", help="Output file for schema (optional)")
    
    args = parser.parse_args()
    
    analyzer = JSONSchemaAnalyzer()
    result = analyzer.analyze_file(args.file)
    
    if "error" in result:
        print(f"Error: {result['error']}")
        sys.exit(1)
    
    analyzer.print_schema(result, args.max_depth)
    
    if args.output:
        with open(args.output, 'w', encoding='utf-8') as f:
            json.dump(result, f, indent=2, ensure_ascii=False)
        print(f"\nDetailed schema saved to: {args.output}")


if __name__ == "__main__":
    main() 