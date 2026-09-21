package fuookami.ospf.kotlin.core.solver

import kotlin.test.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.value_range.Interval
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.core.model.intermediate.ZeroBand
import fuookami.ospf.kotlin.core.model.intermediate.ConditionalValue
import fuookami.ospf.kotlin.core.model.intermediate.ImpliedCondition
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.function.IfInFunction
import fuookami.ospf.kotlin.core.symbol.function.ConditionBounds
import fuookami.ospf.kotlin.core.symbol.function.BinaryzationFunction
import fuookami.ospf.kotlin.core.symbol.function.BalanceTernaryzationFunction
import fuookami.ospf.kotlin.core.variable.BinVar
import fuookami.ospf.kotlin.core.variable.RealVar

class NativeIndicatorTest {
    @Test
    fun differenceRequiresBothBoundsProofsAndDistinctResults() {
        val input = RealVar("difference_input")
        input.range.geq(Flt64(-2.0))
        input.range.leq(Flt64(2.0))
        val function = BalanceTernaryzationFunction(
            x = LinearPolynomial(listOf(LinearMonomial(Flt64.one, input)), Flt64.zero),
            epsilon = Flt64(0.5),
            converter = IntoValue.Identity,
            strictBoundary = Flt64(0.25),
            name = "difference"
        )
        val structure = assertNotNull(function.deferredStructure())
        val difference = assertNotNull(structure.difference)
        val prepared = assertIs<Ok<NativeIndicatorData, *, *>>(prepareNativeIndicator(structure)).value
        assertEquals(function.resultVar.key, prepared.difference!!.resultKey)
        assertEquals(function.negativeVar.key, prepared.difference!!.condition.resultKey)
        assertEquals(-2.5, prepared.lowerBound)
        assertEquals(2.75, prepared.upperBound)
        assertEquals(6, assertIs<Ok<*, *, *>>(structure.generateConstraints()).value.let { it as List<*> }.size)
        assertTrue(prepareNativeIndicator(structure.copy(conditionBounds = ConditionBounds(Flt64(-2.0), Flt64(2.75)))) is Failed)
        assertTrue(prepareNativeIndicator(structure.copy(difference = difference.copy(condition = difference.condition.copy(
            conditionBounds = ConditionBounds(Flt64(-2.0), Flt64(2.75))
        )))) is Failed)
        assertTrue(prepareNativeIndicator(structure.copy(difference = difference.copy(resultVariable = function.positiveVar))) is Failed)
        assertTrue(prepareNativeIndicator(structure.copy(difference = difference.copy(condition = difference.condition.copy(
            input = LinearPolynomial(listOf(LinearMonomial(Flt64.one, function.resultVar)), Flt64.zero)
        )))) is Failed)
        assertTrue(prepareNativeIndicator(structure.copy(input = LinearPolynomial(
            listOf(LinearMonomial(Flt64.one, function.negativeVar)), Flt64.zero
        ))) is Failed)
        assertTrue(prepareNativeIndicator(structure.copy(difference = difference.copy(condition = structure))) is Failed)
    }

