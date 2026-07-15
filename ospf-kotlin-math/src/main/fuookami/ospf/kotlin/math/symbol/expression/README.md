# ospf-kotlin-math/symbol/expression

:us: English | :cn: [简体中文](README_ch.md)

Generic expression AST for SQL-like boolean and scalar expressions. Provides a parallel system to the `symbol.parse` polynomial/inequality parsers with enhanced logical operations.

## Architecture Overview

### New Expression System

| Component | Purpose |
|-----------|---------|
| `PropertyPath` | Unified path abstraction for field/property references (`a.b.c`) |
| `PathSymbol` | Adapter between `PropertyPath` and the `Symbol` interface |
| `ScalarExpression` | AST for scalar values (Constant, Reference, Unary, Binary, Function, **Conditional**, **Boolean**) |
| `BooleanExpression` | AST for boolean logic (And, Or, Not, Comparison, In, PatternMatch, NullCheck) |
| `ExpressionOperator` | Operator definitions (unary, binary, comparison, pattern match) |
| `ScalarParser` | Parses Aviator-compatible scalar expression strings into `ScalarExpression` AST |
| `evaluateScalar` | Public scalar evaluator returning `Ret<Any?>` with injectable function table |
| `MathFunctionEvaluator` | 17 Aviator-whitelisted `math.*` functions |
| `Lexer` / `LexMode` | Tokenizer with Boolean/Scalar modes (controls unary minus handling) |

### Package Structure

```
expression/
├── PropertyPath.kt       # Path abstraction
├── PathSymbol.kt         # Path-Symbol adapter
├── ScalarExpression.kt   # Scalar AST (incl. Conditional, Boolean) + ScalarExpressionFactory
├── BooleanExpression.kt  # Boolean expression AST + BooleanExpressionFactory
├── ExpressionOperator.kt # Operator definitions
├── dsl/                  # DSL construction (ExpressionDsl.kt)
├── parser/               # Lexer (LexMode), BooleanParser (Parser.kt), ScalarParser
├── serde/                # JSON serialization (incl. Conditional/Boolean variants)
└── operation/            # EvaluateScalar, EvaluateBoolean, Normalize, MathFunctions, NumericOps
```

## Relationship to Polynomial/Inequality Parsers

The `expression` package handles SQL-like boolean and scalar expressions, while the `symbol.parse` package handles polynomial and inequality parsing:

1. **`symbol.parse`** - Direct polynomial/inequality parsing (linear, quadratic, canonical)
2. **`BooleanExpression`** - Full boolean logic for SQL expression support

## Usage Examples

### Creating Expressions with DSL

```kotlin
import fuookami.ospf.kotlin.math.symbol.expression.dsl.*

// Boolean expression: (a > 5 and b is not null)
val expr = path("a").gt(5) and path("b").isNotNull()

// With parser
val parsed = parseBooleanExpression("a > 5 and b is not null")
```

### Expression Normalization

```kotlin
import fuookami.ospf.kotlin.math.symbol.expression.operation.*

// And(A, true, And(A)) -> A (after normalize)
val normalized = normalize(complexExpr)
```

### Local Evaluation

```kotlin
import fuookami.ospf.kotlin.math.symbol.expression.operation.*

val expr = Comparison(ComparisonOperator.Gt, 
    ScalarReference(PropertyPath.parse("age")), 
    ScalarConstant(18))

val result = expr.evaluateWith(mapOf("age" to 25))
// result: Trivalent.True
```

### Scalar Expression Parsing

The `ScalarParser` parses Aviator-compatible scalar expression strings into `ScalarExpression` AST. Supports arithmetic, comparison, logic, ternary `? :`, `if/then/else/fi`, and `math.*` functions.

```kotlin
import fuookami.ospf.kotlin.math.symbol.expression.parser.*

// Arithmetic
val sum = parseScalarExpression("x + y").value!!
// -> ScalarBinary(Add, Ref(x), Ref(y))

// Conditional (ternary)
val ternary = parseScalarExpression("x > 0 ? a : b").value!!
// -> ScalarConditional(Comparison(Gt, x, 0), Ref(a), Ref(b))

// if/then/else/fi (condition allows && / || without outer parens)
val cond = parseScalarExpression("if w > 787 then x else y fi").value!!
// -> ScalarConditional(Comparison(Gt, w, 787), Ref(x), Ref(y))

// Boolean as scalar (formula returns boolean)
val boolAsScalar = parseScalarExpression("x > 0 && y > 0").value!!
// -> ScalarBoolean(AndExpression([...]))

// math.* functions (prefix stripped: math.sqrt -> "sqrt")
val fn = parseScalarExpression("math.sqrt(x^2 + y^2)").value!!
// -> ScalarFunction("sqrt", [ScalarBinary(Add, Power(x,2), Power(y,2))])

// Constants resolved at parse time
val pi = parseScalarExpression("math.PI").value!!
// -> ScalarConstant(3.14159...)
```

