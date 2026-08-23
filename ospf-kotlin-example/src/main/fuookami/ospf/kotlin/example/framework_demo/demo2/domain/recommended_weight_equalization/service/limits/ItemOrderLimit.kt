package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.recommended_weight_equalization.service.limits

import fuookami.ospf.kotlin.utils.error.*
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
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*

/**
 * Constrains item ordering so that ordered item pairs cannot be placed in reverse position pairs.
 * 约束项目排序使得有序项目对不能放置在反向位置对中。
 *
 * @property items 货物项目列表 / The list of cargo items
 * @property positions 装载位置列表 / The list of stowage positions
 * @property orderedItems 有序项目对列表 / The list of ordered item pairs
 * @property orderedPositions 有序位置对列表 / The list of ordered position pairs
 * @property stowage 装载分配矩阵 / The stowage assignment matrix
*/
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
            val orderedItems = items
                .sortedWith(compareByDescending<Item> { it.cargo.priority.priority }.thenBy { it.id })
                .flatMapIndexed { index, item ->
                    items
                        .sortedWith(compareByDescending<Item> { it.cargo.priority.priority }.thenBy { it.id })
                        .drop(index + 1)
                        .mapNotNull { other ->
                            (item to other).takeIf {
                                item.cargo.priority.priority > other.cargo.priority.priority
                            }
                        }
                }
            val orderedPositions = positions
                .sortedWith(compareBy<Position> { it.loadingOrder.order }.thenBy { it.id.toString() })
                .flatMapIndexed { index, position ->
                    positions
                        .sortedWith(compareBy<Position> { it.loadingOrder.order }.thenBy { it.id.toString() })
                        .drop(index + 1)
                        .mapNotNull { other ->
                            (position to other).takeIf {
                                position.loadingOrder.order < other.loadingOrder.order
                            }
                        }
                }

            return ItemOrderLimit(
                items = items,
                positions = positions,
                orderedItems = orderedItems,
                orderedPositions = orderedPositions,
                stowage = stowage,
                name = name
            )
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
                    when (val result = model.addConstraint(
            relation = (stowage.stowage[i1, j2] + stowage.stowage[i2, j1]) leq Flt64.one,
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
