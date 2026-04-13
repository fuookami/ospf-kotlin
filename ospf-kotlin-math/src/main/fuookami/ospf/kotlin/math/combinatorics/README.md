# ospf-kotlin-math/combinatorics

[中文文档 (README_ch.md)](./README_ch.md)

Combinatorial algorithms for permutations, combinations, and Cartesian products in OSPF Kotlin.

## Algorithms

| Algorithm | File | Description |
|-----------|------|-------------|
| `permute` | `Permutations.kt` | Generate all permutations using QuickPerm algorithm |
| `permuteCount` | `Permutations.kt` | Calculate permutation count P(n, k) |
| `permuteSequence` | `Permutations.kt` | Lazy sequence of permutations |
| `permuteAsync` | `Permutations.kt` | Async permutation generation via coroutine channels |
| `combine` | `Combinations.kt` | Generate all k-combinations |
| `combineCount` | `Combinations.kt` | Calculate combination count C(n, k) |
| `combineSequence` | `Combinations.kt` | Lazy sequence of combinations |
| `combineAsync` | `Combinations.kt` | Async combination generation |
| `cross` | `Cross.kt` | Cartesian product of two or more collections |
| `crossWithSelf` | `Cross.kt` | Cartesian product of a collection with itself |

## Usage

```kotlin
import fuookami.ospf.kotlin.math.combinatorics.*

// Permutations
val perms = permute(listOf(1, 2, 3))
// [[1, 2, 3], [1, 3, 2], [2, 1, 3], [2, 3, 1], [3, 1, 2], [3, 2, 1]]

val count = permuteCount(5, 3)  // P(5, 3) = 60

// Combinations
val combs = combine(listOf(1, 2, 3, 4), k = 2)
// [[1, 2], [1, 3], [1, 4], [2, 3], [2, 4], [3, 4]]

val countC = combineCount(4, 2)  // C(4, 2) = 6

// Cartesian product
val product = cross(listOf(1, 2), listOf("a", "b"))
// [[1, "a"], [1, "b"], [2, "a"], [2, "b"]]

val selfProduct = crossWithSelf(listOf(1, 2, 3), 2)
// [[1, 1], [1, 2], [1, 3], [2, 1], [2, 2], [2, 3], [3, 1], [3, 2], [3, 3]]
```

## Performance

- `permute` uses the QuickPerm algorithm for O(n!) generation
- `combine` uses lexicographic ordering for O(C(n,k)) generation
- `cross` uses nested iteration for O(n*m) generation
- Async variants use coroutine channels for memory-efficient streaming

## Related

- [Main README](../../README.md)
- [Parallel Module](../parallel/README.md)
