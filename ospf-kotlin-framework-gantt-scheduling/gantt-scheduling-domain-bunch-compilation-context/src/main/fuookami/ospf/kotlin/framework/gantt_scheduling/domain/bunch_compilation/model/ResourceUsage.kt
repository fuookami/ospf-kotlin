package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.model

import kotlin.time.*
import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.value_range.*
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
                            slot.resourceCapacity.quantity.lowerBound.value.unwrap() - (slot.resourceCapacity.lessQuantity
                                ?: Flt64.zero),
                            slot.resourceCapacity.quantity.upperBound.value.unwrap() + (slot.resourceCapacity.overQuantity
                                ?: Flt64.zero)
                        ).value!!
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
                            slot.resourceCapacity.quantity.lowerBound.value.unwrap() - (slot.resourceCapacity.lessQuantity
                                ?: Flt64.zero),
                            slot.resourceCapacity.quantity.upperBound.value.unwrap() + (slot.resourceCapacity.overQuantity
                                ?: Flt64.zero)
                        ).value!!
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
    out E : Executor,
    out R : StorageResource<C>,
    out C : ResourceCapacity
>(
    timeWindow: TimeWindow,
    executors: List<E>,
    resources: List<R>,
    interval: Duration = timeWindow.interval,
    override val name: String
) : AbstractStorageResourceUsage<E, R, C>(timeWindow, executors, resources, interval) {
    override val overEnabled: Boolean = true
    override val lessEnabled: Boolean = true

    override lateinit var executorSupply: LinearExpressionSymbols3
    override lateinit var cost: LinearExpressionSymbols2

    override fun register(model: MetaModel): Try {
        if (!::executorSupply.isInitialized) {
            executorSupply = flatMap(
                "${name}_executor_supply",
                executors,
                resources,
                timeWindow.timeSlots,
                { _, _, _ -> LinearPolynomial() },
                { (_, e), (_, r), (_, t) -> "${e}_${r}_${t}" }
            )
        }
        when (val result = model.add(executorSupply)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        if (!::cost.isInitialized) {
            cost = flatMap(
                "${name}_cost",
                resources,
                timeWindow.timeSlots,
                { r, t ->
                    val time = TimeRange(timeWindow.start, t.end)
                    val fixedCost = r.fixedCostIn(time)
                    LinearPolynomial(fixedCost)
                },
                { (_, r), (_, t) -> "${r}_${t}" }
            )
        }
        when (val result = model.add(cost)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
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
            for ((e, executor) in executors.withIndex()) {
                for ((r, resource) in resources.withIndex()) {
                    for ((t, timeSlot) in timeWindow.timeSlots.withIndex()) {
                        launch(Dispatchers.Default) {
                            val thisBunches = bunches.mapNotNull { bunch ->
                                if (bunch.executor != executor) {
                                    return@mapNotNull null
                                }
                                val supplyQuantity = resource.supplyBy(
                                    bunch,
                                    TimeRange(timeWindow.start, timeSlot.end)
                                )
                                if (supplyQuantity != Flt64.zero) {
                                    Pair(bunch, supplyQuantity)
                                } else {
                                    null
                                }
                            }

                            if (thisBunches.isNotEmpty()) {
                                executorSupply[e, r, t].flush()
                                for ((bunch, supplyQuantity) in thisBunches) {
                                    executorSupply[e, r, t].asMutable() += xi[bunch] * supplyQuantity
                                }
                            }
                        }
                    }
                }
            }
            for ((r, resource) in resources.withIndex()) {
                for ((t, timeSlot) in timeWindow.timeSlots.withIndex()) {
                    launch(Dispatchers.Default) {
                        val thisBunches = bunches.mapNotNull { bunch ->
                            val costQuantity = resource.costBy(
                                bunch,
                                TimeRange(timeWindow.start, timeSlot.end)
                            )
                            if (costQuantity != Flt64.zero) {
                                Pair(bunch, costQuantity)
                            } else {
                                null
                            }
                        }

                        if (thisBunches.isNotEmpty()) {
                            cost[r, t].flush()
                            for ((bunch, costQuantity) in thisBunches) {
                                cost[r, t].asMutable() += xi[bunch] * costQuantity
                            }
                        }
                    }
                }
            }
        }

        return ok
    }
}
