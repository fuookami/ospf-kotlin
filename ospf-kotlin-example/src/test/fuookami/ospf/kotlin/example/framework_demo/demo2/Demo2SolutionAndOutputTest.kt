package fuookami.ospf.kotlin.example.framework_demo.demo2

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.value.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.Aggregation as AircraftAggregation
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.AircraftContext
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.service.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto.*

/** 验证 Demo2 的领域解提取、响应渲染和装载顺序输出 / Verifies Demo2 domain solution extraction, response rendering, and loading-order output. */
class Demo2SolutionAndOutputTest {
    @Test
    fun `solver values are extracted into ordered response and render data`() {
        val request = RequestDTO.sample()
        val context = initializedStowageContext(request, StowageMode.Predistribution)
        val model = registeredModel(context, StowageMode.Predistribution)
        val values = MutableList(model.tokens.tokenList.tokensInSolver.size) { Flt64.zero }

        val assignmentIndex = model.tokens.indexOf(context.aggregation.stowage.x[0, 1])!!
        val predicateWeightIndex = model.tokens.indexOf(context.aggregation.load.y[0].value)!!
        values[assignmentIndex] = Flt64.one
        values[predicateWeightIndex] = Flt64(7.0)

        val solution = analyzedSolution(context, values, model)
        assertEquals(listOf(context.aggregation.items[0]), solution.stowage[context.aggregation.positions[1]])
        assertEquals(Flt64(7.0), solution.predicateLoadWeight[context.aggregation.positions[0]]?.value)
        assertTrue(solution.recommendedLoadWeight.isEmpty())

        val response = response(context, solution, request)
        assertTrue(response.succeed)
        assertEquals("Optimal", response.status)
        assertNull(response.objective)
        assertEquals(listOf("C1 -> P2"), response.assignments)

        val render = solution.render()
        assertEquals(listOf("P1", "P2"), render.positions.map { it.positionName })
        assertEquals(Flt64(7.0).toDouble(), render.positions[0].predicateLoadWeight?.value)
        assertNull(render.positions[0].recommendedLoadWeight)
        assertEquals(listOf("C1"), render.positions[1].itemIds)
    }

    @Test
    fun `empty and partial domain solutions render in stable order`() {
        val context = initializedStowageContext(RequestDTO.sample(), StowageMode.WeightRecommendation)
        val positions = context.aggregation.positions
        val items = context.aggregation.items
        val emptySolution = Solution(
            stowage = emptyMap(),
            predicateLoadWeight = emptyMap(),
            recommendedLoadWeight = emptyMap()
        )
        val partialSolution = Solution(
            stowage = mapOf(positions[1] to listOf(items[1], items[0])),
            predicateLoadWeight = mapOf(positions[0] to positions[0].mlw.mlw),
            recommendedLoadWeight = mapOf(positions[1] to positions[1].mlw.mlw)
        )

        assertTrue(emptySolution.render().positions.isEmpty())

        val render = partialSolution.render()
        assertEquals(listOf("P1", "P2"), render.positions.map { it.positionName })
        assertEquals(emptyList<String>(), render.positions[0].itemIds)
        assertNotNull(render.positions[0].predicateLoadWeight)
        assertEquals(listOf("C1", "C2"), render.positions[1].itemIds)
        assertNotNull(render.positions[1].recommendedLoadWeight)

        val response = response(context, emptySolution, RequestDTO.sample())
        assertTrue(response.succeed)
        assertEquals("Optimal", response.status)
        assertTrue(response.assignments.isEmpty())
    }

    @Test
    fun `missing solver values return Failed instead of throwing`() {
        val context = initializedStowageContext(RequestDTO.sample(), StowageMode.FullLoad)
        val model = registeredModel(context, StowageMode.FullLoad)

        when (val result = context.analyze(emptyList(), model)) {
            is Ok -> fail("missing solver values should not produce a solution")
            is Failed -> assertTrue(result.error.message.contains("缺少"))
            is Fatal -> fail("missing solver values should be a recoverable failure")
        }
    }

    @Test
    fun `complete stowage pipeline registers for every stowage mode`() {
        for (mode in StowageMode.entries) {
            val context = initializedStowageContext(RequestDTO.sample(), mode)
            val model = LinearMetaModel<Flt64>(
                name = "demo2-complete-stowage-pipeline-$mode",
                converter = IntoValue.Identity
            )

            when (val result = context.register(mode, model)) {
                is Ok -> assertTrue(model.constraints.isNotEmpty())
                is Failed -> fail("complete stowage pipeline failed for $mode: ${result.error.message}")
                is Fatal -> fail("complete stowage pipeline failed for $mode: ${result.firstError?.message}")
            }
        }
    }

