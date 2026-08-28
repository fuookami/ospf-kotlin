package fuookami.ospf.kotlin.core.symbol.function

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import fuookami.ospf.kotlin.utils.error.Err
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.math.algebra.number.Flt64
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
import fuookami.ospf.kotlin.core.model.mechanism.LinearConstraintImpl
import fuookami.ospf.kotlin.core.model.mechanism.LinearSubObject
import fuookami.ospf.kotlin.core.model.mechanism.Object
import fuookami.ospf.kotlin.core.model.mechanism.SingleObject
import fuookami.ospf.kotlin.core.solver.report.ModelElementIdentityRegistry
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.IntermediateSymbol
import fuookami.ospf.kotlin.core.token.AbstractTokenTable
import fuookami.ospf.kotlin.core.token.AutoTokenTable
import fuookami.ospf.kotlin.core.token.ManualTokenTable
import fuookami.ospf.kotlin.core.variable.RealVar

class ImplyFunctionRegressionTest {
    @Test
    fun explicitSentinelConsequentBoundsFailBeforeFalseAntecedentShortCircuit() {
        val function = ImplyFunction(
            antecedent = constant(Flt64(-1.0)),
            consequent = constant(Flt64.zero),
            converter = IntoValue.Identity,
            strictBoundary = Flt64.one,
            antecedentBounds = ConditionBounds(Flt64(-1.0), Flt64(-1.0)),
            consequentBounds = ConditionBounds(Flt64.minimum, Flt64.maximum),
            name = "imply_sentinel_consequent"
        )
        val tokens = ManualTokenTable<Flt64>(Linear, false)

        try {
            val result = function.registerAuxiliaryTokens(tokens)
            assertTrue(result is Failed)
            assertEquals(0, tokens.tokens.size)
        } finally {
            tokens.close()
        }
    }

    @Test
    fun explicitSentinelBoundsAreRejectedForEitherOperandAndRegistrationPath() {
        val validBounds = ConditionBounds(Flt64(-1.0), Flt64.one)
        val cases = listOf(
            ConditionBounds(Flt64.minimum, Flt64.one) to validBounds,
            ConditionBounds(Flt64(-1.0), Flt64.maximum) to validBounds,
            validBounds to ConditionBounds(Flt64.minimum, Flt64.one),
            validBounds to ConditionBounds(Flt64(-1.0), Flt64.maximum)
        )

        for ((antecedentBounds, consequentBounds) in cases) {
            val function = ImplyFunction(
                antecedent = constant(Flt64(-1.0)),
                consequent = constant(Flt64.zero),
                converter = IntoValue.Identity,
                strictBoundary = Flt64.one,
                antecedentBounds = antecedentBounds,
                consequentBounds = consequentBounds,
                name = "imply_explicit_sentinel_${cases.indexOf(antecedentBounds to consequentBounds)}"
            )
            val tokens = ManualTokenTable<Flt64>(Linear, false)
            val model = FailingLinearMechanismModel()

            try {
                assertTrue(function.registerAuxiliaryTokens(tokens) is Failed)
                assertEquals(0, tokens.tokens.size)
                assertTrue(function.registerConstraints(model) is Failed)
                assertEquals(0, model.constraints.size)
            } finally {
                tokens.close()
                model.close()
            }
        }
    }

    @Test
    fun invalidConsequentBoundsFailEvenWhenAntecedentIsConstantFalse() {
        val function = ImplyFunction(
            antecedent = constant(Flt64(-1.0)),
            consequent = constant(Flt64.zero),
            converter = IntoValue.Identity,
            strictBoundary = Flt64.one,
            antecedentBounds = ConditionBounds(Flt64(-1.0), Flt64(-1.0)),
            consequentBounds = ConditionBounds(Flt64.one, Flt64(-1.0)),
            name = "imply_invalid_false_antecedent"
        )
        val model = FailingLinearMechanismModel()
        val tokens = ManualTokenTable<Flt64>(Linear, false)

        try {
            assertTrue(function.registerAuxiliaryTokens(tokens) is Failed)
            assertEquals(0, tokens.tokens.size)
            assertTrue(function.registerConstraints(model) is Failed)
            assertEquals(0, model.constraints.size)
        } finally {
            tokens.close()
            model.close()
        }
    }

