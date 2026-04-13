# ospf-kotlin-math/operator

[中文文档 (README_ch.md)](./README_ch.md)

Mathematical operator interfaces for OSPF Kotlin. Defines algebraic operations as reusable interfaces that number types and other algebraic structures implement.

## Arithmetic Operators

| Interface | File | Operation | Notation |
|-----------|------|-----------|----------|
| `Plus<Rhs, Ret>` | `Plus.kt` | Addition | `a + b` |
| `PlusAssign<Rhs>` | `Plus.kt` | Addition assignment | `a += b` |
| `Inc<Self>` | `Plus.kt` | Increment | `++a` |
| `Minus<Self, Ret>` | `Minus.kt` | Subtraction | `a - b` |
| `MinusAssign<Self, Rhs>` | `Minus.kt` | Subtraction assignment | `a -= b` |
| `Dec<Self>` | `Minus.kt` | Decrement | `--a` |
| `Times<Rhs, Ret>` | `Times.kt` | Multiplication | `a * b` |
| `Div<Rhs, Ret>` | `Div.kt` | Division | `a / b` |
| `Rem<Rhs, Ret>` | `Rem.kt` | Remainder | `a % b` |
| `Pow<Self, Exp, Ret>` | `Pow.kt` | Power | `a ^ b` |
| `Neg<Self>` | `Neg.kt` | Negation | `-a` |
| `Reciprocal<Self>` | `Reciprocal.kt` | Reciprocal | `1/a` |
| `RangeTo<Self>` | `RangeTo.kt` | Range creation | `a..b` |

## Transcendental Operators

| Interface | File | Operation |
|-----------|------|-----------|
| `Exp<T>` | `Exp.kt` | Exponential e^x |
| `Log<T>` | `Log.kt` | Natural logarithm ln(x) |
| `Log2<T>` | `Log.kt` | Base-2 logarithm |
| `Log10<T>` | `Log.kt` | Base-10 logarithm |
| `Sin<T>` | `Trigonometry.kt` | Sine |
| `Cos<T>` | `Trigonometry.kt` | Cosine |
| `Tan<T>` | `Trigonometry.kt` | Tangent |
| `Asin<T>` | `Trigonometry.kt` | Arc sine |
| `Acos<T>` | `Trigonometry.kt` | Arc cosine |
| `Atan<T>` | `Trigonometry.kt` | Arc tangent |
| `Sinh<T>` | `Trigonometry.kt` | Hyperbolic sine |
| `Cosh<T>` | `Trigonometry.kt` | Hyperbolic cosine |
| `Tanh<T>` | `Trigonometry.kt` | Hyperbolic tangent |

## Utility Operators

| Interface | File | Operation |
|-----------|------|-----------|
| `Abs<Self>` | `Abs.kt` | Absolute value |
| `Precision<T>` | `Precision.kt` | Precision control |
| `Tolerance<T>` | `Tolerance.kt` | Tolerance-based comparison |

## Usage

```kotlin
import fuookami.ospf.kotlin.math.operator.*
import fuookami.ospf.kotlin.math.algebra.number.*

// Types implement these interfaces
val a = Flt64(3.0)
val b = Flt64(4.0)

val sum = a + b         // Plus
val diff = a - b        // Minus
val product = a * b     // Times
val quotient = a / b    // Div
val power = a.pow(b)    // Pow

// Transcendental functions
val expA = a.exp()      // Exp
val logA = a.ln()       // Log
val sinA = a.sin()      // Sin
val cosA = a.cos()      // Cos
val tanA = a.tan()      // Tan
```

## Related

- [Main README](../../README.md)
- [Algebra Module](../algebra/README.md)
