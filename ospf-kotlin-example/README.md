# ospf-kotlin-example

:us: English | :cn: [简体中文](README_ch.md)

## Introduction

`ospf-kotlin-example` contains runnable examples and compatibility tests for OSPF Kotlin modeling APIs. It is not published as a library artifact.

## Scope

This module demonstrates:

1. Core optimization model construction and function symbols.
2. Generic numeric examples.
3. Framework modeling patterns and DTO boundaries.
4. Starter compatibility checks.
5. Optional solver-backed integration examples when local solver libraries are available.

## Structure

| Area | Description |
| --- | --- |
| `core_demo` | Core model examples and generic number demonstrations |
| `linear_function` | Linear function symbol build-only and solve examples |
| `quadratic_function` | Quadratic function build-only and solve examples |
| `framework_demo` | Framework-oriented example applications and DTOs |
| `business_compat` | Source compatibility checks for business-facing integration paths |

## Maven Profiles

| Profile | Purpose |
| --- | --- |
| `build-only-function-tests` | Compile and run build-only linear/quadratic function structure tests |
| `core-demo-only` | Compile and run core demo tests |
| `business-source-compat` | Check business source compatibility |
| `framework-starter-compat` | Check framework starter compatibility |
| `solver-integration-tests` | Run solver-backed examples; requires local solver libraries |

## Local Validation

```powershell
mvn -B -ntp -pl ospf-kotlin-example -Pcore-demo-only test
mvn -B -ntp -pl ospf-kotlin-example -Pbuild-only-function-tests test
```

Solver integration tests are environment-sensitive and should be run only when the corresponding solver libraries are installed.
