# functional

:us: English | :cn: [简体中文](README_ch.md)

Functional programming utilities for the OSPF Kotlin project.

## Overview

This package provides core functional programming abstractions including result types, equality/ordering typeclasses, variant types, predicate composition, and collection extensions.

## Modules

### Result Types (`Result.kt`)

Error handling with `Ok`, `Failed`, `Fatal`, and `Warn` states, similar to Rust's Result type.

```kotlin
val ok: Result<String, Error> = Ok("success")
val failed: Result<String, Error> = Failed(ErrorCode.FileNotFound, "File not found")
val fatal: Result<String, Error> = Fatal(errors)
val warn: ExResult<String, Error> = Warn("value", ErrorCode.Other, "Warning message")

result.ifOk { println("Success: ${it.value}") }
    .ifFailed { println("Failed: ${it.message}") }
    .ifFatal { it.forEach { error -> println(error) } }
```

### Either Type (`Either.kt`)

A value that can be either `Left` or `Right`, with type-safe extraction and pattern matching.

```kotlin
val left: Either<Int, String> = Either.Left(42)
val right: Either<Int, String> = Either.Right("hello")

val result = either.ifLeft { "Left: $it" }.ifRight { "Right: $it" }.invoke()
```

### Variant Types (`Variant.kt`)

Type-safe sum types (Variant2 through Variant20) with fluent pattern matching.

```kotlin
val v: Variant3<Int, String, Double> = Variant3.V2("hello")

val result = v
    .if1 { "Int: $it" }
    .if2 { "String: $it" }
    .if3 { "Double: $it" }
    .invoke()
```

### Equality and Ordering (`Eq.kt`, `Ord.kt`)

Haskell-style `Eq` and `Ord` typeclasses for type-safe equality and ordering.

```kotlin
class MyClass : Ord<MyClass> {
    override infix fun ord(rhs: MyClass): Order = orderBetween(this.priority, rhs.priority)
}

if (a ls b) { /* a < b */ }
if (a gr b) { /* a > b */ }
```

### Predicate Composition (`Predicate.kt`)

Composable predicates with `and`, `or`, `xor`, `not` operators and type aliases for various predicate/extractor types.

```kotlin
val combined = predicate1 and predicate2
val either = predicate1 or predicate2
val negated = !predicate
```

### Collection Extensions (`Collection.kt`)

Extended collection operations including:
- Shuffle (Fisher-Yates algorithm)
- Min/max/minMax with various comparator types
- Filter/sort with partial and three-way comparators
- Association operations with null-safety

```kotlin
val (min, max) = list.minMax()
val (minBy, maxBy) = list.minMaxBy { it.value }
val shuffled = list.shuffle()
```

### Tuple Types (`Quadruple.kt`)

Quadruple data class and homogeneous tuple type aliases (`Pair2`, `Triple3`, `Quadruple4`).

### Other Utilities

| File | Description |
|------|-------------|
| `Boolean.kt` | Boolean extension functions |
| `Condition.kt` | Conditional execution utilities |
| `DateTimeRange.kt` | Date/time range types and operations |
| `List.kt` | Multi-dimensional list type aliases and extensions |
| `Map.kt` | Multi-dimensional map type aliases and extensions |
| `MinMax.kt` | MinMax result type with comparator-based operations |
| `Nullable.kt` | Null-safety utilities (`ifNull`, `ifNullOrEmpty`) |
