# ospf-kotlin-math/combinatorics

[English Documentation (README.md)](./README.md)

OSPF Kotlin 的组合算法，用于生成排列、组合和笛卡尔积。

## 算法

### 排列 (`Permutations.kt`)

| 函数 | 签名 | 描述 |
|------|------|------|
| `permute` | `permute(input: List<T>, callBack?, stopped?): List<List<T>>` | 使用 QuickPerm 算法生成全排列 |
| `permute` | `permute(input: List<T>, choose: Int, callBack?, stopped?): List<List<T>>` | 生成指定大小的排列 |
| `permuteSequence` | `permuteSequence(input: List<T>): Sequence<List<T>>` | 惰性序列生成全排列 |
| `permuteSequence` | `permuteSequence(input: List<T>, choose: Int): Sequence<List<T>>` | 惰性序列生成指定大小的排列 |
| `permuteCount` | `permuteCount(n: Int, choose: Int = n): Long` | 计算排列数 P(n, choose) |
| `permuteAsync` | `permuteAsync(input: List<T>, scope?): ChannelGuard<List<T>>` | 通过协程通道异步生成排列 |

### 组合 (`Combinations.kt`)

| 函数 | 签名 | 描述 |
|------|------|------|
| `combine` | `combine(input: List<T>, callBack?, stopped?): List<List<T>>` | 生成所有子集组合（幂集减去空集） |
| `combine` | `combine(input: List<T>, choose: Int, callBack?, stopped?): List<List<T>>` | 生成指定大小的组合 |
| `combineSequence` | `combineSequence(input: List<T>): Sequence<List<T>>` | 惰性序列生成所有子集组合 |
| `combineSequence` | `combineSequence(input: List<T>, choose: Int): Sequence<List<T>>` | 惰性序列生成指定大小的组合 |
| `combineCount` | `combineCount(n: Int, choose: Int): Long` | 计算组合数 C(n, choose) |
| `combineAsync` | `combineAsync(input: List<T>, scope?): ChannelGuard<List<T>>` | 通过协程通道异步生成组合 |

### 笛卡尔积 (`Cross.kt`)

| 函数 | 签名 | 描述 |
|------|------|------|
| `cross` | `cross(input: List<List<T>>, callBack?, stopped?): List<List<T>>` | 多个集合的笛卡尔积 |
| `crossSequence` | `crossSequence(input: List<List<T>>): Sequence<List<T>>` | 惰性序列生成笛卡尔积 |
| `crossCount` | `crossCount(input: List<List<T>>): Long` | 计算笛卡尔积元素总数 |
| `cross2` | `cross2(lhs: List<A>, rhs: List<B>): List<Pair<A, B>>` | 两个集合的笛卡尔积，返回 Pair 列表 |
| `cross2Sequence` | `cross2Sequence(lhs: List<A>, rhs: List<B>): Sequence<Pair<A, B>>` | 惰性序列生成两个集合的笛卡尔积 |
| `cross3` | `cross3(a: List<A>, b: List<B>, c: List<C>): List<Triple<A, B, C>>` | 三个集合的笛卡尔积，返回 Triple 列表 |
| `cross3Sequence` | `cross3Sequence(a: List<A>, b: List<B>, c: List<C>): Sequence<Triple<A, B, C>>` | 惰性序列生成三个集合的笛卡尔积 |
| `crossAsync` | `crossAsync(input: List<List<T>>, scope?): ChannelGuard<List<T>>` | 通过协程通道异步生成笛卡尔积 |

## 使用示例

```kotlin
import fuookami.ospf.kotlin.math.combinatorics.*

// 排列
val perms = permute(listOf(1, 2, 3))
// [[1, 2, 3], [1, 3, 2], [2, 1, 3], [2, 3, 1], [3, 1, 2], [3, 2, 1]]

val partialPerms = permute(listOf(1, 2, 3), 2)
// [[1, 2], [1, 3], [2, 1], [2, 3], [3, 1], [3, 2]]

val permCount = permuteCount(5, 3)  // P(5, 3) = 60

// 组合
val combs = combine(listOf(1, 2, 3, 4), 2)
// [[1, 2], [1, 3], [1, 4], [2, 3], [2, 4], [3, 4]]

val allSubsets = combine(listOf(1, 2, 3))
// [[1], [2], [1, 2], [3], [1, 3], [2, 3], [1, 2, 3]]

val combCount = combineCount(4, 2)  // C(4, 2) = 6

// 笛卡尔积
val product = cross(listOf(listOf(1, 2), listOf("a", "b")))
// [[1, "a"], [1, "b"], [2, "a"], [2, "b"]]

val pairProduct = cross2(listOf(1, 2), listOf("a", "b"))
// [Pair(1, "a"), Pair(1, "b"), Pair(2, "a"), Pair(2, "b")]

val tripleProduct = cross3(listOf(1, 2), listOf("a", "b"), listOf("x", "y"))
// [Triple(1, "a", "x"), Triple(1, "a", "y"), ...]

val totalCount = crossCount(listOf(listOf(1, 2), listOf("a", "b", "c")))  // 6

// 带回调
val withCallback = permute(listOf(1, 2, 3)) { perm -> println(perm) }

// 提前停止
val earlyStop = combine(listOf(1, 2, 3, 4, 5), 3, stopped = { it == listOf(1, 2, 3) })

// 异步（返回 ChannelGuard）
val asyncPerms = permuteAsync(listOf(1, 2, 3, 4))
```

## 性能

- `permute` 使用 QuickPerm 算法，O(n!) 生成
- `combine` 使用位掩码枚举子集，字典序生成固定大小组合
- `cross` 使用基于索引的迭代，O(各集合大小之积) 生成
- 序列变体（`*Sequence`）是惰性的，对大型结果集内存高效
- 异步变体（`*Async`）使用协程通道（`ChannelGuard`）进行并发流式处理

## 相关链接

- [主 README](../../README.md)
- [Parallel 模块](../../utils/parallel/README_ch.md)
