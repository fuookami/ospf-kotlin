package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce.model

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.value_range.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.multi_array.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.model.*

interface Consumption {
    val quantity: LinearSymbols1
    val overQuantity: LinearSymbols1
    val lessQuantity: LinearSymbols1

    val overEnabled: Boolean
    val lessEnabled: Boolean

    fun register(model: MetaModel): Try
}

abstract class AbstractConsumption<
    out T : ProductionTask<E, A>,
    out E : Executor,
    out A : AssignmentPolicy<E>
>(
    val materials: List<Pair<Material, MaterialReserves?>>
) : Consumption {
    override lateinit var lessQuantity: LinearSymbols1
    override lateinit var overQuantity: LinearSymbols1

    override fun register(model: MetaModel): Try {
        if (overEnabled) {
            if (!::overQuantity.isInitialized) {
                overQuantity = LinearSymbols1(
                    "consumption_over_quantity",
                    Shape1(materials.size)
                ) { i, _ ->
                    val (product, demand) = materials[i]
                    if (demand != null && demand.overEnabled) {
                        val slack = SlackFunction(
                            UContinuous,
                            x = LinearPolynomial(quantity[product]),
                            threshold = LinearPolynomial(demand.quantity.upperBound.value.unwrap()),
                            constraint = false,
                            name = "consumption_over_quantity_$product"
                        )
                        demand.overQuantity?.let {
                            slack.pos!!.range.leq(it)
                        }
                        slack
                    } else {
                        LinearExpressionSymbol(LinearPolynomial(), "consumption_over_quantity_$product")
                    }
                }
            }
            when (val result = model.add(overQuantity)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }

        if (lessEnabled) {
            if (!::lessQuantity.isInitialized) {
                lessQuantity = LinearSymbols1(
                    "consumption_less_quantity",
                    Shape1(materials.size)
                ) { i, _ ->
                    val (product, demand) = materials[i]
                    if (demand != null && demand.lessEnabled) {
                        val slack = SlackFunction(
                            UContinuous,
                            x = LinearPolynomial(quantity[product]),
                            threshold = LinearPolynomial(demand.quantity.lowerBound.value.unwrap()),
                            withPositive = false,
                            constraint = false,
                            name = "consumption_less_quantity_$product"
                        )
                        demand.lessQuantity?.let {
                            slack.neg!!.range.leq(it)
                        }
                        slack
                    } else {
                        LinearExpressionSymbol(LinearPolynomial(), "consumption_less_quantity_$product")
                    }
                }
            }
            when (val result = model.add(lessQuantity)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }

        return ok
    }
}

class TaskSchedulingConsumption<
    out T : ProductionTask<E, A>,
    out E : Executor,
    out A : AssignmentPolicy<E>
>(
    materials: List<Pair<Material, MaterialReserves?>>,
    override val overEnabled: Boolean = false,
    override val lessEnabled: Boolean = false
) : AbstractConsumption<T, E, A>(materials.sortedBy { it.first.index }) {
    override lateinit var quantity: LinearSymbols1

    override fun register(model: MetaModel): Try {
        TODO("NOT IMPLEMENT YET")
    }
}

class BunchSchedulingConsumption<
    out T : ProductionTask<E, A>,
    out E : Executor,
    out A : AssignmentPolicy<E>
>(
    materials: List<Pair<Material, MaterialReserves?>>,
) : AbstractConsumption<T, E, A>(materials.sortedBy { it.first.index }) {
    override val overEnabled: Boolean = true
    override val lessEnabled: Boolean = true

    override lateinit var quantity: LinearExpressionSymbols1

    override fun register(model: MetaModel): Try {
        if (materials.isNotEmpty()) {
            if (!::quantity.isInitialized) {
                quantity = flatMap(
                    "consumption_quantity",
                    materials,
                    { _ -> LinearPolynomial() },
                    { (_, m) -> "$m" }
                )
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
                for (bunch in bunches) {
                    quantity[material].asMutable() += bunch.consumption(material) * xi[bunch]
                }
            }
        }

        return ok
    }
}
