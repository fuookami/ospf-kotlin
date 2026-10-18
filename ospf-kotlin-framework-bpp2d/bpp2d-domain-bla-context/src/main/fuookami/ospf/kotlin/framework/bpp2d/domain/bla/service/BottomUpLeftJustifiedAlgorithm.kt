/**
 * 底部向上左对齐算法。 / Bottom-up left-justified algorithm.
 *
 * 与 BPP3D 的 `BottomUpLeftJustifiedAlgorithm` 对应，但收敛为二维：在板材平面上依次放置物品，
 * 每次都选择最低且最靠左的可行点。算法不使用流式返回，也不使用非空断言；圆形件不参与 BLA
 * （圆形件没有「左下推挤」语义），会被显式跳过并在结果中标记来源。
 * / Mirrors BPP3D `BottomUpLeftJustifiedAlgorithm` reduced to 2D: items are placed on the sheet plane
 * always choosing the lowest then leftmost feasible point. The algorithm returns a result instead of a
 * stream and avoids non-null assertions. Circle items do not participate in BLA (they have no
 * "push to bottom-left" semantics) and are skipped with an explicit source marker.
*/
package fuookami.ospf.kotlin.framework.bpp2d.domain.bla.service

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.*
import fuookami.ospf.kotlin.framework.bpp2d.infrastructure.*

/**
 * 比较两个落点的放置优先级：先比较「先下后左」的加权距离，再比较 y，最后比较 x。 / Compare two positions by
 * placement priority: the bottom-left weighted distance first, then y, then x.
 *
 * @param V 数值类型 / numeric type
 * @param lhs 左侧落点 / left-hand position
 * @param rhs 右侧落点 / right-hand position
 * @param withWeightedDistance 是否先按「y 优先、x 次之」的加权距离排序 / whether to order by the
 * y-first, x-second weighted distance
 * @return 比较结果，不可比较时返回 null / comparison order, or null when incomparable
*/
fun <V : FloatingNumber<V>> comparePlacementPosition(
    lhs: QuantityPoint2<V>,
    rhs: QuantityPoint2<V>,
    withWeightedDistance: Boolean = true
): Order? {
    if (withWeightedDistance) {
        val lhsDistance = lhs.x + lhs.y
        val rhsDistance = rhs.x + rhs.y
        when (val distanceOrder = quantityOrdOrNull(lhsDistance, rhsDistance)) {
            null -> return null
            is Order.Equal -> {}
            else -> return distanceOrder
        }
    }
    when (val yOrder = quantityOrdOrNull(lhs.y, rhs.y)) {
        null -> return null
        is Order.Equal -> {}
        else -> return yOrder
    }
    return quantityOrdOrNull(lhs.x, rhs.x)
}

