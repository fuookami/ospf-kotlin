package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_scheduling.service

import kotlinx.datetime.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_scheduling.model.*

private typealias AssignedPolicyGenerator<A, E> = (time: TimeRange?, executor: E?) -> A?

class SolutionAnalyzer<T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>> {
    @Suppress("UNCHECKED_CAST")
    operator fun invoke(
        timeWindow: TimeWindow,
        tasks: List<T>,
        executors: List<E>,
        compilation: TaskCompilation<T, E, A>,
        taskTime: TaskSchedulingTaskTime<T, E, A>,
        results: List<Flt64>,
        model: LinearMetaModel,
        assignedPolicyGenerator: AssignedPolicyGenerator<A, E>
    ): Ret<Solution<T, E, A>> {
        val assignedExecutor = HashMap<T, E>()
        for (token in model.tokens.tokens) {
            if (token.belongsTo(compilation.x) && token.result?.let { it eq Flt64.one } == true) {
                val task = token.variable.vectorView[0]
                val executor = token.variable.vectorView[1]
                assignedExecutor[tasks.find { it.index == task }!!] = executors.find { it.index == executor }!!
            }
        }

        val assignedEST = HashMap<T, Instant>()
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
            val assignedTask = assignedPolicyGenerator(assignedTime[task], assignedExecutor[task])
                ?.let { task.assign(it) as T? }
            if (assignedTask != null) {
                assignedTasks.add(assignedTask)
            } else {
                canceledTasks.add(task)
            }
        }

        return Ok(Solution(assignedTasks, canceledTasks))
    }
}
