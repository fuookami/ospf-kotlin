# core_demo

:us: English | :cn: [简体中文](README_ch.md)

## Introduction

`core_demo` contains runnable optimization model construction demos that showcase the OSPF Kotlin core APIs. Each demo builds a linear or quadratic meta-model, registers
variables/symbols/constraints/objectives, solves it, and extracts the solution. `GenericNumberDemo` verifies that the model builders work correctly across multiple numeric types (
`Flt64`, `Rtn64`, `FltX`, `RtnX`).

## Scope

- Demo1 through Demo17 model various classic optimization problems using `LinearMetaModel`, intermediate expression symbols, and the SCIP solver.
- `GenericNumberDemo` builds both linear and quadratic models with four numeric types and extracts mechanism-level summaries without solving.

## Module Structure

| File                   | Problem Type                                                                                                                                                                                        |
|------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `Demo1.kt`             | Capital investment selection -- maximize profit subject to capital and liability constraints (binary variables)                                                                                     |
| `Demo2.kt`             | Assignment problem -- assign products to companies minimizing total cost (binary variables)                                                                                                         |
| `Demo3.kt`             | Production planning / blending -- minimize material cost while meeting product yield requirements (unsigned integer variables)                                                                      |
| `Demo4.kt`             | Production optimization -- maximize profit with material availability and production difference constraints (real variables)                                                                        |
| `Demo5.kt`             | 0-1 knapsack -- maximize cargo value subject to a weight limit (binary variables)                                                                                                                   |
| `Demo6.kt`             | Bounded knapsack -- maximize cargo value with per-item quantity limits (unsigned integer variables)                                                                                                 |
| `Demo7.kt`             | Transportation problem -- minimize shipping cost from warehouses to stores (unsigned integer variables)                                                                                             |
| `Demo8.kt`             | Production planning with equipment man-hours -- maximize profit subject to equipment capacity constraints (unsigned integer variables)                                                              |
| `Demo9.kt`             | Facility location -- find the point minimizing total Manhattan distance to settlements (integer variables, absolute value decomposition)                                                            |
| `Demo10.kt`            | Traveling Salesman Problem (TSP) -- shortest route visiting all cities exactly once, with MTZ subtour elimination (binary + integer variables)                                                      |
| `Demo11.kt`            | Maximum flow -- maximize flow from source to sink in a capacitated network (unsigned integer variables)                                                                                             |
| `Demo12.kt`            | Investment portfolio -- maximize yield subject to risk and fund allocation constraints, using binaryzation and max functions (unsigned integer variables)                                           |
| `Demo13.kt`            | Vehicle routing -- minimize distribution distance from centers to dealers with truck capacity constraints (unsigned integer variables)                                                              |
| `Demo14.kt`            | Multi-commodity distribution -- minimize shipping cost with production, sales, and transshipment nodes (unsigned integer variables)                                                                 |
| `Demo15.kt`            | Multi-plant distribution with substitution -- minimize logistics cost from manufacturers to distribution centers with car model substitution rules (unsigned integer + percentage variables)        |
| `Demo16.kt`            | Production scheduling -- minimize production, storage, and delay delivery costs across monthly periods (unsigned integer variables)                                                                 |
| `Demo17.kt`            | Vehicle Routing Problem with Time Windows (VRPTW) -- minimize fixed and travel costs for a fleet visiting demand nodes with time window and capacity constraints (binary + unsigned real variables) |
| `GenericNumberDemo.kt` | Generic numeric type verification -- builds linear and quadratic models with `Flt64`, `Rtn64`, `FltX`, and `RtnX` to confirm cross-type correctness                                                 |

## Usage

Each demo is a Kotlin `data object` or `class` with a suspending `invoke()` operator. To run a demo:

```kotlin
import fuookami.ospf.kotlin.example.core_demo.Demo1

suspend fun main() {
    val result = Demo1()
    when (result) {
        is Ok -> println("Solved successfully")
        is Failed -> println("Failed: ${result.error}")
        is Fatal -> println("Fatal: ${result.errors}")
    }
}
```

For `GenericNumberDemo` (non-suspending):

```kotlin
import fuookami.ospf.kotlin.example.core_demo.GenericNumberDemo

fun main() {
    val summaries = GenericNumberDemo.runBuildAndDump()
    for (summary in summaries) {
        println("${summary.numberType}: linear=${summary.linear.success}, quadratic=${summary.quadratic.success}")
    }
}
```

## Local Validation

```powershell
mvn -B -ntp -pl ospf-kotlin-example -Pcore-demo-only test
```
