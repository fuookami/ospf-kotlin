@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_selection.service

import kotlin.time.Duration
import org.apache.logging.log4j.kotlin.logger
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.framework.solver.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.application.service.bunch.BranchAndPriceAlgorithm as FrameworkBranchAndPriceAlgorithm
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_compilation.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_compilation.service.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_selection.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.*

/**
 * 用于求解航班恢复调度问题的分支定价算法。Branch and price algorithm for solving the flight recovery scheduling problem.
 *
 * 封装框架 [FrameworkBranchAndPriceAlgorithm]，通过 demo4 Policy 连接各组件。
 * 求解器通过 [LinearSolverBuilder] 注入，不在领域层直接实例化。
 *
 * @property context 批次选择上下文。
 * @property solver 列生成求解器。
 * @property configuration 算法配置。
 */
class BranchAndPriceAlgorithm(
    private val context: BunchSelectionContext,
    private val solver: ColumnGenerationSolver,
    private val configuration: Configuration = Configuration()
) {
    private val logger = logger()

    /**
     * 分支定价算法配置。Branch and price algorithm configuration.
     *
     * @property badReducedAmount 约简成本差数量阈值。
     * @property maximumColumnAmount 最大列数。
     * @property minimumColumnAmountPerExecutor 每个执行器最小列数。
     * @property timeLimit 时间限制。
     */
    data class Configuration(
        val badReducedAmount: UInt64 = UInt64(20UL),
        val maximumColumnAmount: UInt64 = UInt64(50000UL),
        val minimumColumnAmountPerExecutor: UInt64 = UInt64.zero,
        val timeLimit: Duration = Duration.parse("PT30000S")
    )

    private val algorithm: FrameworkBranchAndPriceAlgorithm<
        ShadowPriceMap, ShadowPriceArguments, FlightTaskBunch, FltX, FlightTask, Aircraft, FlightTaskAssignment
    > by lazy {
        val policy = FrameworkBranchAndPriceAlgorithm.Policy<
            ShadowPriceMap, ShadowPriceArguments, FlightTaskBunch, FltX, FlightTask, Aircraft, FlightTaskAssignment
        >(
            contextBuilder = { context.bunchCompilationContext },
            extractContextBuilder = listOf(),
            shadowPriceMap = { ShadowPriceMap() },
            reducedCost = { map, bunch -> map.reducedCost(bunch as FlightTaskBunch) },
            bunchGenerator = { iteration: UInt64, aircrafts: List<Aircraft>, map: ShadowPriceMap ->
                context.bunchGenerationContext.generateFlightTaskBunch(
                    aircrafts = aircrafts,
                    iteration = iteration.toInt64(),
                    shadowPriceMap = map
                )
            }
        )

        val frameworkConfig = FrameworkBranchAndPriceAlgorithm.Configuration(
            badReducedAmount = configuration.badReducedAmount,
            maximumColumnAmount = configuration.maximumColumnAmount,
            minimumColumnAmountPerExecutor = configuration.minimumColumnAmountPerExecutor,
            timeLimit = configuration.timeLimit
        )

        FrameworkBranchAndPriceAlgorithm(
            executors = context.recoveryNeededAircrafts,
            tasks = context.recoveryNeededFlightTasks,
            initialBunches = context.bunchGenerationContext.initialFlightBunches,
            solver = solver,
            policy = policy,
            configuration = frameworkConfig
        )
    }

    /**
     * 执行分支定价算法。Executes the branch and price algorithm.
     *
     * @param id 标识符。
     * @param heartBeatCallBack 心跳回调。
     * @return 批次解。
     */
    suspend operator fun invoke(
        id: String,
        heartBeatCallBack: ((kotlin.time.Instant, Duration, Flt64) -> Try)? = null
    ): Ret<BunchSolution<FlightTaskBunch, FltX, FlightTask, Aircraft, FlightTaskAssignment>> {
        logger.info { "Starting branch-and-price algorithm for $id" }

        val result = algorithm(id, heartBeatCallBack)

        when (result) {
            is Ok -> {
                logger.info { "Branch-and-price completed for $id: ${result.value.bunches.size} bunches, ${result.value.canceledTasks.size} canceled tasks" }
            }
            is Failed -> {
                logger.error { "Branch-and-price failed for $id: ${result.error}" }
            }
            is Fatal -> {
                logger.error { "Branch-and-price fatal error for $id: ${result.errors}" }
            }
        }

        return result
    }
}
