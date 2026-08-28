package fuookami.ospf.kotlin.example.framework_demo.demo2

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.value.*
import fuookami.ospf.kotlin.core.symbol.function.ConditionBounds
import fuookami.ospf.kotlin.core.symbol.function.IfFunction
import fuookami.ospf.kotlin.core.symbol.function.LinearFunctionSymbolAdapter
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.Aggregation as AircraftAggregation
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.FlightPhase
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.service.AggregationInitializer as AircraftAggregationInitializer
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.airworthiness_security.Aggregation as AirworthinessAggregation
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.airworthiness_security.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.airworthiness_security.service.AggregationInitializer as AirworthinessAggregationInitializer
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.airworthiness_security.service.PipelineListGenerator as AirworthinessPipelineListGenerator
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.mac.Aggregation as MacAggregation
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.mac.model.HorizontalStabilizer
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.mac.service.AggregationInitializer as MacAggregationInitializer
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.mac_optimization.Aggregation as MacOptimizationAggregation
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.mac_optimization.model.MACRange
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.mac_optimization.service.AggregationInitializer as MacOptimizationAggregationInitializer
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.airworthiness_security.service.limits.HorizontalStabilizerLimit
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.soft_security.Aggregation as SoftSecurityAggregation
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.soft_security.service.AggregationInitializer as SoftSecurityAggregationInitializer
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.Aggregation as StowageAggregation
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.service.AggregationInitializer as StowageAggregationInitializer
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto.*

/** 验证 Demo2 的 MAC、适航和软安全聚合初始化。 / Verifies Demo2 MAC, airworthiness, and soft-security initialization. */
class Demo2SafetyAndMacInitializationTest {
    @Test
    fun `initializers construct and register the common safety and MAC chain`() {
        val request = pairedPositionRequest()
        val aircraft = aircraftAggregation(request)
        val stowage = stowageAggregation(aircraft, request)
        val mac = macAggregation(aircraft, stowage, request)
        val airworthiness = airworthinessAggregation(aircraft, stowage, mac, request)
        val softSecurity = softSecurityAggregation(aircraft, stowage, request)
        val macOptimization = macOptimizationAggregation(aircraft, stowage, mac, request)

        assertEquals(4, airworthiness.maxCumulativeLoadWeight.checkPoints.size)
        assertEquals(1, airworthiness.linearDensity.limitsZones.size)
        assertTrue(airworthiness.linearDensity.limitLines.isNotEmpty())
        assertEquals(1, airworthiness.surfaceDensity.limitsZones.size)
        assertEquals(1, softSecurity.divideEmptyLoading.adjacentPositions.size)
        assertEquals(MACRange.Type.OPT, macOptimization.macRange.optPoint.type)
        assertEquals(setOf(MACRange.Type.A, MACRange.Type.OPT, MACRange.Type.B, MACRange.Type.C),
            macOptimization.macRange.points.map { it.type }.toSet())

        val model = LinearMetaModel<Flt64>(
            name = "demo2-safety-and-mac-initialization",
            converter = IntoValue.Identity
        )
        assertSuccess(stowage.register(StowageMode.FullLoad, model), "stowage")
        assertSuccess(mac.register(StowageMode.FullLoad, model), "mac")
        assertSuccess(airworthiness.register(StowageMode.FullLoad, model), "airworthiness")
        assertSuccess(softSecurity.register(StowageMode.FullLoad, model), "soft security")
        assertSuccess(macOptimization.register(StowageMode.FullLoad, model), "mac optimization")

        val pipelineNames = unwrap(
            AirworthinessPipelineListGenerator(airworthiness)(StowageMode.FullLoad),
            "airworthiness pipeline generation"
        ).map { it.name }
        assertTrue(pipelineNames.any { it == "linear_density_limit" })
        assertTrue(pipelineNames.any { it == "surface_density_limit" })
        assertFalse(pipelineNames.any { it == "unsymmetrical_linear_density_limit" })
    }

