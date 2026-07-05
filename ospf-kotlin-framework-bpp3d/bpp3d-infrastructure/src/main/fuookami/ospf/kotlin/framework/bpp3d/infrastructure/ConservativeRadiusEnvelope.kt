package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.*

/** 保守半径包络，用于计算三维装箱问题中物品与箱子之间的保守半径约束 / Conservative radius envelope for computing conservative radius constraints between items and boxes in 3D bin packing problems. */
interface ConservativeRadiusEnvelope<Box : Box3D, Item : Item3D> : RadiusEnvelope<Box, Item> {
    /**
     * 计算物品相对于箱子的保守半径包络。
     * Compute the conservative radius envelope of an item relative to a box.
     * @param box 目标箱子 / the target box
     * @param item 目标物品 / the target item
     * @return 保守半径包络结果 / the conservative radius envelope result
     */
    fun conservativeRadiusEnvelope(
        box: Box,
        item: Item
    ): Result<Pair<Flt, Flt>>
}

/**
 * 获取物品相对于箱子的保守半径。
 * Get the conservative radius of an item relative to a box.
 * @param box 目标箱子 / the target box
 * @param item 目标物品 / the target item
 * @param block 半径配置块 / the radius configuration block
 * @return 保守半径结果 / the conservative radius result
 */
fun <Box, Item, R> ConservativeRadiusEnvelope<Box, Item>.conservativeRadius(
    box: Box,
    item: Item,
    block: ConservativeRadiusEnvelope<Box, Item>.() -> R
): Result<R> where Box : Box3D, Item : Item3D {
    return this.conservativeRadius(box, item, block)
}
