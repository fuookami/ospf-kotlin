package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_scheduling.service

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_scheduling.model.*

class TaskSolutionAnalyzer<T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>> {
    operator fun invoke(
        iteration: UInt64,
        tasks: List<T>,
        bunches: List<List<AbstractTaskBunch<T, E, A>>>,
        compilation: BunchCompilation<T, E, A>,
        model: LinearMetaModel
    ): Ret<Solution<T, E, A>> {
        val assignedTasks = ArrayList<T>()
        val canceledTask = ArrayList<T>()
        for (token in model.tokens.tokens) {
            for ((i, xi) in compilation.x.withIndex()) {
                if (UInt64(i.toULong()) >= iteration) {
                    break
                }

                if (token.belongsTo(xi) && token.result?.let { it eq Flt64.one } == true) {
                    val assignedBunch = bunches[i][token.variable.vectorView[0]]
                    for (task in assignedBunch.tasks) {
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

            if (token.belongsTo(compilation.y) && token.result?.let { it eq Flt64.one } == true) {
                canceledTask.add(tasks[token.variable.vectorView[0]])
            }
        }

        return Ok(Solution(assignedTasks, canceledTask))
    }
}
