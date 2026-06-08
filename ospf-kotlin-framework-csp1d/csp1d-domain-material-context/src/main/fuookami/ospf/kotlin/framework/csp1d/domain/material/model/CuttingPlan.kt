package fuookami.ospf.kotlin.framework.csp1d.domain.material.model

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity

/**
 * 切割方案切片，描述单次切割中的产出对象及其幅宽 / Cutting plan slice describing production target and width in one cut
 *
 * @param V 数值类型 / Numeric value type
 * @property production 产出对象（产品或配规）/ Production target (product or costar)
 * @property width 幅宽 / Width
 * @property amount 份数 / Piece amount
 */
data class CuttingPlanSlice<V : RealNumber<V>>(
    val production: Production<V>,
    val width: Quantity<V>,
    val amount: UInt64 = UInt64.one
)

/**
 * 切割方案 / Cutting plan
 *
 * @param V 数值类型 / Numeric value type
 * @property id 方案标识 / Plan identifier
 * @property material 物料 / Material
 * @property machineId 设备标识 / Machine identifier
 * @property slices 切片列表 / Cut slices
 * @property demandContributions 需求贡献 / Demand contributions
 * @property arithmetic 物理量算术策略，为空时按物料值自动解析 / Quantity arithmetic strategy, auto-resolved from material value when null
 * @property capacityConsumption 单次方案使用的设备产能消耗，必须与设备产能同单位才进入主问题约束 / Machine capacity consumed by one plan usage, modeled only when unit matches machine capacity
 */
data class CuttingPlan<V : RealNumber<V>>(
    val id: String,
    val material: Material<V>,
    val machineId: String? = material.machineId,
    val slices: List<CuttingPlanSlice<V>>,
    val demandContributions: List<CuttingPlanDemandContribution<V>> = emptyList(),
    val arithmetic: QuantityArithmetic<V>? = null,
    val capacityConsumption: Quantity<V>? = null
) {
    private val resolvedArithmetic: QuantityArithmetic<V> by lazy {
        arithmetic ?: DefaultQuantityArithmetic.resolveFor(material.widthRange.upperBound.value)
    }

    /**
     * 已使用幅宽 / Used width
     */
    val usedWidth: Quantity<V>? by lazy {
        val arith = resolvedArithmetic
        slices.fold<CuttingPlanSlice<V>, Quantity<V>?>(null) { acc, slice ->
            val sliceWidth = slice.width.repeat(slice.amount, arith)
            if (acc == null) {
                sliceWidth
            } else {
                arith.add(acc, sliceWidth)
            }
        }
    }

    /**
     * 剩余幅宽 / Remaining width
     */
    val restWidth: Quantity<V>? by lazy {
        val currentUsedWidth = usedWidth ?: return@lazy null
        val upperBound = material.widthRange.upperBound
        if (upperBound.unit != currentUsedWidth.unit) {
            return@lazy null
        }
        resolvedArithmetic.subtract(upperBound, currentUsedWidth)
    }
}

/**
 * 将物理量按离散次数累加 / Repeat quantity by discrete amount
 *
 * @param amount 累加次数 / Repeat amount
 * @param arithmetic 物理量算术策略 / Quantity arithmetic strategy
 * @return 累加结果 / Repeated quantity
 */
fun <V : RealNumber<V>> Quantity<V>.repeat(
    amount: UInt64,
    arithmetic: QuantityArithmetic<V>
): Quantity<V> {
    var result = Quantity(value.constants.zero, unit)
    for (i in amount.indices) {
        result = arithmetic.add(result, this)
    }
    return result
}
