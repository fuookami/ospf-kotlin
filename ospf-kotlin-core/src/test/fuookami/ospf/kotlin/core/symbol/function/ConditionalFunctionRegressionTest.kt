package fuookami.ospf.kotlin.core.symbol.function

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import fuookami.ospf.kotlin.utils.error.Err
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.value_range.Interval
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.model.basic.ConstraintRelation
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMechanismModel
import fuookami.ospf.kotlin.core.model.mechanism.Constraint
import fuookami.ospf.kotlin.core.model.mechanism.LinearConstraintInput
import fuookami.ospf.kotlin.core.model.mechanism.LinearConstraintImpl
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.model.mechanism.LinearMechanismModel
import fuookami.ospf.kotlin.core.model.mechanism.LinearSubObject
import fuookami.ospf.kotlin.core.model.mechanism.Object
import fuookami.ospf.kotlin.core.model.mechanism.SingleObject
import fuookami.ospf.kotlin.core.solver.report.ModelElementIdentityRegistry
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.IntermediateSymbol
import fuookami.ospf.kotlin.core.token.AbstractTokenTable
import fuookami.ospf.kotlin.core.token.AutoTokenTable
import fuookami.ospf.kotlin.core.token.ManualTokenTable
import fuookami.ospf.kotlin.core.variable.BinVar
import fuookami.ospf.kotlin.core.variable.RealVar

class ConditionalFunctionRegressionTest {
    @Test
    fun classifierUsesTheSameFourRelationMatrix() {
        val gap = Flt64(0.1)
        assertClassification(Flt64(0.1), Comparison.GT, TruthValue.True, gap)
        assertClassification(Flt64(0.0), Comparison.GT, TruthValue.False, gap)
        assertClassification(Flt64(0.05), Comparison.GT, TruthValue.Undefined, gap)

        assertClassification(Flt64(0.0), Comparison.GE, TruthValue.True, gap)
        assertClassification(Flt64(-0.1), Comparison.GE, TruthValue.False, gap)
        assertClassification(Flt64(-0.05), Comparison.GE, TruthValue.Undefined, gap)

        assertClassification(Flt64(-0.1), Comparison.LT, TruthValue.True, gap)
        assertClassification(Flt64(0.0), Comparison.LT, TruthValue.False, gap)
        assertClassification(Flt64(-0.05), Comparison.LT, TruthValue.Undefined, gap)

        assertClassification(Flt64(0.0), Comparison.LE, TruthValue.True, gap)
        assertClassification(Flt64(0.1), Comparison.LE, TruthValue.False, gap)
        assertClassification(Flt64(0.05), Comparison.LE, TruthValue.Undefined, gap)

        assertTrue(classify(Flt64.zero, Comparison.EQ, gap) is Failed)
        assertTrue(classify(Flt64.zero, Comparison.NE, gap) is Failed)
        assertTrue(classify(Flt64.zero, Comparison.GT, Flt64.zero) is Failed)
    }

    @Test
    fun rangeDrivenConstraintsUseTheDeclaredLowerAndUpperBounds() {
        val x = RealVar("conditional_shape_x")
        val indicator = BinVar("conditional_shape_indicator")
        val poly = linearPoly(x)
        val result = relationIndicatorConstraints(
            poly = poly,
            indicator = indicator,
            relation = Comparison.GT,
            bounds = ConditionBounds(Flt64(-9.0), Flt64(11.0)),
            strictBoundary = Flt64.one,
            namePrefix = "conditional_shape"
        )

        assertTrue(result is Ok)
        if (result is Ok) {
            val constraints = result.value
            assertEquals(2, constraints.size)
            assertEquals(Comparison.GE, constraints[0].comparison)
            assertEquals(Comparison.LE, constraints[1].comparison)
            assertEquals(Flt64(-9.0), constraints[0].rhs.constant)
            assertEquals(Flt64.zero, constraints[1].rhs.constant)
            assertEquals(
                Flt64(-10.0),
                constraints[0].lhs.monomials.single { it.symbol == indicator }.coefficient
            )
            assertEquals(
                Flt64(-11.0),
                constraints[1].lhs.monomials.single { it.symbol == indicator }.coefficient
            )
        }
    }

    @Test
    fun rangeDrivenConstraintsUseTheCorrectBoundaryForEveryRelation() {
        val x = RealVar("conditional_relation_shape_x")
        val indicator = BinVar("conditional_relation_shape_indicator")
        val poly = linearPoly(x)
        val cases = mapOf(
            Comparison.GT to Triple(ConditionBounds(Flt64(-9.0), Flt64(11.0)), Flt64(-10.0), Flt64(-11.0)),
            Comparison.GE to Triple(ConditionBounds(Flt64(-9.0), Flt64(11.0)), Flt64(-9.0), Flt64(-12.0)),
            Comparison.LT to Triple(ConditionBounds(Flt64(-9.0), Flt64(11.0)), Flt64(-12.0), Flt64(-9.0)),
            Comparison.LE to Triple(ConditionBounds(Flt64(-9.0), Flt64(11.0)), Flt64(-11.0), Flt64(-10.0))
        )

        for ((relation, expected) in cases) {
            val result = relationIndicatorConstraints(
                poly = poly,
                indicator = indicator,
                relation = relation,
                bounds = expected.first,
                strictBoundary = Flt64.one,
                namePrefix = "conditional_relation_shape_${relation.symbol}"
            )

            assertTrue(result is Ok, "${relation.symbol} should build successfully")
            if (result is Ok) {
                assertEquals(expected.second, result.value[0].lhs.monomials.single {
                    it.symbol == indicator
                }.coefficient)
                assertEquals(expected.third, result.value[1].lhs.monomials.single {
                    it.symbol == indicator
                }.coefficient)
            }
        }
    }

