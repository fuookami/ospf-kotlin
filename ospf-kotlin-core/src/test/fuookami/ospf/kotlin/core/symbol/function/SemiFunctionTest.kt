package fuookami.ospf.kotlin.core.symbol.function

import kotlin.test.*
import kotlinx.coroutines.runBlocking
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.testing.*
import fuookami.ospf.kotlin.core.token.AutoTokenTable
import fuookami.ospf.kotlin.core.variable.RealVar

class SemiFunctionTest {
    @Test
    fun explicitBoundsAndMarkerSemanticsShouldWorkForFourNumberTypes() {
        runMarkerCase(GenericNumberCases.flt64)
        runMarkerCase(GenericNumberCases.fltX)
        runMarkerCase(GenericNumberCases.rtn64)
        runMarkerCase(GenericNumberCases.rtnX)
    }

    @Test
    fun defaultAndInferredBoundsShouldWorkForFourNumberTypes() {
        runBoundCase(GenericNumberCases.flt64)
        runBoundCase(GenericNumberCases.fltX)
        runBoundCase(GenericNumberCases.rtn64)
        runBoundCase(GenericNumberCases.rtnX)
    }

    @Test
    fun invalidBoundsShouldBeRejected() {
        assertFailsWith<IllegalArgumentException> {
            SemiFunction(
                lb = Flt64.ten,
                ub = Flt64.one,
                converter = GenericNumberCases.flt64.converter,
                name = "invalid_semi"
            )
        }
    }

    @Test
    fun constraintRegistrationShouldBeANoOp() {
        val numberCase = GenericNumberCases.flt64
        val objective = LinearPolynomial<Flt64>(emptyList(), numberCase.zero)
        val metaModel = LinearMetaModel<Flt64>(
            name = "semi-no-op-registration",
            converter = numberCase.converter
        )

        try {
            assertTrue(metaModel.minimize(objective) is Ok)
            val mechanismResult = runBlocking {
                LinearMechanismModel.invoke(metaModel = metaModel, concurrent = false)
            }
            assertTrue(mechanismResult is Ok)
            val mechanismModel = mechanismResult.value
            val function = SemiFunction(
                lb = numberCase.one,
                ub = numberCase.ten,
                converter = numberCase.converter,
                name = "semi_no_op"
            )

            val before = mechanismModel.constraints.size
            assertTrue(function.registerConstraints(mechanismModel) is Ok)
            assertEquals(before, mechanismModel.constraints.size)
        } finally {
            metaModel.close()
        }
    }

    private fun <V> runMarkerCase(numberCase: GenericNumberCase<V>)
            where V : RealNumber<V>, V : NumberField<V> {
        val function = SemiFunction(
            lb = numberCase.one,
            ub = numberCase.ten,
            converter = numberCase.converter,
            name = "semi_${numberCase.name.lowercase()}"
        )

        assertTrue(function.lb eq numberCase.one, "${numberCase.name}: explicit lower bound")
        assertTrue(function.ub eq numberCase.ten, "${numberCase.name}: explicit upper bound")
        assertTrue(function.helperVariables.isEmpty(), "${numberCase.name}: marker has no helpers")
        assertNull(function.evaluate(emptyMap()), "${numberCase.name}: marker has no computed value")

        val tokens = AutoTokenTable<V>(Linear, false)
        assertTrue(function.registerAuxiliaryTokens(tokens) is Ok)
        assertTrue(tokens.tokens.isEmpty(), "${numberCase.name}: marker registers no tokens")
    }

    private fun <V> runBoundCase(numberCase: GenericNumberCase<V>)
            where V : RealNumber<V>, V : NumberField<V> {
        val defaultFunction = SemiFunction(
            converter = numberCase.converter,
            name = "semi_default_${numberCase.name.lowercase()}"
        )
        assertTrue(defaultFunction.lb eq numberCase.zero, "${numberCase.name}: default lower bound")
        assertTrue(
            defaultFunction.ub eq numberCase.converter.intoValue(Flt64(1e6)),
            "${numberCase.name}: default upper bound"
        )

        val variable = RealVar("semi_range_${numberCase.name.lowercase()}")
        variable.range.geq(Flt64(-2.0))
        variable.range.leq(Flt64(7.0))

        val inferred = SemiFunction.from(
            variable = variable,
            converter = numberCase.converter,
            name = "semi_inferred_${numberCase.name.lowercase()}"
        )
        assertTrue(
            inferred.lb eq numberCase.converter.intoValue(Flt64(-2.0)),
            "${numberCase.name}: inferred lower bound"
        )
        assertTrue(
            inferred.ub eq numberCase.converter.intoValue(Flt64(7.0)),
            "${numberCase.name}: inferred upper bound"
        )

        val overridden = SemiFunction.from(
            variable = variable,
            lb = numberCase.one,
            ub = numberCase.five,
            converter = numberCase.converter,
            name = "semi_overridden_${numberCase.name.lowercase()}"
        )
        assertTrue(overridden.lb eq numberCase.one, "${numberCase.name}: explicit lower override")
        assertTrue(overridden.ub eq numberCase.five, "${numberCase.name}: explicit upper override")
    }
}
