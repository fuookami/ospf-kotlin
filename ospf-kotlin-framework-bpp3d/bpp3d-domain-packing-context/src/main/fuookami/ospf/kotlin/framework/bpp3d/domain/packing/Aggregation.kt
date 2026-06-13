@file:Suppress("DEPRECATION")
/**
 * 装箱聚合。
 * Packing aggregation.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.packing

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.*

data class PackedItem(
    val placement: QuantityPlacement3<Item, FltX>,
    val loadingOrder: UInt64
)

data class PackedBin(
    val name: String,
    val type: BinType<FltX>,
    val batchNo: BatchNo? = null,
    val group: List<String> = emptyList(),
    val items: List<PackedItem>
)

data class PackingAggregation(
    val bins: List<PackedBin>
)

data class MaterialSummary(
    val material: MaterialKey,
    val amount: UInt64
)

data class PackingResult(
    val aggregation: PackingAggregation,
    val materialSummary: List<MaterialSummary> = emptyList(),
    val info: Map<String, String> = emptyMap()
)
