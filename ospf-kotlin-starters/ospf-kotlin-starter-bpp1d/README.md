# ospf-kotlin-starter-bpp1d

:us: English | :cn: [简体中文](README_ch.md)

## Introduction

`ospf-kotlin-starter-bpp1d` is the starter artifact reserved for one-dimensional bin packing usage.

## Boundary

The current starter depends on the general [ospf-kotlin-starter](../ospf-kotlin-starter/README.md). The BPP1D framework module is still a planned module, so this starter does not yet add a separate BPP1D runtime dependency.

## Usage

```xml
<dependency>
    <groupId>io.github.fuookami.ospf.kotlin</groupId>
    <artifactId>ospf-kotlin-starter-bpp1d</artifactId>
    <version>1.1.0</version>
</dependency>
```

## Local Validation

```powershell
mvn -B -ntp -pl ospf-kotlin-starters/ospf-kotlin-starter-bpp1d -am -DskipTests package
```
