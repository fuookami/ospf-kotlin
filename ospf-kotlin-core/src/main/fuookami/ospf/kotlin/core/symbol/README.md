# symbol — Intermediate Symbol Package

:us: English | :cn: [简体中文](README_ch.md)

## Overview

The `symbol` package is the **symbolic expression layer** of the OSPF framework, defining the intermediate symbol system for mathematical optimization models. Intermediate symbols encapsulate expression evaluation logic, supporting caching, dependency tracking, and bound management. The package also provides a rich function symbol library (`function` sub-package) for building complex constraint expressions.

## Package Structure

```
symbol/
├── IntermediateSymbol.kt                  # Core intermediate symbol interfaces
├── IntermediateSymbolExpressionSupport.kt  # Expression support for intermediate symbols
├── QuantitySymbolConversion.kt             # Quantity symbol conversion
├── SolverBoundaryCasts.kt                  # Solver boundary type casts
├── SymbolCombination.kt                    # Multi-array-based symbol combination containers
├── flatten/
│   └── FlattenUtility.kt                  # Expression flattening utilities
└── function/                              # Function symbol library (see function/README.md)
```

## Core Concepts

### Intermediate Symbol Interface (`IntermediateSymbol.kt`)

Intermediate symbols are **evaluable symbolic expressions** in mathematical optimization models. They are not direct decision variables but derived values composed of variables and constants.

#### Interface Hierarchy

```
IntermediateSymbol<V>              — Base interface
├── LinearIntermediateSymbol<V>    — Linear intermediate symbol (convertible to linear polynomial)
└── QuadraticIntermediateSymbol<V> — Quadratic intermediate symbol (convertible to quadratic polynomial)
```

#### `IntermediateSymbol<V>` Core Capabilities

| Capability | Method/Property | Description |
|------------|----------------|-------------|
| Evaluation | `prepare()`, `evaluate()` | Evaluate via TokenTable and fixed values |
| Caching | `prepareAndCache()`, `cached`, `flush()` | Cache evaluation results with lazy refresh |
| Dependency tracking | `dependencies`, `parent` | Track inter-symbol dependencies |
| Bound management | `range`, `lowerBound`, `upperBound`, `fixedValue` | Expression value domain |
| Registration | `registerAuxiliaryTokens()` | Register auxiliary tokens to collection |
| Serialization | `toRawString()` | Get raw string representation |

#### `LinearIntermediateSymbol<V>`

An intermediate symbol convertible to a linear polynomial (`LinearPolynomial<V>`). Provides:
- `polynomial` — The associated linear polynomial
- `asMutable()` — Get mutable linear polynomial representation

#### `QuadraticIntermediateSymbol<V>`

An intermediate symbol convertible to a quadratic polynomial (`QuadraticPolynomial<V>`). Provides:
- `polynomial` — The associated quadratic polynomial
- `asMutable()` — Get mutable quadratic polynomial representation

### Intermediate-Variable Realization of Expression Symbols

`LinearExpressionSymbol` / `QuadraticExpressionSymbol` expose the `useIntermediateVariable` configuration (default `false`) controlling how the expression is realized when a meta model is dumped into a mechanism model:

| Configuration | Dump result |
|---------------|-------------|
| `false` (default) | In-place expansion: every use site (constraints, objectives) is replaced by the definition expression |
| `true` | Intermediate-variable realization: one auxiliary continuous variable `"${name}__realization"` plus an equality constraint `aux == definition` are generated, and use sites reference `aux` |

Notes:

- Set the configuration before adding the symbol to a model (`model.add(symbol)`) or before the first dump; re-dump after changing it.
- The symbol must be added to the model (same precondition as function symbols), otherwise the auxiliary variable is not registered and the definition constraint is skipped.
- Symbol values, `range`, and `discrete` are still derived from the definition and the underlying variables, so post-solve `evaluate()` is unaffected.
- Other realized symbols inside the definition appear through their own auxiliary variables in the definition constraint (nesting is supported).
- Combining symbols into polynomials through the `+` / `-` operators still inlines the definition (equivalent result, just without the auxiliary variable).

### Symbol Combination (`SymbolCombination.kt`)

`MultiArray`-based multi-dimensional symbol combination containers for batch creation and management of intermediate symbols.

- **`SymbolCombination<Sym, S>`** — Symbol combination that automatically sets group references and indices for each element on creation
- **`QuantitySymbolCombination<Sym, S>`** — Quantity symbol combination

Provides `LinearIntermediateSymbols` and `QuadraticIntermediateSymbols` factory objects supporting 1D through 4D and dynamic dimensions. Also provides `map` / `flatMap` convenience functions for creating symbol combinations from iterables.

Type aliases:
- `LinearExpressionSymbols1~4<V>`, `DynLinearExpressionSymbols<V>`
- `QuadraticExpressionSymbols1~4<V>`, `DynQuadraticExpressionSymbols<V>`
- And corresponding `Quantity...` variants

### Expression Support (`IntermediateSymbolExpressionSupport.kt`)

Provides extension support for expression operations on intermediate symbols, including arithmetic operations and expression construction between symbols.

### Solver Boundary Casts (`SolverBoundaryCasts.kt`)

Provides safe type conversions between solver boundary types, handling boundary value passing across different numeric types.

### Expression Flattening (`flatten/FlattenUtility.kt`)

Flattens intermediate symbol expressions into solver-consumable monomial list forms, serving as the bridge between the symbol layer and the model layer.

## function Sub-package — Function Symbol Library

The `function` sub-package provides 30+ function symbols for building complex constraints in optimization models. See [function/README.md](function/README.md).

Main categories:

| Category | Function Symbols | Description |
|----------|-----------------|-------------|
| Slack | `Slack`, `SlackRange` | Slack variables and slack ranges |
| Rounding | `Ceiling`, `Floor`, `Rounding` | Ceiling / floor / round |
| Min/Max | `Max`, `MinMax`, `QuadraticMin` | Maximum and minimum values |
| Conditional | `If`, `IfIn`, `IfThen` | Conditional expressions |
| Logic | `And`, `Imply`, `OneOf`, `SameAs` | Logical operations |
| Conversion | `Binaryzation`, `BalanceTernaryzation`, `Semi` | Binary, ternary, semi-continuous |
| Piecewise | `UnivariateLinearPiecewise`, `BivariateLinearPiecewise` | Linear piecewise functions |
| Range | `InStepRange`, `Masking`, `QuadraticMaskingRange` | Step ranges, masking |
| Math | `Abs`, `Mod`, `Product`, `Sigmoid`, `Sin`, `Cos` | Mathematical functions |
| Constraint | `Inequality`, `SatisfiedAmount`, `BigM` | Inequalities, satisfaction count, big-M |

## Relationships with Other Packages

- **variable** — Intermediate symbol evaluation ultimately depends on variable items (via TokenTable)
- **token** — Intermediate symbols query variable solve results and cache evaluation data via `TokenTable`
- **model** — Models register intermediate symbols to TokenTable; symbol flattening generates constraints
- **solver** — Solver boundary casts ensure symbol boundaries are correctly passed to solvers