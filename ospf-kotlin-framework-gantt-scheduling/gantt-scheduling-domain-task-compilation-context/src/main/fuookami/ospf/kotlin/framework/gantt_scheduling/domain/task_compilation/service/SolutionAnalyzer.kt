package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.service

import kotlinx.datetime.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.model.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model.*

data object SolutionAnalyzer {
    @Suppress("UNCHECKED_CAST")
    operator fun <
        T : AbstractTask<E, A>,
        E : Executor,
        A : AssignmentPolicy<E>
    > invoke(
        tasks: List<T>,
        executors: List<E>,
        compilation: TaskCompilation<T, E, A>,
        model: AbstractLinearMetaModel,
        assignedPolicyGenerator: (executor: E?) -> A?,
        solution: Solution? = null,
    ): Ret<TaskSolution<T, E, A>> {
        val assignedExecutor = HashMap<AbstractTask<E, A>, E>()
        for (x in compilation.x) {
            val token = model.tokens.find(x) ?: continue
            val result = if (token.result != null) {
                token.result!!
            } else {
                val index = model.tokens.indexOf(token) ?: continue
                solution?.get(index) ?: continue
            }.round().toUInt64()
            if (result geq UInt64.one) {
                val task = tasks.findOrGet(x.vectorView[0])
                val executor = executors.findOrGet(x.vectorView[1])
                assignedExecutor[task] = executor
            }
        }

        val assignedTasks = ArrayList<T>()
        val canceledTasks = ArrayList<T>()
        for (task in tasks) {
            val assignedTask = assignedPolicyGenerator(assignedExecutor[task])?.let {
                when (val result = task.assign(it)) {
                    is Ok -> {
                        result.value
                    }

                    is Failed -> {
                        return Failed(result.error)
                    }
                }
            }
            if (assignedTask != null) {
                assignedTasks.add(assignedTask as T)
            } else {
                canceledTasks.add(task)
            }
        }

        return Ok(TaskSolution(assignedTasks, canceledTasks))
    }

    @Suppress("UNCHECKED_CAST")
    operator fun <
        T : AbstractTask<E, A>,
        E : Executor,
        A : AssignmentPolicy<E>
    > invoke(
        timeWindow: TimeWindow,
        tasks: List<T>,
        executors: List<E>,
        compilation: TaskCompilation<T, E, A>,
        taskTime: TaskSchedulingTaskTime<T, E, A>,
        results: List<Flt64>,
        model: AbstractLinearMetaModel,
        assignedPolicyGenerator: (time: TimeRange?, executor: E?) -> A?,
        solution: Solution? = null,
    ): Ret<TaskSolution<T, E, A>> {
        val assignedExecutor = HashMap<AbstractTask<E, A>, E>()
        for (x in compilation.x) {
            val token = model.tokens.find(x) ?: continue
            val result = if (token.result != null) {
                token.result!!
            } else {
                val index = model.tokens.indexOf(token) ?: continue
                solution?.get(index) ?: continue
            }.round().toUInt64()
            if (result geq UInt64.one) {
                val task = tasks.findOrGet(x.vectorView[0])
                val executor = executors.findOrGet(x.vectorView[1])
                assignedExecutor[task] = executor
            }
        }

        val assignedEST = HashMap<AbstractTask<E, A>, Instant>()
        for (est in taskTime.est) {
            val task = tasks.findOrGet(est.index)
            if (!assignedExecutor.containsKey(task)) {
                continue
            }

            val token = model.tokens.find(est) ?: continue
            val result = if (token.result != null) {
                token.result!!
            } else {
                val index = model.tokens.indexOf(token) ?: continue
                solution?.get(index) ?: continue
            }
            assignedEST[task] = timeWindow.instantOf(result)
        }

        val assignedECT = assignedExecutor.entries.associate { (task, _) ->
            task to (taskTime.estimateEndTime[task].evaluate(results, model.tokens)?.let {
                with(timeWindow) { it.instant }
            } ?: Instant.DISTANT_FUTURE)
        }

        val assignedTime = assignedExecutor.entries.mapNotNull { (task, _) ->
            if (assignedEST.containsKey(task) && assignedECT.containsKey(task)) {
                task to TimeRange(assignedEST[task]!!, assignedECT[task]!!)
            } else {
                null
            }
        }.toMap()

        val assignedTasks = ArrayList<T>()
        val canceledTasks = ArrayList<T>()
        for (task in tasks) {
            val assignedTask = assignedPolicyGenerator(assignedTime[task], assignedExecutor[task])?.let {
                when (val result = task.assign(it)) {
                    is Ok -> {
                        result.value
                    }

                    is Failed -> {
                        return Failed(result.error)
                    }
                }
            }
            if (assignedTask != null) {
                assignedTasks.add(assignedTask as T)
            } else {
                canceledTasks.add(task)
            }
        }

        return Ok(TaskSolution(assignedTasks, canceledTasks))
    }

    @Suppress("UNCHECKED_CAST")
    operator fun <
        IT : IterativeAbstractTask<E, A>,
        T : AbstractTask<E, A>,
        E : Executor,
        A : AssignmentPolicy<E>
    > invoke(
        iteration: UInt64,
        originTasks: List<T>,
        tasks: List<List<IT>>,
        compilation: IterativeTaskCompilation<IT, T, E, A>,
        model: AbstractLinearMetaModel,
        solution: Solution? = null,
    ): Ret<TaskSolution<T, E, A>> {
        val assignedTasks = ArrayList<T>()
        val canceledTasks = ArrayList<T>()

        for ((i, xi) in compilation.x.withIndex()) {
            if (UInt64(i.toULong()) > iteration) {
                break
            }

            for (x in xi) {
                val token = model.tokens.find(x) ?: continue
                val result = if (token.result != null) {
                    token.result!!
                } else {
                    val index = model.tokens.indexOf(token) ?: continue
                    solution?.get(index) ?: continue
                }.round().toUInt64()
                if (result geq UInt64.one) {
                    val task = tasks[i].findOrGet(x.index)
                    assignedTasks.add(task as T)
                }
            }
        }

        for (y in compilation.y) {
            val token = model.tokens.find(y) ?: continue
            val result = if (token.result != null) {
                token.result!!
            } else {
                val index = model.tokens.indexOf(token) ?: continue
                solution?.get(index) ?: continue
            }.round().toUInt64()
            if (result eq UInt64.one) {
                val task = originTasks.findOrGet(y.index)
                canceledTasks.add(task)
            }
        }

        return Ok(TaskSolution(assignedTasks, canceledTasks))
    }
}
