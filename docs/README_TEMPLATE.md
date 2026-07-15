# <module-name>

:us: English | :cn: [简体中文](README_ch.md)

## Introduction

Briefly describe what this module provides and where it sits in the OSPF Kotlin repository.

## Scope

State what belongs in this module. For framework modules, mention the reusable domain capability and keep downstream business adapters out of scope.

Explicit non-goals:

1. Business-specific DTOs, formula languages, tenant context, and runtime policies.
2. Solver backend implementations unless this is a plugin module.
3. Starter dependency aggregation unless this is a starter module.

## Module Structure

| Package or Sub-module | Responsibility |
| --- | --- |
| `<name>` | `<responsibility>` |

## Architecture Overview

For framework modules, describe the context / aggregation / model component / pipeline flow and the `MetaModel` registration path. For utility or math modules, describe the primary type hierarchy and data-flow boundaries.

<!-- For simple modules, this section can be a short paragraph. For complex modules, consider a diagram or flow description. -->

## Core Concepts

List the concepts a reader must understand before using or extending this module.

<!-- Example:
- **Linear Meta Model**: The optimization model container that holds variables, constraints, and objectives.
- **Shadow Price**: The dual value associated with a constraint, indicating the marginal cost of relaxing it.
-->

## Public API

| API | Responsibility | Stability |
| --- | --- | --- |
| `<TypeOrFunction>` | `<responsibility>` | stable / migration / internal |

<!-- Stability levels:
  - stable: API is production-ready and backward-compatible
  - migration: API is being migrated or will change
  - internal: API is for internal use, not part of public contract
-->

## Modeling Extensions

For framework modules, explain the context, aggregation, model component, pipeline, extra context, or extra pipeline extension points. If this module is not a framework module, say which extension surface is relevant or omit this section.

## Generic Numeric Boundaries

Explain whether public APIs use generic numeric types such as `V : RealNumber<V>` or `V : FloatingNumber<V>`, and where `Flt64` conversion is allowed.

## Physical Quantity Boundaries

Explain which fields should use `Quantity<V>` or explicit physical-unit wrappers, and which values may remain dimensionless.

## Solve Lifecycle

For framework modules, describe the lifecycle as applicable:

1. **Registration**: How variables, constraints, and objectives are registered with the model.
2. **LP/MILP Solving**: How the solver is invoked and configured.
3. **Shadow Price Extraction**: How dual values are obtained from solved constraints.
4. **Column Addition/Removal**: How columns are dynamically added or removed (if applicable).
5. **Final Solve**: How the final optimization pass is triggered.
6. **Solution Extraction**: How results are extracted from the solved model.

<!-- For utility or math modules, omit this section or replace with relevant lifecycle description. -->

## Outputs

Describe the module's primary outputs:

- **Solution**: The optimization result (variable values, objective value).
- **Trace**: Execution trace or solver log (if applicable).
- **KPI**: Key performance indicators extracted from the solution.
- **Render DTO**: Data transfer objects for visualization (if applicable).
- **Diagnostic**: Error reports, validation results, or debugging information.

<!-- Omit subsections that do not apply to this module. -->

## Usage

```kotlin
// Minimal example.
```

## Local Validation

```powershell
mvn -B -ntp -pl <module-name> -am test
```

## Related Modules

- [Root project README](../README.md)
