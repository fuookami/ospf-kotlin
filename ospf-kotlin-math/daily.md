# 2026-04-24 Legacy Expr Removal — COMPLETED

## Goal

Delete the entire legacy `Expr` stack from `ospf-kotlin-math` without breaking:

- polynomial and inequality parsing
- polynomial and inequality DSL entry points
- JSON serialization and round-trip for polynomial and inequality types
- the new `symbol.expression.*` runtime stack

All 8 phases have been executed and verified. The legacy `Expr` stack no longer exists in the codebase.

## Current State

- All 8 phases are DONE. The legacy `Expr` stack has been fully removed from `ospf-kotlin-math`.
- `ospf-kotlin-math` builds and all 705 tests pass.
- The specialized fast paths for linear, quadratic, and canonical polynomials are preserved.
- No production code references any legacy `Expr` symbol.

## Files That Were Part Of The Legacy Expr Stack (all deleted or rewritten)

- ~~`src/main/fuookami/ospf/kotlin/math/symbol/parser/Expr.kt`~~ — DELETED in Phase 6
- ~~`src/main/fuookami/ospf/kotlin/math/symbol/parser/Lexer.kt`~~ — DELETED in Phase 6
- ~~`src/main/fuookami/ospf/kotlin/math/symbol/parser/Token.kt`~~ — DELETED in Phase 6
- ~~`src/main/fuookami/ospf/kotlin/math/symbol/parser/Parser.kt`~~ — REWRITTEN in Phase 6 (delegation to `symbol.parse`)
- ~~`src/main/fuookami/ospf/kotlin/math/symbol/parser/ParseError.kt`~~ — DELETED in Phase 6
- ~~`src/main/fuookami/ospf/kotlin/math/symbol/parser/NumberParser.kt`~~ — DELETED in Phase 6
- ~~`src/main/fuookami/ospf/kotlin/math/symbol/dsl/SymbolDsl.kt`~~ — DELETED in Phase 3
- ~~`src/main/fuookami/ospf/kotlin/math/symbol/serde/SymbolExpr.kt`~~ — DELETED in Phase 6
- ~~`src/main/fuookami/ospf/kotlin/math/symbol/expression/adapter/LegacyExprBridge.kt`~~ — DELETED in Phase 5

## Files That Must Not Be Deleted As Part Of Legacy Expr Removal

These are bridge-like in name or shape, but they are part of the new expression stack and still have production value.

- `src/main/fuookami/ospf/kotlin/math/symbol/expression/PathSymbol.kt`
- `src/main/fuookami/ospf/kotlin/math/symbol/expression/PropertyPath.kt`
- `src/main/fuookami/ospf/kotlin/math/symbol/expression/ScalarExpression.kt`
- `src/main/fuookami/ospf/kotlin/math/symbol/expression/BooleanExpression.kt`
- `src/main/fuookami/ospf/kotlin/math/symbol/expression/serde/ExpressionSerde.kt`
- `src/main/fuookami/ospf/kotlin/math/symbol/expression/operation/EvaluateBoolean.kt`
- `src/main/fuookami/ospf/kotlin/math/symbol/expression/operation/Normalize.kt`

These are also not part of the legacy `Expr` stack and must be preserved because they implement the specialized optimized paths for linear, quadratic, and canonical polynomials that motivated this module design:

- `src/main/fuookami/ospf/kotlin/math/symbol/polynomial/LinearPolynomial.kt`
- `src/main/fuookami/ospf/kotlin/math/symbol/polynomial/MutableLinearPolynomial.kt`
- `src/main/fuookami/ospf/kotlin/math/symbol/inequality/LinearInequality.kt`
- `src/main/fuookami/ospf/kotlin/math/symbol/operation/LinearQuadraticOps.kt`
- `src/main/fuookami/ospf/kotlin/math/symbol/operation/CompileOps.kt`
- `src/main/fuookami/ospf/kotlin/math/symbol/operation/Compile.kt`
- `src/main/fuookami/ospf/kotlin/math/symbol/operation/Evaluate.kt`

Important:

