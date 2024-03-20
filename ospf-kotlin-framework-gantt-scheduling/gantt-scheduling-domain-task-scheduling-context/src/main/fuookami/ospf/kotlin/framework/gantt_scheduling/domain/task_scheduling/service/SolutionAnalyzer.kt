package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_scheduling.service

import kotlinx.datetime.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_scheduling.model.*

private typealias AssignedPolicyGenerator<A, E> = (time: TimeRange?, executor: E?) -> A?

class SolutionAnalyzer<E : Executor, A : AssignmentPolicy<E>> {
    operator fun invoke(
        timeWindow: TimeWindow,
        tasks: List<AbstractTask<E, A>>,
        executors: List<E>,
        compilation: TaskCompilation<E, A>,
        taskTime: TaskSchedulingTaskTime<E, A>,
        results: List<Flt64>,
        model: LinearMetaModel,
        assignedPolicyGenerator: AssignedPolicyGenerator<A, E>
    ): Ret<Solution<E, A>> {
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

        val assignedTasks = ArrayList<AbstractTask<E, A>>()
        val canceledTasks = ArrayList<AbstractTask<E, A>>()
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
                assignedTasks.add(assignedTask)
            } else {
                canceledTasks.add(task)
            }
        }

        return Ok(Solution(assignedTasks, canceledTasks))
    }
}
