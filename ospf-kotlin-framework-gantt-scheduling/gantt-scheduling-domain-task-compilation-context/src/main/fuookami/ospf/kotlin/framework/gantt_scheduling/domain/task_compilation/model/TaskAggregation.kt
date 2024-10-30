package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*

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

    fun removeColumn(task: T) {
        if (!_removedTasks.contains(task)) {
            _removedTasks.add(task)
            _tasks.remove(task)
        }
    }

    fun clear() {
        _tasksIteration.clear()
        _tasks.clear()
        _removedTasks.clear()
    }
}