- `PathSymbol` is still used directly by `ScalarReference` and `NullCheck`.
- `ScalarSymbolReference` is still used by the new expression serde and normalize/evaluate logic.
- Do not delete `PathSymbol`, `PropertyPath`, `ScalarSymbolReference`, or their supporting APIs in the first pass.

## Production Dependencies (all migrated)

All legacy dependencies have been migrated to direct APIs:

- ~~`src/main/fuookami/ospf/kotlin/math/symbol/dsl/SymbolDsl.kt`~~ — DELETED in Phase 3
- `src/main/fuookami/ospf/kotlin/math/symbol/parser/Parser.kt` — REWRITTEN: delegates to `symbol.parse` package
- ~~`src/main/fuookami/ospf/kotlin/math/symbol/serde/SymbolExpr.kt`~~ — DELETED in Phase 6; replaced by `PolynomialSerde.kt` and `InequalitySerde.kt`

## Required Target Architecture

The correct target is not "replace `Expr` with `ScalarExpression` everywhere".

The target should be:

- `symbol.expression.*` remains the runtime expression system.
- polynomial and inequality parsing stop going through a generic expression AST
- polynomial and inequality DSL stop going through a generic expression AST
- polynomial and inequality JSON serde stop going through a generic expression AST
- symbol identity serialization is split out and retained
- linear, quadratic, and canonical specialized evaluation, ordered evaluation, compilation, gradient, and inequality construction remain first-class paths

In other words:

- remove the legacy generic AST
- keep the new runtime expression stack
- move polynomial and inequality flows to direct representations
- keep all specialized linear, quadratic, and canonical hot paths intact

## Non-Negotiable Constraint

Deleting the legacy `Expr` stack must not collapse the current optimized linear, quadratic, and canonical paths into a single slower generic fallback.

Specifically preserve:

- `evaluateLinear`
- `evaluateLinearOrdered`
- `partialEvaluateLinear`
- `compileEvalLinear`
- `compileGradientLinear`
- `evaluateQuadratic`
- `evaluateQuadraticOrdered`
- `partialEvaluateQuadratic`
- `compileEvalQuadratic`
- `compileGradientQuadratic`
- `evaluateCanonical`
- `evaluateCanonicalOrdered`
- `partialEvaluateCanonical`
- `compileEvalCanonical`
- `compileGradientCanonical`
- direct `LinearPolynomial`, `QuadraticPolynomial`, and `CanonicalPolynomial` APIs
- direct `LinearInequality`, `QuadraticInequality`, and `CanonicalInequality` construction helpers

Allowed to change:

- parser
- DSL construction
- JSON bridge format

Not allowed in the final state:

- parsing linear expressions by routing everything through a generic AST and only then down-converting
- parsing quadratic or canonical expressions by collapsing everything to one generic compatibility layer first
- evaluating linear, quadratic, or canonical expressions only through one generic compatibility runtime path
- compiling linear, quadratic, or canonical expressions only through one generic compatibility runtime path
- deleting `LinearQuadraticOps.kt`, `CompileOps.kt`, or the specialized wrappers in `Evaluate.kt` / `Compile.kt` and replacing them with a single legacy-compatibility runtime

## Recommended Execution Order

Use this order. Do not start by deleting files.

1. Split reusable non-legacy pieces out of the old parser and serde files.
2. Replace parser flows with direct polynomial and inequality parsing.
3. Replace DSL flows with direct polynomial and inequality builders.
4. Replace JSON serde flows with direct polynomial and inequality DTOs.
5. Delete `LegacyExprBridge`.
6. Delete the legacy parser, legacy DSL, and legacy AST serde files.
7. Clean tests and documentation.

## Phase 1: Extract Survivors From Legacy Files — DONE

Completed in previous sessions. All items moved to stable non-legacy locations.

### 1A. Split Parse Result Types — DONE

Moved to `src/main/fuookami/ospf/kotlin/math/symbol/parse/ParseResult.kt`:
- `ParseResult<T>` (= `Ret<T>`)
- `ParseIssueType`
- `ParseIssue`

`parser/ParseError.kt` now contains `@Deprecated` typealias bridges to `parse/` package types plus the still-needed `ParseError` class.