    @Test
    fun allConditionalFunctionsShareThreeValuedEvaluation() {
        val x = RealVar("conditional_eval_x")
        val y = RealVar("conditional_eval_y")
        val xPoly = linearPoly(x)
        val yPoly = linearPoly(y)
        val gap = Flt64(0.1)

        val ifFunction = IfFunction(
            condition = xPoly,
            converter = IntoValue.Identity,
            relation = Comparison.GT,
            strictBoundary = gap,
            name = "conditional_eval_if"
        )
        assertEquals(Flt64.one, ifFunction.evaluate(mapOf<Symbol, Flt64>(x to Flt64(0.2))))
        assertEquals(Flt64.zero, ifFunction.evaluate(mapOf<Symbol, Flt64>(x to Flt64.zero)))
        assertEquals(null, ifFunction.evaluate(mapOf<Symbol, Flt64>(x to Flt64(0.05))))

        val sigmoid = SigmoidFunction(
            condition = xPoly,
            converter = IntoValue.Identity,
            relation = Comparison.LE,
            strictBoundary = gap,
            name = "conditional_eval_sigmoid"
        )
        assertEquals(Flt64.one, sigmoid.evaluate(mapOf<Symbol, Flt64>(x to Flt64.zero)))
        assertEquals(Flt64.zero, sigmoid.evaluate(mapOf<Symbol, Flt64>(x to Flt64(0.2))))
        assertEquals(null, sigmoid.evaluate(mapOf<Symbol, Flt64>(x to Flt64(0.05))))

        val ifIn = IfInFunction(
            x = xPoly,
            lower = Flt64.zero,
            upper = Flt64.one,
            converter = IntoValue.Identity,
            strictBoundary = gap,
            name = "conditional_eval_ifin"
        )
        assertEquals(Flt64.one, ifIn.evaluate(mapOf<Symbol, Flt64>(x to Flt64(0.5))))
        assertEquals(Flt64.zero, ifIn.evaluate(mapOf<Symbol, Flt64>(x to Flt64(-1.0))))
        assertEquals(null, ifIn.evaluate(mapOf<Symbol, Flt64>(x to Flt64(-0.05))))

        val ifThen = IfThenFunction(
            condition = xPoly,
            thenPoly = LinearPolynomial(emptyList(), Flt64(-2.0)),
            converter = IntoValue.Identity,
            strictBoundary = gap,
            name = "conditional_eval_ifthen"
        )
        assertEquals(Flt64(-2.0), ifThen.evaluate(mapOf<Symbol, Flt64>(x to Flt64(0.2))))
        assertEquals(Flt64.zero, ifThen.evaluate(mapOf<Symbol, Flt64>(x to Flt64.zero)))
        assertEquals(null, ifThen.evaluate(mapOf<Symbol, Flt64>(x to Flt64(0.05))))

        val imply = ImplyFunction(
            antecedent = xPoly,
            consequent = yPoly,
            converter = IntoValue.Identity,
            strictBoundary = gap,
            name = "conditional_eval_imply"
        )
        assertEquals(
            Flt64.one,
            imply.evaluate(mapOf<Symbol, Flt64>(x to Flt64.zero, y to Flt64(0.05)))
        )
        assertEquals(
            Flt64.one,
            imply.evaluate(mapOf<Symbol, Flt64>(x to Flt64.zero))
        )
        assertEquals(
            Flt64.zero,
            imply.evaluate(mapOf<Symbol, Flt64>(x to Flt64(0.2), y to Flt64(-0.2)))
        )
        assertEquals(
            null,
            imply.evaluate(mapOf<Symbol, Flt64>(x to Flt64(0.05), y to Flt64(0.2)))
        )
        val undefinedAntecedent = imply.classify(mapOf<Symbol, Flt64>(x to Flt64(0.05)))
        assertTrue(undefinedAntecedent is Ok)
        if (undefinedAntecedent is Ok) {
            assertEquals(TruthValue.Undefined, undefinedAntecedent.value)
        }
    }

    @Test
    fun declaredBoundsTakePrecedenceOverInferredBounds() {
        val x = RealVar("conditional_precedence_x")
        x.range.geq(Flt64(-10.0))
        x.range.leq(Flt64(10.0))
        val poly = linearPoly(x)
        val function = IfFunction(
            condition = poly,
            converter = IntoValue.Identity,
            strictBoundary = Flt64.one,
            bounds = ConditionBounds(Flt64.zero, Flt64.one),
            name = "conditional_precedence_if"
        )

        val model = LinearMetaModel<Flt64>(
            name = "conditional-precedence-model",
            converter = IntoValue.Identity
        )
        try {
            assertTrue(model.add(x) is Ok)
            assertTrue(function.registerAuxiliaryTokens(model.tokens) is Ok)
            assertTrue(model.minimize(poly) is Ok)
            val mechanism = runBlocking {
                LinearMechanismModel.invoke<Flt64>(model, concurrent = false)
            }
            assertTrue(mechanism is Ok)
            if (mechanism is Ok) {
                val before = mechanism.value.constraints.size
                assertTrue(function.registerConstraints(mechanism.value) is Ok)
                val appended = mechanism.value.constraints.subList(before, mechanism.value.constraints.size)
                val lower = appended
                    .filterIsInstance<LinearConstraintImpl<Flt64>>()
                    .firstOrNull { it.name == "conditional_precedence_if_if_lower" }
                assertNotNull(lower, "Unexpected conditional constraint names: ${appended.map { it.name }}")
                assertTrue(
                    lower.rhs == Flt64.zero || lower.rhs == -Flt64.zero,
                    "Expected zero RHS, got ${lower.rhs}"
                )
                assertEquals(
                    Flt64(-1.0),
                    lower.lhs.single { it.token.variable.name == "conditional_precedence_if_if_nz" }.coefficient
                )
            }
        } finally {
            model.close()
        }
    }

    @Test
    fun ifInUsesDeclaredBoundsBeforeInferredBounds() {
        val x = RealVar("conditional_ifin_precedence_x")
        x.range.geq(Flt64(-10.0))
        x.range.leq(Flt64(10.0))
        val function = IfInFunction(
            x = linearPoly(x),
            lower = Flt64.zero,
            upper = Flt64.one,
            converter = IntoValue.Identity,
            strictBoundary = Flt64.one,
            conditionBounds = ConditionBounds(Flt64.zero, Flt64.one),
            name = "conditional_ifin_precedence"
        )
        val model = LinearMetaModel<Flt64>(
            name = "conditional-ifin-precedence-model",
            converter = IntoValue.Identity
        )
        try {
            assertTrue(model.add(x) is Ok)
            assertTrue(function.registerAuxiliaryTokens(model.tokens) is Ok)
            assertTrue(model.minimize(linearPoly(x)) is Ok)
            val mechanism = runBlocking {
                LinearMechanismModel.invoke<Flt64>(model, concurrent = false)
            }
            assertTrue(mechanism is Ok)
            if (mechanism is Ok) {
                val before = mechanism.value.constraints.size
                assertTrue(function.registerConstraints(mechanism.value) is Ok)
                val appended = mechanism.value.constraints.subList(before, mechanism.value.constraints.size)
                assertTrue(
                    appended
                        .filterIsInstance<LinearConstraintImpl<Flt64>>()
                        .all { it.name.endsWith("fold_true") || it.name.endsWith("fold_result") },
                    "Explicit [0, 1] bounds should fold both IfIn sides: ${appended.map { it.name }}"
                )
            }
        } finally {
            model.close()
        }
    }

