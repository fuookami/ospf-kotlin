# ospf-kotlin-math/operator

[English Documentation (README.md)](./README.md)

OSPF Kotlin 的数学运算符接口。将代数运算定义为可复用的接口，由数值类型和其他代数结构实现。

## 算术运算符

| 接口 | 文件 | 运算 | 表示法 |
|------|------|------|--------|
| `Plus<Rhs, Ret>` | `Plus.kt` | 加法 | `a + b` |
| `PlusAssign<Rhs>` | `Plus.kt` | 加法赋值 | `a += b` |
| `Inc<Self>` | `Plus.kt` | 自增 | `++a` |
| `Minus<Self, Ret>` | `Minus.kt` | 减法 | `a - b` |
| `MinusAssign<Self, Rhs>` | `Minus.kt` | 减法赋值 | `a -= b` |
| `Dec<Self>` | `Minus.kt` | 自减 | `--a` |
| `Times<Rhs, Ret>` | `Times.kt` | 乘法 | `a * b` |
| `Div<Rhs, Ret>` | `Div.kt` | 除法 | `a / b` |
| `Rem<Rhs, Ret>` | `Rem.kt` | 取余 | `a % b` |
| `Pow<Self, Exp, Ret>` | `Pow.kt` | 幂运算 | `a ^ b` |
| `Neg<Self>` | `Neg.kt` | 取负 | `-a` |
| `Reciprocal<Self>` | `Reciprocal.kt` | 倒数 | `1/a` |
| `RangeTo<Self>` | `RangeTo.kt` | 范围创建 | `a..b` |

## 超越运算符

| 接口 | 文件 | 运算 |
|------|------|------|
| `Exp<T>` | `Exp.kt` | 指数 e^x |
| `Log<T>` | `Log.kt` | 自然对数 ln(x) |
| `Log2<T>` | `Log.kt` | 以 2 为底的对数 |
| `Log10<T>` | `Log.kt` | 以 10 为底的对数 |
| `Sin<T>` | `Trigonometry.kt` | 正弦 |
| `Cos<T>` | `Trigonometry.kt` | 余弦 |
| `Tan<T>` | `Trigonometry.kt` | 正切 |
| `Asin<T>` | `Trigonometry.kt` | 反正弦 |
| `Acos<T>` | `Trigonometry.kt` | 反余弦 |
| `Atan<T>` | `Trigonometry.kt` | 反正切 |
| `Sinh<T>` | `Trigonometry.kt` | 双曲正弦 |
| `Cosh<T>` | `Trigonometry.kt` | 双曲余弦 |
| `Tanh<T>` | `Trigonometry.kt` | 双曲正切 |

## 工具运算符

| 接口 | 文件 | 运算 |
|------|------|------|
| `Abs<Self>` | `Abs.kt` | 绝对值 |
| `Precision<T>` | `Precision.kt` | 精度控制 |
| `Tolerance<T>` | `Tolerance.kt` | 容差比较 |

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
val power = a.pow(b)    // Pow

// 超越函数
val expA = a.exp()      // Exp
val logA = a.ln()       // Log
val sinA = a.sin()      // Sin
val cosA = a.cos()      // Cos
val tanA = a.tan()      // Tan
```

## 相关链接

- [主 README](../../README.md)
- [Algebra 模块](../algebra/README_ch.md)
