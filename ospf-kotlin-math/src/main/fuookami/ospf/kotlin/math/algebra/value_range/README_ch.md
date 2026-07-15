# ospf-kotlin-math/algebra/value_range

:us: [English](README.md) | :cn: 简体中文

OSPF Kotlin 的类型化与动态区间算术。

## 核心类型

### ValueRange（动态）

运行时区间表示，支持开/闭边界。类型参数 `T` 必须同时满足 `RealNumber<T>` 和 `NumberField<T>`。

```kotlin
data class ValueRange<T>(
    val lowerBound: Bound<T>,
    val upperBound: Bound<T>,
    private val constants: RealNumberConstants<T>
) where T : RealNumber<T>, T : NumberField<T>
```

| 类型 | 示例 | 描述 |
|------|------|------|
| `Bounded` | `[1, 5]` | 两端有限 |
| `LowerBounded` | `[0, +inf)` | 上界无穷 |
| `UpperBounded` | `(-inf, 10]` | 下界无穷 |
| `Infinite` | `(-inf, +inf)` | 两端无穷 |

### Bound（边界）

表示单个边界点，包含值包装器和区间类型：

```kotlin
class Bound<T>(
    val value: ValueWrapper<T>,
    interval: Interval
) where T : RealNumber<T>, T : NumberField<T>
```

### Interval（区间类型）

边界开闭枚举：

```kotlin
enum class Interval {
    Open,   // (a, b) -- 严格不等式
    Closed  // [a, b] -- 包含边界
}
```

### ValueWrapper（值包装器）

密封类，包装边界值，支持无穷大：

```kotlin
sealed class ValueWrapper<T> {
    class Value<T>(val value: T, ...)     // 有限值
    class Infinity<T>(...)                 // 正无穷
    class NegativeInfinity<T>(...)         // 负无穷
}
```

### TypedValueRange（编译时）

编译时边界类型追踪，实现类型安全操作：

```kotlin
class TypedValueRange<T, LB : IntervalKind, UB : IntervalKind>(
    private val valueRange: ValueRange<T>,
    val lowerKind: LB,
    val upperKind: UB
) where T : RealNumber<T>, T : NumberField<T>
```

常用模式的类型别名：

```kotlin
typealias ClosedTypedValueRange<T> = TypedValueRange<T, ClosedIntervalKind, ClosedIntervalKind>
typealias OpenTypedValueRange<T> = TypedValueRange<T, OpenIntervalKind, OpenIntervalKind>
typealias ClosedOpenTypedValueRange<T> = TypedValueRange<T, ClosedIntervalKind, OpenIntervalKind>
typealias OpenClosedTypedValueRange<T> = TypedValueRange<T, OpenIntervalKind, ClosedIntervalKind>
typealias DynamicTypedValueRange<T> = TypedValueRange<T, RuntimeIntervalKind, RuntimeIntervalKind>
```

## 使用示例

### 动态 ValueRange

```kotlin
import fuookami.ospf.kotlin.math.algebra.value_range.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.utils.functional.*

// 工厂方法返回 Ret<ValueRange<T>>
val rangeResult = ValueRange(Flt64(0.0), Flt64(10.0))
when (rangeResult) {
    is Ok -> {
        val range = rangeResult.value
        val contains = Flt64(5.0) in range   // true
        val intersection = range.intersect(ValueRange(Flt64(5.0), Flt64(15.0)).value)
        // [5, 10]

        // 算术操作
        val shifted = range + Flt64(2.0)            // [2, 12]
        val scaled = range * Flt64(2.0)             // [0, 20]

        // 复制
        val copied = range.copy()
    }
    is Failed -> println(rangeResult.error)
}
```

### TypedValueRange（编译时安全）

