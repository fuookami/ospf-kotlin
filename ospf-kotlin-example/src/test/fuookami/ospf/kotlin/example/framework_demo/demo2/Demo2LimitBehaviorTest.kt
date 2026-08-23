@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo2

import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.*
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.Aggregation as AircraftAggregation
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.airworthiness_security.Aggregation as AirworthinessAggregation
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.airworthiness_security.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.airworthiness_security.service.PipelineListGenerator as AirworthinessPipelineListGenerator
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.airworthiness_security.service.limits.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.express_effectiveness.Aggregation as ExpressAggregation
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.express_effectiveness.service.AggregationInitializer as ExpressAggregationInitializer
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.express_effectiveness.service.PipelineListGenerator as ExpressPipelineListGenerator
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.express_effectiveness.service.limits.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.loading_effectiveness.Aggregation as LoadingAggregation
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.loading_effectiveness.service.AggregationInitializer as LoadingAggregationInitializer
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.loading_effectiveness.service.PipelineListGenerator as LoadingPipelineListGenerator
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.loading_effectiveness.service.limits.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.mac.model.Torque
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.payload_maximization.Aggregation as PayloadMaximizationAggregation
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.payload_maximization.service.PipelineListGenerator as PayloadMaximizationPipelineListGenerator
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.recommended_weight_equalization.service.AggregationInitializer as RecommendedWeightEqualizationAggregationInitializer
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.recommended_weight_equalization.service.PipelineListGenerator as RecommendedWeightEqualizationPipelineListGenerator
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.recommended_weight_equalization.service.limits.ItemOrderLimit
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.recommended_weight_equalization.service.limits.RecommendedWeightDeviationObjective
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.Aggregation as StowageAggregation
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.service.PipelineListGenerator as StowagePipelineListGenerator
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.service.limits.AppointmentLimit
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.service.limits.LoadWeightLimit
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.service.limits.PredicateLoadWeightLimit
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.value_range.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.multiarray.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.utils.functional.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/** Demo2 四项限制的模型行为测试。Model behavior tests for the four Demo2 limits. */
class Demo2LimitBehaviorTest {

    @Test
    fun `adjacent gap adds both directional constraints for every adjacent pair`() {
        val fixture = limitFixture(itemCount = 1, positionCount = 3)
        val model = fixture.model()

        val result = AdjacentGapLimit(
            positions = fixture.positions,
            load = fixture.load,
            maxAdjacentLoadGap = 5.0
        )(model)

        assertTrue(result is Ok)
        assertEquals(4, model.constraints.size)
        assertEquals(listOf(Comparison.LE, Comparison.LE, Comparison.LE, Comparison.LE), model.relationConstraints.map { it.sign })
        assertEquals(listOf("adjacent_gap_limit_pos_0", "adjacent_gap_limit_neg_0", "adjacent_gap_limit_pos_1", "adjacent_gap_limit_neg_1"), model.relationConstraints.map { it.constraintName })
        assertTrue(model.relationConstraints.all { it.inequality.rhs.constant == Flt64(5.0) })
    }

    @Test
    fun `must ship adds exactly one equality per selected cargo`() {
        val fixture = limitFixture(itemCount = 2, positionCount = 2)
        val model = fixture.model()

        val result = MustShipLimit(
            items = fixture.items,
            positions = fixture.positions,
            stowage = fixture.stowage,
            mustShipIndices = listOf(0, 1)
        )(model)

        assertTrue(result is Ok)
        assertEquals(2, model.constraints.size)
        assertTrue(model.relationConstraints.all { it.sign == Comparison.EQ })
        assertTrue(model.relationConstraints.all { it.inequality.rhs.constant == Flt64.one })
        assertTrue(model.relationConstraints.all { it.constraintName.startsWith("must_ship_limit_") })
    }

    @Test
    fun `appointment only constrains explicitly appointed cargo`() {
        val fixture = limitFixture(
            itemCount = 2,
            positionCount = 2,
            recommendedWeightNeeded = true
        )
        val emptyModel = fixture.model()
        val emptyResult = AppointmentLimit(
            items = fixture.items,
            positions = fixture.positions,
            appointment = Appointment(emptyMap()),
            stowage = fixture.stowage
        )(emptyModel)

        assertTrue(emptyResult is Ok)
        assertEquals(0, emptyModel.constraints.size)

        val appointedModel = fixture.model()
        val appointedResult = AppointmentLimit(
            items = fixture.items,
            positions = fixture.positions,
            appointment = Appointment(mapOf(fixture.items[0] to fixture.positions[1])),
            stowage = fixture.stowage
        )(appointedModel)

        assertTrue(appointedResult is Ok)
        val constraint = appointedModel.relationConstraints.single()
        assertEquals(Comparison.EQ, constraint.sign)
        assertEquals(Flt64.one, constraint.inequality.rhs.constant)
        assertEquals("appointment_limit_I0_P1", constraint.constraintName)
    }

