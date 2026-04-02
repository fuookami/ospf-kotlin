# ospf-kotlin-utils/math/algebra/value_range

[中文文档 (README_ch.md)](./README_ch.md)

Typed and dynamic interval arithmetic for OSPF Kotlin.

## Core Types

### ValueRange (Dynamic)

Runtime interval representation with open/closed boundaries:

```kotlin
data class ValueRange<T>(
    val lowerBound: T?,
    val upperBound: T?,
    val lowerClosed: Boolean = true,
    val upperClosed: Boolean = true
)
```

| Kind | Example | Description |
|------|---------|-------------|
| `Bounded` | `[1, 5]` | Both bounds finite |
| `LowerBounded` | `[0, +∞)` | Upper bound infinite |
| `UpperBounded` | `(-∞, 10]` | Lower bound infinite |
| `Infinite` | `(-∞, +∞)` | Both bounds infinite |

### TypedValueRange (Compile-time)

Compile-time boundary kind tracking for type-safe operations:

```kotlin
sealed class BoundaryKind {
    object Open : BoundaryKind()
    object Closed : BoundaryKind()
}

data class TypedValueRange<T, LK : BoundaryKind, UK : BoundaryKind>(
    val lowerBound: T?,
    val upperBound: T?,
    val lowerKind: LK,
    val upperKind: UK
)
```

Type aliases for common patterns:

```kotlin
typealias ClosedInterval<T> = TypedValueRange<T, BoundaryKind.Closed, BoundaryKind.Closed>
typealias OpenInterval<T> = TypedValueRange<T, BoundaryKind.Open, BoundaryKind.Open>
typealias HalfOpenInterval<T> = TypedValueRange<T, BoundaryKind.Closed, BoundaryKind.Open>
```

## Usage Examples

### Dynamic ValueRange

```kotlin
import fuookami.ospf.kotlin.utils.math.algebra.value_range.*
import fuookami.ospf.kotlin.utils.math.algebra.number.Flt64

val range = ValueRange(Flt64(0.0), Flt64(10.0))
val contains = range.contains(Flt64(5.0))   // true
val intersection = range.intersect(ValueRange(Flt64(5.0), Flt64(15.0)))
// [5, 10]

// Arithmetic operations
val shifted = range + Flt64(2.0)            // [2, 12]
val scaled = range * Flt64(2.0)             // [0, 20]
```

### TypedValueRange (Compile-time Safety)

```kotlin
import fuookami.ospf.kotlin.utils.math.algebra.value_range.*

val closed = typedClosedInterval(Flt64(0.0), Flt64(10.0))
val open = typedOpenInterval(Flt64(0.0), Flt64(10.0))

// Type-safe intersection
val intersect = closed.intersectTyped(closed)  // Returns ClosedInterval

// Boundary kind preserved at compile time
val halfOpen = typedHalfOpenInterval(Flt64(0.0), Flt64(10.0))
// halfOpen.lowerKind == BoundaryKind.Closed
// halfOpen.upperKind == BoundaryKind.Open
```

### Infinite Ranges

```kotlin
val positive = ValueRange.lowerBounded(Flt64(0.0))  // [0, +∞)
val negative = ValueRange.upperBounded(Flt64(0.0))  // (-∞, 0]
val all = ValueRange.infinite<Flt64>()              // (-∞, +∞)

// Contains handles infinities correctly
positive.contains(Flt64(1e100))  // true
positive.contains(Flt64(-1.0))   // false
```

## Boundary Semantics

### Open vs Closed

| Interval | Contains 0? | Contains 1? | Contains 2? |
|----------|-------------|-------------|-------------|
| `[0, 2]` | ✓ | ✓ | ✓ |
| `(0, 2)` | ✗ | ✓ | ✗ |
| `[0, 2)` | ✓ | ✓ | ✗ |
| `(0, 2]` | ✗ | ✓ | ✓ |

### Empty Ranges

Empty ranges occur when:
- Lower bound > Upper bound
- Single-point open interval `(a, a)`

```kotlin
val empty = ValueRange(Flt64(10.0), Flt64(0.0))
empty.isEmpty  // true
empty.contains(Flt64(5.0))  // false
```

### Half-infinite Contains

```kotlin
val halfOpen = ValueRange(Flt64(0.0), Flt64(10.0), lowerClosed = true, upperClosed = false)
// [0, 10)

halfOpen.contains(Flt64(0.0))   // true (closed lower)
halfOpen.contains(Flt64(10.0))  // false (open upper)
halfOpen.contains(Flt64(10.0), epsilon = Flt64(1e-9))  // uses epsilon for open boundary
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

### Cross-kind Operations

```kotlin
val closed = typedClosedInterval(Flt64(0.0), Flt64(10.0))
val open = typedOpenInterval(Flt64(5.0), Flt64(15.0))

// Returns runtime ValueRange (kind determined at runtime)
val result = closed.intersect(open)
```

## Performance Notes

| Operation | Complexity | Notes |
|-----------|------------|-------|
| `contains` | O(1) | Direct bound comparison |
| `intersect` | O(1) | Max/min operations |
| `typedPlus/Minus` | O(1) | Creates new instance |
| `typedTimes/Div` | O(1) | Sign-aware bound swap |

For high-frequency interval operations, prefer `TypedValueRange` for compile-time optimization.

## Test Coverage

- `TypedValueRangeTest.kt`: Contains, intersection, arithmetic, empty/infinite cases

Run tests:

```powershell
mvn -pl ospf-kotlin-utils -Dtest=TypedValueRangeTest test
```

## Related

- [Main README](../../README.md)
- [Geometry Module](../../geometry/README.md)
- [Symbol Module](../../symbol/README.md)