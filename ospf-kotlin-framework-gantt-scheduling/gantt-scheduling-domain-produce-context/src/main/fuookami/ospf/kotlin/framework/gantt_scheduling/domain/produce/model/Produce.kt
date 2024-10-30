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

interface Produce {
    val quantity: LinearSymbols1
    val overQuantity: LinearSymbols1
    val lessQuantity: LinearSymbols1

    val overEnabled: Boolean
    val lessEnabled: Boolean

    fun register(model: MetaModel): Try
}

abstract class AbstractProduce<
    out T : ProductionTask<E, A>,
    out E : Executor,
    out A : AssignmentPolicy<E>
>(
    val products: List<Pair<Product, ProductDemand?>>
) : Produce {
    override lateinit var lessQuantity: LinearSymbols1
    override lateinit var overQuantity: LinearSymbols1

    override fun register(model: MetaModel): Try {
        if (overEnabled) {
            if (!::overQuantity.isInitialized) {
                overQuantity = LinearSymbols1(
                    "produce_over_quantity",
                    Shape1(products.size)
                ) { i, _ ->
                    val (product, demand) = products[i]
                    if (demand != null && demand.overEnabled) {
                        val slack = SlackFunction(
                            UContinuous,
                            x = LinearPolynomial(quantity[product]),
                            threshold = LinearPolynomial(demand.quantity.upperBound.value.unwrap()),
                            constraint = false,
                            name = "produce_over_quantity_$product"
                        )
                        demand.overQuantity?.let {
                            slack.pos!!.range.leq(it)
                        }
                        slack
                    } else {
                        LinearExpressionSymbol(LinearPolynomial(), "produce_over_quantity_$product")
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
                    "produce_less_quantity",
                    Shape1(products.size)
                ) { i, _ ->
                    val (product, demand) = products[i]
                    if (demand != null && demand.lessEnabled) {
                        val slack = SlackFunction(
                            UContinuous,
                            x = LinearPolynomial(quantity[product]),
                            threshold = LinearPolynomial(demand.quantity.lowerBound.value.unwrap()),
                            withPositive = false,
                            constraint = false,
                            name = "produce_less_quantity_$product"
                        )
                        demand.lessQuantity?.let {
                            slack.neg!!.range.leq(it)
                        }
                        slack
                    } else {
                        LinearExpressionSymbol(LinearPolynomial(), "produce_less_quantity_$product")
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

class TaskSchedulingProduce<
    out T : ProductionTask<E, A>,
    out E : Executor,
    out A : AssignmentPolicy<E>
>(
    products: List<Pair<Product, ProductDemand?>>,
    override val overEnabled: Boolean = false,
    override val lessEnabled: Boolean = false
) : AbstractProduce<T, E, A>(products.sortedBy { it.first.index }) {
    override lateinit var quantity: LinearSymbols1

    override fun register(model: MetaModel): Try {
        TODO("NOT IMPLEMENT YET")
    }
}

class BunchSchedulingProduce<
    out T : ProductionTask<E, A>,
    out E : Executor,
    out A : AssignmentPolicy<E>
>(
    products: List<Pair<Product, ProductDemand?>>
) : AbstractProduce<T, E, A>(products.sortedBy { it.first.index }) {
    override val overEnabled: Boolean = true
    override val lessEnabled: Boolean = true

    override lateinit var quantity: LinearExpressionSymbols1

    override fun register(model: MetaModel): Try {
        if (products.isNotEmpty()) {
            if (!::quantity.isInitialized) {
                quantity = flatMap(
                    "produce_quantity",
                    products,
                    { _ -> LinearPolynomial() },
                    { (_, p) -> "$p" }
                )
                for ((product, demand) in products) {
                    if (demand != null) {
                        quantity[product].range.set(
                            ValueRange(
                                demand.quantity.lowerBound.value.unwrap() - (demand.lessQuantity ?: Flt64.zero),
                                demand.quantity.upperBound.value.unwrap() + (demand.overQuantity ?: Flt64.zero)
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

        for ((product, _) in products) {
            val thisBunches = bunches.filter { bunch -> bunch.produce(product) neq Flt64.zero }

            if (thisBunches.isNotEmpty()) {
                quantity[product].flush()
                for (bunch in thisBunches) {
                    quantity[product].asMutable() += bunch.produce(product) * xi[bunch]
                }
            }
        }

        return ok
    }
}
