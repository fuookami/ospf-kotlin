package fuookami.ospf.kotlin.core.intermediate_model

import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Quadratic
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.Flt64LinearInequality as MathLinearInequality
import fuookami.ospf.kotlin.math.symbol.inequality.QuadraticInequality as MathQuadraticInequality
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial as MathQuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial as MathQuadraticPolynomial
import fuookami.ospf.kotlin.utils.functional.Ok
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QuadraticMechanismModelCutTest {
    @Test
    fun optimalCutShouldReturnQuadraticInequalityWhenProjectedTermsContainQuadraticPart() {
        val x = RealVar("x")
        val y = RealVar("y")
        val theta = RealVar("theta")

        val tokens = AutoTokenTable(Quadratic, false)
        assertTrue(tokens.add(listOf(x, y)) is Ok)

        val relation = MathQuadraticInequality(
            lhs = MathQuadraticPolynomial(
                monomials = listOf(
                    MathQuadraticMonomial.quadratic(Flt64.one, x, y),
                    MathQuadraticMonomial.linear(Flt64.one, x)
                ),
                constant = Flt64.zero
            ),
            rhs = MathQuadraticPolynomial(emptyList(), Flt64(6.0)),
            comparison = Comparison.LE
        )
        val constraint = QuadraticConstraint(
            relation = relation,
            tokens = tokens,
            name = "qc-optimal"
        )
        val mechanismModel = QuadraticMechanismModel(
            parent = QuadraticMetaModel(name = "cut-parent-optimal"),
            name = "cut-model-optimal",
            constraints = listOf(constraint),
            objectFunction = SingleObject(ObjectCategory.Minimum, emptyList<QuadraticSubObject>()),
            tokens = tokens
        )

        val result = mechanismModel.generateOptimalCut(
            objective = Flt64.zero,
            objectVariable = theta,
            fixedVariables = mapOf(x to Flt64.one, y to Flt64(2.0)),
            dualSolution = mapOf(constraint to Flt64(2.0))
        )

        assertTrue(result is Ok)
        assertEquals(1, result.value.size)
        val cut = result.value.single() as MathQuadraticInequality

        val flattenData = cut.flattenData
        assertTrue(findLinearCoefficient(flattenData, theta) eq Flt64.one)
        assertTrue(findLinearCoefficient(flattenData, x) eq Flt64(2.0))
        assertTrue(findQuadraticCoefficient(flattenData, x, y) eq Flt64(2.0))
        assertTrue(flattenData.constant eq Flt64(-12.0))

        mechanismModel.close()
    }

    @Test
    fun feasibleCutShouldFlipSignWhenProjectedValueIsNegative() {
        val x = RealVar("x")
        val y = RealVar("y")

        val tokens = AutoTokenTable(Quadratic, false)
        assertTrue(tokens.add(listOf(x, y)) is Ok)

        val relation = MathQuadraticInequality(
            lhs = MathQuadraticPolynomial(
                monomials = listOf(MathQuadraticMonomial.quadratic(Flt64.one, x, y)),
                constant = Flt64.zero
            ),
            rhs = MathQuadraticPolynomial(emptyList(), Flt64.one),
            comparison = Comparison.LE
        )
        val constraint = QuadraticConstraint(
            relation = relation,
            tokens = tokens,
            name = "qc-feasible"
        )
        val mechanismModel = QuadraticMechanismModel(
            parent = QuadraticMetaModel(name = "cut-parent-feasible"),
            name = "cut-model-feasible",
            constraints = listOf(constraint),
            objectFunction = SingleObject(ObjectCategory.Minimum, emptyList<QuadraticSubObject>()),
            tokens = tokens
        )

        val result = mechanismModel.generateFeasibleCut(
            fixedVariables = mapOf(x to Flt64(2.0), y to Flt64(3.0)),
            farkasDualSolution = mapOf(constraint to Flt64.one)
        )

        assertTrue(result is Ok)
        assertEquals(1, result.value.size)
        val cut = result.value.single() as MathQuadraticInequality

        val flattenData = cut.flattenData
        assertTrue(findQuadraticCoefficient(flattenData, x, y) eq Flt64.one)
        assertTrue(flattenData.constant eq Flt64(-1.0))

        mechanismModel.close()
    }

    @Test
    fun optimalCutShouldFallbackToLinearInequalityWhenNoQuadraticTermExists() {
        val x = RealVar("x")
        val theta = RealVar("theta")

        val tokens = AutoTokenTable(Quadratic, false)
        assertTrue(tokens.add(x) is Ok)

        val relation = MathQuadraticInequality(
            lhs = MathQuadraticPolynomial(
                monomials = listOf(MathQuadraticMonomial.linear(Flt64.one, x)),
                constant = Flt64.zero
            ),
            rhs = MathQuadraticPolynomial(emptyList(), Flt64(3.0)),
            comparison = Comparison.LE
        )
        val constraint = QuadraticConstraint(
            relation = relation,
            tokens = tokens,
            name = "qc-linear-fallback"
        )
        val mechanismModel = QuadraticMechanismModel(
            parent = QuadraticMetaModel(name = "cut-parent-linear-fallback"),
            name = "cut-model-linear-fallback",
            constraints = listOf(constraint),
            objectFunction = SingleObject(ObjectCategory.Maximum, emptyList<QuadraticSubObject>()),
            tokens = tokens
        )

        val result = mechanismModel.generateOptimalCut(
            objective = Flt64.zero,
            objectVariable = theta,
            fixedVariables = mapOf(x to Flt64(2.0)),
            dualSolution = mapOf(constraint to Flt64.one)
        )

        assertTrue(result is Ok)
        assertEquals(1, result.value.size)
        val cut = result.value.single() as MathLinearInequality

        val flattenData = cut.flattenData
        assertTrue(findLinearCoefficient(flattenData, theta) eq Flt64.one)
        assertTrue(findLinearCoefficient(flattenData, x) eq Flt64.one)
        assertTrue(flattenData.constant eq Flt64(-3.0))

        mechanismModel.close()
    }

    private fun findLinearCoefficient(
        flattenData: LinearFlattenData,
        variable: AbstractVariableItem<*, *>
    ): Flt64 {
        return flattenData
            .monomials
            .firstOrNull { it.symbol == variable }
            ?.coefficient
            ?: Flt64.zero
    }

    private fun findLinearCoefficient(
        flattenData: QuadraticFlattenData,
        variable: AbstractVariableItem<*, *>
    ): Flt64 {
        return flattenData
            .monomials
            .firstOrNull { it.symbol1 == variable && it.symbol2 == null }
            ?.coefficient
            ?: Flt64.zero
    }

    private fun findQuadraticCoefficient(
        flattenData: QuadraticFlattenData,
        variable1: AbstractVariableItem<*, *>,
        variable2: AbstractVariableItem<*, *>
    ): Flt64 {
        return flattenData
            .monomials
            .firstOrNull {
                (it.symbol1 == variable1 && it.symbol2 == variable2) ||
                    (it.symbol1 == variable2 && it.symbol2 == variable1)
            }
            ?.coefficient
            ?: Flt64.zero
    }
}
