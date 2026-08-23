package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.loading_effectiveness.service.limits

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.loading_effectiveness.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*

/**
 * Minimizes order reversals between item pairs across position pairs in sequential loading.
 * 最小化顺序装载中位置对之间的项目对的顺序反转。
 *
 * @property orderedItems 用于装载顺序比较的有序货物项对列表 / The list of ordered item pairs for loading order comparison.
 * @property orderedPositions 有序位置对列表 / The list of ordered position pairs.
 * @property loading 提供顺序反转符号的顺序装载模型 / The sequential loading model providing order reverse symbols.
 * @property coefficient 计算每个货物项-位置对反转惩罚系数的函数 / Function computing the reversal penalty coefficient for each item-position pair.
*/
class ItemOrderReverseLimit(
    private val orderedItems: List<ItemPair>,
    private val orderedPositions: List<PositionPair>,
    private val loading: SequentialLoading,
    private val coefficient: (Pair<Position, Item>, Pair<Position, Item>) -> Flt64 = { _, _ -> Flt64.one },
    override val name: String = "item_order_reverse_limit"
) : Pipeline<AbstractLinearMetaModel<Flt64>> {
    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        val monomials = orderedItems.flatMapIndexed { p1, (item1, item2) ->
            orderedPositions.mapIndexed { p2, (position1, position2) ->
                coefficient(position2 to item1, position1 to item2) * loading.itemOrderReverse[p1, p2]
            }
        }
        if (monomials.isEmpty()) {
            return ok
        }

        when (val result = model.minimize(
            sum(monomials),
            name = "item order reverse"
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

