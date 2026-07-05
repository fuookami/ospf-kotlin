import os, re, json, sys
sys.stdout.reconfigure(encoding='utf-8', errors='replace')

OBVIOUS_PROPS = {'id', 'name', 'value', 'type'}

def is_src_main(path):
    norm = os.path.normpath(path)
    return os.sep + 'src' + os.sep + 'main' + os.sep in norm

def is_generated(path):
    return 'generated' in path.lower()

def get_module(path):
    parts = path.replace('\\', '/').split('/')
    for i, p in enumerate(parts):
        if p.startswith('ospf-kotlin'):
            if i + 1 < len(parts) and not parts[i+1].startswith('src'):
                return parts[i] + '/' + parts[i+1]
            return parts[i]
    return 'unknown'

def has_preceding_kdoc(lines, line_idx):
    for j in range(line_idx - 1, max(line_idx - 30, -1), -1):
        prev = lines[j].strip()
        if prev.startswith('//') or prev == '':
            continue
        if prev.endswith('*/') or prev == '*/':
            return True
        if prev.startswith('*') or prev.startswith('/*'):
            continue
        break
    return False

def find_kdoc_block(lines, line_idx):
    end = -1
    for j in range(line_idx - 1, max(line_idx - 30, -1), -1):
        prev = lines[j].strip()
        if prev.endswith('*/') or prev == '*/':
            end = j
            break
        if prev.startswith('@') or prev.startswith('//') or prev == '':
            continue
        if prev.startswith('*'):
            continue
        break
    if end == -1:
        return None
    start = end
    for j in range(end - 1, max(end - 30, -1), -1):
        prev = lines[j].strip()
        if prev.startswith('/**') or prev.startswith('/*'):
            start = j
            break
        if prev.startswith('*'):
            continue
        break
    kdoc_text = '\n'.join(lines[start:end+1])
    return (start, end, kdoc_text)

report = {
    'files': {},
    'summary': {
        'total_files': 0,
        'files_with_issues': 0,
        'missing_class_kdoc': 0,
        'missing_func_kdoc': 0,
        'missing_typealias_kdoc': 0,
        'missing_property_tag': 0,
        'missing_param_tag': 0,
        'missing_return_tag': 0,
    }
}

