package fuookami.ospf.kotlin.example.framework_demo.demo2

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.value.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.Aggregation as AircraftAggregation
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.service.AggregationInitializer as AircraftAggregationInitializer
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.loading_effectiveness.model.TrailerType
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.loading_effectiveness.service.AggregationInitializer as LoadingAggregationInitializer
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.Aggregation as StowageAggregation
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.service.AggregationInitializer as StowageAggregationInitializer
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto.*

/** 验证 Demo2 飞机与配载聚合初始化 / Verifies Demo2 aircraft and stowage aggregation initialization. */
class Demo2AggregationInitializerTest {
    @Test
    fun `initializers preserve request data and register the base stowage model`() {
        val request = RequestDTO.sample()
        val aircraft = aircraftAggregation(request)
        val aggregation = stowageAggregation(
            aircraft = aircraft,
            request = request,
            mode = StowageMode.FullLoad
        )

        assertEquals(AircraftType.B737, aircraft.aircraftModel.type)
        assertEquals(request.positions.size, aircraft.positions.size)
        assertEquals(Flt64(-1.0), aircraft.positions[0].coordinate.longitudinalArm.value)
        assertEquals(1, aircraft.neighbours[NeighbourType.Physics]?.size)
        assertEquals(request.cargos.map { it.name }, aggregation.items.map { it.id })
        assertEquals(request.cargos.map { it.source }, aggregation.items.map { it.source!!.no })
        assertEquals(CargoPriorityCategory.High, aggregation.items.first().cargo.priority.category)
        assertEquals(Flt64(request.payloadUpperBound), aggregation.payload.maxPayload.value)
        assertTrue(aggregation.positions.all { it.status.stowageNeeded })

        val model = LinearMetaModel<Flt64>(
            name = "demo2-aggregation-initializer",
            converter = IntoValue.Identity
        )
        when (val result = aggregation.register(StowageMode.FullLoad, model)) {
            is Ok -> {}
            is Failed -> fail("base stowage registration failed: ${result.error.message}")
            is Fatal -> fail("base stowage registration failed: ${result.firstError?.message}")
        }
    }

    @Test
    fun `position status activates only mode appropriate additional weights`() {
        val fullLoad = PositionStatus(
            location = PositionLocation.normalMain,
            code = PositionStatusCode.Unloaded,
            stowageMode = StowageMode.FullLoad
        )
        val predistribution = PositionStatus(
            location = PositionLocation.normalMain,
            code = PositionStatusCode.Unloaded,
            stowageMode = StowageMode.Predistribution
        )
        val weightRecommendation = PositionStatus(
            location = PositionLocation.normalMain,
            code = PositionStatusCode.Unloaded,
            stowageMode = StowageMode.WeightRecommendation
        )
        val loaded = PositionStatus(
            location = PositionLocation.normalMain,
            code = PositionStatusCode.Loaded,
            stowageMode = StowageMode.WeightRecommendation
        )

        assertTrue(fullLoad.available)
        assertFalse(fullLoad.predicateWeightNeeded)
        assertFalse(fullLoad.recommendedWeightNeeded)
        assertTrue(predistribution.predicateWeightNeeded)
        assertFalse(predistribution.recommendedWeightNeeded)
        assertFalse(weightRecommendation.predicateWeightNeeded)
        assertTrue(weightRecommendation.recommendedWeightNeeded)
        assertFalse(loaded.available)
        assertFalse(loaded.stowageNeeded)
    }

    @Test
    fun `request position and cargo fields are wired into production aggregations`() {
        val request = RequestDTO.sample().copy(
            cargos = RequestDTO.sample().cargos.mapIndexed { index, cargo ->
                cargo.copy(order = index)
            },
            positions = listOf(
                PositionInput(
                    name = "P1",
                    maxWeight = 10.0,
                    longitudinalArm = -1.0,
                    lateralArm = -0.5,
                    maxLoadCount = 2,
                    loadedItems = listOf("C1"),
                    predicateLoadWeightMin = 1.0,
                    ala = 1,
                    alw = 8.0
                ),
                PositionInput(
                    name = "P2",
                    maxWeight = 10.0,
                    longitudinalArm = 1.0,
                    lateralArm = 0.5,
                    maxLoadCount = 4,
                    predicateLoadWeightMin = 1.5
                )
            )
        )
        val aircraft = aircraftAggregation(request)
        val fullLoad = stowageAggregation(aircraft, request, StowageMode.FullLoad)
        val predistribution = stowageAggregation(aircraft, request, StowageMode.Predistribution)

        assertEquals(listOf(0, 1, 2), fullLoad.items.map { it.order!!.order.toString().toInt() })
        assertEquals(setOf(fullLoad.items[0]), fullLoad.positions[0].loadedItems)
        assertEquals(UInt64(1UL), fullLoad.positions[0].ala)
        assertEquals(Flt64(8.0), fullLoad.positions[0].alw!!.value)
        assertEquals(UInt64(2UL), fullLoad.positions[0].mla)
        assertEquals(UInt64(4UL), fullLoad.positions[1].mla)
        assertEquals(Flt64(1.0), predistribution.positions[0].plw!!.min.value)
        assertEquals(Flt64(1.5), predistribution.positions[1].plw!!.min.value)
    }

