@file:Suppress("DEPRECATION")

@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce.model

import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.intermediate_symbol.LinearExpressionSymbols1
import fuookami.ospf.kotlin.core.intermediate_symbol.LinearExpressionSymbol
import fuookami.ospf.kotlin.core.intermediate_symbol.LinearIntermediateSymbol
import fuookami.ospf.kotlin.core.intermediate_symbol.LinearIntermediateSymbols1
import fuookami.ospf.kotlin.core.intermediate_symbol.function.SlackFunction
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModelF64
import fuookami.ospf.kotlin.core.model.mechanism.MetaDualSolution
import fuookami.ospf.kotlin.core.model.mechanism.MetaModelF64
import fuookami.ospf.kotlin.core.model.mechanism.leq
import fuookami.ospf.kotlin.core.model.mechanism.geq
import fuookami.ospf.kotlin.core.variable.UContinuous
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.model.BunchCompilation
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AbstractTask
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AbstractTaskBunch
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AssignmentPolicy
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.Executor
import fuookami.ospf.kotlin.framework.model.AbstractShadowPriceMap
import fuookami.ospf.kotlin.framework.model.refresh
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.multiarray.Shape1

interface Consumption {
    val quantity: LinearIntermediateSymbols1
    val overQuantity: LinearIntermediateSymbols1
    val lessQuantity: LinearIntermediateSymbols1

    val overEnabled: Boolean
    val lessEnabled: Boolean

    fun register(model: AbstractLinearMetaModelF64): Try
}

