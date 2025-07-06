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

class ConsumptionQuantityMinimization<
    Args : AbstractGanttSchedulingShadowPriceArguments<E, A>,
    E : Executor,
    A : AssignmentPolicy<E>,
    C : AbstractMaterial
>(
    private val materials: List<C>,
    private val consumption: Consumption,
    private val threshold: (C) -> Flt64 = { Flt64.zero },
    private val coefficient: (C) -> Flt64 = { Flt64.one },
    override val name: String = "consumption_quantity_minimization"
) : AbstractGanttSchedulingCGPipeline<Args, E, A> {
    override fun invoke(model: AbstractLinearMetaModel): Try {
        when (val result = model.minimize(
            sum(materials.map {
                val thresholdValue = threshold(it)
                if (thresholdValue eq Flt64.zero) {
                    coefficient(it) * consumption.quantity[it]
                } else {
                    val slack = SlackFunction(
                        UContinuous,
                        x = LinearPolynomial(consumption.quantity[it]),
                        threshold = LinearPolynomial(thresholdValue),
                        name = "consumption_quantity_minimization_threshold_$it"
                    )
                    when (val result = model.add(slack)) {
                        is Ok -> {}

                        is Failed -> {
                            return Failed(result.error)
                        }
                    }
                    coefficient(it) * slack
                }
            }),
            "consumption quantity"
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }
        return ok
    }
}