    @Test
    fun conjunctionRequiresBothBoundsProofsAndDistinctResults() {
        val input = RealVar("conjoined_input")
        input.range.geq(Flt64(-1.0))
        input.range.leq(Flt64(2.0))
        val function = IfInFunction(
            x = LinearPolynomial(listOf(LinearMonomial(Flt64.one, input)), Flt64.zero),
            lower = Flt64.zero,
            upper = Flt64.one,
            converter = IntoValue.Identity,
            strictBoundary = Flt64(0.25),
            name = "conjoined"
        )
        val structure = assertNotNull(function.deferredStructure())
        val conjunction = assertNotNull(structure.conjunction)
        val prepared = assertIs<Ok<NativeIndicatorData, *, *>>(prepareNativeIndicator(structure)).value
        assertEquals(function.resultVar.key, prepared.conjunction!!.resultKey)
        assertEquals(function.leVar.key, prepared.conjunction!!.condition.resultKey)
        assertEquals(7, assertIs<Ok<*, *, *>>(structure.generateConstraints()).value.let { it as List<*> }.size)
        assertTrue(prepareNativeIndicator(structure.copy(conditionBounds = ConditionBounds(Flt64(-0.5), Flt64(2.25)))) is Failed)
        assertTrue(prepareNativeIndicator(structure.copy(conjunction = conjunction.copy(condition = conjunction.condition.copy(
            conditionBounds = ConditionBounds(Flt64(-0.5), Flt64(2.25))
        )))) is Failed)
        assertTrue(prepareNativeIndicator(structure.copy(conjunction = conjunction.copy(resultVariable = function.geVar))) is Failed)
        assertTrue(prepareNativeIndicator(structure.copy(conjunction = conjunction.copy(condition = conjunction.condition.copy(
            input = LinearPolynomial(listOf(LinearMonomial(Flt64.one, function.resultVar)), Flt64.zero)
        )))) is Failed)
    }

    @Test
    fun zeroBandsValidateParametersSideColumnAndFullDomain() {
        val input = RealVar("band_input")
        input.range.geq(Flt64(-2.0))
        input.range.leq(Flt64(3.0))
        val side = BinVar("band_side")
        val band = ZeroBand(Flt64(0.125), side)
        val structure = BinaryzationFunction(
            polynomial = LinearPolynomial(listOf(LinearMonomial(Flt64.one, input)), Flt64(-1.0)),
            converter = IntoValue.Identity,
            bigM = Flt64(3.0),
            tolerance = Flt64(0.5),
            name = "band"
        ).deferredStructure().copy(zeroBand = band)
        val prepared = assertIs<Ok<NativeIndicatorData, *, *>>(prepareNativeIndicator(structure)).value
        assertEquals(side.key, prepared.zeroBand!!.sideKey)
        assertEquals(0.125, prepared.zeroBand!!.tolerance)
        assertTrue(prepareNativeIndicator(structure.copy(positiveOnZero = true)) is Ok)
        assertTrue(prepareNativeIndicator(structure.copy(zeroBand = band.copy(tolerance = Flt64.zero))) is Ok)
        assertTrue(prepareNativeIndicator(structure.copy(bigM = Flt64(2.0))) is Failed)
        for (invalid in listOf(Flt64(-1.0), Flt64.nan, Flt64(0.5), Flt64.one)) {
            assertTrue(prepareNativeIndicator(structure.copy(zeroBand = band.copy(tolerance = invalid))) is Failed)
        }
        for (invalid in listOf(input, structure.resultVariable)) {
            assertTrue(prepareNativeIndicator(structure.copy(zeroBand = band.copy(sideVariable = invalid))) is Failed)
        }
        assertTrue(prepareNativeIndicator(structure.copy(equivalentResults = listOf(side))) is Failed)
        assertTrue(prepareNativeIndicator(structure.copy(input = LinearPolynomial(listOf(LinearMonomial(Flt64.one, side)), Flt64.zero))) is Failed)
        assertTrue(prepareNativeIndicator(structure.copy(conditionBounds = ConditionBounds(Flt64(-3.0), Flt64(3.0)))) is Failed)
    }

