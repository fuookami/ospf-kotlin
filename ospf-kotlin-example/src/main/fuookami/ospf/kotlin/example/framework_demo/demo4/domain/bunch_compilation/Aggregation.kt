@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_compilation


import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.resource.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.rule.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_compilation.model.*

class Aggregation(
    timeWindow: TimeWindow,
    val recoveryNeededAircrafts: List<Aircraft>,
    val recoveryNeededFlightTasks: List<FlightTask>,
    val originBunches: List<FlightTaskBunch>,
    val flows: List<Flow>,
    val links: LinkMap
): BunchCompilationAggregation<FlightTaskBunch, FlightTask, Aircraft, FlightTaskAssignment>(
    tasks = recoveryNeededFlightTasks,
    executors = recoveryNeededAircrafts
) {
    val taskTime = BunchSchedulingTaskTime(
        timeWindow = timeWindow,
        tasks = recoveryNeededFlightTasks,
        compilation = compilation
    )

    val flow = BunchSchedulingConnectionResourceUsage(
        timeWindow = timeWindow,
        resources = flows,
        name = "flow"
    )

    val fleetBalance = FleetBalance(
        aircrafts = recoveryNeededAircrafts,
        originBunches = originBunches,
        compilation = compilation
    )

    val flightLink = FlightLink(
        links = links.links.filter {
            it.prevTask in recoveryNeededFlightTasks && it.succTask in recoveryNeededFlightTasks
        },
        compilation = compilation
    )

    val flightCapacity = FlightCapacity(
        tasks = recoveryNeededFlightTasks,
        compilation = compilation
    )

    override fun register(model: MetaModel<Flt64>): Try {
        model as AbstractLinearMetaModel<Flt64>

        when (val result = super.register(model)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        when (val result = taskTime.register(model)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        when (val result = flow.register(model)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        when (val result = fleetBalance.register(model)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        when (val result = flightLink.register(model)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        when (val result = flightCapacity.register(model)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        return ok
    }

    override suspend fun addColumns(
        iteration: UInt64,
        newBunches: List<FlightTaskBunch>,
        model: AbstractLinearMetaModel<Flt64>
    ): Ret<List<FlightTaskBunch>> {
        val unduplicatedBunches = when (val result = super.addColumns(iteration, newBunches, model)) {
            is Ok -> {
                result.value
            }

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        when (val result = taskTime.addColumns(iteration, unduplicatedBunches, model)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        when (val result = flow.addColumns(iteration, unduplicatedBunches, compilation)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        when (val result = fleetBalance.addColumns(iteration, unduplicatedBunches)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        when (val result = flightLink.addColumns(iteration, unduplicatedBunches)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        when (val result = flightCapacity.addColumns(iteration, unduplicatedBunches)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        return Ok(unduplicatedBunches)
    }
}











