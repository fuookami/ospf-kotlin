/** 任务聚合 / Task aggregation */
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*

/**
 * 任务聚合 / Task aggregation
 *
 * @param T 迭代任务类型 / Iterative task type
 * @param E 执行器类型 / Executor type
 * @param A 分配策略类型 / Assignment policy type
 * @param _tasksIteration 任务迭代列表 / Task iteration list
 * @param _tasks 任务列表 / Task list
 * @param _removedTasks 已移除任务集合 / Removed tasks set
 * @property tasksIteration 任务迭代列表 / Task iteration list
 * @property tasks 任务列表 / Task list
 * @property removedTasks 已移除任务集合 / Removed tasks set
 * @property lastIterationTasks 最后一轮迭代的任务列表 / Task list from the last iteration
*/
data class TaskAggregation<
        T : IterativeAbstractTask<E, A>,
        out E : Executor,
        out A : AssignmentPolicy<E>
        >(
    private val _tasksIteration: MutableList<List<T>> = ArrayList(),
    private val _tasks: MutableList<T> = ArrayList(),
    private val _removedTasks: MutableSet<T> = HashSet()
) {
    val tasksIteration: List<List<T>> by ::_tasksIteration
    val tasks: List<T> by ::_tasks
    val removedTasks: Set<T> by ::_removedTasks
    val lastIterationTasks: List<T>
        get() = _tasksIteration.lastOrNull { it.isNotEmpty() } ?: emptyList()

    /**
     * 添加列 / Add columns
     *
     * @param newTasks 新任务列表 / List of new tasks
     * @return 去重后的新任务列表 / Deduplicated list of new tasks
    */
    suspend fun addColumns(newTasks: List<T>): List<T> {
        val unduplicatedNewTasks = ArrayList<T>()
        for (task in newTasks) {
            if (unduplicatedNewTasks.all { task neq it }) {
                unduplicatedNewTasks.add(task)
            }
        }

        val unduplicatedTasks = coroutineScope {
            val promises = ArrayList<Deferred<T?>>()
            for (task in unduplicatedNewTasks) {
                promises.add(async(Dispatchers.Default) {
                    if (_tasks.all { task neq it }) {
                        task
                    } else {
                        null
                    }
                })
            }
            promises.mapNotNull { it.await() }
        }

        ManualIndexed.flush(IterativeAbstractTask::class)
        for (task in unduplicatedTasks.filterIsInstance<ManualIndexed>()) {
            task.setIndexed(IterativeAbstractTask::class)
        }
        _tasksIteration.add(unduplicatedTasks)
        _tasks.addAll(unduplicatedTasks)

        return unduplicatedTasks
    }

    /**
     * 移除列 / Remove column
     *
     * @param task 要移除的任务 / Task to remove
    */
    fun removeColumn(task: T) {
        if (!_removedTasks.contains(task)) {
            _removedTasks.add(task)
            _tasks.remove(task)
        }
    }

    /** 清空所有数据 / Clear all data */
    fun clear() {
        _tasksIteration.clear()
        _tasks.clear()
        _removedTasks.clear()
    }
}
