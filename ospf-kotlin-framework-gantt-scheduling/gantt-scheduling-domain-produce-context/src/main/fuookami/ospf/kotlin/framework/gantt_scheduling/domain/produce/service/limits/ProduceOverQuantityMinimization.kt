@file:Suppress("DEPRECATION")

package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce.service.limits

import fuookami.ospf.kotlin.core.intermediate_model.times
import fuookami.ospf.kotlin.core.intermediate_model.MutableLinearPolynomial
import fuookami.ospf.kotlin.core.intermediate_symbol.function.SlackFunction
import fuookami.ospf.kotlin.core.intermediate_model.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.variable.UContinuous
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce.model.AbstractMaterial
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce.model.MaterialDemand
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce.model.Produce
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AbstractGanttSchedulingCGPipeline
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AbstractGanttSchedulingShadowPriceArguments
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AssignmentPolicy
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.Executor
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64

class ProduceOverQuantityMinimization<
        Args : AbstractGanttSchedulingShadowPriceArguments<E, A>,
        E : Executor,
        A : AssignmentPolicy<E>,
        P : AbstractMaterial
        >(
    products: List<Pair<P, MaterialDemand?>>,
    private val produce: Produce,
    private val threshold: (P) -> Flt64 = { Flt64.zero },
    private val coefficient: (P) -> Flt64 = { Flt64.one },
    override val name: String = "produce_over_quantity_minimization"
) : AbstractGanttSchedulingCGPipeline<Args, E, A> {
    private val products = if (produce.overEnabled) {
        products.filter { it.second?.overEnabled == true }
    } else {
        emptyList()
    }

    override fun invoke(model: AbstractLinearMetaModel): Try {
        if (products.isNotEmpty()) {
            val cost = MutableLinearPolynomial()
            for ((product, _) in products) {
                val thresholdValue = threshold(product)
                if (thresholdValue eq Flt64.zero) {
                    cost += coefficient(product) * produce.overQuantity[product]
                } else {
                    val slack = SlackFunction(
                        x = produce.overQuantity[product],
                        threshold = thresholdValue,
                        type = UContinuous,
                        name = "produce_over_quantity_minimization_threshold_${product}"
                    )
                    when (val result = model.add(slack)) {
                        is Ok -> {}

                        is Failed -> {
                            return Failed(result.error)
                        }

                        is Fatal -> {
                            return Fatal(result.errors)
                        }
                    }
                    cost += coefficient(product) * slack
                }
            }
            when (val result = model.minimize(
                polynomial = cost,
                name = "produce over quantity"
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
            }
        }
        return ok
    }
}



