package fuookami.ospf.kotlin.core.solver

import kotlin.test.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.value_range.*
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.model.intermediate.MaxStructure
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.*

class NativeMaxTest {
    private val input = RealVar("max_input").also {
        it.range.geq(Flt64(-2.0))
        it.range.leq(Flt64(3.0))
    }

    @Test
    fun preservesAffineInputsAndConstantCandidates() {
        val prepared = prepareNativeMax(structure())
        assertTrue(prepared is Ok)
        assertEquals(mapOf(input.key to -2.0), prepared.value.inputs.first().terms)
        assertEquals(-5.0, prepared.value.inputs.first().lowerBound)
        assertEquals(5.0, prepared.value.inputs.first().upperBound)
        assertTrue(prepared.value.inputs.last().terms.isEmpty())
        assertEquals(2.0, prepared.value.inputs.last().constant)
    }

    @Test
    fun rejectsSmallMAndExpandedInputDomain() {
        val snapshot = structure()
        assertTrue(prepareNativeMax(snapshot.copy(bigMValues = listOf(Flt64(9.0), Flt64(10.0)))) is Failed)
        input.range.set(ValueRange(
            lb = Flt64(-2.0),
            ub = Flt64(30.0),
            lbInterval = Interval.Closed,
            ubInterval = Interval.Closed,
            constants = Flt64
        ).value!!)
        assertTrue(prepareNativeMax(snapshot) is Failed)
    }

    @Test
    fun rejectsAliasingMissingBoundsAndInvalidNumbers() {
        val snapshot = structure()
        assertTrue(prepareNativeMax(snapshot.copy(selectorVariables = listOf(snapshot.resultVariable, BinVar("other")))) is Failed)
        val unbounded = RealVar("unbounded")
        assertTrue(prepareNativeMax(snapshot.copy(inputs = listOf(
            LinearPolynomial(listOf(LinearMonomial(Flt64.one, unbounded)), Flt64.zero),
            snapshot.inputs.last()
        ))) is Failed)
        assertTrue(prepareNativeMax(snapshot.copy(bigMValues = listOf(Flt64.nan, Flt64(10.0)))) is Failed)
        assertTrue(prepareNativeMax(snapshot, maximumMagnitude = 10.0) is Failed)
    }

    @Test
    fun conversionFailureRemainsAResultError() {
        val converter = object : IntoValue<Flt64> {
            override val zero = Flt64.zero
            override val one = Flt64.one
            override fun intoValue(value: Flt64) = value
            override fun fromValue(value: Flt64): Flt64 = error("conversion rejected")
        }
        assertTrue(prepareNativeMax(structure().copy(converter = converter)) is Failed)
    }

    private fun structure(): MaxStructure<Flt64> = MaxStructure(
        inputs = listOf(
            LinearPolynomial(listOf(LinearMonomial(Flt64(-2.0), input)), Flt64.one),
            LinearPolynomial(emptyList(), Flt64.two)
        ),
        resultVariable = RealVar("max_result"),
        selectorVariables = listOf(BinVar("max_first"), BinVar("max_second")),
        bigMValues = listOf(Flt64(10.0), Flt64(10.0)),
        converter = IntoValue.Identity,
        name = "native_max"
    )
}
