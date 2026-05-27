# ospf-kotlin-math/multiarray

[English Documentation (README.md)](README.md)

OSPF Kotlin 的多维数组运算，包括快速求和与爱因斯坦求和（einsum）。

## 功能

### 快速求和运算（`FastSum.kt`）

| 运算 | 描述 |
|------|------|
| `sumAll` | 对数组所有元素求和 |
| `sumAxis` | 沿单个轴求和 |
| `sumAxes` | 沿多个轴求和 |
| `cumsumAxis` | 沿轴的累积求和 |

### MultiArray 扩展（`MultiArrayExtensions.kt`）

为 `AbstractMultiArray` 类型提供扩展函数，方便访问快速求和运算并正确处理零值。

### 爱因斯坦求和（`einsum/`）

使用爱因斯坦表示法进行强大的张量运算，简洁地表达缩并、矩阵乘法等操作。

## 使用示例

```kotlin
import fuookami.ospf.kotlin.multiarray.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64

// 创建 2D 数组
val arr = MultiArray.newWith(Shape2(3, 4), Flt64(1.0))

// 对所有元素求和
val total = arr.sumAll(Flt64.zero)

// 沿轴 0 求和
val sumAxis0 = arr.sumAxis(0, Flt64.zero)  // 形状: [4]

// 沿轴 1 求和
val sumAxis1 = arr.sumAxis(1, Flt64.zero)  // 形状: [3]

// 沿轴 1 累积求和
val cumsum = arr.cumsumAxis(1, Flt64.zero)
```

### 爱因斯坦求和

```kotlin
import fuookami.ospf.kotlin.multiarray.einsum.*

// 矩阵乘法
val c = einsumDouble(a, b, "ij,jk->ik")

// 张量缩并
val contracted = einsumDouble(tensor, "ijk->ij")

// 外积
val outer = einsumDouble(v1, v2, "i,j->ij")

// 批量矩阵乘法
val batchC = einsumDouble(batchA, batchB, "bij,bjk->bik")
```

## 相关链接

- [主 README](../../README.md)
- [Functional 模块](../math/functional/README_ch.md)
- [Algebra 模块](../math/algebra/README_ch.md)
