package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.service

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model.Solution

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
        model: AbstractLinearMetaModel
    ): Ret<Solution<T, E, A>> {
        val assignedTasks = ArrayList<T>()
        val canceledTask = ArrayList<T>()
        for (token in model.tokens.tokens) {
            for ((i, xi) in compilation.x.withIndex()) {
                if (UInt64(i.toULong()) >= iteration) {
                    break
                }

                if (token.belongsTo(xi) && token.result?.let { (it - Flt64.one) leq Flt64(1e-5) } == true) {
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

            if (token.belongsTo(compilation.y) && token.result?.let { (it - Flt64.one) leq Flt64(1e-5) } == true) {
                canceledTask.add(tasks[token.variable.vectorView[0]])
            }
        }

        return Ok(Solution(assignedTasks, canceledTask))
    }
}
