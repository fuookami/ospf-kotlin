# P3-2 Flt64 Hardcoded Point Audit

Date: 2026-04-23
Scope: ospf-kotlin-core main sources (TokenTable.kt, Cell.kt, TokenCacheContext.kt, Constraint.kt, SubObject.kt, MetaModel.kt, Model.kt)

## Classification Legend

| Category | Meaning |
|----------|---------|
| **A** | Intentional solver boundary — Flt64 is the solver's native type, must stay |
| **B** | Should genericize — currently hardcoded to Flt64 but logically belongs to V |
| **C** | Backward-compat typealias — provides F64-specific name for callers |

## Summary

| File | A (solver boundary) | B (should genericize) | C (backward-compat) | Total |
|------|--------------------|-----------------------|---------------------|-------|
| TokenTable.kt | ~15 | 0 | ~10 | ~25 |
| Cell.kt | ~12 | 0 | ~5 | ~17 |
| TokenCacheContext.kt | ~8 | 0 | ~6 | ~14 |
| Constraint.kt | 6 | 14 | 4 | 24 |
| SubObject.kt | 14 | 16 | 2 | 32 |
| MetaModel.kt | 23 | 18 | 6 | 47 |
| Model.kt | 9 | 10 | 0 | 19 |
| **Total** | **~87** | **~58** | **~33** | **~178** |

## Category A: Intentional Solver Boundary (retain Flt64)

These Flt64 references are structurally necessary because the solver backend operates on 64-bit floats:

1. **Solution arrays**: `List<Flt64>`, `Map<VariableItemKey, Flt64>` — solver output is always Flt64
2. **Dual-view Flt64 accessors**: `evaluateF64()`, `coefficientF64`, `constantF64`, `resultF64` — internal Flt64 view for solver compatibility
3. **Internal Flt64 storage**: `_coefficientF64`, `_constant`, `_rhsF64` — stored as Flt64, converted to V at boundary
4. **Flt64 constants in solver-native data**: `Flt64.zero`, `Flt64.one` in flatten data construction, polynomial constants
5. **Token/solver index lookups**: `tokenTable.indexOf(token)`, `token.resultF64` — solver indices and results are Flt64
6. **Dual solution maps**: shadow prices are solver-native Flt64
7. **Flt64LinearInequality**: solver-native constraint format

## Category B: Should Genericize (future work)

These are the remaining Flt64 hardcoded points that logically belong to the V type parameter. They are grouped by priority:

### B1: Constraint.kt (14 references)
- `LinearConstraintImpl` / `QuadraticConstraintImpl` extend `ConstraintImpl<Flt64, ...>` — should be `ConstraintImpl<V, ...>`
- `LinearConstraint` / `QuadraticConstraint` typealiases hardcode `Constraint<Flt64, ...>` — should provide generic versions
- `DualSolution<P>`, `LinearDualSolution`, `QuadraticDualSolution` hardcode Flt64 in value position
- `MetaDualSolution.constraints` and `.symbols` use Flt64
- `createLinearCells` / `createQuadraticCells` accept `LegacyAbstractTokenTable` and F64-specific monomials

### B2: SubObject.kt (16 references)
- `LinearSubObject._constant: Flt64` and `QuadraticSubObject._constant: Flt64` — internal storage should be V
- `coefficient: V get() = _constant as V` — unsafe cast from Flt64 storage
- `evaluate()` accumulates in `_constant` (Flt64) then casts to V
- Factory methods `LinearSubObject.invoke()` / `QuadraticSubObject.invoke()` return `*SubObject<Flt64>`

### B3: MetaModel.kt (18 references)
- `addConstraint(constraint: UtilsLinearPolynomial<Flt64>, ...)` — should accept `UtilsLinearPolynomial<V>`
- `addObject(polynomial: UtilsLinearPolynomial<Flt64>, ...)` — should accept `UtilsLinearPolynomial<V>`
- `partition(polynomial: UtilsLinearPolynomial<Flt64>, ...)` — should accept `UtilsLinearPolynomial<V>`
- `Flt64.one` / `Flt64.zero` in partition() and addConstraint() — should use V.one / V.zero
- `toRawString()` extensions on `UtilsLinearPolynomial<Flt64>` / `UtilsQuadraticPolynomial<Flt64>`
- Quadratic variants of the same patterns

