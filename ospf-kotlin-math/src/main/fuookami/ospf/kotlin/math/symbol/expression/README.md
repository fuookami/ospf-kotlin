# ospf-kotlin-math/symbol/expression

[中文文档 (README_ch.md)](./README_ch.md)

Generic expression AST for SQL-like boolean and scalar expressions. Provides a parallel system to the `symbol.parse` polynomial/inequality parsers with enhanced logical operations.

## Architecture Overview

### New Expression System

| Component | Purpose |
|-----------|---------|
| `PropertyPath` | Unified path abstraction for field/property references (`a.b.c`) |
| `PathSymbol` | Bridge between `PropertyPath` and `Symbol` interface |
| `ScalarExpression` | AST for scalar values (Constant, Reference, Unary, Binary, Function) |
| `BooleanExpression` | AST for boolean logic (And, Or, Not, Comparison, In, PatternMatch, NullCheck) |
| `ExpressionOperator` | Operator definitions (unary, binary, comparison, pattern match) |

### Package Structure

```
expression/
├── PropertyPath.kt       # Path abstraction
├── PathSymbol.kt         # Path-Symbol bridge
├── ScalarExpression.kt   # Scalar expression AST
├── BooleanExpression.kt  # Boolean expression AST
├── ExpressionOperator.kt # Operator definitions
├── ExpressionFactory.kt  # Factory methods
├── dsl/                  # DSL construction
├── parser/               # Lexer and Parser
├── serde/                # JSON serialization
└── operation/            # Normalize, Evaluate
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

### JSON Serialization

```kotlin
import fuookami.ospf.kotlin.math.symbol.expression.serde.*

val json = expr.toJsonString()
val restored = booleanExpressionFromJson(json)
```

### Legacy Bridge

The legacy `Expr` AST and `LegacyExprBridge` have been removed. Use `BooleanExpression` directly for SQL-like expressions, and `symbol.parse` functions for polynomial/inequality parsing.

## Key Differences from Polynomial/Inequality Parsers

| Feature | `symbol.parse` | `expression` |
|---------|----------------|--------------|
| Focus | Polynomial/inequality | SQL boolean/scalar |
| Boolean logic | None | Full `and/or/not` support |
| Null handling | None | `isNull/isNotNull` operators |
| Pattern match | None | `PatternMatch` with modes |
| In operator | None | `In` set membership |
| Path reference | Symbol-based | `PropertyPath` with segments |
| Serialization | Direct DTO | `kotlinx.serialization` JSON |
| Normalization | None | Flatten, constant fold, de Morgan |

## Related

- [symbol/README.md](../README.md) - Main symbol module documentation
- [symbol/parse/](../parse/) - Polynomial/inequality parsers