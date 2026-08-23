package fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.infrastructure

import kotlin.time.Duration
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.networkSchedulingFailure

/**
 * 非负行驶时间。 / Non-negative travel time.
 *
 * @property duration 行驶时长 / Travel duration
 */
class TravelTime private constructor(val duration: Duration) {
    companion object {
        /** 创建行驶时间。 / Create travel time. */
        operator fun invoke(duration: Duration): Ret<TravelTime> {
            if (duration.isNegative()) {
                return networkSchedulingFailure(
                    "创建行驶时间失败：时间不能为负 / Failed to create travel time: time cannot be negative"
                )
            }
            return ok(TravelTime(duration))
        }
    }
}