### 1B. Split Number Parsers — DONE

Moved to `src/main/fuookami/ospf/kotlin/math/symbol/parse/NumberParser.kt`:
- `NumberParser<T>`
- `Flt64NumberParser`
- `Int64NumberParser`

`parser/NumberParser.kt` now contains `@Deprecated` typealias bridges to `parse/` package types.

### 1C. Split Symbol Identity Serde — DONE

Extracted to `src/main/fuookami/ospf/kotlin/math/symbol/serde/SymbolIdentitySerde.kt`:
- `SerializedSymbolIdentityPrefix`
- `SymbolIdentityExpr`
- `symbolOfSerializedIdentifier`
- `toSymbolIdentityExpr`
- `toSerializedIdentifier`
- helper functions required only for symbol identity serialization

## Phase 2: Replace Parser Flows — PARTIALLY DONE

### What Was Completed

- New direct polynomial/inequality parser created at `src/main/fuookami/ospf/kotlin/math/symbol/parse/PolynomialParser.kt`
  - `PolynomialLexer` and `PolynomialToken` at `src/main/fuookami/ospf/kotlin/math/symbol/parse/PolynomialLexer.kt`
  - Direct recursive-descent parser that produces `CanonicalPolynomial`, `LinearPolynomial`, `QuadraticPolynomial`, `CanonicalInequality`, `LinearInequality`, `QuadraticInequality` directly
  - Throwing variants: `parseCanonical`, `parseLinear`, `parseQuadratic`, `parseCanonicalInequality`, `parseLinearInequality`, `parseQuadraticInequality`
  - Typed variants: `parseCanonicalTyped`, `parseLinearTypedOrNull`, `parseQuadraticTypedOrNull`
  - Ret-wrapped variants: `parseCanonicalRet`, `parseLinearRet`, `parseQuadraticRet`, `parseCanonicalTypedRet`, `parseLinearTypedRetOrNull`, `parseQuadraticTypedRetOrNull`, `parseCanonicalInequalityRet`, `parseLinearInequalityRet`, `parseQuadraticInequalityRet`
- `parser/Parser.kt` delegation rewritten: `parseCanonical`, `parseLinear`, `parseQuadratic`, `parseLinearInequality`, `parseQuadraticInequality` now delegate to `parse/` package's Ret-wrapped functions instead of going through `Expr` AST
- `serde/SymbolExpr.kt` imports updated: `NumberParser` and `Flt64NumberParser` now imported from `parse/` package
- `parser/NumberParser.kt` replaced with `@Deprecated` typealias bridges to `parse.NumberParser`, `parse.Flt64NumberParser`, `parse.Int64NumberParser`
- `parser/ParseError.kt` replaced `ParseResult`, `ParseIssueType`, `ParseIssue` with `@Deprecated` typealias bridges to `parse/` package types; `ParseError` class retained
- Test file created: `src/test/fuookami/ospf/kotlin/math/symbol/parse/DirectPolynomialParserTest.kt`

### What Remains For Phase 2 Acceptance

Phase 2 acceptance criteria:
- no production code imports `fuookami.ospf.kotlin.math.symbol.parser.Expr` — **NOT YET**: `serde/SymbolExpr.kt` still imports `parser.Expr`, `parser.BinaryOperator`, `parser.ComparisonOperator`
- no production code imports `parseLegacySymbolExpression*` — **NOT YET**: `parser/Parser.kt` still contains `parseLegacySymbolExpression`/`parseLegacySymbolInequality` functions (deprecated but present). The `symbol.dsl` package that was a major consumer has been removed in Phase 3.
- all parse tests are green after being rewritten to the new direct parser — **DONE**: `DirectPolynomialParserTest.kt` passes

The remaining `parser.Expr` imports in `SymbolExpr.kt` are for JSON serde, which is Phase 4's scope. The `parseLegacySymbolExpression*` functions are deprecated bridges that can be deleted in Phase 6 after all callers are migrated.

### Files Deleted In Phase 6 (deferred from Phase 2)

