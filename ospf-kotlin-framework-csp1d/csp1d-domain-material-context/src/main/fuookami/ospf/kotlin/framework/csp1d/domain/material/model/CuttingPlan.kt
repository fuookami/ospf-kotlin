package fuookami.ospf.kotlin.framework.csp1d.domain.material.model

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.FltX
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
 */
data class CuttingPlan<V : RealNumber<V>>(
    val id: String,
    val material: Material<V>,
    val machineId: String? = material.machineId,
    val slices: List<CuttingPlanSlice<V>>,
    val demandContributions: List<CuttingPlanDemandContribution<V>> = emptyList()
) {
    /**
     * 已使用幅宽 / Used width
     */
    val usedWidth: Quantity<V>? by lazy {
        slices.fold<CuttingPlanSlice<V>, Quantity<V>?>(null) { acc, slice ->
            val sliceWidth = slice.width.repeat(slice.amount)
            if (acc == null) {
                sliceWidth
            } else {
                addQuantity(
                    lhs = acc,
                    rhs = sliceWidth
                )
            }
        }
    }

    /**
     * 剩余幅宽 / Remaining width
     */
    val restWidth: Quantity<V>? by lazy {
        val currentUsedWidth = usedWidth ?: return@lazy null
        if (material.widthRange.upperBound.unit != currentUsedWidth.unit) {
            return@lazy null
        }
        subtractQuantity(
            lhs = material.widthRange.upperBound,
            rhs = currentUsedWidth
        )
    }
}

/**
 * 将物理量按离散次数累加 / Repeat quantity by discrete amount
 *
 * @param amount 累加次数 / Repeat amount
 * @return 累加结果 / Repeated quantity
 */
fun <V : RealNumber<V>> Quantity<V>.repeat(amount: UInt64): Quantity<V> {
    var result = Quantity(value.constants.zero, unit)
    for (i in amount.indices) {
        result = addQuantity(
            lhs = result,
            rhs = this
        )
    }
    return result
}

private fun <V : RealNumber<V>> addQuantity(
    lhs: Quantity<V>,
    rhs: Quantity<V>
): Quantity<V> {
    check(lhs.unit == rhs.unit)
    return Quantity(
        value = lhs.value + rhs.value,
        unit = lhs.unit
    )
}

private fun <V : RealNumber<V>> subtractQuantity(
    lhs: Quantity<V>,
    rhs: Quantity<V>
): Quantity<V>? {
    check(lhs.unit == rhs.unit)
    @Suppress("UNCHECKED_CAST")
    return when {
        lhs.value is Flt64 && rhs.value is Flt64 -> {
            Quantity(
                value = (lhs.value as Flt64) - (rhs.value as Flt64),
                unit = lhs.unit
            ) as Quantity<V>
        }

        lhs.value is FltX && rhs.value is FltX -> {
            Quantity(
                value = (lhs.value as FltX) - (rhs.value as FltX),
                unit = lhs.unit
            ) as Quantity<V>
        }

        else -> {
            null
        }
    }
}
