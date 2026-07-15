# ospf-kotlin-starter

:us: English | :cn: [简体中文](README_ch.md)

## Introduction

`ospf-kotlin-starter` is the general starter artifact for OSPF Kotlin. It aggregates the common runtime modules needed for core optimization modeling and framework-level infrastructure.

## Boundary

This module only aggregates dependencies. It should not contain runtime code, domain logic, or solver backend implementation.

## Included Modules

| Module | Purpose |
| --- | --- |
| `ospf-kotlin-utils` | Common utilities and functional result types |
| `ospf-kotlin-multiarray` | Multi-dimensional array foundation |
| `ospf-kotlin-math` | Mathematical and symbolic foundations |
| `ospf-kotlin-quantities` | Physical quantity and unit support |
| `ospf-kotlin-core` | Optimization modeling core |
| `ospf-kotlin-framework` | Shared framework abstractions |

## Usage

```xml
<dependency>
    <groupId>io.github.fuookami.ospf.kotlin</groupId>
    <artifactId>ospf-kotlin-starter</artifactId>
    <version>1.1.0</version>
</dependency>
```

Add a concrete solver plugin separately when solving with a backend.

## Local Validation

```powershell
mvn -B -ntp -pl ospf-kotlin-starters/ospf-kotlin-starter -am -DskipTests package
```
