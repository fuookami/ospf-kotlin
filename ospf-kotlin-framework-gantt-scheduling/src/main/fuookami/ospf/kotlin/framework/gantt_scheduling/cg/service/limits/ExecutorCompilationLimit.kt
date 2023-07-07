package fuookami.ospf.kotlin.framework.gantt_scheduling.cg.service.limits

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.model.CGPipeline
import fuookami.ospf.kotlin.framework.model.Extractor
import fuookami.ospf.kotlin.framework.model.ShadowPrice
import fuookami.ospf.kotlin.framework.model.ShadowPriceKey
import fuookami.ospf.kotlin.framework.gantt_scheduling.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.cg.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.cg.model.ShadowPriceMap

private data class ExecutorCompilationShadowPriceKey<E : Executor>(
    val executor: E
) : ShadowPriceKey(ExecutorCompilationShadowPriceKey::class) {
    override fun toString() = "Executor Compilation (${executor})"
}

class ExecutorCompilationLimit<E : Executor>(
    private val executors: List<E>,
    private val compilation: Compilation<E>,
    private val executorLeisureCostCalculator: fuookami.ospf.kotlin.utils.functional.Extractor<Flt64?, E>? = null,
    override val name: String = "executor_compilation"
) : CGPipeline<LinearMetaModel, ShadowPriceMap<E>> {
    override fun invoke(model: LinearMetaModel): Try<Error> {
        val executorCompilation = compilation.executorCompilation
        val z = compilation.z

        for (executor in executors) {
            model.addConstraint(
                executorCompilation[executor]!! eq Flt64.one,
                "${name}_${executor}"
            )
        }

        if (executorLeisureCostCalculator != null) {
            val cost = LinearPolynomial()
            for (executor in executors) {
                val penalty = executorLeisureCostCalculator!!(executor) ?: Flt64.infinity
                cost += penalty * z[executor]!!
            }
            model.minimize(cost, "executor leisure")
        }

        return Ok(success)
    }

    override fun extractor(): Extractor<ShadowPriceMap<E>> {
        return wrap { map, prevTask: Task<E>?, task: Task<E>?, executor: E? ->
            if (executor != null && prevTask == null && task == null) {
                map[ExecutorCompilationShadowPriceKey(executor)]?.price ?: Flt64.zero
            } else {
                Flt64.zero
            }
        }
    }

    override fun refresh(map: ShadowPriceMap<E>, model: LinearMetaModel, shadowPrices: List<Flt64>): Try<Error> {
        for ((i, j) in model.indicesOfConstraintGroup(name)!!.withIndex()) {
            map.put(
                ShadowPrice(
                    key = ExecutorCompilationShadowPriceKey(executors[i]),
                    price = shadowPrices[j]
                )
            )
        }

        return Ok(success)
    }
}
