@file:Suppress("DEPRECATION")

/**
 * 放置类型模型。
 * Placement typing model.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.item.model

import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.bottomPlacements
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.topPlacements





fun AnyPlacement3.toItemPlacementOrNull(): ItemPlacement3? {
    val item = unit as? Item ?: return null
    val itemView = view as? ItemView ?: ItemView(item, orientation)
    return itemPlacement3Of(
        view = itemView,
        position = absolutePosition
    )
}

fun topItemPlacements(placements: List<ItemPlacement3>): List<ItemPlacement3> {
    val tops = topPlacements(placements).toSet()
    return placements.filter { tops.contains(it) }
}

fun bottomItemPlacements(placements: List<ItemPlacement3>): List<ItemPlacement3> {
    val bottoms = bottomPlacements(placements).toSet()
    return placements.filter { bottoms.contains(it) }
}
