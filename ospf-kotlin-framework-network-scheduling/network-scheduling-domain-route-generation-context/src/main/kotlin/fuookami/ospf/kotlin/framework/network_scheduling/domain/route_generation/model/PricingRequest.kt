package fuookami.ospf.kotlin.framework.network_scheduling.domain.route_generation.model

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.model.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.pricing.PricingDuals
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.infrastructure.BranchMask

/** 定价中断检查器。 / Pricing interruption checker. */
fun interface PricingInterruptionChecker {
    /** 是否应立即停止定价。 / Whether pricing should stop immediately. */
    fun shouldStop(): Boolean

    companion object {
        /** 永不中断的默认检查器。 / Default checker that never interrupts pricing. */
        val Never = PricingInterruptionChecker { false }
    }
}

/**
 * 定价请求。 / Pricing request.
 *
 * 不可变输入合同，由 application 层构造后传入 EspprcPricer。
 * 与 route compilation 解耦，仅依赖 VRP context 的 PricingDuals。
 *
 * Immutable input contract, constructed by the application layer and passed
 * to the EspprcPricer. Decoupled from route compilation, depends only on
 * the VRP context's PricingDuals.
 *
 * @property instance VRPTW 实例 / VRPTW instance
 * @property duals 定价对偶 / Pricing duals
 * @property branchMask 分支掩码 / Branch mask (null if no branching)
 * @property pricingTolerance 定价容差 / Pricing tolerance
 * @property maxColumnsPerPricing 每次定价最大返回列数 / Max columns returned per pricing call
 * @property vehicleTypeId 当前定价的车辆类型 / Vehicle type being priced
 * @property interruptionChecker 定价中断检查器 / Pricing interruption checker
 */
data class PricingRequest<V : RealNumber<V>>(
    val instance: VrptwInstance<V>,
    val duals: PricingDuals,
    val branchMask: BranchMask<VehicleTypeId>?,
    val pricingTolerance: Flt64,
    val maxColumnsPerPricing: Int = Int.MAX_VALUE,
    val vehicleTypeId: VehicleTypeId,
    val interruptionChecker: PricingInterruptionChecker = PricingInterruptionChecker.Never
)
