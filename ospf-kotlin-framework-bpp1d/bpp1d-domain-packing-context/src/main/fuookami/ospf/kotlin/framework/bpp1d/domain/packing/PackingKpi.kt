package fuookami.ospf.kotlin.framework.bpp1d.domain.packing

import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.unit.PhysicalUnit
import fuookami.ospf.kotlin.quantities.quantity.*

/** 保留领域精度和长度口径的装箱指标。 / Packing indicators preserving domain precision and length units.
 * @property binCount 箱数 / Bin count
 * @property itemCount 件数 / Item count
 * @property consumedLength 消耗箱长 / Consumed bin length
 * @property remnantLength 可用尾余长度，不包含间距 / Free tail length, excluding spacing
 * @property utilization 长度利用率 / Length utilization
 * @property cost 成本 / Cost
 */
data class PackingKpi<V : FloatingNumber<V>>(
    val binCount: UInt64,
    val itemCount: UInt64,
    val consumedLength: Quantity<V>,
    val remnantLength: Quantity<V>,
    val utilization: V,
    val cost: V
)

/** 生成物理量 KPI。 / Build quantity-aware KPIs.
 * @param unit 统计长度单位 / Reporting length unit
 * @return 指标或量纲错误 / Indicators or a dimension error
 */
fun <V : FloatingNumber<V>> PackingResult<V>.kpi(unit: PhysicalUnit): Ret<PackingKpi<V>> {
    val zero = utilization.constants.zero
    var consumed = zero
    var free = zero
    var cost = zero
    var count = 0uL
    for (plan in plans) {
        consumed += plan.bin.type.length.convertTo(unit)?.value ?: return Failed(ErrorCode.IllegalArgument, "KPI 长度量纲无效 / Invalid KPI length dimension")
        cost += plan.bin.type.cost
        count += plan.placements.size.toULong()
    }
    for (remnant in remnants) free += remnant.length.convertTo(unit)?.value ?: return Failed(ErrorCode.IllegalArgument, "KPI 余量量纲无效 / Invalid KPI remnant dimension")
    return Ok(PackingKpi(
        binCount = UInt64(plans.size.toULong()), itemCount = UInt64(count),
        consumedLength = Quantity(consumed, unit), remnantLength = Quantity(free, unit), utilization = utilization, cost = cost
    ))
}