    @Test
    fun impliedConditionsRequireIndependentBoundsAndColumns() {
        val antecedent = RealVar("antecedent")
        antecedent.range.geq(Flt64(-2.0))
        antecedent.range.leq(Flt64(3.0))
        val consequent = RealVar("consequent")
        consequent.range.geq(Flt64(-2.0))
        consequent.range.leq(Flt64(3.0))
        val flag = BinVar("consequent_flag")
        val value = ImpliedCondition(
            input = LinearPolynomial(listOf(LinearMonomial(Flt64(-2.0), consequent)), Flt64.one),
            indicatorVariable = flag,
            bounds = ConditionBounds(Flt64(-5.0), Flt64(5.0)),
            name = "implied"
        )
        val structure = BinaryzationFunction(
            polynomial = LinearPolynomial(listOf(LinearMonomial(Flt64.one, antecedent)), Flt64.zero),
            converter = IntoValue.Identity,
            bigM = Flt64(3.0),
            tolerance = Flt64.one,
            name = "antecedent"
        ).deferredStructure().copy(impliedCondition = value)
        val prepared = assertIs<Ok<NativeIndicatorData, *, *>>(prepareNativeIndicator(structure)).value
        assertEquals(flag.key, prepared.impliedCondition!!.resultKey)
        assertEquals(mapOf(consequent.key to -2.0), prepared.impliedCondition!!.terms)
        assertTrue(prepareNativeIndicator(structure.copy(impliedCondition = value.copy(bounds = ConditionBounds(Flt64(-4.0), Flt64(5.0))))) is Failed)
        assertTrue(prepareNativeIndicator(structure.copy(impliedCondition = value.copy(bounds = ConditionBounds(Flt64(-5.0), Flt64(4.0))))) is Failed)
        assertTrue(prepareNativeIndicator(structure.copy(impliedCondition = value.copy(indicatorVariable = structure.resultVariable))) is Failed)
        assertTrue(prepareNativeIndicator(structure.copy(impliedCondition = value.copy(indicatorVariable = consequent))) is Failed)
        assertTrue(prepareNativeIndicator(structure.copy(positiveOnZero = true)) is Failed)
        assertTrue(prepareNativeIndicator(structure.copy(impliedCondition = value.copy(input = LinearPolynomial(listOf(LinearMonomial(Flt64.one, flag)), Flt64.zero)))) is Failed)
    }

    @Test
    fun conditionalValuesRequireTheirOwnBoundsProof() {
        val input = RealVar("conditional_input")
        input.range.geq(Flt64(-2.0))
        input.range.leq(Flt64(3.0))
        val output = RealVar("conditional_output")
        val value = ConditionalValue(
            input = LinearPolynomial(listOf(LinearMonomial(Flt64(-2.0), input)), Flt64.one),
            resultVariable = output,
            bounds = ConditionBounds(Flt64(-5.0), Flt64(5.0)),
            name = "value"
        )
        val structure = BinaryzationFunction(
            polynomial = LinearPolynomial(listOf(LinearMonomial(Flt64.one, input)), Flt64.zero),
            converter = IntoValue.Identity,
            bigM = Flt64(3.0),
            name = "conditional"
        ).deferredStructure().copy(conditionalValue = value)
        val prepared = assertIs<Ok<NativeIndicatorData, *, *>>(prepareNativeIndicator(structure)).value
        assertEquals(output.key, prepared.conditionalValue!!.resultKey)
        assertEquals(mapOf(input.key to -2.0), prepared.conditionalValue!!.terms)
        assertTrue(prepareNativeIndicator(structure.copy(conditionalValue = value.copy(bounds = ConditionBounds(Flt64(-4.0), Flt64(5.0))))) is Failed)
        assertTrue(prepareNativeIndicator(structure.copy(conditionalValue = value.copy(bounds = ConditionBounds(Flt64(-5.0), Flt64(4.0))))) is Failed)
        assertTrue(prepareNativeIndicator(structure.copy(conditionalValue = value.copy(resultVariable = input))) is Failed)
        assertTrue(prepareNativeIndicator(structure.copy(conditionalValue = value.copy(resultVariable = structure.resultVariable))) is Failed)
    }