for root, dirs, files in os.walk('.'):
    dirs[:] = [d for d in dirs if d not in ('.git', 'build', '.gradle', 'target', 'node_modules', '.idea', '.claude', '.supergoal')]
    for f in files:
        if not f.endswith('.kt'):
            continue
        path = os.path.join(root, f)
        if not is_src_main(path) or is_generated(path):
            continue

        report['summary']['total_files'] += 1

        try:
            with open(path, 'r', encoding='utf-8', errors='replace') as fh:
                lines = fh.readlines()
        except:
            continue

        file_issues = []

        for i, line in enumerate(lines):
            stripped = line.strip()

            class_match = re.match(
                r'^(public\s+|internal\s+|private\s+|abstract\s+|open\s+|data\s+|sealed\s+|enum\s+|annotation\s+|value\s+|inline\s+)*'
                r'(class|interface|object)\s+(\w+)',
                stripped
            )
            if class_match:
                if stripped.startswith('companion'):
                    continue
                if 'object :' in stripped and not stripped.startswith('object '):
                    continue

                if not has_preceding_kdoc(lines, i):
                    file_issues.append({
                        'type': 'missing_class_kdoc',
                        'line': i + 1,
                        'declaration': stripped[:120],
                        'name': class_match.group(3),
                    })
                    report['summary']['missing_class_kdoc'] += 1
                else:
                    if 'data class' in stripped or 'class' in stripped:
                        paren_line = stripped
                        if '(' not in stripped:
                            for k in range(i+1, min(i+5, len(lines))):
                                paren_line += ' ' + lines[k].strip()
                                if '(' in paren_line:
                                    break
                        paren_match = re.search(r'\(([^)]*)\)', paren_line)
                        if paren_match:
                            params_str = paren_match.group(1)
                            props = re.findall(r'(?:val|var)\s+(\w+)\s*:', params_str)
                            non_obvious = [p for p in props if p not in OBVIOUS_PROPS]
                            if non_obvious:
                                kdoc_info = find_kdoc_block(lines, i)
                                if kdoc_info:
                                    kdoc_text = kdoc_info[2]
                                    if '@property' not in kdoc_text:
                                        file_issues.append({
                                            'type': 'missing_property_tag',
                                            'line': i + 1,
                                            'declaration': stripped[:120],
                                            'name': class_match.group(3),
                                            'missing_props': non_obvious,
                                        })
                                        report['summary']['missing_property_tag'] += 1

            func_match = re.match(
                r'^(public\s+|internal\s+|private\s+|protected\s+|open\s+|abstract\s+|suspend\s+|inline\s+|tailrec\s+|infix\s+)*'
                r'fun\s+(\w+)',
                stripped
            )
            if func_match:
                if stripped.startswith('override') or 'operator' in stripped:
                    continue
                if func_match.group(2) == 'main':
                    continue

                func_sig = stripped
                if '(' in stripped:
                    depth = stripped.count('(') - stripped.count(')')
                    for k in range(i+1, min(i+10, len(lines))):
                        depth += lines[k].count('(') - lines[k].count(')')
                        func_sig += ' ' + lines[k].strip()
                        if depth <= 0:
                            break

                paren_match = re.search(r'\(([^)]*)\)', func_sig)
                params = []
                if paren_match:
                    params_str = paren_match.group(1)
                    params = re.findall(r'(\w+)\s*:', params_str)
                    params = [p for p in params if p not in ('this',)]

                ret_type = None
                ret_match = re.search(r'\):\s*(\w+(?:<[^>]*>)?)', func_sig)
                if ret_match:
                    ret_type = ret_match.group(1)

                if not has_preceding_kdoc(lines, i):
                    file_issues.append({
                        'type': 'missing_func_kdoc',
                        'line': i + 1,
                        'declaration': stripped[:120],
                        'name': func_match.group(2),
                        'params': params,
                        'return_type': ret_type,
                    })
                    report['summary']['missing_func_kdoc'] += 1
                else:
                    kdoc_info = find_kdoc_block(lines, i)
                    if kdoc_info:
                        kdoc_text = kdoc_info[2]
                        if params and '@param' not in kdoc_text:
                            file_issues.append({
                                'type': 'missing_param_tag',
                                'line': i + 1,
                                'declaration': stripped[:120],
                                'name': func_match.group(2),
                                'params': params,
                            })
                            report['summary']['missing_param_tag'] += 1
                        if ret_type and ret_type != 'Unit' and '@return' not in kdoc_text:
                            file_issues.append({
                                'type': 'missing_return_tag',
                                'line': i + 1,
                                'declaration': stripped[:120],
                                'name': func_match.group(2),
                                'return_type': ret_type,
                            })
                            report['summary']['missing_return_tag'] += 1

            typealias_match = re.match(r'^typealias\s+(\w+)', stripped)
            if typealias_match:
                if not has_preceding_kdoc(lines, i):
                    file_issues.append({
                        'type': 'missing_typealias_kdoc',
                        'line': i + 1,
                        'declaration': stripped[:120],
                        'name': typealias_match.group(1),
                    })
                    report['summary']['missing_typealias_kdoc'] += 1

        if file_issues:
            report['files'][path] = {
                'module': get_module(path),
                'issues': file_issues,
            }
            report['summary']['files_with_issues'] += 1

with open('kdoc_missing_report.json', 'w', encoding='utf-8') as f:
    json.dump(report, f, ensure_ascii=False, indent=2)

s = report['summary']
total = s['missing_class_kdoc'] + s['missing_func_kdoc'] + s['missing_typealias_kdoc'] + s['missing_property_tag'] + s['missing_param_tag'] + s['missing_return_tag']
print(f"Total: {s['files_with_issues']} files, {total} issues")
