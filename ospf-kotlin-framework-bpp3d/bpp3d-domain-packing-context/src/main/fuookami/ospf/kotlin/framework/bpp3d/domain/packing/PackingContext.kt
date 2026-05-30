@file:Suppress("DEPRECATION")

/**
 * 装箱上下文。
 * Packing context.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.packing

import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Item
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.MaterialKey
import fuookami.ospf.kotlin.math.algebra.number.UInt64

data class PackingContext(
    val restItems: Map<Item, UInt64> = emptyMap(),
    val restMaterials: Map<MaterialKey, UInt64> = emptyMap(),
    val info: Map<String, String> = emptyMap()
)
