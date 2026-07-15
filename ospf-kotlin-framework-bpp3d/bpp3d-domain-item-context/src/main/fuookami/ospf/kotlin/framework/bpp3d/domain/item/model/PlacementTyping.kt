/**
 * Placement typing model.
 * 放置类型模型。
*/
package fuookami.ospf.kotlin.framework.bpp3d.domain.item.model

import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*
/**
 * Converts a polymorphic 3D placement to an item-typed placement, or null if the unit is not an Item.
 * 将多态三维放置转换为货物类型放置，若单元不是货物则返回 null。
 *
 * @return item-typed 3D placement, or null if the underlying unit is not an Item / 货物类型的三维放置，若底层单元不是货物则返回 null
*/
fun QuantityPlacement3<*, FltX>.toItemPlacementOrNull(): QuantityPlacement3<Item, FltX>? {
    val item = unit as? Item ?: return null
    val itemView = view as? ItemView ?: ItemView(item, orientation)
    return itemPlacement3Of(
        view = itemView,
        position = absolutePosition
    )
}

/**
 * Converts to pItemPlacements.
 * 转换为pItemPlacements。
 * @param placements item placements to filter / 待筛选的货物放置列表
 * @return item placements that are on the top surface / 位于顶面的货物放置列表
*/
fun topItemPlacements(placements: List<QuantityPlacement3<Item, FltX>>): List<QuantityPlacement3<Item, FltX>> {
    val tops = topPlacements(placements).toSet()
    return placements.filter { tops.contains(it) }
}

/**
 * bottomItemPlacements.
 * bottomItemPlacements。
 * @param placements item placements to filter / 待筛选的货物放置列表
 * @return item placements that are on the bottom surface / 位于底面的货物放置列表
*/
fun bottomItemPlacements(placements: List<QuantityPlacement3<Item, FltX>>): List<QuantityPlacement3<Item, FltX>> {
    val bottoms = bottomPlacements(placements).toSet()
    return placements.filter { bottoms.contains(it) }
}
