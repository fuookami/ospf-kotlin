# ospf-kotlin-math/combinatorics

[Chinese Documentation (README_ch.md)](./README_ch.md)

Combinatorial algorithms for permutations, combinations, and Cartesian products in OSPF Kotlin.

## Algorithms

### Permutations (`Permutations.kt`)

| Function | Signature | Description |
|----------|-----------|-------------|
| `permute` | `permute(input: List<T>, callBack?, stopped?): List<List<T>>` | Generate all full permutations using QuickPerm algorithm |
| `permute` | `permute(input: List<T>, choose: Int, callBack?, stopped?): List<List<T>>` | Generate all permutations of specified size |
| `permuteSequence` | `permuteSequence(input: List<T>): Sequence<List<T>>` | Lazy sequence of all full permutations |
| `permuteSequence` | `permuteSequence(input: List<T>, choose: Int): Sequence<List<T>>` | Lazy sequence of permutations of specified size |
| `permuteCount` | `permuteCount(n: Int, choose: Int = n): Long` | Calculate permutation count P(n, choose) |
| `permuteAsync` | `permuteAsync(input: List<T>, scope?): ChannelGuard<List<T>>` | Async permutation generation via coroutine channel |

### Combinations (`Combinations.kt`)

| Function | Signature | Description |
|----------|-----------|-------------|
| `combine` | `combine(input: List<T>, callBack?, stopped?): List<List<T>>` | Generate all subset combinations (power set minus empty) |
| `combine` | `combine(input: List<T>, choose: Int, callBack?, stopped?): List<List<T>>` | Generate all combinations of specified size |
| `combineSequence` | `combineSequence(input: List<T>): Sequence<List<T>>` | Lazy sequence of all subset combinations |
| `combineSequence` | `combineSequence(input: List<T>, choose: Int): Sequence<List<T>>` | Lazy sequence of combinations of specified size |
| `combineCount` | `combineCount(n: Int, choose: Int): Long` | Calculate combination count C(n, choose) |
| `combineAsync` | `combineAsync(input: List<T>, scope?): ChannelGuard<List<T>>` | Async combination generation via coroutine channel |

### Cartesian Products (`Cross.kt`)

| Function | Signature | Description |
|----------|-----------|-------------|
| `cross` | `cross(input: List<List<T>>, callBack?, stopped?): List<List<T>>` | Cartesian product of multiple collections |
| `crossSequence` | `crossSequence(input: List<List<T>>): Sequence<List<T>>` | Lazy sequence of Cartesian product |
| `crossCount` | `crossCount(input: List<List<T>>): Long` | Calculate total count of Cartesian product elements |
| `cross2` | `cross2(lhs: List<A>, rhs: List<B>): List<Pair<A, B>>` | Cartesian product of two collections, returning Pairs |
| `cross2Sequence` | `cross2Sequence(lhs: List<A>, rhs: List<B>): Sequence<Pair<A, B>>` | Lazy sequence of two-collection Cartesian product |
| `cross3` | `cross3(a: List<A>, b: List<B>, c: List<C>): List<Triple<A, B, C>>` | Cartesian product of three collections, returning Triples |
| `cross3Sequence` | `cross3Sequence(a: List<A>, b: List<B>, c: List<C>): Sequence<Triple<A, B, C>>` | Lazy sequence of three-collection Cartesian product |
| `crossAsync` | `crossAsync(input: List<List<T>>, scope?): ChannelGuard<List<T>>` | Async Cartesian product generation via coroutine channel |

## Usage

```kotlin
import fuookami.ospf.kotlin.math.combinatorics.*

// Permutations
val perms = permute(listOf(1, 2, 3))
// [[1, 2, 3], [1, 3, 2], [2, 1, 3], [2, 3, 1], [3, 1, 2], [3, 2, 1]]

val partialPerms = permute(listOf(1, 2, 3), 2)
// [[1, 2], [1, 3], [2, 1], [2, 3], [3, 1], [3, 2]]

val permCount = permuteCount(5, 3)  // P(5, 3) = 60

// Combinations
val combs = combine(listOf(1, 2, 3, 4), 2)
// [[1, 2], [1, 3], [1, 4], [2, 3], [2, 4], [3, 4]]

val allSubsets = combine(listOf(1, 2, 3))
// [[1], [2], [1, 2], [3], [1, 3], [2, 3], [1, 2, 3]]

val combCount = combineCount(4, 2)  // C(4, 2) = 6

// Cartesian products
val product = cross(listOf(listOf(1, 2), listOf("a", "b")))
// [[1, "a"], [1, "b"], [2, "a"], [2, "b"]]

val pairProduct = cross2(listOf(1, 2), listOf("a", "b"))
// [Pair(1, "a"), Pair(1, "b"), Pair(2, "a"), Pair(2, "b")]

val tripleProduct = cross3(listOf(1, 2), listOf("a", "b"), listOf("x", "y"))
// [Triple(1, "a", "x"), Triple(1, "a", "y"), ...]

val totalCount = crossCount(listOf(listOf(1, 2), listOf("a", "b", "c")))  // 6

// With callbacks
val withCallback = permute(listOf(1, 2, 3)) { perm -> println(perm) }

// Early stopping
val earlyStop = combine(listOf(1, 2, 3, 4, 5), 3, stopped = { it == listOf(1, 2, 3) })

// Async (returns ChannelGuard)
val asyncPerms = permuteAsync(listOf(1, 2, 3, 4))
```

## Performance

- `permute` uses the QuickPerm algorithm for O(n!) generation
- `combine` uses bit-mask enumeration for subsets, lexicographic ordering for fixed-size combinations
- `cross` uses index-based iteration for O(product of sizes) generation
- Sequence variants (`*Sequence`) are lazy and memory-efficient for large result sets
- Async variants (`*Async`) use coroutine channels (`ChannelGuard`) for concurrent streaming

## Related

- [Main README](../../README.md)
- [Parallel Module](../../utils/parallel/README.md)