    @Test
    fun equivalentResultsAreRetainedAndValidated() {
        val input = RealVar("equivalent_input")
        input.range.geq(Flt64(-1.0))
        input.range.leq(Flt64.one)
        val alias = BinVar("equivalent_result")
        val structure = BinaryzationFunction(
            polynomial = LinearPolynomial(listOf(LinearMonomial(Flt64.one, input)), Flt64.zero),
            converter = IntoValue.Identity,
            name = "equivalent"
        ).deferredStructure().copy(equivalentResults = listOf(alias))
        val prepared = assertIs<Ok<NativeIndicatorData, *, *>>(prepareNativeIndicator(structure)).value
        assertEquals(listOf(alias.key), prepared.equivalentResultKeys)
        assertTrue(prepareNativeIndicator(structure.copy(equivalentResults = listOf(alias, alias))) is Failed)
        assertTrue(prepareNativeIndicator(structure.copy(equivalentResults = listOf(input))) is Failed)
        assertTrue(prepareNativeIndicator(structure.copy(equivalentResults = listOf(structure.resultVariable))) is Failed)
        assertTrue(prepareNativeIndicator(structure.copy(input = LinearPolynomial(listOf(LinearMonomial(Flt64.one, alias)), Flt64.zero))) is Failed)
    }

    @Test
    fun asymmetricBoundsRequireFullInputCoverage() {
        val input = RealVar("bounded_indicator")
        input.range.geq(Flt64(-2.0))
        input.range.leq(Flt64(3.0))
        val structure = BinaryzationFunction(
            polynomial = LinearPolynomial(listOf(LinearMonomial(Flt64.one, input)), Flt64.zero),
            converter = IntoValue.Identity,
            bigM = Flt64(3.0),
            name = "bounded_indicator"
        ).deferredStructure().copy(conditionBounds = ConditionBounds(Flt64(-2.0), Flt64(3.0)))
        val prepared = assertIs<Ok<NativeIndicatorData, *, *>>(prepareNativeIndicator(structure)).value
        assertEquals(-2.0, prepared.lowerBound)
        assertEquals(3.0, prepared.upperBound)
        assertTrue(prepareNativeIndicator(structure.copy(conditionBounds = ConditionBounds(Flt64(-1.0), Flt64(3.0)))) is Failed)
        assertTrue(prepareNativeIndicator(structure.copy(conditionBounds = ConditionBounds(Flt64(-2.0), Flt64(2.0)))) is Failed)
    }

    @Test
    fun affineInputsRequireBigMToCoverBothBounds() {
        val input = RealVar("indicator_affine")
        input.range.geq(Flt64(-2.0))
        input.range.leq(Flt64(3.0))
        val structure = BinaryzationFunction(
            polynomial = LinearPolynomial(listOf(LinearMonomial(Flt64(-2.0), input)), Flt64.one),
            converter = IntoValue.Identity,
            bigM = Flt64(5.0),
            name = "indicator"
        ).deferredStructure()
        val result = prepareNativeIndicator(structure)
        assertTrue(result is Ok)
        assertEquals(mapOf(input.key to -2.0), result.value.terms)
        assertEquals(1.0, result.value.constant)
        assertTrue(prepareNativeIndicator(structure.copy(bigM = Flt64(4.0))) is Failed)
        assertTrue(prepareNativeIndicator(structure.copy(tolerance = Flt64.nan)) is Failed)
        assertTrue(prepareNativeIndicator(structure.copy(resultVariable = input)) is Failed)
    }

    @Test
    fun unboundedInputKeepsFallback() {
        val input = RealVar("indicator_unbounded")
        input.range.set(ValueRange(
            lb = Flt64.minimum,
            ub = Flt64.maximum,
            lbInterval = Interval.Closed,
            ubInterval = Interval.Closed,
            constants = Flt64
        ).value!!)
        val structure = BinaryzationFunction(
            polynomial = LinearPolynomial(listOf(LinearMonomial(Flt64.one, input)), Flt64.zero),
            converter = IntoValue.Identity,
            name = "indicator"
        ).deferredStructure()
        assertTrue(prepareNativeIndicator(structure) is Failed)
    }
}