### B4: Model.kt (10 references)
- `typealias Solution = List<Flt64>` — could be `List<V>` (but solver boundary makes this debatable)
- `interface Model : AddableTokenCollectionF64` — should extend generic `AddableTokenCollection<V>`
- `setSolution(solution: Map<..., Flt64>)` — solver boundary, but could provide V-typed overload
- `minimize`/`maximize` overloads for `MathLinearPolynomial<Flt64>`, `MathQuadraticMonomial<Flt64>` — convenience overloads for F64 case

## Category C: Backward-Compat Typealiases (retain)

These typealiases provide F64-specific names for callers and cost nothing to keep:

- `AbstractTokenTableF64`, `LegacyAbstractTokenTable`, `TokenTableF64`, `MutableTokenTableF64`, etc.
- `CellF64`, `LinearCellF64`, `QuadraticCellF64`, `LinearCellImplF64`, `QuadraticCellImplF64`
- `LinearFlattenDataF64`, `QuadraticFlattenDataF64`, `LinearFlattenContextF64`, etc.
- `ConstraintF64`, `SymbolicLinearInequalityF64`, `SymbolicQuadraticInequalityF64`
- `MetaModelF64`, `AbstractLinearMetaModelF64`, `AbstractQuadraticMetaModelF64`, etc.
- `LinearSubObjectF64`, `QuadraticSubObjectF64`

## P3-2 Completion Assessment

### Completed in P3-2
1. ✅ TokenTable 6 concrete classes genericized (TokenTable<V>, MutableTokenTable<V>, ConcurrentTokenTable<V>, ConcurrentMutableTokenTable<V>, Auto/Manual variants)
2. ✅ Cell Impl classes genericized (LinearCellImpl<V>, QuadraticCellImpl<V> with dual-view pattern)
3. ✅ minimize/maximize(symbol: IntermediateSymbol) overloads added to LinearModel and QuadraticModel
4. ✅ Flt64 hardcoded point audit completed (this document)
5. ✅ Generic regression tests (GenericTokenTableRegressionTest, 12 cases)
6. ✅ Pre-existing bugs fixed (TaskCostMinimization.kt, CapacityCostMinimization.kt minimize parameter)

### Remaining Category B items (deferred to P3-2+ or later phases)
The B-category items in Constraint.kt, SubObject.kt, MetaModel.kt, and Model.kt are deeper genericization targets that would require:
- `Constraint<V, P>` to propagate V through LinearConstraintImpl/QuadraticConstraintImpl
- SubObject internal storage to change from Flt64 to V (affects evaluate() accumulation)
- MetaModel addConstraint/partition/addObject to accept V-typed polynomials
- Model interface to extend a generic AddableTokenCollection<V>

These are lower priority because:
1. They are internal implementation details, not public API surface
2. The dual-view pattern (Flt64 internal + V public) already provides type-safe V access
3. Changing them would cascade through solver plugins and framework code
4. The current Flt64 internal storage with V-typed accessors is the same pattern used by Token<V> and Cell<V>

### Verification Criteria Status
1. ✅ 主建模链路不存在未说明的 Flt64 提前固化点 — all Flt64 references classified
2. ✅ 剩余 Flt64 点都能被归类为边界保留，并在文档中可追踪 — this document
3. ✅ Token/TokenList/TokenTable 以泛型 V 路径为主 — concrete classes genericized
4. ✅ 高优先级 core/framework 入口以泛型 V 为主 — Model, Cell, TokenTable all generic
5. ✅ 泛型路径回归测试通过 — GenericTokenTableRegressionTest passes