abstract class AbstractConsumption<
        out T : ProductionTask<E, A, P, C>,
        out E : Executor,
        out A : AssignmentPolicy<E>,
        P : AbstractMaterial,
        C : AbstractMaterial
        >(
    val materials: List<Pair<C, MaterialReserves?>>
) : Consumption {
    override lateinit var lessQuantity: LinearIntermediateSymbols1
    override lateinit var overQuantity: LinearIntermediateSymbols1

    override fun register(model: AbstractLinearMetaModelF64): Try {
        if (overEnabled) {
            if (!::overQuantity.isInitialized) {
                val overConstraints = mutableListOf<Pair<LinearPolynomial<Flt64>, Flt64>>()
                overQuantity = LinearIntermediateSymbols1(
                    name = "consumption_over_quantity",
                    shape = Shape1(materials.size)
                ) { i, _ ->
                    val (product, demand) = materials[i]
                    if (demand != null && demand.overEnabled) {
                        val slack = SlackFunction(
                            x = quantity[product],
                            threshold = demand.quantity.upperBound.value.unwrap(),
                            type = UContinuous,
                            constraint = false,
                            name = "consumption_over_quantity_${product}"
                        )
                        demand.overQuantity?.let { overConstraints.add(slack.pos!! to it) }
                        slack
                    } else {
                        LinearIntermediateSymbol.empty(
                            name = "consumption_over_quantity_${product}"
                        )
                    }
                }
                for ((pos, overQty) in overConstraints) {
                    when (val result = model.addConstraint(
                        pos leq overQty,
                        name = "consumption_over_quantity_ub"
                    )) {
                        is Ok -> {}
                        is Failed -> return Failed(result.error)
                        is Fatal -> return Fatal(result.errors)
                    }
                }
            }
            when (val result = model.add(overQuantity)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
            }
        }

        if (lessEnabled) {
            if (!::lessQuantity.isInitialized) {
                val lessConstraints = mutableListOf<Pair<LinearPolynomial<Flt64>, Flt64>>()
                lessQuantity = LinearIntermediateSymbols1(
                    name = "consumption_less_quantity",
                    shape = Shape1(materials.size)
                ) { i, _ ->
                    val (product, demand) = materials[i]
                    if (demand != null && demand.lessEnabled) {
                        val slack = SlackFunction(
                            x = quantity[product],
                            threshold = demand.quantity.lowerBound.value.unwrap(),
                            type = UContinuous,
                            withPositive = false,
                            constraint = false,
                            name = "consumption_less_quantity_${product}"
                        )
                        demand.lessQuantity?.let { lessConstraints.add(slack.neg!! to it) }
                        slack
                    } else {
                        LinearIntermediateSymbol.empty(
                            name = "consumption_less_quantity_${product}"
                        )
                    }
                }
                for ((neg, lessQty) in lessConstraints) {
                    when (val result = model.addConstraint(
                        neg geq -lessQty,
                        name = "consumption_less_quantity_lb"
                    )) {
                        is Ok -> {}
                        is Failed -> return Failed(result.error)
                        is Fatal -> return Fatal(result.errors)
                    }
                }
            }
            when (val result = model.add(lessQuantity)) {
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

    /**
     * 提取影子价格
     * Extract shadow prices from slack variables
     *
     * @param Map              影子价格表类型
     * @param shadowPriceMap   影子价格表 / Shadow price map
     * @param shadowPrices     原始影子价格（对偶变量的解）/ Raw shadow prices (dual solution)
     * @return                 成功与否 / Success or failure
     */
    fun <Map : AbstractShadowPriceMap<*, Map>> refresh(
        shadowPriceMap: Map,
        shadowPrices: MetaDualSolution
    ): Try {
        if (::overQuantity.isInitialized) {
            for (overQuantity in this.overQuantity) {
                when (val result = overQuantity.refresh(
                    shadowPriceMap = shadowPriceMap,
                    shadowPrices = shadowPrices
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
        }

        if (::lessQuantity.isInitialized) {
            for (lessQuantity in this.lessQuantity) {
                when (val result = lessQuantity.refresh(
                    shadowPriceMap = shadowPriceMap,
                    shadowPrices = shadowPrices
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
        }

        return ok
    }
}

class TaskSchedulingConsumption<
        out T : ProductionTask<E, A, P, C>,
        out E : Executor,
        out A : AssignmentPolicy<E>,
        P : AbstractMaterial,
        C : AbstractMaterial
        >(
    materials: List<Pair<C, MaterialReserves?>>,
    override val overEnabled: Boolean = false,
    override val lessEnabled: Boolean = false
) : AbstractConsumption<T, E, A, P, C>(materials.sortedBy { it.first.index }) {
    override lateinit var quantity: LinearIntermediateSymbols1

    override fun register(model: AbstractLinearMetaModelF64): Try {
        TODO("NOT IMPLEMENT YET")
    }
}

class BunchSchedulingConsumption<
        out T : ProductionTask<E, A, P, C>,
        out E : Executor,
        out A : AssignmentPolicy<E>,
        P : AbstractMaterial,
        C : AbstractMaterial
        >(
    materials: List<Pair<C, MaterialReserves?>>
) : AbstractConsumption<T, E, A, P, C>(materials.sortedBy { it.first.index }) {
    override val overEnabled: Boolean = true
    override val lessEnabled: Boolean = true

    override lateinit var quantity: LinearExpressionSymbols1

    override fun register(model: AbstractLinearMetaModelF64): Try {
        if (materials.isNotEmpty()) {
            if (!::quantity.isInitialized) {
                quantity = LinearExpressionSymbols1(
                    name = "consumption_quantity",
                    shape = Shape1(materials.size)
                ) { m, _ ->
                    val material = materials[m]
                    LinearExpressionSymbol(
                        name = "consumption_quantity_${material}"
                    )
                }
                for ((material, reserve) in materials) {
                    if (reserve != null) {
                        quantity[material].range.set(
                            ValueRange(
                                reserve.quantity.lowerBound.value.unwrap() - (reserve.lessQuantity ?: Flt64.zero),
                                reserve.quantity.upperBound.value.unwrap() + (reserve.overQuantity ?: Flt64.zero)
                            ).value!!
                        )
                    }
                }
            }
            when (val result = model.add(quantity)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
            }
        }

        return super.register(model)
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

        for ((material) in materials) {
            val thisBunches = bunches.filter { bunch -> bunch.consumption(material) neq Flt64.zero }

            if (thisBunches.isNotEmpty()) {
                quantity[material].flush()
                for (bunch in thisBunches) {
                    quantity[material].asMutable() += LinearMonomial(bunch.consumption(material), xi[bunch])
                }
            }
        }

        return ok
    }
}