# demo3 — One-Dimensional Cutting Stock Problem (CSP1D)

:us: English | :cn: [简体中文](README_ch.md)

## Introduction

`demo3` demonstrates the **One-Dimensional Cutting Stock Problem (CSP1D)** solved via column generation with the SCIP solver. Given a set of raw materials with fixed width and a set of products with specific width demands, the goal is to find cutting patterns that minimize the number of raw materials used while satisfying all product demands.

## Scope

- Define raw materials (rolls/sheets) with fixed width.
- Define products with width requirements and demand quantities.
- Generate cutting patterns (columns) using pricing sub-problem.
- Solve the master problem with column generation using SCIP.
- Use physical quantities (`Quantity<Flt64>` with `Meter` unit) for widths.

## Module Structure

| File | Description |
| --- | --- |
| `Main.kt` | Entry point: `CSP` class that defines the problem and solves it |

## Problem Description

The CSP1D problem:
- **Input**: Raw material width (e.g., 1000mm) and a list of products with `(width, demand)` pairs.
- **Output**: Cutting patterns that minimize the number of raw materials used.
- **Method**: Column generation — master problem selects patterns, pricing sub-problem generates new patterns with negative reduced cost.

### Example Data

| Product | Width (mm) | Demand |
| --- | --- | --- |
| p-0 | 450 | 97 |
| p-1 | 360 | 610 |
| p-2 | 310 | 395 |
| p-3 | 140 | 211 |

## Architecture

This demo uses the `csp1d` framework module:

```
ospf-kotlin-framework-csp1d/
  csp1d-application/
    model/Csp1dConfiguration.kt  -- Problem configuration
    model/csp1dProblem.kt        -- Problem definition
    service/Csp1dColumnGeneration.kt  -- Column generation algorithm
  csp1d-domain-material/
    model/Material.kt            -- Raw material definition
    model/Product.kt             -- Product definition
  csp1d-domain-cutting_plan_generation/
    model/GenerationConstraints.kt  -- Generation constraints
    service/FullSumGenerator.kt     -- Full sum pattern generator
    service/NSameGenerator.kt       -- N-same pattern generator
    ReducedCostPricingGenerator.kt  -- Pricing sub-problem
```

## Usage

```kotlin
import fuookami.ospf.kotlin.example.framework_demo.demo3.CSP

suspend fun main() {
    val csp = CSP()
    val result = csp()
    when (result) {
        is Ok -> println("Solved successfully")
        is Failed -> println("Failed: ${result.error}")
        is Fatal -> println("Fatal: ${result.errors}")
    }
}
```

## Local Validation

```powershell
mvn -B -ntp -pl ospf-kotlin-example -Pcore-demo-only test
```