    @Test
    fun `wide body initialization registers CLIM from request envelope`() {
        val request = RequestDTO.sample().copy(
            aircraftType = AircraftTypeInput.B767,
            envelopeLongitudinalMomentMin = -13.0,
            envelopeLongitudinalMomentMax = 17.0
        )
        val aircraft = aircraftAggregation(request)
        val stowage = stowageAggregation(aircraft, request)
        val mac = macAggregation(aircraft, stowage, request)
        val airworthiness = airworthinessAggregation(aircraft, stowage, mac, request)

        assertTrue(aircraft.aircraftModel.wideBody)
        assertNotNull(airworthiness.maxCLIM)

        val model = LinearMetaModel<Flt64>(
            name = "demo2-wide-body-clim",
            converter = IntoValue.Identity
        )
        assertSuccess(stowage.register(StowageMode.FullLoad, model), "stowage")
        assertSuccess(mac.register(StowageMode.FullLoad, model), "mac")
        assertSuccess(airworthiness.register(StowageMode.FullLoad, model), "airworthiness")
        val pipelines = unwrap(
            AirworthinessPipelineListGenerator(airworthiness)(StowageMode.FullLoad),
            "airworthiness pipeline generation"
        )
        assertTrue(pipelines.any { it.name == "max_clim_limit" })
        pipelines.forEach { pipeline -> assertSuccess(pipeline(model), pipeline.name) }
        assertTrue(model.relationConstraints.any { it.constraintName == "max_clim_limit_ub" })
        assertTrue(model.relationConstraints.any { it.constraintName == "max_clim_limit_lb" })
    }

    @Test
    fun `cumulative load zone uses the exact position overlap ratio`() {
        val request = RequestDTO.sample()
        val aircraft = aircraftAggregation(request)
        val stowage = stowageAggregation(aircraft, request)
        val position = stowage.positions.first()
        val zone = MaxCumulativeLoadWeight.LimitZone(
            direction = MaxCumulativeLoadWeight.Direction.FWD,
            fromArm = position.coordinate.frontArm,
            points = listOf(MaxCumulativeLoadWeight.Point(
                toArm = position.coordinate.longitudinalArm,
                maxSum = Quantity(Flt64(20.0), aircraft.aircraftModel.weightUnit)
            )),
            positions = stowage.positions
        )

        val checkpoint = zone.checkpoints.single()
        assertSame(zone, checkpoint.zone)
        assertEquals(1, checkpoint.parts.size)
        assertEquals(Flt64(0.5), checkpoint.parts.single().weight)
    }

    @Test
    fun `unsymmetrical density zone creates bilateral limits for paired positions`() {
        val request = RequestDTO.sample().copy(positions = listOf(
            PositionInput(name = "L", maxWeight = 10.0, longitudinalArm = 0.0, lateralArm = -2.0),
            PositionInput(name = "R", maxWeight = 10.0, longitudinalArm = 0.0, lateralArm = 2.0)
        ))
        val aircraft = aircraftAggregation(request)
        val stowage = stowageAggregation(aircraft, request)
        val zone = MaxUnsymmetricalLinearDensity.LimitZone(
            name = "paired",
            frontArm = Quantity(Flt64(-0.5), aircraft.aircraftModel.lengthUnit),
            backArm = Quantity(Flt64(0.5), aircraft.aircraftModel.lengthUnit),
            points = listOf(MaxUnsymmetricalLinearDensity.LimitPoint(
                lhs = Quantity(Flt64(3.0), aircraft.aircraftModel.linearDensityUnit),
                rhs = Quantity(Flt64(4.0), aircraft.aircraftModel.linearDensityUnit)
            )),
            positions = stowage.positions
        )

        assertTrue(zone.lines.isNotEmpty())
        assertTrue(zone.lines.all { line ->
            val arm = line.arm.to(aircraft.aircraftModel.lengthUnit)!!.value.toDouble()
            arm >= -0.5 && arm <= 0.5
        })
        assertTrue(zone.lines.all { line -> line.positions.size == 2 })
        assertEquals(2, zone.limits.size)
        assertEquals(Flt64.one, zone.limits[0].leftCoefficient)
        assertEquals(Flt64(-1.0), zone.limits[0].rightCoefficient)
        assertEquals(Flt64(-1.0), zone.limits[1].leftCoefficient)
        assertEquals(Flt64.one, zone.limits[1].rightCoefficient)
    }