    @Test
    fun subtractingLowerRelaxationThatOverflowsFailsBeforeWritingConstraints() {
        val antecedentVariable = boundedVariable("imply_rhs_overflow_antecedent")
        val unknownConsequent = object : Symbol {
            override val name: String = "imply_rhs_overflow_consequent"
            override val displayName: String? = null
        }
        val function = ImplyFunction(
            antecedent = linear(antecedentVariable),
            consequent = LinearPolynomial(
                monomials = listOf(LinearMonomial(Flt64.one, unknownConsequent)),
                constant = Flt64.zero
            ),
            converter = IntoValue.Identity,
            strictBoundary = Flt64.one,
            antecedentBounds = ConditionBounds(Flt64(-1.0), Flt64.one),
            consequentBounds = ConditionBounds(Flt64(-1.0e308), Flt64(1.0e308)),
            name = "imply_rhs_overflow"
        )
        val model = FailingLinearMechanismModel()

        try {
            val result = function.registerConstraints(model)
            assertTrue(result is Failed)
            assertEquals(0, model.constraints.size)
            assertEquals(0, model.relations.size)
        } finally {
            model.close()
        }
    }

    @Test
    fun finalFlattenedConstraintOverflowFailsBeforeModelWrite() {
        val function = ImplyFunction(
            antecedent = constant(Flt64.one),
            consequent = constant(Flt64(1.0e308)),
            converter = IntoValue.Identity,
            strictBoundary = Flt64.one,
            antecedentBounds = ConditionBounds(Flt64.one, Flt64.one),
            consequentBounds = ConditionBounds(Flt64(-1.0e308), Flt64(1.0e308)),
            name = "imply_final_flatten_overflow"
        )
        val model = FailingLinearMechanismModel()

        try {
            val result = function.registerConstraints(model)
            assertTrue(result is Failed)
            assertEquals(0, model.constraints.size)
            assertEquals(0, model.relations.size)
        } finally {
            model.close()
        }
    }

    @Test
    fun constraintWritesRollBackWhenImplyWriteFailsInTheMiddle() {
        val function = validFunction("imply_atomic_write")
        val model = FailingLinearMechanismModel(failOnWrite = 2)

        try {
            val result = function.registerConstraints(model)
            assertTrue(result is Failed)
            assertEquals(0, model.constraints.size)
            assertEquals(0, model.relations.size)
        } finally {
            model.close()
        }
    }

    @Test
    fun auxiliaryTokenBatchFailureRestoresExistingTokenAndIndex() {
        val function = validFunction("imply_atomic_tokens")
        val table = ManualTokenTable<Flt64>(Linear, true)

        try {
            assertTrue(table.add(function.consequentIndicatorVar) is Ok)
            val existingToken = table.find(function.consequentIndicatorVar)
            assertTrue(existingToken != null)

            val result = function.registerAuxiliaryTokens(table)

            assertTrue(result is Failed)
            assertEquals(1, table.tokens.size)
            assertSame(existingToken, table.find(function.consequentIndicatorVar))
            assertNull(table.find(function.antecedentIndicatorVar))
            assertEquals(0, table.indexOf(existingToken!!))

            val next = RealVar("imply_atomic_tokens_next")
            assertTrue(table.add(next) is Ok)
            assertEquals(1, table.indexOf(next))
        } finally {
            table.close()
        }
    }

