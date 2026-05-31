@file:Suppress("DEPRECATION")

package fuookami.ospf.kotlin.framework.bpp3d.domain.block_loading.service

import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Item
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.CylinderPackingShape3
import fuookami.ospf.kotlin.math.algebra.number.UInt64

internal fun requireNoCylinderItemsForCuboidSearch(
    items: Map<Item, UInt64>,
    source: String
) {
    val unsupportedCylinder = items.entries.firstOrNull { (item, amount) ->
        amount != UInt64.zero && item.packingShape is CylinderPackingShape3
    }?.key
    if (unsupportedCylinder != null) {
        throw IllegalArgumentException(
            "Unsupported cylinder in $source: DFS/MLHS space-splitting path is cuboid-only and does not provide verified cylinder geometry yet."
        )
    }
}