```kotlin
import fuookami.ospf.kotlin.math.algebra.value_range.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.utils.functional.*

val closed = TypedValueRange.closed(Flt64(0.0), Flt64(10.0))
val open = TypedValueRange.open(Flt64(0.0), Flt64(10.0))

// 类型安全的交集
when (val closedVal = closed) {
    is Ok -> {
        val intersect = closedVal.value.intersectTyped(closedVal.value)  // 返回 ClosedTypedValueRange

        // 边界类型在编译时保留
        val halfOpen = TypedValueRange.closedOpen(Flt64(0.0), Flt64(10.0))
        // halfOpen lowerKind == ClosedIntervalKind
        // halfOpen upperKind == OpenIntervalKind
    }
}
```

### 无穷区间

```kotlin
// 使用 ValueRange 伴随对象与 Infinity/NegativeInfinity 标记
val all = ValueRange<Flt64>()  // 全范围 (-inf, +inf)

// 使用 geq/leq 工厂方法
val geqResult = ValueRange.geq(Flt64(0.0))    // [0, +inf)
val leqResult = ValueRange.leq(Flt64(100.0))  // (-inf, 100]
val grResult = ValueRange.gr(Flt64(0.0))       // (0, +inf)
val lsResult = ValueRange.ls(Flt64(100.0))     // (-inf, 100)
```

## 边界语义

### 开区间 vs 闭区间

| 区间 | 包含 0? | 包含 1? | 包含 2? |
|------|---------|---------|---------|
| `[0, 2]` | 是 | 是 | 是 |
| `(0, 2)` | 否 | 是 | 否 |
| `[0, 2)` | 是 | 是 | 否 |
| `(0, 2]` | 否 | 是 | 是 |

### 空区间

空区间出现在：
- 下界 > 上界
- 单点开区间 `(a, a)`

工厂方法对无效范围返回 `Failed`：

```kotlin
val result = ValueRange(Flt64(10.0), Flt64(0.0))
// result 是 Failed，包含 IllegalArgument 错误
```

### 固定值（单点）

```kotlin
val point = ValueRange(Flt64(5.0))  // 创建 [5, 5]
// point.fixed == true
// point.fixedValue == Flt64(5.0)
```

## 算术操作

| 操作 | 示例 | 结果 |
|------|------|------|
| `plus` | `[0, 10] + 5` | `[5, 15]` |
| `minus` | `[0, 10] - 5` | `[-5, 5]` |
| `times`（正数） | `[1, 2] * 3` | `[3, 6]` |
| `times`（负数） | `[1, 2] * -1` | `[-2, -1]` |
| `div`（正数） | `[0, 10] / 2` | `[0, 5]` |
| `div`（负数） | `[0, 10] / -2` | `[-5, 0]` |

### 值约束

```kotlin
val range = ValueRange(Flt64(0.0), Flt64(100.0)).value
val clamped = Flt64(150.0).coerceIn(range)  // 返回 Flt64(100.0)
```

## 集合操作

| 操作 | 方法 | 描述 |
|------|------|------|
| 并集 | `range1 union range2` | 合并重叠区间，不相交时返回 null |
| 交集 | `range1 intersect range2` | 重叠区域，不相交时返回 null |
| 包含 | `value in range` | 判断值是否在范围内 |
| 子范围 | `range1.contains(range2)` | 判断 range2 是否完全在 range1 内 |

## TypedValueRange 操作

类型化变体保留编译时边界类型：

| 动态 | 类型化 | 描述 |
|------|--------|------|
| `plus(T)` | `plusTyped(T)` | 加数值，保持类型 |
| `minus(T)` | `minusTyped(T)` | 减数值，保持类型 |
| `union` | `unionTyped` | 同类型并集，保持类型 |
| `intersect` | `intersectTyped` | 同类型交集，保持类型 |
| `times(T)` | `timesPositive(T)` / `timesNegative(T)` | 乘法，类型感知 |
| `div(T)` | `divPositive(T)` / `divNegative(T)` | 除法，类型感知 |

跨类型操作返回 `DynamicTypedValueRange<T>` 或推导最静态类型：

```kotlin
val result = closed.plusTypedAcrossKinds(open)  // 从结果边界推导类型
```

## 相关链接

- [主 README](../../README.md)
- [Geometry 模块](../../geometry/README.md)
- [Symbol 模块](../../symbol/README.md)
