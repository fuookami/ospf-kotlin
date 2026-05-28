# ospf-kotlin-math/functional

[English Documentation (README.md)](README.md)

OSPF Kotlin 的集合扩展函数，用于求和、平均值和函数式操作。

## 扩展函数

### Iterable 扩展

| 函数 | 描述 |
|------|------|
| `sum()` | 所有元素之和（空集合返回 zero） |
| `sumOrNull()` | 所有元素之和（空集合返回 null） |
| `sumOf(extractor)` | 提取属性的和 |
| `sumOfOrNull(extractor)` | 提取属性的和（空安全） |
| `average()` | 元素的平均值（空集合抛出异常） |
| `averageOrNull()` | 元素的平均值（空集合返回 null） |
| `averageOf(extractor)` | 提取属性的平均值 |
| `averageOfOrNull(extractor)` | 提取属性的平均值（空安全） |

### Sequence 扩展

| 函数 | 描述 |
|------|------|
| `sum()` | 序列元素之和 |
| `sumOrNull()` | 序列元素之和（空安全） |
| `average()` | 序列元素平均值 |
| `averageOrNull()` | 序列元素平均值（空安全） |

### Map 扩展

| 函数 | 描述 |
|------|------|
| `sumValues()` | Map 值之和 |
| `sumValuesOrNull()` | Map 值之和（空安全） |
| `averageValues()` | Map 值平均值 |
| `averageValuesOrNull()` | Map 值平均值（空安全） |

## 使用示例

```kotlin
import fuookami.ospf.kotlin.math.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64

// Iterable 求和
val list = listOf(Flt64(1.0), Flt64(2.0), Flt64(3.0))
val total = list.sum()  // Flt64(6.0)

// 空安全求和
val nullableList = listOf<Flt64?>(Flt64(1.0), null, Flt64(3.0))
val safeTotal = nullableList.sumOrNull()  // null（包含 null 元素）

// 平均值
val avg = list.average()  // Flt64(2.0)

// Map 值求和
val map = mapOf("a" to Flt64(10.0), "b" to Flt64(20.0))
val mapSum = map.sumValues()  // Flt64(30.0)
```

## 相关链接

- [主 README](../../README.md)
- [Algebra 模块](../algebra/README_ch.md)
