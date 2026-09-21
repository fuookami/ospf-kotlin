package fuookami.ospf.kotlin.core.symbol.function

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.Test
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.mechanism.Object
import fuookami.ospf.kotlin.core.model.mechanism.Constraint
import fuookami.ospf.kotlin.core.model.mechanism.SingleObject
import fuookami.ospf.kotlin.core.model.mechanism.LinearSubObject
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMechanismModel
import fuookami.ospf.kotlin.core.token.AutoTokenTable
import fuookami.ospf.kotlin.core.token.ManualTokenTable
import fuookami.ospf.kotlin.core.token.AbstractTokenTable
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.solver.report.ModelElementIdentityRegistry
import fuookami.ospf.kotlin.core.symbol.IntermediateSymbol
import fuookami.ospf.kotlin.core.variable.RealVar

/** [InequalityFunction] 契约测试。 / Dedicated contract tests. */
class InequalityFunctionDedicatedTest {
    @Test
    fun symbolImplementsMathFunctionContract() {
        assertTrue(MathFunctionSymbol::class.java.isAssignableFrom(InequalityFunction::class.java))
    }

    @Test
    fun lessOrEqualTrueBranchDoesNotRequireViolation() {
        val variable = RealVar("inequality_le_x")
        val function = InequalityFunction(
            lhs = LinearPolynomial(listOf(LinearMonomial(Flt64.one, variable)), Flt64.zero),
            rhs = Flt64.zero,
            sign = Comparison.LE,
            converter = IntoValue.Identity,
            bigM = Flt64(10.0),
            tolerance = Flt64(0.1),
            name = "inequality_le"
        )
        val model = CapturingLinearMechanismModel()

        try {
            assertTrue(function.registerConstraints(model) is Ok)
            assertEquals(2, model.relations.size)
            val violated = model.relations.single { it.name == "inequality_le_violated" }
            assertEquals(Comparison.GE, violated.comparison)
            assertEquals(Flt64(10.1), violated.lhs.monomials.single { it.symbol == function.helperVariables.single() }.coefficient)
            assertEquals(Flt64(0.1), violated.rhs.constant)
        } finally {
            model.close()
        }
    }

    @Test
    fun relationsPreserveBothBranchesAndDomainEndpoints() {
        for (sign in listOf(Comparison.LE, Comparison.GE, Comparison.LT, Comparison.GT)) {
            val variable = RealVar("inequality_branch_x")
            val function = InequalityFunction(
                lhs = LinearPolynomial(listOf(LinearMonomial(Flt64.one, variable)), Flt64(3.0)),
                rhs = Flt64(3.0),
                sign = sign,
                converter = IntoValue.Identity,
                bigM = Flt64(10.0),
                tolerance = Flt64(0.1),
                strictBoundary = Flt64(0.5),
                name = "inequality_branch"
            )
            val model = CapturingLinearMechanismModel()
            try {
                assertTrue(function.registerConstraints(model) is Ok)
                for (input in listOf(-10.0, -1.0, -0.5, -0.1, -0.05, 0.0, 0.05, 0.1, 0.5, 1.0, 10.0)) {
                    for (flag in listOf(0.0, 1.0)) {
                        val values = mapOf<Symbol, Flt64>(
                            variable to Flt64(input), function.helperVariables.single() to Flt64(flag)
                        )
                        val feasible = model.relations.all { relation ->
                            val left = relation.lhs.evaluateWith(values)!!
                            val right = relation.rhs.constant
                            when (relation.comparison) {
                                Comparison.LE -> left.toDouble() <= right.toDouble() + 1e-12
                                Comparison.GE -> left.toDouble() >= right.toDouble() - 1e-12
                                else -> false
                            }
                        }
                        val expected = when (sign) {
                            Comparison.LE -> if (flag == 1.0) input <= 0.0 else input >= 0.1
                            Comparison.GE -> if (flag == 1.0) input >= 0.0 else input <= -0.1
                            Comparison.LT -> if (flag == 1.0) input <= -0.5 else input >= 0.0
                            Comparison.GT -> if (flag == 1.0) input >= 0.5 else input <= 0.0
                            else -> false
                        }
                        assertEquals(expected, feasible, "$sign input=$input flag=$flag")
                    }
                    if (sign == Comparison.LT || sign == Comparison.GT) {
                        val expectedValue = when {
                            sign == Comparison.LT && input <= -0.5 -> Flt64.one
                            sign == Comparison.GT && input >= 0.5 -> Flt64.one
                            sign == Comparison.LT && input >= 0.0 -> Flt64.zero
                            sign == Comparison.GT && input <= 0.0 -> Flt64.zero
                            else -> null
                        }
                        assertEquals(expectedValue, function.evaluate(mapOf(variable to Flt64(input))))
                    }
                }
            } finally {
                model.close()
            }
        }
    }