    @Test
    fun constantFoldsAndClassifierUsesThreeValuedImplicationSemantics() {
        val falseAntecedent = ImplyFunction(
            antecedent = constant(Flt64(-1.0)),
            consequent = constant(Flt64.nan),
            converter = IntoValue.Identity,
            strictBoundary = Flt64(0.1),
            name = "imply_constant_false"
        )
        val falseClassification = falseAntecedent.classify(emptyMap())
        assertTrue(falseClassification is Ok)
        if (falseClassification is Ok) {
            assertEquals(TruthValue.True, falseClassification.value)
        }

        val x = RealVar("imply_three_valued_x")
        val y = RealVar("imply_three_valued_y")
        val function = ImplyFunction(
            antecedent = linear(x),
            consequent = linear(y),
            converter = IntoValue.Identity,
            strictBoundary = Flt64(0.1),
            name = "imply_three_valued"
        )

        val falseAntecedentClassification = function.classify(
            mapOf(x to Flt64.zero)
        )
        assertTrue(falseAntecedentClassification is Ok)
        if (falseAntecedentClassification is Ok) {
            assertEquals(TruthValue.True, falseAntecedentClassification.value)
        }

        val trueAntecedentClassification = function.classify(
            mapOf(x to Flt64(0.2), y to Flt64(0.2))
        )
        assertTrue(trueAntecedentClassification is Ok)
        if (trueAntecedentClassification is Ok) {
            assertEquals(TruthValue.True, trueAntecedentClassification.value)
        }

        val falseConsequentClassification = function.classify(
            mapOf(x to Flt64(0.2), y to Flt64.zero)
        )
        assertTrue(falseConsequentClassification is Ok)
        if (falseConsequentClassification is Ok) {
            assertEquals(TruthValue.False, falseConsequentClassification.value)
        }

        val undefinedAntecedentClassification = function.classify(
            mapOf(x to Flt64(0.05))
        )
        assertTrue(undefinedAntecedentClassification is Ok)
        if (undefinedAntecedentClassification is Ok) {
            assertEquals(TruthValue.Undefined, undefinedAntecedentClassification.value)
        }
    }

    private fun constant(value: Flt64): LinearPolynomial<Flt64> {
        return LinearPolynomial(emptyList(), value)
    }

    private fun linear(variable: RealVar): LinearPolynomial<Flt64> {
        return LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64.one, variable)),
            constant = Flt64.zero
        )
    }

    private fun boundedVariable(name: String): RealVar {
        return RealVar(name).also { variable ->
            variable.range.geq(Flt64(-1.0))
            variable.range.leq(Flt64.one)
        }
    }

    private fun validFunction(name: String): ImplyFunction<Flt64> {
        val antecedent = boundedVariable("${name}_antecedent")
        val consequent = boundedVariable("${name}_consequent")
        return ImplyFunction(
            antecedent = linear(antecedent),
            consequent = linear(consequent),
            converter = IntoValue.Identity,
            strictBoundary = Flt64(0.1),
            antecedentBounds = ConditionBounds(Flt64(-1.0), Flt64.one),
            consequentBounds = ConditionBounds(Flt64(-1.0), Flt64.one),
            name = name
        )
    }

    private class FailingLinearMechanismModel(
        private val failOnWrite: Int? = null
    ) : AbstractLinearMechanismModel<Flt64> {
        override var name: String = "imply-failing-mechanism"
        override val tokens: AbstractTokenTable<Flt64> = AutoTokenTable(Linear, false)
        override val identityRegistry: ModelElementIdentityRegistry? = null
        override val objectFunction: Object = SingleObject(
            category = ObjectCategory.Minimum,
            subObjects = emptyList<LinearSubObject<Flt64>>()
        )

        val relations = mutableListOf<LinearInequality<Flt64>>()
        private val storedConstraints = mutableListOf<Constraint<Flt64, *>>()
        override val constraints: List<Constraint<Flt64, *>> get() = storedConstraints
        private var writeCount = 0

        override fun addConstraint(
            relation: LinearInequality<Flt64>,
            name: String?,
            from: Pair<IntermediateSymbol<out Flt64>, Boolean>?
        ): Try {
            val write = writeCount++
            relations += relation
            storedConstraints += LinearConstraintImpl(
                lhs = emptyList(),
                sign = ConstraintRelation.Equal,
                rhs = Flt64.zero,
                name = name.orEmpty()
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
            relations.subList(size, relations.size).clear()
            return ok
        }
    }
}
