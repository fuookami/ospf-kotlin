package fuookami.ospf.kotlin.core.intermediate_model

import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Quadratic
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.QuadraticInequality as MathQuadraticInequality
import fuookami.ospf.kotlin.math.symbol.inequality.Flt64LinearInequality as MathLinearInequality
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial as MathQuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial as MathQuadraticPolynomial
import fuookami.ospf.kotlin.utils.functional.Ok
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QuadraticMechanismModelCutTest {
    @Test
    fun optimalCutShouldReturnQuadraticCutWhenProjectedTermsContainQuadraticPart() {
        val x = RealVar("x")
        val y = RealVar("y")
        val theta = RealVar("theta")

        val tokens = AutoTokenTable<Flt64>(Quadratic, false)
        assertTrue(tokens.add(listOf(x, y)) is Ok<*, *, *>)

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
        val constraint = QuadraticConstraintImpl(
            relation = QuadraticRelationImpl(relation.flattenData, relation.comparison),
            tokens = tokens,
            name = "qc-optimal"
        )
        val mechanismModel = QuadraticMechanismModel<Flt64>(
            parent = QuadraticMetaModel(name = "cut-parent-optimal"),
            name = "cut-model-optimal",
            constraints = listOf(constraint),
            objectFunction = SingleObject(ObjectCategory.Minimum, emptyList()),
            tokens = tokens
        )

        val result = mechanismModel.generateOptimalCut(
            objective = Flt64.zero,
            objectVariable = theta,
            fixedVariables = mapOf(x to Flt64.one, y to Flt64(2.0)),
            dualSolution = mapOf(constraint as QuadraticConstraint to Flt64(2.0))
        )

        assertTrue(result is Ok)
        assertEquals(1, result.value.size)
        assertEquals(MathQuadraticInequality::class.qualifiedName, result.value.first()::class.qualifiedName)

        mechanismModel.close()
    }

    @Test
    fun optimalCutShouldFallbackToLinearCutWhenNoQuadraticProjectedTermExists() {
        val x = RealVar("x")
        val theta = RealVar("theta")

        val tokens = AutoTokenTable<Flt64>(Quadratic, false)
        assertTrue(tokens.add(x) is Ok<*, *, *>)

        val relation = MathQuadraticInequality(
            lhs = MathQuadraticPolynomial(
                monomials = listOf(MathQuadraticMonomial.linear(Flt64.one, x)),
                constant = Flt64.zero
            ),
            rhs = MathQuadraticPolynomial(emptyList(), Flt64(3.0)),
            comparison = Comparison.LE
        )
        val constraint = QuadraticConstraintImpl(
            relation = QuadraticRelationImpl(relation.flattenData, relation.comparison),
            tokens = tokens,
            name = "qc-linear-fallback"
        )
        val mechanismModel = QuadraticMechanismModel<Flt64>(
            parent = QuadraticMetaModel(name = "cut-parent-linear"),
            name = "cut-model-linear",
            constraints = listOf(constraint),
            objectFunction = SingleObject(ObjectCategory.Minimum, emptyList()),
            tokens = tokens
        )

        val result = mechanismModel.generateOptimalCut(
            objective = Flt64.zero,
            objectVariable = theta,
            fixedVariables = mapOf(x to Flt64(2.0)),
            dualSolution = mapOf(constraint as QuadraticConstraint to Flt64.one)
        )

        assertTrue(result is Ok)
        assertEquals(1, result.value.size)
        assertEquals(MathLinearInequality::class.qualifiedName, result.value.first()::class.qualifiedName)

        mechanismModel.close()
    }
}
