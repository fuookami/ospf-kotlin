package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.recommended_weight_equalization.service.limits

import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*

import fuookami.ospf.kotlin.utils.functional.*

import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*

import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.token.*

import fuookami.ospf.kotlin.framework.model.*

/** Constrains item ordering so that ordered item pairs cannot be placed in reverse position pairs. */
class ItemOrderLimit(
    private val items: List<Item>,
    private val positions: List<Position>,
    private val orderedItems: List<ItemPair>,
    private val orderedPositions: List<PositionPair>,
    private val stowage: Stowage,
    override val name: String = "item_order_limit"
): Pipeline<AbstractLinearMetaModel<Flt64>> {
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

    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
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
                        is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}

                        is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                            return Failed(result.error)
                        }

                        is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                            return Fatal(result.errors)
                        }
                    }
                }
            }
        }

        return ok
    }
}