    @Test
    fun `airworthiness initializer rejects negative operational limits`() {
        val request = RequestDTO.sample().copy(maxCumulativeForwardLoad = -1.0)
        val aircraft = aircraftAggregation(request)
        val stowage = stowageAggregation(aircraft, request)
        val mac = macAggregation(aircraft, stowage, request)

        assertTrue(AirworthinessAggregationInitializer(
            aircraftAggregation = aircraft,
            stowageAggregation = stowage,
            macAggregation = mac,
            input = request
        ) is Failed)
    }

    @Test
    fun `public payload and moment bounds become hard model constraints`() {
        val request = pairedPositionRequest().copy(
            payloadUpperBound = 16.0,
            minPayloadRatio = 0.5,
            envelopeLongitudinalMomentMin = -9.0,
            envelopeLongitudinalMomentMax = 11.0,
            targetLongitudinalMoment = 1.0,
            maxLongitudinalMomentDeviation = 3.0,
            maxLateralImbalance = 7.0
        )
        val aircraft = aircraftAggregation(request)
        val stowage = stowageAggregation(aircraft, request)
        val mac = macAggregation(aircraft, stowage, request)
        val airworthiness = airworthinessAggregation(aircraft, stowage, mac, request)
        val model = LinearMetaModel<Flt64>(
            name = "demo2-public-safety-bounds",
            converter = IntoValue.Identity
        )
        assertSuccess(stowage.register(StowageMode.FullLoad, model), "stowage")
        assertSuccess(mac.register(StowageMode.FullLoad, model), "mac")
        assertSuccess(airworthiness.register(StowageMode.FullLoad, model), "airworthiness")
        val pipelines = unwrap(
            AirworthinessPipelineListGenerator(airworthiness)(StowageMode.FullLoad),
            "airworthiness pipeline generation"
        )
        for (pipeline in pipelines) {
            assertSuccess(pipeline(model), pipeline.name)
        }

        val constraints = model.relationConstraints.associateBy { it.constraintName }
        val minPayload = constraints["payload_limit_minimum"]!!
        assertEquals(fuookami.ospf.kotlin.math.symbol.inequality.Comparison.GE, minPayload.sign)
        assertEquals(Flt64(8.0), minPayload.inequality.rhs.constant)

        val envelopeNames = FlightPhase.entries.flatMap { phase ->
            listOf(
                "envelope_limit_request_envelope_${phase.name.lowercase()}_lb",
                "envelope_limit_request_envelope_${phase.name.lowercase()}_ub"
            )
        }
        assertTrue(
            envelopeNames.all { it in constraints },
            "expected=$envelopeNames actual=${constraints.keys.filter { it.contains("envelope") }}"
        )
        for (phase in FlightPhase.entries) {
            val envelope = airworthiness.envelopes[phase]!!.single() as Envelope
            assertEquals(Flt64(-9.0), envelope.lhsSide.points.first().index.value)
            assertEquals(Flt64(11.0), envelope.rhsSide.points.first().index.value)
            val lower = constraints["envelope_limit_request_envelope_${phase.name.lowercase()}_lb"]!!
            val upper = constraints["envelope_limit_request_envelope_${phase.name.lowercase()}_ub"]!!
            assertEquals(fuookami.ospf.kotlin.math.symbol.inequality.Comparison.GE, lower.sign)
            assertEquals(fuookami.ospf.kotlin.math.symbol.inequality.Comparison.LE, upper.sign)
        }

        val targetNames = FlightPhase.entries.flatMap { phase ->
            listOf(
                "target_longitudinal_moment_limit_${phase.name.lowercase()}_minimum",
                "target_longitudinal_moment_limit_${phase.name.lowercase()}_maximum"
            )
        }
        assertTrue(targetNames.all { it in constraints })
        assertTrue(targetNames.filter { it.endsWith("_minimum") }.all {
            constraints[it]!!.inequality.rhs.constant == Flt64(-2.0)
        })
        assertTrue(targetNames.filter { it.endsWith("_maximum") }.all {
            constraints[it]!!.inequality.rhs.constant == Flt64(4.0)
        })
        assertEquals(Flt64(7.0), constraints["lateral_imbalance_limit_maximum"]!!.inequality.rhs.constant)
        assertEquals(Flt64(7.0), constraints["lateral_imbalance_limit_minimum"]!!.inequality.rhs.constant)
        assertTrue(constraints.keys.any { it.startsWith("linear_density_limit_") })
        assertTrue(constraints.keys.any { it.startsWith("surface_density_limit_") })
        assertFalse(constraints.keys.any { it.startsWith("unsymmetrical_linear_density_limit_") })
        assertFalse(constraints.keys.any { it.startsWith("envelope_longitudinal_moment_limit_") })
    }

