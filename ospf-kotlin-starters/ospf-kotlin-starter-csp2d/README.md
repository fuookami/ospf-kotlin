# ospf-kotlin-starter-csp2d

:us: English | :cn: [简体中文](README_ch.md)

## Introduction

`ospf-kotlin-starter-csp2d` is the starter artifact reserved for two-dimensional cutting stock usage.

## Boundary

The current starter depends on the general [ospf-kotlin-starter](../ospf-kotlin-starter/README.md). The CSP2D framework module is still planned, so this starter does not yet add a separate CSP2D runtime dependency.

## Usage

```xml
<dependency>
    <groupId>io.github.fuookami.ospf.kotlin</groupId>
    <artifactId>ospf-kotlin-starter-csp2d</artifactId>
    <version>1.1.0</version>
</dependency>
```

## Local Validation

```powershell
mvn -B -ntp -pl ospf-kotlin-starters/ospf-kotlin-starter-csp2d -am -DskipTests package
```
