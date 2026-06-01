# ospf-kotlin-math/parallel

:us: [English](README.md) | :cn: 简体中文

OSPF Kotlin 的并行计算工具，使用 Kotlin 协程实现。

## 运算

| 函数 | 文件 | 描述 |
|------|------|------|
| `sumOfParallelly` | `Fold.kt` | 通过协程分块的并行求和 |
| `trySumOfParallelly` | `Fold.kt` | 带错误处理的并行求和（快速失败） |
| `exTrySumOfParallelly` | `Fold.kt` | 收集所有错误的并行求和 |

## 使用示例

```kotlin
import fuookami.ospf.kotlin.math.parallel.*
import fuookami.ospf.kotlin.math.algebra.number.*
import kotlinx.coroutines.runBlocking

val numbers = (1..1000).map { Flt64(it.toDouble()) }

// 简单并行求和
runBlocking {
    val total = numbers.sumOfParallelly(
        chunkSize = 100
    ) { it }
    println(total)
}

// 带错误处理
runBlocking {
    val result = numbers.trySumOfParallelly(
        chunkSize = 100
    ) { element ->
        // 返回 Ret<Flt64> - 可以优雅地失败
        Ok(element)
    }
}
```

## 工作原理

1. **分块**：集合按 `chunkSize` 大小分成块
2. **并行执行**：每个块在独立的协程中使用 `Dispatchers.Default` 处理
3. **部分求和**：每个协程独立计算部分和
4. **合并**：部分结果在父协程中合并

这种方法将并发协程数量限制为 `collection.size / chunkSize`，防止在大集合上耗尽资源。

## 相关链接

- [主 README](../../README.md)
- [Combinatorics 模块](../combinatorics/README_ch.md)