    @Test
    fun `priority order adds a constraint only for strictly higher priority pair`() {
        val fixture = limitFixture(itemCount = 2, positionCount = 2, priorities = listOf(10UL, 1UL))
        val model = fixture.model()

        val result = PriorityOrderLimit(
            items = fixture.items,
            positions = fixture.positions,
            stowage = fixture.stowage
        )(model)

        assertTrue(result is Ok)
        assertEquals(1, model.constraints.size)
        val constraint = model.relationConstraints.single()
        assertEquals(Comparison.LE, constraint.sign)
        assertEquals(Flt64(4.0), constraint.inequality.rhs.constant)
        assertEquals(listOf(2.0, 2.0, 3.0, 1.0), constraint.inequality.lhs.monomials.map { it.coefficient.toDouble() })
    }

    @Test
    fun `source early adds one lower-bound constraint for a multi-cargo source`() {
        val fixture = limitFixture(itemCount = 2, positionCount = 2)
        val model = fixture.model()

        val result = SourceEarlyLimit(
            items = fixture.items,
            positions = fixture.positions,
            stowage = fixture.stowage,
            cargosBySource = mapOf("SRC" to listOf(0, 1)),
            earlyEnd = 0
        )(model)

        assertTrue(result is Ok)
        assertEquals(1, model.constraints.size)
        val constraint = model.relationConstraints.single()
        assertEquals(Comparison.GE, constraint.sign)
        assertEquals(Flt64.one, constraint.inequality.rhs.constant)
        assertEquals("source_early_limit_SRC", constraint.constraintName)
    }

    @Test
    fun `single item source and equal priorities do not create optional constraints`() {
        val fixture = limitFixture(itemCount = 2, positionCount = 2, priorities = listOf(1UL, 1UL))

        val priorityModel = fixture.model()
        val priorityResult = PriorityOrderLimit(
            items = fixture.items,
            positions = fixture.positions,
            stowage = fixture.stowage
        )(priorityModel)

        val sourceModel = fixture.model()
        val sourceResult = SourceEarlyLimit(
            items = fixture.items,
            positions = fixture.positions,
            stowage = fixture.stowage,
            cargosBySource = mapOf("SRC" to listOf(0)),
            earlyEnd = 0
        )(sourceModel)

        assertTrue(priorityResult is Ok)
        assertTrue(sourceResult is Ok)
        assertEquals(0, priorityModel.constraints.size)
        assertEquals(0, sourceModel.constraints.size)
    }

    @Test
    fun `adjacent gap and must ship skip inapplicable inputs`() {
        val adjacentFixture = limitFixture(itemCount = 1, positionCount = 1)
        val adjacentModel = adjacentFixture.model()
        val adjacentResult = AdjacentGapLimit(
            positions = adjacentFixture.positions,
            load = adjacentFixture.load,
            maxAdjacentLoadGap = 5.0
        )(adjacentModel)

        val mustShipFixture = limitFixture(itemCount = 1, positionCount = 2)
        val mustShipModel = mustShipFixture.model()
        val mustShipResult = MustShipLimit(
            items = mustShipFixture.items,
            positions = mustShipFixture.positions,
            stowage = mustShipFixture.stowage,
            mustShipIndices = emptyList()
        )(mustShipModel)

        assertTrue(adjacentResult is Ok)
        assertTrue(mustShipResult is Ok)
        assertEquals(0, adjacentModel.constraints.size)
        assertEquals(0, mustShipModel.constraints.size)
    }

    @Test
    fun `airworthiness pipeline registers adjacent gap only when configured`() {
        val fixture = limitFixture(itemCount = 2, positionCount = 2)
        val aggregation = fixture.airworthinessAggregation(maxAdjacentLoadGap = 5.0)
        val pipelines = when (val result = AirworthinessPipelineListGenerator(aggregation)(StowageMode.WeightRecommendation)) {
            is Ok -> result.value!!
            is Failed -> fail("airworthiness pipeline generation failed: ${result.error}")
            is Fatal -> fail("airworthiness pipeline generation failed: ${result.errors}")
        }

        val adjacentGap = pipelines.filterIsInstance<AdjacentGapLimit>().single()
        val model = fixture.model()
        assertTrue(adjacentGap(model) is Ok)
        assertEquals(2, model.constraints.size)

        val disabledAggregation = fixture.airworthinessAggregation(maxAdjacentLoadGap = null)
        val disabledPipelines = when (val result = AirworthinessPipelineListGenerator(disabledAggregation)(StowageMode.WeightRecommendation)) {
            is Ok -> result.value!!
            is Failed -> fail("airworthiness pipeline generation failed: ${result.error}")
            is Fatal -> fail("airworthiness pipeline generation failed: ${result.errors}")
        }
        assertTrue(disabledPipelines.none { it is AdjacentGapLimit })
    }

