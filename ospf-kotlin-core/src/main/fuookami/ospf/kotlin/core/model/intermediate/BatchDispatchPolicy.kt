package fuookami.ospf.kotlin.core.model.intermediate

internal data class BatchSlice(
    val fromIndex: Int,
    val toIndexExclusive: Int
)

internal data class BatchDispatchPlan(
    val availableProcessors: Int,
    val workerCount: Int,
    val segmentSize: Int,
    val shouldUseParallelPath: Boolean,
    val shouldSplitIntoSegments: Boolean
)

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
