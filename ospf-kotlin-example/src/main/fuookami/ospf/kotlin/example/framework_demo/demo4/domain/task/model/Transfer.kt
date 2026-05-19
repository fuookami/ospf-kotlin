@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model

import java.util.*
import kotlin.time.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.infrastructure.*

class TransferPlan internal constructor(
    override val dep: Airport,
    override val arr: Airport,
    override val timeWindow: TimeRange,
    override val aircraft: Aircraft? = null,
    override val enabledAircrafts: Set<Aircraft>,
    override val duration: Duration? = null,
    override val actualId: String = UUID.randomUUID().toString(),
    status: Set<FlightTaskStatus> = stableStatus
) : FlightTaskPlan(
    id = "${prefix}_${actualId.replace("-", "")}",
    name = "transfer_${dep}_${arr}_${timeWindow.start.toShortString()}",
    flightTaskStatus = status
) {
    companion object {
        private const val prefix = "tf"

        val stableStatus = setOf(
            FlightTaskStatus.NotDelay,
            FlightTaskStatus.NotAdvance,
            FlightTaskStatus.NotTerminalChange
        )

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

    override val displayName = "transfer"
    override val scheduledTime: TimeRange? = null

    override fun duration(executor: Aircraft): Duration {
        return duration ?: executor.routeFlyTime[dep, arr] ?: executor.maxRouteFlyTime
    }

    fun enabled(aircraft: Aircraft): Boolean {
        return enabledAircrafts.contains(aircraft)
    }
}

object TransferFlightTask : FlightTaskType(FlightTaskCategory.Flight, TransferFlightTask::class) {
    override val type = "transfer"
}

class Transfer internal constructor(
    override val plan: TransferPlan,
    val recoveryAircraft: Aircraft? = null,
    val recoveryTime: TimeRange? = null,
    origin: Transfer? = null
) : FlightTask(TransferFlightTask, origin) {
    companion object {
        operator fun invoke(plan: Transfer): Transfer {
            return Transfer(plan = plan)
        }

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