/**
 * 底部向上左对齐算法。 / Bottom-up left-justified algorithm.
 *
 * @param V 数值类型 / numeric type
 * @property sheet 板材形状 / sheet shape
 * @property config 算法配置 / algorithm configuration
*/
class BottomUpLeftJustifiedAlgorithm<V : FloatingNumber<V>>(
    private val sheet: Sheet2Shape<V>,
    private val config: Config<V> = Config(),
    private val kerf: Quantity<V>? = null
) {

    /**
     * 算法配置。 / Algorithm configuration.
     *
     * @param V 数值类型 / numeric type
     * @property withDisplacementX 是否允许沿 X 轴向左滑动 / whether sliding left along X is allowed
     * @property withDisplacementY 是否允许沿 Y 轴向下滑动 / whether sliding down along Y is allowed
     * @property withWeightedDistance 是否按加权距离排序候选点 / whether candidate points are ordered by weighted distance
    */
    data class Config<V : FloatingNumber<V>>(
        val withDisplacementX: Boolean = true,
        val withDisplacementY: Boolean = true,
        val withWeightedDistance: Boolean = true
    ) {
        /** 是否启用任一方向的滑动 / whether sliding is enabled in either direction */
        val withDisplacement: Boolean get() = withDisplacementX || withDisplacementY
    }

    /**
     * 执行排样。 / Execute the layout.
     *
     * @param items 物品列表，含件数 / item list with amounts
     * @return 放置结果：成功时返回放置列表（可能为空）；圆形件被跳过 / layout result: the placement list on
     * success (possibly empty); circle items are skipped
    */
    fun invoke(items: List<Pair<ActualItem<V>, Int>>): Ret<Bla2dLayoutResult<V>> {
        val placements = ArrayList<LayerPlacement2<V>>()
        val skippedCircleItems = ArrayList<ItemId>()
        val skippedUnfittedItems = ArrayList<ItemId>()

        for ((item, amount) in items) {
            if (item.isCircleItem()) {
                skippedCircleItems.add(item.id)
                continue
            }
            repeat(amount) {
                val placed = placeItem(item, placements)
                if (placed != null) {
                    placements.add(placed)
                } else {
                    skippedUnfittedItems.add(item.id)
                }
            }
        }

        return ok(
            Bla2dLayoutResult(
                placements = placements,
                skippedCircleItems = skippedCircleItems,
                skippedUnfittedItems = skippedUnfittedItems
            )
        )
    }

    /**
     * 放置单个物品。 / Place a single item.
     *
     * @param item 物品 / item
     * @param placed 已放置列表 / already placed placements
     * @return 放置，若无可行点则返回 null / placement, or null when no feasible point exists
    */
    private fun placeItem(
        item: ActualItem<V>,
        placed: List<LayerPlacement2<V>>
    ): LayerPlacement2<V>? {
        for (candidate in item.candidateShapes()) {
            val shape = candidate.shape
            val feasible = feasiblePoints(placed)
            for (point in feasible) {
                val placement = LayerPlacement2(
                    shape = shape,
                    position = point,
                    rotated = candidate.rotated,
                    itemId = item.id.value
                )
                if (isFeasible(placement, placed)) {
                    return placement
                }
            }
        }
        return null
    }

    /**
     * 计算所有候选落点。 / Compute all candidate positions.
     *
     * 候选点由已放置矩形的「左上角」与「右下角」派生；为空时给出原点。 / Candidates derive from the
     * upper-left and lower-right corners of placed rectangles, falling back to the origin.
     *
     * @param placed 已放置列表 / already placed placements
     * @return 排序后的候选点 / sorted candidate positions
    */
    private fun feasiblePoints(placed: List<LayerPlacement2<V>>): List<QuantityPoint2<V>> {
        val points = LinkedHashSet<QuantityPoint2<V>>()
        for (placement in placed) {
            val maxX = (placement.maxX() as? Ok)?.value ?: continue
            val maxY = (placement.maxY() as? Ok)?.value ?: continue
            points.add(QuantityPoint2(x = placement.x, y = maxY))
            points.add(QuantityPoint2(x = maxX, y = placement.y))
            if (config.withDisplacement) {
                points.addAll(displacedPoints(placement, maxX, maxY, placed))
            }
        }
        val origin = originPoint2Of(sheet.width)
        if (points.isEmpty()) {
            return listOf(origin)
        }

        val insideSheet = points.filter { point ->
            quantityGreaterOrEqual(point.x, origin.x)
                    && quantityGreaterOrEqual(point.y, origin.y)
                    && quantityLessOrEqual(point.x, sheet.width)
                    && quantityLessOrEqual(point.y, sheet.height)
        }
        val usable = if (insideSheet.isEmpty()) listOf(origin) else insideSheet
        return usable.sortedWith { lhs, rhs ->
            when (comparePlacementPosition(lhs, rhs, config.withWeightedDistance)) {
                is Order.Less -> -1
                is Order.Greater -> 1
                is Order.Equal, null -> 0
            }
        }
    }

    /**
     * 由既有放置沿 X/Y 轴滑动得到额外候选点。 / Derive extra candidate positions by sliding along X and Y.
     *
     * @param placement 参考放置 / reference placement
     * @param maxX 参考放置的右边界 / right edge of the reference placement
     * @param maxY 参考放置的上边界 / top edge of the reference placement
     * @param placed 已放置列表 / already placed placements
     * @return 额外候选点 / extra candidate positions
    */
    private fun displacedPoints(
        placement: LayerPlacement2<V>,
        maxX: Quantity<V>,
        maxY: Quantity<V>,
        placed: List<LayerPlacement2<V>>
    ): List<QuantityPoint2<V>> {
        val points = ArrayList<QuantityPoint2<V>>()
        if (config.withDisplacementX) {
            points.add(QuantityPoint2(x = pointOnX(placement, placed), y = maxY))
        }
        if (config.withDisplacementY) {
            points.add(QuantityPoint2(x = maxX, y = pointOnY(placement, placed)))
        }
        return points
    }

    /**
     * 沿 X 轴向左滑动到最近的可行位置。 / Slide left along X to the nearest feasible position.
     *
     * @param placement 参考放置 / reference placement
     * @param placed 已放置列表 / already placed placements
     * @return 滑动后的 x 坐标 / x coordinate after sliding
    */
    private fun pointOnX(placement: LayerPlacement2<V>, placed: List<LayerPlacement2<V>>): Quantity<V> {
        val placementMaxY = (placement.maxY() as? Ok)?.value ?: placement.y
        var best = quantityZeroOf(placement.x)
        for (other in placed) {
            if (other === placement) {
                continue
            }
            val otherMaxX = (other.maxX() as? Ok)?.value ?: continue
            val otherMaxY = (other.maxY() as? Ok)?.value ?: continue
            // 仅考虑在 Y 方向与本放置重叠、且完全位于其左侧的放置。
            // Only consider placements overlapping in Y and lying fully to the left.
            val overlapsInY = quantityLess(other.y, placementMaxY) && quantityGreater(otherMaxY, placement.y)
            val liesLeft = quantityLessOrEqual(otherMaxX, placement.x)
            if (overlapsInY && liesLeft && quantityGreater(otherMaxX, best)) {
                best = otherMaxX
            }
        }
        return best
    }

    /**
     * 沿 Y 轴向下滑动到最近的可行位置。 / Slide down along Y to the nearest feasible position.
     *
     * @param placement 参考放置 / reference placement
     * @param placed 已放置列表 / already placed placements
     * @return 滑动后的 y 坐标 / y coordinate after sliding
    */
    private fun pointOnY(placement: LayerPlacement2<V>, placed: List<LayerPlacement2<V>>): Quantity<V> {
        var best = quantityZeroOf(placement.y)
        val placementMaxX = (placement.maxX() as? Ok)?.value ?: placement.x
        for (other in placed) {
            if (other === placement) {
                continue
            }
            val otherMaxX = (other.maxX() as? Ok)?.value ?: continue
            val otherMaxY = (other.maxY() as? Ok)?.value ?: continue
            // 仅考虑在 X 方向与本放置重叠、且完全位于其下方的放置。
            // Only consider placements overlapping in X and lying fully below.
            val overlapsInX = quantityLess(other.x, placementMaxX) && quantityGreater(otherMaxX, placement.x)
            val liesBelow = quantityLessOrEqual(otherMaxY, placement.y)
            if (overlapsInX && liesBelow && quantityGreater(otherMaxY, best)) {
                best = otherMaxY
            }
        }
        return best
    }

    /**
     * 判断放置是否可行：在板材内且与既有放置不重叠。 / Check whether a placement is feasible: inside the sheet
     * and not overlapping existing placements.
     *
     * @param placement 待检查放置 / placement to check
     * @param placed 已放置列表 / already placed placements
     * @return 是否可行 / whether feasible
    */
    private fun isFeasible(placement: LayerPlacement2<V>, placed: List<LayerPlacement2<V>>): Boolean {
        val inside = when (val result = placement.insideSheet(sheet)) {
            is Ok -> result.value
            is Failed -> return false
            is Fatal -> return false
        }
        if (!inside) {
            return false
        }
        for (other in placed) {
            // 刀缝双向检查：任一侧膨胀后相交，说明真实间隙小于 kerf。
            // Kerf is checked in both directions: an intersection after inflating either side means that the real
            // gap is below the kerf.
            val overlapped = if (kerf != null && quantityIsPositive(kerf)) {
                val forward = when (
                    val result = placement.copy(shape = placement.shape.inflatedBy(kerf)).overlapped(other)
                ) {
                    is Ok -> result.value
                    is Failed -> return false
                    is Fatal -> return false
                }
                if (forward) {
                    true
                } else {
                    when (
                        val result = placement.overlapped(other.copy(shape = other.shape.inflatedBy(kerf)))
                    ) {
                        is Ok -> result.value
                        is Failed -> return false
                        is Fatal -> return false
                    }
                }
            } else {
                when (val result = placement.overlapped(other)) {
                    is Ok -> result.value
                    is Failed -> return false
                    is Fatal -> return false
                }
            }
            if (overlapped) {
                return false
            }
        }
        return true
    }
}

/**
 * BLA 排样结果。 / BLA layout result.
 *
 * @param V 数值类型 / numeric type
 * @property placements 放置列表 / placement list
 * @property skippedCircleItems 被跳过的圆形件编号 / identifiers of skipped circle items
 * @property skippedUnfittedItems 无法放置的物品编号 / identifiers of items that could not be placed
*/
data class Bla2dLayoutResult<V : FloatingNumber<V>>(
    val placements: List<LayerPlacement2<V>>,
    val skippedCircleItems: List<ItemId> = emptyList(),
    val skippedUnfittedItems: List<ItemId> = emptyList()
) {

    /** 是否全部放置成功 / whether everything was placed successfully */
    val fullyPlaced: Boolean get() = skippedUnfittedItems.isEmpty() && skippedCircleItems.isEmpty()
}
