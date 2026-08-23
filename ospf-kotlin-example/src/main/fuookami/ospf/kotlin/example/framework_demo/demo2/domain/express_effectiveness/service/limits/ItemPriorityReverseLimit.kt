package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.express_effectiveness.service.limits

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
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.express_effectiveness.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*

/**
 * Minimizes priority reversals between item pairs across position pairs.
 * 最小化位置对之间的项目对的优先级反转。
 *
 * @property orderedItems 用于优先级比较的有序货物项对列表 / The list of ordered item pairs for priority comparison.
 * @property orderedPositions 有序位置对列表 / The list of ordered position pairs.
 * @property unloading 相对顺序优先级反转计算模型 / The relative order model for priority reversal computation.
 * @property coefficient 计算每个货物项-位置对反转惩罚系数的函数 / Function computing the reversal penalty coefficient for each item-position pair.
*/
class ItemPriorityReverseLimit(
    private val orderedItems: List<ItemPair>,
    private val orderedPositions: List<PositionPair>,
    private val unloading: RelativeOrder,
    private val coefficient: (Pair<Position, Item>, Pair<Position, Item>) -> Flt64 = { _, _ -> Flt64.one },
    override val name: String = "item_priority_reverse_limit"
) : Pipeline<AbstractLinearMetaModel<Flt64>> {
    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        val monomials = orderedItems.flatMapIndexed { p1, (item1, item2) ->
            orderedPositions.mapIndexed { p2, (position1, position2) ->
                coefficient(position2 to item1, position1 to item2) * unloading.itemPriorityReverse[p1, p2]
            }
        }
        if (monomials.isEmpty()) {
            return ok
        }

        when (val result = model.minimize(
            sum(monomials),
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
