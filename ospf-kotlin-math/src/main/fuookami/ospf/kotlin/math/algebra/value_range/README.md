# ospf-kotlin-math/algebra/value_range

:us: English | :cn: [简体中文](README_ch.md)

Typed and dynamic interval arithmetic for OSPF Kotlin.

## Core Types

### ValueRange (Dynamic)

Runtime interval representation with open/closed boundaries. The type parameter `T` must satisfy both `RealNumber<T>` and `NumberField<T>`.

```kotlin
data class ValueRange<T>(
    val lowerBound: Bound<T>,
    val upperBound: Bound<T>,
    private val constants: RealNumberConstants<T>
) where T : RealNumber<T>, T : NumberField<T>
```

| Kind | Example | Description |
|------|---------|-------------|
| `Bounded` | `[1, 5]` | Both bounds finite |
| `LowerBounded` | `[0, +inf)` | Upper bound infinite |
| `UpperBounded` | `(-inf, 10]` | Lower bound infinite |
| `Infinite` | `(-inf, +inf)` | Both bounds infinite |

### Bound

Represents a single boundary point with a value wrapper and interval type:

```kotlin
class Bound<T>(
    val value: ValueWrapper<T>,
    interval: Interval
) where T : RealNumber<T>, T : NumberField<T>
```

### Interval

Enum for boundary openness:

```kotlin
enum class Interval {
    Open,   // (a, b) -- strict inequality
    Closed  // [a, b] -- inclusive
}
```

### ValueWrapper

Sealed class wrapping boundary values, supporting infinities:

```kotlin
sealed class ValueWrapper<T> {
    class Value<T>(val value: T, ...)     // finite value
    class Infinity<T>(...)                 // positive infinity
    class NegativeInfinity<T>(...)         // negative infinity
}
```

### TypedValueRange (Compile-time)

Compile-time boundary kind tracking for type-safe operations:

```kotlin
class TypedValueRange<T, LB : IntervalKind, UB : IntervalKind>(
    private val valueRange: ValueRange<T>,
    val lowerKind: LB,
    val upperKind: UB
) where T : RealNumber<T>, T : NumberField<T>
```

Type aliases for common patterns:

```kotlin
typealias ClosedTypedValueRange<T> = TypedValueRange<T, ClosedIntervalKind, ClosedIntervalKind>
typealias OpenTypedValueRange<T> = TypedValueRange<T, OpenIntervalKind, OpenIntervalKind>
typealias ClosedOpenTypedValueRange<T> = TypedValueRange<T, ClosedIntervalKind, OpenIntervalKind>
typealias OpenClosedTypedValueRange<T> = TypedValueRange<T, OpenIntervalKind, ClosedIntervalKind>
typealias DynamicTypedValueRange<T> = TypedValueRange<T, RuntimeIntervalKind, RuntimeIntervalKind>
```

## Usage Examples

### Dynamic ValueRange

```kotlin
import fuookami.ospf.kotlin.math.algebra.value_range.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.utils.functional.*

// Factory returns Ret<ValueRange<T>>
val rangeResult = ValueRange(Flt64(0.0), Flt64(10.0))
when (rangeResult) {
    is Ok -> {
        val range = rangeResult.value
        val contains = Flt64(5.0) in range   // true
        val intersection = range.intersect(ValueRange(Flt64(5.0), Flt64(15.0)).value)
        // [5, 10]

        // Arithmetic operations
        val shifted = range + Flt64(2.0)            // [2, 12]
        val scaled = range * Flt64(2.0)             // [0, 20]

        // Copy
        val copied = range.copy()
    }
    is Failed -> println(rangeResult.error)
}
```

### TypedValueRange (Compile-time Safety)

