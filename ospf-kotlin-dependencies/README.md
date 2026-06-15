# ospf-kotlin-dependencies

:us: English | :cn: [简体中文](README_ch.md)

## Introduction

`ospf-kotlin-dependencies` is the dependency-management BOM module for the OSPF Kotlin repository. It centralizes third-party dependency versions and internal module coordinates so downstream Maven modules can inherit a consistent dependency graph.

## Boundary

This module is a build and publishing module only. It does not contain Kotlin source code, public runtime APIs, or domain modeling logic.

## Responsibility

| Area | Description |
| --- | --- |
| Third-party versions | Kotlin, kotlinx, logging, serialization, database, solver, and utility libraries |
| Internal coordinates | OSPF Kotlin core, framework, plugin, domain framework, starter, and example artifacts |
| Dependency convergence | Shared dependency-management baseline for modules that inherit from `ospf-kotlin-parent` |

## Related Modules

- [ospf-kotlin-parent](../ospf-kotlin-parent/README.md)
- [Root project README](../README.md)

## Local Validation

```powershell
mvn -B -ntp -pl ospf-kotlin-dependencies validate
```