- `src/main/fuookami/ospf/kotlin/math/symbol/parser/Expr.kt` — DELETED
- `src/main/fuookami/ospf/kotlin/math/symbol/parser/Lexer.kt` — DELETED
- `src/main/fuookami/ospf/kotlin/math/symbol/parser/Token.kt` — DELETED
- `src/main/fuookami/ospf/kotlin/math/symbol/parser/Parser.kt` — REWRITTEN (delegation to `symbol.parse`, not deleted)
- `src/main/fuookami/ospf/kotlin/math/symbol/parser/ParseError.kt` — DELETED
- `src/main/fuookami/ospf/kotlin/math/symbol/parser/NumberParser.kt` — DELETED

## Phase 3: Replace DSL Flows — DONE

### Decision

The `symbol.dsl` package was removed entirely. No production code used it — only test code. The same functionality is available through the direct parser (`parse/PolynomialParser.kt`) and the type-safe infix DSL in `ospf-kotlin-core` (`MathInequalityDsl.kt`).

### What Was Done

- Deleted `src/main/fuookami/ospf/kotlin/math/symbol/dsl/SymbolDsl.kt`
- Deleted `src/test/fuookami/ospf/kotlin/math/symbol/dsl/DslTest.kt`
- Removed empty `dsl/` directories
- Fixed pre-existing test compilation error in `NumberParserIntegrationTest.kt` (import `Flt64NumberParser`/`Int64NumberParser` from `parse` package instead of deprecated `parser` package typealias)
- Fixed pre-existing test failure in `ParserTypedEntryTest.kt` (improved `ParseIssue` type assertion and added debug diagnostics)

### What Remains

- `legacyToLinearPolynomialOrNull`, `legacyToQuadraticPolynomialOrNull`, `legacyToCanonicalPolynomial`, and inequality variants are still defined as extension functions on `LegacySymbolExpr` in `serde/SymbolExpr.kt`. These are Phase 4 scope — they will be replaced when JSON serde is rewritten.
- The `symbol.dsl` package no longer exists. Users should use `parse/PolynomialParser.kt` functions or the core infix DSL instead.

## Phase 4: Replace JSON Serde Flows — DONE

### Decision

Wire compatibility with the old Expr AST JSON format was NOT preserved. New direct DTO serde replaces the legacy Expr-based JSON entirely.

### What Was Done

- Created `src/main/fuookami/ospf/kotlin/math/symbol/serde/PolynomialSerde.kt`
  - DTO classes: `CanonicalPolynomialData`, `CanonicalMonomialData`, `LinearPolynomialData`, `LinearMonomialData`, `QuadraticPolynomialData`, `QuadraticMonomialData`
  - Public API: `CanonicalPolynomial<Flt64>.toJsonString(symbolComparator?)`, `LinearPolynomial<Flt64>.toJsonString()`, `QuadraticPolynomial<Flt64>.toJsonString()`, `canonicalPolynomialFromJson`, `linearPolynomialFromJson`, `quadraticPolynomialFromJson`
  - Coefficients serialized as `Double` via `Flt64.value`
  - Symbol identifiers serialized via `Symbol.toSymbolIdentityExpr().toSerializedIdentifier()`
  - Powers serialized as `Map<String, Int>` (symbol identifier -> exponent)

- Created `src/main/fuookami/ospf/kotlin/math/symbol/serde/InequalitySerde.kt`
  - DTO classes: `CanonicalInequalityData`, `LinearInequalityData`, `QuadraticInequalityData`
  - Comparison serialized as String ("LT", "LE", "EQ", "NE", "GE", "GT")
  - Public API: `CanonicalInequality.toJsonString(symbolComparator?)`, `LinearInequality<Flt64>.toJsonString()`, `QuadraticInequalityOf<Flt64>.toJsonString(symbolComparator?)`, `canonicalInequalityFromJson`, `linearInequalityFromJson`, `quadraticInequalityFromJson`

