package fuookami.ospf.kotlin.framework.bpp1d.domain.item.model

import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.quantities.quantity.*

/** 一维物品。 / One-dimensional item.
 * @property length 单件长度 / Piece length
 * @property weight 单件重量 / Piece weight
 * @property amount 精确需求件数 / Exact demand
 * @property materials 单件物料贡献 / Per-piece material contributions
 */
data class Item<V : FloatingNumber<V>>(
    val id: String,
    val length: Quantity<V>,
    val weight: Quantity<V>,
    val amount: UInt64,
    val materials: Map<String, UInt64> = emptyMap()
)

/** 箱型。 / Bin type.
 * @property length 容量长度 / Length capacity
 * @property maxWeight 承重 / Weight capacity
 * @property cost 无量纲成本 / Dimensionless cost
 * @property inventory 可用箱数，null 表示不限 / Available count, null for unlimited
 */
data class BinType<V : FloatingNumber<V>>(
    val id: String,
    val length: Quantity<V>,
    val maxWeight: Quantity<V>,
    val cost: V,
    val inventory: UInt64? = null
)

/** 实例化箱体。 / Instantiated bin.
 * @property type 箱型 / Bin type
 */
data class Bin<V : FloatingNumber<V>>(val id: String, val type: BinType<V>)

/** 一维放置。 / One-dimensional placement.
 * @property itemId 物品身份 / Item identity
 * @property start 起点 / Start
 * @property length 长度 / Length
 */
data class Placement<V : FloatingNumber<V>>(
    val itemId: String,
    val start: Quantity<V>,
    val length: Quantity<V>
)

/** 组合列，以箱型和逐物品件数去重。 / Pattern column, deduplicated by bin type and item counts.
 * @property binTypeId 箱型 / Bin type
 * @property amounts 物品件数 / Item counts
 */
data class PackingPattern<V : FloatingNumber<V>>(
    val binTypeId: String,
    val amounts: Map<String, UInt64>
) {
    /** 稳定组合键。 / Stable composition key. */
    val key: String get() = "${binTypeId.length}:$binTypeId|" + amounts.entries.sortedBy { it.key }.joinToString(";") { "${it.key.length}:${it.key}=${it.value}" }
}

/** 箱内完整放置。 / Complete bin placement.
 * @property bin 箱体 / Bin
 * @property placements 单件放置 / Piece placements
 */
data class PackingPlan<V : FloatingNumber<V>>(val bin: Bin<V>, val placements: List<Placement<V>>)

/** 装箱目标。 / Packing objective. */
enum class PackingObjective { BinCount, ConsumedLength, Cost }

/** 一维装箱输入。 / One-dimensional packing input.
 * @property items 物品精确需求 / Exact item demands
 * @property binTypes 箱型与库存 / Bin types and inventory
 * @property spacing 相邻物品间距，无额外尾部间距 / Inter-piece spacing, no trailing spacing
 * @property objective 目标口径 / Objective policy
 */
data class PackingProblem<V : FloatingNumber<V>>(
    val items: List<Item<V>>,
    val binTypes: List<BinType<V>>,
    val spacing: Quantity<V>,
    val objective: PackingObjective = PackingObjective.BinCount
) {
    /** 验证量纲、有限值、身份和容量。 / Validate dimensions, finite values, identities and capacities.
     * @return 操作结果 / Operation result
     */
    fun validate(): Try {
        fun invalid(message: String): Try = Failed(ErrorCode.IllegalArgument, "一维装箱输入无效 / Invalid BPP1D input: $message")
        if (items.map { it.id }.distinct().size != items.size || items.any { it.id.isBlank() }) return invalid("item IDs")
        if (binTypes.isEmpty() || binTypes.map { it.id }.distinct().size != binTypes.size || binTypes.any { it.id.isBlank() }) return invalid("bin types")
        val gap = spacing.convertTo(Meter) ?: return invalid("spacing unit")
        if (!gap.value.isFinite() || gap.value < gap.value.constants.zero) return invalid("spacing")
        for (item in items) {
            val length = item.length.convertTo(Meter) ?: return invalid("length unit ${item.id}")
            val weight = item.weight.convertTo(Kilogram) ?: return invalid("weight unit ${item.id}")
            if (!length.value.isFinite() || length.value <= length.value.constants.zero || !weight.value.isFinite() || weight.value < weight.value.constants.zero) return invalid("item ${item.id}")
        }
        for (bin in binTypes) {
            val length = bin.length.convertTo(Meter) ?: return invalid("length unit ${bin.id}")
            val weight = bin.maxWeight.convertTo(Kilogram) ?: return invalid("weight unit ${bin.id}")
            if (!length.value.isFinite() || length.value <= length.value.constants.zero || !weight.value.isFinite() || weight.value < weight.value.constants.zero || !bin.cost.isFinite() || bin.cost <= bin.cost.constants.zero) return invalid("bin ${bin.id}")
        }
        return ok
    }

    /** 验证外部或定价产生的组合列。 / Validate an externally supplied or priced pattern.
     * @param pattern 待验证列 / Pattern to validate
     * @return 操作结果 / Operation result
     */
    fun validatePattern(pattern: PackingPattern<V>): Try {
        val validation = validate()
        if (validation !is Ok) return validation
        val bin = binTypes.find { it.id == pattern.binTypeId }
            ?: return Failed(ErrorCode.IllegalArgument, "未知箱型 / Unknown bin type")
        var length = bin.length.value.constants.zero
        var weight = bin.maxWeight.value.constants.zero
        var nonempty = false
        val gap = spacing.convertTo(bin.length.unit)!!.value
        for ((id, amount) in pattern.amounts) {
            val item = items.find { it.id == id }
                ?: return Failed(ErrorCode.IllegalArgument, "未知物品 / Unknown item: $id")
            if (amount == UInt64.zero || amount > item.amount) return Failed(ErrorCode.IllegalArgument, "方案件数无效 / Invalid pattern count")
            // 二进制倍增在领域数值内缩放，不逐件遍历大件数。 / Binary scaling stays in the domain numeric type.
            fun scale(value: V, count: UInt64): V {
                var factor = value
                var remaining = count.toULong()
                var result = value.constants.zero
                while (remaining > 0uL) {
                    if (remaining and 1uL != 0uL) result += factor
                    remaining = remaining shr 1
                    if (remaining > 0uL) factor += factor
                }
                return result
            }
            length += scale(item.length.convertTo(bin.length.unit)!!.value, amount)
            length += scale(gap, if (nonempty) amount else amount - UInt64.one)
            weight += scale(item.weight.convertTo(bin.maxWeight.unit)!!.value, amount)
            nonempty = true
        }
        return if (!nonempty || !length.isFinite() || !weight.isFinite() || length > bin.length.value || weight > bin.maxWeight.value) {
            Failed(ErrorCode.IllegalArgument, "方案超过箱体容量 / Pattern exceeds bin capacity")
        } else ok
    }
}