    @Test
    fun invalidExplicitBoundsMustNotFallBackToInferredBounds() {
        val invalidBounds = ConditionBounds(Flt64.one, Flt64.zero)
        val table = ManualTokenTable<Flt64>(Linear, false)

        try {
            val ifFunction = IfFunction(
                condition = LinearPolynomial(emptyList(), Flt64.zero),
                converter = IntoValue.Identity,
                conditionBounds = invalidBounds,
                name = "conditional_invalid_explicit_if"
            )
            assertTrue(ifFunction.registerAuxiliaryTokens(table) is Failed)
            assertEquals(0, table.tokens.size)

            val ifInFunction = IfInFunction(
                x = LinearPolynomial(emptyList(), Flt64.zero),
                lower = Flt64(-1.0),
                upper = Flt64.one,
                converter = IntoValue.Identity,
                conditionBounds = invalidBounds,
                name = "conditional_invalid_explicit_ifin"
            )
            assertTrue(ifInFunction.registerAuxiliaryTokens(table) is Failed)
            assertEquals(0, table.tokens.size)

            val sigmoid = SigmoidFunction(
                condition = LinearPolynomial(emptyList(), Flt64.zero),
                converter = IntoValue.Identity,
                conditionBounds = invalidBounds,
                name = "conditional_invalid_explicit_sigmoid"
            )
            assertTrue(sigmoid.registerAuxiliaryTokens(table) is Failed)
            assertEquals(0, table.tokens.size)
        } finally {
            table.close()
        }
    }

    @Test
    fun ifThenResultRangeCoversZeroAndNegativeThenValue() {
        val function = IfThenFunction(
            condition = LinearPolynomial(emptyList(), Flt64.one),
            thenPoly = LinearPolynomial(emptyList(), Flt64(-2.0)),
            converter = IntoValue.Identity,
            strictBoundary = Flt64(0.1),
            name = "conditional_negative_then"
        )
        val table = ManualTokenTable<Flt64>(Linear, false)

        assertTrue(function.registerAuxiliaryTokens(table) is Ok)
        val resultVariable = function.helperVariables.single { it.name == "conditional_negative_then_y" }
        assertNotNull(resultVariable.lowerBound)
        assertNotNull(resultVariable.upperBound)
        assertEquals(Flt64(-2.0), resultVariable.lowerBound!!.value.unwrap())
        assertEquals(Flt64.zero, resultVariable.upperBound!!.value.unwrap())
    }

    @Test
    fun ifThenAuxiliaryRegistrationDoesNotChangeResultRangeAfterTokenConflict() {
        val function = IfThenFunction(
            condition = LinearPolynomial(emptyList(), Flt64.one),
            thenPoly = LinearPolynomial(emptyList(), Flt64(5.0)),
            converter = IntoValue.Identity,
            strictBoundary = Flt64(0.1),
            conditionBounds = ConditionBounds(Flt64.one, Flt64.one),
            thenBounds = ConditionBounds(Flt64(5.0), Flt64(5.0)),
            name = "conditional_atomic_tokens"
        )
        val resultVariable = function.helperVariables.single { it.name == "conditional_atomic_tokens_y" } as RealVar
        resultVariable.range.geq(Flt64(-1.0))
        resultVariable.range.leq(Flt64.one)
        val beforeLower = resultVariable.lowerBound!!.value.unwrap()
        val beforeUpper = resultVariable.upperBound!!.value.unwrap()
        val table = ManualTokenTable<Flt64>(Linear, true)

        try {
            assertTrue(table.add(resultVariable) is Ok)
            assertTrue(function.registerAuxiliaryTokens(table) is Failed)
            assertEquals(1, table.tokens.size)
            assertEquals(beforeLower, resultVariable.lowerBound!!.value.unwrap())
            assertEquals(beforeUpper, resultVariable.upperBound!!.value.unwrap())
        } finally {
            table.close()
        }
    }

    @Test
    fun ifThenConstraintWriteFailureDoesNotChangeResultRangeAfterRollback() {
        val x = RealVar("conditional_atomic_write_x")
        val function = IfThenFunction(
            condition = linearPoly(x),
            thenPoly = LinearPolynomial(emptyList(), Flt64(2.0)),
            converter = IntoValue.Identity,
            strictBoundary = Flt64(0.1),
            conditionBounds = ConditionBounds(Flt64(-1.0), Flt64.one),
            thenBounds = ConditionBounds(Flt64(2.0), Flt64(2.0)),
            name = "conditional_atomic_write"
        )
        val resultVariable = function.helperVariables.single { it.name == "conditional_atomic_write_y" } as RealVar
        resultVariable.range.geq(Flt64(-1.0))
        resultVariable.range.leq(Flt64.one)
        val beforeLower = resultVariable.lowerBound!!.value.unwrap()
        val beforeUpper = resultVariable.upperBound!!.value.unwrap()
        val model = FailingLinearMechanismModel(failOnWrite = 3)

        try {
            assertTrue(model.mutableTokens.add(function.helperVariables) is Ok)
            assertTrue(model.addConstraint(zeroRelation("conditional_atomic_existing")) is Ok)
            val beforeConstraints = model.constraints.size

            assertTrue(function.registerConstraints(model) is Failed)
            assertEquals(beforeConstraints, model.constraints.size)
            assertEquals(beforeLower, resultVariable.lowerBound!!.value.unwrap())
            assertEquals(beforeUpper, resultVariable.upperBound!!.value.unwrap())
        } finally {
            model.close()
        }
    }

