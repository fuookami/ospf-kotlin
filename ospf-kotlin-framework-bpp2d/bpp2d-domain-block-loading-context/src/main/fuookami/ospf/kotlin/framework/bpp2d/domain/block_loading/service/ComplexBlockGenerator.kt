/**
 * 复杂块生成器。 / Complex block generator.
 *
 * 与 BPP3D 的 `ComplexBlockGenerator` 对应：把尺寸不同的多种物品按行拼接为异构块，块内每行使用同一
 * 物品，行高取该行物品的包围高度。二维侧不做支撑校验（平面排样无堆叠语义）。
 * / Mirrors BPP3D `ComplexBlockGenerator`: stitches different items into a heterogeneous block row by row,
 * each row using one item and taking that item's bounding height. 2D needs no support validation because
 * plane layout has no stacking semantics.
*/
package fuookami.ospf.kotlin.framework.bpp2d.domain.block_loading.service

import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.framework.bpp2d.domain.block_loading.model.Block2
import fuookami.ospf.kotlin.framework.bpp2d.domain.block_loading.model.Space2
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.AbstractItem
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.isCircleItem
import fuookami.ospf.kotlin.framework.bpp2d.infrastructure.*

/**
 * 复杂块生成器。 / Complex block generator.
*/
class ComplexBlockGenerator {

    /**
     * 按给定物品顺序逐行拼接异构块。 / Stitch a heterogeneous block row by row in the given item order.
     *
     * @param V 数值类型 / numeric type
     * @param items 按优先级排序的物品 / items ordered by priority
     * @param space 目标区域 / target region
     * @param withRotation 是否允许旋转候选 / whether rotated candidates are allowed
     * @return 生成的块列表 / generated blocks
    */
    fun <V : FloatingNumber<V>> invoke(
        items: List<AbstractItem<V>>,
        space: Space2<V>,
        withRotation: Boolean = true
    ): Ret<List<Block2<V>>> {
        if (space.isDegenerate() || items.isEmpty()) {
            return ok(emptyList())
        }
        val placements = ArrayList<LayerPlacement2<V>>()
        val sources = LinkedHashSet<String>()
        var cursorY = space.y
        var usedWidth = quantityZeroOf(space.width)

        for (item in items) {
            if (item.isCircleItem()) {
                continue
            }
            val remainingHeight = (space.y + space.height) - cursorY
            if (!quantityIsPositive(remainingHeight)) {
                break
            }
            val candidate = item.shape.candidateShapes(withRotation)
                .filter { space.enabled(it.shape) }
                .filter { quantityLessOrEqual(it.shape.boundingHeight, remainingHeight) }
                .maxByQuantityOrNull { it.shape.boundingWidth }
                ?: continue

            val shape = candidate.shape
            val cols = countFitting(space.width, shape.boundingWidth)
            if (cols <= 0) {
                continue
            }
            val rowWidth = scaledQuantity(shape.boundingWidth, cols)
            for (col in 0 until cols) {
                val x = space.x + scaledQuantity(shape.boundingWidth, col)
                placements.add(
                    LayerPlacement2(
                        shape = shape,
                        position = QuantityPoint2(x = x, y = cursorY),
                        rotated = candidate.rotated,
                        itemId = item.id.value
                    )
                )
            }
            usedWidth = quantityMaxOf(usedWidth, rowWidth)
            sources.add(if (candidate.rotated) "complex-block-rotated" else "complex-block")
            cursorY += shape.boundingHeight
        }

        if (placements.isEmpty()) {
            return ok(emptyList())
        }
        val block = Block2(
            space = Space2(
                x = space.x,
                y = space.y,
                width = usedWidth,
                height = cursorY - space.y
            ),
            placements = placements,
            source = sources.joinToString(separator = "+")
        )
        return ok(listOf(block))
    }
}
