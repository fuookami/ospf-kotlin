/**
 * 空间切分搜索算法。 / Space-splitting search algorithms.
 *
 * 与 BPP3D 的 `DepthFirstSearchAlgorithm` / `MultiLayerHeuristicSearchAlgorithm` 对应，裁剪为二维：
 * 通过递归切分剩余空间逐块放置物品。两者都不使用异常表达失败，返回放置结果。
 * / Mirrors BPP3D `DepthFirstSearchAlgorithm` / `MultiLayerHeuristicSearchAlgorithm` reduced to 2D:
 * items are placed block by block by recursively splitting the remaining space. Neither uses exceptions
 * for failure; both return the layout result.
*/
package fuookami.ospf.kotlin.framework.bpp2d.domain.block_loading.service

import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.framework.geometry2d.*
import fuookami.ospf.kotlin.framework.bpp2d.domain.block_loading.model.Space2
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.ActualItem
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.isCircleItem
import fuookami.ospf.kotlin.framework.bpp2d.infrastructure.*

/**
 * 空间切分搜索结果。 / Space-splitting search result.
 *
 * @param V 数值类型 / numeric type
 * @property placements 放置列表 / placement list
 * @property placedAmounts 各物品已放置件数 / placed amount per item
 * @property restAmounts 各物品剩余件数 / remaining amount per item
 * @property source 算法来源标记 / algorithm source marker
*/
data class SpaceSplit2Result<V : FloatingNumber<V>>(
    val placements: List<Placement2<V>>,
    val placedAmounts: Map<String, Int>,
    val restAmounts: Map<String, Int>,
    val source: String
)

/**
 * 由剩余件数推导已放置件数。 / Derive placed amounts from the remaining ones.
 *
 * @param V 数值类型 / numeric type
 * @param items 物品与件数 / items and their amounts
 * @param rest 剩余件数 / remaining amounts
 * @return 已放置件数 / placed amounts
*/
private fun <V : FloatingNumber<V>> placedAmountsOf(
    items: List<Pair<ActualItem<V>, Int>>,
    rest: Map<String, Int>
): Map<String, Int> {
    val placed = LinkedHashMap<String, Int>()
    for ((item, amount) in items) {
        val left = rest[item.id.value] ?: 0
        if (left < amount) {
            placed[item.id.value] = amount - left
        }
    }
    return placed
}

/**
 * 深度优先空间切分算法。 / Depth-first space-splitting algorithm.
 *
 * 每次取剩余区域，放入包围面积最大的可放物品，然后沿放置块的外边界切分剩余空间并递归。
 * / Each step takes the remaining region, places the largest-placeable item, then splits the remainder
 * along the block boundary and recurses.
*/
class DepthFirstSearchAlgorithm2D {

    /**
     * 执行深度优先空间切分。 / Run the depth-first space split.
     *
     * @param V 数值类型 / numeric type
     * @param space 初始区域 / initial region
     * @param items 物品与件数 / items and their amounts
     * @param maxDepth 最大递归深度 / maximum recursion depth
     * @return 搜索结果 / search result
    */
    fun <V : FloatingNumber<V>> invoke(
        space: Space2<V>,
        items: List<Pair<ActualItem<V>, Int>>,
        maxDepth: Int = 8,
        kerf: Quantity<V>? = null
    ): Ret<SpaceSplit2Result<V>> {
        val placements = ArrayList<Placement2<V>>()
        val rest = LinkedHashMap<String, Int>()
        for ((item, amount) in items) {
            rest[item.id.value] = amount
        }
        descend(
            space = space,
            items = items,
            rest = rest,
            placements = placements,
            depth = 0,
            maxDepth = maxDepth,
            kerf = kerf
        )
        return ok(
            SpaceSplit2Result(
                placements = placements,
                placedAmounts = placedAmountsOf(items, rest),
                restAmounts = rest.filterValues { it > 0 },
                source = "dfs"
            )
        )
    }

