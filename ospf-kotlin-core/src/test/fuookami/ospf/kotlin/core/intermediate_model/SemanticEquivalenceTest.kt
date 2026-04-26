package fuookami.ospf.kotlin.core.intermediate_model

import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.le
import fuookami.ospf.kotlin.math.symbol.inequality.Flt64LinearInequality as MathLinearInequality
import fuookami.ospf.kotlin.math.symbol.inequality.QuadraticInequality as MathQuadraticInequality
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial as MathLinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial as MathQuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial as MathLinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial as MathQuadraticPolynomial
import fuookami.ospf.kotlin.utils.functional.Ok
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SemanticEquivalenceTest {
    @Test
    fun constraintRelationRoundTripIsBijective() {
        assertEquals(ConstraintRelation.LessEqual, ConstraintRelation(Comparison.LE))
        assertEquals(ConstraintRelation.Equal, ConstraintRelation(Comparison.EQ))
        assertEquals(ConstraintRelation.GreaterEqual, ConstraintRelation(Comparison.GE))
        assertEquals(Comparison.LE, ConstraintRelation.LessEqual.toComparison())
        assertEquals(Comparison.EQ, ConstraintRelation.Equal.toComparison())
        assertEquals(Comparison.GE, ConstraintRelation.GreaterEqual.toComparison())
    }

    @Test
    fun linearMetaModelInvokeAndF64ConversionShouldPreserveConstraintCount() = runBlocking {
        val x = RealVar("x")
        val y = RealVar("y")

        val metaModel = LinearMetaModel<Flt64>(name = "semantic-linear")
        metaModel.add(listOf(x, y))

        val lhs = MathLinearPolynomial(
            monomials = listOf(MathLinearMonomial(Flt64(2.0), x), MathLinearMonomial(Flt64(3.0), y)),
            constant = Flt64.zero
        )
        val inequality: MathLinearInequality = lhs le Flt64(10.0)
        metaModel.addConstraint(relation = inequality, name = "c1")
        metaModel.minimize(MathLinearPolynomial(listOf(MathLinearMonomial(Flt64.one, x)), Flt64.zero))

        val mechResult = LinearMechanismModel.invoke(metaModel, concurrent = false)
        assertTrue(mechResult is Ok)

        val mechanismModel = mechResult.value
        assertEquals(1, mechanismModel.constraints.size)
        assertEquals(ObjectCategory.Minimum, mechanismModel.objectFunction.category)

        val f64Result = convertMechanismModelToF64(mechanismModel)
        assertTrue(f64Result is Ok)
        assertEquals(mechanismModel.constraints.size, f64Result.value.constraints.size)

        metaModel.close()
    }

    @Test
    fun quadraticMetaModelShouldCarryLinearAndQuadraticConstraintsThroughInvoke() = runBlocking {
        val x = RealVar("x")
        val y = RealVar("y")

        val metaModel = QuadraticMetaModel<Flt64>(name = "semantic-quadratic")
        metaModel.add(listOf(x, y))

        val quadLhs = MathQuadraticPolynomial(
            monomials = listOf(MathQuadraticMonomial.quadratic(Flt64.one, x, y)),
            constant = Flt64.zero
        )
        val quadInequality: MathQuadraticInequality = quadLhs le Flt64(5.0)
        metaModel.addConstraint(relation = quadInequality, name = "qc")

        val linLhs = MathLinearPolynomial(
            monomials = listOf(MathLinearMonomial(Flt64.one, x)),
            constant = Flt64.zero
        )
        val linInequality: MathLinearInequality = linLhs le Flt64(3.0)
        metaModel.addConstraint(relation = linInequality, name = "lc")

        metaModel.minimize(
            MathQuadraticPolynomial(
                monomials = listOf(MathQuadraticMonomial.quadratic(Flt64.one, x, y)),
                constant = Flt64.zero
            )
        )

        val mechResult = QuadraticMechanismModel.invoke(metaModel, concurrent = false)
        assertTrue(mechResult is Ok)

        val mechanismModel = mechResult.value
        assertEquals(2, mechanismModel.constraints.size)
        assertEquals(ObjectCategory.Minimum, mechanismModel.objectFunction.category)

        val f64Result = convertMechanismModelToF64(mechanismModel)
        assertTrue(f64Result is Ok)
        assertEquals(mechanismModel.constraints.size, f64Result.value.constraints.size)

        metaModel.close()
    }
}