    @Test
    fun ifThenConstraintConstructionFailureDoesNotChangeResultRange() {
        val x = RealVar("conditional_atomic_construction_x")
        val function = IfThenFunction(
            condition = linearPoly(x),
            thenPoly = LinearPolynomial(emptyList(), Flt64.nan),
            converter = IntoValue.Identity,
            strictBoundary = Flt64(0.1),
            conditionBounds = ConditionBounds(Flt64(-1.0), Flt64.one),
            thenBounds = ConditionBounds(Flt64(-1.0), Flt64.one),
            name = "conditional_atomic_construction"
        )
        val resultVariable = function.helperVariables.single { it.name == "conditional_atomic_construction_y" } as RealVar
        resultVariable.range.geq(Flt64(-1.0))
        resultVariable.range.leq(Flt64.one)
        val beforeLower = resultVariable.lowerBound!!.value.unwrap()
        val beforeUpper = resultVariable.upperBound!!.value.unwrap()
        val model = FailingLinearMechanismModel(failOnWrite = null)

        try {
            assertTrue(model.mutableTokens.add(function.helperVariables) is Ok)
            assertTrue(function.registerConstraints(model) is Failed)
            assertEquals(0, model.constraints.size)
            assertEquals(beforeLower, resultVariable.lowerBound!!.value.unwrap())
            assertEquals(beforeUpper, resultVariable.upperBound!!.value.unwrap())
        } finally {
            model.close()
        }
    }

    @Test
    fun ifThenConstraintFailureRollsBackAutoMaterializedTokens() {
        val x = RealVar("conditional_atomic_materialized_x")
        x.range.geq(Flt64(-1.0))
        x.range.leq(Flt64.one)
        val function = IfThenFunction(
            condition = linearPoly(x),
            thenPoly = LinearPolynomial(emptyList(), Flt64(2.0)),
            converter = IntoValue.Identity,
            strictBoundary = Flt64(0.1),
            conditionBounds = ConditionBounds(Flt64(-1.0), Flt64.one),
            thenBounds = ConditionBounds(Flt64(2.0), Flt64(2.0)),
            name = "conditional_atomic_materialized"
        )
        val materializedVariable = RealVar("conditional_atomic_materialized_helper")
        val model = FailingLinearMechanismModel(failOnWrite = 1)
        model.materializeOnWrite = materializedVariable

        try {
            assertTrue(function.registerConstraints(model) is Failed)
            assertEquals(0, model.constraints.size)
            assertTrue(model.mutableTokens.tokens.none { it.variable === materializedVariable })
        } finally {
            model.close()
        }
    }

    @Test
    fun ifThenConstraintFailureRestoresOriginallyEmptyResultRange() {
        val x = RealVar("conditional_empty_range_x")
        val function = IfThenFunction(
            condition = linearPoly(x),
            thenPoly = LinearPolynomial(emptyList(), Flt64(2.0)),
            converter = IntoValue.Identity,
            strictBoundary = Flt64(0.1),
            conditionBounds = ConditionBounds(Flt64(-1.0), Flt64.one),
            thenBounds = ConditionBounds(Flt64(2.0), Flt64(2.0)),
            name = "conditional_empty_range"
        )
        val resultVariable = function.helperVariables.single {
            it.name == "conditional_empty_range_y"
        } as RealVar
        resultVariable.range.geq(Flt64.one)
        resultVariable.range.leq(Flt64.zero)
        assertNull(resultVariable.range.range)
        val wasSet = resultVariable.range.set
        val model = FailingLinearMechanismModel(failOnWrite = 1)

        try {
            assertTrue(model.mutableTokens.add(function.helperVariables) is Ok)
            assertTrue(function.registerConstraints(model) is Failed)
            assertNull(resultVariable.range.range)
            assertEquals(wasSet, resultVariable.range.set)
        } finally {
            model.close()
        }
    }

    @Test
    fun ifThenRejectsMergedThenOverflowBeforeWritingConstraints() {
        val conditionVariable = RealVar("conditional_ifthen_merged_overflow_condition")
        val thenVariable = RealVar("conditional_ifthen_merged_overflow_then")
        val thenPoly = LinearPolynomial(
            monomials = listOf(
                LinearMonomial(Flt64(1.0e308), thenVariable),
                LinearMonomial(Flt64(1.0e308), thenVariable)
            ),
            constant = Flt64.zero
        )
        val function = IfThenFunction(
            condition = linearPoly(conditionVariable),
            thenPoly = thenPoly,
            converter = IntoValue.Identity,
            strictBoundary = Flt64(0.1),
            conditionBounds = ConditionBounds(Flt64(-1.0), Flt64.one),
            thenBounds = ConditionBounds(Flt64(-1.0), Flt64.one),
            name = "conditional_ifthen_merged_overflow"
        )
        val model = FailingLinearMechanismModel(failOnWrite = null)

        try {
            assertTrue(function.registerAuxiliaryTokens(model.mutableTokens) is Failed)
            assertEquals(0, model.mutableTokens.tokens.size)
            assertTrue(model.addConstraint(zeroRelation("conditional_ifthen_overflow_existing")) is Ok)
            val beforeConstraints = model.constraints.size
            val beforeTokens = model.mutableTokens.tokens.size
            val resultVariable = function.helperVariables.single {
                it.name == "conditional_ifthen_merged_overflow_y"
            } as RealVar
            val beforeRange = resultVariable.range.range

            assertTrue(function.registerConstraints(model) is Failed)
            assertEquals(beforeConstraints, model.constraints.size)
            assertEquals(beforeTokens, model.mutableTokens.tokens.size)
            assertEquals(beforeRange, resultVariable.range.range)
        } finally {
            model.close()
        }
    }

    @Test
    fun ifThenRejectsMergedThenOverflowInFoldedConstraintBeforeWriting() {
        val thenVariable = RealVar("conditional_ifthen_folded_merged_overflow_then")
        val function = IfThenFunction(
            condition = LinearPolynomial(emptyList(), Flt64.one),
            thenPoly = LinearPolynomial(
                monomials = listOf(
                    LinearMonomial(Flt64(1.0e308), thenVariable),
                    LinearMonomial(Flt64(1.0e308), thenVariable)
                ),
                constant = Flt64.zero
            ),
            converter = IntoValue.Identity,
            strictBoundary = Flt64(0.1),
            conditionBounds = ConditionBounds(Flt64.one, Flt64.one),
            thenBounds = ConditionBounds(Flt64(-1.0), Flt64.one),
            name = "conditional_ifthen_folded_merged_overflow"
        )
        val model = FailingLinearMechanismModel(failOnWrite = null)

        try {
            assertTrue(function.registerAuxiliaryTokens(model.mutableTokens) is Failed)
            assertEquals(0, model.mutableTokens.tokens.size)
            assertTrue(model.addConstraint(zeroRelation("conditional_ifthen_folded_overflow_existing")) is Ok)
            val beforeConstraints = model.constraints.size
            val beforeTokens = model.mutableTokens.tokens.size

            assertTrue(function.registerConstraints(model) is Failed)
            assertEquals(beforeConstraints, model.constraints.size)
            assertEquals(beforeTokens, model.mutableTokens.tokens.size)
        } finally {
            model.close()
        }
    }

