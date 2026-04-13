# ospf-kotlin-math/algebra

[中文文档 (README_ch.md)](./README_ch.md)

Algebraic structures, number types, and value ranges for OSPF Kotlin.

## Sub-Packages

| Package | Description | Key Types |
|---------|-------------|-----------|
| [`concept/`](concept/) | Algebraic structure interfaces | `Arithmetic`, `Semigroup`, `Monoid`, `Group`, `Ring`, `Field` |
| [`law/`](law/) | Algebraic law validation utilities | `Associativity`, `Commutativity`, `Distributivity` |
| [`number/`](number/) | Concrete number type implementations | `Int8`-`IntX`, `UInt8`-`UIntX`, `Flt32`, `Flt64`, `FltX`, `Rtn8`-`RtnX` |
| [`value_range/`](value_range/) | Typed value ranges with bounds | `ValueRange`, `TypedValueRange`, `Bound`, `Interval` |

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

// Rational numbers
val rational = RtnX(IntX(1), IntX(3))  // 1/3
```

## Value Ranges

```kotlin
import fuookami.ospf.kotlin.math.algebra.value_range.*

// Create value range
val range = ValueRange(
    lower = Bound(Flt64(0.0), Interval.Closed),
    upper = Bound(Flt64(100.0), Interval.Closed)
)

// Typed value range for compile-time safety
val percentage: ClosedTypedValueRange<Flt64> = TypedValueRange.closed(
    Flt64(0.0),
    Flt64(100.0)
)

// Clamp values
val clamped = range.clamp(Flt64(150.0))  // Returns 100.0
```

## Related

- [Main README](../../README.md)
- [Operator Module](../operator/README.md)
- [Symbol Module](../symbol/README.md)
