package fuookami.ospf.kotlin.core.intermediate_model

import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.le
import fuookami.ospf.kotlin.math.symbol.inequality.QuadraticInequalityOf
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.utils.functional.Ok
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality

private val flt64Converter = object : IntoValue<Flt64> {
        override fun intoValue(value: Flt64) = value
        override val zero get() = Flt64.zero
        override val one get() = Flt64.one
        override fun fromValue(value: Flt64) = value
    }

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

        val metaModel = LinearMetaModel<Flt64>(name = "semantic-linear", converter = flt64Converter)
        metaModel.add(listOf(x, y))

        val lhs = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64(2.0), x), LinearMonomial(Flt64(3.0), y)),
            constant = Flt64.zero
        )
        val inequality: LinearInequality<Flt64> = lhs le Flt64(10.0)
        metaModel.addConstraint(relation = inequality, name = "c1")
        metaModel.minimize(LinearPolynomial(listOf(LinearMonomial(Flt64.one, x)), Flt64.zero))

        @Suppress("DEPRECATION")
        val mechResult = LinearMechanismModel.invoke<Flt64>(metaModel, concurrent = false)
        assertTrue(mechResult is Ok)

        val mechanismModel = mechResult.value
        assertEquals(1, mechanismModel.constraints.size)
        assertEquals(ObjectCategory.Minimum, mechanismModel.objectFunction.category)

        val f64Result = convertMechanismModelToFlt64(mechanismModel)
        assertTrue(f64Result is Ok)
        assertEquals(mechanismModel.constraints.size, f64Result.value.constraints.size)

        metaModel.close()
    }

    @Test
    fun quadraticMetaModelShouldCarryLinearAndQuadraticConstraintsThroughInvoke() = runBlocking {
        val x = RealVar("x")
        val y = RealVar("y")

        val metaModel = QuadraticMetaModel<Flt64>(name = "semantic-quadratic", converter = flt64Converter)
        metaModel.add(listOf(x, y))

        val quadLhs = QuadraticPolynomial(
            monomials = listOf(QuadraticMonomial.quadratic(Flt64.one, x, y)),
            constant = Flt64.zero
        )
        val quadInequality: QuadraticInequalityOf<Flt64> = quadLhs le Flt64(5.0)
        metaModel.addConstraint(relation = quadInequality, name = "qc")

        val linLhs = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64.one, x)),
            constant = Flt64.zero
        )
        val linInequality: LinearInequality<Flt64> = linLhs le Flt64(3.0)
        metaModel.addConstraint(relation = linInequality, name = "lc")

        metaModel.minimize(
            QuadraticPolynomial(
                monomials = listOf(QuadraticMonomial.quadratic(Flt64.one, x, y)),
                constant = Flt64.zero
            )
        )

        @Suppress("DEPRECATION")
        val mechResult = QuadraticMechanismModel.invoke<Flt64>(metaModel, concurrent = false)
        assertTrue(mechResult is Ok)

        val mechanismModel = mechResult.value
        assertEquals(2, mechanismModel.constraints.size)
        assertEquals(ObjectCategory.Minimum, mechanismModel.objectFunction.category)

        val f64Result = convertMechanismModelToFlt64(mechanismModel)
        assertTrue(f64Result is Ok)
        assertEquals(mechanismModel.constraints.size, f64Result.value.constraints.size)

        metaModel.close()
    }
}
