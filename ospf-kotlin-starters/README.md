# ospf-kotlin-starters

:us: English | :cn: [简体中文](README_ch.md)

## Introduction

`ospf-kotlin-starters` is the Maven aggregator for starter artifacts. Starter modules provide convenient dependency bundles for users who want a ready-to-use OSPF Kotlin stack without selecting every core, framework, and plugin module manually.

## Boundary

Starter modules should only aggregate dependencies. They should not contain domain logic, solver orchestration, or business APIs.

## Starter Modules

| Module | Purpose |
| --- | --- |
| `ospf-kotlin-starter` | General OSPF Kotlin starter |
| `ospf-kotlin-starter-bpp1d` | 1D bin packing starter |
| `ospf-kotlin-starter-bpp2d` | 2D bin packing starter |
| `ospf-kotlin-starter-bpp3d` | 3D bin packing starter |
| `ospf-kotlin-starter-csp1d` | 1D cutting stock starter |
| `ospf-kotlin-starter-csp2d` | 2D cutting stock starter |
| `ospf-kotlin-starter-gantt-scheduling` | Gantt scheduling starter |
| `ospf-kotlin-starter-network-scheduling` | Network scheduling starter |

## Usage

Most users should depend on one starter that matches their domain, then add a solver plugin when they need a concrete backend.

```xml
<dependency>
    <groupId>io.github.fuookami.ospf.kotlin</groupId>
    <artifactId>ospf-kotlin-starter-csp1d</artifactId>
    <version>1.1.0</version>
</dependency>
```

## Local Validation

```powershell
mvn -B -ntp -pl ospf-kotlin-starters -am -DskipTests package
```
