/**
 * 机制模型转储支持
 * Mechanism model dump support
 */
package fuookami.ospf.kotlin.core.model.mechanism

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.math.usize
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.core.model.intermediate.*

private fun <V> dumpingConstraintsProgressCallback(
    metaModel: MetaModel<V>,
    callBack: MechanismModelDumpingStatusCallBack?
): ((UInt64) -> Unit)? where V : RealNumber<V>, V : NumberField<V> {
    return callBack?.let { callback ->
        { ready ->
            callback(
                MechanismModelDumpingStatus.dumpingConstrains(
                    ready = ready,
                    model = metaModel
                )
            )
        }
    }
}

private fun <V> notifyDumpingConstraintsCompleted(
    metaModel: MetaModel<V>,
    callBack: MechanismModelDumpingStatusCallBack?
) where V : RealNumber<V>, V : NumberField<V> {
    if (callBack != null) {
        callBack(
            MechanismModelDumpingStatus.dumpingConstrains(
                ready = metaModel.constraints.usize,
                model = metaModel
            )
        )
    }
}

internal suspend fun <T, R> dumpItemsAsync(
    items: List<T>,
    scope: CoroutineScope,
    memoryCheckInSingleTask: Boolean,
    onProgress: ((UInt64) -> Unit)? = null,
    transform: (T) -> R
): List<R> {
    val dispatchPlan = computeBatchDispatchPlan(items.size)
    val deferredItems = if (dispatchPlan.shouldSplitIntoSegments) {
        val completedLock = Any()
        var completed = UInt64.zero
        buildBatchSlices(
            itemCount = items.size,
            segmentSize = dispatchPlan.segmentSize
        ).map { slice ->
            scope.async(Dispatchers.Default) {
                val result = items
                    .subList(
                        slice.fromIndex,
                        slice.toIndexExclusive
                    ).map(transform)
                MemoryCleanupPolicy.cleanupAfterAsyncBatch()
                if (onProgress != null) {
                    synchronized(completedLock) {
                        completed += result.usize
                        onProgress(completed)
                    }
                }
                result
            }
        }
    } else {
        items.map { item ->
            scope.async(Dispatchers.Default) {
                val result = listOf(transform(item))
                if (memoryCheckInSingleTask) {
                    MemoryCleanupPolicy.cleanupAfterAsyncBatch()
                }
                result
            }
        }
    }
    return deferredItems.flatMap { it.await() }
}

internal suspend fun <V, RC, SO, C, S> dumpMechanismPartsAsync(
    metaModel: MetaModel<V>,
    relationConstraints: List<RC>,
    subObjects: List<SO>,
    scope: CoroutineScope,
    callBack: MechanismModelDumpingStatusCallBack?,
    memoryCheckInSingleTask: Boolean,
    createConstraint: (RC) -> C,
    createSubObject: (SO) -> S
): Pair<List<C>, List<S>> where V : RealNumber<V>, V : NumberField<V> {
    val constraints = dumpItemsAsync(
        items = relationConstraints,
        scope = scope,
        memoryCheckInSingleTask = memoryCheckInSingleTask,
        onProgress = dumpingConstraintsProgressCallback(metaModel, callBack),
        transform = createConstraint
    )
    val dumpedSubObjects = dumpItemsAsync(
        items = subObjects,
        scope = scope,
        memoryCheckInSingleTask = memoryCheckInSingleTask,
        transform = createSubObject
    )

    notifyDumpingConstraintsCompleted(metaModel, callBack)
    return constraints to dumpedSubObjects
}