    @Test
    fun invalidStrictParametersFailBeforeRegistration() {
        for ((bigM, gap) in listOf(
            Flt64.zero to Flt64.one,
            Flt64(-1.0) to Flt64.one,
            Flt64.nan to Flt64.one,
            Flt64.one to Flt64.zero,
            Flt64.one to Flt64(-1.0),
            Flt64.one to Flt64.nan
        )) {
            val function = InequalityFunction(
                lhs = LinearPolynomial(emptyList(), Flt64.zero),
                rhs = Flt64.zero,
                sign = Comparison.GT,
                converter = IntoValue.Identity,
                bigM = bigM,
                strictBoundary = gap,
                name = "invalid_strict"
            )
            val model = CapturingLinearMechanismModel()
            try {
                ManualTokenTable<Flt64>(Linear, false).use { tokens ->
                    assertTrue(function.registerAuxiliaryTokens(tokens) is Failed)
                    assertTrue(tokens.tokens.isEmpty())
                }
                assertTrue(function.registerConstraints(model) is Failed)
                assertTrue(model.relations.isEmpty())
            } finally {
                model.close()
            }
        }
    }

    @Test
    fun equalityAndInequalityAreComplementaryOutsideTheGap() {
        for (sign in listOf(Comparison.EQ, Comparison.NE)) {
            val variable = RealVar("equality_input")
            val function = InequalityFunction(
                lhs = LinearPolynomial(listOf(LinearMonomial(Flt64.one, variable)), Flt64(3.0)),
                rhs = Flt64(3.0),
                sign = sign,
                converter = IntoValue.Identity,
                bigM = Flt64(10.0),
                tolerance = Flt64(0.125),
                strictBoundary = Flt64(0.5),
                name = "equality_band"
            )
            val model = CapturingLinearMechanismModel()
            try {
                ManualTokenTable<Flt64>(Linear, false).use { tokens ->
                    assertTrue(function.registerAuxiliaryTokens(tokens) is Ok)
                    assertEquals(2, tokens.tokens.size)
                }
                assertTrue(function.registerConstraints(model) is Ok)
                assertEquals(4, model.relations.size)
                for (input in listOf(-10.0, -0.5, -0.25, -0.125, -0.0625, 0.0, 0.0625, 0.125, 0.25, 0.5, 10.0)) {
                    val distance = kotlin.math.abs(input)
                    val expectedEqual = when {
                        distance <= 0.125 -> true
                        distance >= 0.5 -> false
                        else -> null
                    }
                    val expectedFlag = expectedEqual?.let { if (it == (sign == Comparison.EQ)) Flt64.one else Flt64.zero }
                    assertEquals(expectedFlag, function.evaluate(mapOf(variable to Flt64(input))))
                    for (flag in listOf(Flt64.zero, Flt64.one)) {
                        val feasible = listOf(Flt64.zero, Flt64.one).any { side ->
                            val values = mapOf<Symbol, Flt64>(
                                variable to Flt64(input),
                                function.helperVariables[0] to flag,
                                function.helperVariables[1] to side
                            )
                            model.relations.all { relation ->
                                val left = relation.lhs.evaluateWith(values)!!.toDouble()
                                val right = relation.rhs.constant.toDouble()
                                when (relation.comparison) {
                                    Comparison.LE -> left <= right + 1e-12
                                    Comparison.GE -> left >= right - 1e-12
                                    else -> false
                                }
                            }
                        }
                        assertEquals(expectedFlag == flag, feasible, "$sign input=$input flag=$flag")
                    }
                }
                assertEquals(null, function.evaluate(mapOf(variable to Flt64.nan)))
            } finally {
                model.close()
            }
        }
    }

    @Test
    fun invalidEqualityBandsFailWithoutWritingTokensOrConstraints() {
        for (sign in listOf(Comparison.EQ, Comparison.NE)) {
            for ((tolerance, boundary) in listOf(
                Flt64(0.5) to Flt64(0.5),
                Flt64.one to Flt64(0.5),
                Flt64(-0.1) to Flt64(0.5),
                Flt64.nan to Flt64(0.5),
                Flt64.zero to Flt64.nan
            )) {
                val function = InequalityFunction(
                    lhs = LinearPolynomial(emptyList(), Flt64.zero),
                    rhs = Flt64.zero,
                    sign = sign,
                    converter = IntoValue.Identity,
                    bigM = Flt64.one,
                    tolerance = tolerance,
                    strictBoundary = boundary,
                    name = "invalid_band"
                )
                val model = CapturingLinearMechanismModel()
                try {
                ManualTokenTable<Flt64>(Linear, false).use { tokens ->
                    assertTrue(function.registerAuxiliaryTokens(tokens) is Failed)
                        assertTrue(tokens.tokens.isEmpty())
                    }
                assertTrue(function.registerConstraints(model) is Failed)
                    assertTrue(model.relations.isEmpty())
                    assertEquals(null, function.evaluate(emptyMap()))
                } finally {
                    model.close()
                }
            }
        }
    }

