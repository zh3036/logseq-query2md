#!/usr/bin/env python3
import json
import os
from datetime import datetime

def parse_moments(json_file_path):
    """
    Parse WeChat Moments backup JSON file to extract text content with date and length.
    
    Args:
        json_file_path: Path to the JSON file
    
    Returns:
        List of dictionaries containing date, content, and content_length
    """
    print(f"Processing file: {json_file_path}")
    file_size = os.path.getsize(json_file_path) / (1024 * 1024)  # Size in MB
    print(f"File size: {file_size:.2f} MB")
    
    # Read the JSON file
    try:
        with open(json_file_path, 'r', encoding='utf-8') as file:
            data = json.load(file)
    except Exception as e:
        print(f"Error reading JSON file: {e}")
        return []
    
    # Check if the expected structure exists
    if 'moments' not in data:
        print("Error: JSON doesn't contain 'moments' key")
        return []
    
    # Parse each moment entry
    results = []
    for moment in data['moments']:
        if 'content' in moment and 'create_time' in moment:
            content = moment['content']
            create_time = moment['create_time']
            content_length = len(content)
            
            results.append({
                'date': create_time,
                'content': content,
                'content_length': content_length
            })
    
    # Sort by date (newest first)
    results.sort(key=lambda x: x['date'], reverse=True)
    
    print(f"Extracted {len(results)} moments")
    return results

def save_parsed_data(parsed_data, output_file):
    """
    Save the parsed data to a JSON file
    
    Args:
        parsed_data: List of dictionaries with parsed data
        output_file: Path to save the output
    """
    with open(output_file, 'w', encoding='utf-8') as file:
        json.dump(parsed_data, file, ensure_ascii=False, indent=2)
    print(f"Results saved to {output_file}")

def generate_summary(parsed_data, output_file):
    """
    Generate a summary of the parsed data in markdown format
    
    Args:
        parsed_data: List of dictionaries with parsed data
        output_file: Path to save the output
    """
    # Group by year and month
    posts_by_month = {}
    for item in parsed_data:
        try:
            date = datetime.strptime(item['date'], '%Y-%m-%d %H:%M:%S')
            year_month = f"{date.year}-{date.month:02d}"
            
            if year_month not in posts_by_month:
                posts_by_month[year_month] = []
            
            posts_by_month[year_month].append(item)
        except ValueError:
            continue
    
    # Calculate statistics and write summary
    with open(output_file, 'w', encoding='utf-8') as file:
        file.write("# WeChat Moments Analysis Summary\n\n")
        file.write(f"Total posts analyzed: {len(parsed_data)}\n\n")
        
        file.write("## Posts by Month\n\n")
        file.write("| Month | Number of Posts | Avg Content Length |\n")
        file.write("|-------|----------------|--------------------|\n")
        
        # Sort by year and month
        for year_month in sorted(posts_by_month.keys(), reverse=True):
            posts = posts_by_month[year_month]
            avg_length = sum(post['content_length'] for post in posts) / len(posts)
            file.write(f"| {year_month} | {len(posts)} | {avg_length:.1f} |\n")
        
        file.write("\n## Longest Posts\n\n")
        # Sort by content length
        longest_posts = sorted(parsed_data, key=lambda x: x['content_length'], reverse=True)[:10]
        for i, post in enumerate(longest_posts, 1):
            date = post['date']
            content_preview = post['content'][:100] + "..." if len(post['content']) > 100 else post['content']
            file.write(f"{i}. **Date:** {date}, **Length:** {post['content_length']} chars\n")
            file.write(f"   > {content_preview}\n\n")

if __name__ == "__main__":
    input_file = "pyqs/pyq backup.json"
    output_file = "parsed_moments.json"
    summary_file = "moments_summary.md"
    
    # Parse the JSON file
    parsed_data = parse_moments(input_file)
    
    if parsed_data:
        # Save the parsed data
        save_parsed_data(parsed_data, output_file)
        
        # Generate summary
        generate_summary(parsed_data, summary_file)
        
        print("Processing complete!")
    else:
        print("No data was extracted or an error occurred.")