    @Test
    fun `full-load and predistribution domain pipelines register completely`() {
        val request = RequestDTO.sample()
        val parameter = request.parameter
        val fullLoad = FullLoadPipelineContext()
        val predistribution = PredistributionPipelineContext()

        when (val result = fullLoad.init(request, StowageMode.FullLoad)) {
            is Ok -> {}
            is Failed -> fail("full-load pipeline initialization failed: ${result.error.message}")
            is Fatal -> fail("full-load pipeline initialization failed: ${result.firstError?.message}")
        }
        when (val result = fullLoad.register(
            stowageMode = StowageMode.FullLoad,
            parameter = parameter,
            model = LinearMetaModel(
                name = "demo2-full-load-domain-pipeline",
                converter = IntoValue.Identity
            )
        )) {
            is Ok -> {}
            is Failed -> fail("full-load pipeline registration failed: ${result.error.message}")
            is Fatal -> fail("full-load pipeline registration failed: ${result.firstError?.message}")
        }

        when (val result = predistribution.init(request)) {
            is Ok -> {}
            is Failed -> fail("predistribution pipeline initialization failed: ${result.error.message}")
            is Fatal -> fail("predistribution pipeline initialization failed: ${result.firstError?.message}")
        }
        when (val result = predistribution.register(
            stowageMode = StowageMode.Predistribution,
            parameter = parameter,
            model = LinearMetaModel(
                name = "demo2-predistribution-domain-pipeline",
                converter = IntoValue.Identity
            )
        )) {
            is Ok -> {}
            is Failed -> fail("predistribution pipeline registration failed: ${result.error.message}")
            is Fatal -> fail("predistribution pipeline registration failed: ${result.firstError?.message}")
        }
    }

    @Test
    fun `all applications report hard capacity infeasibility before solving`() = runBlocking {
        val request = RequestDTO.sample().copy(
            cargos = listOf(
                CargoInput(
                    name = "too-heavy",
                    weight = 11.0,
                    priority = 10,
                    source = "S1",
                    destination = "D1"
                )
            )
        )

        for ((path, result) in applications(request)) {
            val (response, render) = result

            assertFalse(response.succeed, "$path should reject a cargo that exceeds every position")
            assertEquals("NoSolution", response.status)
            assertTrue(response.notes.any { it.contains("exceeds every position") })
            assertNull(render)
        }
    }

    @Test
    fun `loading order output is stable when source deck positions are shuffled`() = runBlocking {
        val request = RequestDTO.sample()
        val aggregation = aircraftAggregation(request)
        val shuffledAggregation = aggregation.copy(
            decks = aggregation.decks.map { deck ->
                deck.copy(positions = deck.positions.reversed())
            }
        )

        val directResponse = loadingOrderResponse(shuffledAggregation, request)
        val applicationResponse = LoadingOrderAlgorithm(request)
        val expected = listOf(
            "0: P1 (order=0)",
            "1: P2 (order=1)"
        )

        assertTrue(directResponse.succeed)
        assertEquals("Optimal", directResponse.status)
        assertEquals(expected, directResponse.orders)
        assertTrue(applicationResponse.succeed)
        assertEquals(expected, applicationResponse.orders)
    }

    private suspend fun applications(
        request: RequestDTO
    ): List<Pair<String, Pair<ResponseDTO, RenderDTO?>>> {
        return listOf(
            "full-load" to FullLoadAlgorithm(request),
            "predistribution" to PredistributionApplication()(request),
            "weight-recommendation" to WeightRecommendationApplication()(request)
        )
    }

    private fun initializedStowageContext(
        request: RequestDTO,
        mode: StowageMode
    ): StowageContext {
        val aircraftContext = AircraftContext()
        when (val result = aircraftContext.init(request)) {
            is Ok -> {}
            is Failed -> fail("aircraft initialization failed: ${result.error.message}")
            is Fatal -> fail("aircraft initialization failed: ${result.firstError?.message}")
        }

        return StowageContext().also { context ->
            when (val result = context.init(aircraftContext, request, mode)) {
                is Ok -> {}
                is Failed -> fail("stowage initialization failed: ${result.error.message}")
                is Fatal -> fail("stowage initialization failed: ${result.firstError?.message}")
            }
        }
    }

    private fun registeredModel(
        context: StowageContext,
        mode: StowageMode
    ): LinearMetaModel<Flt64> {
        return LinearMetaModel(
            name = "demo2-solution-output-$mode",
            converter = IntoValue.Identity
        ).also { model ->
            when (val result = context.aggregation.register(mode, model)) {
                is Ok -> {}
                is Failed -> fail("base stowage registration failed: ${result.error.message}")
                is Fatal -> fail("base stowage registration failed: ${result.firstError?.message}")
            }
        }
    }

    private fun analyzedSolution(
        context: StowageContext,
        values: List<Flt64>,
        model: LinearMetaModel<Flt64>
    ): Solution {
        return when (val result = context.analyze(values, model)) {
            is Ok -> result.value!!
            is Failed -> fail("solution analysis failed: ${result.error.message}")
            is Fatal -> fail("solution analysis failed: ${result.firstError?.message}")
        }
    }

    private fun response(
        context: StowageContext,
        solution: Solution,
        request: RequestDTO
    ): ResponseDTO {
        return when (val result = context.analyze(solution, request)) {
            is Ok -> result.value!!
            is Failed -> fail("response analysis failed: ${result.error.message}")
            is Fatal -> fail("response analysis failed: ${result.firstError?.message}")
        }
    }

    private fun aircraftAggregation(request: RequestDTO): AircraftAggregation {
        return when (val result = fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.service.AggregationInitializer(request)) {
            is Ok -> result.value!!
            is Failed -> fail("aircraft initialization failed: ${result.error.message}")
            is Fatal -> fail("aircraft initialization failed: ${result.firstError?.message}")
        }
    }

    private fun loadingOrderResponse(
        aggregation: AircraftAggregation,
        request: RequestDTO
    ): LoadingOrderResponseDTO {
        return when (val result = LoadingOrderOutputExporter(aggregation)(request)) {
            is Ok -> result.value!!
            is Failed -> fail("loading-order export failed: ${result.error.message}")
            is Fatal -> fail("loading-order export failed: ${result.firstError?.message}")
        }
    }
}
