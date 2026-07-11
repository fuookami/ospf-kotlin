/**
 * 圆柱未开放路径门禁。
 * Cylinder unsupported-path guards.
*/
package fuookami.ospf.kotlin.framework.bpp3d.domain.block_loading.service

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.*

/**
 * 要求 DFS/MLHS 仅长方体搜索路径不接收圆柱。
 * Require DFS/MLHS cuboid-only search paths to reject cylinders.
 *
 * @param items 待检查的物品及其数量映射，数量为零的物品将被过滤。
 *              Item-to-quantity map to check; items with zero quantity are filtered out.
 * @return 校验通过返回成功，否则返回包含错误信息的失败。
 *         Success if validation passes, otherwise a failure with error details.
*/
internal fun requireNoCylinderItemsForCuboidSearch(
    items: Map<Item, UInt64>
): Try {
    return requireNoCylinderItemsForCuboidOnlyPath(
        items = items
            .filter { (_, amount) -> amount != UInt64.zero }
            .keys,
        path = CylinderCapabilityPath.DfsMlhsCuboidSearch
    )
}
