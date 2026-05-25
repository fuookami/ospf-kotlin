@file:Suppress("DEPRECATION")

package fuookami.ospf.kotlin.framework.bpp3d.domain.item.model

import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Placement3
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.bottomPlacements
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.topPlacements

fun Placement3<*>.toItemPlacementOrNull(): ItemPlacement3? {
    val item = unit as? Item ?: return null
    val itemView = view as? ItemView ?: ItemView(item, orientation)
    return Placement3(itemView, absolutePosition)
}

fun topItemPlacements(placements: List<ItemPlacement3>): List<ItemPlacement3> {
    val tops = topPlacements(placements).toSet()
    return placements.filter { tops.contains(it) }
}

fun bottomItemPlacements(placements: List<ItemPlacement3>): List<ItemPlacement3> {
    val bottoms = bottomPlacements(placements).toSet()
    return placements.filter { bottoms.contains(it) }
}
