/**
 * 简单块生成器。 / Simple block generator.
 *
 * 与 BPP3D 的 `SimpleBlockGenerator` 对应：把同一种物品按「整行重复」的方式铺满一个矩形区域，形成
 * 规则块。二维侧的块是矩形条带，可直接作为候选层的基础。
 * / Mirrors BPP3D `SimpleBlockGenerator`: fills a rectangular region with repeated whole rows of a single
 * item, forming a regular block. In 2D a block is a rectangular strip usable as the basis of a candidate
 * layer.
*/
package fuookami.ospf.kotlin.framework.bpp2d.domain.block_loading.service

import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.framework.bpp2d.domain.block_loading.model.Block2
import fuookami.ospf.kotlin.framework.bpp2d.domain.block_loading.model.Space2
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.AbstractItem
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.isCircleItem
import fuookami.ospf.kotlin.framework.bpp2d.infrastructure.*

/**
 * 简单块生成器配置。 / Simple block generator configuration.
 *
 * @property withRotation 是否同时生成旋转排列 / whether rotated layouts are also generated
 * @property withRemainder 是否在剩余空间继续生成块 / whether blocks continue to be generated in the remainder
*/
data class SimpleBlockConfig(
    val withRotation: Boolean = true,
    val withRemainder: Boolean = false
)

/**
 * 简单块生成器。 / Simple block generator.
*/
class SimpleBlockGenerator {

    /**
     * 为单个物品在给定区域生成规则块。 / Generate regular blocks for a single item inside the region.
     *
     * @param V 数值类型 / numeric type
     * @param item 物品 / item
     * @param space 目标区域 / target region
     * @param config 配置 / configuration
     * @return 生成的块列表 / generated blocks
    */
    fun <V : FloatingNumber<V>> invoke(
        item: AbstractItem<V>,
        space: Space2<V>,
        config: SimpleBlockConfig = SimpleBlockConfig()
    ): Ret<List<Block2<V>>> {
        if (space.isDegenerate() || item.isCircleItem()) {
            return ok(emptyList())
        }
        val blocks = ArrayList<Block2<V>>()
        for (candidate in item.shape.candidateShapes(config.withRotation)) {
            val shape = candidate.shape
            val cols = countFitting(space.width, shape.boundingWidth)
            val rows = countFitting(space.height, shape.boundingHeight)
            if (cols <= 0 || rows <= 0) {
                continue
            }
            val blockWidth = scaledQuantity(shape.boundingWidth, cols)
            val blockHeight = scaledQuantity(shape.boundingHeight, rows)
            val placements = ArrayList<LayerPlacement2<V>>(cols * rows)
            for (row in 0 until rows) {
                val y = space.y + scaledQuantity(shape.boundingHeight, row)
                for (col in 0 until cols) {
                    val x = space.x + scaledQuantity(shape.boundingWidth, col)
                    placements.add(
                        LayerPlacement2(
                            shape = shape,
                            position = QuantityPoint2(x = x, y = y),
                            rotated = candidate.rotated,
                            itemId = item.id.value
                        )
                    )
                }
            }
            blocks.add(
                Block2(
                    space = Space2(
                        x = space.x,
                        y = space.y,
                        width = blockWidth,
                        height = blockHeight
                    ),
                    placements = placements,
                    source = if (candidate.rotated) "simple-block-rotated" else "simple-block"
                )
            )
            if (!config.withRemainder) {
                continue
            }
            val (_, remainingRight) = space.splitX(blockWidth)
            if (!remainingRight.isDegenerate()) {
                when (val remainder = invoke(item, remainingRight, config)) {
                    is Ok -> blocks.addAll(remainder.value)
                    is Failed -> return Failed(remainder.error)
                    is Fatal -> return Fatal(remainder.errors)
                }
            }
        }
        return ok(blocks)
    }
}
