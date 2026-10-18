# ospf-kotlin-starter-csp2d

:us: English | :cn: [简体中文](README_ch.md)

## Introduction

`ospf-kotlin-starter-csp2d` is the starter artifact for two-dimensional cutting stock usage. It brings in the general
[ospf-kotlin-starter](../ospf-kotlin-starter/README.md) plus every module of
[ospf-kotlin-framework-csp2d](../../ospf-kotlin-framework-csp2d/README.md), and transitively the shared
[ospf-kotlin-framework-geometry2d](../../ospf-kotlin-framework-geometry2d/README.md):

1. `ospf-kotlin-framework-csp2d-infrastructure`
2. `ospf-kotlin-framework-csp2d-domain-material-context`
3. `ospf-kotlin-framework-csp2d-domain-pattern-generation-context`
4. `ospf-kotlin-framework-csp2d-domain-produce-context`
5. `ospf-kotlin-framework-csp2d-domain-yield-context`
6. `ospf-kotlin-framework-csp2d-domain-wasting-minimization-context`
7. `ospf-kotlin-framework-csp2d-domain-sheet-assignment-context`
8. `ospf-kotlin-framework-csp2d-application`

## Boundary

The starter only aggregates dependencies; it contains no code. Solver plugins (for example
`ospf-kotlin-core-plugin-gurobi`) are **not** included, because solver choice is an application concern. Coil cutting,
fixed-size sheet cutting, the three cutting processes and the cut trees all live in the framework modules listed above.

## Usage

```xml
<dependency>
    <groupId>io.github.fuookami.ospf.kotlin</groupId>
    <artifactId>ospf-kotlin-starter-csp2d</artifactId>
    <version>1.1.1</version>
</dependency>
```

## Local Validation

```powershell
mvn -B -ntp -pl ospf-kotlin-starters/ospf-kotlin-starter-csp2d -am -DskipTests package
```