- Removed conflicting JSON serde functions from `serde/SymbolExpr.kt`:
  - Removed `toLegacyJsonString`, `toJsonString` on Expr
  - Removed `legacySymbolExprFromJson`, `symbolExprFromJson`
  - Removed polynomial/inequality `toJsonString` and `fromJson` functions
  - Removed unused imports: `readFromJson`, `writeJson`, `ByteArrayInputStream`
  - `SymbolExpr.kt` still contains: `LegacySymbolExpr` typealias, `toLegacyExpr()` extensions, `legacyTo*` conversion helpers, `@Deprecated` bridge functions (needed until Phase 5/6)

- Rewrote `src/test/fuookami/ospf/kotlin/math/symbol/serialization/SerializationTest.kt`
  - Tests direct DTO serde round-trips for all polynomial and inequality types
  - Uses structural comparison (symbol names, coefficients) instead of object equality since deserialization creates `DefaultSymbol` instances

- Rewrote `src/test/fuookami/ospf/kotlin/math/symbol/roundtrip/SymbolRoundTripTest.kt`
  - Same approach as SerializationTest with structural comparison

### Acceptance Criteria Met

- Polynomial and inequality `toJsonString` no longer call `toLegacyExpr` — **DONE**
- `fromJson` no longer parses legacy `Expr` — **DONE**
- Symbol identity serde still works — **DONE** (unchanged, `SymbolIdentitySerde.kt`)
- Linear and quadratic JSON round-trip still land on their specialized optimized runtime paths — **DONE** (`linearPolynomialFromJson` returns `LinearPolynomial`, `quadraticPolynomialFromJson` returns `QuadraticPolynomial`)

### What Remains

- `SymbolExpr.kt` still contains `LegacySymbolExpr` typealias, `toLegacyExpr()` extensions, and `legacyTo*` conversion helpers. These will be removed in Phase 5/6 when `LegacyExprBridge.kt` is deleted.
- `SymbolExpr.kt` still imports `parser.Expr`, `parser.BinaryOperator`, `parser.ComparisonOperator` for the remaining legacy conversion functions. These imports will be removed when the file is cleaned up in Phase 6.

## Phase 5: Delete LegacyExprBridge — DONE

### What Was Done

- Deleted `src/main/fuookami/ospf/kotlin/math/symbol/expression/adapter/LegacyExprBridge.kt`
- Deleted `src/test/fuookami/ospf/kotlin/math/symbol/expression/adapter/LegacyExprBridgeTest.kt`
- Removed empty `expression/adapter/` directories

### Problem

`LegacyExprBridge.kt` exists only to convert between the new expression stack and the old legacy `Expr` stack.

### Safe Time To Delete

Only delete it after:

- Phase 2 is complete
- Phase 3 is complete
- Phase 4 is complete
- there is no production need to convert to or from legacy `Expr`

### Files To Delete

- `src/main/fuookami/ospf/kotlin/math/symbol/expression/adapter/LegacyExprBridge.kt`
- tests under `src/test/.../symbol/expression/adapter/LegacyExprBridgeTest.kt`

### Important

Do not confuse this file with `PathSymbol` or `ScalarSymbolReference`. Those are not part of the legacy `Expr` stack removal.

## Phase 6: Delete Legacy Files — DONE

### What Was Done

- Deleted `src/main/fuookami/ospf/kotlin/math/symbol/serde/SymbolExpr.kt` (entirely legacy — LegacySymbolExpr typealias, toLegacyExpr, legacyTo* conversions, @Deprecated bridges)
- Deleted `src/main/fuookami/ospf/kotlin/math/symbol/parser/Expr.kt` (legacy AST)
- Deleted `src/main/fuookami/ospf/kotlin/math/symbol/parser/Lexer.kt` (legacy lexer)
- Deleted `src/main/fuookami/ospf/kotlin/math/symbol/parser/Token.kt` (legacy tokens)
- Deleted `src/main/fuookami/ospf/kotlin/math/symbol/parser/NumberParser.kt` (@Deprecated typealias bridges)
- Deleted `src/main/fuookami/ospf/kotlin/math/symbol/parser/ParseError.kt` (@Deprecated typealias bridges + ParseError class)
- Rewrote `src/main/fuookami/ospf/kotlin/math/symbol/parser/Parser.kt` — removed legacy Parser class, parseLegacySymbolExpression, parseLegacySymbolInequality, and all @Deprecated bridges. Kept only delegation functions (parseCanonical, parseLinear, parseQuadratic, parseLinearInequality, parseQuadraticInequality) that delegate to `symbol.parse` package.

