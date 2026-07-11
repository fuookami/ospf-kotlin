/**
 * Packing context.
 * 装箱上下文。
*/
package fuookami.ospf.kotlin.framework.bpp3d.domain.packing

import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.*

/**
 * Context information for the packing process, including remaining items, materials, and additional info.
 * 装箱过程的上下文信息，包括剩余物品、剩余物料和附加信息。
 *
 * @property restItems The remaining unpacked items and their quantities.
 * 剩余未装箱物品及其数量。
 * @property restMaterials The remaining unassigned materials and their quantities.
 * 剩余未分配物料及其数量。
 * @property info Additional key-value context information.
 * 附加的键值上下文信息。
*/
data class PackingContext(
    val restItems: Map<Item, UInt64> = emptyMap(),
    val restMaterials: Map<MaterialKey, UInt64> = emptyMap(),
    val info: Map<String, String> = emptyMap()
)
