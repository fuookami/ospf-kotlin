package fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.model

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.quantities.dimension.Length
import fuookami.ospf.kotlin.quantities.unit.PhysicalUnit
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.networkSchedulingFailure

/**
 * 单个 VRPTW 实例的归一化单位口径。 / Normalized unit contract for one VRPTW instance.
 *
 * @property distanceUnit 距离单位 / Distance unit
 * @property loadUnit 负载单位 / Load unit
 * @property costUnit 成本单位 / Cost unit
 */
class VrptwUnits private constructor(
    val distanceUnit: PhysicalUnit,
    val loadUnit: PhysicalUnit,
    val costUnit: PhysicalUnit
) {
    companion object {
        /**
         * 创建实例单位口径。 / Create an instance unit contract.
         *
         * @param distanceUnit 距离单位 / Distance unit
         * @param loadUnit 负载单位 / Load unit
         * @param costUnit 成本单位 / Cost unit
         * @return 单位口径或校验失败 / Unit contract or validation failure
         */
        operator fun invoke(
            distanceUnit: PhysicalUnit,
            loadUnit: PhysicalUnit,
            costUnit: PhysicalUnit
        ): Ret<VrptwUnits> {
            if (distanceUnit.quantity != Length || distanceUnit.isAffine) {
                return networkSchedulingFailure(
                    "创建 VRPTW 单位口径失败：距离必须使用线性长度单位 / " +
                            "Failed to create VRPTW unit contract: distance must use a linear length unit"
                )
            }
            if (loadUnit.isAffine || costUnit.isAffine) {
                return networkSchedulingFailure(
                    "创建 VRPTW 单位口径失败：负载和成本单位不能是仿射单位 / " +
                            "Failed to create VRPTW unit contract: load and cost units cannot be affine"
                )
            }
            return ok(
                VrptwUnits(
                    distanceUnit = distanceUnit,
                    loadUnit = loadUnit,
                    costUnit = costUnit
                )
            )
        }
    }
}
