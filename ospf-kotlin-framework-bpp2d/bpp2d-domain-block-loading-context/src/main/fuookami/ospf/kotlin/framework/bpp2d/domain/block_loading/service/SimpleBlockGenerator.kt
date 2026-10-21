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
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.framework.geometry2d.*
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
        config: SimpleBlockConfig = SimpleBlockConfig(),
        kerf: Quantity<V>? = null
    ): Ret<List<Block2<V>>> {
        if (space.isDegenerate() || item.isCircleItem()) {
            return ok(emptyList())
        }
        val blocks = ArrayList<Block2<V>>()
        // 旋转候选同时受配置与物品自身许可约束：只检查 config 会让未允许旋转的物品产出旋转块，
        // 与 requireRotationAllowed 的守卫语义自相矛盾。
        // Rotated candidates require both the config and the item's own allowance: checking only the config
        // would let an item that disallows rotation yield rotated blocks, contradicting requireRotationAllowed.
        for (candidate in item.shape.candidateShapes(config.withRotation && item.allowRotate)) {
            val shape = candidate.shape
            // 刀缝通过节距体现：相邻件按「包围尺寸 + kerf」推进。
            // The kerf is expressed through the pitch: neighbours advance by bounding size plus kerf.
            val spacing = if (kerf != null && quantityIsPositive(kerf)) kerf else quantityZeroOf(shape.boundingWidth)
            val pitch = pitchOf(shape, spacing)
            val cols = countFitting(space.width, pitch.x)
            val rows = countFitting(space.height, pitch.y)
            if (cols <= 0 || rows <= 0) {
                continue
            }
            val blockWidth = scaledQuantity(pitch.x, cols)
            val blockHeight = scaledQuantity(pitch.y, rows)
            val placements = ArrayList<Placement2<V>>(cols * rows)
            for (row in 0 until rows) {
                val y = space.y + scaledQuantity(pitch.y, row)
                for (col in 0 until cols) {
                    val x = space.x + scaledQuantity(pitch.x, col)
                    placements.add(
                        Placement2(
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
                when (val remainder = invoke(item, remainingRight, config, kerf)) {
                    is Ok -> blocks.addAll(remainder.value)
                    is Failed -> return Failed(remainder.error)
                    is Fatal -> return Fatal(remainder.errors)
                }
            }
        }
        return ok(blocks)
    }
}