/**
 * 机制模型转储支持
 * Mechanism model dump support
*/
package fuookami.ospf.kotlin.core.model.mechanism

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.math.usize
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.model.intermediate.*

/** 创建约束转储进度回调函数 / Create a constraint dumping progress callback function */
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

/** 通知约束转储完成 / Notify that constraint dumping has completed */
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

/**
 * 异步批量转储项目
 * Dump items asynchronously in batches
 *
 * @param T 输入类型 / Input type
 * @param R 输出类型 / Output type
 * @param items 待转储项目列表 / Items to dump
 * @param scope 协程作用域 / Coroutine scope
 * @param memoryCheckInSingleTask 单任务时是否检查内存 / Whether to check memory in single task
 * @param onProgress 进度回调 / Progress callback
 * @param transform 转换函数 / Transform function
 * @return 转储结果列表 / List of dumped results
*/
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

/**
 * 异步转储机制模型部件（约束和子目标）
 * Dump mechanism model parts (constraints and sub-objectives) asynchronously
 *
 * @param V 数值类型 / The number type
 * @param RC 关系约束类型 / Relation constraint type
 * @param SO 子目标类型 / Sub-objective type
 * @param C 输出约束类型 / Output constraint type
 * @param S 输出子目标类型 / Output sub-objective type
 * @param metaModel 元模型 / Meta model
 * @param relationConstraints 关系约束列表 / Relation constraint list
 * @param subObjects 子目标列表 / Sub-objective list
 * @param scope 协程作用域 / Coroutine scope
 * @param callBack 转储状态回调 / Dump status callback
 * @param memoryCheckInSingleTask 单任务时是否检查内存 / Whether to check memory in single task
 * @param createConstraint 约束创建函数 / Constraint creation function
 * @param createSubObject 子目标创建函数 / Sub-objective creation function
 * @return 约束和子目标对 / Pair of constraints and sub-objectives
*/
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