    @Test
    fun `request trailers are converted to loading effectiveness trailers`() {
        val request = RequestDTO.sample().copy(
            trailers = listOf(
                TrailerInput(
                    type = TrailerTypeInput.Hardstand,
                    order = 1,
                    name = "T1",
                    items = listOf("C1", "C2")
                ),
                TrailerInput(
                    type = TrailerTypeInput.Transit,
                    order = 2,
                    name = "T2",
                    items = listOf("C3")
                )
            )
        )
        val aircraft = aircraftAggregation(request)
        val stowage = stowageAggregation(aircraft, request, StowageMode.FullLoad)
        val loading = when (val result = LoadingAggregationInitializer(
            aircraftAggregation = aircraft,
            stowageAggregation = stowage,
            input = request,
            stowageMode = StowageMode.FullLoad
        )) {
            is Ok -> result.value!!
            is Failed -> fail("loading initialization failed: ${result.error.message}")
            is Fatal -> fail("loading initialization failed: ${result.firstError?.message}")
        }

        assertEquals(listOf("T1", "T2"), loading.trailers.map { it.name })
        assertEquals(TrailerType.Hardstand, loading.trailers[0].type)
        assertEquals(listOf("C1", "C2"), loading.trailers[0].items.map { it.id })
        assertEquals(TrailerType.Transit, loading.trailers[1].type)
    }

    @Test
    fun `B757 stowage derives ballast from available positions`() {
        val request = RequestDTO.sample().copy(aircraftType = AircraftTypeInput.B757)
        val aircraft = aircraftAggregation(request)
        val aggregation = stowageAggregation(
            aircraft = aircraft,
            request = request,
            mode = StowageMode.FullLoad
        )

        assertNotNull(aggregation.ballast)
        assertEquals(aggregation.positions, aggregation.ballast!!.ballastPositions)
        assertNull(aggregation.ballast!!.minBallastWeight)
        assertNull(aggregation.ballast!!.adviceBallastWeight)
    }

    @Test
    fun `initializers return Failed for malformed request data`() {
        val validRequest = RequestDTO.sample()
        assertTrue(AircraftAggregationInitializer(validRequest.copy(positions = emptyList())) is Failed)
        assertTrue(AircraftAggregationInitializer(validRequest.copy(aircraftType = AircraftTypeInput.Unknown)) is Failed)

        val aircraft = aircraftAggregation(validRequest)
        assertTrue(StowageAggregationInitializer(
            aircraftAggregation = aircraft,
            input = validRequest.copy(cargos = emptyList()),
            stowageMode = StowageMode.FullLoad
        ) is Failed)
        assertTrue(StowageAggregationInitializer(
            aircraftAggregation = aircraft,
            input = validRequest.copy(cargos = listOf(
                CargoInput(
                    name = "C",
                    weight = 1.0,
                    priority = 1,
                    source = "S",
                    destination = "D"
                ),
                CargoInput(
                    name = "C",
                    weight = 1.0,
                    priority = 1,
                    source = "S",
                    destination = "D"
                )
            )),
            stowageMode = StowageMode.FullLoad
        ) is Failed)
        assertTrue(StowageAggregationInitializer(
            aircraftAggregation = aircraft,
            input = validRequest.copy(
                positions = validRequest.positions.map {
                    it.copy(predicateLoadWeightMin = null)
                }
            ),
            stowageMode = StowageMode.Predistribution
        ) is Failed)
        assertTrue(LoadingAggregationInitializer(
            aircraftAggregation = aircraft,
            stowageAggregation = stowageAggregation(aircraft, validRequest, StowageMode.FullLoad),
            input = validRequest.copy(
                trailers = listOf(
                    TrailerInput(
                        type = TrailerTypeInput.Hardstand,
                        order = 1,
                        name = "T1",
                        items = listOf("C1")
                    ),
                    TrailerInput(
                        type = TrailerTypeInput.Transit,
                        order = 2,
                        name = "T2",
                        items = listOf("C1")
                    )
                )
            ),
            stowageMode = StowageMode.FullLoad
        ) is Failed)
    }

    private fun aircraftAggregation(request: RequestDTO): AircraftAggregation {
        return when (val result = AircraftAggregationInitializer(request)) {
            is Ok -> result.value!!
            is Failed -> fail("aircraft initialization failed: ${result.error.message}")
            is Fatal -> fail("aircraft initialization failed: ${result.firstError?.message}")
        }
    }

    private fun stowageAggregation(
        aircraft: AircraftAggregation,
        request: RequestDTO,
        mode: StowageMode
    ): StowageAggregation {
        return when (val result = StowageAggregationInitializer(
            aircraftAggregation = aircraft,
            input = request,
            stowageMode = mode
        )) {
            is Ok -> result.value!!
            is Failed -> fail("stowage initialization failed: ${result.error.message}")
            is Fatal -> fail("stowage initialization failed: ${result.firstError?.message}")
        }
    }
}
