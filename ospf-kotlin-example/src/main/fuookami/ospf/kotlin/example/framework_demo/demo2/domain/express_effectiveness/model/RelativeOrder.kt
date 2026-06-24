package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.express_effectiveness.model

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.multiarray.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.*
import fuookami.ospf.kotlin.core.symbol.function.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*

private val flt64Converter = object : IntoValue<Flt64> {
    override fun intoValue(value: Flt64) = value
    override val zero get() = Flt64.zero
    override val one get() = Flt64.one
    override fun fromValue(value: Flt64) = value
}

/**
 * 计算快递效能中位置对之间的项目对的相对优先级反转。Computes relative priority reversal between item pairs across position pairs for express effectiveness.
 *
 * @property items 参数。
 * @property positions 参数。
 * @property internal val orderedItems 参数。
 * @property internal val orderedPositions 参数。
 * @property stowage 参数。
 */
class RelativeOrder(
    private val items: List<Item>,
    private val positions: List<Position>,
    internal val orderedItems: List<ItemPair>,
    internal val orderedPositions: List<PositionPair>,
    private val stowage: Stowage
) {
    companion object {
        operator fun invoke(
            items: List<Item>,
            positions: List<Position>,
            stowage: Stowage
        ): RelativeOrder {
            TODO("not implemented yet")
        }
    }

    lateinit var itemPriorityReverse: LinearIntermediateSymbols2<Flt64>

    fun register(
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        if (!::itemPriorityReverse.isInitialized) {
            itemPriorityReverse = LinearIntermediateSymbols2<Flt64>("item_priority_reverse", Shape2(items.size, positions.size)) { _, v ->
                val (item1, item2) = orderedItems[v[0]]
                val i1 = items.indexOf(item1)
                val i2 = items.indexOf(item2)
                val (position1, position2) = orderedPositions[v[1]]
                val j1 = positions.indexOf(position1)
                val j2 = positions.indexOf(position2)

                if (Stowage.stowageNeeded(item2, position1) && Stowage.stowageNeeded(item1, position2)) {
                    LinearFunctionSymbolAdapter(
                        delegate = IfFunction(
                            condition = stowage.stowage[i1, j2] + stowage.stowage[i2, j1] - Flt64.two,
                            converter = flt64Converter,
                            name = "item_priority_reverse_${item1}_${item2}_${position1}_${position2}"
                        ),
                        converter = flt64Converter
                    )
                } else {
                    LinearExpressionSymbol(
                        Flt64.zero,
                        name = "item_priority_reverse_${item1}_${item2}_${position1}_${position2}",
                    )
                }
            }
        }
        when (val result = model.add(itemPriorityReverse)) {
            is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}

            is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Failed(result.error)
                }

                is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Fatal(result.errors)
                }
        }

        return ok
    }
}
