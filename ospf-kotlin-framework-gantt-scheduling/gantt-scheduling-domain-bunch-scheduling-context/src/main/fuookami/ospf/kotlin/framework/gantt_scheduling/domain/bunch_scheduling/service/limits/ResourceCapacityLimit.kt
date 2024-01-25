package fuookami.ospf.kotlin.framework.gantt_scheduling.cg.service.limits

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.model.CGPipeline
import fuookami.ospf.kotlin.framework.model.ShadowPrice
import fuookami.ospf.kotlin.framework.model.ShadowPriceKey
import fuookami.ospf.kotlin.framework.gantt_scheduling.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.cg.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_scheduling.model.ResourceCapacity

typealias TimeSlot<E> = ResourceCapacity.TimeSlot<E>

private data class ResourceCapacityShadowPriceKey<E : Executor>(
    val timeSlot: TimeSlot<E>
) : ShadowPriceKey(ResourceCapacityShadowPriceKey::class) {
    override fun toString() = "Resource Capacity ($timeSlot)"
}

class ResourceCapacityLimit<E : Executor>(
    private val resourceCapacity: ResourceCapacity<E>,
    private val overResourceCapacityCostCalculator: fuookami.ospf.kotlin.utils.functional.Extractor<Flt64?, Pair<Resource<E>, TimeRange>>? = null,
    override val name: String = "resource_capacity_control"
) : CGPipeline<LinearMetaModel, ShadowPriceMap<E>> {
    private val timeSlots by resourceCapacity::timeSlots

    override fun invoke(model: LinearMetaModel): Try {
        if (timeSlots.isEmpty()) {
            return Ok(success)
        }

        val usageAmount = resourceCapacity.usageAmount
        val m = resourceCapacity.m

        for (timeSlot in timeSlots) {
            model.addConstraint(
                (usageAmount[timeSlot]!! - m[timeSlot]!!) leq timeSlot.capacity,
                "${name}_${timeSlot}"
            )
        }

        if (overResourceCapacityCostCalculator != null) {
            val cost = LinearPolynomial()
            for (timeSlot in timeSlots) {
                val penalty =
                    overResourceCapacityCostCalculator!!(Pair(timeSlot.resource, timeSlot.time)) ?: Flt64.infinity
                cost += penalty * m[timeSlot]!!
            }
            model.minimize(cost, "over resource capacity")
        }

        return Ok(success)
    }

    override fun extractor(): Extractor<ShadowPriceMap<E>> {
        return wrap { map, prevTask: Task<E>?, task: Task<E>?, _: Executor? ->
            var ret = Flt64.zero
            for (timeSlot in timeSlots) {
                if (timeSlot(prevTask, task)) {
                    ret += map[ResourceCapacityShadowPriceKey(timeSlot)]?.price ?: Flt64.zero
                }
            }
            ret
        }
    }

    override fun refresh(map: ShadowPriceMap<E>, model: LinearMetaModel, shadowPrices: List<Flt64>): Try {
        for ((i, j) in model.indicesOfConstraintGroup(name)!!.withIndex()) {
            map.put(
                ShadowPrice(
                    key = ResourceCapacityShadowPriceKey(timeSlots[i]),
                    price = shadowPrices[j]
                )
            )
        }

        return Ok(success)
    }
}
