# ospf-kotlin-math/operator

:us: English | :cn: [简体中文](README_ch.md)

Mathematical operator interfaces for OSPF Kotlin. Defines algebraic operations as reusable interfaces that number types and other algebraic structures implement.

## Arithmetic Operators

| Interface | File | Operation | Notation |
|-----------|------|-----------|----------|
| `Plus<in Rhs, out Ret>` | `Plus.kt` | Addition | `a + b` |
| `PlusTrait<Self, in Rhs, out Ret>` | `Plus.kt` | Addition (extension trait) | `a + b` |
| `PlusAssign<in Rhs>` | `Plus.kt` | Addition assignment | `a += b` |
| `Inc<Self>` | `Plus.kt` | Increment | `++a` |
| `Minus<in Rhs, out Ret>` | `Minus.kt` | Subtraction | `a - b` |
| `MinusAssign<in Rhs>` | `Minus.kt` | Subtraction assignment | `a -= b` |
| `Dec<Self>` | `Minus.kt` | Decrement | `--a` |
| `Times<in Rhs, out Ret>` | `Times.kt` | Multiplication | `a * b` |
| `TimesAssign<in Rhs>` | `Times.kt` | Multiplication assignment | `a *= b` |
| `Cross<in Rhs, out Ret>` | `Times.kt` | Cross product | `a x b` |
| `Div<in Rhs, out Ret>` | `Div.kt` | Division | `a / b` |
| `DivAssign<in Rhs>` | `Div.kt` | Division assignment | `a /= b` |
| `IntDiv<in Rhs, out Ret>` | `Div.kt` | Integer division | `a intDiv b` |
| `IntDivAssign<in Rhs>` | `Div.kt` | Integer division assignment | `a intDivAssign b` |
| `Rem<in Rhs, out Ret>` | `Rem.kt` | Remainder | `a % b` / `a mod b` |
| `RemAssign<in Rhs>` | `Rem.kt` | Remainder assignment | `a %= b` |
| `Pow<out Ret>` | `Pow.kt` | Integer power | `a^n` (n: Int) |
| `PowP<Ret>` | `Pow.kt` | Integer power with precision | `a^n` with digits/precision |
| `PowFun<in Self, out Ret>` | `Pow.kt` | Integer power (extension) | `a.pow(n)` |
| `PowFunP<in Self, Ret>` | `Pow.kt` | Integer power with precision (extension) | `a.pow(n, digits, precision)` |
| `PowF<in Index, out Ret>` | `Pow.kt` | Floating-point power | `a^x` (x: Index) |
| `PowFP<in Index, Ret>` | `Pow.kt` | Floating-point power with precision | `a^x` with digits/precision |
| `PowFFun<in Self, in Index, out Ret>` | `Pow.kt` | Floating-point power (extension) | `a.pow(x)` |
| `PowFPFun<in Self, in Index, Ret>` | `Pow.kt` | Floating-point power with precision (extension) | `a.pow(x, digits, precision)` |
| `Neg<out Ret>` | `Neg.kt` | Negation | `-a` |
| `Reciprocal<out Ret>` | `Reciprocal.kt` | Reciprocal | `1/a` |
| `RangeTo<in Rhs, out Ret>` | `RangeTo.kt` | Range creation | `a..b` / `a until b` |
| `Contains<in T>` | `Contains.kt` | Containment check | `a in b` |

## Transcendental Operators

| Interface | File | Key Methods |
|-----------|------|-------------|
| `Exp<out Ret>` | `Exp.kt` | `exp()` -- e^x |
| `ExpP<Ret>` | `Exp.kt` | `exp(digits, precision)` -- e^x with precision |
| `Log<in Base, out Ret>` | `Log.kt` | `log(base)`, `lg()` (base 10), `lg2()` (base 2), `ln()` (base e) |
| `LogFun<in Self, in Base, out Ret>` | `Log.kt` | Extension variants: `Self.log(base)`, `Self.lg()`, `Self.lg2()`, `Self.ln()` |
| `LogP<in Base, Ret>` | `Log.kt` | Precision-aware variants of all Log methods |
| `LogFunP<in Self, in Base, Ret>` | `Log.kt` | Precision-aware extension variants |
| `Trigonometry<out Ret>` | `Trigonometry.kt` | `sin()`, `cos()`, `tan()`, `sec()`, `csc()`, `cot()`, `asin()`, `acos()`, `atan()`, `asec()`, `acsc()`, `acot()`, `sinh()`, `cosh()`, `tanh()`, `sech()`, `csch()`, `coth()`, `asinh()`, `acosh()`, `atanh()`, `asech()`, `acsch()`, `acoth()` |

## Utility Operators

| Interface | File | Operation |
|-----------|------|-----------|
| `Abs<out Ret>` | `Abs.kt` | Absolute value |
| `Precision<T>` | `Precision.kt` | Precision-based comparison (`equal`, `order`, `less`, `greater`, etc.) |
| `Tolerance<T>` | `Tolerance.kt` | Tolerance value |
| `TolerancedEq<T>` | `Tolerance.kt` | Tolerance-based equality test |
| `TolerancedOrd<T>` | `Tolerance.kt` | Tolerance-based ordering |
| `AbsoluteTolerance<T>` | `Tolerance.kt` | Absolute tolerance data class |

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
val remainder = a % b   // Rem
val power = a.pow(b)    // Pow

// Transcendental functions
val expA = a.exp()      // Exp
val logA = a.ln()       // Log (natural logarithm)
val sinA = a.sin()      // Trigonometry
val cosA = a.cos()      // Trigonometry
```

## Related

- [Main README](../../README.md)
- [Algebra Module](../algebra/README.md)