    @Test
    fun nonStrictEvaluationUsesToleranceGapAndRejectsNonFiniteValues() {
        for (sign in listOf(Comparison.LE, Comparison.GE)) {
            val variable = RealVar("non_strict_evaluate")
            val function = InequalityFunction(
                lhs = LinearPolynomial(listOf(LinearMonomial(Flt64.one, variable)), Flt64(3.0)),
                rhs = Flt64(3.0),
                sign = sign,
                converter = IntoValue.Identity,
                tolerance = Flt64(0.125),
                strictBoundary = Flt64(0.5),
                name = "non_strict_evaluate"
            )
            for (input in listOf(-1.0, -0.125, -0.0625, 0.0, 0.0625, 0.125, 1.0)) {
                val expected = if (sign == Comparison.LE) {
                    when {
                        input <= 0.0 -> Flt64.one
                        input >= 0.125 -> Flt64.zero
                        else -> null
                    }
                } else {
                    when {
                        input >= 0.0 -> Flt64.one
                        input <= -0.125 -> Flt64.zero
                        else -> null
                    }
                }
                assertEquals(expected, function.evaluate(mapOf(variable to Flt64(input))), "$sign $input")
            }
            assertEquals(null, function.evaluate(mapOf(variable to Flt64.nan)))
            assertEquals(null, function.evaluate(mapOf(variable to Flt64(Double.POSITIVE_INFINITY))))
            assertEquals(null, function.evaluate(emptyMap()))
        }
    }

    @Test
    fun allRelationsRejectInvalidInputsAndOverflowBeforeWriting() {
        for (sign in Comparison.entries) {
            for (invalidCase in listOf("zeroM", "negativeM", "nanM", "rhs", "coefficient", "overflow")) {
                val variable = RealVar("invalid_comparison_input")
                val coefficient = when (invalidCase) {
                    "coefficient" -> Flt64.nan
                    "overflow" -> Flt64(1e308)
                    else -> Flt64.one
                }
                val function = InequalityFunction(
                    lhs = LinearPolynomial(
                        listOf(LinearMonomial(coefficient, variable), LinearMonomial(coefficient, variable)),
                        Flt64.zero
                    ),
                    rhs = if (invalidCase == "rhs") Flt64.nan else Flt64.zero,
                    sign = sign,
                    converter = IntoValue.Identity,
                    bigM = when (invalidCase) {
                        "zeroM" -> Flt64.zero
                        "negativeM" -> Flt64(-1.0)
                        "nanM" -> Flt64.nan
                        else -> Flt64(10.0)
                    },
                    name = "invalid_comparison"
                )
                val model = CapturingLinearMechanismModel()
                try {
                    ManualTokenTable<Flt64>(Linear, false).use { tokens ->
                        assertTrue(function.registerAuxiliaryTokens(tokens) is Failed, "$sign $invalidCase")
                        assertTrue(tokens.tokens.isEmpty())
                    }
                    assertTrue(function.registerConstraints(model) is Failed, "$sign $invalidCase")
                    assertTrue(model.relations.isEmpty())
                } finally {
                    model.close()
                }
            }
        }
    }

    @Test
    fun nonStrictZeroNegativeAndNonFiniteToleranceAreRejected() {
        for (sign in listOf(Comparison.LE, Comparison.GE)) {
            for (tolerance in listOf(Flt64.zero, Flt64(-0.1), Flt64.nan, Flt64(Double.POSITIVE_INFINITY))) {
                val function = InequalityFunction(
                    lhs = LinearPolynomial(emptyList(), Flt64.zero),
                    rhs = Flt64.zero,
                    sign = sign,
                    converter = IntoValue.Identity,
                    bigM = Flt64.one,
                    tolerance = tolerance,
                    name = "invalid_tolerance"
                )
                val model = CapturingLinearMechanismModel()
                try {
                    ManualTokenTable<Flt64>(Linear, false).use { tokens ->
                        assertTrue(function.registerAuxiliaryTokens(tokens) is Failed)
                        assertTrue(tokens.tokens.isEmpty())
                    }
                    assertTrue(function.registerConstraints(model) is Failed)
                    assertTrue(model.relations.isEmpty())
                    assertEquals(null, function.evaluate(emptyMap()))
                } finally {
                    model.close()
                }
            }
        }
    }

    private class CapturingLinearMechanismModel : AbstractLinearMechanismModel<Flt64> {
        override val name: String = "inequality-test"
        override val tokens: AbstractTokenTable<Flt64> = AutoTokenTable(Linear, false)
        override val identityRegistry: ModelElementIdentityRegistry? = null
        override val objectFunction: Object = SingleObject(
            category = ObjectCategory.Minimum,
            subObjects = emptyList<LinearSubObject<Flt64>>()
        )
        override val constraints: List<Constraint<Flt64, *>> get() = emptyList()
        val relations = mutableListOf<LinearInequality<Flt64>>()

        override fun addConstraint(
            relation: LinearInequality<Flt64>,
            name: String?,
            from: Pair<IntermediateSymbol<out Flt64>, Boolean>?
        ): Try {
            relations += relation
            return ok
        }
    }
}
