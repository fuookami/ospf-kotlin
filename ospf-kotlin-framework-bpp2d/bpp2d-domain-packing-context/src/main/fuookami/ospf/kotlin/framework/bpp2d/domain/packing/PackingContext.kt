/**
 * 二维装箱上下文。 / 2D packing context.
 *
 * 对应 BPP3D 的 `PackingContext`：描述装箱过程之外仍然剩余的物品与物料需求，以及调用方附加的键值信息。
 * / Mirrors BPP3D `PackingContext`: describes the item and material demands still remaining outside the
 * packing process, plus caller-provided key-value information.
*/
package fuookami.ospf.kotlin.framework.bpp2d.domain.packing

import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.*

/**
 * 装箱上下文。 / Packing context.
 *
 * @property restItems 剩余未装箱物品及其件数 / remaining unpacked items with their amounts
 * @property restMaterials 剩余未分配物料及其件数 / remaining unassigned materials with their amounts
 * @property info 附加键值信息 / additional key-value information
*/
data class PackingContext(
    val restItems: Map<ItemId, UInt64> = emptyMap(),
    val restMaterials: Map<MaterialKey, UInt64> = emptyMap(),
    val info: Map<String, String> = emptyMap()
)
