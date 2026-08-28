package fuookami.ospf.kotlin.core.symbol.function

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.variable.IntVar
import fuookami.ospf.kotlin.core.variable.RealVar

class DiscreteConditionRegressionTest {
    @Test
    fun unitDeltaPreservesAllRelationsAndNegativeCoefficients() {
        val variable = IntVar("discrete_condition_relation_x")
        val condition = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64(-2.0), variable)),
            constant = Flt64(3.0)
        )
        val delta = Flt64.one
        val expectedConstants = mapOf(
            Comparison.GT to Flt64(3.0),
            Comparison.GE to Flt64(4.0),
            Comparison.LT to Flt64(-3.0),
            Comparison.LE to Flt64(-2.0)
        )
        val expectedCoefficients = mapOf(
            Comparison.GT to Flt64(-2.0),
            Comparison.GE to Flt64(-2.0),
            Comparison.LT to Flt64(2.0),
            Comparison.LE to Flt64(2.0)
        )

        for (relation in expectedConstants.keys) {
            val result = condition.toStrictPositiveCondition(
                relation = relation,
                delta = delta
            )
            assertTrue(result is Ok, "${relation.symbol} conversion should succeed")
            if (result is Ok) {
                assertEquals(expectedConstants.getValue(relation), result.value.constant)
                assertEquals(expectedCoefficients.getValue(relation), result.value.monomials.single().coefficient)
            }
        }
    }

    @Test
    fun largerDeltaRequiresAProvenDiscreteStep() {
        val integerVariable = IntVar("discrete_condition_integer_x")
        val realVariable = RealVar("discrete_condition_real_x")
        val integerCondition = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64(-2.0), integerVariable)),
            constant = Flt64(3.0)
        )
        val realCondition = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64(-2.0), realVariable)),
            constant = Flt64(3.0)
        )
        val fractionalCoefficientCondition = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64(0.5), integerVariable)),
            constant = Flt64(3.0)
        )
        val deltaAlignedCondition = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64(5.0), integerVariable)),
            constant = Flt64.zero
        )
        val bounds = ConditionBounds(Flt64(-20.0), Flt64(20.0))
        val delta = Flt64(5.0)
        val strictBoundary = Flt64(0.1)

        for (relation in listOf(Comparison.GT, Comparison.LT)) {
            val result = normalizeDiscreteCondition(
                poly = integerCondition,
                relation = relation,
                bounds = bounds,
                delta = delta,
                strictBoundary = strictBoundary
            )
            assertTrue(result is Ok, "${relation.symbol} should accept integer variables and coefficients")
        }
        for (relation in listOf(Comparison.GE, Comparison.LE)) {
            val result = normalizeDiscreteCondition(
                poly = integerCondition,
                relation = relation,
                bounds = bounds,
                delta = delta,
                strictBoundary = strictBoundary
            )
            assertTrue(result is Failed, "${relation.symbol} should reject an unproven non-unit step")
        }

        for (relation in listOf(Comparison.GE, Comparison.LE)) {
            val result = normalizeDiscreteCondition(
                poly = deltaAlignedCondition,
                relation = relation,
                bounds = bounds,
                delta = delta,
                strictBoundary = strictBoundary
            )
            assertTrue(result is Ok, "${relation.symbol} should accept a delta-aligned discrete condition")
        }

        val realResult = normalizeDiscreteCondition(
            poly = realCondition,
            relation = Comparison.GT,
            bounds = bounds,
            delta = delta,
            strictBoundary = strictBoundary
        )
        assertTrue(realResult is Failed)

        val fractionalCoefficientResult = normalizeDiscreteCondition(
            poly = fractionalCoefficientCondition,
            relation = Comparison.GT,
            bounds = bounds,
            delta = delta,
            strictBoundary = strictBoundary
        )
        assertTrue(fractionalCoefficientResult is Failed)

        val equalBoundaryResult = normalizeDiscreteCondition(
            poly = realCondition,
            relation = Comparison.GT,
            bounds = bounds,
            delta = strictBoundary,
            strictBoundary = strictBoundary
        )
        assertTrue(equalBoundaryResult is Ok)
    }

    @Test
    fun equalStrictBoundaryStillRejectsAUnitStepForNonUnitDelta() {
        val variable = IntVar("discrete_condition_equal_boundary_x")
        val unitStepCondition = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64.one, variable)),
            constant = Flt64.zero
        )
        val deltaAlignedCondition = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64(5.0), variable)),
            constant = Flt64.zero
        )
        val bounds = ConditionBounds(Flt64(-20.0), Flt64(20.0))
        val delta = Flt64(5.0)

        for (relation in listOf(Comparison.GE, Comparison.LE)) {
            val unsafe = normalizeDiscreteCondition(
                poly = unitStepCondition,
                relation = relation,
                bounds = bounds,
                delta = delta,
                strictBoundary = delta
            )
            assertTrue(
                unsafe is Failed,
                "${relation.symbol} must reject x with delta == strictBoundary == 5"
            )

            val safe = normalizeDiscreteCondition(
                poly = deltaAlignedCondition,
                relation = relation,
                bounds = bounds,
                delta = delta,
                strictBoundary = delta
            )
            assertTrue(
                safe is Ok,
                "${relation.symbol} must accept a value lattice with spacing 5"
            )
        }

        assertTrue(
            unitStepCondition.toStrictPositiveCondition(
                relation = Comparison.GE,
                delta = delta
            ) is Failed
        )
    }

    @Test
    fun offsetLatticeProofChecksTheRelevantSideOfZero() {
        val variable = IntVar("discrete_condition_offset_x")
        val condition = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64(5.0), variable)),
            constant = Flt64(2.0)
        )

        assertTrue(
            condition.toStrictPositiveCondition(
                relation = Comparison.GE,
                delta = Flt64(3.0)
            ) is Ok,
            "GE can safely shift the nearest negative value -3 by delta 3"
        )
        assertTrue(
            condition.toStrictPositiveCondition(
                relation = Comparison.LE,
                delta = Flt64(3.0)
            ) is Failed,
            "LE must reject the nearest positive value 2 below delta 3"
        )
        assertTrue(
            condition.toStrictPositiveCondition(
                relation = Comparison.LE,
                delta = Flt64(2.0)
            ) is Ok,
            "LE is safe when its nearest positive value reaches delta 2"
        )
    }

    @Test
    fun coefficientGcdDeterminesSpacingWithoutRequiringDeltaDivisibility() {
        val firstVariable = IntVar("discrete_condition_gcd_x")
        val secondVariable = IntVar("discrete_condition_gcd_y")
        val condition = LinearPolynomial(
            monomials = listOf(
                LinearMonomial(Flt64(-6.0), firstVariable),
                LinearMonomial(Flt64(10.0), secondVariable)
            ),
            constant = Flt64(2.0)
        )
        val bounds = ConditionBounds(Flt64(-40.0), Flt64(40.0))
        val delta = Flt64(1.5)
        val strictBoundary = Flt64(0.1)

        for (relation in listOf(Comparison.GE, Comparison.LE)) {
            val result = normalizeDiscreteCondition(
                poly = condition,
                relation = relation,
                bounds = bounds,
                delta = delta,
                strictBoundary = strictBoundary
            )
            assertTrue(
                result is Ok,
                "${relation.symbol} should use gcd(-6, 10)=2; the constant must not be treated as a coefficient"
            )
        }

        val offsetCondition = LinearPolynomial(
            monomials = listOf(
                LinearMonomial(Flt64(-10.0), firstVariable),
                LinearMonomial(Flt64(15.0), secondVariable)
            ),
            constant = Flt64(3.0)
        )
        for (relation in listOf(Comparison.GE, Comparison.LE)) {
            val result = offsetCondition.toStrictPositiveCondition(
                relation = relation,
                delta = Flt64(5.0)
            )
            assertTrue(
                result is Failed,
                "${relation.symbol} must reject a constant that is not aligned with gcd(-10, 15)=5"
            )
        }

        val unsafeShiftCondition = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64(6.0), firstVariable)),
            constant = Flt64(3.0)
        )
        for (relation in listOf(Comparison.GE, Comparison.LE)) {
            val result = unsafeShiftCondition.toStrictPositiveCondition(
                relation = relation,
                delta = Flt64(5.0)
            )
            assertTrue(
                result is Failed,
                "${relation.symbol} must reject 6*x+3 with delta=5 because -3 crosses zero"
            )
        }
    }

    @Test
    fun nonUnitDeltaRejectsGtAndLtLatticesThatFallInsideTheStrictGap() {
        val variable = IntVar("discrete_condition_gt_lt_offset_x")
        val unsafeCondition = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64(5.0), variable)),
            constant = Flt64(0.05)
        )
        val unsafeLessThanCondition = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64(5.0), variable)),
            constant = Flt64(-0.05)
        )
        val alignedCondition = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64(5.0), variable)),
            constant = Flt64.zero
        )
        val bounds = ConditionBounds(Flt64(-20.0), Flt64(20.0))
        val delta = Flt64(5.0)
        val strictBoundary = Flt64(0.1)

        for (relation in listOf(Comparison.GT, Comparison.LT)) {
            val unsafe = normalizeDiscreteCondition(
                poly = if (relation == Comparison.LT) unsafeLessThanCondition else unsafeCondition,
                relation = relation,
                bounds = bounds,
                delta = delta,
                strictBoundary = strictBoundary
            )
            assertTrue(
                unsafe is Failed,
                "${relation.symbol} must reject a positive lattice value inside the strict gap"
            )

            val safe = normalizeDiscreteCondition(
                poly = alignedCondition,
                relation = relation,
                bounds = bounds,
                delta = delta,
                strictBoundary = strictBoundary
            )
            assertTrue(safe is Ok, "${relation.symbol} should accept a zero-aligned lattice")
        }
    }

    @Test
    fun indicatorNormalizationUsesStrictBoundaryInsteadOfDiscreteDelta() {
        val variable = IntVar("discrete_condition_indicator_shift_x")
        val condition = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64(5.0), variable)),
            constant = Flt64.zero
        )
        val bounds = ConditionBounds(Flt64(-50.0), Flt64(50.0))
        val delta = Flt64(5.0)
        val strictBoundary = Flt64(0.1)

        val ge = normalizeDiscreteCondition(
            poly = condition,
            relation = Comparison.GE,
            bounds = bounds,
            delta = delta,
            strictBoundary = strictBoundary
        )
        assertTrue(ge is Ok, "GE should normalize on the 5-spaced lattice")
        if (ge is Ok) {
            assertEquals(strictBoundary, ge.value.polynomial.constant)
            assertEquals(Flt64(-49.9), ge.value.bounds.lower)
            assertEquals(Flt64(50.1), ge.value.bounds.upper)
        }

        val le = normalizeDiscreteCondition(
            poly = condition,
            relation = Comparison.LE,
            bounds = bounds,
            delta = delta,
            strictBoundary = strictBoundary
        )
        assertTrue(le is Ok, "LE should normalize on the 5-spaced lattice")
        if (le is Ok) {
            assertEquals(strictBoundary, le.value.polynomial.constant)
            assertEquals(Flt64(-49.9), le.value.bounds.lower)
            assertEquals(Flt64(50.1), le.value.bounds.upper)
        }
    }

    @Test
    fun gtAndLtCanFoldWhenTheirDeclaredRangeStaysOnOneBranch() {
        val variable = IntVar("discrete_condition_gt_lt_fold_x")
        val condition = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64.one, variable)),
            constant = Flt64.zero
        )
        val delta = Flt64(5.0)
        val strictBoundary = Flt64(0.1)

        val trueResult = normalizeDiscreteCondition(
            poly = condition,
            relation = Comparison.GT,
            bounds = ConditionBounds(Flt64(1.0), Flt64(2.0)),
            delta = delta,
            strictBoundary = strictBoundary
        )
        assertTrue(trueResult is Ok)
        if (trueResult is Ok) {
            assertEquals(TruthValue.True, trueResult.value.fixedValue)
        }

        val falseResult = normalizeDiscreteCondition(
            poly = condition,
            relation = Comparison.LT,
            bounds = ConditionBounds(Flt64(1.0), Flt64(2.0)),
            delta = delta,
            strictBoundary = strictBoundary
        )
        assertTrue(falseResult is Ok)
        if (falseResult is Ok) {
            assertEquals(TruthValue.False, falseResult.value.fixedValue)
        }
    }

    @Test
    fun spacingProofCombinesRepeatedTermsBeforeTakingTheGcd() {
        val variable = IntVar("discrete_condition_repeated_terms_x")
        val condition = LinearPolynomial(
            monomials = listOf(
                LinearMonomial(Flt64(6.0), variable),
                LinearMonomial(Flt64(-1.0), variable)
            ),
            constant = Flt64.zero
        )

        for (relation in listOf(Comparison.GE, Comparison.LE)) {
            val result = condition.toStrictPositiveCondition(
                relation = relation,
                delta = Flt64(5.0)
            )
            assertTrue(
                result is Ok,
                "${relation.symbol} should see repeated terms as the effective 5*x lattice"
            )
        }
    }

    @Test
    fun unprovenGeAndLeShiftCannotFoldAMixedIntegerRangeToTrue() {
        val variable = IntVar("discrete_condition_shift_x")
        val condition = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64.one, variable)),
            constant = Flt64.zero
        )
        val bounds = ConditionBounds(Flt64(-1.0), Flt64.one)
        val delta = Flt64(5.0)
        val strictBoundary = Flt64(0.1)

        for (relation in listOf(Comparison.GE, Comparison.LE)) {
            val result = normalizeDiscreteCondition(
                poly = condition,
                relation = relation,
                bounds = bounds,
                delta = delta,
                strictBoundary = strictBoundary
            )
            assertTrue(
                result is Failed,
                "${relation.symbol} with x in [-1, 1] must fail rather than fold to true"
            )
        }

        assertTrue(
            condition.toStrictPositiveCondition(
                relation = Comparison.GE,
                delta = delta
            ) is Failed
        )
    }

    @Test
    fun singleBranchBoundsPermitSafeConstantFoldingWithoutShiftProof() {
        val variable = IntVar("discrete_condition_single_branch_x")
        val condition = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64.one, variable)),
            constant = Flt64.zero
        )
        val delta = Flt64(5.0)
        val strictBoundary = Flt64(0.1)
        val cases = listOf(
            Triple(Comparison.GE, ConditionBounds(Flt64(0.0), Flt64(1.0)), TruthValue.True),
            Triple(Comparison.GE, ConditionBounds(Flt64(-6.0), Flt64(-5.0)), TruthValue.False),
            Triple(Comparison.LE, ConditionBounds(Flt64(-1.0), Flt64(0.0)), TruthValue.True),
            Triple(Comparison.LE, ConditionBounds(Flt64(5.0), Flt64(6.0)), TruthValue.False)
        )

        for ((relation, bounds, expected) in cases) {
            val result = normalizeDiscreteCondition(
                poly = condition,
                relation = relation,
                bounds = bounds,
                delta = delta,
                strictBoundary = strictBoundary
            )
            assertTrue(result is Ok, "${relation.symbol} should fold a single safe branch")
            if (result is Ok) {
                assertEquals(expected, result.value.fixedValue)
            }
        }

        val continuousVariable = RealVar("discrete_condition_single_branch_real_x")
        val continuousCondition = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64(0.5), continuousVariable)),
            constant = Flt64(0.25)
        )
        val continuousFold = normalizeDiscreteCondition(
            poly = continuousCondition,
            relation = Comparison.GE,
            bounds = ConditionBounds(Flt64(0.25), Flt64(0.75)),
            delta = delta,
            strictBoundary = strictBoundary
        )
        assertTrue(continuousFold is Ok)
        if (continuousFold is Ok) {
            assertEquals(TruthValue.True, continuousFold.value.fixedValue)
        }
    }

    @Test
    fun geAndLeFoldUsingTheirOriginalBoundaryBeforeALargeShift() {
        val condition = LinearPolynomial<Flt64>(emptyList(), Flt64(-1.0))
        val delta = Flt64(1.5)
        val strictBoundary = Flt64.one

        val geResult = normalizeDiscreteCondition(
            poly = condition,
            relation = Comparison.GE,
            bounds = ConditionBounds(Flt64(-1.0), Flt64(-1.0)),
            delta = delta,
            strictBoundary = strictBoundary
        )
        assertTrue(geResult is Ok)
        if (geResult is Ok) {
            assertEquals(TruthValue.False, geResult.value.fixedValue)
        }

        val leResult = normalizeDiscreteCondition(
            poly = LinearPolynomial(emptyList(), Flt64.one),
            relation = Comparison.LE,
            bounds = ConditionBounds(Flt64.one, Flt64.one),
            delta = delta,
            strictBoundary = strictBoundary
        )
        assertTrue(leResult is Ok)
        if (leResult is Ok) {
            assertEquals(TruthValue.False, leResult.value.fixedValue)
        }
    }

    @Test
    fun shiftedDiscreteConditionRejectsAFiniteButUnrepresentableResult() {
        val condition = LinearPolynomial<FltX>(
            monomials = emptyList(),
            constant = FltX("1.7e308")
        )

        assertTrue(
            condition.toStrictPositiveCondition(
                relation = Comparison.GE,
                delta = FltX("1e308")
            ) is Failed,
            "a finite shifted constant beyond the solver range must be rejected"
        )
    }

    @Test
    fun classificationUsesRelationBoundariesAndKeepsGtZeroFalse() {
        val delta = Flt64(5.0)
        val strictBoundary = Flt64(0.1)
        val cases = listOf(
            Triple(Comparison.GT, Flt64(-5.0), TruthValue.False),
            Triple(Comparison.GT, Flt64(0.0), TruthValue.False),
            Triple(Comparison.GT, Flt64(0.05), TruthValue.Undefined),
            Triple(Comparison.GT, Flt64(0.1), TruthValue.True),
            Triple(Comparison.GE, Flt64(-0.1), TruthValue.False),
            Triple(Comparison.GE, Flt64(-0.05), TruthValue.Undefined),
            Triple(Comparison.GE, Flt64(0.0), TruthValue.True),
            Triple(Comparison.LT, Flt64(-0.1), TruthValue.True),
            Triple(Comparison.LT, Flt64(-0.05), TruthValue.Undefined),
            Triple(Comparison.LT, Flt64(0.0), TruthValue.False),
            Triple(Comparison.LE, Flt64(0.0), TruthValue.True),
            Triple(Comparison.LE, Flt64(0.05), TruthValue.Undefined),
            Triple(Comparison.LE, Flt64(0.1), TruthValue.False)
        )

        for ((relation, value, expected) in cases) {
            val result = classifyDiscreteCondition(
                d = value,
                relation = relation,
                delta = delta,
                strictBoundary = strictBoundary
            )
            assertTrue(result is Ok, "${relation.symbol} classification should succeed")
            if (result is Ok) {
                assertEquals(expected, result.value, "unexpected ${relation.symbol} classification for d=$value")
            }
        }
    }

    @Test
    fun invalidDiscreteArgumentsReturnFailedResults() {
        val condition = LinearPolynomial<Flt64>(emptyList(), Flt64.zero)

        assertTrue(
            validateDiscreteConditionParameters(
                delta = Flt64.zero,
                strictBoundary = Flt64(0.1)
            ) is Failed
        )
        assertTrue(
            condition.toStrictPositiveCondition(
                relation = Comparison.EQ,
                delta = Flt64.one
            ) is Failed
        )
        assertTrue(
            classifyDiscreteCondition(
                d = Flt64.zero,
                relation = Comparison.NE,
                delta = Flt64.one,
                strictBoundary = Flt64(0.1)
            ) is Failed
        )
    }

    @Test
    fun discreteConversionRejectsSentinelsAndEffectiveCoefficientOverflow() {
        val variable = IntVar("discrete_condition_non_finite_x")
        val overflowCondition = LinearPolynomial(
            monomials = listOf(
                LinearMonomial(Flt64(1.0e308), variable),
                LinearMonomial(Flt64(1.0e308), variable)
            ),
            constant = Flt64.zero
        )

        assertTrue(
            overflowCondition.toStrictPositiveCondition(
                relation = Comparison.GT,
                delta = Flt64(0.1)
            ) is Failed,
            "combined repeated coefficients must be rejected when they overflow"
        )
        assertTrue(
            LinearPolynomial<Flt64>(emptyList(), Flt64.zero).toStrictPositiveCondition(
                relation = Comparison.GT,
                delta = Flt64.maximum
            ) is Failed,
            "the solver maximum sentinel cannot be used as delta"
        )
        assertTrue(
            normalizeDiscreteCondition(
                poly = LinearPolynomial(emptyList(), Flt64.zero),
                relation = Comparison.GT,
                bounds = ConditionBounds(Flt64.minimum, Flt64.one),
                delta = Flt64(0.1),
                strictBoundary = Flt64(0.1)
            ) is Failed,
            "the solver minimum sentinel cannot be used as a condition bound"
        )
    }
}
