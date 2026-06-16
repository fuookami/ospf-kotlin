@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_generation

import kotlin.time.*
import kotlinx.datetime.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.rule.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_generation.service.*

/** Context for bunch generation, managing aggregation and generators. */
class BunchGenerationContext {
    private lateinit var aggregation: Aggregation
    private lateinit var feasibilityJudger: FlightTaskFeasibilityJudger
    private lateinit var generators: Map<Aircraft, FlightTaskBunchGenerator>
    private lateinit var costCalculator: CostCalculator
    private lateinit var totalCostCalculator: TotalCostCalculator

    /** Gets the initial flight bunches. */
    val initialFlightBunches get() = aggregation.initialFlightBunches

    /** Initializes the bunch generation context. */
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

    /** Generates flight task bunches for the given aircrafts and shadow price map. */
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
