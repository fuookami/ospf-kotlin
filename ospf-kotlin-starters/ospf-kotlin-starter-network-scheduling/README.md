# ospf-kotlin-starter-network-scheduling

:us: English | :cn: [简体中文](README_ch.md)

## Introduction

`ospf-kotlin-starter-network-scheduling` is the starter artifact for the network scheduling framework. It brings in all six network-scheduling modules plus the base starter:

- `ospf-kotlin-framework-network-scheduling-infrastructure` — generic graph, arc, flow primitives, value adapter
- `ospf-kotlin-framework-network-scheduling-domain-vrp-context` — VRPTW instances, routes, validators, cost policies, BranchMask and VRP time value objects
- `ospf-kotlin-framework-network-scheduling-domain-flow-context` — single/multi-commodity flow, capacity bounds, conservation, and minimum-cost objective
- `ospf-kotlin-framework-network-scheduling-domain-route-generation-context` — ESPPRC pricer, pricing graph
- `ospf-kotlin-framework-network-scheduling-domain-route-compilation-context` — column pool, CG lifecycle, shadow prices
- `ospf-kotlin-framework-network-scheduling-application` — Branch-and-Price algorithm, application service

Solver plugins (e.g. `ospf-kotlin-core-plugin-gurobi`) are **not** included; add the one you need separately.

## Usage

```xml
<dependency>
    <groupId>io.github.fuookami.ospf.kotlin</groupId>
    <artifactId>ospf-kotlin-starter-network-scheduling</artifactId>
    <version>1.1.0</version>
</dependency>
```

For a Gurobi-backed VRPTW Branch-and-Price solver, also add:

```xml
<dependency>
    <groupId>io.github.fuookami.ospf.kotlin</groupId>
    <artifactId>ospf-kotlin-core-plugin-gurobi</artifactId>
    <version>1.1.0</version>
</dependency>
```

## Local Validation

```powershell
mvn -B -ntp -pl ospf-kotlin-starters/ospf-kotlin-starter-network-scheduling -am -DskipTests package
```
