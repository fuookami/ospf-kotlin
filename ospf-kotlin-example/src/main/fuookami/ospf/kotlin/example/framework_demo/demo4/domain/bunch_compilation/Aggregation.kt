@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_compilation

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.resource.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_compilation.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.rule.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.*

/**
 * 束编译聚合，组合任务时间、流、机队平衡、航班连接和航班容量约束用于列生成过程。/ Aggregation for bunch compilation combining task time, flow, fleet balance,
 * flight link, and flight capacity constraints for the column generation process.
 *
 * @property timeWindow Time window for the scheduling period / 调度周期的时间窗口
 * @property recoveryNeededAircrafts Aircrafts that require recovery / 需要恢复的飞机列表
 * @property recoveryNeededFlightTasks Flight tasks that require recovery / 需要恢复的航班任务列表
 * @property originBunches Original flight task bunches / 原始航班任务束列表
 * @property flows Flow control resources / 流量控制资源列表
 * @property links Link map for flight connections / 航班连接映射
*/
class Aggregation(
    timeWindow: TimeWindow<*>,
    val recoveryNeededAircrafts: List<Aircraft>,
    val recoveryNeededFlightTasks: List<FlightTask>,
    val originBunches: List<FlightTaskBunch>,
    val flows: List<Flow>,
    val links: LinkMap
): BunchCompilationAggregation<FlightTaskBunch, FltX, FlightTask, Aircraft, FlightTaskAssignment>(
    tasks = recoveryNeededFlightTasks,
    executors = recoveryNeededAircrafts
) {
    private val solverTimeWindow = timeWindow.toFlt64Boundary()
    private val resourceTimeWindow = TimeWindow(
        window = timeWindow.window,
        continues = timeWindow.continues,
        durationUnit = timeWindow.durationUnit,
        dateOffset = timeWindow.dateOffset,
        interval = timeWindow.interval,
        fromDouble = { FltX(it) },
        toDouble = { it.toDouble() }
    )

    val taskTime = BunchSchedulingTaskTime(
        timeWindow = solverTimeWindow,
        tasks = recoveryNeededFlightTasks,
        compilation = compilation
    )

    val flow = BunchSchedulingConnectionResourceUsage(
        timeWindow = resourceTimeWindow,
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

    /**
     * 向模型注册所有子聚合组件。/ Registers all sub-aggregation components with the model.
     *
     * @param model The optimization model to register with / 要注册的优化模型
     * @return Success or failure / 成功或失败
    */
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

    /**
     * 向所有子聚合组件添加新束的列。/ Adds columns for new bunches to all sub-aggregation components.
     *
     * @param iteration The current column generation iteration / 当前列生成迭代次数
     * @param newBunches New flight task bunches to add columns for / 要添加列的新航班任务束
     * @param model The optimization model / 优化模型
     * @return The unduplicated bunches, or an error / 去重后的束列表，或错误
    */
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

        if (unduplicatedBunches.isEmpty()) {
            return Ok(unduplicatedBunches)
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
