@file:Suppress("DEPRECATION")

package fuookami.ospf.kotlin.framework.bpp3d.domain.packing

import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BinType
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.ItemPlacement3
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.MaterialKey
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.BatchNo
import fuookami.ospf.kotlin.math.algebra.number.UInt64

data class PackedItem(
    val placement: ItemPlacement3,
    val loadingOrder: UInt64
)

data class PackedBin(
    val name: String,
    val type: BinType,
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
