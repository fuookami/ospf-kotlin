package fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.model

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.networkSchedulingFailure

/**
 * VRPTW 算法统一容差合同。 / Unified tolerance contract for VRPTW algorithms.
 *
 * @property feasibility 可行性容差 / Feasibility tolerance
 * @property pricing 定价容差 / Pricing tolerance
 * @property integrality 整数性容差 / Integrality tolerance
 * @property costValidation 成本复核容差 / Cost-validation tolerance
 * @property relativeGap 相对 gap 容差 / Relative-gap tolerance
 */
class VrptwTolerances private constructor(
    val feasibility: Flt64,
    val pricing: Flt64,
    val integrality: Flt64,
    val costValidation: Flt64,
    val relativeGap: Flt64
) {
    companion object {
        /** 默认容差合同。 / Default tolerance contract. */
        val default = VrptwTolerances(
            feasibility = Flt64(1e-7),
            pricing = Flt64(1e-8),
            integrality = Flt64(1e-7),
            costValidation = Flt64(1e-7),
            relativeGap = Flt64(1e-4)
        )

        /**
         * 创建容差合同。 / Create a tolerance contract.
         *
         * @param feasibility 可行性容差 / Feasibility tolerance
         * @param pricing 定价容差 / Pricing tolerance
         * @param integrality 整数性容差 / Integrality tolerance
         * @param costValidation 成本复核容差 / Cost-validation tolerance
         * @param relativeGap 相对 gap 容差 / Relative-gap tolerance
         * @return 容差合同或校验失败 / Tolerance contract or validation failure
         */
        operator fun invoke(
            feasibility: Flt64 = default.feasibility,
            pricing: Flt64 = default.pricing,
            integrality: Flt64 = default.integrality,
            costValidation: Flt64 = default.costValidation,
            relativeGap: Flt64 = default.relativeGap
        ): Ret<VrptwTolerances> {
            val values = listOf(feasibility, pricing, integrality, costValidation, relativeGap)
            if (values.any { !it.isFinite() || it ls Flt64.zero }) {
                return networkSchedulingFailure(
                    "创建 VRPTW 容差失败：所有容差必须是有限非负数 / " +
                            "Failed to create VRPTW tolerances: all tolerances must be finite and non-negative"
                )
            }
            return ok(
                VrptwTolerances(
                    feasibility = feasibility,
                    pricing = pricing,
                    integrality = integrality,
                    costValidation = costValidation,
                    relativeGap = relativeGap
                )
            )
        }
    }
}
