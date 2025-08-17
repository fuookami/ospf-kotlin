package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.service.limits

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model.*

class ResourceLessQuantityMinimization<
    Args : AbstractGanttSchedulingShadowPriceArguments<E, A>,
    E : Executor,
    A : AssignmentPolicy<E>,
    S : ResourceTimeSlot<R, C>,
    R : Resource<C>,
    C : AbstractResourceCapacity
>(
    private val quantity: ResourceUsage<S, R, C>,
    private val threshold: (S) -> Flt64 = { Flt64.zero },
    private val coefficient: (S) -> Flt64 = { Flt64.one },
    override val name: String = "resource_less_capacity_minimization"
) : AbstractGanttSchedulingCGPipeline<Args, E, A> {
    private val slots = if (quantity.lessEnabled) {
        quantity.timeSlots.filter { it.resourceCapacity.lessEnabled }
    } else {
        emptyList()
    }

    override fun invoke(model: AbstractLinearMetaModel): Try {
        if (slots.isNotEmpty()) {
            val cost = MutableLinearPolynomial()
            for (slot in slots) {
                val thresholdValue = threshold(slot)
                val thisCoefficient = coefficient(slot)
                if (thresholdValue eq Flt64.zero) {
                    cost += thisCoefficient * quantity.lessQuantity[slot]
                } else {
                    val slack = SlackFunction(
                        x = quantity.lessQuantity[slot],
                        threshold = thresholdValue,
                        type = UContinuous,
                        name = "${quantity.name}_${slot}_${name}_over_quantity_threshold"
                    )
                    when (val result = model.add(slack)) {
                        is Ok -> {}

                        is Failed -> {
                            return Failed(result.error)
                        }
                    }
                    cost += thisCoefficient * slack
                }
            }
            when (val result = model.minimize(cost, "${quantity.name} less quantity")) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }

        return ok
    }
}
