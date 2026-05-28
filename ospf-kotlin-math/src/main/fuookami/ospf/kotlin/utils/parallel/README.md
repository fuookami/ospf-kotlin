# ospf-kotlin-math/parallel

[中文文档 (README_ch.md)](README_ch.md)

Parallel computation utilities using Kotlin coroutines for OSPF Kotlin.

## Operations

| Function | File | Description |
|----------|------|-------------|
| `sumOfParallelly` | `Fold.kt` | Parallel sum via coroutine chunking |
| `trySumOfParallelly` | `Fold.kt` | Parallel sum with error handling (fails fast) |
| `exTrySumOfParallelly` | `Fold.kt` | Parallel sum collecting all errors |

## Usage

```kotlin
import fuookami.ospf.kotlin.math.parallel.*
import fuookami.ospf.kotlin.math.algebra.number.*
import kotlinx.coroutines.runBlocking

val numbers = (1..1000).map { Flt64(it.toDouble()) }

// Simple parallel sum
runBlocking {
    val total = numbers.sumOfParallelly(
        chunkSize = 100
    ) { it }
    println(total)
}

// With error handling
runBlocking {
    val result = numbers.trySumOfParallelly(
        chunkSize = 100
    ) { element ->
        // Return Ret<Flt64> - can fail gracefully
        Ok(element)
    }
}
```

## How It Works

1. **Chunking**: The collection is split into chunks of `chunkSize` elements
2. **Parallel execution**: Each chunk is processed in a separate coroutine using `Dispatchers.Default`
3. **Partial sums**: Each coroutine computes a partial sum independently
4. **Merging**: Partial results are merged in the parent coroutine

This approach limits concurrent coroutines to `collection.size / chunkSize`, preventing resource exhaustion on large collections.

## Related

- [Main README](../../README.md)
- [Combinatorics Module](../combinatorics/README.md)
