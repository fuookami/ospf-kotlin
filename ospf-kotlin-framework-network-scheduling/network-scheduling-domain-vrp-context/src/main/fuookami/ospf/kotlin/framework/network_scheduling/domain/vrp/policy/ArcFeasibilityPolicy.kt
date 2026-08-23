package fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.policy

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.NetworkNode
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.model.VehicleType

/** 静态弧可行性扩展点。 / Static arc-feasibility extension point. */
fun interface ArcFeasibilityPolicy<V : RealNumber<V>> {
    /**
     * 判断弧是否允许。 / Check whether an arc is allowed.
     *
     * @param from 起点 / Origin
     * @param to 终点 / Destination
     * @param vehicleType 车辆类型 / Vehicle type
     * @return 可行性或判断失败 / Feasibility or evaluation failure
     */
    fun isFeasible(
        from: NetworkNode<V>,
        to: NetworkNode<V>,
        vehicleType: VehicleType<V>
    ): Ret<Boolean>
}

/** 默认仅禁止自环。 / Default policy that only rejects self-loops. */
class DefaultArcFeasibilityPolicy<V : RealNumber<V>> : ArcFeasibilityPolicy<V> {
    override fun isFeasible(
        from: NetworkNode<V>,
        to: NetworkNode<V>,
        vehicleType: VehicleType<V>
    ): Ret<Boolean> {
        return ok(from.id != to.id)
    }
}
