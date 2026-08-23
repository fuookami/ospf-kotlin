package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.loading_effectiveness.model

import fuookami.ospf.kotlin.utils.error.*
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
 * Models sequential loading order reversal between item pairs across position pairs.
 * 建模位置对之间的项目对顺序装载反转。
 *
 * @property items 货物项列表 / The list of cargo items.
 * @property positions 配载位置列表 / The list of stowage positions.
 * @property orderedItems 用于装载顺序比较的有序货物项对列表 / The list of ordered item pairs for loading order comparison.
 * @property orderedPositions 有序位置对列表 / The list of ordered position pairs.
 * @property stowage 配载分配模型 / The stowage assignment model.
*/
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
            val orderedItems = items
                .filter { it.order?.order != null }
                .sortedWith(compareBy<Item> { it.order!!.order!! }.thenBy { it.id })
                .flatMapIndexed { index, item ->
                    items
                        .filter { it.order?.order != null }
                        .sortedWith(compareBy<Item> { it.order!!.order!! }.thenBy { it.id })
                        .drop(index + 1)
                        .mapNotNull { other ->
                            (item to other).takeIf {
                                item.order!!.order!! < other.order!!.order!!
                            }
                        }
                }

            return SequentialLoading(
                items = items,
                positions = positions,
                orderedItems = orderedItems,
                orderedPositions = orderedPositions,
                stowage = stowage
            )
        }
    }

    lateinit var itemOrderReverse: LinearIntermediateSymbols2<Flt64>

    /**
     * Registers the item order reverse intermediate symbols into the optimization model.
     * 将项目顺序反转中间符号注册到优化模型中。
     *
     * @param model 要注册到的线性元模型 / The linear meta model to register into.
     * @return 注册操作的结果 / The result of the registration operation.
    */
    fun register(
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        if (!::itemOrderReverse.isInitialized) {
            itemOrderReverse = LinearIntermediateSymbols2<Flt64>("item_order_reverse", Shape2(orderedItems.size, orderedPositions.size)) { _, v ->
                val (item1, item2) = orderedItems[v[0]]
                val i1 = items.indexOf(item1)
                val i2 = items.indexOf(item2)
                val (position1, position2) = orderedPositions[v[1]]
                val j1 = positions.indexOf(position1)
                val j2 = positions.indexOf(position2)

                if (Stowage.stowageNeeded(item2, position1) && Stowage.stowageNeeded(item1, position2)) {
                    LinearFunctionSymbolAdapter(
                        delegate = IfFunction(
                            condition = stowage.stowage[i1, j2] + stowage.stowage[i2, j1]
                                - Flt64.two + Flt64(NONZERO_TOLERANCE),
                            converter = flt64Converter,
                            name = "item_order_reverse_${item1}_${item2}_${position1}_${position2}"
                        ),
                        converter = flt64Converter
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