    @Test
    fun ifThenRejectsMergedThenOverflowInFalseFoldBeforeWriting() {
        val thenVariable = RealVar("conditional_ifthen_false_folded_merged_overflow_then")
        val function = IfThenFunction(
            condition = LinearPolynomial(emptyList(), Flt64.zero),
            thenPoly = LinearPolynomial(
                monomials = listOf(
                    LinearMonomial(Flt64(1.0e308), thenVariable),
                    LinearMonomial(Flt64(1.0e308), thenVariable)
                ),
                constant = Flt64.zero
            ),
            converter = IntoValue.Identity,
            strictBoundary = Flt64(0.1),
            conditionBounds = ConditionBounds(Flt64.zero, Flt64.zero),
            thenBounds = ConditionBounds(Flt64(-1.0), Flt64.one),
            name = "conditional_ifthen_false_folded_merged_overflow"
        )
        val model = FailingLinearMechanismModel(failOnWrite = null)

        try {
            assertTrue(function.registerAuxiliaryTokens(model.mutableTokens) is Failed)
            assertEquals(0, model.mutableTokens.tokens.size)
            assertTrue(model.addConstraint(zeroRelation("conditional_ifthen_false_folded_overflow_existing")) is Ok)
            val beforeConstraints = model.constraints.size
            val beforeTokens = model.mutableTokens.tokens.size

            assertTrue(function.registerConstraints(model) is Failed)
            assertEquals(beforeConstraints, model.constraints.size)
            assertEquals(beforeTokens, model.mutableTokens.tokens.size)
        } finally {
            model.close()
        }
    }

    @Test
    fun factoriesPreserveEverySupportedComparisonRelation() {
        val x = RealVar("conditional_factory_x")
        val xPoly = linearPoly(x)
        val rhs = LinearPolynomial<Flt64>(emptyList(), Flt64.one)
        val lhsRange = ValueRange(
            lb = Flt64(-10.0),
            ub = Flt64(10.0),
            lbInterval = Interval.Closed,
            ubInterval = Interval.Closed,
            constants = Flt64
        ).value!!
        val bounds = ConditionBounds(Flt64(-11.0), Flt64(9.0))

        for (relation in listOf(Comparison.GT, Comparison.GE, Comparison.LT, Comparison.LE)) {
            val input = LinearConstraintInput.from(
                relation = LinearInequality(xPoly, rhs, relation, "conditional_factory_$relation"),
                lhsRange = lhsRange,
                rhsConstant = Flt64.one
            ).value!!
            val ifFunction = IfFunction.from(
                inequality = input,
                converter = IntoValue.Identity,
                strictBoundary = Flt64(0.1),
                bounds = bounds,
                name = "conditional_factory_if_$relation"
            )
            val ifThenFunction = IfThenFunction.from(
                inequality = input,
                converter = IntoValue.Identity,
                strictBoundary = Flt64(0.1),
                conditionBounds = bounds,
                name = "conditional_factory_ifthen_$relation"
            )
            assertEquals(relation, (ifFunction.delegate as IfFunction<Flt64>).relation)
            assertEquals(relation, (ifThenFunction.delegate as IfThenFunction<Flt64>).relation)
        }
    }

