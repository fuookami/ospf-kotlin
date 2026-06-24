@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_compilation.service

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.service.limits.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.service.limits.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_compilation.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_compilation.service.limits.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.rule.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.*

/**
 * 包含编译约束和限制的列生成管线列表生成器。Generator for the column generation pipeline list including compilation constraints and limits.
 *
 * @property aggregation 编译聚合。Compilation aggregation.
 * @property parameter 列生成主模型系数参数。Column generation master model coefficient parameters.
 */
class PipelineListGenerator(
    private val aggregation: Aggregation,
    private val parameter: Parameter = Parameter()
) {
    /**
     * 创建并返回包含所有编译约束和限制的管线列表。Creates and returns the pipeline list with all compilation constraints and limits.
     *
     * @return 返回结果。
     */
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
                coefficient = { _: Aircraft ->
                    // Source: Parameter.executorLeisureCoeff — ported from fsra-proof aircraftLeisure.
                    parameter.executorLeisureCoeff
                }
            )
        )

        pipelines.add(
            TaskCancelMinimization(
                tasks = aggregation.recoveryNeededFlightTasks,
                compilation = aggregation.compilation,
                coefficient = { _: FlightTask ->
                    // Source: Parameter.taskCancelCoeff — ported from fsra-proof flightCancel.
                    parameter.taskCancelCoeff
                }
            )
        )

        pipelines.add(
            FleetBalanceLimit(
                fleetBalance = aggregation.fleetBalance,
                coefficient = { airport: Airport, _: AircraftMinorType ->
                    // Source: Parameter.fleetBalanceBaseSlack / fleetBalanceSlack — ported from fsra-proof FleetBalanceLimit.
                    if (airport.base) parameter.fleetBalanceBaseSlack else parameter.fleetBalanceSlack
                }
            )
        )

        pipelines.add(
            FlightLinkLimit(
                flightLink = aggregation.flightLink,
                coefficient = { link: Link ->
                    // Source: Link.splitCost domain field — ported from fsra-proof FlightLinkLimit.
                    link.splitCost.toFlt64()
                }
            )
        )

        return Ok(pipelines)
    }
}
