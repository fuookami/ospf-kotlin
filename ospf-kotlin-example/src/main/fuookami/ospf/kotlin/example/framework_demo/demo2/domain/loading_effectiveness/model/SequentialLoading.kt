package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.loading_effectiveness.model


import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.multiarray.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.intermediate_symbol.*
import fuookami.ospf.kotlin.core.intermediate_symbol.function.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*

class SequentialLoading(
    private val items: List<Item>,
    private val positions: List<Position>,
    internal val orderedItems: List<ItemPair>,
    private val orderedPositions: List<PositionPair>,
    private val stowage: Stowage
) {
    companion object {
        operator fun invoke(
            items: List<Item>,
            positions: List<Position>,
            orderedPositions: List<PositionPair>,
            stowage: Stowage
        ): SequentialLoading {
            TODO("not implemented yet")
        }
    }

    lateinit var itemOrderReverse: LinearIntermediateSymbols2Flt64

    fun register(
        model: AbstractLinearMetaModelFlt64
    ): Try {
        if (!::itemOrderReverse.isInitialized) {
            itemOrderReverse = LinearIntermediateSymbols2Flt64("item_order_reverse", Shape2(orderedItems.size, orderedPositions.size)) { _, v ->
                val (item1, item2) = orderedItems[v[0]]
                val i1 = items.indexOf(item1)
                val i2 = items.indexOf(item2)
                val (position1, position2) = orderedPositions[v[1]]
                val j1 = positions.indexOf(position1)
                val j2 = positions.indexOf(position2)

                if (Stowage.stowageNeeded(item2, position1) && Stowage.stowageNeeded(item1, position2)) {
                    IfFunction(
                        (stowage.stowage[i1, j2] + stowage.stowage[i2, j1]) geq Flt64.two,
                        name = "item_order_reverse_${item1}_${item2}_${position1}_${position2}"
                    )
                } else {
                    LinearExpressionSymbol(
                        Flt64.zero,
                        name = "item_order_reverse_${item1}_${item2}_${position1}_${position2}",
                    )
                }
            }
        }
        when (val result = model.add(itemOrderReverse)) {
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













