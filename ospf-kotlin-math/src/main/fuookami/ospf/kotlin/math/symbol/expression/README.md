# ospf-kotlin-math/symbol/expression

[中文文档 (README_ch.md)](./README_ch.md)

Generic expression AST for SQL-like boolean and scalar expressions. Provides a parallel system to the legacy `symbol.parser.Expr` with enhanced logical operations.

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

## Migration Strategy

### Coexistence with Legacy `Expr`

The new `expression` package coexists with the legacy `symbol.parser.Expr`:

1. **Legacy `Expr`** - Retained for backward compatibility, serves polynomial/inequality use cases
2. **New `BooleanExpression`** - Enhanced logical operations for SQL expression support
3. **Bridge layer** - `adapter/LegacyExprBridge` provides conversion between systems

### Migration Path

```kotlin
// Legacy (parser.Expr) - arithmetic and comparison focused
val legacyExpr = Expr.parse("x > 5")

// New (BooleanExpression) - full boolean logic support
val newExpr = BooleanExpression.parse("x > 5 and y is not null")

// Bridge conversion (Phase M4)
val converted = LegacyExprBridge.toBooleanExpression(legacyExpr)
```

### Migration Timeline

| Phase | Status | Description |
|-------|--------|-------------|
| M0 | Complete | Scaffolding and package structure |
| M1 | Complete | Core AST (PropertyPath, ScalarExpression, BooleanExpression) |
| M2 | Complete | DSL and Parser |
| M3 | Complete | Serde, Normalize, Evaluate |
| M4 | Complete | Legacy bridge and compatibility layer |
| M5 | In Progress | Documentation |

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

```kotlin
import fuookami.ospf.kotlin.math.symbol.expression.adapter.*

// Convert legacy Expr to new BooleanExpression
val legacyExpr: Expr = Expr.Comparison(...)
val newExpr = legacyExpr.toBooleanExpressionOrNull()

// Convert new expression back to legacy
val backToLegacy = newExpr?.toLegacyExprOrNull()
```

## Key Differences from Legacy Expr

| Feature | Legacy `Expr` | New `Expression` |
|---------|---------------|------------------|
| Boolean logic | Limited | Full `and/or/not` support |
| Null handling | None | `isNull/isNotNull` operators |
| Pattern match | None | `PatternMatch` with modes |
| In operator | None | `In` set membership |
| Path reference | Basic | `PropertyPath` with segments |
| Serialization | Custom | `kotlinx.serialization` JSON |
| Normalization | None | Flatten, constant fold, de Morgan |

## Related

- [symbol/README.md](../README.md) - Main symbol module documentation
- [symbol/parser/README.md](../parser/README.md) - Legacy parser documentation