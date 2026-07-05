package fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.model

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.PhysicalUnit
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.*

/**
 * 切割方案结构化去重键 / Structural deduplication key for a cutting plan
 *
 * @property materialId 物料标识 / Material identifier
 * @property machineId 设备标识 / Machine identifier
 * @property capacityConsumption 设备产能消耗键 / Machine capacity consumption key
 * @property slices 切片结构键 / Slice structural keys
 * @property demandContributions 需求贡献结构键 / Demand contribution structural keys
 */
data class CuttingPlanCanonicalKey(
    val materialId: MaterialId,
    val machineId: MachineId?,
    val capacityConsumption: String?,
    val slices: List<CuttingPlanSliceCanonicalKey>,
    val demandContributions: List<CuttingPlanDemandContributionCanonicalKey>
) {
    /**
     * 从自定义字符串键构造简并 canonical key / Construct a degenerate canonical key from a custom string key
     *
     * 当下游通过 Csp1dGenerationStrategy.canonicalKeyFor 提供自定义 key 时，
     * 使用此构造函数将字符串封装为 CuttingPlanCanonicalKey 以参与去重集合。
     *
     * When downstream provides a custom key via Csp1dGenerationStrategy.canonicalKeyFor,
     * this constructor wraps the string into a CuttingPlanCanonicalKey for dedup set membership.
     */
    constructor(customKey: String) : this(
        materialId = CustomKeyMaterialId(customKey),
        machineId = null,
        capacityConsumption = null,
        slices = emptyList(),
        demandContributions = emptyList()
    )
}

/**
 * 切片结构化去重键 / Structural deduplication key for a cutting plan slice
 *
 * @property productionType 产出类型 / Production type
 * @property productionId 产出标识 / Production identifier
 * @property width 宽度键 / Width key
 * @property amount 数量 / Amount
 */
data class CuttingPlanSliceCanonicalKey(
    val productionType: String,
    val productionId: ProductionId?,
    val width: String,
    val amount: UInt64
)

/**
 * 需求贡献结构化去重键 / Structural deduplication key for demand contribution
 *
 * @property productId 产品标识 / Product identifier
 * @property unit 单位键 / Unit key
 * @property quantityValue 需求贡献值 / Demand contribution value
 */
data class CuttingPlanDemandContributionCanonicalKey(
    val productId: ProductId,
    val unit: String,
    val quantityValue: String
)

/**
 * 生成切割方案的结构化去重键 / Build a structural deduplication key for a cutting plan
 *
 * @return 结构化去重键 / Structural deduplication key
 */
fun <V : RealNumber<V>> CuttingPlan<V>.canonicalKey(): CuttingPlanCanonicalKey {
    return CuttingPlanCanonicalKey(
        materialId = material.id,
        machineId = machineId,
        capacityConsumption = capacityConsumption?.canonicalQuantityKey(),
        slices = slices.canonicalSliceKeys(),
        demandContributions = demandContributions.canonicalDemandContributionKeys()
    )
}

/**
 * 按结构化去重键保留首个切割方案 / Keep the first cutting plan for each structural deduplication key
 *
 * @return 去重后的切割方案列表 / Deduplicated cutting plans
 */
fun <V : RealNumber<V>> Iterable<CuttingPlan<V>>.distinctByCanonicalKey(): List<CuttingPlan<V>> {
    val seen = HashSet<CuttingPlanCanonicalKey>()
    val plans = ArrayList<CuttingPlan<V>>()
    for (plan in this) {
        if (seen.add(plan.canonicalKey())) {
            plans.add(plan)
        }
    }
    return plans
}

private data class SliceGroupKey(
    val productionType: String,
    val productionId: ProductionId?,
    val width: String
)

private data class DemandContributionGroupKey(
    val productId: ProductId,
    val unit: String
)

/**
 * 自定义字符串 canonical key 的 MaterialId 哨兵实现 /
 * MaterialId sentinel for degenerate canonical keys built from custom string keys
 */
private data class CustomKeyMaterialId(val value: String) : MaterialId {
    override fun toString(): String = value
}

private fun <V : RealNumber<V>> List<CuttingPlanSlice<V>>.canonicalSliceKeys(): List<CuttingPlanSliceCanonicalKey> {
    val grouped = LinkedHashMap<SliceGroupKey, UInt64>()
    for (slice in this) {
        val key = SliceGroupKey(
            productionType = slice.production.canonicalProductionType(),
            productionId = slice.production.id,
            width = slice.width.canonicalQuantityKey()
        )
        grouped[key] = grouped[key]?.let { it + slice.amount } ?: slice.amount
    }
    return grouped.map { (key, amount) ->
        CuttingPlanSliceCanonicalKey(
            productionType = key.productionType,
            productionId = key.productionId,
            width = key.width,
            amount = amount
        )
    }.sortedWith(
        compareBy<CuttingPlanSliceCanonicalKey> { it.productionType }
            .thenBy { it.productionId?.toString() }
            .thenBy { it.width }
            .thenBy { it.amount.toString() }
    )
}

private fun <V : RealNumber<V>> List<CuttingPlanDemandContribution<V>>.canonicalDemandContributionKeys():
    List<CuttingPlanDemandContributionCanonicalKey> {
    val grouped = LinkedHashMap<DemandContributionGroupKey, V>()
    for (contribution in this) {
        val key = DemandContributionGroupKey(
            productId = contribution.product.id,
            unit = contribution.quantity.unit.canonicalUnitKey()
        )
        grouped[key] = grouped[key]?.let { it + contribution.quantity.value } ?: contribution.quantity.value
    }
    return grouped.map { (key, value) ->
        CuttingPlanDemandContributionCanonicalKey(
            productId = key.productId,
            unit = key.unit,
            quantityValue = value.toString()
        )
    }.sortedWith(
        compareBy<CuttingPlanDemandContributionCanonicalKey> { it.productId.toString() }
            .thenBy { it.unit }
            .thenBy { it.quantityValue }
    )
}

private fun <V : RealNumber<V>> Quantity<V>.canonicalQuantityKey(): String {
    return "${value}:${unit.canonicalUnitKey()}"
}

private fun PhysicalUnit.canonicalUnitKey(): String {
    return symbol ?: name ?: toString()
}

/**
 * 生成产出的规范类型键 / Generate canonical production type key
 *
 * @return 产出类型的字符串标识 / String identifier for production type
 */
private fun <V : RealNumber<V>> Production<V>.canonicalProductionType(): String {
    return when (this) {
        is Product<*> -> "product"
        is Costar<*> -> "costar"
        is Material<*> -> "material"
        else -> javaClass.name
    }
}
