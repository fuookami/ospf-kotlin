package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_scheduling.model

import kotlin.time.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_scheduling.model.*

class BunchSchedulingExecutionResourceUsage<R : ExecutionResource<C>, C : ResourceCapacity>(
    timeWindow: TimeWindow,
    resources: List<R>,
    duration: Duration,
    override val name: String
) : AbstractExecutionResourceUsage<R, C>(timeWindow, resources, duration) {
    override val overEnabled: Boolean = true
    override val lessEnabled: Boolean = true

    override lateinit var quantity: LinearExpressionSymbols1

    override fun register(model: LinearMetaModel): Try {
        if (timeSlots.isNotEmpty()) {
            if (!::quantity.isInitialized) {
                quantity = flatMap(
                    "${name}_quantity",
                    timeSlots,
                    { s -> LinearPolynomial(s.resource.initialQuantity) },
                    { (_, s) -> "$s" }
                )
                for (slot in timeSlots) {
                    quantity[slot].range.set(
                        ValueRange(
                            slot.resourceCapacity.quantity.lowerBound.toFlt64() - (slot.resourceCapacity.lessQuantity ?: Flt64.zero),
                            slot.resourceCapacity.quantity.upperBound.toFlt64() + (slot.resourceCapacity.overQuantity ?: Flt64.zero),
                            Flt64
                        )
                    )
                }
            }
            model.addSymbols(quantity)
        }

        return Ok(success)
    }

    fun <T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>> addColumns(
        iteration: UInt64,
        bunches: List<AbstractTaskBunch<T, E, A>>,
        compilation: BunchCompilation<T, E, A>
    ): Try {
        assert(bunches.isNotEmpty())

        val xi = compilation.x[iteration.toInt()]

        for (slot in timeSlots) {
            val thisBunches = bunches.filter { bunch ->
                bunch.tasks.any { slot.relatedTo(null, it) }
            }

            if (thisBunches.isNotEmpty()) {
                quantity[slot].flush()
                for (bunch in thisBunches) {
                    quantity[slot].asMutable() += slot.resource.usedQuantity(bunch, TimeRange(timeWindow.start, slot.time.end)) * xi[bunch]
                }
            }
        }

        return Ok(success)
    }
}

class BunchSchedulingConnectionResourceUsage<R : ConnectionResource<C>, C : ResourceCapacity>(
    timeWindow: TimeWindow,
    resources: List<R>,
    duration: Duration,
    override val name: String
) : AbstractConnectionResourceUsage<R, C>(timeWindow, resources, duration) {
    override val overEnabled: Boolean = true
    override val lessEnabled: Boolean = true

    override lateinit var quantity: LinearExpressionSymbols1

    override fun register(model: LinearMetaModel): Try {
        if (timeSlots.isNotEmpty()) {
            if (!::quantity.isInitialized) {
                quantity = flatMap(
                    "${name}_quantity",
                    timeSlots,
                    { s -> LinearPolynomial(s.resource.initialQuantity) },
                    { (_, s) -> "$s" }
                )
                for (slot in timeSlots) {
                    quantity[slot].range.set(
                        ValueRange(
                            slot.resourceCapacity.quantity.lowerBound.toFlt64() - (slot.resourceCapacity.lessQuantity ?: Flt64.zero),
                            slot.resourceCapacity.quantity.upperBound.toFlt64() + (slot.resourceCapacity.overQuantity ?: Flt64.zero),
                            Flt64
                        )
                    )
                }
            }
            model.addSymbols(quantity)
        }

        return Ok(success)
    }

    fun <T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>> addColumns(
        iteration: UInt64,
        bunches: List<AbstractTaskBunch<T, E, A>>,
        compilation: BunchCompilation<T, E, A>
    ): Try {
        assert(bunches.isNotEmpty())

        val xi = compilation.x[iteration.toInt()]

        for (slot in timeSlots) {
            val thisBunches = bunches.filter { bunch ->
                bunch.connections.any { slot.relatedTo(it.first, it.second) }
            }

            if (thisBunches.isNotEmpty()) {
                quantity[slot].flush()
                for (bunch in thisBunches) {
                    quantity[slot].asMutable() += slot.resource.usedQuantity(bunch, TimeRange(timeWindow.start, slot.time.end)) * xi[bunch]
                }
            }
        }

        return Ok(success)
    }
}

class BunchSchedulingStorageResourceUsage<R : StorageResource<C>, C: ResourceCapacity>(
    timeWindow: TimeWindow,
    resources: List<R>,
    duration: Duration,
    override val name: String
) : AbstractStorageResourceUsage<R, C>(timeWindow, resources, duration) {
    override val overEnabled: Boolean = true
    override val lessEnabled: Boolean = true

    override lateinit var quantity: LinearExpressionSymbols1

    override fun register(model: LinearMetaModel): Try {
        if (timeSlots.isNotEmpty()) {
            if (!::quantity.isInitialized) {
                quantity = flatMap(
                    "${name}_quantity",
                    timeSlots,
                    { s -> LinearPolynomial(s.resource.initialQuantity) },
                    { (_, s) -> "$s" }
                )
                for (slot in timeSlots) {
                    quantity[slot].range.set(
                        ValueRange(
                            slot.resourceCapacity.quantity.lowerBound.toFlt64() - (slot.resourceCapacity.lessQuantity
                                ?: Flt64.zero),
                            slot.resourceCapacity.quantity.upperBound.toFlt64() + (slot.resourceCapacity.overQuantity
                                ?: Flt64.zero),
                            Flt64
                        )
                    )
                }
            }
            model.addSymbols(quantity)
        }

        return super.register(model)
    }

    fun <T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>> addColumns(
        iteration: UInt64,
        bunches: List<AbstractTaskBunch<T, E, A>>,
        compilation: BunchCompilation<T, E, A>
    ): Try {
        assert(bunches.isNotEmpty())

        val xi = compilation.x[iteration.toInt()]

        for (slot in timeSlots) {
            val thisBunches = bunches.filter { bunch ->
                bunch.tasks.any { slot.relatedTo(null, it) }
            }

            if (thisBunches.isNotEmpty()) {
                quantity[slot].flush()
                for (bunch in thisBunches) {
                    quantity[slot].asMutable() += slot.resource.usedQuantity(bunch, TimeRange(timeWindow.start, slot.time.end)) * xi[bunch]
                }
            }
        }

        return Ok(success)
    }
}
