package fuookami.ospf.kotlin.example.framework_demo.demo2

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.value.*
import fuookami.ospf.kotlin.core.symbol.function.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.Aggregation as AircraftAggregation
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.service.AggregationInitializer as AircraftAggregationInitializer
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.express_effectiveness.service.AggregationInitializer as ExpressAggregationInitializer
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.loading_effectiveness.model.SequentialLoading
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.loading_effectiveness.model.Trailer
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.loading_effectiveness.model.TrailerLoading
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.loading_effectiveness.model.TrailerType
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.loading_effectiveness.service.AggregationInitializer as LoadingAggregationInitializer
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.recommended_weight_equalization.service.AggregationInitializer as RecommendedWeightEqualizationAggregationInitializer
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.redundancy.service.AggregationInitializer as RedundancyAggregationInitializer
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.Aggregation as StowageAggregation
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.service.AggregationInitializer as StowageAggregationInitializer
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto.*

/** 验证 Demo2 附加业务模型的初始化与模式选择。 / Verifies Demo2 additional business-model initialization and mode selection. */
class Demo2AdditionalModelInitializationTest {
    @Test
    fun `additional models are selected only by their applicable stowage mode`() {
        val request = RequestDTO.sample()
        val aircraft = aircraftAggregation(request)
        val fullLoad = stowageAggregation(aircraft, request, StowageMode.FullLoad)
        val predistribution = stowageAggregation(aircraft, request, StowageMode.Predistribution)
        val weightRecommendation = stowageAggregation(aircraft, request, StowageMode.WeightRecommendation)

        val fullExpress = unwrap(ExpressAggregationInitializer(
            aircraftAggregation = aircraft,
            stowageAggregation = fullLoad,
            input = request,
            stowageMode = StowageMode.FullLoad
        ), "full-load express initialization")
        val predistributionExpress = unwrap(ExpressAggregationInitializer(
            aircraftAggregation = aircraft,
            stowageAggregation = predistribution,
            input = request,
            stowageMode = StowageMode.Predistribution
        ), "predistribution express initialization")
        val recommendationExpress = unwrap(ExpressAggregationInitializer(
            aircraftAggregation = aircraft,
            stowageAggregation = weightRecommendation,
            input = request,
            stowageMode = StowageMode.WeightRecommendation
        ), "weight-recommendation express initialization")

        assertNotNull(fullExpress.relativeOrder)
        assertNull(fullExpress.absoluteOrder)
        assertNotNull(predistributionExpress.absoluteOrder)
        assertNull(predistributionExpress.relativeOrder)
        assertNull(recommendationExpress.absoluteOrder)
        assertNull(recommendationExpress.relativeOrder)

        val fullLoading = unwrap(LoadingAggregationInitializer(
            aircraftAggregation = aircraft,
            stowageAggregation = fullLoad,
            input = request,
            stowageMode = StowageMode.FullLoad
        ), "full-load loading initialization")
        val predistributionLoading = unwrap(LoadingAggregationInitializer(
            aircraftAggregation = aircraft,
            stowageAggregation = predistribution,
            input = request,
            stowageMode = StowageMode.Predistribution
        ), "predistribution loading initialization")
        val recommendationLoading = unwrap(LoadingAggregationInitializer(
            aircraftAggregation = aircraft,
            stowageAggregation = weightRecommendation,
            input = request,
            stowageMode = StowageMode.WeightRecommendation
        ), "weight-recommendation loading initialization")

        assertNotNull(fullLoading.trailerLoading)
        assertNull(fullLoading.sequentialLoading)
        assertNotNull(predistributionLoading.adviceLoading)
        assertNotNull(predistributionLoading.sequentialLoading)
        assertNull(predistributionLoading.trailerLoading)
        assertNull(recommendationLoading.adviceLoading)
        assertNull(recommendationLoading.sequentialLoading)
        assertNull(recommendationLoading.trailerLoading)
    }

    @Test
    fun `priority and trailer models derive stable ordered pairs`() {
        val request = RequestDTO.sample()
        val aircraft = aircraftAggregation(request)
        val aggregation = stowageAggregation(aircraft, request, StowageMode.FullLoad)
        val express = unwrap(ExpressAggregationInitializer(
            aircraftAggregation = aircraft,
            stowageAggregation = aggregation,
            input = request,
            stowageMode = StowageMode.FullLoad
        ), "express initialization")
        val relativeOrder = express.relativeOrder ?: fail("full-load relative order must be initialized")
        val absoluteOrder = unwrap(ExpressAggregationInitializer(
            aircraftAggregation = aircraft,
            stowageAggregation = stowageAggregation(aircraft, request, StowageMode.Predistribution),
            input = request,
            stowageMode = StowageMode.Predistribution
        ), "predistribution express initialization").absoluteOrder!!

        assertEquals(listOf("C1" to "C2", "C1" to "C3", "C2" to "C3"),
            relativeOrder.orderedItems.map { it.first.id to it.second.id })
        assertEquals(1, relativeOrder.orderedPositions.size)
        assertTrue(absoluteOrder(aggregation.items.first().cargo.priority, aggregation.positions.first()) >
            absoluteOrder(aggregation.items.first().cargo.priority, aggregation.positions.last()))
        assertTrue(absoluteOrder(aggregation.items.first().cargo.priority, aggregation.positions.first()) >
            absoluteOrder(aggregation.items.last().cargo.priority, aggregation.positions.first()))

        val model = LinearMetaModel<Flt64>(
            name = "demo2-relative-order",
            converter = IntoValue.Identity
        )
        assertSuccess(aggregation.register(StowageMode.FullLoad, model), "base stowage registration")
        assertSuccess(relativeOrder.register(model), "relative-order registration")

        val sequentialLoading = SequentialLoading(
            items = aggregation.items,
            positions = aggregation.positions,
            orderedPositions = listOf(aggregation.positions.first() to aggregation.positions.last()),
            stowage = aggregation.stowage
        )
        assertEquals(
            listOf("C1" to "C2", "C1" to "C3", "C2" to "C3"),
            sequentialLoading.orderedItems.map { it.first.id to it.second.id }
        )

        val trailerLoading = TrailerLoading(
            items = aggregation.items,
            positions = aggregation.positions,
            trailers = listOf(
                Trailer(TrailerType.Hardstand, UInt8(1.toUByte()), "T1", aggregation.items.take(2)),
                Trailer(TrailerType.Transit, UInt8(2.toUByte()), "T2", aggregation.items.takeLast(1))
            ),
            adjacentPositions = listOf(aggregation.positions.first() to aggregation.positions.last()),
            stowage = aggregation.stowage,
            load = aggregation.load
        )
        assertEquals(listOf("C1" to "C2"),
            trailerLoading.orderedItemsInTrailers.map { it.first.id to it.second.id })
        assertEquals(listOf("T1" to "T2"),
            trailerLoading.orderedTrailers.map { it.first.name to it.second.name })
    }