    /**
     * 递归放置。 / Recursive placement step.
     *
     * @param V 数值类型 / numeric type
     * @param space 当前区域 / current region
     * @param items 物品与件数 / items and their amounts
     * @param rest 剩余件数，原地更新 / remaining amounts, updated in place
     * @param placements 放置列表，原地追加 / placement list, appended in place
     * @param depth 当前深度 / current depth
     * @param maxDepth 最大深度 / maximum depth
    */
    private fun <V : FloatingNumber<V>> descend(
        space: Space2<V>,
        items: List<Pair<ActualItem<V>, Int>>,
        rest: MutableMap<String, Int>,
        placements: MutableList<Placement2<V>>,
        depth: Int,
        maxDepth: Int,
        kerf: Quantity<V>?
    ) {
        if (depth >= maxDepth || space.isDegenerate()) {
            return
        }
        val pick = items
            .filter { (item, _) -> (rest[item.id.value] ?: 0) > 0 && !item.isCircleItem() }
            .maxByQuantityOrNull { it.first.boundingArea }
            ?: return

        val item = pick.first
        val candidate = item.shape.candidateShapes(item.allowRotate)
            .filter { space.enabled(it.shape) }
            .maxByQuantityOrNull { it.shape.boundingWidth }
            ?: return
        val shape = candidate.shape
        // 刀缝通过节距体现。 / The kerf is expressed through the pitch.
        val spacing = if (kerf != null && quantityIsPositive(kerf)) kerf else quantityZeroOf(shape.boundingWidth)
        val pitch = pitchOf(shape, spacing)
        val cols = countFitting(space.width, pitch.x)
        val rows = countFitting(space.height, pitch.y)
        if (cols <= 0 || rows <= 0) {
            return
        }

        var remaining = rest[item.id.value] ?: 0
        var placedCols = 0
        var placedRows = 0
        loop@ for (row in 0 until rows) {
            for (col in 0 until cols) {
                if (remaining <= 0) {
                    break@loop
                }
                val x = space.x + scaledQuantity(pitch.x, col)
                val y = space.y + scaledQuantity(pitch.y, row)
                placements.add(
                    Placement2(
                        shape = shape,
                        position = QuantityPoint2(x = x, y = y),
                        rotated = candidate.rotated,
                        itemId = item.id.value
                    )
                )
                remaining -= 1
                placedCols = maxOf(placedCols, col + 1)
                placedRows = maxOf(placedRows, row + 1)
            }
        }
        rest[item.id.value] = remaining

        val usedWidth = scaledQuantity(pitch.x, placedCols)
        val usedHeight = scaledQuantity(pitch.y, placedRows)
        val (_, rightSpace) = space.splitX(usedWidth)
        val topSpace = Space2(
            x = space.x,
            y = space.y + usedHeight,
            width = usedWidth,
            height = space.height - usedHeight
        )
        descend(rightSpace, items, rest, placements, depth + 1, maxDepth, kerf)
        descend(topSpace, items, rest, placements, depth + 1, maxDepth, kerf)
    }
}

/**
 * 多行启发式搜索算法。 / Multi-row heuristic search algorithm.
 *
 * 按行推进：每行选择能放入剩余宽度中数量最多的物品，放满一行后按行高向上推进。
 * / Advances row by row: each row picks the item fitting the most copies into the remaining width, then
 * moves up by that row's height.
*/
class MultiRowHeuristicAlgorithm2D {

    /**
     * 一行的候选选择结果。 / Candidate choice for one row.
     *
     * @param V 数值类型 / numeric type
     * @property item 物品 / item
     * @property candidate 形状候选 / shape candidate
     * @property cols 该行件数 / amount in the row
    */
    private data class RowPick<V : FloatingNumber<V>>(
        val item: ActualItem<V>,
        val candidate: Shape2Candidate<V>,
        val cols: Int,
        val pitch: fuookami.ospf.kotlin.framework.geometry2d.QuantityVector2<V>
    )

    /**
     * 执行多行启发式搜索。 / Run the multi-row heuristic search.
     *
     * @param V 数值类型 / numeric type
     * @param space 初始区域 / initial region
     * @param items 物品与件数 / items and their amounts
     * @param maxRows 最大行数 / maximum row count
     * @return 搜索结果 / search result
    */
    fun <V : FloatingNumber<V>> invoke(
        space: Space2<V>,
        items: List<Pair<ActualItem<V>, Int>>,
        maxRows: Int = 32,
        kerf: Quantity<V>? = null
    ): Ret<SpaceSplit2Result<V>> {
        val placements = ArrayList<Placement2<V>>()
        val rest = LinkedHashMap<String, Int>()
        for ((item, amount) in items) {
            rest[item.id.value] = amount
        }

        var cursorY = space.y
        var rows = 0
        while (rows < maxRows) {
            val remainingHeight = (space.y + space.height) - cursorY
            if (!quantityIsPositive(remainingHeight)) {
                break
            }
            val pick = items
                .filter { (item, _) -> (rest[item.id.value] ?: 0) > 0 && !item.isCircleItem() }
                .mapNotNull { (item, _) ->
                    val candidate = item.shape.candidateShapes(item.allowRotate)
                        .filter { space.enabled(it.shape) }
                        .filter { quantityLessOrEqual(it.shape.boundingHeight, remainingHeight) }
                        .maxByQuantityOrNull { it.shape.boundingWidth }
                        ?: return@mapNotNull null
                    val spacing = if (kerf != null && quantityIsPositive(kerf)) kerf else quantityZeroOf(candidate.shape.boundingWidth)
                    val pitch = pitchOf(candidate.shape, spacing)
                    val cols = countFitting(space.width, pitch.x)
                    if (cols <= 0) null else RowPick(item, candidate, cols, pitch)
                }
                .maxByOrNull { it.cols }
                ?: break

            val shape = pick.candidate.shape
            var remaining = rest[pick.item.id.value] ?: 0
            var placed = 0
            for (col in 0 until pick.cols) {
                if (remaining <= 0) {
                    break
                }
                val x = space.x + scaledQuantity(pick.pitch.x, col)
                placements.add(
                    Placement2(
                        shape = shape,
                        position = QuantityPoint2(x = x, y = cursorY),
                        rotated = pick.candidate.rotated,
                        itemId = pick.item.id.value
                    )
                )
                remaining -= 1
                placed += 1
            }
            rest[pick.item.id.value] = remaining
            if (placed == 0) {
                break
            }
            cursorY += pick.pitch.y
            rows += 1
        }

        return ok(
            SpaceSplit2Result(
                placements = placements,
                placedAmounts = placedAmountsOf(items, rest),
                restAmounts = rest.filterValues { it > 0 },
                source = "multi-row"
            )
        )
    }
}