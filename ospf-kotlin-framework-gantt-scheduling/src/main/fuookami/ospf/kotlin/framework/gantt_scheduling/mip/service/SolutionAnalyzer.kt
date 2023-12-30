package fuookami.ospf.kotlin.framework.gantt_scheduling.mip.service

import kotlinx.datetime.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.mip.model.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.LinearSymbol

typealias AssignedTaskGenerator<E> = (task: Task<E>, time: TimeRange, executor: E) -> Task<E>?

class SolutionAnalyzer<E : Executor> {
    operator fun invoke(
        timeWindow: TimeWindow,
        tasks: List<Task<E>>,
        executors: List<E>,
        compilation: Compilation<E>,
        taskTime: TaskTime<E>,
        result: List<Flt64>,
        model: LinearMetaModel,
        assignedTaskGenerator: AssignedTaskGenerator<E>? = null
    ): Ret<Solution<E>> {
        val assignedExecutor = HashMap<Task<E>, E>()
        for (token in model.tokens.tokens) {
            if (token.name.startsWith(compilation.x.name) && token.result?.let { it eq Flt64.one } == true) {
                val task = token.variable.vectorView[0]
                val executor = token.variable.vectorView[1]
                assignedExecutor[tasks.find { it.index == task }!!] = executors.find { it.index == executor }!!
            }
        }

        val assignedEST = HashMap<Task<E>, Instant>()
        for (token in model.tokens.tokens) {
            if (token.name.startsWith(taskTime.est.name)) {
                val task = token.variable.vectorView[0]
                assignedEST[tasks.find { it.index == task }!!] = timeWindow.dump(token.result!!)
            }
        }
        val assignedECT = tasks.associateWith { timeWindow.dump((taskTime.ect[it]!! as LinearSymbol).polynomial.value(result, model.tokens)) }

        val assignedTime = tasks.mapNotNull {
            if (assignedEST.containsKey(it) && assignedECT.containsKey(it)) {
                Pair(it, TimeRange(assignedEST[it]!!, assignedECT[it]!!))
            } else {
                null
            }
        }.toMap()

        val assignedTasks = ArrayList<Task<E>>()
        val canceledTasks = ArrayList<Task<E>>()
        for (task in tasks) {
            val assignedTask = if (assignedExecutor.containsKey(task) && assignedTime.containsKey(task)) {
                assignedTaskGenerator?.let { it(task, assignedTime[task]!!, assignedExecutor[task]!!) }
                    ?: generateAssignedTask(task, assignedTime[task]!!, assignedExecutor[task]!!)
            } else {
                null
            }

            if (assignedTask != null) {
                assignedTasks.add(assignedTask)
            } else {
                canceledTasks.add(task)
            }
        }

        return Ok(Solution(assignedTasks, canceledTasks))
    }

    private fun generateAssignedTask(
        task: Task<E>,
        time: TimeRange,
        executor: E
    ): Task<E>? {
        val assignedExecutor: E? = if (task.executor == null || task.executor != executor) {
            executor
        } else {
            null
        }

        val assignmentPolicy = AssignmentPolicy(
            executor = assignedExecutor,
            time = time
        )

        return if (assignmentPolicy.empty) {
            task
        } else if (!task.assigningEnabled(assignmentPolicy)) {
            null
        } else {
            task.assign(assignmentPolicy)
        }
    }
}