## Phase 7: Documentation Cleanup — DONE

### What Was Done

- Updated `ospf-kotlin-math/README.md` — removed legacy Expr references from module structure table, expression entry points, parse examples, and DSL examples
- Updated `ospf-kotlin-math/README_ch.md` — same changes in Chinese
- Updated `src/main/fuookami/ospf/kotlin/math/symbol/README.md` — removed legacy Expr references, replaced DSL section with Parsing section
- Updated `src/main/fuookami/ospf/kotlin/math/symbol/README_ch.md` — same changes in Chinese
- Updated `src/main/fuookami/ospf/kotlin/math/symbol/expression/README.md` — replaced legacy coexistence/migration sections with relationship to polynomial/inequality parsers, updated comparison table
- Updated `src/main/fuookami/ospf/kotlin/math/symbol/expression/README_ch.md` — same changes in Chinese

### Removed References

All mentions of the following have been removed from documentation:
- legacy `Expr` as a supported public stack
- `legacySymbolExpr`
- `parseLegacySymbolExpression`
- `LegacySymbolExpr`
- `toLegacyExpr`
- `LegacyExprBridge`

Update these files:

- `ospf-kotlin-math/README.md`
- `ospf-kotlin-math/README_ch.md`
- `src/main/fuookami/ospf/kotlin/math/symbol/README.md`
- `src/main/fuookami/ospf/kotlin/math/symbol/README_ch.md`
- `src/main/fuookami/ospf/kotlin/math/symbol/expression/README.md`
- `src/main/fuookami/ospf/kotlin/math/symbol/expression/README_ch.md`

Remove all mentions of:

- legacy `Expr` as a supported public stack
- `legacySymbolExpr`
- `parseLegacySymbolExpression`
- `LegacySymbolExpr`
- `toLegacyExpr`
- `LegacyExprBridge`

After the deletion is complete, the docs should say:

- `symbol.expression.*` is the expression stack
- polynomial and inequality parsing/DSL/serde are direct APIs, not AST-compatibility APIs

## Phase 8: Test Migration — DONE

### What Was Done

- Rewrote `NumberParserIntegrationTest.kt` to test `Flt64NumberParser` and `Int64NumberParser` from `symbol.parse` package
- Rewrote `ParserTypedEntryTest.kt` to test typed parse entry points from `symbol.parser` package
- Deleted `ParserPolynomialTest.kt` (tested legacy Expr-based parsing)
- Deleted `ParserInequalityTest.kt` (tested legacy Expr-based parsing)

Rewrite or delete these test groups:

- `src/test/.../symbol/parser/*`
- `src/test/.../symbol/dsl/DslTest.kt`
- `src/test/.../symbol/serialization/SerializationTest.kt`
- `src/test/.../symbol/roundtrip/SymbolRoundTripTest.kt`
- `src/test/.../symbol/expression/adapter/LegacyExprBridgeTest.kt`

Keep these test groups:

- `src/test/.../symbol/expression/*`
- `src/test/.../symbol/operation/*`
- `src/test/.../symbol/inequality/*`
- `src/test/.../symbol/polynomial/*`
- `src/test/.../symbol/SymbolIdentityTest.kt`
- `src/test/.../symbol/expression/PropertyPathPathSymbolTest.kt`

Add new tests for:

- direct polynomial parser
- direct inequality parser
- direct polynomial and inequality JSON serde
- DSL direct-to-polynomial behavior
- old input syntax parity where required

## Suggested Commit Sequence

Use small commits. Recommended order:

1. ~~`extract parse and symbol-identity survivors from legacy files`~~ — DONE
2. ~~`introduce direct polynomial and inequality parser`~~ — DONE
3. ~~`rewrite symbol dsl to direct builders`~~ — DONE (removed entirely, no production consumers)
4. ~~`replace legacy expr json serde with direct dto serde`~~ — DONE
5. ~~`remove legacy expr bridge`~~ — DONE
6. ~~`delete legacy expr parser and legacy expr wrappers`~~ — DONE
7. ~~`cleanup docs and tests`~~ — DONE

