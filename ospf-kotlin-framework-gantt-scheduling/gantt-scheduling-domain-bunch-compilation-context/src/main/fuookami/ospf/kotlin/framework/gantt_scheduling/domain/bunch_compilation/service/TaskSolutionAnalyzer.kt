package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.service

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.model.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.model.*

data object TaskSolutionAnalyzer {
    operator fun <
        B : AbstractTaskBunch<T, E, A>,
        T : AbstractTask<E, A>,
        E : Executor,
        A : AssignmentPolicy<E>
    > invoke(
        iteration: UInt64,
        tasks: List<T>,
        bunches: List<List<B>>,
        compilation: BunchCompilation<B, T, E, A>,
        model: AbstractLinearMetaModel,
        solution: Solution? = null
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
                if (result eq UInt64.one) {
                    val bunch = bunches[i].findOrGet(x.index)
                    for (task in bunch.tasks) {
                        when (val policy = task.assignmentPolicy) {
                            null -> {}

                            else -> {
                                if (!policy.empty) {
                                    assignedTasks.add(task)
                                }
                            }
                        }
                    }
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
                canceledTasks.add(tasks.findOrGet(y.index))
            }
        }

        return Ok(TaskSolution(assignedTasks, canceledTasks))
    }
}
