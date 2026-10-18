/**
 * 装载顺序计算服务。 / Loading order calculation service.
 *
 * 对应 BPP3D 的 `LoadingOrderCalculator`：按「先下后上、先左后右」给出物品的装载顺序。
 * 二维侧的排序键为 y 坐标再 x 坐标，与 BLA 的落点优先序一致。
 * / Mirrors BPP3D `LoadingOrderCalculator`: orders items "bottom first, then left to right". The 2D sort
 * key is y then x, matching the BLA placement priority.
*/
package fuookami.ospf.kotlin.framework.bpp2d.domain.item.service

import fuookami.ospf.kotlin.utils.functional.Order
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.framework.geometry2d.*
import fuookami.ospf.kotlin.framework.bpp2d.infrastructure.*

/**
 * 装载顺序计算器。 / Loading order calculator.
 *
 * @param V 数值类型 / numeric type
*/
class LoadingOrderCalculator<V : FloatingNumber<V>> {

    /**
     * 计算放置的装载顺序。 / Compute the loading order of placements.
     *
     * @param placements 放置列表 / placement list
     * @return 放置到装载序号的映射，首个装载的序号为 1 / placement to loading order mapping, the first loaded is 1
    */
    operator fun invoke(placements: List<Placement2<V>>): Map<Placement2<V>, UInt64> {
        val sorted = placements.sortedWith { lhs, rhs -> compare(lhs, rhs) }
        val orders = LinkedHashMap<Placement2<V>, UInt64>()
        for ((index, placement) in sorted.withIndex()) {
            orders[placement] = UInt64((index + 1).toULong())
        }
        return orders
    }

    /**
     * 比较两个放置的装载优先级：先 y 再 x，最后按包围面积大者优先。 / Compare two placements by loading
     * priority: y then x, finally the larger bounding area first.
     *
     * @param lhs 左放置 / left placement
     * @param rhs 右放置 / right placement
     * @return 比较结果，负数表示左放置先装载 / comparison result, negative means the left loads first
    */
    private fun compare(lhs: Placement2<V>, rhs: Placement2<V>): Int {
        when (quantityOrdOrNull(lhs.y, rhs.y)) {
            is Order.Less -> return -1
            is Order.Greater -> return 1
            is Order.Equal, null -> {}
        }
        when (quantityOrdOrNull(lhs.x, rhs.x)) {
            is Order.Less -> return -1
            is Order.Greater -> return 1
            is Order.Equal, null -> {}
        }
        return when (quantityOrdOrNull(lhs.shape.boundingArea, rhs.shape.boundingArea)) {
            is Order.Greater -> -1
            is Order.Less -> 1
            is Order.Equal, null -> 0
        }
    }
}