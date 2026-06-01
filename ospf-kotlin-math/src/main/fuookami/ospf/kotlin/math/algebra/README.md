# ospf-kotlin-math/algebra

:us: English | :cn: [简体中文](README_ch.md)

Algebraic structures, number types, and value ranges for OSPF Kotlin.

## Sub-Packages

| Package | Description | Key Types |
|---------|-------------|-----------|
| [`concept/`](concept/) | Algebraic structure interfaces | `Arithmetic`, `Semigroup`, `Monoid`, `Group`, `AbelianGroup`, `Ring`, `CommutativeRing`, `Field`, `RealNumber`, `IntegerNumber`, `FloatingNumber`, `RationalNumber`, `VectorSpace`, `NormedSpace`, `InnerProductSpace` |
| [`law/`](law/) | Algebraic law validation utilities | `GroupLaw`, `RingLaw`, `FieldLaw` |
| [`number/`](number/) | Concrete number type implementations | `Int8`-`IntX`, `UInt8`-`UIntX`, `Flt32`, `Flt64`, `FltX`, `Rtn8`-`RtnX`, `URtn8`-`URtnX`, `NInt8`-`NIntX`, `NUInt8`-`NUIntX` |
| [`value_range/`](value_range/) | Typed value ranges with bounds | `ValueRange`, `TypedValueRange`, `Bound`, `Interval`, `ValueWrapper` |

## Algebraic Hierarchy

```
Semigroup (associative +)
    └── Monoid (identity element)
        └── Group (inverse element)
            └── AbelianGroup (commutative)

MultiplicativeSemigroup (associative *)
    └── MultiplicativeMonoid (identity element)
        └── MultiplicativeGroup (inverse element)

Ring = AbelianGroup (+) + MultiplicativeSemigroup (*)
    └── CommutativeRing (commutative *)
        └── Field (multiplicative inverse /)
```

## Number Types

```kotlin
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.utils.functional.*

// Fixed precision integers
val i8 = Int8(127)
val i64 = Int64(9223372036854775807L)

// Arbitrary precision integers
val bigInt = IntX("123456789012345678901234567890")

// Floating-point numbers
val f32 = Flt32(3.14159f)
val f64 = Flt64(3.141592653589793)

// Arbitrary precision floating-point
val bigFloat = FltX("3.141592653589793238462643383279")

// Rational numbers (signed)
val rational = RtnX(IntX(1), IntX(3))  // 1/3

// Unsigned rational numbers
val urational = URtn64(UInt64(1u), UInt64(3u))  // 1/3
```

### Numeric Integer Types (with rational division)

Numeric integer types (`NInt8`-`NIntX`, `NUInt8`-`NUIntX`) return rational numbers from division, providing precise arithmetic:

```kotlin
val ni = NInt64(Int64(7))
val result: Rtn64 = ni / NInt64(Int64(3))  // (7 / 3), not 2
```

### Ret\<T\> Wrapping

Number factory methods return `Ret<T>` (a result type) rather than bare `T`:

```kotlin
import fuookami.ospf.kotlin.utils.functional.*

val result: Ret<Flt64> = Flt64(3.14)  // Direct value class construction bypasses Ret
val parsed: Ret<IntX> = IntX("12345") // String parsing may fail

when (parsed) {
    is Ok -> println(parsed.value)
    is Failed -> println(parsed.error)
}
```

## Value Ranges

```kotlin
import fuookami.ospf.kotlin.math.algebra.value_range.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.utils.functional.*

// Create value range (returns Ret<ValueRange<T>>)
val rangeResult = ValueRange(Flt64(0.0), Flt64(100.0))
when (rangeResult) {
    is Ok -> {
        val range = rangeResult.value
        println(Flt64(50.0) in range)  // true
    }
    is Failed -> println(rangeResult.error)
}

// Typed value range for compile-time safety
val closed = TypedValueRange.closed(Flt64(0.0), Flt64(100.0))
val open = TypedValueRange.open(Flt64(0.0), Flt64(100.0))

// Clamp values using coerceIn
val clamped = Flt64(150.0).coerceIn(rangeResult.value)  // Returns 100.0
```

## Related

- [Main README](../../README.md)
- [Operator Module](../operator/README.md)
- [Symbol Module](../symbol/README.md)