    @Test
    fun `horizontal stabilizer warning lower bound uses the lower-bound relation`() {
        val request = RequestDTO.sample()
        val aircraft = aircraftAggregation(request)
        val stowage = stowageAggregation(aircraft, request)
        val baseMac = macAggregation(aircraft, stowage, request)
        val key = HorizontalStabilizer.Key(
            angle = HorizontalStabilizerAngle("test"),
            thrustDrate = null
        )
        val point = HorizontalStabilizer.Point(
            tow = Quantity(Flt64.zero, aircraft.aircraftModel.weightUnit),
            mac = baseMac.mac,
            trim = Flt64.zero
        )
        val configuredMac = MacAggregation(
            aircraftModel = aircraft.aircraftModel,
            fuselage = aircraft.fuselage,
            fuel = aircraft.fuel,
            formula = aircraft.formula,
            positions = stowage.positions,
            load = stowage.load,
            totalWeight = stowage.totalWeight,
            horizontalStabilizers = hashMapOf(
                key to (listOf(point) to HorizontalStabilizer.Limit(
                    minTrim = Flt64(-2.0),
                    maxTrim = Flt64(2.0),
                    warnMinTrim = Flt64(-1.0),
                    warnMaxTrim = Flt64(1.0)
                ))
            )
        )
        val airworthiness = airworthinessAggregation(aircraft, stowage, configuredMac, request)
        val model = LinearMetaModel<Flt64>(
            name = "demo2-horizontal-stabilizer-warning",
            converter = IntoValue.Identity
        )
        assertSuccess(stowage.register(StowageMode.WeightRecommendation, model), "stowage")
        assertSuccess(configuredMac.register(StowageMode.WeightRecommendation, model), "mac")
        assertSuccess(airworthiness.register(StowageMode.WeightRecommendation, model), "airworthiness")
        val pipelines = unwrap(
            AirworthinessPipelineListGenerator(airworthiness)(StowageMode.WeightRecommendation),
            "airworthiness pipeline generation"
        )
        pipelines.filter { it.name == "horizontal_stabilizer_limit" }
            .forEach { pipeline -> assertSuccess(pipeline(model), pipeline.name) }

        val constraints = model.relationConstraints.filter {
            it.constraintName.startsWith("horizontal_stabilizer_limit_")
        }
        assertTrue(constraints.any { it.constraintName.endsWith("_warn_lb") && it.sign == fuookami.ospf.kotlin.math.symbol.inequality.Comparison.GE })
        assertTrue(constraints.any { it.constraintName.endsWith("_lb") && !it.constraintName.endsWith("_warn_lb") && it.sign == fuookami.ospf.kotlin.math.symbol.inequality.Comparison.GE })
        assertTrue(constraints.any { it.constraintName.endsWith("_warn_ub") && it.sign == fuookami.ospf.kotlin.math.symbol.inequality.Comparison.LE })

        val warnOnlyKey = HorizontalStabilizer.Key(
            angle = HorizontalStabilizerAngle("warn_only"),
            thrustDrate = null
        )
        val warnOnly = HorizontalStabilizer(
            aircraftModel = aircraft.aircraftModel,
            key = warnOnlyKey,
            points = listOf(point),
            limit = HorizontalStabilizer.Limit(
                minTrim = null,
                maxTrim = null,
                warnMinTrim = Flt64(-1.0),
                warnMaxTrim = null
            ),
            totalWeight = stowage.totalWeight,
            mac = baseMac.mac
        )
        assertSuccess(warnOnly.register(StowageMode.WeightRecommendation, model), "warn-only stabilizer")
        assertSuccess(
            HorizontalStabilizerLimit(
                horizontalStabilizers = mapOf(warnOnlyKey to warnOnly),
                stowageMode = StowageMode.WeightRecommendation
            )(model),
            "warn-only stabilizer limits"
        )

        val hardOnlyKey = HorizontalStabilizer.Key(
            angle = HorizontalStabilizerAngle("hard_only"),
            thrustDrate = null
        )
        val hardOnly = HorizontalStabilizer(
            aircraftModel = aircraft.aircraftModel,
            key = hardOnlyKey,
            points = listOf(point),
            limit = HorizontalStabilizer.Limit(
                minTrim = Flt64(-2.0),
                maxTrim = null,
                warnMinTrim = null,
                warnMaxTrim = null
            ),
            totalWeight = stowage.totalWeight,
            mac = baseMac.mac
        )
        assertSuccess(hardOnly.register(StowageMode.WeightRecommendation, model), "hard-only stabilizer")
        assertSuccess(
            HorizontalStabilizerLimit(
                horizontalStabilizers = mapOf(hardOnlyKey to hardOnly),
                stowageMode = StowageMode.WeightRecommendation
            )(model),
            "hard-only stabilizer limits"
        )

        val lowerConstraintNames = model.relationConstraints.map { it.constraintName }.toSet()
        assertTrue(
            lowerConstraintNames.any { it.endsWith("_warn_lb") && it.contains("warn_only") },
            lowerConstraintNames.toString()
        )
        assertFalse(
            lowerConstraintNames.any {
                it.endsWith("_lb") && !it.endsWith("_warn_lb") && it.contains("warn_only")
            }
        )
        assertTrue(
            lowerConstraintNames.any { it.endsWith("_lb") && it.contains("hard_only") },
            lowerConstraintNames.toString()
        )
    }