    @Test
    fun `express pipeline registers must ship only for selected cargo`() {
        val fixture = limitFixture(itemCount = 2, positionCount = 2)
        val aggregation = ExpressAggregation(
            stowageMode = StowageMode.WeightRecommendation,
            items = fixture.items,
            positions = fixture.positions,
            stowage = fixture.stowage,
            mustShipIndices = listOf(0, 1)
        )
        val pipelines = when (val result = ExpressPipelineListGenerator(aggregation)(StowageMode.WeightRecommendation, testParameter())) {
            is Ok -> result.value!!
            is Failed -> fail("express pipeline generation failed: ${result.error}")
            is Fatal -> fail("express pipeline generation failed: ${result.errors}")
        }

        val mustShip = pipelines.filterIsInstance<MustShipLimit>().single()
        val model = fixture.model()
        assertTrue(mustShip(model) is Ok)
        assertEquals(2, model.constraints.size)

        val disabledAggregation = ExpressAggregation(
            stowageMode = StowageMode.WeightRecommendation,
            items = fixture.items,
            positions = fixture.positions,
            stowage = fixture.stowage,
            mustShipIndices = emptyList()
        )
        val disabledPipelines = when (val result = ExpressPipelineListGenerator(disabledAggregation)(StowageMode.WeightRecommendation, testParameter())) {
            is Ok -> result.value!!
            is Failed -> fail("express pipeline generation failed: ${result.error}")
            is Fatal -> fail("express pipeline generation failed: ${result.errors}")
        }
        assertTrue(disabledPipelines.none { it is MustShipLimit })

        val predistributionPipelines = when (val result = ExpressPipelineListGenerator(aggregation)(StowageMode.Predistribution, testParameter())) {
            is Ok -> result.value!!
            is Failed -> fail("express pipeline generation failed: ${result.error}")
            is Fatal -> fail("express pipeline generation failed: ${result.errors}")
        }
        assertTrue(predistributionPipelines.none { it is MustShipLimit })
    }

    @Test
    fun `loading pipeline registers priority and source early limits`() {
        val fixture = limitFixture(itemCount = 2, positionCount = 2)
        val aggregation = LoadingAggregation(
            aircraftModel = fixture.aircraftModel,
            stowageMode = StowageMode.WeightRecommendation,
            flight = testFlight(),
            items = fixture.items,
            positions = fixture.positions,
            trailers = emptyList(),
            stowage = fixture.stowage,
            load = fixture.load,
            cargosBySource = mapOf("SRC" to listOf(0, 1)),
            earlyEnd = 0
        )
        val pipelines = when (val result = LoadingPipelineListGenerator(aggregation)(StowageMode.WeightRecommendation, testParameter())) {
            is Ok -> result.value!!
            is Failed -> fail("loading pipeline generation failed: ${result.error}")
            is Fatal -> fail("loading pipeline generation failed: ${result.errors}")
        }

        val priority = pipelines.filterIsInstance<PriorityOrderLimit>().single()
        val sourceEarly = pipelines.filterIsInstance<SourceEarlyLimit>().single()
        val model = fixture.model()
        assertTrue(priority(model) is Ok)
        assertTrue(sourceEarly(model) is Ok)
        assertEquals(2, model.constraints.size)

        val singleSourceAggregation = LoadingAggregation(
            aircraftModel = fixture.aircraftModel,
            stowageMode = StowageMode.WeightRecommendation,
            flight = testFlight(),
            items = fixture.items,
            positions = fixture.positions,
            trailers = emptyList(),
            stowage = fixture.stowage,
            load = fixture.load,
            cargosBySource = mapOf("SRC" to listOf(0)),
            earlyEnd = 0
        )
        val singleSourcePipelines = when (val result = LoadingPipelineListGenerator(singleSourceAggregation)(StowageMode.WeightRecommendation, testParameter())) {
            is Ok -> result.value!!
            is Failed -> fail("loading pipeline generation failed: ${result.error}")
            is Fatal -> fail("loading pipeline generation failed: ${result.errors}")
        }
        val singleSourceLimit = singleSourcePipelines.filterIsInstance<SourceEarlyLimit>().single()
        val before = model.constraints.size
        assertTrue(singleSourceLimit(model) is Ok)
        assertEquals(before, model.constraints.size)

        val predistributionPipelines = when (val result = LoadingPipelineListGenerator(aggregation)(StowageMode.Predistribution, testParameter())) {
            is Ok -> result.value!!
            is Failed -> fail("loading pipeline generation failed: ${result.error}")
            is Fatal -> fail("loading pipeline generation failed: ${result.errors}")
        }
        assertTrue(predistributionPipelines.any { it is PriorityOrderLimit })
        assertTrue(predistributionPipelines.none { it is SourceEarlyLimit })
        assertEquals(2.0, aggregation.bigM)
    }