## Grep Checklist Before Final Delete

Run these searches and drive them to zero before deleting files:

```powershell
Get-ChildItem -Path ospf-kotlin-math\src -Recurse -Filter *.kt |
  Select-String -Pattern 'legacySymbolExpr|parseLegacySymbolExpression|LegacySymbolExpr|legacySymbolExprFromJson|toLegacyExpr\(|legacyToCanonicalPolynomial|legacyToLinearPolynomial|legacyToQuadraticPolynomial|legacyToCanonicalInequality|legacyToLinearInequality|legacyToQuadraticInequality|LegacyExprBridge|symbol\.parser\.Expr'
```

After the final delete, the only remaining hits should be historical notes in docs if intentionally kept. Prefer zero.

## Verification Commands

At the end of each phase, at minimum run:

```powershell
mvn -pl ospf-kotlin-math test
```

Before final handoff, also run:

```powershell
mvn -pl ospf-kotlin-math clean test
mvn -pl ospf-kotlin-math -am test
```

## Acceptance Criteria For The Final State

- no public or internal production code depends on `symbol.parser.Expr`
- no public API returns or accepts legacy `Expr`
- no production code depends on `legacySymbolExpr`, `parseLegacySymbolExpression`, or `legacySymbolExprFromJson`
- no production code depends on `toLegacyExpr` or `legacyTo...`
- `LegacyExprBridge.kt` is deleted
- old parser files are deleted
- old DSL AST builder is deleted or fully rewritten to non-AST internals
- old AST-based serde is deleted
- specialized optimized runtime functions for linear, quadratic, and canonical forms still exist and still back their fast paths
- parser / DSL / serde do not regress to a single generic compatibility runtime implementation
- all tests pass

## Notes

- `PathSymbol` is not the target of this cleanup.
- `ScalarSymbolReference` is not the target of this cleanup.
- The `parseLinearInequality<T>` generic function in `Parser.kt` uses `@Suppress("UNCHECKED_CAST")` because the underlying `parseLinearInequalityRet` only returns `LinearInequality<Flt64>`. This is a pre-existing type-safety limitation, not a regression.

## 2026-04-25 Post-Review Cleanup (Generic/Optimization/Redundancy)

### Scope

- Finish remaining follow-up items after the parser/genericity review in `ospf-kotlin-math`.

### Completed

- [x] Added dedicated `Scale` tests:
  - `src/test/fuookami/ospf/kotlin/math/ScaleTest.kt`
  - Covers single-base exponent update, tidy-to-unit behavior, `Scale * Scale` merge, `Scale / Scale` subtraction, and left/right base separation.
- [x] Removed obvious test warnings in expression DSL tests:
  - cleaned always-true `is` checks in `symbol/expression/dsl/BooleanDslTest.kt`
- [x] Removed unchecked-cast warnings in einsum tests:
  - replaced `MultiArray<Flt64, *>` casts with `MultiArray<*, *>` shape checks in `multiarray/einsum/EinsumTest.kt`
- [x] Removed unchecked cast in production code:
  - `LinearMonomial.abs()` now uses safe generic bound `where T : NumberField<T>, T : Abs<T>`

### API Compatibility / Deprecation Follow-up

- [x] Audited call sites for deprecated inequality serde signatures with `symbolComparator`.
- [x] No external call sites found in `ospf-kotlin-math`; only compatibility overload definitions remain intentionally.

### Verification

- [x] `mvn -pl ospf-kotlin-math test` passed (`711` tests).
- [ ] `mvn test` at repo root is blocked by existing compile errors outside this module:
  - module: `ospf-kotlin-core-plugin-cplex`
  - file: `src/main/fuookami/ospf/kotlin/core/solver/cplex/CplexBendersDecompositionSolver.kt`
  - key errors: unresolved reference `solveFarkasDual`, and `Ok/Failed/Fatal` type argument mismatches.
