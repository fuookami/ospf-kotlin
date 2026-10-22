package fuookami.ospf.kotlin.example.framework_demo.demo2

import kotlin.test.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.framework.bpp.domain.discrete_loading.DiscreteLoadingContext
import fuookami.ospf.kotlin.framework.bpp.domain.discrete_loading.model.PositionConflict
import fuookami.ospf.kotlin.framework.bpp.domain.discrete_loading.model.AssignmentKey
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.service.AggregationInitializer as AircraftInitializer
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.service.AggregationInitializer as StowageInitializer
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.StowageMode
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto.*

/** 严格几何契约及宽窄体框架接线。 / Strict geometry contract and narrow/wide-body framework wiring. */
class Demo2DiscreteContractTest {
    @Test
    fun `predistribution total payload conserves actual and additional position weights`(): Unit = runBlocking {
        val request = RequestDTO.sample()
        val aircraft = assertIs<Ok<fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.Aggregation, *, *>>(AircraftInitializer(request)).value
        val aggregation = assertIs<Ok<fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.Aggregation, *, *>>(StowageInitializer(
            aircraftAggregation = aircraft, input = request, stowageMode = StowageMode.Predistribution
        )).value
        val model = LinearMetaModel<Flt64>(name = "conserved-payload", converter = IntoValue.Identity)
        assertIs<Ok<*, *, *>>(aggregation.register(StowageMode.Predistribution, model))
        assertIs<Ok<LinearMechanismModel<Flt64>, *, *>>(LinearMechanismModel(model)).value.use { expanded ->
            val row = expanded.constraints.single { it.name == "predistribution_payload_conservation" }
            assertEquals(Flt64(request.cargos.fold(0.0) { total, cargo -> total + cargo.weight }), row.rhs)
            assertTrue(row.lhs.isNotEmpty())
        }
    }

    @Test
    fun `cargo deck is explicit and supports lower deck sparse assignments`() {
        val sample = RequestDTO.sample()
        val lower = sample.positions[1].copy(spaceId = "lower", geometry = sample.positions[0].geometry)
        val request = sample.copy(
            spaces = sample.spaces + LoadingSpaceInput("lower", DeckLocationInput.LowForward),
            positions = listOf(sample.positions[0], lower),
            cargos = listOf(sample.cargos[0].copy(deckLocation = DeckLocationInput.LowForward, allowedPositions = setOf(lower.name)))
        )
        val loading = context(request)
        assertEquals(setOf(AssignmentKey(request.cargos.single().name, lower.name)), loading.problem.allowedAssignments)
        assertTrue(loading.problem.conflicts.isEmpty())
        assertIs<Failed<*, *, *>>(initialize(request.copy(
            positions = listOf(sample.positions[0].copy(loadedItems = listOf(request.cargos.single().name)), lower)
        )))
    }

    @Test
    fun `full load payload counts only selected assignments rather than all offered cargo`(): Unit = runBlocking {
        val request = RequestDTO.sample()
        val aggregation = assertIs<Ok<fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.Aggregation, *, *>>(initialize(request)).value
        val model = LinearMetaModel<Flt64>(name = "actual-payload", converter = IntoValue.Identity)
        assertIs<Ok<*, *, *>>(aggregation.register(StowageMode.FullLoad, model))
        assertIs<Ok<*, *, *>>(model.minimize(aggregation.payload.actualPayload.value))
        assertIs<Ok<LinearMechanismModel<Flt64>, *, *>>(LinearMechanismModel(model)).value.use { expanded ->
            val parts = expanded.objectFunction.subObjects
            assertEquals(Flt64.zero, parts.fold(Flt64.zero) { total, part -> total + part.constant })
            val assignment = aggregation.stowage.loadingContext.aggregation.assignments.entries.first()
            val coefficients = parts.flatMap { it.linearTerms() }
            val selectedWeight = coefficients.filter { it.second == assignment.value }.fold(Flt64.zero) { total, term -> total + term.first }
            assertEquals(Flt64(request.cargos.first { it.name == assignment.key.itemId }.weight), selectedWeight)
            assertTrue(coefficients.all { term -> term.second in aggregation.stowage.loadingContext.aggregation.assignments.values })
        }
    }

