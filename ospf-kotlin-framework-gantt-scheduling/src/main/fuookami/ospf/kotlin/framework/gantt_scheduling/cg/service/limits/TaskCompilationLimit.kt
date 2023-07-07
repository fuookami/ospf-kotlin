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

private data class TaskCompilationShadowPriceKey<E : Executor>(
    val task: Task<E>
) : ShadowPriceKey(TaskCompilationShadowPriceKey::class) {
    override fun toString() = "Task Compilation (${task})"
}

class TaskCompilationLimit<E : Executor>(
    private val tasks: List<Task<E>>,
    private val compilation: Compilation<E>,
    private val cancelCostCalculator: CostCalculator<E>? = null,
    override val name: String = "flight_task_compilation"
) : CGPipeline<LinearMetaModel, ShadowPriceMap<E>> {
    override fun invoke(model: LinearMetaModel): Try<Error> {
        val taskCompilation = compilation.taskCompilation
        val y = this.compilation.y

        for (task in tasks) {
            model.addConstraint(
                taskCompilation[task]!! eq Flt64.one,
                "${name}_${task}"
            )
        }

        model.minimize(LinearPolynomial(compilation.bunchCost))

        if (cancelCostCalculator != null) {
            val cancelCostPoly = LinearPolynomial()
            for (task in tasks) {
                val penalty = cancelCostCalculator!!(task) ?: Flt64.infinity
                cancelCostPoly += penalty * y[task]!!
            }
            model.minimize(cancelCostPoly, "flight task cancel")
        }

        return Ok(success)
    }

    override fun extractor(): Extractor<ShadowPriceMap<E>> {
        return wrap { map, _: Task<E>?, task: Task<E>?, _: E? ->
            if (task != null) {
                map[TaskCompilationShadowPriceKey(task.originTask)]?.price ?: Flt64.zero
            } else {
                Flt64.zero
            }
        }
    }

    override fun refresh(map: ShadowPriceMap<E>, model: LinearMetaModel, shadowPrices: List<Flt64>): Try<Error> {
        for ((i, j) in model.indicesOfConstraintGroup(name)!!.withIndex()) {
            map.put(
                ShadowPrice(
                    key = TaskCompilationShadowPriceKey(tasks[i].originTask),
                    price = shadowPrices[j]
                )
            )
        }

        return Ok(success)
    }
}
