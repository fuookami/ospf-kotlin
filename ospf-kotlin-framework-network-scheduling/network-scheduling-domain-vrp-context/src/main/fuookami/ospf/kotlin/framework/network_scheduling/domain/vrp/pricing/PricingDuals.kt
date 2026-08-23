package fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.pricing

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.model.*

/** 定价阶段。 / Pricing phase. */
enum class PricingPhase {
    PhaseOne,
    PhaseTwo
}

/**
 * 与 compilation 解耦的不可变定价对偶快照。 / Immutable pricing-dual snapshot decoupled from compilation.
 *
 * 符号约定 / Sign convention:
 * - customer: 等式覆盖约束 `= 1` 的标准 LP 对偶变量，自由（可正可负）；
 *   在 reduced cost 公式中以 `-customerDual` 出现，正值降低 reduced cost（奖励覆盖客户）。
 * - fleet: `<=` 车队约束的框架有符号 LP 对偶变量；当前最小化实现和测试为非正值，
 *   在 reduced cost 公式中直接以 `-fleetDual` 使用，负值增加 reduced cost（惩罚使用车辆）。
 *   这与 Gantt 模块的 `reducedCost` 函数（`cost -= dual`）符号约定一致。
 *
 * - customer: standard LP dual for equality coverage constraint `= 1`, sign-free;
 *   appears as `-customerDual` in reduced cost, positive values lower reduced cost (reward covering customers).
 * - fleet: framework-signed LP dual for `<=` fleet constraint; the current minimization
 *   implementation and tests use a non-positive value, consumed directly as `-fleetDual`
 *   so a negative value increases reduced cost (penalizes vehicle usage).
 *   Consistent with the Gantt module's `reducedCost` function (`cost -= dual`).
 *
 * @property phase 当前阶段 / Current phase
 * @property customer 客户覆盖对偶 / Customer-coverage duals
 * @property fleet 车队数量对偶 / Fleet-size duals
 */
class PricingDuals(
    val phase: PricingPhase,
    customer: Map<CustomerId, Flt64>,
    fleet: Map<VehicleTypeId, Flt64>
) {
    val customer: Map<CustomerId, Flt64> = customer.toMap()
    val fleet: Map<VehicleTypeId, Flt64> = fleet.toMap()
}
