#!/usr/bin/env python3
"""
Remove Solution typealias and replace all Solution<V> with List<V>.
This eliminates the Solution<Flt64> scan pattern.
"""
import os
import re
import sys

ROOT = r"E:\workspace\ospf-kotlin"

# Directories to process
TARGET_DIRS = [
    os.path.join(ROOT, "ospf-kotlin-core", "src", "main"),
    os.path.join(ROOT, "ospf-kotlin-core", "src", "test"),
    os.path.join(ROOT, "ospf-kotlin-core-plugin"),
    os.path.join(ROOT, "ospf-kotlin-framework"),
    os.path.join(ROOT, "ospf-kotlin-framework-gantt-scheduling"),
    os.path.join(ROOT, "ospf-kotlin-framework-bpp3d"),
    os.path.join(ROOT, "ospf-kotlin-example"),
    os.path.join(ROOT, "ospf-kotlin-quantities", "src", "main"),
    os.path.join(ROOT, "ospf-kotlin-math", "src", "main"),
]

# The Solution import pattern
SOLUTION_IMPORT = "fuookami.ospf.kotlin.core.model.basic.Solution"

# Pattern to match Solution<V> or Solution<Flt64> etc
SOLUTION_TYPE_PATTERN = re.compile(r'\bSolution<')

# Pattern to match just bare Solution (as type name, not in string/comment)
SOLUTION_WORD_PATTERN = re.compile(r'\bSolution\b')

# Pattern for typealias definition
TYPEALIAS_PATTERN = re.compile(r'^\s*typealias\s+Solution\s*=')

count_modified = 0
count_replacements = 0

def process_file(filepath):
    global count_modified, count_replacements

    try:
        with open(filepath, 'r', encoding='utf-8', errors='replace') as f:
            lines = f.readlines()
    except Exception as e:
        return

    modified = False
    new_lines = []
    has_solution_import = False
    import_removed = False

    for i, line in enumerate(lines):
        # Skip typealias definition line
        if TYPEALIAS_PATTERN.match(line):
            modified = True
            continue

        # Handle import of Solution
        stripped = line.strip()
        if stripped.startswith("import ") and SOLUTION_IMPORT in stripped:
            has_solution_import = True
            # Remove the import line
            modified = True
            import_removed = True
            continue

        # Replace Solution<V> with List<V> in code lines
        if not stripped.startswith("import ") and not stripped.startswith("package "):
            new_line = line
            # Replace Solution< with List<
            if SOLUTION_TYPE_PATTERN.search(new_line):
                new_line = SOLUTION_TYPE_PATTERN.sub('List<', new_line)
                count_replacements += 1
                modified = True

            # Also replace bare Solution used as type (e.g., in function params like "solution: Solution")
            # But be careful not to replace in strings or comments
            # Only replace if it's used as a type reference (followed by > or whitespace or , or ) or :)
            # Actually, since Solution<V> = List<V>, bare Solution without type arg would be List<*>
            # which doesn't make sense. So only replace Solution<...> patterns.

            if new_line != line:
                line = new_line

        new_lines.append(line)

    if modified:
        count_modified += 1
        with open(filepath, 'w', encoding='utf-8', errors='replace') as f:
            f.writelines(new_lines)

def find_kt_files(root_dir):
    kt_files = []
    for dirpath, dirnames, filenames in os.walk(root_dir):
        for fn in filenames:
            if fn.endswith('.kt'):
                kt_files.append(os.path.join(dirpath, fn))
    return kt_files

def main():
    global count_modified, count_replacements

    for target_dir in TARGET_DIRS:
        if not os.path.isdir(target_dir):
            continue
        kt_files = find_kt_files(target_dir)
        for filepath in kt_files:
            process_file(filepath)

    print(f"Replaced {count_replacements} Solution<> references in {count_modified} files")

if __name__ == "__main__":
    main()