    @Test
    fun `request derives Rust aligned default objective weights`() {
        val parameter = RequestDTO.sample().parameter

        assertEquals(
            List(22) { Flt64.one },
            listOf(
                parameter.macRangeC,
                parameter.longitudinalBalance,
                parameter.B737LongitudinalBalance,
                parameter.lateralBalance,
                parameter.horizontalStabilizerWarn,
                parameter.ballastWeight,
                parameter.emptyHated,
                parameter.besideDoorMainPosition,
                parameter.dividedEmpty,
                parameter.adviceLoadAmount,
                parameter.adviceLoadWeight,
                parameter.sameFlowTransferIn,
                parameter.sameFlowTransferOut,
                parameter.itemOrder,
                parameter.trailerChange,
                parameter.trailerCircling,
                parameter.priority,
                parameter.priorityCategory,
                parameter.experimentalLongitudinalBalance,
                parameter.redundancyRange,
                parameter.weightRecommendationBalance,
                parameter.weightRecommendationPayload
            )
        )
    }

    @Test
    fun `effectiveness initializers derive Rust aligned aggregation data`() {
        val fixture = limitFixture(itemCount = 3, positionCount = 5)
        val input = RequestDTO(
            id = "derived-aggregation",
            cargos = listOf(
                CargoInput(name = "C0", weight = 1.0, priority = 8, source = "SRC-A", destination = "DST"),
                CargoInput(name = "C1", weight = 1.0, priority = 7, source = "SRC-B", destination = "DST"),
                CargoInput(name = "C2", weight = 1.0, priority = 9, source = "SRC-A", destination = "DST")
            )
        )
        val aircraftAggregation = fixture.aircraftAggregation()
        val stowageAggregation = fixture.stowageAggregation()

        val expressAggregation = when (val result = ExpressAggregationInitializer(
            aircraftAggregation = aircraftAggregation,
            stowageAggregation = stowageAggregation,
            input = input,
            stowageMode = StowageMode.WeightRecommendation
        )) {
            is Ok -> result.value!!
            is Failed -> fail("express aggregation initialization failed: ${result.error}")
            is Fatal -> fail("express aggregation initialization failed: ${result.errors}")
        }
        val loadingAggregation = when (val result = LoadingAggregationInitializer(
            aircraftAggregation = aircraftAggregation,
            stowageAggregation = stowageAggregation,
            input = input,
            stowageMode = StowageMode.WeightRecommendation
        )) {
            is Ok -> result.value!!
            is Failed -> fail("loading aggregation initialization failed: ${result.error}")
            is Fatal -> fail("loading aggregation initialization failed: ${result.errors}")
        }

        assertEquals(listOf(0, 2), expressAggregation.mustShipIndices)
        assertEquals(mapOf("SRC-A" to listOf(0, 2), "SRC-B" to listOf(1)), loadingAggregation.cargosBySource)
        assertEquals(2, loadingAggregation.earlyEnd)
        assertEquals(5.0, loadingAggregation.bigM)
    }

    @Test
    fun `express pipeline registers the configured priority objective`() {
        val fixture = limitFixture(itemCount = 1, positionCount = 2, priorities = listOf(2UL))
        val aggregation = ExpressAggregation(
            stowageMode = StowageMode.Predistribution,
            items = fixture.items,
            positions = fixture.positions,
            stowage = fixture.stowage
        )
        val parameter = testParameter().copy(priority = Flt64(4.0))
        val pipelines = when (val result = ExpressPipelineListGenerator(aggregation)(StowageMode.Predistribution, parameter)) {
            is Ok -> result.value!!
            is Failed -> fail("express pipeline generation failed: ${result.error}")
            is Fatal -> fail("express pipeline generation failed: ${result.errors}")
        }
        val priorityLimit = pipelines.filterIsInstance<ItemPriorityLimit>().single()
        val model = fixture.model()

        assertTrue(model.add(fixture.stowage.stowage) is Ok)
        assertTrue(priorityLimit(model) is Ok)
        val mechanismResult = runBlocking {
            LinearMechanismModel.invoke<Flt64>(metaModel = model)
        }
        assertTrue(mechanismResult is Ok)
        val objective = mechanismResult.value!!.objectFunction.subObjects.single()
        assertEquals("item priority", objective.name)
        assertEquals(ObjectCategory.Maximum, objective.category)
    }

    @Test
    fun `item order creates hard constraints for strict priority and position pairs only`() {
        val fixture = limitFixture(itemCount = 3, positionCount = 3, priorities = listOf(10UL, 10UL, 1UL))
        val model = fixture.model()

        val result = ItemOrderLimit(
            items = fixture.items,
            positions = fixture.positions,
            stowage = fixture.stowage
        )(model)

        assertTrue(result is Ok)
        assertEquals(6, model.constraints.size)
        assertTrue(model.relationConstraints.all { it.sign == Comparison.LE })
        assertTrue(model.relationConstraints.all { it.inequality.rhs.constant == Flt64.one })
        assertTrue(model.relationConstraints.all { it.constraintName.startsWith("item_order_limit_") })
    }

