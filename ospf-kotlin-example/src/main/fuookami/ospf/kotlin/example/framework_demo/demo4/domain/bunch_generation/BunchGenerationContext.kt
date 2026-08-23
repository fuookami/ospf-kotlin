@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_generation

import kotlinx.datetime.*
import kotlin.time.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_generation.service.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.rule.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.*

/** 批次生成上下文（管理聚合和生成器）。Context for bunch generation, managing aggregation and generators. */
class BunchGenerationContext(
    private val aggregationInitializer: AggregationInitializerProvider = AggregationInitializerProvider {
            aircrafts,
            aircraftUsability,
            flightTasks,
            originBunches,
            lock,
            flightTaskFeasibilityJudger,
            initialFlightTaskBunchGenerator,
            withOrderChange
        ->
        AggregationInitializer()(
            aircrafts = aircrafts,
            aircraftUsability = aircraftUsability,
            flightTasks = flightTasks,
            originBunches = originBunches,
            lock = lock,
            flightTaskFeasibilityJudger = flightTaskFeasibilityJudger,
            initialFlightTaskBunchGenerator = initialFlightTaskBunchGenerator,
            withOrderChange = withOrderChange
        )
    }
) {
    private lateinit var aggregation: Aggregation
    private lateinit var feasibilityJudger: FlightTaskFeasibilityJudger
    private lateinit var generators: Map<Aircraft, FlightTaskBunchGenerator>
    private lateinit var costCalculator: CostCalculator
    private lateinit var totalCostCalculator: TotalCostCalculator

    /** 获取路线图。Gets route graphs. */
    val graphs get() = if (::aggregation.isInitialized) aggregation.graphs else emptyMap()

    /** 获取可反转任务对。Gets reverse-enabled task pairs. */
    val reverse get() = if (::aggregation.isInitialized) aggregation.reverse else null

    /** 获取路线图构建诊断。Gets route graph construction diagnostics. */
    val routeGraphDiagnostics get() = if (::aggregation.isInitialized) aggregation.routeGraphDiagnostics else emptyMap()

    /** 获取最近一轮 pricing 诊断。Gets diagnostics from the latest pricing round. */
    val pricingDiagnostics get() = if (::generators.isInitialized) {
        generators.mapValues { (_, generator) -> generator.diagnostics }
    } else {
        emptyMap()
    }

    /** 获取初始航班束。Gets the initial flight bunches. */
    val initialFlightBunches get() = aggregation.initialFlightBunches

    /**
     * 初始化批次生成上下文。Initializes the bunch generation context.
     *
     * @param aircrafts 飞机列表 / list of aircraft
     * @param aircraftUsability 飞机可用性映射 / aircraft usability map
     * @param flightTasks 航班任务列表 / list of flight tasks
     * @param originBunches 原始航班任务束 / original flight task bunches
     * @param lock 锁定约束 / lock constraints
     * @param connectionTimeCalculator 连接时间计算器 / connection time calculator
     * @param minimumDepartureTimeCalculator 最小离港时间计算器 / minimum departure time calculator
     * @param ruleChecker 规则检查器 / rule checker
     * @param costCalculator 成本计算器 / cost calculator
     * @param totalCostCalculator 总成本计算器 / total cost calculator
     * @param withOrderChange 是否启用换序 / whether order change is enabled
     * @return 初始化结果 / initialization result
    */
    fun init(
        aircrafts: List<Aircraft>,
        aircraftUsability: Map<Aircraft, AircraftUsability>,
        flightTasks: List<FlightTask>,
        originBunches: List<FlightTaskBunch>,
        lock: Lock,
        connectionTimeCalculator: ConnectionTimeCalculator,
        minimumDepartureTimeCalculator: MinimumDepartureTimeCalculator,
        ruleChecker: RuleChecker,
        costCalculator: CostCalculator,
        totalCostCalculator: TotalCostCalculator,
        withOrderChange: Boolean = false
    ): Try {
        for (aircraft in aircrafts) {
            if (!aircraftUsability.containsKey(aircraft)) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "批次生成初始化失败：缺少飞机可用性 / Bunch generation initialization failed: aircraft usability is missing"
                )
            }
        }

        this.costCalculator = costCalculator
        this.totalCostCalculator = totalCostCalculator

        feasibilityJudger = FlightTaskFeasibilityJudger(
            aircraftUsability = aircraftUsability,
            connectionTimeCalculator = connectionTimeCalculator,
            ruleChecker = ruleChecker
        )

        val initialFlightTaskBunchGenerator = InitialFlightTaskBunchGenerator(
            feasibilityJudger = feasibilityJudger,
            connectionTimeCalculator = connectionTimeCalculator,
            minimumDepartureTimeCalculator = minimumDepartureTimeCalculator,
            costCalculator = totalCostCalculator
        )

        aggregation = when (val ret = aggregationInitializer(
            aircrafts,
            aircraftUsability,
            flightTasks,
            originBunches,
            lock,
            feasibilityJudger,
            initialFlightTaskBunchGenerator,
            withOrderChange
        )) {
            is Ok -> ret.value
            is Failed -> return Failed(ret.error)
            is Fatal -> return Fatal(ret.errors)
        }

        val generators = HashMap<Aircraft, FlightTaskBunchGenerator>()
        for (aircraft in aircrafts) {
            val usability = aircraftUsability[aircraft]
                ?: return Failed(
                    ErrorCode.IllegalArgument,
                    "批次生成初始化失败：缺少飞机可用性 / Bunch generation initialization failed: aircraft usability is missing"
                )
            val graph = aggregation.graphs[aircraft]
                ?: return Failed(
                    ErrorCode.DataNotFound,
                    "批次生成初始化失败：缺少飞机路线图 / Bunch generation initialization failed: aircraft route graph is missing"
                )
            generators[aircraft] = FlightTaskBunchGenerator(
                aircraft = aircraft,
                aircraftUsability = usability,
                graph = graph,
                connectionTimeCalculator = connectionTimeCalculator,
                minimumDepartureTimeCalculator = minimumDepartureTimeCalculator,
                costCalculator = costCalculator,
                totalCostCalculator = totalCostCalculator,
                configuration = BunchGenerationConfiguration(withOrderChange = withOrderChange)
            )
        }
        this.generators = generators

        return ok
    }

    /**
     * 为给定的飞机和影子价格映射生成航班任务束。Generates flight task bunches for the given aircrafts and shadow price map.
     *
     * @param aircrafts 需要生成束的飞机列表 / list of aircraft to generate bunches for
     * @param iteration 当前迭代次数 / current iteration number
     * @param shadowPriceMap 影子价格映射 / shadow price map
     * @return 生成的航班任务束 / generated flight task bunches
    */
    fun generateFlightTaskBunch(
        aircrafts: List<Aircraft>,
        iteration: Int64,
        shadowPriceMap: ShadowPriceMap
    ): Ret<List<FlightTaskBunch>> {
        if (!::generators.isInitialized) {
            return Failed(
                ErrorCode.ApplicationFailed,
                "批次生成失败：上下文尚未初始化 / Bunch generation failed: context is not initialized"
            )
        }

        val bunches = ArrayList<FlightTaskBunch>()
        for (aircraft in aircrafts) {
            val generator = generators[aircraft]
                ?: return Failed(
                    ErrorCode.DataNotFound,
                    "批次生成失败：缺少飞机生成器 / Bunch generation failed: aircraft generator is missing"
                )
            val thisBunches = when (val ret = generator(iteration, shadowPriceMap)) {
                is Ok -> ret.value
                is Failed -> return Failed(ret.error)
                is Fatal -> return Fatal(ret.errors)
            }
            bunches.addAll(thisBunches)
        }

        return Ok(bunches)
    }
}
