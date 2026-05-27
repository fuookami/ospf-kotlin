# ospf-kotlin-math/algebra

[English Documentation (README.md)](./README.md)

OSPF Kotlin 的代数结构、数值类型和值范围。

## 子包

| 包 | 描述 | 关键类型 |
|------|------|---------|
| [`concept/`](concept/) | 代数结构接口 | `Arithmetic`、`Semigroup`、`Monoid`、`Group`、`AbelianGroup`、`Ring`、`CommutativeRing`、`Field`、`RealNumber`、`IntegerNumber`、`FloatingNumber`、`RationalNumber`、`VectorSpace`、`NormedSpace`、`InnerProductSpace` |
| [`law/`](law/) | 代数定律验证工具 | `GroupLaw`、`RingLaw`、`FieldLaw` |
| [`number/`](number/) | 具体数值类型实现 | `Int8`-`IntX`、`UInt8`-`UIntX`、`Flt32`、`Flt64`、`FltX`、`Rtn8`-`RtnX`、`URtn8`-`URtnX`、`NInt8`-`NIntX`、`NUInt8`-`NUIntX` |
| [`value_range/`](value_range/) | 带边界的类型化值范围 | `ValueRange`、`TypedValueRange`、`Bound`、`Interval`、`ValueWrapper` |

## 代数层次结构

```
Semigroup（半群，结合律 +）
    └── Monoid（幺半群，单位元）
        └── Group（群，逆元）
            └── AbelianGroup（阿贝尔群，交换律）

MultiplicativeSemigroup（乘法半群，结合律 *）
    └── MultiplicativeMonoid（乘法幺半群，单位元）
        └── MultiplicativeGroup（乘法群，逆元）

Ring（环）= AbelianGroup（+）+ MultiplicativeSemigroup（*）
    └── CommutativeRing（交换环，乘法交换律）
        └── Field（域，乘法逆元 /）
```

## 数值类型

```kotlin
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.utils.functional.*

// 固定精度整数
val i8 = Int8(127)
val i64 = Int64(9223372036854775807L)

// 任意精度整数
val bigInt = IntX("123456789012345678901234567890")

// 浮点数
val f32 = Flt32(3.14159f)
val f64 = Flt64(3.141592653589793)

// 任意精度浮点数
val bigFloat = FltX("3.141592653589793238462643383279")

// 有理数（有符号）
val rational = RtnX(IntX(1), IntX(3))  // 1/3

// 无符号有理数
val urational = URtn64(UInt64(1u), UInt64(3u))  // 1/3
```

### 数值整数类型（有理数除法）

数值整数类型（`NInt8`-`NIntX`、`NUInt8`-`NUIntX`）的除法运算返回有理数结果，提供精确算术：

```kotlin
val ni = NInt64(Int64(7))
val result: Rtn64 = ni / NInt64(Int64(3))  // (7 / 3)，不是 2
```

### Ret\<T\> 包装

数值工厂方法返回 `Ret<T>`（结果类型），而非裸 `T`：

```kotlin
import fuookami.ospf.kotlin.utils.functional.*

val result: Ret<Flt64> = Flt64(3.14)  // 直接值类构造绕过 Ret
val parsed: Ret<IntX> = IntX("12345") // 字符串解析可能失败

when (parsed) {
    is Ok -> println(parsed.value)
    is Failed -> println(parsed.error)
}
```

## 值范围

```kotlin
import fuookami.ospf.kotlin.math.algebra.value_range.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.utils.functional.*

// 创建值范围（返回 Ret<ValueRange<T>>）
val rangeResult = ValueRange(Flt64(0.0), Flt64(100.0))
when (rangeResult) {
    is Ok -> {
        val range = rangeResult.value
        println(Flt64(50.0) in range)  // true
    }
    is Failed -> println(rangeResult.error)
}

// 类型化值范围，编译时安全
val closed = TypedValueRange.closed(Flt64(0.0), Flt64(100.0))
val open = TypedValueRange.open(Flt64(0.0), Flt64(100.0))

// 值截断
val clamped = Flt64(150.0).coerceIn(rangeResult.value)  // 返回 100.0
```

## 相关链接

- [主 README](../../README.md)
- [Operator 模块](../operator/README.md)
- [Symbol 模块](../symbol/README.md)
