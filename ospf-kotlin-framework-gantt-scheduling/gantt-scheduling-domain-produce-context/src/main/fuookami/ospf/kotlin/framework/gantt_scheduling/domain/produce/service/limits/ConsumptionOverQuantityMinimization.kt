package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce.service.limits

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce.model.*

class ConsumptionOverQuantityMinimization<
    Args : AbstractGanttSchedulingShadowPriceArguments<E, A>,
    E : Executor,
    A : AssignmentPolicy<E>,
    C : AbstractMaterial
>(
    products: List<Pair<C, MaterialReserves?>>,
    private val consumption: Consumption,
    private val threshold: (C) -> Flt64 = { Flt64.zero },
    private val coefficient: (C) -> Flt64 = { Flt64.one },
    override val name: String = "consumption_less_quantity_minimization"
) : AbstractGanttSchedulingCGPipeline<Args, E, A> {
    private val materials = if (consumption.overEnabled) {
        products.filter { it.second?.overEnabled == true }
    } else {
        emptyList()
    }

    override fun invoke(model: AbstractLinearMetaModel): Try {
        if (materials.isNotEmpty()) {
            val cost = MutableLinearPolynomial()
            for ((material, _) in materials) {
                val thresholdValue = threshold(material)
                if (thresholdValue eq Flt64.zero) {
                    cost += coefficient(material) * consumption.overQuantity[material]
                } else {
                    val slack = SlackFunction(
                        UContinuous,
                        x = LinearPolynomial(consumption.overQuantity[material]),
                        threshold = LinearPolynomial(thresholdValue),
                        name = "consumption_over_quantity_minimization_threshold_$material"
                    )
                    when (val result = model.add(slack)) {
                        is Ok -> {}

                        is Failed -> {
                            return Failed(result.error)
                        }
                    }
                    cost += coefficient(material) * slack
                }
            }
            when (val result = model.minimize(
                cost,
                "consumption over quantity"
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }
        return ok
    }
}
