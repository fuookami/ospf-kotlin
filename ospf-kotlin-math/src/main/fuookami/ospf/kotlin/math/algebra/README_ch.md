# ospf-kotlin-math/algebra

[English Documentation (README.md)](./README.md)

OSPF Kotlin 的代数结构、数值类型和值范围。

## 子包

| 包 | 描述 | 关键类型 |
|------|------|---------|
| [`concept/`](concept/) | 代数结构接口 | `Arithmetic`、`Semigroup`、`Monoid`、`Group`、`Ring`、`Field` |
| [`law/`](law/) | 代数定律验证工具 | `Associativity`、`Commutativity`、`Distributivity` |
| [`number/`](number/) | 具体数值类型实现 | `Int8`-`IntX`、`UInt8`-`UIntX`、`Flt32`、`Flt64`、`FltX`、`Rtn8`-`RtnX` |
| [`value_range/`](value_range/) | 带边界的类型化值范围 | `ValueRange`、`TypedValueRange`、`Bound`、`Interval` |

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

// 有理数
val rational = RtnX(IntX(1), IntX(3))  // 1/3
```

## 值范围

```kotlin
import fuookami.ospf.kotlin.math.algebra.value_range.*

// 创建值范围
val range = ValueRange(
    lower = Bound(Flt64(0.0), Interval.Closed),
    upper = Bound(Flt64(100.0), Interval.Closed)
)

// 类型化值范围，编译时安全
val percentage: ClosedTypedValueRange<Flt64> = TypedValueRange.closed(
    Flt64(0.0),
    Flt64(100.0)
)

// 值截断
val clamped = range.clamp(Flt64(150.0))  // 返回 100.0
```

## 相关链接

- [主 README](../../README.md)
- [Operator 模块](../operator/README.md)
- [Symbol 模块](../symbol/README.md)