    @Test
    fun registrationRejectsMissingBoundsBeforeWritingTokensOrConstraints() {
        val x = RealVar("conditional_missing_bounds_x")
        val y = RealVar("conditional_missing_bounds_y")
        val xPoly = linearPoly(x)
        val yPoly = linearPoly(y)
        val unknownSymbol = object : Symbol {
            override val name = "conditional_unknown_symbol"
            override val displayName: String? = null
        }
        val unknownPoly = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64.one, unknownSymbol)),
            constant = Flt64.zero
        )
        val functions = listOf<MathFunctionSymbol<Flt64>>(
            IfFunction(
                condition = unknownPoly,
                converter = IntoValue.Identity,
                bigM = Flt64(10.0),
                name = "conditional_missing_if"
            ),
            SigmoidFunction(
                condition = unknownPoly,
                converter = IntoValue.Identity,
                bigM = Flt64(10.0),
                name = "conditional_missing_sigmoid"
            ),
            IfInFunction(
                x = unknownPoly,
                lower = Flt64.zero,
                upper = Flt64.one,
                converter = IntoValue.Identity,
                bigM = Flt64(10.0),
                name = "conditional_missing_ifin"
            ),
            IfThenFunction(
                condition = unknownPoly,
                thenPoly = unknownPoly,
                converter = IntoValue.Identity,
                bigM = Flt64(10.0),
                name = "conditional_missing_ifthen"
            ),
            ImplyFunction(
                antecedent = unknownPoly,
                consequent = unknownPoly,
                converter = IntoValue.Identity,
                bigM = Flt64(10.0),
                name = "conditional_missing_imply"
            )
        )

        for (function in functions) {
            val table = ManualTokenTable<Flt64>(Linear, false)
            val before = table.tokens.size
            assertTrue(function.registerAuxiliaryTokens(table) is Failed, function.name)
            assertEquals(before, table.tokens.size, function.name)
        }

        val invalid = IfFunction(
            condition = xPoly,
            converter = IntoValue.Identity,
            relation = Comparison.EQ,
            bounds = ConditionBounds(Flt64(-1.0), Flt64(1.0)),
            name = "conditional_invalid_relation"
        )
        val table = ManualTokenTable<Flt64>(Linear, false)
        assertTrue(invalid.registerAuxiliaryTokens(table) is Failed)
        assertEquals(0, table.tokens.size)

        val invalidBounds = IfFunction(
            condition = unknownPoly,
            converter = IntoValue.Identity,
            bounds = ConditionBounds(Flt64.one, Flt64.zero),
            name = "conditional_invalid_bounds"
        )
        val invalidBoundsTable = ManualTokenTable<Flt64>(Linear, false)
        assertTrue(invalidBounds.registerAuxiliaryTokens(invalidBoundsTable) is Failed)
        assertEquals(0, invalidBoundsTable.tokens.size)

        val narrowX = RealVar("conditional_narrow_ifin_x")
        val narrowInterval = IfInFunction(
            x = linearPoly(narrowX),
            lower = Flt64.zero,
            upper = Flt64(0.05),
            converter = IntoValue.Identity,
            strictBoundary = Flt64(0.1),
            conditionBounds = ConditionBounds(Flt64.zero, Flt64(0.05)),
            name = "conditional_narrow_ifin"
        )
        assertEquals(Flt64.one, narrowInterval.evaluate(mapOf(narrowX to Flt64.zero)))
        assertEquals(Flt64.one, narrowInterval.evaluate(mapOf(narrowX to Flt64(0.05))))
        assertEquals(null, narrowInterval.evaluate(mapOf(narrowX to Flt64(-0.05))))
        val narrowTable = ManualTokenTable<Flt64>(Linear, false)
        assertTrue(narrowInterval.registerAuxiliaryTokens(narrowTable) is Ok)
        assertEquals(3, narrowTable.tokens.size)

        val model = LinearMetaModel<Flt64>(
            name = "conditional_missing_bounds_model",
            converter = IntoValue.Identity
        )
        try {
            assertTrue(model.add(x) is Ok)
            assertTrue(model.minimize(xPoly) is Ok)
            val mechanismResult = runBlocking {
                LinearMechanismModel.invoke<Flt64>(model, concurrent = false)
            }
            assertTrue(mechanismResult is Ok)
            if (mechanismResult is Ok) {
                val mechanism = mechanismResult.value
                val before = mechanism.constraints.size
                assertTrue(functions.first().registerConstraints(mechanism) is Failed)
                assertEquals(before, mechanism.constraints.size)
            }
        } finally {
            model.close()
        }
    }

    @Test
    fun implyRejectsNonFiniteGatedRhsWithoutWritingConstraints() {
        val antecedentVariable = RealVar("conditional_overflow_antecedent")
        antecedentVariable.range.geq(Flt64(-1.0))
        antecedentVariable.range.leq(Flt64(1.0))
        val consequentSymbol = object : Symbol {
            override val name = "conditional_overflow_consequent"
            override val displayName: String? = null
        }
        val antecedent = linearPoly(antecedentVariable)
        val consequent = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64.one, consequentSymbol)),
            constant = Flt64.zero
        )
        val function = ImplyFunction(
            antecedent = antecedent,
            consequent = consequent,
            converter = IntoValue.Identity,
            strictBoundary = Flt64.one,
            antecedentBounds = ConditionBounds(Flt64(-1.0), Flt64.one),
            consequentBounds = ConditionBounds(Flt64(-1.0e308), Flt64(1.0e308)),
            name = "conditional_overflow_imply"
        )
        val model = LinearMetaModel<Flt64>(
            name = "conditional-overflow-model",
            converter = IntoValue.Identity
        )
        try {
            assertTrue(model.add(antecedentVariable) is Ok)
            val auxiliaryTokens = ManualTokenTable<Flt64>(Linear, false)
            assertTrue(function.registerAuxiliaryTokens(auxiliaryTokens) is Failed)
            assertEquals(0, auxiliaryTokens.tokens.size)
            assertTrue(model.minimize(antecedent) is Ok)
            val mechanismResult = runBlocking {
                LinearMechanismModel.invoke<Flt64>(model, concurrent = false)
            }
            assertTrue(mechanismResult is Ok)
            if (mechanismResult is Ok) {
                val mechanism = mechanismResult.value
                val before = mechanism.constraints.size
                assertTrue(function.registerConstraints(mechanism) is Failed)
                assertEquals(before, mechanism.constraints.size)
            }
        } finally {
            model.close()
        }
    }

    @Test
    fun implyValidatesConsequentWhenAntecedentIsConstantFalse() {
        val function = ImplyFunction(
            antecedent = LinearPolynomial(emptyList(), Flt64(-1.0)),
            consequent = LinearPolynomial(emptyList(), Flt64.nan),
            converter = IntoValue.Identity,
            strictBoundary = Flt64.one,
            antecedentBounds = ConditionBounds(Flt64(-1.0), Flt64(-1.0)),
            consequentBounds = ConditionBounds(Flt64(-1.0), Flt64.one),
            name = "conditional_invalid_short_circuit_imply"
        )
        val table = ManualTokenTable<Flt64>(Linear, false)

        assertTrue(function.registerAuxiliaryTokens(table) is Failed)
        assertEquals(0, table.tokens.size)
    }

    @Test
    fun implyRejectsExplicitSentinelBoundsBeforeFalseAntecedentShortCircuit() {
        val function = ImplyFunction(
            antecedent = LinearPolynomial(emptyList(), Flt64(-1.0)),
            consequent = LinearPolynomial(emptyList(), Flt64.zero),
            converter = IntoValue.Identity,
            strictBoundary = Flt64.one,
            antecedentBounds = ConditionBounds(Flt64(-1.0), Flt64(-1.0)),
            consequentBounds = ConditionBounds(Flt64.minimum, Flt64.maximum),
            name = "conditional_sentinel_short_circuit_imply"
        )
        val table = ManualTokenTable<Flt64>(Linear, false)

        assertTrue(function.registerAuxiliaryTokens(table) is Failed)
        assertEquals(0, table.tokens.size)
    }

    @Test
    fun implyRejectsInvalidConsequentBoundsBeforeFalseAntecedentShortCircuit() {
        val function = ImplyFunction(
            antecedent = LinearPolynomial(emptyList(), Flt64(-1.0)),
            consequent = LinearPolynomial(emptyList(), Flt64.zero),
            converter = IntoValue.Identity,
            strictBoundary = Flt64.one,
            antecedentBounds = ConditionBounds(Flt64(-1.0), Flt64(-1.0)),
            consequentBounds = ConditionBounds(Flt64.one, Flt64.zero),
            name = "conditional_invalid_bounds_short_circuit_imply"
        )
        val table = ManualTokenTable<Flt64>(Linear, false)

        assertTrue(function.registerAuxiliaryTokens(table) is Failed)
        assertEquals(0, table.tokens.size)
    }

    @Test
    fun sharedConditionAdapterRejectsNonFiniteEffectivePolynomialValues() {
        val variable = RealVar("conditional_effective_overflow_x")
        val indicator = BinVar("conditional_effective_overflow_indicator")
        val repeatedTerms = LinearPolynomial(
            monomials = listOf(
                LinearMonomial(Flt64(1.0e308), variable),
                LinearMonomial(Flt64(1.0e308), variable)
            ),
            constant = Flt64.zero
        )
        val overflowResult = relationIndicatorConstraints(
            poly = repeatedTerms,
            indicator = indicator,
            relation = Comparison.GT,
            bounds = ConditionBounds(Flt64(-1.0), Flt64.one),
            strictBoundary = Flt64(0.1),
            namePrefix = "conditional_effective_overflow"
        )
        assertTrue(overflowResult is Failed)

        val nonFiniteResult = relationIndicatorConstraints(
            poly = LinearPolynomial(emptyList(), Flt64.nan),
            indicator = indicator,
            relation = Comparison.GT,
            bounds = ConditionBounds(Flt64(-1.0), Flt64.one),
            strictBoundary = Flt64(0.1),
            namePrefix = "conditional_non_finite"
        )
        assertTrue(nonFiniteResult is Failed)
    }

    @Test
    fun sharedConditionAdapterRejectsAnInvalidIndicatorContract() {
        val x = RealVar("conditional_invalid_indicator_x")
        val realIndicator = RealVar("conditional_real_indicator")
        val poly = linearPoly(x)
        val bounds = ConditionBounds(Flt64(-1.0), Flt64.one)

        assertTrue(
            relationIndicatorConstraints(
                poly = poly,
                indicator = realIndicator,
                relation = Comparison.GT,
                bounds = bounds,
                strictBoundary = Flt64(0.1),
                namePrefix = "conditional_real_indicator"
            ) is Failed
        )

        val selfIndicator = BinVar("conditional_self_indicator")
        assertTrue(
            relationIndicatorConstraints(
                poly = LinearPolynomial(
                    monomials = listOf(LinearMonomial(Flt64.one, selfIndicator)),
                    constant = Flt64.zero
                ),
                indicator = selfIndicator,
                relation = Comparison.GT,
                bounds = bounds,
                strictBoundary = Flt64(0.1),
                namePrefix = "conditional_self_reference"
            ) is Failed
        )
    }

    @Test
    fun sharedClassifierRejectsNonSolverRepresentableValues() {
        assertTrue(classify(Flt64.zero, Comparison.GT, Flt64.maximum) is Failed)
        assertTrue(classify(Flt64.maximum, Comparison.GT, Flt64(0.1)) is Failed)
    }

    @Test
    fun ifInRejectsEndpointAndDifferenceOverflowBeforeRegistration() {
        val endpointOverflow = IfInFunction(
            x = LinearPolynomial(emptyList(), Flt64(1.0e308)),
            lower = Flt64(-1.0e308),
            upper = Flt64(1.0e308),
            converter = IntoValue.Identity,
            conditionBounds = ConditionBounds(Flt64(1.0e308), Flt64(1.0e308)),
            name = "conditional_ifin_endpoint_overflow"
        )
        val endpointTable = ManualTokenTable<Flt64>(Linear, false)
        assertTrue(endpointOverflow.registerAuxiliaryTokens(endpointTable) is Failed)
        assertEquals(0, endpointTable.tokens.size)

        val repeatedVariable = RealVar("conditional_ifin_effective_x")
        val repeatedTerms = LinearPolynomial(
            monomials = listOf(
                LinearMonomial(Flt64(1.0e308), repeatedVariable),
                LinearMonomial(Flt64(1.0e308), repeatedVariable)
            ),
            constant = Flt64.zero
        )
        val polynomialOverflow = IfInFunction(
            x = repeatedTerms,
            lower = Flt64.zero,
            upper = Flt64.one,
            converter = IntoValue.Identity,
            conditionBounds = ConditionBounds(Flt64.zero, Flt64.zero),
            name = "conditional_ifin_polynomial_overflow"
        )
        val polynomialTable = ManualTokenTable<Flt64>(Linear, false)
        assertTrue(polynomialOverflow.registerAuxiliaryTokens(polynomialTable) is Failed)
        assertEquals(0, polynomialTable.tokens.size)
    }

    @Test
    fun ifInClassificationRejectsOverflowingEndpointDifferences() {
        val function = IfInFunction(
            x = LinearPolynomial(emptyList(), Flt64(1.0e308)),
            lower = Flt64(-1.0e308),
            upper = Flt64(1.0e308),
            converter = IntoValue.Identity,
            name = "conditional_ifin_classify_overflow"
        )

        assertTrue(function.classify(emptyMap()) is Failed)
        assertNull(function.evaluate(emptyMap()))
    }

    @Test
    fun ifInUsesInclusiveEndpointsAndThreeValuedBoundarySemantics() {
        val x = RealVar("conditional_ifin_boundary_x")
        val function = IfInFunction(
            x = linearPoly(x),
            lower = Flt64.zero,
            upper = Flt64.one,
            converter = IntoValue.Identity,
            strictBoundary = Flt64(0.1),
            name = "conditional_ifin_boundary"
        )

        val cases = mapOf(
            Flt64(-0.2) to TruthValue.False,
            Flt64(-0.05) to TruthValue.Undefined,
            Flt64.zero to TruthValue.True,
            Flt64(0.5) to TruthValue.True,
            Flt64.one to TruthValue.True,
            Flt64(1.05) to TruthValue.Undefined,
            Flt64(1.2) to TruthValue.False
        )
        for ((value, expected) in cases) {
            val result = function.classify(mapOf<Symbol, Flt64>(x to value))
            assertTrue(result is Ok, "IfIn classification should succeed for x=$value")
            if (result is Ok) {
                assertEquals(expected, result.value, "unexpected IfIn classification for x=$value")
            }
        }
    }

    @Test
    fun ifInRejectsAnUnprovenNonUnitDeltaBeforeRegisteringTokens() {
        val x = RealVar("conditional_ifin_unproven_delta_x")
        val function = IfInFunction(
            x = linearPoly(x),
            lower = Flt64.zero,
            upper = Flt64.one,
            converter = IntoValue.Identity,
            strictBoundary = Flt64(0.1),
            conditionBounds = ConditionBounds(Flt64(-1.0), Flt64(2.0)),
            delta = Flt64(5.0),
            name = "conditional_ifin_unproven_delta"
        )
        val table = ManualTokenTable<Flt64>(Linear, false)

        assertTrue(function.registerAuxiliaryTokens(table) is Failed)
        assertEquals(0, table.tokens.size)
    }

    @Test
    fun ifInRejectsAnInvertedIntervalBeforeEvaluatingOrRegistering() {
        val function = IfInFunction(
            x = LinearPolynomial(emptyList(), Flt64.zero),
            lower = Flt64.one,
            upper = Flt64.zero,
            converter = IntoValue.Identity,
            conditionBounds = ConditionBounds(Flt64.zero, Flt64.zero),
            name = "conditional_ifin_inverted"
        )
        val table = ManualTokenTable<Flt64>(Linear, false)

        assertTrue(function.classify(emptyMap()) is Failed)
        assertTrue(function.registerAuxiliaryTokens(table) is Failed)
        assertEquals(0, table.tokens.size)
    }

    @Test
    fun implyRejectsMissingConsequentBoundsBeforeFalseAntecedentShortCircuit() {
        val consequentSymbol = object : Symbol {
            override val name = "conditional_missing_consequent"
            override val displayName: String? = null
        }
        val function = ImplyFunction(
            antecedent = LinearPolynomial(emptyList(), Flt64(-1.0)),
            consequent = LinearPolynomial(
                monomials = listOf(LinearMonomial(Flt64.one, consequentSymbol)),
                constant = Flt64.zero
            ),
            converter = IntoValue.Identity,
            strictBoundary = Flt64.one,
            antecedentBounds = ConditionBounds(Flt64(-1.0), Flt64(-1.0)),
            name = "conditional_missing_bounds_short_circuit_imply"
        )
        val table = ManualTokenTable<Flt64>(Linear, false)

        assertTrue(function.registerAuxiliaryTokens(table) is Failed)
        assertEquals(0, table.tokens.size)
    }

    @Test
    fun implyFalseAntecedentDoesNotRegisterUndefinedConsequentRelation() {
        val consequentSymbol = RealVar("conditional_irrelevant_consequent")
        val function = ImplyFunction(
            antecedent = LinearPolynomial(emptyList(), Flt64(-1.0)),
            consequent = LinearPolynomial(
                monomials = listOf(LinearMonomial(Flt64.one, consequentSymbol)),
                constant = Flt64.zero
            ),
            converter = IntoValue.Identity,
            strictBoundary = Flt64.one,
            antecedentBounds = ConditionBounds(Flt64(-1.0), Flt64(-1.0)),
            consequentBounds = ConditionBounds(Flt64(-1.0), Flt64.one),
            name = "conditional_irrelevant_consequent_imply"
        )
        val model = FailingLinearMechanismModel(failOnWrite = null)

        try {
            assertTrue(function.registerAuxiliaryTokens(model.mutableTokens) is Ok)
            assertTrue(function.registerConstraints(model) is Ok)
            assertEquals(2, model.constraints.size)
            assertTrue(model.constraints.all { it.name.endsWith("fold_false") || it.name.endsWith("fold_irrelevant") })
        } finally {
            model.close()
        }
    }

    @Test
    fun implyConstraintWriteFailureRollsBackAllConstraints() {
        val antecedentVariable = RealVar("conditional_atomic_imply_antecedent")
        antecedentVariable.range.geq(Flt64(-1.0))
        antecedentVariable.range.leq(Flt64.one)
        val consequentVariable = RealVar("conditional_atomic_imply_consequent")
        consequentVariable.range.geq(Flt64(-1.0))
        consequentVariable.range.leq(Flt64.one)
        val function = ImplyFunction(
            antecedent = linearPoly(antecedentVariable),
            consequent = linearPoly(consequentVariable),
            converter = IntoValue.Identity,
            strictBoundary = Flt64(0.1),
            antecedentBounds = ConditionBounds(Flt64(-1.0), Flt64.one),
            consequentBounds = ConditionBounds(Flt64(-1.0), Flt64.one),
            name = "conditional_atomic_imply"
        )
        val model = FailingLinearMechanismModel(failOnWrite = 1)

        try {
            assertTrue(model.mutableTokens.add(function.helperVariables) is Ok)
            assertTrue(model.addConstraint(zeroRelation("conditional_atomic_imply_existing")) is Ok)
            val beforeConstraints = model.constraints.size
            val beforeTokens = model.mutableTokens.tokens.size

            assertTrue(function.registerConstraints(model) is Failed)
            assertEquals(beforeConstraints, model.constraints.size)
            assertEquals(beforeTokens, model.mutableTokens.tokens.size)
        } finally {
            model.close()
        }
    }

    private fun assertClassification(
        value: Flt64,
        relation: Comparison,
        expected: TruthValue,
        gap: Flt64
    ) {
        val result = classify(value, relation, gap)
        assertTrue(result is Ok, "${relation.symbol} at $value should classify successfully")
        if (result is Ok) {
            assertEquals(expected, result.value)
        }
    }

    private fun linearPoly(variable: RealVar): LinearPolynomial<Flt64> {
        return LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64.one, variable)),
            constant = Flt64.zero
        )
    }

    private fun zeroRelation(name: String): LinearInequality<Flt64> {
        return LinearInequality(
            lhs = LinearPolynomial(emptyList(), Flt64.zero),
            rhs = LinearPolynomial(emptyList(), Flt64.zero),
            comparison = Comparison.LE,
            name = name
        )
    }

    private class FailingLinearMechanismModel(
        private val failOnWrite: Int?
    ) : AbstractLinearMechanismModel<Flt64> {
        override var name: String = "conditional-failing-mechanism"
        val mutableTokens = AutoTokenTable<Flt64>(Linear, false)
        override val tokens: AbstractTokenTable<Flt64> = mutableTokens
        override val identityRegistry: ModelElementIdentityRegistry? = null
        override val objectFunction: Object = SingleObject(
            category = ObjectCategory.Minimum,
            subObjects = emptyList<LinearSubObject<Flt64>>()
        )

        private val storedConstraints = mutableListOf<Constraint<Flt64, *>>()
        override val constraints: List<Constraint<Flt64, *>> get() = storedConstraints
        var materializeOnWrite: RealVar? = null
        private var writeCount = 0

        override fun addConstraint(
            relation: LinearInequality<Flt64>,
            name: String?,
            from: Pair<IntermediateSymbol<out Flt64>, Boolean>?
        ): Try {
            val write = writeCount++
            materializeOnWrite?.let { tokens.find(it) }
            storedConstraints.add(
                LinearConstraintImpl(
                    lhs = emptyList(),
                    sign = ConstraintRelation.Equal,
                    rhs = Flt64.zero,
                    name = name.orEmpty()
                )
            )
            return if (write == failOnWrite) {
                Failed(
                    Err(
                        ErrorCode.ApplicationFailed,
                        "约束构造失败 / Constraint construction failed"
                    )
                )
            } else {
                ok
            }
        }

        override fun rollbackConstraintsTo(size: Int): Try {
            if (size < 0 || size > storedConstraints.size) {
                return Failed(
                    Err(
                        ErrorCode.IllegalArgument,
                        "约束回滚位置无效：$size / Invalid constraint rollback position: $size"
                    )
                )
            }
            storedConstraints.subList(size, storedConstraints.size).clear()
            return ok
        }
    }
}
