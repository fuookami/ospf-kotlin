#!/usr/bin/env python3
"""
Replace Solution<Flt64> with List<Flt64> throughout the codebase.
Also replace IntoValue.Flt64 with a private val in each file.
"""
import os
import re
import sys

ROOT = r"E:\workspace\ospf-kotlin"

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

SOLUTION_FLT64_PATTERN = re.compile(r'\bSolution<Flt64>')
SOLUTION_V_PATTERN = re.compile(r'\bSolution<')
INTO_VALUE_FLT64_PATTERN = re.compile(r'\bIntoValue\.Flt64\b')
SOLUTION_IMPORT = "fuookami.ospf.kotlin.core.model.basic.Solution"
INTO_VALUE_IMPORT = "fuookami.ospf.kotlin.core.solver.value.IntoValue"

count_modified = 0
count_solution_replacements = 0
count_intovalue_replacements = 0

def process_file(filepath):
    global count_modified, count_solution_replacements, count_intovalue_replacements

    try:
        with open(filepath, 'r', encoding='utf-8', errors='replace') as f:
            content = f.read()
    except Exception:
        return

    original = content
    has_solution_flt64 = SOLUTION_FLT64_PATTERN.search(content) is not None
    has_intovalue_flt64 = INTO_VALUE_FLT64_PATTERN.search(content) is not None

    if not has_solution_flt64 and not has_intovalue_flt64:
        return

    # Replace Solution<Flt64> with List<Flt64>
    if has_solution_flt64:
        content = SOLUTION_FLT64_PATTERN.sub('List<Flt64>', content)
        count_solution_replacements += 1

        # Remove Solution import if it exists and no more Solution<V> usages remain
        if SOLUTION_IMPORT in content and SOLUTION_V_PATTERN.search(content) is None:
            # Remove the import line
            lines = content.split('\n')
            new_lines = [l for l in lines if SOLUTION_IMPORT not in l.strip()]
            content = '\n'.join(new_lines)

    # Replace IntoValue.Flt64 with private val
    if has_intovalue_flt64:
        # Check if there's already a private val for this
        if 'private val flt64Converter' not in content:
            # Add private val after package declaration and imports
            content = INTO_VALUE_FLT64_PATTERN.sub('flt64Converter', content)
            count_intovalue_replacements += 1

            # Find the last import line and add the private val after it
            lines = content.split('\n')
            last_import_idx = -1
            for i, line in enumerate(lines):
                stripped = line.strip()
                if stripped.startswith('import ') or stripped.startswith('import\t'):
                    last_import_idx = i

            # Insert the private val after imports
            private_val_line = '\nprivate val flt64Converter = IntoValue.Flt64'
            if last_import_idx >= 0:
                lines.insert(last_import_idx + 1, private_val_line)
            else:
                # Insert after package declaration
                for i, line in enumerate(lines):
                    if line.strip().startswith('package '):
                        lines.insert(i + 1, private_val_line)
                        break
            content = '\n'.join(lines)

    if content != original:
        count_modified += 1
        with open(filepath, 'w', encoding='utf-8', errors='replace') as f:
            f.write(content)

def find_kt_files(root_dir):
    kt_files = []
    for dirpath, dirnames, filenames in os.walk(root_dir):
        for fn in filenames:
            if fn.endswith('.kt'):
                kt_files.append(os.path.join(dirpath, fn))
    return kt_files

def main():
    for target_dir in TARGET_DIRS:
        if not os.path.isdir(target_dir):
            continue
        kt_files = find_kt_files(target_dir)
        for filepath in kt_files:
            process_file(filepath)

    print(f"Modified {count_modified} files")
    print(f"  Solution<Flt64> -> List<Flt64>: {count_solution_replacements}")
    print(f"  IntoValue.Flt64 -> flt64Converter: {count_intovalue_replacements}")

if __name__ == "__main__":
    main()