    @Test
    fun `recommended weight pipeline is only active in recommendation mode`() {
        val fixture = limitFixture(
            itemCount = 2,
            positionCount = 2,
            recommendedWeightNeeded = true
        )
        val aggregation = when (val result = RecommendedWeightEqualizationAggregationInitializer(
            aircraftAggregation = fixture.aircraftAggregation(),
            stowageAggregation = fixture.stowageAggregation(),
            input = RequestDTO.sample()
        )) {
            is Ok -> result.value!!
            is Failed -> fail("recommended-weight aggregation initialization failed: ${result.error}")
            is Fatal -> fail("recommended-weight aggregation initialization failed: ${result.errors}")
        }
        val generator = RecommendedWeightEqualizationPipelineListGenerator(aggregation)

        val fullLoad = when (val result = generator(StowageMode.FullLoad, testParameter())) {
            is Ok -> result.value!!
            is Failed -> fail("recommended-weight pipeline generation failed: ${result.error}")
            is Fatal -> fail("recommended-weight pipeline generation failed: ${result.errors}")
        }
        val recommendation = when (val result = generator(StowageMode.WeightRecommendation, testParameter())) {
            is Ok -> result.value!!
            is Failed -> fail("recommended-weight pipeline generation failed: ${result.error}")
            is Fatal -> fail("recommended-weight pipeline generation failed: ${result.errors}")
        }

        assertTrue(fullLoad.isEmpty())
        assertEquals(4, recommendation.size)
        assertTrue(recommendation.any { it is ItemOrderLimit })
        assertTrue(recommendation.any { it is RecommendedWeightDeviationObjective })
    }

    @Test
    fun `weight recommendation priorities register the intended objective coefficients`() {
        val fixture = limitFixture(
            itemCount = 2,
            positionCount = 2,
            recommendedWeightNeeded = true,
            withMultiLoadingSchema = true
        )
        val request = RequestDTO.sample().copy(
            weightRecommendationObjective = WeightRecommendationObjectiveConfig(
                balancePriority = 17.0,
                payloadPriority = 3.0
            )
        )
        val parameter = request.parameter
        assertEquals(Flt64(17.0), parameter.weightRecommendationBalance)
        assertEquals(Flt64(3.0), parameter.weightRecommendationPayload)

        val stowageAggregation = fixture.stowageAggregation()
        val model = fixture.model()
        assertTrue(stowageAggregation.stowage.register(model) is Ok)
        assertTrue(stowageAggregation.load.register(model) is Ok)
        fixture.useRecommendedWeightsAsEstimatedLoad(stowageAggregation)
        assertTrue(stowageAggregation.payload.register(StowageMode.WeightRecommendation, model) is Ok)
        assertEquals(2, model.tokens.tokens.count { it.variable.name.startsWith("z_") })
        assertNotNull(model.tokens.find(stowageAggregation.load.z[0].value))
        assertNotNull(model.tokens.find(stowageAggregation.load.z[1].value))
        assertEquals(2, stowageAggregation.payload.estimatePayload.value.polynomial.monomials.size)

        val recommendedAggregation = when (val result = RecommendedWeightEqualizationAggregationInitializer(
            aircraftAggregation = fixture.aircraftAggregation(),
            stowageAggregation = stowageAggregation,
            input = request
        )) {
            is Ok -> result.value!!
            is Failed -> fail("recommended-weight aggregation initialization failed: ${result.error}")
            is Fatal -> fail("recommended-weight aggregation initialization failed: ${result.errors}")
        }
        val recommendedPipelines = when (val result = RecommendedWeightEqualizationPipelineListGenerator(recommendedAggregation)(
            stowageMode = StowageMode.WeightRecommendation,
            parameter = parameter
        )) {
            is Ok -> result.value!!
            is Failed -> fail("recommended-weight pipeline generation failed: ${result.error}")
            is Fatal -> fail("recommended-weight pipeline generation failed: ${result.errors}")
        }
        val payloadPipelines = when (val result = PayloadMaximizationPipelineListGenerator(
            PayloadMaximizationAggregation(
                aircraftModel = fixture.aircraftModel,
                payload = stowageAggregation.payload
            )
        )(
            stowageMode = StowageMode.WeightRecommendation,
            parameter = parameter
        )) {
            is Ok -> result.value!!
            is Failed -> fail("payload-maximization pipeline generation failed: ${result.error}")
            is Fatal -> fail("payload-maximization pipeline generation failed: ${result.errors}")
        }
        for (pipeline in recommendedPipelines + payloadPipelines) {
            assertTrue(pipeline(model) is Ok)
        }

        val mechanismResult = runBlocking {
            LinearMechanismModel.invoke<Flt64>(metaModel = model)
        }
        assertTrue(mechanismResult is Ok)
        val objectives = mechanismResult.value!!.objectFunction.subObjects.associateBy { it.name }
        val balanceObjective = objectives["recommended_weight_deviation_objective"]!!
        val payloadObjective = objectives["max_payload_limit"]!!
        assertEquals(ObjectCategory.Maximum, balanceObjective.category)
        assertTrue(balanceObjective.cells.isNotEmpty())
        assertTrue(balanceObjective.cells.all { it.coefficient == Flt64(-17.0) })
        assertEquals(fixture.positions.size, payloadObjective.cells.size)
        assertTrue(payloadObjective.cells.all { it.coefficient == Flt64(3.0) })
        assertTrue(payloadObjective.cells.all { it.token.variable.name.startsWith("z_") })
    }