Operator precedence (high to low): `^`/`**` > unary `+` > `* / %` > `+ -` > comparison > `&&`/`and` > `||`/`or` > ternary/`if`.

Notable behaviors:
- `-x^2` parses as `-(x^2)` (unary minus below power), matching Python/Excel/Aviator.
- `**` and `^` both map to `BinaryOperator.Power`.
- The lexer has two modes: `LexMode.Boolean` (default) merges `-3` into a number; `LexMode.Scalar` emits `MINUS` + `NUMBER`. `parseScalarExpression` uses scalar mode.

### Scalar Evaluation

`evaluateScalar` returns `Ret<Any?>` for explicit error handling, with an injectable function evaluator:

```kotlin
import fuookami.ospf.kotlin.math.symbol.expression.operation.*

val context = MapEvaluationContext.fromStringMap(mapOf("x" to 3.0, "y" to 4.0))

// Basic evaluation
val r1 = evaluateScalar(parseScalarExpression("x + y").value!!, context).value!!  // 7.0

// Conditional evaluation
val r2 = evaluateScalar(
    parseScalarExpression("if x > 0 then x else y fi").value!!,
    context
).value!!  // 3.0

// With math.* functions (inject MathFunctionEvaluator)
val r3 = evaluateScalar(
    parseScalarExpression("math.pow(x, 2) + math.pow(y, 2)").value!!,
    context,
    MathFunctionEvaluator
).value!!  // 25.0

// Convenience extension
val r4 = parseScalarExpression("x * 2").value!!
    .evaluateWith(mapOf("x" to 3.0)).value!!  // 6.0
```

Error semantics: division by zero, unknown functions, type mismatches, and unbound symbol references yield `Failed`. `ScalarSymbolReference` and `ScalarCustom` return `Failed` (opaque). Boolean-in-arithmetic (e.g. `(x > 0) + 1`) returns `Failed` -- no implicit boolean-to-number coercion.

### math.* Functions

`MathFunctionEvaluator` implements `ScalarFunctionEvaluator` with 17 Aviator-whitelisted functions:

| Function | Returns | Notes |
|----------|---------|-------|
| `sqrt`, `exp`, `log`, `log10` | `Double` | `log` is natural log |
| `sin`, `cos`, `tan`, `asin`, `acos`, `atan` | `Double` | radians |
| `floor`, `ceil` | `Double` | |
| `round` | `Long` | aligns with Aviator `math.round(3.7) = 4L` |
| `pow`, `max`, `min` | `Double` | 2-argument |
| `abs` | delegates to `DefaultScalarFunctionEvaluator` | |

`MathFunctionEvaluator` composes with `DefaultScalarFunctionEvaluator` (which provides `abs`, `lower`, `upper`, `trim`, `length`, `coalesce`). To restrict callers to the `math.*` whitelist, validate `ScalarFunction.name` against `MathFunctionEvaluator.supportedFunctions` at the AST level -- do not rely on evaluator rejection, since the composite chain exposes string functions.

### JSON Serialization

```kotlin
import fuookami.ospf.kotlin.math.symbol.expression.serde.*

val json = expr.toJsonString()
val restored = booleanExpressionFromJson(json)
```

### Legacy AST

The legacy `Expr` AST and `LegacyExprBridge` have been removed. Use `BooleanExpression` directly for SQL-like expressions, and `symbol.parse` functions for polynomial/inequality parsing.

## Key Differences from Polynomial/Inequality Parsers

| Feature | `symbol.parse` | `expression` |
|---------|----------------|--------------|
| Focus | Polynomial/inequality | SQL boolean/scalar |
| Boolean logic | None | Full `and/or/not` support |
| Null handling | None | `isNull/isNotNull` operators |
| Pattern match | None | `PatternMatch` with modes |
| In operator | None | `In` set membership |
| Conditional | None | `if/then/else/fi`, ternary `? :` |
| Scalar parsing | None | `parseScalarExpression` (Aviator-compatible) |
| math.* functions | None | `MathFunctionEvaluator` (17 functions) |
| Path reference | Symbol-based | `PropertyPath` with segments |
| Serialization | Direct DTO | `kotlinx.serialization` JSON |
| Normalization | None | Flatten, constant fold, de Morgan |

## Related

- [symbol/README.md](../README.md) - Main symbol module documentation
- [symbol/parse/](../parse/) - Polynomial/inequality parsers