    @Test
    fun `conditional envelope registers longitudinal moment bounds with torque units`() {
        val request = RequestDTO.sample()
        val aircraft = aircraftAggregation(request)
        val stowage = stowageAggregation(aircraft, request)
        val mac = macAggregation(aircraft, stowage, request)
        val model = LinearMetaModel<Flt64>(
            name = "demo2-conditional-envelope",
            converter = IntoValue.Identity
        )
        assertSuccess(stowage.register(StowageMode.FullLoad, model), "stowage")
        assertSuccess(mac.register(StowageMode.FullLoad, model), "mac")

        fun side(
            name: String,
            type: AbstractEnvelope.SideType,
            index: Double
        ): AbstractEnvelope.Side {
            return AbstractEnvelope.Side(
                aircraftModel = aircraft.aircraftModel,
                name = name,
                type = type,
                points = listOf(
                    AbstractEnvelope.Point(
                        totalWeight = Quantity(Flt64.zero, aircraft.aircraftModel.weightUnit),
                        index = Quantity(Flt64(index), aircraft.aircraftModel.torqueUnit)
                    ),
                    AbstractEnvelope.Point(
                        totalWeight = Quantity(Flt64(100.0), aircraft.aircraftModel.weightUnit),
                        index = Quantity(Flt64(index), aircraft.aircraftModel.torqueUnit)
                    )
                )
            )
        }

        val conditional = ConditionalEnvelope(
            aircraftModel = aircraft.aircraftModel,
            phase = FlightPhase.TakeOff,
            name = "conditional_request_envelope",
            lhsSide1 = side("lhs_1", AbstractEnvelope.SideType.Left, -3.0),
            rhsSide1 = side("rhs_1", AbstractEnvelope.SideType.Right, 4.0),
            lhsSide2 = side("lhs_2", AbstractEnvelope.SideType.Left, -2.0),
            rhsSide2 = side("rhs_2", AbstractEnvelope.SideType.Right, 5.0),
            valueCondition = { null },
            symbolCondition = { conditionName ->
                Either.Right(
                    LinearFunctionSymbolAdapter(
                        IfFunction(
                            condition = LinearPolynomial(emptyList(), Flt64.one),
                            converter = IntoValue.Identity,
                            name = conditionName,
                            conditionBounds = ConditionBounds(Flt64.one, Flt64.one)
                        ),
                        converter = IntoValue.Identity
                    )
                )
            },
            totalWeight = stowage.totalWeight
        )

        assertSuccess(conditional.register(model), "conditional envelope")
        assertNotNull(conditional.minIndex)
        assertNotNull(conditional.maxIndex)
        val dynamicTokenNames = model.tokens.symbols.map { it.name }
        assertTrue(
            dynamicTokenNames.any { it == "conditional_request_envelope_takeoff_min_switch" },
            dynamicTokenNames.toString()
        )
        assertTrue(
            dynamicTokenNames.any { it == "conditional_request_envelope_takeoff_max_switch" },
            dynamicTokenNames.toString()
        )

        fun invalidSide(
            name: String,
            type: AbstractEnvelope.SideType
        ): AbstractEnvelope.Side {
            return AbstractEnvelope.Side(
                aircraftModel = aircraft.aircraftModel,
                name = name,
                type = type,
                points = listOf(
                    AbstractEnvelope.Point(
                        totalWeight = Quantity(Flt64.zero, aircraft.aircraftModel.weightUnit),
                        index = Quantity(Flt64.zero, aircraft.aircraftModel.weightUnit)
                    ),
                    AbstractEnvelope.Point(
                        totalWeight = Quantity(Flt64(100.0), aircraft.aircraftModel.weightUnit),
                        index = Quantity(Flt64.zero, aircraft.aircraftModel.weightUnit)
                    )
                )
            )
        }

        val invalidConditional = ConditionalEnvelope(
            aircraftModel = aircraft.aircraftModel,
            phase = FlightPhase.TakeOff,
            name = "conditional_invalid_units",
            lhsSide1 = invalidSide("invalid_lhs_1", AbstractEnvelope.SideType.Left),
            rhsSide1 = side("invalid_rhs_1", AbstractEnvelope.SideType.Right, 4.0),
            lhsSide2 = side("invalid_lhs_2", AbstractEnvelope.SideType.Left, -2.0),
            rhsSide2 = side("invalid_rhs_2", AbstractEnvelope.SideType.Right, 5.0),
            valueCondition = { true },
            symbolCondition = { _ ->
                Either.Left(LinearPolynomial(emptyList(), Flt64.zero))
            },
            totalWeight = stowage.totalWeight
        )
        val invalidModel = LinearMetaModel<Flt64>(
            name = "demo2-conditional-envelope-invalid-units",
            converter = IntoValue.Identity
        )
        val invalidResult = invalidConditional.register(invalidModel)
        assertTrue(invalidResult is Failed)
        assertTrue((invalidResult as Failed<*, *, *>).error.message.contains("单位"))
    }