    @Test
    fun `stowage pipeline registers load weight limit`() {
        val fixture = limitFixture(itemCount = 2, positionCount = 2)
        val aggregation = fixture.stowageAggregation()
        val pipelines = when (val result = StowagePipelineListGenerator(aggregation)(StowageMode.WeightRecommendation)) {
            is Ok -> result.value!!
            is Failed -> fail("stowage pipeline generation failed: ${result.error}")
            is Fatal -> fail("stowage pipeline generation failed: ${result.errors}")
        }

        val loadWeight = pipelines.filterIsInstance<LoadWeightLimit>().single()
        val model = fixture.model()
        assertTrue(loadWeight(model) is Ok)
        assertEquals(2, model.constraints.size)
        assertTrue(pipelines.none { it::class.simpleName == "CapacityLimit" })
    }

    @Test
    fun `predicate load weight limit adds a direct upper bound for each predicate position`() {
        val fixture = limitFixture(
            itemCount = 1,
            positionCount = 2,
            predicateWeightNeeded = true
        )
        val model = fixture.model()
        assertTrue(fixture.stowage.register(model) is Ok)
        assertTrue(fixture.load.register(model) is Ok)

        val result = PredicateLoadWeightLimit(
            positions = fixture.positions,
            load = fixture.load
        )(model)

        assertTrue(result is Ok)
        val constraints = model.relationConstraints.filter {
            it.constraintName.startsWith("predicate_load_weight_limit_")
        }
        assertEquals(2, constraints.size)
        assertTrue(constraints.all { it.sign == Comparison.LE })
        assertTrue(constraints.all { it.inequality.rhs.constant == Flt64(100.0) })
        assertTrue(constraints.all { relation ->
            relation.inequality.lhs.monomials.size == 1 &&
                relation.inequality.lhs.monomials.single().coefficient == Flt64.one &&
                relation.inequality.lhs.monomials.single().symbol.name.startsWith("y_")
        })
    }

    private fun limitFixture(
        itemCount: Int,
        positionCount: Int,
        priorities: List<ULong> = List(itemCount) { (itemCount - it).toULong() },
        recommendedWeightNeeded: Boolean = false,
        predicateWeightNeeded: Boolean = false,
        withMultiLoadingSchema: Boolean = false
    ): LimitFixture {
        val aircraftModel = AircraftModel(AircraftMinorModel("B737"))
        val positions = List(positionCount) { index ->
            val basePosition = BasePosition(
                aircraftModel = aircraftModel,
                id = UInt64(index.toULong()),
                spaceName = "P$index",
                sizeCode = "N",
                frontArm = Quantity(Flt64(index.toDouble()), aircraftModel.lengthUnit),
                backArm = Quantity(Flt64((index + 1).toDouble()), aircraftModel.lengthUnit),
                leftArm = Quantity(Flt64(-1.0), aircraftModel.lengthUnit),
                rightArm = Quantity(Flt64(1.0), aircraftModel.lengthUnit),
                volume = Quantity(Flt64(1.0), aircraftModel.volumeUnit),
                offsets = HashMap(),
                location = PositionLocation.normalMain,
                linearLoadingOrder = UInt8(index.toUByte())
            )
            basePosition._loadingOrder = LoadingOrder(
                location = DeckLocation.Main,
                order = UInt8(index.toUByte()),
                directPrec = emptySet(),
                directSucc = emptySet()
            )
            val maxWeight = Quantity(Flt64(100.0), aircraftModel.weightUnit)
            fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.Position(
                base = basePosition,
                loadedItems = emptySet(),
                ala = null,
                mla = if (predicateWeightNeeded) UInt64.one else UInt64(100UL),
                alw = null,
                mlw = PositionMaximumLoadWeight(
                    aircraftModel = aircraftModel,
                    position = basePosition,
                    mlw = maxWeight,
                    segments = emptyList(),
                    mzfw = maxWeight
                ),
                plw = if (predicateWeightNeeded) {
                    PositionPredicateLoadWeight(
                        plw = Quantity(Flt64.zero, aircraftModel.weightUnit),
                        min = Quantity(Flt64.zero, aircraftModel.weightUnit),
                        max = maxWeight
                    )
                } else {
                    null
                },
                type = PositionType(emptySet()),
                taboo = PositionStowageTaboo(emptyList()),
                status = PositionStatus(
                    code = PositionStatusCode.Unloaded,
                    available = true,
                    stowageNeeded = true,
                    adjustmentNeeded = false,
                    predicateWeightNeeded = predicateWeightNeeded,
                    recommendedWeightNeeded = recommendedWeightNeeded
                )
            )
        }
        val items = List(itemCount) { index ->
            Item(
                id = "I$index",
                destination = IATA("DST"),
                source = FlightNo("SRC"),
                uld = null,
                weight = Quantity(Flt64(1.0), aircraftModel.weightUnit),
                location = ItemLocation.normalMain,
                cargo = ItemCargo(
                    types = setOf(CargoType("GENERAL")),
                    priority = CargoPriority(
                        name = "P$index",
                        priority = UInt64(priorities[index]),
                        category = CargoPriorityCategory.Normal
                    )
                ),
                status = ItemStatus.Optional,
                order = null
            )
        }
        val stowage = Stowage(items, positions)
        stowage.x = BinVariable2("x", Shape2(items.size, positions.size))
        stowage.stowage = LinearIntermediateSymbols2(
            "stowage",
            Shape2(items.size, positions.size)
        ) { _, index ->
            LinearExpressionSymbol(Flt64.zero, name = "stowage_${index.joinToString("_")}")
        }
        val formula = Formula(
            aircraftModel = aircraftModel,
            lip = Quantity(Flt64.zero, aircraftModel.lengthUnit),
            chord = Quantity(Flt64.one, aircraftModel.lengthUnit),
            standardDatum = Quantity(Flt64.zero, aircraftModel.lengthUnit),
            forceDistanceCoefficient = Flt64.one,
            doiCorrection = Quantity(Flt64.zero, aircraftModel.torqueUnit)
        )
        val load = Load(
            aircraftModel = aircraftModel,
            formula = formula,
            items = items,
            positions = positions,
            withMultiLoadingSchema = withMultiLoadingSchema,
            stowage = stowage
        )
        load.estimateLoadWeight = QuantityLinearIntermediateSymbols1(
            "load_weight",
            Shape1(positions.size)
        ) { _, index ->
            Quantity(
                LinearExpressionSymbol(Flt64.zero, name = "load_weight_${index.joinToString("_")}"),
                aircraftModel.weightUnit
            )
        }
        return LimitFixture(
            aircraftModel = aircraftModel,
            formula = formula,
            withMultiLoadingSchema = withMultiLoadingSchema,
            items = items,
            positions = positions,
            stowage = stowage,
            load = load
        )
    }

