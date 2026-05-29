/**
 * 批处理调度策略
 * Batch dispatch policy
 */
package fuookami.ospf.kotlin.core.model.intermediate

/** 批处理切片，表示左闭右开区间 / Batch slice representing a half-open interval */
internal data class BatchSlice(
    val fromIndex: Int,
    val toIndexExclusive: Int
)

/** 批处理调度计划 / Batch dispatch plan */
internal data class BatchDispatchPlan(
    val availableProcessors: Int,
    val workerCount: Int,
    val segmentSize: Int,
    val shouldUseParallelPath: Boolean,
    val shouldSplitIntoSegments: Boolean
)

/**
 * 计算批处理调度计划
 * Compute batch dispatch plan
 *
 * 根据项目数量和可用处理器数，计算最优的批处理调度策略。
 * Computes the optimal batch dispatch strategy based on item count and available processors.
 *
 * @param itemCount 项目数量 / Item count
 * @param availableProcessors 可用处理器数 / Available processors
 * @return 批处理调度计划 / Batch dispatch plan
 */
internal fun computeBatchDispatchPlan(
    itemCount: Int,
    availableProcessors: Int = Runtime.getRuntime().availableProcessors()
): BatchDispatchPlan {
    val normalizedItemCount = itemCount.coerceAtLeast(0)
    val normalizedProcessors = availableProcessors.coerceAtLeast(1)
    val workerCount = (normalizedProcessors - 1).coerceAtLeast(1)
    val ratio = normalizedItemCount / workerCount

    var reducedRatio = ratio
    var factor = 0
    while (reducedRatio >= 10) {
        reducedRatio /= 10
        ++factor
    }

    var segmentSize = 1
    repeat(factor) {
        segmentSize *= 10
    }
    if (factor < 1) {
        segmentSize = 10
    }

    return BatchDispatchPlan(
        availableProcessors = normalizedProcessors,
        workerCount = workerCount,
        segmentSize = segmentSize.coerceAtLeast(1),
        shouldUseParallelPath = normalizedProcessors > 2 && normalizedItemCount > normalizedProcessors,
        shouldSplitIntoSegments = factor >= 1
    )
}

/**
 * 构建批处理切片列表
 * Build batch slice list
 *
 * 将项目按指定段大小分割为多个切片。
 * Splits items into multiple slices by specified segment size.
 *
 * @param itemCount 项目数量 / Item count
 * @param segmentSize 段大小 / Segment size
 * @return 批处理切片列表 / Batch slice list
 */
internal fun buildBatchSlices(
    itemCount: Int,
    segmentSize: Int
): List<BatchSlice> {
    val normalizedItemCount = itemCount.coerceAtLeast(0)
    if (normalizedItemCount == 0) {
        return emptyList()
    }
    val safeSegment = segmentSize.coerceAtLeast(1)
    val batchAmount = (normalizedItemCount + safeSegment - 1) / safeSegment
    return (0 until batchAmount).map { batchIndex ->
        val fromIndex = batchIndex * safeSegment
        BatchSlice(
            fromIndex = fromIndex,
            toIndexExclusive = minOf(normalizedItemCount, fromIndex + safeSegment)
        )
    }
}
