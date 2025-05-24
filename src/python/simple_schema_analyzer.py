#!/usr/bin/env python3
"""
Simple JSON Schema Analyzer

This script provides a high-level overview of JSON structure
without getting into deep recursive analysis.
"""

import json
import sys
from typing import Any, Dict, List, Set
from collections import defaultdict, Counter


def analyze_structure(data: Any, path: str = "", max_depth: int = 3, current_depth: int = 0) -> Dict[str, Any]:
    """Analyze JSON structure at a high level."""
    
    if current_depth > max_depth:
        return {"type": "truncated", "note": f"Max depth {max_depth} reached"}
    
    if data is None:
        return {"type": "null"}
    
    if isinstance(data, bool):
        return {"type": "boolean"}
    
    elif isinstance(data, int):
        return {"type": "integer", "sample_value": data}
    
    elif isinstance(data, float):
        return {"type": "number", "sample_value": data}
    
    elif isinstance(data, str):
        return {
            "type": "string", 
            "length": len(data),
            "sample": data[:50] + "..." if len(data) > 50 else data
        }
    
    elif isinstance(data, list):
        if not data:
            return {"type": "array", "length": 0, "items": {}}
        
        # Analyze first few items to understand structure
        item_types = Counter()
        sample_items = []
        
        for i, item in enumerate(data[:5]):  # Only analyze first 5 items
            item_schema = analyze_structure(item, f"{path}[{i}]", max_depth, current_depth + 1)
            item_types[item_schema.get("type", "unknown")] += 1
            if i < 3:  # Keep sample of first 3 items
                sample_items.append(item_schema)
        
        return {
            "type": "array",
            "length": len(data),
            "item_types": dict(item_types),
            "sample_items": sample_items
        }
    
    elif isinstance(data, dict):
        properties = {}
        for key, value in data.items():
            properties[key] = analyze_structure(value, f"{path}.{key}", max_depth, current_depth + 1)
        
        return {
            "type": "object",
            "num_properties": len(properties),
            "properties": properties
        }
    
    else:
        return {"type": "unknown", "python_type": type(data).__name__}


def get_summary_stats(data: Any) -> Dict[str, Any]:
    """Get high-level statistics about the data."""
    stats = {
        "total_objects": 0,
        "total_arrays": 0,
        "total_strings": 0,
        "total_numbers": 0,
        "max_string_length": 0,
        "total_size_estimate": 0
    }
    
    def count_recursive(obj, depth=0):
        if depth > 10:  # Prevent infinite recursion
            return
        
        if isinstance(obj, dict):
            stats["total_objects"] += 1
            for value in obj.values():
                count_recursive(value, depth + 1)
        elif isinstance(obj, list):
            stats["total_arrays"] += 1
            for item in obj[:100]:  # Only count first 100 items
                count_recursive(item, depth + 1)
        elif isinstance(obj, str):
            stats["total_strings"] += 1
            stats["max_string_length"] = max(stats["max_string_length"], len(obj))
            stats["total_size_estimate"] += len(obj)
        elif isinstance(obj, (int, float)):
            stats["total_numbers"] += 1
    
    count_recursive(data)
    return stats


def print_schema_summary(schema: Dict[str, Any], indent: int = 0):
    """Print a readable summary of the schema."""
    indent_str = "  " * indent
    schema_type = schema.get("type", "unknown")
    
    if schema_type == "object":
        num_props = schema.get("num_properties", 0)
        print(f"{indent_str}Object with {num_props} properties:")
        
        properties = schema.get("properties", {})
        for prop_name, prop_schema in properties.items():
            print(f"{indent_str}  {prop_name}:")
            print_schema_summary(prop_schema, indent + 2)
    
    elif schema_type == "array":
        length = schema.get("length", 0)
        item_types = schema.get("item_types", {})
        print(f"{indent_str}Array with {length} items")
        print(f"{indent_str}  Item types: {item_types}")
        
        sample_items = schema.get("sample_items", [])
        if sample_items:
            print(f"{indent_str}  Sample items:")
            for i, item in enumerate(sample_items):
                print(f"{indent_str}    [{i}]:")
                print_schema_summary(item, indent + 3)
    
    elif schema_type == "string":
        length = schema.get("length", 0)
        sample = schema.get("sample", "")
        print(f"{indent_str}String (length: {length})")
        if sample:
            print(f"{indent_str}  Sample: \"{sample}\"")
    
    elif schema_type in ["integer", "number"]:
        sample_value = schema.get("sample_value", "")
        print(f"{indent_str}{schema_type.title()}: {sample_value}")
    
    elif schema_type == "truncated":
        note = schema.get("note", "")
        print(f"{indent_str}Truncated: {note}")
    
    else:
        print(f"{indent_str}{schema_type.title()}")


def main():
    if len(sys.argv) != 2:
        print("Usage: python3 simple_schema_analyzer.py <json_file>")
        sys.exit(1)
    
    filepath = sys.argv[1]
    
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            data = json.load(f)
        
        print(f"Analyzing: {filepath}")
        print("=" * 50)
        
        # Get high-level statistics
        stats = get_summary_stats(data)
        print("Statistics:")
        for key, value in stats.items():
            print(f"  {key}: {value:,}")
        
        print("\nSchema Structure:")
        print("-" * 30)
        
        # Analyze structure
        schema = analyze_structure(data, max_depth=4)
        print_schema_summary(schema)
        
    except json.JSONDecodeError as e:
        print(f"Error: Invalid JSON - {e}")
        sys.exit(1)
    except FileNotFoundError:
        print(f"Error: File not found - {filepath}")
        sys.exit(1)
    except Exception as e:
        print(f"Error: {e}")
        sys.exit(1)


if __name__ == "__main__":
    main() 