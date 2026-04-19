import re, os, glob

# Comprehensive fix: Replace AbstractTokenTableF64 with LegacyAbstractTokenTable
# and AbstractMutableTokenTableF64 with LegacyAbstractMutableTokenTable
# in ALL files that use these as concrete types (not in generic declarations)

base = 'ospf-kotlin-core/src/main'
count = 0

for root, dirs, files in os.walk(base):
    for f in files:
        if f.endswith('.kt'):
            path = os.path.join(root, f)
            with open(path, 'r', encoding='utf-8') as fh:
                content = fh.read()

            original = content

            # Replace AbstractTokenTableF64 with LegacyAbstractTokenTable
            # but NOT when followed by < (generic usage)
            content = content.replace(
                'import fuookami.ospf.kotlin.core.intermediate_model.AbstractTokenTableF64',
                'import fuookami.ospf.kotlin.core.intermediate_model.LegacyAbstractTokenTable'
            )
            content = re.sub(r'\bAbstractTokenTableF64\b(?!\s*<)', 'LegacyAbstractTokenTable', content)

            # Replace AbstractMutableTokenTableF64 with LegacyAbstractMutableTokenTable
            content = content.replace(
                'import fuookami.ospf.kotlin.core.intermediate_model.AbstractMutableTokenTableF64',
                'import fuookami.ospf.kotlin.core.intermediate_model.LegacyAbstractMutableTokenTable'
            )
            content = re.sub(r'\bAbstractMutableTokenTableF64\b(?!\s*<)', 'LegacyAbstractMutableTokenTable', content)

            if content != original:
                with open(path, 'w', encoding='utf-8') as fh:
                    fh.write(content)
                count += 1
                print(f'Fixed: {path}')

print(f'\nTotal files fixed: {count}')