package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.service

import kotlinx.datetime.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*
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
        assignedPolicyGenerator: (executor: E?) -> A?
    ): Ret<Solution<T, E, A>> {
        val assignedExecutor = HashMap<AbstractTask<E, A>, E>()
        for (token in model.tokens.tokens) {
            if (token.belongsTo(compilation.x) && token.result?.let { it eq Flt64.one } == true) {
                val task = token.variable.vectorView[0]
                val executor = token.variable.vectorView[1]
                assignedExecutor[tasks.find { it.index == task }!!] = executors.find { it.index == executor }!!
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

        return Ok(Solution(assignedTasks, canceledTasks))
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
        assignedPolicyGenerator: (time: TimeRange?, executor: E?) -> A?
    ): Ret<Solution<T, E, A>> {
        val assignedExecutor = HashMap<AbstractTask<E, A>, E>()
        for (token in model.tokens.tokens) {
            if (token.belongsTo(compilation.x) && token.result?.let { it eq Flt64.one } == true) {
                val task = token.variable.vectorView[0]
                val executor = token.variable.vectorView[1]
                assignedExecutor[tasks.find { it.index == task }!!] = executors.find { it.index == executor }!!
            }
        }

        val assignedEST = HashMap<AbstractTask<E, A>, Instant>()
        for (token in model.tokens.tokens) {
            if (token.belongsTo(taskTime.est)) {
                val task = token.variable.vectorView[0]
                assignedEST[tasks.find { it.index == task }!!] = timeWindow.instantOf(token.result!!)
            }
        }

        val assignedECT = tasks.associateWith { task ->
            taskTime.estimateEndTime[task].value(results, model.tokens)?.let { timeWindow.instantOf(it) }
                ?: Instant.DISTANT_FUTURE
        }

        val assignedTime = tasks.mapNotNull {
            if (assignedEST.containsKey(it) && assignedECT.containsKey(it)) {
                Pair(it, TimeRange(assignedEST[it]!!, assignedECT[it]!!))
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

        return Ok(Solution(assignedTasks, canceledTasks))
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
        model: AbstractLinearMetaModel
    ): Ret<Solution<T, E, A>> {
        val assignedTasks = ArrayList<T>()
        val canceledTasks = ArrayList<T>()
        for (token in model.tokens.tokens) {
            for ((i , xi) in compilation.x.withIndex()) {
                if (UInt64(i.toULong()) > iteration) {
                    break
                }

                if (token.belongsTo(xi) && token.result?.let { it eq Flt64.one } == true) {
                    val assignedTask = tasks[i][token.variable.vectorView[0]]
                    assignedTasks.add(assignedTask as T)
                }
            }

            if (token.belongsTo(compilation.y) && token.result?.let { it eq Flt64.one } == true) {
                canceledTasks.add(originTasks[token.variable.vectorView[0]])
            }
        }

        return Ok(Solution(assignedTasks, canceledTasks))
    }
}
