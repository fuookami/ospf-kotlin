# ospf-kotlin-starter-bpp2d

:us: English | :cn: [简体中文](README_ch.md)

## Introduction

`ospf-kotlin-starter-bpp2d` is the starter artifact for two-dimensional bin packing usage.

## Boundary

The current starter depends on the general [ospf-kotlin-starter](../ospf-kotlin-starter/README.md). The BPP2D framework is still early-stage; users who need its current experimental API can depend on `ospf-kotlin-framework-bpp2d` directly.

## Usage

```xml
<dependency>
    <groupId>io.github.fuookami.ospf.kotlin</groupId>
    <artifactId>ospf-kotlin-starter-bpp2d</artifactId>
    <version>1.1.0</version>
</dependency>
```

## Local Validation

```powershell
mvn -B -ntp -pl ospf-kotlin-starters/ospf-kotlin-starter-bpp2d -am -DskipTests package
```
