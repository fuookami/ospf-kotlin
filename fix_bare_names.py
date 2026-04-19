import re, os, sys

# Strategy: For each .kt file, replace bare type names with F64-suffixed versions
# when they appear in type positions (not in declarations, not with type params).

# The replacements map: bare_name -> F64_name
# Order matters - longer names first to avoid partial matches
replacements = [
    # TokenTable types (longest first)
    ("ConcurrentMutableTokenTable", "ConcurrentMutableTokenTableF64"),
    ("AbstractMutableTokenTable", "AbstractMutableTokenTableF64"),
    ("AbstractTokenTable", "AbstractTokenTableF64"),
    ("MutableTokenTable", "MutableTokenTableF64"),

    # MetaModel types
    ("AbstractLinearMetaModel", "AbstractLinearMetaModelF64"),
    ("AbstractQuadraticMetaModel", "AbstractQuadraticMetaModelF64"),
    ("AbstractMetaModel", "AbstractMetaModelF64"),
    ("LinearMetaModel", "LinearMetaModelF64"),
    ("QuadraticMetaModel", "QuadraticMetaModelF64"),
    ("MetaModel", "MetaModelF64"),

    # MechanismModel types
    ("AbstractLinearMechanismModel", "AbstractLinearMechanismModelF64"),
    ("AbstractQuadraticMechanismModel", "AbstractQuadraticMechanismModelF64"),
    ("SingleObjectMechanismModel", "SingleObjectMechanismModelF64"),
    ("LinearMechanismModel", "LinearMechanismModelF64"),
    ("QuadraticMechanismModel", "QuadraticMechanismModelF64"),
    ("MechanismModel", "MechanismModelF64"),

    # TokenList types
    ("AbstractMutableTokenList", "AbstractMutableTokenListF64"),
    ("AbstractTokenList", "AbstractTokenListF64"),
    ("MutableTokenList", "MutableTokenListF64"),
    ("AutoTokenList", "AutoTokenListF64"),
    ("ManualTokenList", "ManualTokenListF64"),
    ("TokenList", "TokenListF64"),
    ("AddableTokenCollection", "AddableTokenCollectionF64"),

    # FlattenData types
    ("LinearFlattenData", "LinearFlattenDataF64"),
    ("QuadraticFlattenData", "QuadraticFlattenDataF64"),

    # MonomialCell types
    ("LinearMonomialCell", "LinearMonomialCellF64"),
    ("QuadraticMonomialCell", "QuadraticMonomialCellF64"),

    # Cell types
    ("LinearCellI", "LinearCellF64"),
    ("QuadraticCellI", "QuadraticCellF64"),
    ("LinearCell", "LinearCellF64"),
    ("QuadraticCell", "QuadraticCellF64"),
    ("Cell", "CellF64"),

    # Constraint type
    ("Constraint", "ConstraintF64"),

    # Token type
    ("Token", "TokenF64"),

    # BasicModel
    ("BasicModel", "BasicModelF64"),
    ("BasicMechanismModel", "BasicMechanismModelF64"),
]

base = sys.argv[1] if len(sys.argv) > 1 else 'ospf-kotlin-core/src/main'
count = 0

for root, dirs, files in os.walk(base):
    for f in files:
        if f.endswith('.kt'):
            path = os.path.join(root, f)
            with open(path, 'r', encoding='utf-8') as fh:
                lines = fh.readlines()

            new_lines = []
            changed = False

            for line in lines:
                new_line = line

                # Skip lines that are declarations
                # Declaration patterns: class/interface/data class/sealed class/sealed interface
                # Also skip typealias definitions (left side)
                stripped = line.strip()

                is_declaration = False
                for kw in ['class ', 'interface ', 'data class ', 'sealed class ', 'sealed interface ']:
                    if stripped.startswith(kw):
                        # Check if any of our bare names appear after the keyword
                        # This is a declaration - don't change the name being declared
                        for bare, f64 in replacements:
                            # Pattern: keyword + F64Name (if we already changed it)
                            # We need to NOT change the declared name
                            decl_pattern = kw + f64
                            if decl_pattern in line:
                                # Revert the declaration name
                                line = line.replace(kw + f64, kw + bare)
                                new_line = line
                                is_declaration = True
                                break

                # Also handle typealias definitions: "typealias F64Name = " is fine
                # but "typealias bareName = " should not have bareName changed
                if stripped.startswith('typealias '):
                    # The left side of typealias should not be changed
                    # But we may have already changed it in a bulk replace
                    # This is actually fine - typealias F64Name = ... is the new convention
                    # But the original "typealias bareName = Foo<V>" should remain as-is
                    # Since those are in declaration files, let's just skip typealias lines
                    # for replacement
                    pass

                if not is_declaration:
                    for bare, f64 in replacements:
                        # Replace bare name with F64 name when NOT followed by < (generic)
                        # and NOT in a declaration
                        pattern = r'\b' + re.escape(bare) + r'\b(?!\s*<)'
                        new_line = re.sub(pattern, f64, new_line)

                if new_line != line:
                    changed = True

                new_lines.append(new_line)

            if changed:
                with open(path, 'w', encoding='utf-8') as fh:
                    fh.writelines(new_lines)
                count += 1
                print(f'Updated: {path}')

print(f'\nTotal files updated: {count}')