    @Test
    fun `dynamic conditional envelope rejects non finite side ranges`() {
        val request = RequestDTO.sample()
        val aircraft = aircraftAggregation(request)
        val stowage = stowageAggregation(aircraft, request)
        val model = LinearMetaModel<Flt64>(
            name = "demo2-conditional-envelope-non-finite-range",
            converter = IntoValue.Identity
        )
        assertSuccess(stowage.register(StowageMode.FullLoad, model), "stowage")

        fun finiteSide(name: String, type: AbstractEnvelope.SideType, index: Double): AbstractEnvelope.Side {
            return AbstractEnvelope.Side(
                aircraftModel = aircraft.aircraftModel,
                name = name,
                type = type,
                points = listOf(
                    AbstractEnvelope.Point(
                        totalWeight = Quantity(Flt64.zero, aircraft.aircraftModel.weightUnit),
                        index = Quantity(Flt64(index), aircraft.aircraftModel.torqueUnit)
                    ),
                    AbstractEnvelope.Point(
                        totalWeight = Quantity(Flt64(100.0), aircraft.aircraftModel.weightUnit),
                        index = Quantity(Flt64(index), aircraft.aircraftModel.torqueUnit)
                    )
                )
            )
        }

        val nonFiniteSide = AbstractEnvelope.Side(
            aircraftModel = aircraft.aircraftModel,
            name = "non_finite_lhs_1",
            type = AbstractEnvelope.SideType.Left,
            points = listOf(
                AbstractEnvelope.Point(
                    totalWeight = Quantity(Flt64.zero, aircraft.aircraftModel.weightUnit),
                    index = Quantity(Flt64.infinity, aircraft.aircraftModel.torqueUnit)
                ),
                AbstractEnvelope.Point(
                    totalWeight = Quantity(Flt64(100.0), aircraft.aircraftModel.weightUnit),
                    index = Quantity(Flt64.infinity, aircraft.aircraftModel.torqueUnit)
                )
            )
        )
        val conditional = ConditionalEnvelope(
            aircraftModel = aircraft.aircraftModel,
            phase = FlightPhase.TakeOff,
            name = "conditional_non_finite_range",
            lhsSide1 = nonFiniteSide,
            rhsSide1 = finiteSide("non_finite_rhs_1", AbstractEnvelope.SideType.Right, 4.0),
            lhsSide2 = finiteSide("non_finite_lhs_2", AbstractEnvelope.SideType.Left, -2.0),
            rhsSide2 = finiteSide("non_finite_rhs_2", AbstractEnvelope.SideType.Right, 5.0),
            valueCondition = { null },
            symbolCondition = { _ -> Either.Left(LinearPolynomial(emptyList(), Flt64.zero)) },
            totalWeight = stowage.totalWeight
        )

        val result = conditional.register(model)
        assertTrue(result is Failed)
        val message = (result as Failed<*, *, *>).error.message
        assertTrue(message.contains("有限"), message)
        assertTrue(message.contains("finite"), message)
    }