    @Test
    fun `serialization requires explicit units spaces layouts and position geometry`() {
        val request = RequestDTO.sample()
        val encoded = Json.encodeToString(request)
        assertEquals(request, Json.decodeFromString<RequestDTO>(encoded))
        val root = Json.parseToJsonElement(encoded).jsonObject
        for (field in listOf("lengthUnit", "weightUnit", "spaces", "layoutGroups", "positionConflicts")) {
            assertFailsWith<SerializationException>(field) {
                Json.decodeFromString<RequestDTO>(JsonObject(root - field).toString())
            }
        }
        val first = root.getValue("positions").jsonArray.first().jsonObject
        for (field in listOf("geometry", "spaceId", "partitionId", "loadingOrder", "maxLoadCount", "maxWeight")) {
            val missing = JsonObject(root + ("positions" to JsonArray(listOf(JsonObject(first - field)))))
            assertFailsWith<SerializationException>(field) { Json.decodeFromString<RequestDTO>(missing.toString()) }
        }
    }

    @Test
    fun `four aircraft use dimension appropriate geometry without list inferred coordinates`() {
        val sample = RequestDTO.sample()
        val positions = listOf(
            sample.positions[0].copy(geometry = PositionGeometryInput(0.0, 2.0, -2.0, 0.0)),
            sample.positions[1].copy(geometry = PositionGeometryInput(0.0, 2.0, 0.0, 2.0))
        )
        for (type in AircraftTypeInput.entries.filter { it != AircraftTypeInput.Unknown }) {
            val request = sample.copy(aircraftType = type, positions = positions.reversed())
            val loading = context(request)
            val narrow = type == AircraftTypeInput.B737 || type == AircraftTypeInput.B757
            assertEquals(narrow, loading.problem.conflicts.isNotEmpty(), type.name)
            val aircraft = assertIs<Ok<*, *, *>>(AircraftInitializer(request)).value
            assertNotNull(aircraft)
        }
        val separate = sample.copy(
            spaces = sample.spaces + LoadingSpaceInput("lower", DeckLocationInput.LowForward),
            positions = listOf(positions[0], positions[1].copy(spaceId = "lower"))
        )
        assertTrue(context(separate).problem.conflicts.isEmpty())
    }

    @Test
    fun `explicit conflicts cannot remove geometry and fixed layout must be compatible`() {
        val sample = RequestDTO.sample()
        val first = sample.positions[0].copy(loadedItems = listOf("C1"))
        val second = sample.positions[1].copy(geometry = first.geometry, loadedItems = listOf("C2"))
        val conflict = context(sample.copy(positions = listOf(first.copy(loadedItems = emptyList()), second.copy(loadedItems = emptyList()))))
        assertTrue(conflict.problem.conflicts.contains(PositionConflict("P1", "P2")))
        val conflictingFixed = sample.copy(positions = listOf(first, second))
        assertIs<Failed<*, *, *>>(initialize(conflictingFixed))
        val layoutRequest = sample.copy(
            positions = listOf(first, sample.positions[1].copy(loadedItems = listOf("C2"))),
            layoutGroups = listOf(LayoutGroupInput(
                id = "whole-aircraft",
                positions = setOf("P1", "P2"),
                layouts = listOf(LoadingLayoutInput("first", setOf("P1")), LoadingLayoutInput("second", setOf("P2")))
            ))
        )
        assertIs<Failed<*, *, *>>(initialize(layoutRequest))
    }

    private fun initialize(request: RequestDTO): Ret<fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.Aggregation> {
        val aircraft = when (val result = AircraftInitializer(request)) {
            is Ok -> result.value
            else -> fail("Aircraft initialization failed: $result")
        }
        return StowageInitializer(aircraft, request, StowageMode.FullLoad)
    }

    private fun context(request: RequestDTO): DiscreteLoadingContext<Flt64> {
        return when (val result = initialize(request)) {
            is Ok -> result.value.stowage.loadingContext
            is Failed -> fail("Stowage initialization failed: ${result.error.message}")
            is Fatal -> fail("Stowage initialization failed: ${result.errors}")
        }
    }
}
