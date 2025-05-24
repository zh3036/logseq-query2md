#!/usr/bin/env python3
import json

def filter_long_content(input_file, output_file, min_length=70):
    """
    Filter parsed moments JSON file to only include entries with content length > min_length
    
    Args:
        input_file: Path to the input JSON file
        output_file: Path to save the filtered output
        min_length: Minimum content length to include (default: 70)
    """
    print(f"Reading from: {input_file}")
    
    # Read the input JSON file
    try:
        with open(input_file, 'r', encoding='utf-8') as file:
            data = json.load(file)
    except Exception as e:
        print(f"Error reading JSON file: {e}")
        return
    
    # Filter entries where content_length > min_length
    filtered_data = [entry for entry in data if entry.get('content_length', 0) > min_length]
    
    # Save the filtered data
    with open(output_file, 'w', encoding='utf-8') as file:
        json.dump(filtered_data, file, ensure_ascii=False, indent=2)
    
    print(f"Original entries: {len(data)}")
    print(f"Filtered entries (length > {min_length}): {len(filtered_data)}")
    print(f"Results saved to: {output_file}")

if __name__ == "__main__":
    input_file = "parsed_moments.json"
    output_file = "long_moments.json"
    min_length = 70
    
    filter_long_content(input_file, output_file, min_length)
