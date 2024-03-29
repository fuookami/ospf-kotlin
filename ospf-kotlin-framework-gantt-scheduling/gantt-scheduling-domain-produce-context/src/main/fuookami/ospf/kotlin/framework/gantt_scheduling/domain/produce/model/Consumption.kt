package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce.model

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.multi_array.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_scheduling.model.*

interface Consumption {
    val quantity: LinearSymbols1
    val overQuantity: LinearSymbols1
    val lessQuantity: LinearSymbols1

    val overEnabled: Boolean
    val lessEnabled: Boolean

    fun register(model: LinearMetaModel): Try
}

abstract class AbstractConsumption<T : ProductionTask<E, A>, E : Executor, A : AssignmentPolicy<E>>(
    val materials: List<Pair<Material, MaterialReserves?>>
) : Consumption {
    override lateinit var lessQuantity: LinearSymbols1
    override lateinit var overQuantity: LinearSymbols1

    override fun register(model: LinearMetaModel): Try {
        if (overEnabled) {
            if (!::overQuantity.isInitialized) {
                overQuantity = LinearSymbols1(
                    "consumption_over_quantity",
                    Shape1(materials.size)
                ) { (i, _) ->
                    val (product, demand) = materials[i]
                    if (demand != null && demand.overEnabled) {
                        val slack = SlackFunction(
                            UContinuous,
                            x = LinearPolynomial(quantity[product]),
                            threshold = LinearPolynomial(demand.quantity.upperBound.toFlt64()),
                            constraint = false,
                            name = "consumption_over_quantity_$product"
                        )
                        demand.overQuantity?.let {
                            (slack.pos as URealVar).range.leq(it)
                        }
                        slack
                    } else {
                        ExpressionSymbol(LinearPolynomial(), "consumption_over_quantity_$product")
                    }
                }
            }
            model.addSymbols(overQuantity)
        }

        if (lessEnabled) {
            if (!::lessQuantity.isInitialized) {
                lessQuantity = LinearSymbols1(
                    "consumption_less_quantity",
                    Shape1(materials.size)
                ) { (i, _) ->
                    val (product, demand) = materials[i]
                    if (demand != null && demand.lessEnabled) {
                        val slack = SlackFunction(
                            UContinuous,
                            x = LinearPolynomial(quantity[product]),
                            threshold = LinearPolynomial(demand.quantity.lowerBound.toFlt64()),
                            withPositive = false,
                            constraint = false,
                            name = "consumption_less_quantity_$product"
                        )
                        demand.lessQuantity?.let {
                            (slack.neg as URealVar).range.leq(it)
                        }
                        slack
                    } else {
                        ExpressionSymbol(LinearPolynomial(), "consumption_less_quantity_$product")
                    }
                }
            }
            model.addSymbols(lessQuantity)
        }

        return Ok(success)
    }
}

class TaskSchedulingConsumption<T : ProductionTask<E, A>, E : Executor, A : AssignmentPolicy<E>>(
    materials: List<Pair<Material, MaterialReserves?>>,
    override val overEnabled: Boolean = false,
    override val lessEnabled: Boolean = false
) : AbstractConsumption<T, E, A>(materials.sortedBy { it.first.index }) {
    override lateinit var quantity: LinearSymbols1

    override fun register(model: LinearMetaModel): Try {
        TODO("NOT IMPLEMENT YET")
    }
}

class BunchSchedulingConsumption<T : ProductionTask<E, A>, E : Executor, A : AssignmentPolicy<E>>(
    materials: List<Pair<Material, MaterialReserves?>>,
) : AbstractConsumption<T, E, A>(materials.sortedBy { it.first.index }) {
    override val overEnabled: Boolean = true
    override val lessEnabled: Boolean = true

    override lateinit var quantity: LinearExpressionSymbols1

    override fun register(model: LinearMetaModel): Try {
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
                                reserve.quantity.lowerBound.toFlt64() - (reserve.lessQuantity ?: Flt64.zero),
                                reserve.quantity.upperBound.toFlt64() + (reserve.overQuantity ?: Flt64.zero)
                            )
                        )
                    }
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

        for ((material) in materials) {
            val thisBunches = bunches.filter { bunch -> bunch.consumption(material) neq Flt64.zero }

            if (thisBunches.isNotEmpty()) {
                quantity[material].flush()
                for (bunch in bunches) {
                    quantity[material].asMutable() += bunch.consumption(material) * xi[bunch]
                }
            }
        }

        return Ok(success)
    }
}