    @Test
    fun `envelope interpolation and conditional branches use weight and both side groups`() {
        val request = RequestDTO.sample()
        val aircraft = aircraftAggregation(request)
        val stowage = stowageAggregation(aircraft, request)
        val phase = FlightPhase.TakeOff
        val weightUnit = aircraft.aircraftModel.weightUnit
        val torqueUnit = aircraft.aircraftModel.torqueUnit
        val evaluationWeight = Quantity(Flt64(50.0), weightUnit)
        val totalWeight = TotalWeight(
            maxTotalWeight = emptyMap(),
            computedTotalWeight = mapOf(phase to evaluationWeight),
            aircraftModel = aircraft.aircraftModel,
            fuselage = aircraft.fuselage,
            fuel = aircraft.fuel,
            payload = stowage.payload
        )

        fun side(
            name: String,
            type: AbstractEnvelope.SideType,
            firstIndex: Double,
            secondIndex: Double
        ): AbstractEnvelope.Side {
            return AbstractEnvelope.Side(
                aircraftModel = aircraft.aircraftModel,
                name = name,
                type = type,
                points = listOf(
                    AbstractEnvelope.Point(
                        totalWeight = Quantity(Flt64.zero, weightUnit),
                        index = Quantity(Flt64(firstIndex), torqueUnit)
                    ),
                    AbstractEnvelope.Point(
                        totalWeight = Quantity(Flt64(100.0), weightUnit),
                        index = Quantity(Flt64(secondIndex), torqueUnit)
                    )
                )
            )
        }

        val interpolated = side(
            name = "interpolated",
            type = AbstractEnvelope.SideType.Left,
            firstIndex = 0.0,
            secondIndex = 10.0
        )
        assertEquals(Flt64(5.0), interpolated(evaluationWeight).value)
        assertEquals(Flt64.zero, interpolated(Quantity(Flt64(-1.0), weightUnit)).value)
        assertEquals(Flt64(10.0), interpolated(Quantity(Flt64(101.0), weightUnit)).value)

        fun conditional(value: Boolean?, name: String): ConditionalEnvelope {
            return ConditionalEnvelope(
                aircraftModel = aircraft.aircraftModel,
                phase = phase,
                name = name,
                lhsSide1 = side("${name}_lhs_1", AbstractEnvelope.SideType.Left, -3.0, -1.0),
                rhsSide1 = side("${name}_rhs_1", AbstractEnvelope.SideType.Right, 4.0, 6.0),
                lhsSide2 = side("${name}_lhs_2", AbstractEnvelope.SideType.Left, -8.0, -6.0),
                rhsSide2 = side("${name}_rhs_2", AbstractEnvelope.SideType.Right, 8.0, 10.0),
                valueCondition = { value },
                symbolCondition = { _ -> Either.Left(LinearPolynomial(emptyList(), Flt64.one)) },
                totalWeight = totalWeight
            )
        }

        val trueEnvelope = conditional(true, "conditional_true")
        val trueModel = LinearMetaModel<Flt64>(
            name = "demo2-conditional-envelope-true",
            converter = IntoValue.Identity
        )
        assertSuccess(trueEnvelope.register(trueModel), "true conditional envelope")
        assertEquals(Flt64(-2.0), trueEnvelope.minIndex.value.polynomial.constant)
        assertEquals(Flt64(5.0), trueEnvelope.maxIndex.value.polynomial.constant)

        val falseEnvelope = conditional(false, "conditional_false")
        val falseModel = LinearMetaModel<Flt64>(
            name = "demo2-conditional-envelope-false",
            converter = IntoValue.Identity
        )
        assertSuccess(falseEnvelope.register(falseModel), "false conditional envelope")
        assertEquals(Flt64(-7.0), falseEnvelope.minIndex.value.polynomial.constant)
        assertEquals(Flt64(9.0), falseEnvelope.maxIndex.value.polynomial.constant)

        val unknownEnvelope = conditional(null, "conditional_unknown")
        val unknownModel = LinearMetaModel<Flt64>(
            name = "demo2-conditional-envelope-unknown",
            converter = IntoValue.Identity
        )
        assertSuccess(unknownEnvelope.register(unknownModel), "unknown conditional envelope")
        val tokenNames = unknownModel.tokens.symbols.map { it.name }
        assertTrue(tokenNames.any { it.contains("conditional_unknown_lhs_1") }, tokenNames.toString())
        assertTrue(tokenNames.any { it.contains("conditional_unknown_lhs_2") }, tokenNames.toString())
        assertTrue(tokenNames.any { it.contains("conditional_unknown_rhs_1") }, tokenNames.toString())
        assertTrue(tokenNames.any { it.contains("conditional_unknown_rhs_2") }, tokenNames.toString())
    }

