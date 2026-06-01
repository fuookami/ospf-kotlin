# token — Token Management Package

:us: English | :cn: [简体中文](README_ch.md)

## Overview

The `token` package is the core bridging layer in the OSPF framework, connecting **variables** with **solvers**. It manages the mapping between variable items and solver indices, provides dual-view access to solve results (Flt64 solver view ↔ generic V type view), and implements a multi-level caching mechanism for intermediate symbol evaluation and expression flattening.

## Package Structure

```
token/
├── Token.kt                      # Token data structure (variable ↔ solver mapping unit)
├── TokenList.kt                  # Token list (immutable / mutable / auto / manual)
├── TokenTable.kt                 # Token table interface and implementation
├── ConcurrentTokenTable.kt       # Concurrent token table
├── TokenCacheContext.kt          # Cache context (linear flatten / quadratic flatten / value / range)
├── TokenCacheKey.kt              # Cache key
└── TokenTableRegistrationSupport.kt # Token table registration support
```

## Core Concepts

### Token (`Token.kt`)

`Token<V>` is the core data structure representing a variable's registration unit in the solver. It employs a **dual-view pattern**:

- **Flt64 view** (`resultFlt64`) — Solver backends always produce `Flt64`, used for solver-internal interaction
- **V type view** (`result`) — Provides a type-safe public API via the `IntoValue<V>` converter

Core properties:
- `variable` — Associated variable item (`AbstractVariableItem`)
- `solverIndex` — Index in the solver
- `result` / `resultFlt64` — Solve result (dual view)
- `lowerBound` / `upperBound` — Variable bounds
- `range` — Value range (`ValueRange<Flt64>`)

### TokenList (`TokenList.kt`)

Manages the mapping between variables and tokens, providing four implementations:

| Class | Characteristics |
|-------|----------------|
| `TokenList` | Immutable token list, thread-safe |
| `MutableTokenList` | Sealed base class for mutable token lists |
| `AutoTokenList` | Auto-creates tokens on query if not found |
| `ManualTokenList` | Variables must be explicitly added before querying |

All lists maintain a `tokensInSolver` view sorted by `solverIndex`, and support setting solve results via list or map form.

### TokenTable (`TokenTable.kt`)

Token table is a higher-level abstraction combining token lists with intermediate symbol collections, providing:

- **Symbol registration** — Register `IntermediateSymbol` with operation category and duplication validation
- **Token queries** — Find tokens by variable item or index
- **Solution management** — Set / clear solve results
- **Multi-level caching** — Linear flatten, quadratic flatten, value, and range caches
- **Symbol dependency management** — Track symbol dependencies, validate acyclicity

Implementation hierarchy:
- `AbstractTokenTable<V>` — Abstract interface
- `AbstractMutableTokenTable<V>` — Mutable table interface
- `TokenTable<V>` — Immutable implementation
- `MutableTokenTable<V>` — Sealed base class for mutable implementation

### Cache System (`TokenCacheContext.kt`)

Provides four specialized cache contexts:

- **`LinearFlattenContext`** — Linear expression flatten cache (`LinearFlattenData`)
- **`QuadraticFlattenContext`** — Quadratic expression flatten cache (`QuadraticFlattenData`)
- **`ValueCacheContext`** — Solve value cache, separately cached by solution and fixedValues dimensions
- **`RangeCacheContext`** — Expression range cache (`ExpressionRange`)

The `TokenCacheContexts` aggregation container manages all cache contexts uniformly, supporting binding and unbinding of token table contexts by symbol.

### Concurrency Support (`ConcurrentTokenTable.kt`)

Thread-safe token table implementation for multi-threaded solving scenarios.

## Dual-View Design Pattern

```
Solver Backend ──Flt64──▶ Token._result ──IntoValue<V>──▶ Token.result (V?)
                                                  │
                                          Token.resultFlt64 (Flt64?)
```

This design enables:
- Solver plugins only need to handle the unified `Flt64` type
- User API obtains a type-safe V type view via `IntoValue<V>`
- No runtime force-casting required

## Relationships with Other Packages

- **variable** — Token holds a reference to `AbstractVariableItem`, establishing variable-to-solver-index mapping
- **symbol** — Intermediate symbols query and cache evaluation results via TokenTable
- **model** — Models register variables and symbols through TokenTable, and set solve results
- **solver** — Solvers write `Flt64` results via `TokenList.setSolverSolution`