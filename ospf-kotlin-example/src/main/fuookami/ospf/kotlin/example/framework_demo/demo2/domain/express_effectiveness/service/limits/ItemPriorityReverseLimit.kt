package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.express_effectiveness.service.limits


import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.express_effectiveness.model.*

class ItemPriorityReverseLimit(
    private val orderedItems: List<ItemPair>,
    private val orderedPositions: List<PositionPair>,
    private val unloading: RelativeOrder,
    private val coefficient: (Pair<Position, Item>, Pair<Position, Item>) -> Flt64 = { _, _ -> Flt64.one },
    override val name: String = "item_priority_reverse_limit"
) : Pipeline<AbstractLinearMetaModelFlt64> {
    override fun invoke(model: AbstractLinearMetaModelFlt64): Try {
        when (val result = model.minimize(
            sum(orderedItems.flatMapIndexed { p1, (item1, item2) ->
                orderedPositions.mapIndexed { p2, (position1, position2) ->
                    coefficient(position2 to item1, position1 to item2) * unloading.itemPriorityReverse[p1, p2]
                }
            }),
            name = "item priority reverse"
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


















