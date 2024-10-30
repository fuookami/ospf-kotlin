package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.service

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.operator.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.model.*

data object BunchSolutionAnalyzer {
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
    ): Ret<BunchSolution<B, T, E, A>> {
        val assignedBunches = ArrayList<B>()
        val canceledTasks = ArrayList<T>()
        for (token in model.tokens.tokens) {
            for ((i, xi) in compilation.x.withIndex()) {
                if (UInt64(i.toULong()) > iteration) {
                    break
                }

                if (token.belongsTo(xi) && token.result?.let { abs(it - Flt64.one) leq Flt64(1e-5) } == true) {
                    val assignedBunch = bunches[i][token.variable.vectorView[0]]
                    assignedBunches.add(assignedBunch)
                }
            }

            if (token.belongsTo(compilation.y) && token.result?.let { abs(it - Flt64.one) leq Flt64(1e-5) } == true) {
                canceledTasks.add(tasks[token.variable.vectorView[0]])
            }
        }

        return Ok(BunchSolution(assignedBunches, canceledTasks))
    }
}
