# ospf-kotlin-math/combinatorics

[English Documentation (README.md)](./README.md)

OSPF Kotlin 的组合算法，用于生成排列、组合和笛卡尔积。

## 算法

| 算法 | 文件 | 描述 |
|------|------|------|
| `permute` | `Permutations.kt` | 使用 QuickPerm 算法生成全排列 |
| `permuteCount` | `Permutations.kt` | 计算排列数 P(n, k) |
| `permuteSequence` | `Permutations.kt` | 惰性序列生成排列 |
| `permuteAsync` | `Permutations.kt` | 通过协程通道异步生成排列 |
| `combine` | `Combinations.kt` | 生成所有 k-组合 |
| `combineCount` | `Combinations.kt` | 计算组合数 C(n, k) |
| `combineSequence` | `Combinations.kt` | 惰性序列生成组合 |
| `combineAsync` | `Combinations.kt` | 异步生成组合 |
| `cross` | `Cross.kt` | 两个或多个集合的笛卡尔积 |
| `crossWithSelf` | `Cross.kt` | 集合与自身的笛卡尔积 |

## 使用示例

```kotlin
import fuookami.ospf.kotlin.math.combinatorics.*

// 排列
val perms = permute(listOf(1, 2, 3))
// [[1, 2, 3], [1, 3, 2], [2, 1, 3], [2, 3, 1], [3, 1, 2], [3, 2, 1]]

val count = permuteCount(5, 3)  // P(5, 3) = 60

// 组合
val combs = combine(listOf(1, 2, 3, 4), k = 2)
// [[1, 2], [1, 3], [1, 4], [2, 3], [2, 4], [3, 4]]

val countC = combineCount(4, 2)  // C(4, 2) = 6

// 笛卡尔积
val product = cross(listOf(1, 2), listOf("a", "b"))
// [[1, "a"], [1, "b"], [2, "a"], [2, "b"]]

val selfProduct = crossWithSelf(listOf(1, 2, 3), 2)
// [[1, 1], [1, 2], [1, 3], [2, 1], [2, 2], [2, 3], [3, 1], [3, 2], [3, 3]]
```

## 性能

- `permute` 使用 QuickPerm 算法，O(n!) 生成
- `combine` 使用字典序，O(C(n,k)) 生成
- `cross` 使用嵌套迭代，O(n*m) 生成
- 异步变体使用协程通道，内存高效的流式处理

## 相关链接

- [主 README](../../README.md)
- [Parallel 模块](../parallel/README_ch.md)
