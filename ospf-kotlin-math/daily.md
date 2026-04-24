# 2026-04-24 Legacy Expr Removal Handoff

## Goal

Delete the entire legacy `Expr` stack from `ospf-kotlin-math` without breaking:

- polynomial and inequality parsing
- polynomial and inequality DSL entry points
- JSON serialization and round-trip for polynomial and inequality types
- the new `symbol.expression.*` runtime stack

This document is a handoff plan for the next environment to execute. No code deletion is performed in this handoff.

## Current State

- `ospf-kotlin-math` currently builds (pre-existing errors in `ospf-kotlin-core-plugin-mosek` are unrelated).
- The legacy stack has already been renamed explicitly, but it still exists and still backs production flows.
- One of the original design goals of this symbol system is preserving specialized fast paths for linear, quadratic, and canonical polynomials.
- Legacy `Expr` removal must not delete or regress those polynomial-category-specific optimizations.
- The current legacy public entry points are:
  - `legacySymbolExpr`
  - `parseLegacySymbolExpression`
  - `parseLegacySymbolInequality`
  - `parseLegacySymbolExpressionRet`
  - `parseLegacySymbolInequalityRet`
  - `LegacySymbolExpr`
  - `legacySymbolExprFromJson`
  - `toLegacyExpr`
  - `legacyToCanonicalPolynomial`
  - `legacyToLinearPolynomialOrNull`
  - `legacyToQuadraticPolynomialOrNull`
  - `legacyToCanonicalInequality`
  - `legacyToLinearInequalityOrNull`
  - `legacyToQuadraticInequalityOrNull`

## Files That Are Part Of The Legacy Expr Stack

- `src/main/fuookami/ospf/kotlin/math/symbol/parser/Expr.kt`
- `src/main/fuookami/ospf/kotlin/math/symbol/parser/Lexer.kt`
- `src/main/fuookami/ospf/kotlin/math/symbol/parser/Token.kt`
- `src/main/fuookami/ospf/kotlin/math/symbol/parser/Parser.kt`
- `src/main/fuookami/ospf/kotlin/math/symbol/parser/ParseError.kt`
- `src/main/fuookami/ospf/kotlin/math/symbol/parser/NumberParser.kt`
- `src/main/fuookami/ospf/kotlin/math/symbol/dsl/SymbolDsl.kt`
- `src/main/fuookami/ospf/kotlin/math/symbol/serde/SymbolExpr.kt`
- `src/main/fuookami/ospf/kotlin/math/symbol/expression/adapter/LegacyExprBridge.kt`

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

## Production Dependencies That Still Use Legacy Expr

These are the main reasons the stack cannot simply be deleted today.

- `src/main/fuookami/ospf/kotlin/math/symbol/dsl/SymbolDsl.kt`
  - `linearPolynomial`
  - `quadraticPolynomial`
  - `canonicalPolynomial`
  - `linearInequality`
  - `quadraticInequality`
  - `canonicalInequality`
- `src/main/fuookami/ospf/kotlin/math/symbol/parser/Parser.kt`
  - `parseCanonical`
  - `parseLinear`
  - `parseQuadratic`
  - `parseLinearInequality`
  - `parseQuadraticInequality`
- `src/main/fuookami/ospf/kotlin/math/symbol/serde/SymbolExpr.kt`
  - polynomial and inequality `toJsonString`
  - `linearPolynomialFromJson`
  - `quadraticPolynomialFromJson`
  - `canonicalPolynomialFromJson`
  - `linearInequalityFromJson`
  - `quadraticInequalityFromJson`
  - `canonicalInequalityFromJson`

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
- no production code imports `parseLegacySymbolExpression*` — **NOT YET**: `parser/Parser.kt` still contains `parseLegacySymbolExpression`/`parseLegacySymbolInequality` functions (deprecated but present)
- all parse tests are green after being rewritten to the new direct parser — **DONE**: `DirectPolynomialParserTest.kt` passes

The remaining `parser.Expr` imports in `SymbolExpr.kt` are for JSON serde, which is Phase 4's scope. The `parseLegacySymbolExpression*` functions are deprecated bridges that can be deleted in Phase 6 after all callers are migrated.

### Files To Delete At End Of Phase 2 (deferred to Phase 6)

- `src/main/fuookami/ospf/kotlin/math/symbol/parser/Expr.kt`
- `src/main/fuookami/ospf/kotlin/math/symbol/parser/Lexer.kt`
- `src/main/fuookami/ospf/kotlin/math/symbol/parser/Token.kt`
- `src/main/fuookami/ospf/kotlin/math/symbol/parser/Parser.kt`
- `src/main/fuookami/ospf/kotlin/math/symbol/parser/ParseError.kt`
- `src/main/fuookami/ospf/kotlin/math/symbol/parser/NumberParser.kt`

Only delete these after Phase 3 and Phase 4 are complete and no production code references them.

## Phase 3: Replace DSL Flows — NOT STARTED

### Problem

`symbol.dsl` is still an AST builder for legacy `Expr`.

### Target

`symbol.dsl` should become one of these:

- direct polynomial and inequality DSL only
- or be removed entirely if you do not want to keep symbolic polynomial DSL

Recommended option:

- keep the package
- keep the high-level shortcuts
- remove raw generic AST building
- make the linear and quadratic shortcuts build their own structures directly in the final state

### Keep