    @Test
    fun `redundancy and recommended weight initializers preserve stowage data and register`() {
        val request = RequestDTO.sample()
        val aircraft = aircraftAggregation(request)
        val predistribution = stowageAggregation(aircraft, request, StowageMode.Predistribution)
        val weightRecommendation = stowageAggregation(aircraft, request, StowageMode.WeightRecommendation)
        val redundancy = unwrap(RedundancyAggregationInitializer(
            aircraftAggregation = aircraft,
            stowageAggregation = predistribution,
            input = request
        ), "redundancy initialization")
        val recommendedWeightEqualization = unwrap(RecommendedWeightEqualizationAggregationInitializer(
            aircraftAggregation = aircraft,
            stowageAggregation = weightRecommendation,
            input = request
        ), "recommended-weight initialization")

        assertTrue(weightRecommendation.items.all { item ->
            weightRecommendation.positions.all { position ->
                recommendedWeightEqualization.priorityAppointment(item.cargo.priority, position)
            }
        })

        val model = LinearMetaModel<Flt64>(
            name = "demo2-redundancy",
            converter = IntoValue.Identity
        )
        assertSuccess(predistribution.register(StowageMode.Predistribution, model), "predistribution stowage registration")
        assertSuccess(redundancy.register(StowageMode.Predistribution, model), "redundancy registration")
        assertNotNull(redundancy.redundancy.redundancy)
        assertNotNull(redundancy.redundancy.predicateRedundancy)
        assertNotNull(redundancy.redundancy.redundancySlack)
        assertNotNull(redundancy.experimentalLongitudinalBalance.predicateLongitudinalTorque)

        val actualRedundancy = redundancy.redundancy.redundancy.toLinearPolynomial()
        val predicateRedundancy = redundancy.redundancy.predicateRedundancy.toLinearPolynomial()
        assertEquals(Flt64(20.0), actualRedundancy.constant)
        assertEquals(Flt64(20.0), predicateRedundancy.constant)
        assertTrue(actualRedundancy.monomials.all { it.coefficient leq Flt64.zero })
        assertTrue(predicateRedundancy.monomials.all { it.coefficient leq Flt64.zero })
        assertNotEquals(actualRedundancy, predicateRedundancy)

        val balanceSlack = redundancy.experimentalLongitudinalBalance.longitudinalTorqueSlack.value
        assertTrue(balanceSlack is LinearFunctionSymbolAdapter<*>)
        val slackFunction = (balanceSlack as LinearFunctionSymbolAdapter<Flt64>).delegate
        assertTrue(slackFunction is SlackFunction<*>)
        val torqueSlack = slackFunction as SlackFunction<Flt64>
        assertEquals(
            "main_actual_longitudinal_torque",
            torqueSlack.x.monomials.single().symbol.name
        )
        assertEquals(
            "predicate_longitudinal_torque",
            torqueSlack.y.monomials.single().symbol.name
        )
    }

    private fun aircraftAggregation(request: RequestDTO): AircraftAggregation {
        return unwrap(AircraftAggregationInitializer(request), "aircraft initialization")
    }

    private fun stowageAggregation(
        aircraft: AircraftAggregation,
        request: RequestDTO,
        mode: StowageMode
    ): StowageAggregation {
        return unwrap(StowageAggregationInitializer(
            aircraftAggregation = aircraft,
            input = request,
            stowageMode = mode
        ), "stowage initialization")
    }

    private fun <T> unwrap(result: Ret<T>, description: String): T {
        return when (result) {
            is Ok -> result.value!!
            is Failed -> fail("$description failed: ${result.error.message}")
            is Fatal -> fail("$description failed: ${result.firstError?.message}")
        }
    }

    private fun assertSuccess(result: Try, description: String) {
        when (result) {
            is Ok -> {}
            is Failed -> fail("$description failed: ${result.error.message}")
            is Fatal -> fail("$description failed: ${result.firstError?.message}")
        }
    }
}
