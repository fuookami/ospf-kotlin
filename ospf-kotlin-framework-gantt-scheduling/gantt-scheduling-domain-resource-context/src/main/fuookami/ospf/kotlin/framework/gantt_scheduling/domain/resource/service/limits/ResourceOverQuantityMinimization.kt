@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.resource.service.limits

import fuookami.ospf.kotlin.core.frontend.expression.monomial.times
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.MutableLinearPolynomial
import fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function.SlackFunction
import fuookami.ospf.kotlin.core.frontend.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.frontend.variable.UContinuous
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.resource.model.AbstractResourceCapacity
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.resource.model.Resource
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.resource.model.ResourceTimeSlot
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.resource.model.ResourceUsage
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AbstractGanttSchedulingCGPipeline
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AbstractGanttSchedulingShadowPriceArguments
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AssignmentPolicy
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.Executor
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.math.Flt64

class ResourceOverQuantityMinimization<
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
    override val name: String = "resource_over_capacity_minimization"
) : AbstractGanttSchedulingCGPipeline<Args, E, A> {
    private val slots = if (quantity.overEnabled) {
        quantity.timeSlots.filter { it.resourceCapacity.overEnabled }
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
                    cost += thisCoefficient * quantity.overQuantity[slot]
                } else {
                    val slack = SlackFunction(
                        x = quantity.overQuantity[slot],
                        threshold = thresholdValue,
                        type = UContinuous,
                        name = "${quantity.name}_${slot}_${name}_over_quantity_threshold"
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
                    cost += thisCoefficient * slack
                }
            }
            when (val result = model.minimize(
                polynomial = cost,
                name = "${quantity.name} over quantity"
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
