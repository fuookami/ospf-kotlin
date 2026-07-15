package fuookami.ospf.kotlin.core.intermediate_model

import kotlin.test.*
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.test.flt64TestConverter
import fuookami.ospf.kotlin.core.token.AutoTokenTable
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.math.symbol.Quadratic
import fuookami.ospf.kotlin.math.symbol.Quadratic as MechanismQuadratic
import fuookami.ospf.kotlin.utils.functional.Ok

class QuadraticMechanismModelCutTest {
    @Test
    fun optimalCutShouldReturnQuadraticCutWhenProjectedTermsContainQuadraticPart() {
        val x = RealVar("x")
        val y = RealVar("y")
        val theta = RealVar("theta")

        val tokens = AutoTokenTable<Flt64>(Quadratic, false)
        assertTrue(tokens.add(listOf(x, y)) is Ok<*, *, *>)

        val relation = QuadraticInequalityOf(
            lhs = QuadraticPolynomial(
                monomials = listOf(
                    QuadraticMonomial.quadratic(Flt64.one, x, y),
                    QuadraticMonomial.linear(Flt64.one, x)
                ),
                constant = Flt64.zero
            ),
            rhs = QuadraticPolynomial(emptyList(), Flt64(6.0)),
            comparison = Comparison.LE
        )
        val constraint = QuadraticConstraintImpl<Flt64>(
            relation = QuadraticRelationImpl(relation.flattenData, relation.comparison),
            tokens = tokens,
            converter = IntoValue.Identity,
            name = "qc-optimal"
        ).valueOrFail()
        val mechanismModel = QuadraticMechanismModel<Flt64>(
            parent = QuadraticMetaModel(name = "cut-parent-optimal", converter = flt64TestConverter),
            name = "cut-model-optimal",
            constraints = listOf(constraint),
            objectFunction = SingleObject(ObjectCategory.Minimum, emptyList()),
            tokens = tokens
        )

        val result = mechanismModel.generateFlt64OptimalCut(
            objectVariable = theta,
            fixedVariables = mapOf(x to Flt64.one, y to Flt64(2.0)),
            dualSolution = mapOf(constraint as Constraint<Flt64, MechanismQuadratic> to Flt64(2.0))
        )

        assertTrue(result is Ok)
        assertEquals(1, result.value.size)
        assertEquals(QuadraticInequalityOf::class.qualifiedName, result.value.first()::class.qualifiedName)

        mechanismModel.close()
    }

    @Test
    fun optimalCutShouldFallbackToLinearCutWhenNoQuadraticProjectedTermExists() {
        val x = RealVar("x")
        val theta = RealVar("theta")

        val tokens = AutoTokenTable<Flt64>(Quadratic, false)
        assertTrue(tokens.add(x) is Ok<*, *, *>)

        val relation = QuadraticInequalityOf(
            lhs = QuadraticPolynomial(
                monomials = listOf(QuadraticMonomial.linear(Flt64.one, x)),
                constant = Flt64.zero
            ),
            rhs = QuadraticPolynomial(emptyList(), Flt64(3.0)),
            comparison = Comparison.LE
        )
        val constraint = QuadraticConstraintImpl<Flt64>(
            relation = QuadraticRelationImpl(relation.flattenData, relation.comparison),
            tokens = tokens,
            converter = IntoValue.Identity,
            name = "qc-linear-fallback"
        ).valueOrFail()
        val mechanismModel = QuadraticMechanismModel<Flt64>(
            parent = QuadraticMetaModel(name = "cut-parent-linear", converter = flt64TestConverter),
            name = "cut-model-linear",
            constraints = listOf(constraint),
            objectFunction = SingleObject(ObjectCategory.Minimum, emptyList()),
            tokens = tokens
        )

        val result = mechanismModel.generateFlt64OptimalCut(
            objectVariable = theta,
            fixedVariables = mapOf(x to Flt64(2.0)),
            dualSolution = mapOf(constraint as Constraint<Flt64, MechanismQuadratic> to Flt64.one)
        )

        assertTrue(result is Ok)
        assertEquals(1, result.value.size)
        assertEquals(LinearInequality::class.qualifiedName, result.value.first()::class.qualifiedName)

        mechanismModel.close()
    }
}
