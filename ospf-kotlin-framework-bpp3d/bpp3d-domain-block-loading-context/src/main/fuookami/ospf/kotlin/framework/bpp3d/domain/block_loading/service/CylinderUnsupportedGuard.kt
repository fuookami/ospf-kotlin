/**
 * 圆柱未开放路径门禁。
 * Cylinder unsupported-path guards.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.block_loading.service

import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.*

/**
 * 要求 DFS/MLHS 仅长方体搜索路径不接收圆柱。
 * Require DFS/MLHS cuboid-only search paths to reject cylinders.
 */
internal fun requireNoCylinderItemsForCuboidSearch(
    items: Map<Item, UInt64>
) {
    requireNoCylinderItemsForCuboidOnlyPath(
        items = items
            .filter { (_, amount) -> amount != UInt64.zero }
            .keys,
        path = CylinderCapabilityPath.DfsMlhsCuboidSearch
    )!!
}
