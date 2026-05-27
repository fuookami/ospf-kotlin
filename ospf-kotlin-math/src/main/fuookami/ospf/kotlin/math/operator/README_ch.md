# ospf-kotlin-math/operator

[English Documentation (README.md)](./README.md)

OSPF Kotlin 的数学运算符接口。将代数运算定义为可复用的接口，由数值类型和其他代数结构实现。

## 算术运算符

| 接口 | 文件 | 运算 | 表示法 |
|------|------|------|--------|
| `Plus<in Rhs, out Ret>` | `Plus.kt` | 加法 | `a + b` |
| `PlusTrait<Self, in Rhs, out Ret>` | `Plus.kt` | 加法（扩展特征） | `a + b` |
| `PlusAssign<in Rhs>` | `Plus.kt` | 加法赋值 | `a += b` |
| `Inc<Self>` | `Plus.kt` | 自增 | `++a` |
| `Minus<in Rhs, out Ret>` | `Minus.kt` | 减法 | `a - b` |
| `MinusAssign<in Rhs>` | `Minus.kt` | 减法赋值 | `a -= b` |
| `Dec<Self>` | `Minus.kt` | 自减 | `--a` |
| `Times<in Rhs, out Ret>` | `Times.kt` | 乘法 | `a * b` |
| `TimesAssign<in Rhs>` | `Times.kt` | 乘法赋值 | `a *= b` |
| `Cross<in Rhs, out Ret>` | `Times.kt` | 叉积 | `a x b` |
| `Div<in Rhs, out Ret>` | `Div.kt` | 除法 | `a / b` |
| `DivAssign<in Rhs>` | `Div.kt` | 除法赋值 | `a /= b` |
| `IntDiv<in Rhs, out Ret>` | `Div.kt` | 整数除法 | `a intDiv b` |
| `IntDivAssign<in Rhs>` | `Div.kt` | 整数除法赋值 | `a intDivAssign b` |
| `Rem<in Rhs, out Ret>` | `Rem.kt` | 取余 | `a % b` / `a mod b` |
| `RemAssign<in Rhs>` | `Rem.kt` | 取余赋值 | `a %= b` |
| `Pow<out Ret>` | `Pow.kt` | 整数幂运算 | `a^n`（n: Int） |
| `PowP<Ret>` | `Pow.kt` | 带精度的整数幂运算 | `a^n`（带精度参数） |
| `PowFun<in Self, out Ret>` | `Pow.kt` | 整数幂运算（扩展函数） | `a.pow(n)` |
| `PowFunP<in Self, Ret>` | `Pow.kt` | 带精度的整数幂运算（扩展函数） | `a.pow(n, digits, precision)` |
| `PowF<in Index, out Ret>` | `Pow.kt` | 浮点幂运算 | `a^x`（x: Index） |
| `PowFP<in Index, Ret>` | `Pow.kt` | 带精度的浮点幂运算 | `a^x`（带精度参数） |
| `PowFFun<in Self, in Index, out Ret>` | `Pow.kt` | 浮点幂运算（扩展函数） | `a.pow(x)` |
| `PowFPFun<in Self, in Index, Ret>` | `Pow.kt` | 带精度的浮点幂运算（扩展函数） | `a.pow(x, digits, precision)` |
| `Neg<out Ret>` | `Neg.kt` | 取负 | `-a` |
| `Reciprocal<out Ret>` | `Reciprocal.kt` | 倒数 | `1/a` |
| `RangeTo<in Rhs, out Ret>` | `RangeTo.kt` | 范围创建 | `a..b` / `a until b` |
| `Contains<in T>` | `Contains.kt` | 包含检查 | `a in b` |

## 超越运算符

| 接口 | 文件 | 主要方法 |
|------|------|----------|
| `Exp<out Ret>` | `Exp.kt` | `exp()` -- e^x |
| `ExpP<Ret>` | `Exp.kt` | `exp(digits, precision)` -- 带精度的 e^x |
| `Log<in Base, out Ret>` | `Log.kt` | `log(base)`、`lg()`（以 10 为底）、`lg2()`（以 2 为底）、`ln()`（以 e 为底） |
| `LogFun<in Self, in Base, out Ret>` | `Log.kt` | 扩展函数变体：`Self.log(base)`、`Self.lg()`、`Self.lg2()`、`Self.ln()` |
| `LogP<in Base, Ret>` | `Log.kt` | 带精度的所有 Log 方法变体 |
| `LogFunP<in Self, in Base, Ret>` | `Log.kt` | 带精度的扩展函数变体 |
| `Trigonometry<out Ret>` | `Trigonometry.kt` | `sin()`、`cos()`、`tan()`、`sec()`、`csc()`、`cot()`、`asin()`、`acos()`、`atan()`、`asec()`、`acsc()`、`acot()`、`sinh()`、`cosh()`、`tanh()`、`sech()`、`csch()`、`coth()`、`asinh()`、`acosh()`、`atanh()`、`asech()`、`acsch()`、`acoth()` |

## 工具运算符

| 接口 | 文件 | 运算 |
|------|------|------|
| `Abs<out Ret>` | `Abs.kt` | 绝对值 |
| `Precision<T>` | `Precision.kt` | 精度比较（`equal`、`order`、`less`、`greater` 等） |
| `Tolerance<T>` | `Tolerance.kt` | 容差值 |
| `TolerancedEq<T>` | `Tolerance.kt` | 带容差的相等判断 |
| `TolerancedOrd<T>` | `Tolerance.kt` | 带容差的顺序比较 |
| `AbsoluteTolerance<T>` | `Tolerance.kt` | 绝对容差数据类 |

## 使用示例

```kotlin
import fuookami.ospf.kotlin.math.operator.*
import fuookami.ospf.kotlin.math.algebra.number.*

// 类型实现这些接口
val a = Flt64(3.0)
val b = Flt64(4.0)

val sum = a + b         // Plus
val diff = a - b        // Minus
val product = a * b     // Times
val quotient = a / b    // Div
val remainder = a % b   // Rem
val power = a.pow(b)    // Pow

// 超越函数
val expA = a.exp()      // Exp
val logA = a.ln()       // Log（自然对数）
val sinA = a.sin()      // Trigonometry
val cosA = a.cos()      // Trigonometry
```

## 相关链接

- [主 README](../../README.md)
- [Algebra 模块](../algebra/README_ch.md)
