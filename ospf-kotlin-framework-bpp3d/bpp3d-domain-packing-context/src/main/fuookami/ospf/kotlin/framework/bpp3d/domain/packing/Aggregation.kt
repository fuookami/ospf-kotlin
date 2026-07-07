/**
 * Packing aggregation.
 * 装箱聚合。
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.packing

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.*

/**
 * A packed item with its placement and loading order.
 * 已装箱物品，包含放置信息和装载顺序。
 *
 * @property placement The quantity placement of the item in 3D space.
 * 物品在三维空间中的数量放置。
 * @property loadingOrder The loading order of the item within the bin.
 * 物品在箱内的装载顺序。
 */
data class PackedItem(
    val placement: QuantityPlacement3<Item, FltX>,
    val loadingOrder: UInt64
)

/**
 * A packed bin containing a list of packed items.
 * 已装箱的容器，包含已装箱物品列表。
 *
 * @property name The name of the packed bin.
 * 已装箱容器的名称。
 * @property type The bin type with dimensional information.
 * 容器的类型，包含尺寸信息。
 * @property batchNo The batch number of the bin, if applicable.
 * 容器的批次号，可选。
 * @property group The group labels for the bin.
 * 容器的分组标签列表。
 * @property items The list of packed items in this bin.
 * 该容器内已装箱物品的列表。
 */
data class PackedBin(
    val name: String,
    val type: BinType<FltX>,
    val batchNo: BatchNo? = null,
    val group: List<String> = emptyList(),
    val items: List<PackedItem>
)

/**
 * Aggregation of all packed bins in a packing result.
 * 装箱结果中所有已装箱容器的聚合。
 *
 * @property bins The list of packed bins.
 * 已装箱容器列表。
 */
data class PackingAggregation(
    val bins: List<PackedBin>
)

/**
 * Summary of material usage with the total amount.
 * 物料使用汇总，包含物料键和总数量。
 *
 * @property material The material key identifying the material.
 * 标识物料的物料键。
 * @property amount The total amount of the material used.
 * 使用的物料总数量。
 */
data class MaterialSummary(
    val material: MaterialKey,
    val amount: UInt64
)

/**
 * The final packing result containing aggregation, material summary, and additional info.
 * 最终装箱结果，包含聚合、物料汇总和附加信息。
 *
 * @property aggregation The packing aggregation of all bins.
 * 所有容器的装箱聚合。
 * @property materialSummary The summary of material usage.
 * 物料使用汇总列表。
 * @property info Additional key-value information about the packing result.
 * 关于装箱结果的附加键值信息。
 */
data class PackingResult(
    val aggregation: PackingAggregation,
    val materialSummary: List<MaterialSummary> = emptyList(),
    val info: Map<String, String> = emptyMap()
)
