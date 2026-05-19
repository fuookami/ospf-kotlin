@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_compilation.service

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.service.limits.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.service.limits.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.rule.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_compilation.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_compilation.service.limits.*

class PipelineListGenerator(
    private val aggregation: Aggregation
) {
    operator fun invoke(): Ret<CGPipelineList> {
        val pipelines = ArrayList<CGPipeline>()

        pipelines.add(
            ExecutorCompilationConstraint(
                executors = aggregation.recoveryNeededAircrafts,
                compilation = aggregation.compilation
            )
        )

        pipelines.add(
            TaskCompilationConstraint(
                tasks = aggregation.recoveryNeededFlightTasks,
                compilation = aggregation.compilation
            )
        )

        pipelines.add(
            BunchCostMinimization(
                compilation = aggregation.compilation
            )
        )

        pipelines.add(
            ExecutorLeisureMinimization(
                executors = aggregation.recoveryNeededAircrafts,
                compilation = aggregation.compilation,
                coefficient = { aircraft: Aircraft ->
                    TODO("not implemented yet")
                }
            )
        )

        pipelines.add(
            TaskCancelMinimization(
                tasks = aggregation.recoveryNeededFlightTasks,
                compilation = aggregation.compilation,
                coefficient = { task: FlightTask ->
                    TODO("not implemented yet")
                }
            )
        )

        pipelines.add(
            FleetBalanceLimit(
                fleetBalance = aggregation.fleetBalance,
                coefficient = { airport: Airport, aircraftMinorType: AircraftMinorType ->
                    TODO("not implemented yet")
                }
            )
        )

        pipelines.add(
            FlightLinkLimit(
                flightLink = aggregation.flightLink,
                coefficient = { link: Link ->
                    TODO("not implemented yet")
                }
            )
        )

        return Ok(pipelines)
    }
}