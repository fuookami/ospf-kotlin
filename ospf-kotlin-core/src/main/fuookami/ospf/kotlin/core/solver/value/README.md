# solver/value — Value Type Conversion Package

:us: English | :cn: [简体中文](README_ch.md)

## Overview

The `value` sub-package provides the **value type conversion** mechanism at the solver boundary in the OSPF framework. It defines the `IntoValue<V>` interface for converting between the solver's standard `Flt64` type and generic value types, along with solve value types and validation utilities.

## Package Structure

```
value/
├── IntoValue.kt                    # Value type conversion interface
├── SolveValue.kt                   # Solve value types
├── SolveValueConversionContext.kt  # Conversion context
└── SolveValueValidation.kt        # Value validation
```

## Core Concepts

### IntoValue (`IntoValue.kt`)

The `IntoValue<V>` interface is the core conversion mechanism at the solver boundary:

- `intoValue(Flt64) → V` — Convert Flt64 to generic value type V
- `fromValue(V) → Flt64` — Convert generic value type V back to Flt64
- `zero` / `one` — V-typed constants (eliminates unsafe `Flt64.zero as V` casts)
- `negativeInfinity` / `infinity` — V-typed infinity values
- `IntoValue.Identity` — Flt64 identity converter (no-op)

Also provides adapter from `Flt64ValueConverter<V>` (math layer) to `IntoValue<V>` (core layer).

### SolveValue (`SolveValue.kt`)

Solve value types wrapping solver results with metadata.

### SolveValueConversionContext (`SolveValueConversionContext.kt`)

Conversion context providing configuration for value type conversion during solving.

### SolveValueValidation (`SolveValueValidation.kt`)

Validation utilities for checking solve value correctness and bounds.

## Relationships with Other Packages

- **token** — `Token<V>` uses `IntoValue<V>` to provide the V-typed `result` view
- **symbol** — Intermediate symbol evaluation uses `IntoValue<V>` for value conversion
- **solver/output** — `FeasibleSolverOutput` provides `convertTo(converter)` using `IntoValue<V>`
- **solver** — Solver `solve()` methods accept `IntoValue<V>` for generic solving