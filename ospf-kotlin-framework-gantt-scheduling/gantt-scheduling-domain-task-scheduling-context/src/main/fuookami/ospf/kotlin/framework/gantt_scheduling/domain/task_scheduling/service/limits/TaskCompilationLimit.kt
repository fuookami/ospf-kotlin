package fuookami.ospf.kotlin.framework.gantt_scheduling.mip.service.limits

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.mip.model.*

class TaskCompilationLimit<E : Executor>(
    private val tasks: List<Task<E>>,
    private val compilation: Compilation<E>,
    private val cancelCostCalculator: CostCalculator<E>? = null,
    override val name: String = "task_compilation_limit"
) : HAPipeline<LinearMetaModel> {
    override operator fun invoke(model: LinearMetaModel): Try {
        val y = compilation.y
        val taskCompilation = compilation.taskCompilation

        for (task in tasks) {
            model.addConstraint(
                taskCompilation[task]!! eq Flt64.one,
                "${name}_${task}"
            )
        }

        if (this.compilation.taskCancelEnabled && cancelCostCalculator != null) {
            val cost = LinearPolynomial()
            for (task in tasks.filter { it.cancelEnabled }) {
                val penalty = cancelCostCalculator!!(task) ?: Flt64.infinity
                cost += penalty * y[task]!!
            }
            model.minimize(cost, "task cancel cost")
        }

        return Ok(success)
    }

    override fun calculate(model: LinearMetaModel, solution: List<Flt64>): Ret<Flt64?> {
        // todo: impl
        return Ok(Flt64.zero)
    }
}
