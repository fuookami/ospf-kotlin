@file:Suppress("DEPRECATION")

package fuookami.ospf.kotlin.framework.bpp3d.domain.block_loading.service

import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Item
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.requireNoCylinderItemsForCuboidOnlyPath
import fuookami.ospf.kotlin.math.algebra.number.UInt64

internal fun requireNoCylinderItemsForCuboidSearch(
    items: Map<Item, UInt64>,
    source: String
) {
    requireNoCylinderItemsForCuboidOnlyPath(
        items = items
            .filter { (_, amount) -> amount != UInt64.zero }
            .keys,
        source = source,
        pathPredicate = "DFS/MLHS space-splitting path is"
    )
}