    private fun LimitFixture.airworthinessAggregation(maxAdjacentLoadGap: Double?): AirworthinessAggregation {
        val fuselage = Fuselage(
            liferaft = null,
            dow = Quantity(Flt64.zero, aircraftModel.weightUnit),
            doi = Quantity(Flt64.zero, aircraftModel.torqueUnit),
            balancedArm = Quantity(Flt64.zero, aircraftModel.lengthUnit)
        )
        val payload = Payload(
            plannedPayload = Quantity(Flt64.zero, aircraftModel.weightUnit),
            maxPayload = Quantity(Flt64(100.0), aircraftModel.weightUnit),
            computedPayload = null,
            aircraftModel = aircraftModel,
            items = items,
            positions = positions,
            load = load
        )
        val totalWeight = TotalWeight(
            maxTotalWeight = emptyMap(),
            computedTotalWeight = emptyMap(),
            aircraftModel = aircraftModel,
            fuselage = fuselage,
            fuel = emptyMap(),
            payload = payload
        )
        return AirworthinessAggregation(
            aircraftModel = aircraftModel,
            fuselage = fuselage,
            positions = positions,
            linearDensityLimitZones = emptyList(),
            surfaceDensityLimitZones = emptyList(),
            maxZoneLoadWeight = MaxZoneLoadWeight(aircraftModel, emptyList(), load),
            maxCumulativeLoadWeight = MaxCumulativeLoadWeight(emptyList()),
            minPayload = Quantity(Flt64.zero, aircraftModel.weightUnit),
            envelopeLongitudinalMomentMin = Quantity(Flt64(-100.0), aircraftModel.torqueUnit),
            envelopeLongitudinalMomentMax = Quantity(Flt64(100.0), aircraftModel.torqueUnit),
            targetLongitudinalMoment = Quantity(Flt64.zero, aircraftModel.torqueUnit),
            maxLongitudinalMomentDeviation = Quantity(Flt64(100.0), aircraftModel.torqueUnit),
            maxLateralImbalance = Quantity(Flt64(100.0), aircraftModel.torqueUnit),
            maxCLIMPoints = null,
            minLowPayloadPoints = emptyList(),
            envelopeBuilders = { _, _ -> emptyList() },
            load = load,
            payload = payload,
            totalWeight = totalWeight,
            ballast = null,
            torque = Torque(
                aircraftModel = aircraftModel,
                fuselage = fuselage,
                fuel = emptyMap(),
                formula = formula,
                positions = positions,
                load = load
            ),
            horizontalStabilizers = emptyMap(),
            stowage = stowage,
            maxAdjacentLoadGap = maxAdjacentLoadGap
        )
    }