- `linearPolynomial`
- `quadraticPolynomial`
- `canonicalPolynomial`
- `linearInequality`
- `quadraticInequality`
- `canonicalInequality`

### Remove

- `legacySymbolExpr`
- `symbolExpr`
- all `Expr`-typed operator overloads
- all `Expr`-typed `num`, `symbol`, `call`

### Replacement Strategy

Introduce internal DSL nodes inside `SymbolDsl.kt` or split into:

- `PolynomialDsl.kt`
- `InequalityDsl.kt`

These internal nodes should be private or internal.

They may model:

- literal constants
- symbolic variables
- unary minus
- add/subtract/multiply/power
- comparison

But they should not be exposed as a public generic AST.

Acceptance for Phase 3:

- `symbol.dsl` public API no longer exposes `Expr`
- `SymbolDsl.kt` no longer imports `symbol.parser.Expr`
- DSL tests are rewritten and passing

## Phase 4: Replace JSON Serde Flows — NOT STARTED

### Problem

`SymbolExpr.kt` currently uses legacy `Expr` as the JSON interchange form for polynomial and inequality types.

### Target

Split `SymbolExpr.kt` into:

- `SymbolIdentitySerde.kt` (already extracted in Phase 1C)
- `PolynomialSerde.kt`
- `InequalitySerde.kt`

### Recommended Strategy

Use direct DTOs for polynomial and inequality JSON instead of any generic AST.

Important:

- `LinearPolynomial` JSON should restore directly to `LinearPolynomial`
- `QuadraticPolynomial` JSON should restore directly to `QuadraticPolynomial`
- `LinearInequality` JSON should restore directly to `LinearInequality`
- `QuadraticInequality` JSON should restore directly to `QuadraticInequality`
- do not make the final linear/quadratic JSON paths deserialize to canonical first unless temporary compatibility demands it

Recommended DTO shapes:

- `CanonicalPolynomialData`
- `LinearPolynomialData`
- `QuadraticPolynomialData`
- `CanonicalInequalityData`
- `LinearInequalityData`
- `QuadraticInequalityData`

If wire compatibility with the current JSON matters, decide this before implementation.

Decision gate:

- if wire compatibility matters, preserve current JSON shape with internal compatibility DTOs and golden tests
- if wire compatibility does not matter, switch to direct canonical DTOs and update docs

### Remove After Migration

- `LegacySymbolExpr`
- `SymbolExpr`
- `legacySymbolExprFromJson`
- `symbolExprFromJson`
- `toLegacyExpr`
- `toExpr`
- all `legacyTo...` conversion helpers

Acceptance for Phase 4:

- polynomial and inequality `toJsonString` no longer call `toLegacyExpr`
- `fromJson` no longer parses legacy `Expr`
- symbol identity serde still works
- linear and quadratic JSON round-trip still land on their specialized optimized runtime paths

## Phase 5: Delete LegacyExprBridge — NOT STARTED

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

## Phase 6: Delete Legacy Files — NOT STARTED

Delete these files after Phases 2-5 are complete and no production code references them:

- `src/main/fuookami/ospf/kotlin/math/symbol/parser/Expr.kt`
- `src/main/fuookami/ospf/kotlin/math/symbol/parser/Lexer.kt`
- `src/main/fuookami/ospf/kotlin/math/symbol/parser/Token.kt`
- `src/main/fuookami/ospf/kotlin/math/symbol/parser/Parser.kt`
- `src/main/fuookami/ospf/kotlin/math/symbol/parser/ParseError.kt`
- `src/main/fuookami/ospf/kotlin/math/symbol/parser/NumberParser.kt`
- `src/main/fuookami/ospf/kotlin/math/symbol/dsl/SymbolDsl.kt` (if fully rewritten, keep the new version)
- `src/main/fuookami/ospf/kotlin/math/symbol/serde/SymbolExpr.kt` (if fully split, delete the old monolith)

## Phase 7: Documentation Cleanup — NOT STARTED

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

## Phase 8: Test Migration Checklist — NOT STARTED

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
3. `rewrite symbol dsl to direct builders`
4. `replace legacy expr json serde with direct dto serde`
5. `remove legacy expr bridge`
6. `delete legacy expr parser and legacy expr wrappers`
7. `cleanup docs and tests`

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

## Notes For The Next Environment

- Do not start with file deletion. Start with replacement paths.
- `PathSymbol` is not the target of this cleanup.
- `ScalarSymbolReference` is not the target of this cleanup.
- `NumberParser`, `ParseResult`, `ParseIssue`, and symbol identity serde probably survive, but must move out of legacy files first.
- If JSON schema compatibility is important, decide that before rewriting serde. That choice affects the whole migration.
- Phase 1 is fully done. Phase 2 is partially done — the parser delegation works, but `SymbolExpr.kt` still imports `parser.Expr` for JSON serde (Phase 4 scope). The next environment should start with Phase 3 (DSL rewrite).
- `parser/NumberParser.kt` and `parser/ParseError.kt` are now `@Deprecated` typealias bridges. They can be deleted in Phase 6 after all callers are migrated.
- The `parseLinearInequality<T>` generic function in `Parser.kt` uses `@Suppress("UNCHECKED_CAST")` because the underlying `parseLinearInequalityRet` only returns `LinearInequality<Flt64>`. This is a pre-existing type-safety limitation, not a regression.
