package fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.policy

import kotlin.time.Duration
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeWindow as SchedulingTimeWindow
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.infrastructure.ResourceArc
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.model.*

/** 行驶时间计算扩展点。 / Travel-time calculation extension point. */
fun interface TravelTimeCalculator<V : RealNumber<V>> {
    /**
     * 计算一条弧的行驶时间。 / Calculate travel time for an arc.
     *
     * @param from 起点 / Origin
     * @param to 终点 / Destination
     * @param vehicleType 车辆类型 / Vehicle type
     * @return 行驶时间或计算失败 / Travel time or calculation failure
     */
    fun travelTime(
        from: NetworkNode<V>,
        to: NetworkNode<V>,
        vehicleType: VehicleType<V>
    ): Ret<Duration>

    /** 按稳定弧 ID 计算行驶时间；默认退回端点策略。
     * Calculate travel time by stable arc ID; defaults to the endpoint policy.
     * @param arc 资源弧 / Resource arc
     * @param from 起点 / Origin
     * @param to 终点 / Destination
     * @param vehicleType 车辆类型 / Vehicle type
     * @return 行驶时间或计算失败 / Travel time or calculation failure
     */
    fun travelTime(
        arc: ResourceArc<VehicleTypeId>,
        from: NetworkNode<V>,
        to: NetworkNode<V>,
        vehicleType: VehicleType<V>
    ): Ret<Duration> = travelTime(from, to, vehicleType)
}

/** 将距离数值按 1:1 映射为实例时间轴持续时间的显式策略。 / Explicit policy mapping distance values to instance-timeline durations at 1:1. */
class DistanceAsTravelTimeCalculator<V : RealNumber<V>>(
    private val distanceCalculator: DistanceCalculator<V>,
    private val schedulingWindow: SchedulingTimeWindow<V>
) : TravelTimeCalculator<V> {
    override fun travelTime(
        from: NetworkNode<V>,
        to: NetworkNode<V>,
        vehicleType: VehicleType<V>
    ): Ret<Duration> {
        val distance = distanceCalculator.distance(from, to).value
            ?: return networkSchedulingFailure(
                "计算行驶时间失败：距离计算失败 / Failed to calculate travel time: distance calculation failed"
            )
        return ok(schedulingWindow.durationOf(distance.value))
    }

    override fun travelTime(
        arc: ResourceArc<VehicleTypeId>,
        from: NetworkNode<V>,
        to: NetworkNode<V>,
        vehicleType: VehicleType<V>
    ): Ret<Duration> {
        val distance = distanceCalculator.distance(arc, from, to).value
            ?: return networkSchedulingFailure(
                "计算行驶时间失败：弧距离计算失败 / Failed to calculate travel time: arc distance calculation failed"
            )
        return ok(schedulingWindow.durationOf(distance.value))
    }
}
