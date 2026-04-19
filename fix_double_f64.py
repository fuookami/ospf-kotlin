import re, os

# Fix double F64 suffixes: FooF64F64 -> FooF64
double_f64_pattern = re.compile(r'F64F64\b')

# Fix function names that shouldn't have F64 suffix
# e.g., toLinearFlattenDataF64 -> toLinearFlattenData
# e.g., toQuadraticFlattenDataF64 -> toQuadraticFlattenData
# e.g., toLinearMonomialCellF64s -> toLinearMonomialCells
# e.g., toQuadraticMonomialCellF64s -> toQuadraticMonomialCells

base = 'ospf-kotlin-core/src/main'
count = 0

for root, dirs, files in os.walk(base):
    for f in files:
        if f.endswith('.kt'):
            path = os.path.join(root, f)
            with open(path, 'r', encoding='utf-8') as fh:
                content = fh.read()

            original = content

            # Fix double F64
            content = double_f64_pattern.sub('F64', content)

            # Fix function names with incorrectly added F64
            content = content.replace('toLinearFlattenDataF64', 'toLinearFlattenData')
            content = content.replace('toQuadraticFlattenDataF64', 'toQuadraticFlattenData')
            content = content.replace('toLinearMonomialCellF64s', 'toLinearMonomialCells')
            content = content.replace('toQuadraticMonomialCellF64s', 'toQuadraticMonomialCells')
            content = content.replace('toLinearMonomialCellsF64s', 'toLinearMonomialCells')
            content = content.replace('toQuadraticMonomialCellsF64s', 'toQuadraticMonomialCells')
            content = content.replace('flattenedMonomialsF64', 'flattenedMonomials')
            content = content.replace('cacheF64', 'cache')
            content = content.replace('clearSolutionF64', 'clearSolution')
            content = content.replace('setSolutionF64', 'setSolution')
            content = content.replace('registerF64', 'register')

            # Fix import paths with double F64
            content = content.replace('LinearMonomialCellF64F64', 'LinearMonomialCellF64')
            content = content.replace('QuadraticMonomialCellF64F64', 'QuadraticMonomialCellF64')

            if content != original:
                with open(path, 'w', encoding='utf-8') as fh:
                    fh.write(content)
                count += 1
                print(f'Fixed: {path}')

print(f'\nTotal files fixed: {count}')