```kotlin
import fuookami.ospf.kotlin.math.algebra.value_range.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.utils.functional.*

val closed = TypedValueRange.closed(Flt64(0.0), Flt64(10.0))
val open = TypedValueRange.open(Flt64(0.0), Flt64(10.0))

// Type-safe intersection
when (val closedVal = closed) {
    is Ok -> {
        val intersect = closedVal.value.intersectTyped(closedVal.value)  // Returns ClosedTypedValueRange

        // Boundary kind preserved at compile time
        val halfOpen = TypedValueRange.closedOpen(Flt64(0.0), Flt64(10.0))
        // halfOpen lowerKind == ClosedIntervalKind
        // halfOpen upperKind == OpenIntervalKind
    }
}
```

### Infinite Ranges

```kotlin
// Using ValueRange companion with Infinity/NegativeInfinity markers
val all = ValueRange<Flt64>()  // full range (-inf, +inf)

// Using geq/leq factory methods
val geqResult = ValueRange.geq(Flt64(0.0))    // [0, +inf)
val leqResult = ValueRange.leq(Flt64(100.0))  // (-inf, 100]
val grResult = ValueRange.gr(Flt64(0.0))       // (0, +inf)
val lsResult = ValueRange.ls(Flt64(100.0))     // (-inf, 100)
```

## Boundary Semantics

### Open vs Closed

| Interval | Contains 0? | Contains 1? | Contains 2? |
|----------|-------------|-------------|-------------|
| `[0, 2]` | Yes | Yes | Yes |
| `(0, 2)` | No | Yes | No |
| `[0, 2)` | Yes | Yes | No |
| `(0, 2]` | No | Yes | Yes |

### Empty Ranges

Empty ranges occur when:
- Lower bound > Upper bound
- Single-point open interval `(a, a)`

The factory returns `Failed` for invalid ranges:

```kotlin
val result = ValueRange(Flt64(10.0), Flt64(0.0))
// result is Failed with IllegalArgument error
```

### Fixed Value (Single Point)

```kotlin
val point = ValueRange(Flt64(5.0))  // creates [5, 5]
// point.fixed == true
// point.fixedValue == Flt64(5.0)
```

## Arithmetic Operations

| Operation | Example | Result |
|-----------|---------|--------|
| `plus` | `[0, 10] + 5` | `[5, 15]` |
| `minus` | `[0, 10] - 5` | `[-5, 5]` |
| `times` (positive) | `[1, 2] * 3` | `[3, 6]` |
| `times` (negative) | `[1, 2] * -1` | `[-2, -1]` |
| `div` (positive) | `[0, 10] / 2` | `[0, 5]` |
| `div` (negative) | `[0, 10] / -2` | `[-5, 0]` |

### Coercion

```kotlin
val range = ValueRange(Flt64(0.0), Flt64(100.0)).value
val clamped = Flt64(150.0).coerceIn(range)  // returns Flt64(100.0)
```

## Set Operations

| Operation | Method | Description |
|-----------|--------|-------------|
| Union | `range1 union range2` | Combines overlapping ranges, returns null if disjoint |
| Intersection | `range1 intersect range2` | Overlapping region, returns null if disjoint |
| Containment | `value in range` | Checks if value is within range |
| Sub-range | `range1.contains(range2)` | Checks if range2 is fully within range1 |

## TypedValueRange Operations

Typed variants preserve compile-time boundary kinds:

| Dynamic | Typed | Description |
|---------|-------|-------------|
| `plus(T)` | `plusTyped(T)` | Add number, preserves kind |
| `minus(T)` | `minusTyped(T)` | Subtract number, preserves kind |
| `union` | `unionTyped` | Union with same kind, preserves kind |
| `intersect` | `intersectTyped` | Intersect with same kind, preserves kind |
| `times(T)` | `timesPositive(T)` / `timesNegative(T)` | Multiply, kind-aware |
| `div(T)` | `divPositive(T)` / `divNegative(T)` | Divide, kind-aware |

Cross-kind operations return `DynamicTypedValueRange<T>` or infer the most static kind:

```kotlin
val result = closed.plusTypedAcrossKinds(open)  // infers kind from result bounds
```

## Related

- [Main README](../../README.md)
- [Geometry Module](../../geometry/README.md)
- [Symbol Module](../../symbol/README.md)
