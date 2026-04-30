package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce.service.limits

import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.intermediate_symbol.function.SlackFunction
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModelFlt64
import fuookami.ospf.kotlin.core.variable.UContinuous
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce.model.AbstractMaterial
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce.model.Consumption
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce.model.MaterialReserves
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AbstractGanttSchedulingCGPipeline
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AbstractGanttSchedulingShadowPriceArguments
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AssignmentPolicy
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.Executor
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64

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
    override val name: String = "consumption_over_quantity_minimization"
) : AbstractGanttSchedulingCGPipeline<Args, E, A> {
    private val materials = if (consumption.overEnabled) {
        products.filter { it.second?.overEnabled == true }
    } else {
        emptyList()
    }

    override fun invoke(model: AbstractLinearMetaModelFlt64): Try {
        if (materials.isNotEmpty()) {
            val cost = MutableLinearPolynomial<Flt64>(emptyList(), Flt64.zero)
            for ((material, _) in materials) {
                val thresholdValue = threshold(material)
                if (thresholdValue eq Flt64.zero) {
                    cost += LinearMonomial(coefficient(material), consumption.overQuantity[material])
                } else {
                    val slack = SlackFunction(
                        x = consumption.overQuantity[material],
                        threshold = thresholdValue,
                        type = UContinuous,
                        name = "consumption_over_quantity_minimization_threshold_$material"
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
                    cost += LinearMonomial(coefficient(material), slack)
                }
            }
            when (val result = model.minimize(
                polynomial = cost.toLinearPolynomial(),
                name = "consumption over quantity"
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