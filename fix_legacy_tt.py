import re, os

# The key insight: Many files use AbstractTokenTableF64 (which is AbstractTokenTable<Flt64>),
# but they actually need LegacyAbstractTokenTable which has the cache() methods.
#
# Files that use cache(), cacheIfNotCached(), cachedLinearFlatten(), etc. on tokenTable
# need LegacyAbstractTokenTable, not AbstractTokenTable<Flt64>.
#
# Similarly, MetaModel uses AbstractMutableTokenTableF64 but needs LegacyAbstractMutableTokenTable.

base = 'ospf-kotlin-core/src/main'
count = 0

# Files that need LegacyAbstractTokenTable instead of AbstractTokenTableF64
# (because they call cache(), cacheIfNotCached(), etc.)
files_needing_legacy_tt = [
    'IntermediateSymbol.kt',
    'FlattenUtility.kt',
    'CallBackModel.kt',
    'FunctionSymbol.kt',
    'Masking.kt',
    'If.kt',
    'Product.kt',
    'And.kt',
    'Abs.kt',
    'Inequality.kt',
    'Rounding.kt',
    'Ceiling.kt',
    'Floor.kt',
    'Semi.kt',
    'Mod.kt',
    'BigM.kt',
    'Binaryzation.kt',
    'BalanceTernaryzation.kt',
    'BivariateLinearPiecewise.kt',
    'First.kt',
    'IfIn.kt',
    'IfThen.kt',
    'InStepRange.kt',
    'Max.kt',
    'MinMax.kt',
    'OneOf.kt',
    'SameAs.kt',
    'SatisfiedAmount.kt',
    'SatisfiedAmountInequality.kt',
    'Slack.kt',
    'SlackRange.kt',
    'UnivariateLinearPiecewise.kt',
    'Cos.kt',
    'Sin.kt',
    'Sigmoid.kt',
    'LinearMonomial.kt',
    'QuadraticMonomial.kt',
    'Monomial.kt',
    'LinearMonomialSymbol.kt',
    'TokenCacheContext.kt',
    'LinearConstraintInput.kt',
    'LinearTriadModel.kt',
    'QuadraticTetradModel.kt',
    'Policy.kt',
    'CallBackModelInterface.kt',
    'Relation.kt',
    'SubObject.kt',
    'MetaConstraint.kt',
]

for root, dirs, files in os.walk(base):
    for f in files:
        if f.endswith('.kt') and f in files_needing_legacy_tt:
            path = os.path.join(root, f)
            with open(path, 'r', encoding='utf-8') as fh:
                content = fh.read()

            original = content

            # Replace AbstractTokenTableF64 with LegacyAbstractTokenTable in type positions
            # but NOT when it's used as AbstractTokenTableF64<...> (which would be wrong)
            # and NOT in import statements (change import path instead)

            # In import statements: change AbstractTokenTableF64 import to LegacyAbstractTokenTable
            content = content.replace(
                'import fuookami.ospf.kotlin.core.intermediate_model.AbstractTokenTableF64',
                'import fuookami.ospf.kotlin.core.intermediate_model.LegacyAbstractTokenTable'
            )

            # In type positions: change AbstractTokenTableF64 to LegacyAbstractTokenTable
            # but be careful not to change AbstractTokenTable<Flt64> or AbstractTokenTable<V>
            content = re.sub(r'\bAbstractTokenTableF64\b(?!\s*<)', 'LegacyAbstractTokenTable', content)

            # Similarly for AbstractMutableTokenTableF64 -> LegacyAbstractMutableTokenTable
            content = content.replace(
                'import fuookami.ospf.kotlin.core.intermediate_model.AbstractMutableTokenTableF64',
                'import fuookami.ospf.kotlin.core.intermediate_model.LegacyAbstractMutableTokenTable'
            )
            content = re.sub(r'\bAbstractMutableTokenTableF64\b(?!\s*<)', 'LegacyAbstractMutableTokenTable', content)

            # Also fix the import of AbstractTokenListF64 - this is used as a concrete type
            # The variable package already has typealias AbstractTokenListF64 = AbstractTokenList<Flt64>
            # So the import should be fine, but in type positions we need AbstractTokenListF64 not AbstractTokenListOf
            # Actually the error was "AbstractTokenList<Flt64>" doesn't match "AbstractTokenListOf<Flt64>"
            # This is a different issue - let's handle it separately

            if content != original:
                with open(path, 'w', encoding='utf-8') as fh:
                    fh.write(content)
                count += 1
                print(f'Fixed: {path}')

print(f'\nTotal files fixed: {count}')
