# ospf-kotlin-starter-csp1d

:us: English | :cn: [简体中文](README_ch.md)

## Introduction

`ospf-kotlin-starter-csp1d` is the starter artifact for the CSP1D framework stack.

## Included Modules

This starter depends on the general [ospf-kotlin-starter](../ospf-kotlin-starter/README.md) and the CSP1D framework modules:

| Module | Purpose |
| --- | --- |
| `ospf-kotlin-framework-csp1d-infrastructure` | Shared render DTOs and infrastructure types |
| `ospf-kotlin-framework-csp1d-domain-material-context` | Products, materials, machines, demands, and cutting plans |
| `ospf-kotlin-framework-csp1d-domain-produce-context` | Master problem variables and production modeling |
| `ospf-kotlin-framework-csp1d-domain-cutting-plan-generation-context` | Cutting-plan generation and pricing |
| `ospf-kotlin-framework-csp1d-domain-yield-context` | Yield constraints and objectives |
| `ospf-kotlin-framework-csp1d-domain-length-assignment-context` | Dynamic length assignment |
| `ospf-kotlin-framework-csp1d-domain-wasting-minimization-context` | Waste analysis and penalties |
| `ospf-kotlin-framework-csp1d-application` | Application-level solving workflows |

## Usage

```xml
<dependency>
    <groupId>io.github.fuookami.ospf.kotlin</groupId>
    <artifactId>ospf-kotlin-starter-csp1d</artifactId>
    <version>1.1.0</version>
</dependency>
```

## Local Validation

```powershell
mvn -B -ntp -pl ospf-kotlin-starters/ospf-kotlin-starter-csp1d -am -DskipTests package
```
