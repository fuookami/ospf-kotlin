package fuookami.ospf.kotlin.framework.gantt_scheduling.mip.service.limits

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.functional.Extractor
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.mip.model.*

class ExecutorCostMinimizeLimit<E : Executor>(
    private val tasks: List<Task<E>>,
    private val executors: List<E>,
    private val compilation: Compilation<E>,
    private val executorCostCalculator: Extractor<Flt64?, Pair<Task<E>, E>>? = null,
    override val name: String = "executor_cost_limit"
) : HAPipeline<LinearMetaModel> {
    override operator fun invoke(model: LinearMetaModel): Try {
        val x = compilation.x

        if (executorCostCalculator != null) {
            val cost = LinearPolynomial()
            for (task in tasks) {
                for (executor in executors) {
                    val penalty = executorCostCalculator!!(Pair(task, executor)) ?: Flt64.infinity
                    cost += penalty * x[task, executor]!!
                }
            }
            model.minimize(cost, "executor")
        }

        return Ok(success)
    }

    override fun calculate(model: LinearMetaModel, solution: List<Flt64>): Ret<Flt64?> {
        // todo: impl
        return Ok(Flt64.zero)
    }
}
