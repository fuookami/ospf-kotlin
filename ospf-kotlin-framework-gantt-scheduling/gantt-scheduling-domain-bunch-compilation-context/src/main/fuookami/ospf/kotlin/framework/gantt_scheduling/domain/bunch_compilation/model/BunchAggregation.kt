@file:Suppress("DEPRECATION")

/** 任务束聚合 / Bunch aggregation */
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.model

import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AbstractTask
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AbstractTaskBunch
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AssignmentPolicy
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.Executor
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.utils.concept.ManualIndexed
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * 任务束聚合 / Bunch aggregation
 *
 * @param B 任务束类型 / Bunch type
 * @param T 任务类型 / Task type
 * @param E 执行器类型 / Executor type
 * @param A 分配策略类型 / Assignment policy type
 * @param _bunchesIteration 任务束迭代列表 / Bunch iteration list
 * @param _bunches 任务束列表 / Bunch list
 * @param _removedBunches 已移除任务束集合 / Removed bunches set
 */
open class BunchAggregation<
        B : AbstractTaskBunch<T, E, A, V>,
        V : RealNumber<V>,
        out T : AbstractTask<E, A>,
        out E : Executor,
        out A : AssignmentPolicy<E>
        >(
    private val _bunchesIteration: MutableList<List<B>> = ArrayList(),
    private val _bunches: MutableList<B> = ArrayList(),
    private val _removedBunches: MutableSet<B> = HashSet()
) {
    val bunchesIteration: List<List<B>> by ::_bunchesIteration
    val bunches: List<B> by ::_bunches
    val removedBunches: Set<B> by ::_removedBunches
    val lastIterationBunches: List<B>
        get() = _bunchesIteration.lastOrNull { it.isNotEmpty() } ?: emptyList()

    /**
     * 添加列 / Add columns
     *
     * @param newBunches 新任务束列表 / List of new bunches
     * @return 去重后的新任务束列表 / Deduplicated list of new bunches
     */
    suspend fun addColumns(newBunches: List<B>): List<B> {
        val unduplicatedNewBunches = coroutineScope {
            val promises = ArrayList<Deferred<List<B>>>()
            for (bunches in newBunches.groupBy { Pair(it.executor, it.tasks.size) }.values) {
                promises.add(async(Dispatchers.Default) {
                    val unduplicatedNewBunches = ArrayList<B>()
                    for (bunch in bunches) {
                        if (unduplicatedNewBunches.none { bunch sameColumnAs it }) {
                            unduplicatedNewBunches.add(bunch)
                        }
                    }
                    unduplicatedNewBunches
                })
            }
            promises.flatMap { it.await() }
        }

        val unduplicatedBunches = coroutineScope {
            val promises = ArrayList<Deferred<B?>>()
            for (bunch in unduplicatedNewBunches) {
                promises.add(async(Dispatchers.Default) {
                    if (_bunches.none { bunch sameColumnAs it }) {
                        bunch
                    } else {
                        null
                    }
                })
            }
            promises.mapNotNull { it.await() }
        }

        ManualIndexed.flush(AbstractTaskBunch::class)
        for (bunch in unduplicatedBunches) {
            bunch.setIndexed(AbstractTaskBunch::class)
        }
        _bunchesIteration.add(unduplicatedBunches)
        _bunches.addAll(unduplicatedBunches)

        return unduplicatedBunches
    }

    /**
     * 检查是否为同一列 / Check if same column
     *
     * @param other 另一个任务束 / Another bunch
     * @return 是否为同一列 / Whether same column
     */
    protected open infix fun B.sameColumnAs(other: B): Boolean {
        return !(this neq other)
    }

    /**
     * 移除列 / Remove column
     *
     * @param bunch 要移除的任务束 / Bunch to remove
     */
    fun removeColumn(bunch: B) {
        if (!_removedBunches.contains(bunch)) {
            _removedBunches.add(bunch)
            _bunches.remove(bunch)
        }
    }

    /**
     * 移除多列 / Remove columns
     *
     * @param bunches 要移除的任务束列表 / List of bunches to remove
     */
    fun removeColumns(bunches: List<B>) {
        for (bunch in bunches) {
            removeColumn(bunch)
        }
    }

    /** 清空所有数据 / Clear all data */
    fun clear() {
        _bunchesIteration.clear()
        _bunches.clear()
        _removedBunches.clear()
    }
}