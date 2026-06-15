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

## Public API

List the stable entry points users should import or depend on.

| API | Responsibility |
| --- | --- |
| `<TypeOrFunction>` | `<responsibility>` |

## Modeling Extensions

For framework modules, explain the context, aggregation, model component, pipeline, extra context, or extra pipeline extension points. If this module is not a framework module, say which extension surface is relevant or omit this section.

## Generic Numeric Boundaries

Explain whether public APIs use generic numeric types such as `V : RealNumber<V>` or `V : FloatingNumber<V>`, and where `Flt64` conversion is allowed.

## Physical Quantity Boundaries

Explain which fields should use `Quantity<V>` or explicit physical-unit wrappers, and which values may remain dimensionless.

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
