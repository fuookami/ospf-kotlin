package fuookami.ospf.kotlin.core.solver

import kotlin.test.*
import kotlinx.coroutines.runBlocking
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.model.intermediate.AbsStructure
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.solver.value.SolveValueConversionPolicy
import fuookami.ospf.kotlin.core.solver.value.withSolveValueConversionPolicy
import fuookami.ospf.kotlin.core.variable.*

class NativeAbsTest {
    private val input = RealVar("abs_input")
    private val result = URealVar("abs_result")
    private val positive = URealVar("abs_positive")
    private val negative = URealVar("abs_negative")
    private val sign = BinVar("abs_sign")

    @Test
    fun preservesAsymmetricFallbackLimits() {
        val prepared = prepareNativeAbs(structure())
        assertTrue(prepared is Ok)
        assertEquals(input.key, prepared.value.inputKey)
        assertEquals(result.key, prepared.value.resultKey)
        assertEquals(3.0, prepared.value.positiveBigM)
        assertEquals(7.0, prepared.value.negativeBigM)
    }

    @Test
    fun rejectsAffineInputsAndAliasedHelpers() {
        val snapshot = structure()
        assertTrue(prepareNativeAbs(snapshot.copy(input = LinearPolynomial(
            listOf(LinearMonomial(Flt64.two, input)), Flt64.zero
        ))) is Failed)
        assertTrue(prepareNativeAbs(snapshot.copy(input = LinearPolynomial(
            listOf(LinearMonomial(Flt64.one, input)), Flt64.one
        ))) is Failed)
        assertTrue(prepareNativeAbs(snapshot.copy(positiveVariable = result)) is Failed)
    }

    @Test
    fun rejectsInvalidNumericBoundsAndSdkSentinels() {
        val snapshot = structure()
        assertTrue(prepareNativeAbs(snapshot.copy(positiveBigM = Flt64(-1.0))) is Failed)
        assertTrue(prepareNativeAbs(snapshot.copy(negativeBigM = Flt64.nan)) is Failed)
        assertTrue(prepareNativeAbs(snapshot, maximumMagnitude = 7.0) is Failed)
        assertTrue(prepareNativeAbs(snapshot, maximumMagnitude = Double.NaN) is Failed)
    }

    @Test
    fun conversionFailureRemainsAResultError() {
        val converter = object : IntoValue<Flt64> {
            override val zero = Flt64.zero
            override val one = Flt64.one
            override fun intoValue(value: Flt64) = value
            override fun fromValue(value: Flt64): Flt64 = error("conversion rejected")
        }
        assertTrue(prepareNativeAbs(structure().copy(converter = converter)) is Failed)
    }

    @Test
    fun strictConversionFailureDoesNotEscape() = runBlocking {
        withSolveValueConversionPolicy(SolveValueConversionPolicy.Strict) {
            assertTrue(prepareNativeAbs(structure().copy(positiveBigM = Flt64.infinity)) is Failed)
        }
    }

    private fun structure(): AbsStructure<Flt64> = AbsStructure(
        input = LinearPolynomial(listOf(LinearMonomial(Flt64.one, input)), Flt64.zero),
        resultVariable = result,
        positiveVariable = positive,
        negativeVariable = negative,
        signVariable = sign,
        positiveBigM = Flt64(3.0),
        negativeBigM = Flt64(7.0),
        converter = IntoValue.Identity,
        name = "native_abs"
    )
}