    private fun LimitFixture.aircraftAggregation(): AircraftAggregation {
        return AircraftAggregation(
            regNo = RegNo("B-TEST"),
            aircraftModel = aircraftModel,
            formula = formula,
            fuselage = Fuselage(
                liferaft = null,
                dow = Quantity(Flt64.zero, aircraftModel.weightUnit),
                doi = Quantity(Flt64.zero, aircraftModel.torqueUnit),
                balancedArm = Quantity(Flt64.zero, aircraftModel.lengthUnit)
            ),
            fuelTanks = Quantity(
                value = FuelTank(
                    type = FuelTankType.Main,
                    name = "T1",
                    maxVolume = Quantity(Flt64.zero, aircraftModel.volumeUnit),
                    balancedArm = FuelTankBalancedArm(emptyList())
                ),
                unit = NoneUnit
            ),
            fuel = emptyMap(),
            decks = emptyList(),
            neighbours = HashMap()
        )
    }

    private fun LimitFixture.stowageAggregation(): StowageAggregation {
        val fuselage = Fuselage(
            liferaft = null,
            dow = Quantity(Flt64.zero, aircraftModel.weightUnit),
            doi = Quantity(Flt64.zero, aircraftModel.torqueUnit),
            balancedArm = Quantity(Flt64.zero, aircraftModel.lengthUnit)
        )
        val aggregation = StowageAggregation(
            aircraftModel = aircraftModel,
            formula = formula,
            fuselage = fuselage,
            fuel = emptyMap(),
            flight = testFlight(),
            items = items,
            positions = positions,
            plannedPayload = Quantity(Flt64.zero, aircraftModel.weightUnit),
            maxPayload = Quantity(Flt64(100.0), aircraftModel.weightUnit),
            computedPayload = null,
            maxTotalWeight = emptyMap(),
            computedTotalWeight = emptyMap(),
            withMultiLoadingSchema = withMultiLoadingSchema,
            appointment = Appointment(emptyMap()),
            biologicalLimit = BiologicalLimit(emptyList(), emptyList(), emptyList()),
            neighbours = hashMapOf(
                NeighbourType.Physics to listOf(
                    Neighbour(NeighbourType.Physics, positions[0].base to positions[1].base)
                ),
                NeighbourType.IndirectPhysics to listOf(
                    Neighbour(NeighbourType.IndirectPhysics, positions[0].base to positions[1].base)
                )
            )
        )
        aggregation.load.estimateLoadWeight = load.estimateLoadWeight
        aggregation.maxLoadWeight.maxLoadWeight = QuantityLinearIntermediateSymbols1(
            "max_load_weight",
            Shape1(positions.size)
        ) { _, index ->
            Quantity(
                LinearExpressionSymbol(Flt64(100.0), name = "max_load_weight_${index.joinToString("_")}"),
                aircraftModel.weightUnit
            )
        }
        return aggregation
    }

    private fun LimitFixture.useRecommendedWeightsAsEstimatedLoad(aggregation: StowageAggregation) {
        aggregation.load.estimateLoadWeight = QuantityLinearIntermediateSymbols1(
            name = "objective_load_weight",
            shape = Shape1(positions.size)
        ) { j, _ ->
            Quantity(
                LinearExpressionSymbol(
                    LinearPolynomial(
                        monomials = listOf(LinearMonomial(Flt64.one, aggregation.load.z[j].value)),
                        constant = Flt64.zero
                    ),
                    name = "objective_load_weight_${positions[j]}"
                ),
                aircraftModel.weightUnit
            )
        }
    }

    private fun testFlight() = Flight(
        leg = IATA("SRC-DST"),
        now = kotlin.time.Instant.fromEpochMilliseconds(0),
        etd = kotlin.time.Instant.fromEpochMilliseconds(0)
    )

    private fun testParameter() = Parameter(
        macRangeC = Flt64.zero,
        longitudinalBalance = Flt64.zero,
        B737LongitudinalBalance = Flt64.zero,
        lateralBalance = Flt64.zero,
        horizontalStabilizerWarn = Flt64.zero,
        ballastWeight = Flt64.zero,
        emptyHated = Flt64.zero,
        besideDoorMainPosition = Flt64.zero,
        dividedEmpty = Flt64.zero,
        adviceLoadAmount = Flt64.zero,
        adviceLoadWeight = Flt64.zero,
        sameFlowTransferIn = Flt64.zero,
        sameFlowTransferOut = Flt64.zero,
        itemOrder = Flt64.zero,
        trailerChange = Flt64.zero,
        trailerCircling = Flt64.zero,
        priority = Flt64.zero,
            priorityCategory = Flt64.zero,
            experimentalLongitudinalBalance = Flt64.zero,
            redundancyRange = Flt64.zero,
            weightRecommendationBalance = Flt64.zero,
            weightRecommendationPayload = Flt64.zero
    )

    private data class LimitFixture(
        val aircraftModel: AircraftModel,
        val formula: Formula,
        val withMultiLoadingSchema: Boolean,
        val items: List<Item>,
        val positions: List<fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.Position>,
        val stowage: Stowage,
        val load: Load
    ) {
        fun model() = LinearMetaModel(name = "demo2-limit-test", converter = IntoValue.Identity)
    }
}
