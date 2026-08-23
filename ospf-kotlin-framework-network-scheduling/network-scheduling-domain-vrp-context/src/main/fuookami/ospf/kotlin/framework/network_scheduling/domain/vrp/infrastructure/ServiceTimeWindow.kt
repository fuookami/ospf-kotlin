@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.infrastructure

import kotlin.time.Instant
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.networkSchedulingFailure

/**
 * 闭区间服务时间窗。 / Closed service time window.
 *
 * VRPTW 的截止时刻允许恰好开始服务；因此不能直接使用 Gantt 的右开 `TimeRange`。 / / VRPTW allows service to start exactly at its due time, so Gantt's half-open `TimeRange` cannot be used directly.
 *
 * @property readyTime 最早服务开始时刻 / Earliest service-start instant
 * @property dueTime 最晚服务开始时刻 / Latest service-start instant
 */
class ServiceTimeWindow private constructor(
    val readyTime: Instant,
    val dueTime: Instant
) {
    /** 判断服务开始时刻是否在闭区间内。 / Check whether a service-start instant is inside the closed interval. */
    fun contains(serviceStart: Instant): Boolean = readyTime <= serviceStart && serviceStart <= dueTime

    companion object {
        /**
         * 创建闭区间服务时间窗。 / Create a closed service time window.
         *
         * @param readyTime 最早服务开始时刻 / Earliest service-start instant
         * @param dueTime 最晚服务开始时刻 / Latest service-start instant
         * @return 服务时间窗或校验失败 / Service time window or validation failure
         */
        operator fun invoke(readyTime: Instant, dueTime: Instant): Ret<ServiceTimeWindow> {
            if (dueTime < readyTime) {
                return networkSchedulingFailure(
                    "创建服务时间窗失败：截止时刻早于起始时刻 / " +
                            "Failed to create service time window: due time is before ready time"
                )
            }
            return ok(ServiceTimeWindow(readyTime, dueTime))
        }
    }
}
