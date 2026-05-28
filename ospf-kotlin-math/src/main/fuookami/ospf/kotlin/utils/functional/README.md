# ospf-kotlin-math/functional

[中文文档 (README_ch.md)](README_ch.md)

Collection extension functions for sum, average, and functional operations in OSPF Kotlin.

## Extensions

### Iterable Extensions

| Function | Description |
|----------|-------------|
| `sum()` | Sum of all elements (returns zero for empty) |
| `sumOrNull()` | Sum of all elements (returns null for empty) |
| `sumOf(extractor)` | Sum of extracted properties |
| `sumOfOrNull(extractor)` | Sum of extracted properties (null-safe) |
| `average()` | Average of elements (throws for empty) |
| `averageOrNull()` | Average of elements (returns null for empty) |
| `averageOf(extractor)` | Average of extracted properties |
| `averageOfOrNull(extractor)` | Average of extracted properties (null-safe) |

### Sequence Extensions

| Function | Description |
|----------|-------------|
| `sum()` | Sum of sequence elements |
| `sumOrNull()` | Sum of sequence elements (null-safe) |
| `average()` | Average of sequence elements |
| `averageOrNull()` | Average of sequence elements (null-safe) |

### Map Extensions

| Function | Description |
|----------|-------------|
| `sumValues()` | Sum of map values |
| `sumValuesOrNull()` | Sum of map values (null-safe) |
| `averageValues()` | Average of map values |
| `averageValuesOrNull()` | Average of map values (null-safe) |

## Usage

```kotlin
import fuookami.ospf.kotlin.math.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64

// Iterable sum
val list = listOf(Flt64(1.0), Flt64(2.0), Flt64(3.0))
val total = list.sum()  // Flt64(6.0)

// Null-safe sum
val nullableList = listOf<Flt64?>(Flt64(1.0), null, Flt64(3.0))
val safeTotal = nullableList.sumOrNull()  // null (contains null)

// Average
val avg = list.average()  // Flt64(2.0)

// Map value sum
val map = mapOf("a" to Flt64(10.0), "b" to Flt64(20.0))
val mapSum = map.sumValues()  // Flt64(30.0)
```

## Related

- [Main README](../../README.md)
- [Algebra Module](../algebra/README.md)
