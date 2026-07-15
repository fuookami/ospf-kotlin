@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model

import kotlin.time.*
import java.util.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.infrastructure.*

/** 具有出发、到达、时间窗和启用飞机集的中转航班计划。A transfer flight plan with departure, arrival, time window, and enabled aircraft set. */
class TransferPlan internal constructor(
    override val dep: Airport,
    override val arr: Airport,
    override val timeWindow: TimeRange,
    override val aircraft: Aircraft? = null,
    override val enabledAircrafts: Set<Aircraft>,
    override val duration: Duration? = null,
    actualId: String = UUID.randomUUID().toString(),
    status: Set<FlightTaskStatus> = stableStatus
) : FlightTaskPlan(
    id = "${prefix}_${actualId.replace("-", "")}",
    name = "transfer_${dep}_${arr}_${timeWindow.start.toShortString()}",
    flightTaskStatus = status
) {
    companion object {
        private const val prefix = "tf"

        /** The default stable status set for transfer plans, preventing delay, advance, and terminal change.
         * 中转计划的默认稳定状态集合，禁止延误、提前和航站楼变更。 */
        val stableStatus = setOf(
            FlightTaskStatus.NotDelay,
            FlightTaskStatus.NotAdvance,
            FlightTaskStatus.NotTerminalChange
        )

        /**
         * 使用给定参数创建 [TransferPlan]，为单飞机集调整状态。Creates a [TransferPlan] with the given parameters, adjusting status for single-aircraft sets.
         *
         * @param dep The departure airport / 出发机场
         * @param arr The arrival airport / 到达机场
         * @param timeWindow The time window for the transfer / 中转的时间窗口
         * @param aircrafts The set of enabled aircraft / 启用的飞机集合
         * @param duration The optional flight duration / 可选的飞行时长
         * @return The created transfer plan / 创建的中转计划
        */
        operator fun invoke(
            dep: Airport,
            arr: Airport,
            timeWindow: TimeRange,
            aircrafts: Set<Aircraft>,
            duration: Duration? = null
        ): TransferPlan {
            assert(aircrafts.isNotEmpty())
            val status = stableStatus.toMutableSet()

            return if (aircrafts.size == 1) {
                status.add(FlightTaskStatus.NotAircraftChange)
                TransferPlan(
                    dep,
                    arr,
                    timeWindow,
                    aircrafts.first(),
                    aircrafts,
                    duration,
                    status = status
                )
            } else {
                TransferPlan(
                    dep = dep,
                    arr = arr,
                    timeWindow = timeWindow,
                    aircraft = aircrafts.first(),
                    enabledAircrafts = aircrafts,
                    duration = duration,
                    status = status
                )
            }
        }
    }

    override val actualId = FlightTaskPlanId(actualId)
    override val displayName = "transfer"
    override val scheduledTime: TimeRange? = null

    override fun duration(executor: Aircraft): Duration {
        return duration ?: executor.routeFlyTime[dep, arr] ?: executor.maxRouteFlyTime
    }

    /**
     * 检查给定飞机是否对此中转计划启用。Checks whether the given aircraft is enabled for this transfer plan.
     *
     * @param aircraft The aircraft to check / 要检查的飞机
     * @return true if the aircraft is enabled, false otherwise / 如果飞机启用则为true，否则为false
    */
    fun enabled(aircraft: Aircraft): Boolean {
        return enabledAircrafts.contains(aircraft)
    }
}

/** 中转航班的任务类型对象。Task type object for transfer flights. */
object TransferFlightTask : FlightTaskType(FlightTaskCategory.Flight, TransferFlightTask::class) {
    override val type = "transfer"
}

/** 具有可选恢复飞机和时间的中转航班任务。A transfer flight task with optional recovery aircraft and time. */
class Transfer internal constructor(
    override val plan: TransferPlan,
    val recoveryAircraft: Aircraft? = null,
    val recoveryTime: TimeRange? = null,
    origin: Transfer? = null
) : FlightTask(TransferFlightTask, origin) {
    companion object {
        /**
         * 从计划创建 [Transfer]（恒等构造函数）。Creates a [Transfer] from a plan (identity constructor).
         *
         * @param plan The transfer plan / 中转计划
         * @return The created transfer / 创建的中转
        */
        operator fun invoke(plan: Transfer): Transfer {
            return Transfer(plan = plan)
        }

        /**
         * 创建应用给定恢复策略的已恢复 [Transfer]。Creates a recovered [Transfer] applying the given recovery policy.
         *
         * @param origin The original transfer / 原始中转
         * @param recoveryPolicy The recovery policy assignment / 恢复策略分配
         * @return The recovered transfer instance / 恢复后的中转实例
        */
        operator fun invoke(origin: Transfer, recoveryPolicy: FlightTaskAssignment): Transfer {
            val recoveryAircraft =
                if (origin.plan.aircraft != null && (recoveryPolicy.aircraft == null || recoveryPolicy.aircraft == origin.plan.aircraft)) {
                    null
                } else {
                    recoveryPolicy.aircraft
                }

            assert(origin.recoveryEnabled(recoveryPolicy))
            return Transfer(
                plan = origin.plan,
                recoveryAircraft = recoveryAircraft,
                recoveryTime = recoveryPolicy.time!!,
                origin = origin
            )
        }
    }

    override val aircraft get() = recoveryAircraft ?: plan.aircraft
    override val time get() = recoveryTime ?: plan.time

    override val recovered get() = recoveryAircraft != null || recoveryTime != null
    override val recoveryPolicy get() = FlightTaskAssignment(recoveryAircraft, recoveryTime, null)
    override fun recoveryEnabled(policy: FlightTaskAssignment): Boolean {
        if (policy.aircraft != null && !plan.enabled(policy.aircraft)) {
            return false
        }
        if (policy.time == null || !plan.timeWindow.contains(policy.time!!)) {
            return false
        }
        return true
    }

    override fun recovery(policy: FlightTaskAssignment): FlightTask {
        assert(recoveryEnabled(policy))
        return Transfer(this, policy)
    }
}
