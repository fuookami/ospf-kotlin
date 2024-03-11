package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce.service.limits

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce.model.*

class ProduceLessQuantityMinimization<Args : GanttSchedulingShadowPriceArguments<E, A>, E : Executor, A : AssignmentPolicy<E>>(
    products: List<Pair<Product, ProductDemand?>>,
    private val produce: Produce,
    private val threshold: (Product) -> Flt64 = { Flt64.zero },
    private val coefficient: (Product) -> Flt64 = { Flt64.one },
    override val name: String = "produce_less_quantity_minimization"
) : GanttSchedulingCGPipeline<Args, E, A> {
    private val products = if (produce.lessEnabled) {
        products.filter { it.second?.lessEnabled == true }
    } else {
        emptyList()
    }

    override fun invoke(model: LinearMetaModel): Try {
        if (products.isNotEmpty()) {
            val cost = MutableLinearPolynomial()
            for ((product, _) in products) {
                val thresholdValue = threshold(product)
                if (thresholdValue eq Flt64.zero) {
                    cost += coefficient(product) * produce.lessQuantity[product]
                } else {
                    val slack = SlackFunction(
                        UContinuous,
                        x = LinearPolynomial(produce.lessQuantity[product]),
                        threshold = LinearPolynomial(thresholdValue),
                        name = "produce_less_quantity_minimization_threshold_$product"
                    )
                    model.addSymbol(slack)
                    cost += coefficient(product) * slack
                }
            }
            model.minimize(cost, "produce less quantity")
        }
        return Ok(success)
    }
}