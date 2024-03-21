package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_scheduling.model

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*

data class TaskAggregation<E : Executor, A : AssignmentPolicy<E>>(
    private val _tasksIteration: MutableList<List<AbstractTask<E, A>>> = ArrayList(),
    private val _tasks: MutableList<AbstractTask<E, A>> = ArrayList(),
    private val _removedTasks: MutableMap<AbstractTask<E, A>, UInt64> = HashMap()
) {
    val tasksIteration: List<List<AbstractTask<E, A>>> by ::_tasksIteration
    val tasks: List<AbstractTask<E, A>> by ::_tasks
    val removedTasks: Map<AbstractTask<E, A>, UInt64> by ::_removedTasks
    val lastIterationTasks: List<AbstractTask<E, A>>
        get() = _tasksIteration.lastOrNull { it.isNotEmpty() } ?: emptyList()

    suspend fun addColumns(newTasks: List<AbstractTask<E, A>>): List<AbstractTask<E, A>> {
        val unduplicatedNewTasks = ArrayList<AbstractTask<E, A>>()
        for (task in newTasks) {
            if (unduplicatedNewTasks.all { task neq it }) {
                unduplicatedNewTasks.add(task)
            }
        }

        val unduplicatedTasks = coroutineScope {
            val promises = ArrayList<Deferred<AbstractTask<E, A>?>>()
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

        ManualIndexed.flush(AbstractTask::class)
        for (task in unduplicatedTasks.filterIsInstance<ManualIndexed>()) {
            task.setIndexed(AbstractTask::class)
        }
        _tasksIteration.add(unduplicatedTasks)
        _tasks.addAll(unduplicatedTasks)

        return unduplicatedTasks
    }

    fun removeColumn(
        iteration: UInt64,
        task: AbstractTask<E, A>
    ) {
        if (!_removedTasks.contains(task)) {
            _removedTasks[task] = iteration
            _tasks.remove(task)
        }
    }

    fun clear() {
        _tasksIteration.clear()
        _tasks.clear()
        _removedTasks.clear()
    }
}
