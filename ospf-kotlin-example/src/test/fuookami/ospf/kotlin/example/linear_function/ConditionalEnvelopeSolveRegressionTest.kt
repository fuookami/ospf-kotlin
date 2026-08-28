package fuookami.ospf.kotlin.example.linear_function

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

import fuookami.ospf.kotlin.utils.functional.Either
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.solver.report.ProblemStatus
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.AircraftModel
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.FlightPhase
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.Formula
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.FuelConstant
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.Fuselage
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.airworthiness_security.model.AbstractEnvelope
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.airworthiness_security.model.ConditionalEnvelope
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.Load
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.Payload
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.Stowage
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.TotalWeight
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.AircraftMinorModel
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.StowageMode

/**
 * 验证符号条件能够在求解时切换两组动态包络边界。
 * Verifies that a symbolic condition switches between two dynamic envelope boundary groups at solve time.
 */
class ConditionalEnvelopeSolveRegressionTest {
    private val converter = IntoValue.Identity
    private val phase = FlightPhase.TakeOff

    @Test
    fun symbolicConditionShouldSwitchBothEnvelopeBounds() {
        conditionalSolverCasesOrSkip().forEach { solverCase ->
            val falseOutput = solveEnvelope(solverCase, Flt64.zero)
            val trueOutput = solveEnvelope(solverCase, Flt64.one)

            assertNumericallyEqual(
                Flt64(-7.0),
                falseOutput.minIndex,
                "${solverCase.name}: false condition min envelope"
            )
            assertNumericallyEqual(
                Flt64(9.0),
                falseOutput.maxIndex,
                "${solverCase.name}: false condition max envelope"
            )
            assertNumericallyEqual(
                Flt64(-2.0),
                trueOutput.minIndex,
                "${solverCase.name}: true condition min envelope"
            )
            assertNumericallyEqual(
                Flt64(5.0),
                trueOutput.maxIndex,
                "${solverCase.name}: true condition max envelope"
            )
            assertTrue(
                abs(falseOutput.minIndex.toDouble() - trueOutput.minIndex.toDouble()) > 1e-6,
                "${solverCase.name}: conditions must select different minimum bounds"
            )
            assertTrue(
                abs(falseOutput.maxIndex.toDouble() - trueOutput.maxIndex.toDouble()) > 1e-6,
                "${solverCase.name}: conditions must select different maximum bounds"
            )
        }
    }

