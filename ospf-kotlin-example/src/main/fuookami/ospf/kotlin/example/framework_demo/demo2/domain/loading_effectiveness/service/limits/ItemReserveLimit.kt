package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.loading_effectiveness.service.limits


import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.intermediate_symbol.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*

class ItemReserveLimit(
    private val items: List<Item>,
    private val stowage: Stowage,
    private val coefficient: (Item) -> Flt64 = { Flt64.one },
    override val name: String = "item_reserve_limit"
) : Pipeline<AbstractLinearMetaModelFlt64> {
    override fun invoke(model: AbstractLinearMetaModelFlt64): Try {
        val poly = MutableLinearPolynomial()
        for ((i, item) in items.withIndex()) {
            val c = coefficient(item)
            poly += c
            poly += LinearMonomial(-c, stowage.loaded[i])
        }
        when (val result = model.minimize(
            LinearExpressionSymbol(LinearPolynomial(poly.monomials, poly.constant)),
            name = "item reserve"
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        return ok
    }
}


















