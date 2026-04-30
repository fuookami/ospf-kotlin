package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.recommended_weight_equalization.service.limits

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*

class ItemOrderLimit(
    private val items: List<Item>,
    private val positions: List<Position>,
    private val orderedItems: List<ItemPair>,
    private val orderedPositions: List<PositionPair>,
    private val stowage: Stowage,
    override val name: String = "item_order_limit"
): Pipeline<AbstractLinearMetaModelFlt64> {
    companion object {
        operator fun invoke(
            items: List<Item>,
            positions: List<Position>,
            stowage: Stowage,
            name: String = "item_order_limit"
        ): ItemOrderLimit {
            TODO("not implemented yet")
        }
    }

    override fun invoke(model: AbstractLinearMetaModelFlt64): Try {
        for ((item1, item2) in orderedItems) {
            val i1 = items.indexOf(item1)
            val i2 = items.indexOf(item2)

            for ((position1, position2) in orderedPositions) {
                val j1 = positions.indexOf(position1)
                val j2 = positions.indexOf(position2)

                if (Stowage.stowageNeeded(item1, position2) && Stowage.stowageNeeded(item2, position1)) {
                    val poly = MutableLinearPolynomial()
                    poly += LinearMonomial(Flt64.one, stowage.stowage[i1, j2])
                    poly += LinearMonomial(Flt64.one, stowage.stowage[i2, j1])
                    when (val result = model.addConstraint(
            relation = LinearPolynomial(poly) leq Flt64.one,
            name = "${name}_${item1}_${item2}_${position1}_${position2}"
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
        }

        return ok
    }
}



















