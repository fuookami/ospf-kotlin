package fuookami.ospf.kotlin.framework.gantt_scheduling.mip.service.limits

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.functional.Extractor
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.mip.model.*

class ExecutorLeisureMinimizeLimit<E : Executor>(
    private val executors: List<E>,
    private val compilation: Compilation<E>,
    private val executorLeisureCostCalculator: Extractor<Flt64?, E>? = null,
    override val name: String = "executor_leisure_limit"
) : HAPipeline<LinearMetaModel> {
    override operator fun invoke(model: LinearMetaModel): Try {
        val z = compilation.z
        val executorCompilation = compilation.executorCompilation

        if (compilation.withExecutorLeisure && executorLeisureCostCalculator != null) {
            for (executor in executors) {
                model.addConstraint(
                    executorCompilation[executor]!! eq Flt64.one,
                    "${name}_${executor}"
                )
            }

            val cost = LinearPolynomial()
            for (executor in executors) {
                val penalty = executorLeisureCostCalculator!!(executor) ?: Flt64.infinity
                cost += penalty * z[executor]!!
            }
            model.minimize(cost, "executor leisure")
        }

        return Ok(success)
    }

    override fun calculate(model: LinearMetaModel, solution: List<Flt64>): Ret<Flt64?> {
        // todo: impl
        return Ok(Flt64.zero)
    }
}
