# ospf-kotlin-starter-bpp2d

:us: English | :cn: [简体中文](README_ch.md)

## Introduction

`ospf-kotlin-starter-bpp2d` is the starter artifact for two-dimensional bin packing usage. It brings in the general
[ospf-kotlin-starter](../ospf-kotlin-starter/README.md) plus every module of
[ospf-kotlin-framework-bpp2d](../../ospf-kotlin-framework-bpp2d/README.md):

1. `ospf-kotlin-framework-bpp2d-infrastructure`
2. `ospf-kotlin-framework-bpp2d-domain-item-context`
3. `ospf-kotlin-framework-bpp2d-domain-bla-context`
4. `ospf-kotlin-framework-bpp2d-domain-block-loading-context`
5. `ospf-kotlin-framework-bpp2d-domain-layer-generation-context`
6. `ospf-kotlin-framework-bpp2d-domain-layer-assignment-context`
7. `ospf-kotlin-framework-bpp2d-domain-packing-context`
8. `ospf-kotlin-framework-bpp2d-application`

## Boundary

The starter only aggregates dependencies; it contains no code. Solver plugins (for example
`ospf-kotlin-core-plugin-gurobi`) are **not** included, because solver choice is an application concern. Rectangle and
circle packing, column generation and renderer DTO output all live in the framework modules listed above.

## Usage

```xml
<dependency>
    <groupId>io.github.fuookami.ospf.kotlin</groupId>
    <artifactId>ospf-kotlin-starter-bpp2d</artifactId>
    <version>1.1.1</version>
</dependency>
```

## Local Validation

```powershell
mvn -B -ntp -pl ospf-kotlin-starters/ospf-kotlin-starter-bpp2d -am -DskipTests package
```