    private fun solveEnvelope(
        solverCase: ConditionalSolverCase,
        conditionValue: Flt64
    ): EnvelopeOutput {
        val aircraftModel = AircraftModel(AircraftMinorModel("B737"))
        val fixture = minimalFixture(aircraftModel)
        val conditionVariable = RealVar(
            "conditional_envelope_${solverCase.name}_${conditionValue.toDouble()}_condition"
        )
        conditionVariable.range.eq(conditionValue)
        val condition = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64.one, conditionVariable)),
            constant = Flt64.zero
        )
        val model = LinearMetaModel<Flt64>(
            name = "conditional-envelope-${solverCase.name}-${conditionValue.toDouble()}",
            converter = converter
        )
        val envelope = ConditionalEnvelope(
            aircraftModel = aircraftModel,
            phase = phase,
            name = "symbolic_conditional_envelope_${solverCase.name}_${conditionValue.toDouble()}",
            lhsSide1 = side(
                aircraftModel = aircraftModel,
                name = "lhs_1",
                type = AbstractEnvelope.SideType.Left,
                firstIndex = -3.0,
                secondIndex = -1.0
            ),
            rhsSide1 = side(
                aircraftModel = aircraftModel,
                name = "rhs_1",
                type = AbstractEnvelope.SideType.Right,
                firstIndex = 4.0,
                secondIndex = 6.0
            ),
            lhsSide2 = side(
                aircraftModel = aircraftModel,
                name = "lhs_2",
                type = AbstractEnvelope.SideType.Left,
                firstIndex = -8.0,
                secondIndex = -6.0
            ),
            rhsSide2 = side(
                aircraftModel = aircraftModel,
                name = "rhs_2",
                type = AbstractEnvelope.SideType.Right,
                firstIndex = 8.0,
                secondIndex = 10.0
            ),
            valueCondition = { null },
            symbolCondition = { _ -> Either.Left(condition) },
            totalWeight = fixture.totalWeight
        )

        try {
            assertTrue(model.add(conditionVariable) is Ok, "${solverCase.name}: condition should be accepted")
            assertTrue(
                fixture.payload.register(StowageMode.Predistribution, model) is Ok,
                "${solverCase.name}: payload should be accepted"
            )
            assertTrue(
                fixture.totalWeight.register(model) is Ok,
                "${solverCase.name}: total weight should be accepted"
            )
            assertTrue(
                envelope.register(model) is Ok,
                "${solverCase.name}: dynamic conditional envelope should be accepted"
            )
            assertTrue(
                model.minimize(envelope.minIndex.value) is Ok,
                "${solverCase.name}: minimum envelope objective should be accepted"
            )

            val result = runBlocking { solveConditionalMetaModel(solverCase.create(), model) }
            val report = result.value
                ?: error("${solverCase.name}: solver should return an envelope report")
            assertEquals(ProblemStatus.Feasible, report.problemStatus, "${solverCase.name}: solve status")
            assertNumericallyEqual(
                conditionValue,
                model.tokens.find(conditionVariable)?.result,
                "${solverCase.name}: fixed condition value"
            )

            return EnvelopeOutput(
                minIndex = envelope.minIndex.value.evaluate(model.tokens, converter)
                    ?: error("${solverCase.name}: minimum envelope value is missing"),
                maxIndex = envelope.maxIndex.value.evaluate(model.tokens, converter)
                    ?: error("${solverCase.name}: maximum envelope value is missing")
            )
        } finally {
            model.close()
        }
    }

    private fun minimalFixture(aircraftModel: AircraftModel): EnvelopeFixture {
        val weightUnit = aircraftModel.weightUnit
        val torqueUnit = aircraftModel.torqueUnit
        val stowage = Stowage(emptyList(), emptyList())
        val formula = Formula(
            aircraftModel = aircraftModel,
            lip = Quantity(Flt64.zero, aircraftModel.lengthUnit),
            chord = Quantity(Flt64.one, aircraftModel.lengthUnit),
            standardDatum = Quantity(Flt64.zero, aircraftModel.lengthUnit),
            forceDistanceCoefficient = Flt64.one,
            doiCorrection = Quantity(Flt64.zero, torqueUnit)
        )
        val load = Load(
            aircraftModel = aircraftModel,
            formula = formula,
            items = emptyList(),
            positions = emptyList(),
            withMultiLoadingSchema = false,
            stowage = stowage
        )
        val payload = Payload(
            plannedPayload = Quantity(Flt64(50.0), weightUnit),
            maxPayload = Quantity(Flt64(100.0), weightUnit),
            computedPayload = null,
            aircraftModel = aircraftModel,
            items = emptyList(),
            positions = emptyList(),
            load = load
        )
        val fuselage = Fuselage(
            liferaft = null,
            dow = Quantity(Flt64.zero, weightUnit),
            doi = Quantity(Flt64.zero, torqueUnit),
            balancedArm = Quantity(Flt64.zero, aircraftModel.lengthUnit)
        )
        val zeroFuel = FuelConstant(
            density = Quantity(Flt64.zero, aircraftModel.fuelDensityUnit),
            weight = Quantity(Flt64.zero, weightUnit),
            index = Quantity(Flt64.zero, torqueUnit)
        )
        val totalWeight = TotalWeight(
            maxTotalWeight = emptyMap(),
            computedTotalWeight = emptyMap(),
            aircraftModel = aircraftModel,
            fuselage = fuselage,
            fuel = mapOf(
                FlightPhase.TakeOff to zeroFuel,
                FlightPhase.Landing to zeroFuel
            ),
            payload = payload
        )
        return EnvelopeFixture(payload = payload, totalWeight = totalWeight)
    }

    private fun side(
        aircraftModel: AircraftModel,
        name: String,
        type: AbstractEnvelope.SideType,
        firstIndex: Double,
        secondIndex: Double
    ): AbstractEnvelope.Side {
        return AbstractEnvelope.Side(
            aircraftModel = aircraftModel,
            name = name,
            type = type,
            points = listOf(
                AbstractEnvelope.Point(
                    totalWeight = Quantity(Flt64.zero, aircraftModel.weightUnit),
                    index = Quantity(Flt64(firstIndex), aircraftModel.torqueUnit)
                ),
                AbstractEnvelope.Point(
                    totalWeight = Quantity(Flt64(100.0), aircraftModel.weightUnit),
                    index = Quantity(Flt64(secondIndex), aircraftModel.torqueUnit)
                )
            )
        )
    }

    private fun assertNumericallyEqual(expected: Flt64, actual: Flt64?, message: String) {
        val actualValue = actual ?: error(message)
        assertTrue(
            abs(actualValue.toDouble() - expected.toDouble()) <= 1e-6,
            "$message: expected=${expected.toDouble()}, actual=${actualValue.toDouble()}"
        )
    }

    private data class EnvelopeFixture(
        val payload: Payload,
        val totalWeight: TotalWeight
    )

    private data class EnvelopeOutput(
        val minIndex: Flt64,
        val maxIndex: Flt64
    )
}
