# ospf-kotlin-utils/math/algebra/value_range

[English Documentation (README.md)](./README.md)

OSPF Kotlin 的类型化与动态区间算术。

## 核心类型

### ValueRange（动态）

运行时区间表示，支持开/闭边界：

```kotlin
data class ValueRange<T>(
    val lowerBound: T?,
    val upperBound: T?,
    val lowerClosed: Boolean = true,
    val upperClosed: Boolean = true
)
```

| 类型 | 示例 | 描述 |
|------|------|------|
| `Bounded` | `[1, 5]` | 两端有限 |
| `LowerBounded` | `[0, +∞)` | 上界无穷 |
| `UpperBounded` | `(-∞, 10]` | 下界无穷 |
| `Infinite` | `(-∞, +∞)` | 两端无穷 |

### TypedValueRange（编译时）

编译时边界类型追踪，实现类型安全操作：

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

常用模式的类型别名：

```kotlin
typealias ClosedInterval<T> = TypedValueRange<T, BoundaryKind.Closed, BoundaryKind.Closed>
typealias OpenInterval<T> = TypedValueRange<T, BoundaryKind.Open, BoundaryKind.Open>
typealias HalfOpenInterval<T> = TypedValueRange<T, BoundaryKind.Closed, BoundaryKind.Open>
```

## 使用示例

### 动态 ValueRange

```kotlin
import fuookami.ospf.kotlin.utils.math.algebra.value_range.*
import fuookami.ospf.kotlin.utils.math.algebra.number.Flt64

val range = ValueRange(Flt64(0.0), Flt64(10.0))
val contains = range.contains(Flt64(5.0))   // true
val intersection = range.intersect(ValueRange(Flt64(5.0), Flt64(15.0)))
// [5, 10]

// 算术操作
val shifted = range + Flt64(2.0)            // [2, 12]
val scaled = range * Flt64(2.0)             // [0, 20]
```

### TypedValueRange（编译时安全）

```kotlin
import fuookami.ospf.kotlin.utils.math.algebra.value_range.*

val closed = typedClosedInterval(Flt64(0.0), Flt64(10.0))
val open = typedOpenInterval(Flt64(0.0), Flt64(10.0))

// 类型安全的交集
val intersect = closed.intersectTyped(closed)  // 返回 ClosedInterval

// 边界类型在编译时保留
val halfOpen = typedHalfOpenInterval(Flt64(0.0), Flt64(10.0))
// halfOpen.lowerKind == BoundaryKind.Closed
// halfOpen.upperKind == BoundaryKind.Open
```

### 无穷区间

```kotlin
val positive = ValueRange.lowerBounded(Flt64(0.0))  // [0, +∞)
val negative = ValueRange.upperBounded(Flt64(0.0))  // (-∞, 0]
val all = ValueRange.infinite<Flt64>()              // (-∞, +∞)

// 包含检查正确处理无穷
positive.contains(Flt64(1e100))  // true
positive.contains(Flt64(-1.0))   // false
```

## 边界语义

### 开区间 vs 闭区间

| 区间 | 包含 0? | 包含 1? | 包含 2? |
|------|---------|---------|---------|
| `[0, 2]` | ✓ | ✓ | ✓ |
| `(0, 2)` | ✗ | ✓ | ✗ |
| `[0, 2)` | ✓ | ✓ | ✗ |
| `(0, 2]` | ✗ | ✓ | ✓ |

### 空区间

空区间出现在：
- 下界 > 上界
- 单点开区间 `(a, a)`

```kotlin
val empty = ValueRange(Flt64(10.0), Flt64(0.0))
empty.isEmpty  // true
empty.contains(Flt64(5.0))  // false
```

### 半无穷包含检查

```kotlin
val halfOpen = ValueRange(Flt64(0.0), Flt64(10.0), lowerClosed = true, upperClosed = false)
// [0, 10)

halfOpen.contains(Flt64(0.0))   // true（闭下界）
halfOpen.contains(Flt64(10.0))  // false（开上界）
halfOpen.contains(Flt64(10.0), epsilon = Flt64(1e-9))  // 对开边界使用 epsilon
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

### 跨类型操作

```kotlin
val closed = typedClosedInterval(Flt64(0.0), Flt64(10.0))
val open = typedOpenInterval(Flt64(5.0), Flt64(15.0))

// 返回运行时 ValueRange（类型在运行时确定）
val result = closed.intersect(open)
```

## 性能建议

| 操作 | 复杂度 | 说明 |
|------|--------|------|
| `contains` | O(1) | 直接边界比较 |
| `intersect` | O(1) | Max/min 操作 |
| `typedPlus/Minus` | O(1) | 创建新实例 |
| `typedTimes/Div` | O(1) | 符号感知的边界交换 |

高频区间操作建议使用 `TypedValueRange` 以获得编译时优化。

## 测试覆盖

- `TypedValueRangeTest.kt`：包含、交集、算术、空/无穷情况

运行测试：

```powershell
mvn -pl ospf-kotlin-utils -Dtest=TypedValueRangeTest test
```

## 相关链接

- [主 README](../../README.md)
- [Geometry 模块](../../geometry/README.md)
- [Symbol 模块](../../symbol/README.md)