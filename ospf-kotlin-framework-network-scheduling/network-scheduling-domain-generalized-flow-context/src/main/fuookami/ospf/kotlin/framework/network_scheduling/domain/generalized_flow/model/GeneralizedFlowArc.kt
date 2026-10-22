package fuookami.ospf.kotlin.framework.network_scheduling.domain.generalized_flow.model

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.quantities.unit.NoneUnit
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.*

/** 单输入、单输出的广义流服务弧。 / Single-input, single-output generalized-flow service arc.
 *
 * 容量和资费适用于起点投入量；[inputToOutputFactor] 只负责单位/工艺计量换算，[productionYield] 独立表示业务损耗或增益。
 * Capacity and tariff apply to origin input; [inputToOutputFactor] expresses unit/process measurement conversion,
 * while [productionYield] independently expresses business loss or gain.
 *
 * @property networkArc 拓扑与每起点单位投入资费 / Topology and tariff per origin-unit input
 * @property inputCapacity 按起点单位计的有限投入容量 / Finite input capacity measured in origin units
 * @property productionYield 无量纲业务产率 / Dimensionless business yield
 * @property inputToOutputFactor 可选的终点单位/起点单位转换系数 / Optional destination-unit per origin-unit conversion factor
 */
class GeneralizedFlowArc<V : RealNumber<V>> private constructor(
    val networkArc: NetworkArc<V>,
    val inputCapacity: CapacityBounds<V>,
    val productionYield: Quantity<V>,
    val inputToOutputFactor: Quantity<V>?
) {
    companion object {
        /** 创建单输入广义流弧。 / Create a single-input generalized-flow arc.
         *
         * @param networkArc 通用网络弧；其 cost 为每起点单位投入资费，单位为 costUnit/sourceUnit / Network arc; cost is tariff per origin-unit input, in costUnit/sourceUnit
         * @param inputCapacity 投入容量，单位由起点节点决定 / Input capacity, measured in the origin node unit
         * @param productionYield 无量纲业务产率，允许损耗、保持或增益 / Dimensionless business yield; loss, retention, or gain is allowed
         * @param inputToOutputFactor 跨量纲时必须显式提供的终点单位/起点单位系数 / Destination-unit per origin-unit factor, required for cross-dimensional conversion
         * @return 弧或产率量纲错误 / Arc or yield-dimension error
         */
        operator fun <V : RealNumber<V>> invoke(
            networkArc: NetworkArc<V>,
            inputCapacity: CapacityBounds<V>,
            productionYield: Quantity<V> = Quantity(networkArc.cost.value.constants.one, NoneUnit),
            inputToOutputFactor: Quantity<V>? = null
        ): Ret<GeneralizedFlowArc<V>> {
            if (productionYield.unit.isAffine || !productionYield.unit.sameDimension(NoneUnit)) {
                return networkSchedulingFailure(
                    "广义流业务产率必须是非仿射无量纲量 / Generalized-flow business yield must be a non-affine dimensionless quantity"
                )
            }
            return ok(GeneralizedFlowArc(
                networkArc = networkArc,
                inputCapacity = inputCapacity,
                productionYield = productionYield,
                inputToOutputFactor = inputToOutputFactor
            ))
        }
    }
}
