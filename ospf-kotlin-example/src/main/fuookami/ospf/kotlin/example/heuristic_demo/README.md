# heuristic_demo

:us: English | :cn: [简体中文](README_ch.md)

## Introduction

`heuristic_demo` contains heuristic solver demonstrations using Particle Swarm Optimization (PSO). These demos show how to use `CallBackModel` for black-box objective functions and how to convert a `LinearMetaModel` into a `CallBackModel` for PSO-based solving.

## Scope

- **Demo1**: Builds a `CallBackModel` with a simple quadratic objective `(x - 1)^2` and solves it via PSO directly.
- **Demo2**: Constructs a `LinearMetaModel` (maximize `x + y` with bounds), converts it to a `MechanismModel`, then wraps it as a `CallBackModel` and solves with PSO.

## Module Structure

| File | Description |
| --- | --- |
| `Demo1.kt` | Simple quadratic minimization using `CallBackModel` -- defines a black-box objective `(x - 1)^2` and solves with PSO (10-second time limit) |
| `Demo2.kt` | Linear meta-model with PSO -- builds a linear model via `LinearMetaModel`, converts it to a mechanism model, wraps it in `CallBackModel`, and solves with PSO (10-second time limit) |

## Usage

Each demo is a `class` with an `invoke()` operator:

```kotlin
import fuookami.ospf.kotlin.example.heuristic_demo.Demo1

fun main() {
    val result = Demo1()
    when (result) {
        is Ok -> println("Solved successfully")
        is Failed -> println("Failed: ${result.error}")
        is Fatal -> println("Fatal: ${result.errors}")
    }
}
```

```kotlin
import fuookami.ospf.kotlin.example.heuristic_demo.Demo2

fun main() {
    val result = Demo2()
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
