package fuookami.ospf.kotlin.core.symbol.function

import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.test.flt64TestConverter
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.Quadratic
import fuookami.ospf.kotlin.utils.functional.Ok
import kotlin.test.*
import org.junit.jupiter.api.Test

class ProductFunctionTest {
    @Test
    fun registerConstraintsShouldAddConstraintToModel() {
        val x = RealVar("x")
        val y = RealVar("y")

        val left = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64.one, x)),
            constant = Flt64.zero
        )
        val right = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64.one, y)),
            constant = Flt64.zero
        )

        val product = ProductFunction(left, right, converter = flt64TestConverter, name = "x_times_y")

        val tokens = AutoTokenTable<Flt64>(Quadratic, false)
        tokens.add(listOf(x, y))

        val metaModel = QuadraticMetaModel<Flt64>(name = "test-product", converter = flt64TestConverter)
        val model = QuadraticMechanismModel<Flt64>(
            parent = metaModel,
            name = "test-mech",
            constraints = mutableListOf(),
            objectFunction = SingleObject(ObjectCategory.Minimum, emptyList<QuadraticSubObject<Flt64>>()),
            tokens = tokens
        )

        val result = product.registerConstraints(model)
        assertTrue(result is Ok)
        assertEquals(1, model.constraints.size)
        val constraint = model.constraints.first()
        assertTrue(constraint is QuadraticConstraintImpl)
        assertEquals(ConstraintRelation.Equal, constraint.sign)

        metaModel.close()
    }

    @Test
    fun toQuadraticPolynomialShouldExpand() {
        val x = RealVar("x")
        val y = RealVar("y")

        val left = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64(2.0), x)),
            constant = Flt64(3.0)
        )
        val right = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64(5.0), y)),
            constant = Flt64(7.0)
        )

        val product = ProductFunction(left, right, converter = flt64TestConverter, name = "test")
        val poly = product.toQuadraticPolynomial()

        // (2x + 3)(5y + 7) = 10xy + 14x + 15y + 21
        assertEquals(21.0, poly.constant.toDouble(), 1e-10)

        val linearTerms = poly.monomials.filter { it.symbol2 == null }
        assertEquals(2, linearTerms.size)

        val quadraticTerms = poly.monomials.filter { it.symbol2 != null }
        assertEquals(1, quadraticTerms.size)
        assertEquals(10.0, quadraticTerms.first().coefficient.toDouble(), 1e-10)
    }
}
