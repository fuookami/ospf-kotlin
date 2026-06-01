# variable — Variable Definition Package

:us: English | :cn: [简体中文](README_ch.md)

## Overview

The `variable` package defines the **variable system** for mathematical optimization models in OSPF (Open Solver Platform Framework). It provides abstract base classes for variable items, a variable type system, variable combination containers, independent variable items, and variable range definitions — forming the foundational infrastructure layer for building optimization models.

## Package Structure

```
variable/
├── AbstractVariableItem.kt    # Abstract base class for variable items
├── AnyVariable.kt             # Generic variable type
├── Type.kt                    # Variable type system
├── VariableCombinationItem.kt # Multi-array-based variable combination
├── VariableIndependentItem.kt # Independent variable item
└── VariableRange.kt           # Variable range definition
```

## Core Concepts

### Variable Type System (`Type.kt`)

Defines a complete variable type hierarchy using sealed classes for type safety:

- **`Binary`** — Binary variable {0, 1} (underlying type `UInt8`)
- **`Ternary`** — Ternary variable {0, 1, 2} (`UInt8`)
- **`BalancedTernary`** — Balanced ternary variable {-1, 0, 1} (`Int8`)
- **`Percentage`** — Percentage variable [0, 1] (`Flt64`)
- **Integer types** — `Int8` ~ `Int256`, `UInt8` ~ `UInt256`
- **Continuous types** — `Flt32`, `Flt64`

Types are classified by interfaces:
- `IntegerVariableType<T>` — Signed integer variables
- `UIntegerVariableType<T>` — Unsigned integer variables
- `ContinuesVariableType<T>` — Signed continuous variables
- `UContinuesVariableType<T>` — Unsigned continuous variables

Each type provides `minimum`, `maximum` bounds and `RealNumberConstants<T>` definitions.

### Variable Item (`AbstractVariableItem.kt`)

Abstract base class for all variable items, defining core properties:
- `key` — Unique variable key (`VariableItemKey`)
- `name` — Variable name
- `type` — Variable type (`VariableType<*>`)
- `lowerBound` / `upperBound` — Variable bounds
- `fixedValue` — Fixed value (for variable fixing strategies)

### Independent Variable Item (`VariableIndependentItem.kt`)

Implementation for standalone scalar decision variables in the model.

### Variable Combination Item (`VariableCombinationItem.kt`)

`MultiArray`-based multi-dimensional variable combination container. Enables batch creation and management of variables, supporting 1D through 4D as well as dynamic dimensions. Each variable in the combination automatically receives a group reference and index.

### Variable Range (`VariableRange.kt`)

Defines the value domain range for variables, including lower bound, upper bound, and fixed value information, used for solver boundary passing and validation.

## Relationships with Other Packages

- **token** — `Token` holds a reference to `AbstractVariableItem`, establishing the mapping between variables and solver indices
- **symbol** — Intermediate symbol evaluation depends on variable item solve results
- **model** — Models register and manage decision variables through variable items