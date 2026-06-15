/**
 * 放置类型模型。
 * Placement typing model.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.item.model

import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*

fun QuantityPlacement3<*, FltX>.toItemPlacementOrNull(): QuantityPlacement3<Item, FltX>? {
    val item = unit as? Item ?: return null
    val itemView = view as? ItemView ?: ItemView(item, orientation)
    return itemPlacement3Of(
        view = itemView,
        position = absolutePosition
    )
}

fun topItemPlacements(placements: List<QuantityPlacement3<Item, FltX>>): List<QuantityPlacement3<Item, FltX>> {
    val tops = topPlacements(placements).toSet()
    return placements.filter { tops.contains(it) }
}

fun bottomItemPlacements(placements: List<QuantityPlacement3<Item, FltX>>): List<QuantityPlacement3<Item, FltX>> {
    val bottoms = bottomPlacements(placements).toSet()
    return placements.filter { bottoms.contains(it) }
}