    @Test
    fun `airworthiness initializer rejects invalid public moment ranges`() {
        val request = RequestDTO.sample().copy(
            envelopeLongitudinalMomentMin = 1.0,
            envelopeLongitudinalMomentMax = -1.0
        )
        val aircraft = aircraftAggregation(request)
        val stowage = stowageAggregation(aircraft, request)
        val mac = macAggregation(aircraft, stowage, request)

        assertTrue(AirworthinessAggregationInitializer(
            aircraftAggregation = aircraft,
            stowageAggregation = stowage,
            macAggregation = mac,
            input = request
        ) is Failed)
    }

    private fun aircraftAggregation(request: RequestDTO): AircraftAggregation {
        return unwrap(AircraftAggregationInitializer(request), "aircraft initialization")
    }

    private fun pairedPositionRequest(): RequestDTO {
        return RequestDTO.sample().copy(positions = listOf(
            PositionInput(name = "L", maxWeight = 10.0, longitudinalArm = -1.0, lateralArm = -2.0),
            PositionInput(name = "R", maxWeight = 10.0, longitudinalArm = 1.0, lateralArm = 2.0)
        ))
    }

    private fun stowageAggregation(
        aircraft: AircraftAggregation,
        request: RequestDTO
    ): StowageAggregation {
        return unwrap(StowageAggregationInitializer(
            aircraftAggregation = aircraft,
            input = request,
            stowageMode = StowageMode.FullLoad
        ), "stowage initialization")
    }

    private fun macAggregation(
        aircraft: AircraftAggregation,
        stowage: StowageAggregation,
        request: RequestDTO
    ): MacAggregation {
        return unwrap(MacAggregationInitializer(aircraft, stowage, request), "mac initialization")
    }

    private fun airworthinessAggregation(
        aircraft: AircraftAggregation,
        stowage: StowageAggregation,
        mac: MacAggregation,
        request: RequestDTO
    ): AirworthinessAggregation {
        return unwrap(AirworthinessAggregationInitializer(aircraft, stowage, mac, request), "airworthiness initialization")
    }

    private fun softSecurityAggregation(
        aircraft: AircraftAggregation,
        stowage: StowageAggregation,
        request: RequestDTO
    ): SoftSecurityAggregation {
        return unwrap(SoftSecurityAggregationInitializer(aircraft, stowage, request), "soft-security initialization")
    }

    private fun macOptimizationAggregation(
        aircraft: AircraftAggregation,
        stowage: StowageAggregation,
        mac: MacAggregation,
        request: RequestDTO
    ): MacOptimizationAggregation {
        return unwrap(MacOptimizationAggregationInitializer(aircraft, stowage, mac, request), "mac-optimization initialization")
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
            is Failed -> fail("$description registration failed: ${result.error.message}")
            is Fatal -> fail("$description registration failed: ${result.firstError?.message}")
        }
    }
}
