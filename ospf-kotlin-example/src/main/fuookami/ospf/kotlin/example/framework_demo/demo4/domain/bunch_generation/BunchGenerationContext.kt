@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_generation

import kotlinx.datetime.*
import kotlin.time.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_generation.service.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.rule.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.*

/** 批次生成上下文（管理聚合和生成器）。Context for bunch generation, managing aggregation and generators. */
class BunchGenerationContext {
    private lateinit var aggregation: Aggregation
    private lateinit var feasibilityJudger: FlightTaskFeasibilityJudger
    private lateinit var generators: Map<Aircraft, FlightTaskBunchGenerator>
    private lateinit var costCalculator: CostCalculator
    private lateinit var totalCostCalculator: TotalCostCalculator

    /** 获取初始航班束。Gets the initial flight bunches. */
    val initialFlightBunches get() = aggregation.initialFlightBunches

    /**
     * 初始化批次生成上下文。Initializes the bunch generation context.
 *
     * @param aircrafts 参数。
     * @param aircraftUsability 参数。
     * @param flightTasks 参数。
     * @param originBunches 参数。
     * @param lock 参数。
     * @param connectionTimeCalculator 参数。
     * @param minimumDepartureTimeCalculator 参数。
     * @param ruleChecker 参数。
     * @param costCalculator 参数。
     * @param totalCostCalculator 参数。
     * @param withOrderChange 参数。
     * @return 返回结果。
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

        val initializer = AggregationInitializer()
        aggregation = when (val ret = initializer(
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
            generators[aircraft] = FlightTaskBunchGenerator(
                aircraft = aircraft,
                aircraftUsability = aircraftUsability[aircraft]!!,
                graph = aggregation.graphs[aircraft]!!,
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
     * @param aircrafts 参数。
     * @param iteration 参数。
     * @param shadowPriceMap 参数。
     * @return 返回结果。
     */
    fun generateFlightTaskBunch(
        aircrafts: List<Aircraft>,
        iteration: Int64,
        shadowPriceMap: ShadowPriceMap
    ): Ret<List<FlightTaskBunch>> {
        val bunches = ArrayList<FlightTaskBunch>()
        for (aircraft in aircrafts) {
            val thisBunches = when (val ret = generators[aircraft]!!(iteration, shadowPriceMap)) {
                is Ok -> ret.value
                is Failed -> return Failed(ret.error)
                is Fatal -> return Fatal(ret.errors)
            }
            bunches.addAll(thisBunches)
        }

        return Ok(bunches)
    }
}
