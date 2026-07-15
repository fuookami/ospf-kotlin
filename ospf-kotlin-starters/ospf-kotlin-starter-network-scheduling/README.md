# ospf-kotlin-starter-network-scheduling

:us: English | :cn: [简体中文](README_ch.md)

## Introduction

`ospf-kotlin-starter-network-scheduling` is the starter artifact reserved for network scheduling usage.

## Boundary

The current starter depends on the general [ospf-kotlin-starter](../ospf-kotlin-starter/README.md). The network scheduling framework module is still planned, so this starter does not yet add a separate network scheduling runtime dependency.

## Usage

```xml
<dependency>
    <groupId>io.github.fuookami.ospf.kotlin</groupId>
    <artifactId>ospf-kotlin-starter-network-scheduling</artifactId>
    <version>1.1.0</version>
</dependency>
```

## Local Validation

```powershell
mvn -B -ntp -pl ospf-kotlin-starters/ospf-kotlin-starter-network-scheduling -am -DskipTests package
```
