# P4-2 Generic Baseline Freeze

Date: 2026-04-27
Branch: rewrite-bigbang
Base commit: d7320de1 (P4-1 Phase A2-A4 complete)

## 1. TokenF64 Distribution

**Definition:** `typealias TokenF64 = Token<Flt64>` in `core/token/Token.kt:135`

| Module | Files | Occurrences | Classification |
|--------|-------|-------------|----------------|
| core/intermediate_symbol | 4 | 44 | B (internal, most in IntermediateSymbol.kt) |
| core/token | 1 | 3 | A (definition + TokenTable) |
| core/model/mechanism | 5 | 25 | B (internal) |
| core/model/intermediate | 2 | 2 | B (internal) |
| framework | 0 | 0 | — |
| gantt-scheduling | 0 | 0 | — |
| example | 0 | 0 | — |
| **Total** | **12** | **74** | |

Excluding whitelist (Token.kt, TokenList.kt, TokenTable.kt, TokenCacheContext.kt): **34**

## 2. LegacyAbstractTokenTable Distribution

**Definitions:**
- `typealias LegacyAbstractTokenTable = AbstractTokenTable<Flt64>` in `core/token/TokenTable.kt:188`
- `typealias LegacyAbstractMutableTokenTable = AbstractMutableTokenTable<Flt64>` in `core/token/TokenTable.kt:190` (both @Deprecated)

| Module | Files | Occurrences | Classification |
|--------|-------|-------------|----------------|
| core/model/mechanism | 3 | 10 | B (MetaModel, MechanModel, BasicModel) |
| core/model/callback | 2 | 5 | B (CallBackModel, CallBackModelInterface) |
| core/token | 1 | 1 | A (definition) |
| framework | 0 | 0 | — |
| gantt-scheduling | 0 | 0 | — |
| example | 0 | 0 | — |
| **Total** | **6** | **16** | |

Excluding whitelist (TokenTable.kt): **109**

## 3. @Deprecated typealias Count

| File | Count | Category |
|------|-------|----------|
| intermediate_symbol/function/FunctionSymbol.kt | 41 | Flt64*Function aliases |
| intermediate_symbol/SymbolCombination.kt | 20 | *FunctionSymbols* aliases |
| token/TokenTable.kt | 2 | LegacyAbstract*Table |
| model/intermediate/LinearTriadModel.kt | 1 | sparseLhs deprecation |
| model/intermediate/QuadraticTetradModel.kt | 1 | sparseLhs deprecation |
| model/basic/ModelView.kt | 1 | sparseLhs deprecation |
| model/mechanism/MathInequalityBridge.kt | 2 | bridge deprecation |
| **Total** | **68** | |

## 4. Flt64-specific typealias Inventory (non-deprecated)

| File | Count | Examples |
|------|-------|----------|
| token/Token.kt | 1 | TokenF64 |
| token/TokenList.kt | 6 | AbstractTokenListF64, TokenListF64, ... |
| token/TokenTable.kt | 4 | AbstractTokenTableF64, TokenTableF64, ... |
| token/TokenCacheContext.kt | 6 | LinearFlattenDataF64, ... |
| model/mechanism/MechanismModel.kt | 5 | MechanismModelF64, ... |
| model/mechanism/MetaModel.kt | 5 | MetaModelF64, ... |
| model/mechanism/Constraint.kt | 3 | ConstraintF64, ... |
| model/mechanism/SubObject.kt | 2 | LinearSubObjectF64, ... |
| model/intermediate/Cell.kt | 5 | CellF64, ... |
| model/intermediate/SparseMatrix.kt | 2 | SparseVectorF64, ... |
| intermediate_symbol/IntermediateSymbol.kt | 3 | IntermediateSymbolF64, ... |
| variable/AbstractVariableItem.kt | 1 | AnyVariableF64 |
| intermediate_symbol/function/Product.kt | 1 | ProductFunctionF64 |
| **Total** | **~44** | |

## 5. Prohibited New Patterns

1. No new `TokenF64` usage outside of typealias definition and compat tests
2. No new `LegacyAbstractTokenTable*` usage outside of typealias definition and compat tests
3. No new references to old `FunctionSymbol` / `LinearFunctionSymbol` / `QuadraticFunctionSymbol` / `FunctionSymbolRegistrationScope`
4. No new non-boundary `Flt64` hardening points in `core/intermediate_symbol`, `core/token`, `core/model` main paths

## 6. Boundary Classification

- **A (Solver boundary):** `LinearTriadModel`, `QuadraticTetradModel`, `Cell` solver-facing APIs, `TokenF64` definition
- **B (Internal, to migrate):** `IntermediateSymbol`, `MechanismModel`, `MetaModel`, `BasicModel`, `CallBackModel`, `Constraint`, `SubObject`, `MathInequalityDsl`, `LinearConstraintInput`
- **C (Backward compat, keep):** All `*F64` typealias, `@Deprecated` typealias bridges
