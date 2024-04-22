package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.model

import kotlin.time.*
import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model.*

class BunchSchedulingExecutionResourceUsage<
    out R : ExecutionResource<C>,
    out C : ResourceCapacity
>(
    timeWindow: TimeWindow,
    resources: List<R>,
    interval: Duration = timeWindow.interval,
    override val name: String
) : AbstractExecutionResourceUsage<R, C>(timeWindow, resources, interval) {
    override val overEnabled: Boolean = true
    override val lessEnabled: Boolean = true

    override lateinit var quantity: LinearExpressionSymbols1

    override fun register(model: MetaModel): Try {
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
                                ?: Flt64.zero)
                        )
                    )
                }
            }
            when (val result = model.add(quantity)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }

        return ok
    }

    fun <
        B : AbstractTaskBunch<T, E, A>,
        T : AbstractTask<E, A>,
        E : Executor,
        A : AssignmentPolicy<E>
    > addColumns(
        iteration: UInt64,
        bunches: List<B>,
        compilation: BunchCompilation<B, T, E, A>
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
                    quantity[slot].asMutable() += slot.resource.usedQuantity(
                        bunch,
                        TimeRange(timeWindow.start, slot.time.end)
                    ) * xi[bunch]
                }
            }
        }

        return ok
    }
}

class BunchSchedulingConnectionResourceUsage<
    out R : ConnectionResource<C>,
    out C : ResourceCapacity
>(
    timeWindow: TimeWindow,
    resources: List<R>,
    interval: Duration = timeWindow.interval,
    override val name: String
) : AbstractConnectionResourceUsage<R, C>(timeWindow, resources, interval) {
    override val overEnabled: Boolean = true
    override val lessEnabled: Boolean = true

    override lateinit var quantity: LinearExpressionSymbols1

    override fun register(model: MetaModel): Try {
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
                                ?: Flt64.zero)
                        )
                    )
                }
            }
            when (val result = model.add(quantity)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }

        return ok
    }

    fun <
        B : AbstractTaskBunch<T, E, A>,
        T : AbstractTask<E, A>,
        E : Executor,
        A : AssignmentPolicy<E>
    > addColumns(
        iteration: UInt64,
        bunches: List<B>,
        compilation: BunchCompilation<B, T, E, A>
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
                    quantity[slot].asMutable() += slot.resource.usedQuantity(
                        bunch,
                        TimeRange(timeWindow.start, slot.time.end)
                    ) * xi[bunch]
                }
            }
        }

        return ok
    }
}

class BunchSchedulingStorageResourceUsage<
    out R : StorageResource<C>,
    out C : ResourceCapacity
>(
    timeWindow: TimeWindow,
    resources: List<R>,
    interval: Duration = timeWindow.interval,
    override val name: String
) : AbstractStorageResourceUsage<R, C>(timeWindow, resources, interval) {
    override val overEnabled: Boolean = true
    override val lessEnabled: Boolean = true

    override lateinit var quantity: LinearExpressionSymbols1

    override fun register(model: MetaModel): Try {
        if (timeSlots.isNotEmpty()) {
            if (!::quantity.isInitialized) {
                quantity = flatMap(
                    "${name}_quantity",
                    timeSlots,
                    { s ->
                        val time = TimeRange(timeWindow.start, s.time.end)
                        val fixedSupply = s.resource.fixedSupplyIn(time)
                        val fixedCost = s.resource.fixedCostIn(time)
                        LinearPolynomial(s.resource.initialQuantity + fixedSupply - fixedCost)
                    },
                    { (_, s) -> "$s" }
                )
                for (slot in timeSlots) {
                    quantity[slot].range.set(
                        ValueRange(
                            slot.resourceCapacity.quantity.lowerBound.toFlt64() -
                                    (slot.resourceCapacity.lessQuantity ?: Flt64.zero),
                            slot.resourceCapacity.quantity.upperBound.toFlt64() +
                                    (slot.resourceCapacity.overQuantity ?: Flt64.zero)
                        )
                    )
                }
            }
            when (val result = model.add(quantity)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }

        return super.register(model)
    }

    suspend fun <
        B : AbstractTaskBunch<T, E, A>,
        T : AbstractTask<E, A>,
        E : Executor,
        A : AssignmentPolicy<E>
    > addColumns(
        iteration: UInt64,
        bunches: List<B>,
        compilation: BunchCompilation<B, T, E, A>
    ): Try {
        assert(bunches.isNotEmpty())

        val xi = compilation.x[iteration.toInt()]

        coroutineScope {
            for (slot in timeSlots) {
                launch(Dispatchers.Default) {
                    val thisBunches = bunches.mapNotNull { bunch ->
                        val usedQuantity = slot.resource.usedQuantity(
                            bunch,
                            TimeRange(timeWindow.start, slot.time.end)
                        )
                        if (usedQuantity != Flt64.zero) {
                            Pair(bunch, usedQuantity)
                        } else {
                            null
                        }
                    }

                    if (thisBunches.isNotEmpty()) {
                        quantity[slot].flush()
                        for (bunch in thisBunches) {
                            quantity[slot].asMutable() += bunch.second * xi[bunch.first]
                        }
                    }
                }
            }
        }

        return ok
    }
}
