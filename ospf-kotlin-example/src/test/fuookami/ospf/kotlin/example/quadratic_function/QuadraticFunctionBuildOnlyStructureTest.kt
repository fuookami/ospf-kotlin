package fuookami.ospf.kotlin.example.quadratic_function

import fuookami.ospf.kotlin.core.intermediate_symbol.function.ProductFunction
import fuookami.ospf.kotlin.core.intermediate_symbol.function.QuadraticLinearFunction
import fuookami.ospf.kotlin.core.intermediate_symbol.function.SemiFunction
import fuookami.ospf.kotlin.core.model.basic.ConstraintRelation
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.mechanism.QuadraticMechanismModel
import fuookami.ospf.kotlin.core.model.mechanism.QuadraticMetaModel
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.QuadraticInequalityOf
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.utils.functional.Ok
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class QuadraticFunctionBuildOnlyStructureTest {
    @Test
    fun semiFunctionShouldExposeBoundsAndNoHelperVariables() {
        val semi = SemiFunction(
            lb = Flt64.one,
            ub = Flt64(4.0),
            converter = IntoValue.Identity,
            name = "p12_semi"
        )
        assertEquals(Flt64.one, semi.lb, "SemiFunction lb should be 1")
        assertEquals(Flt64(4.0), semi.ub, "SemiFunction ub should be 4")
        assertTrue(semi.helperVariables.isEmpty(), "SemiFunction should have no helper variables")
        assertNull(semi.evaluate(emptyMap()), "SemiFunction evaluate with empty map should return null")
    }

    @Test
    fun productFunctionShouldBuildAndRegisterConstraints() {
        val x = RealVar("p12_prod_x")
        val y = RealVar("p12_prod_y")

        val left = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64.one, x)),
            constant = Flt64.zero
        )
        val right = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64.one, y)),
            constant = Flt64.zero
        )

        val product = ProductFunction(left, right, name = "p12_product")

        val model = QuadraticMetaModel(name = "p12-product-build")
        try {
            assertTrue(model.add(listOf(x, y)) is Ok)
            assertTrue(product.registerAuxiliaryTokens(model.tokens) is Ok)
            assertTrue(model.minimize(product.polynomial) is Ok)

            assertEquals(2, model.tokens.tokens.size, "MetaModel should have 2 tokens (x, y)")

            @Suppress("DEPRECATION")
            val mechanismRet = runBlocking {
                QuadraticMechanismModel.invoke<Flt64>(metaModel = model)
            }
            assertTrue(mechanismRet is Ok)
            val mechanismModel = requireNotNull(mechanismRet.value)

            assertEquals(2, mechanismModel.numVariables, "MechanismModel should have 2 variables")
            assertTrue(mechanismModel.objectFunction.subObjects.isNotEmpty(),
                "objective should have sub-objects")

            val before = mechanismModel.constraints.size
            assertTrue(product.registerConstraints(mechanismModel) is Ok)
            val appended = mechanismModel.constraints.subList(before, mechanismModel.constraints.size)

            assertEquals(1, appended.size, "ProductFunction should append 1 equality constraint")
            assertEquals(ConstraintRelation.Equal, appended.first().sign)
            assertTrue(appended.first().name.contains("p12_product"))
            assertTrue(appended.first().lhs.isNotEmpty(), "constraint lhs should have cells")
        } finally {
            model.close()
        }
    }

    @Test
    fun quadraticLinearFunctionWithQuadraticTermsShouldRegisterHelperAndConstraint() {
        val x = RealVar("p12_qlin_x")
        val y = RealVar("p12_qlin_y")

        val poly = QuadraticPolynomial(
            monomials = listOf(
                QuadraticMonomial.quadratic(Flt64.one, x, y),
                QuadraticMonomial.linear(Flt64(2.0), x)
            ),
            constant = Flt64(3.0)
        )

        val qlin = QuadraticLinearFunction(poly, name = "p12_qlin")

        val model = QuadraticMetaModel(name = "p12-qlin-build")
        try {
            assertTrue(model.add(listOf(x, y)) is Ok)
            assertTrue(qlin.registerAuxiliaryTokens(model.tokens) is Ok)

            // QuadraticLinearFunction with quadratic terms should add helper variable y
            assertEquals(3, model.tokens.tokens.size,
                "MetaModel should have 3 tokens (x, y, helper)")

            assertTrue(model.minimize(qlin.polynomial) is Ok)

            @Suppress("DEPRECATION")
            val mechanismRet = runBlocking {
                QuadraticMechanismModel.invoke<Flt64>(metaModel = model)
            }
            assertTrue(mechanismRet is Ok)
            val mechanismModel = requireNotNull(mechanismRet.value)

            val before = mechanismModel.constraints.size
            assertTrue(qlin.registerConstraints(mechanismModel) is Ok)
            val appended = mechanismModel.constraints.subList(before, mechanismModel.constraints.size)

            // Quadratic terms => 1 equality constraint y = polynomial
            assertEquals(1, appended.size,
                "QuadraticLinearFunction with quadratic terms should append 1 equality constraint")
            assertEquals(ConstraintRelation.Equal, appended.first().sign)
        } finally {
            model.close()
        }
    }

    @Test
    fun quadraticLinearFunctionWithOnlyLinearTermsShouldNotAddConstraint() {
        val x = RealVar("p12_qlin_linear_x")

        val poly = QuadraticPolynomial(
            monomials = listOf(QuadraticMonomial.linear(Flt64.one, x)),
            constant = Flt64.zero
        )

        val qlin = QuadraticLinearFunction(poly, name = "p12_qlin_linear")

        val model = QuadraticMetaModel(name = "p12-qlin-linear-build")
        try {
            assertTrue(model.add(listOf(x)) is Ok)
            assertTrue(qlin.registerAuxiliaryTokens(model.tokens) is Ok)

            // Purely linear polynomial => no helper variable added
            assertEquals(1, model.tokens.tokens.size,
                "MetaModel should have only 1 token (no helper for linear-only poly)")

            assertTrue(model.minimize(qlin.polynomial) is Ok)

            @Suppress("DEPRECATION")
            val mechanismRet = runBlocking {
                QuadraticMechanismModel.invoke<Flt64>(metaModel = model)
            }
            assertTrue(mechanismRet is Ok)
            val mechanismModel = requireNotNull(mechanismRet.value)

            val before = mechanismModel.constraints.size
            assertTrue(qlin.registerConstraints(mechanismModel) is Ok)
            val appended = mechanismModel.constraints.subList(before, mechanismModel.constraints.size)

            // Linear-only => no constraint appended
            assertEquals(0, appended.size,
                "QuadraticLinearFunction with only linear terms should not append constraints")
        } finally {
            model.close()
        }
    }

    @Test
    fun fullQuadraticModelingChainShouldBuildMechanismModel() {
        val x = RealVar("p12_full_x")
        val y = RealVar("p12_full_y")

        val model = QuadraticMetaModel(
            name = "p12-full-quadratic",
            objectCategory = ObjectCategory.Minimum
        )
        try {
            assertTrue(model.add(listOf(x, y)) is Ok)

            // Add quadratic constraint: x*y <= 10
            val lhs = QuadraticPolynomial(
                monomials = listOf(QuadraticMonomial.quadratic(Flt64.one, x, y)),
                constant = Flt64.zero
            )
            val rhs = QuadraticPolynomial(emptyList(), Flt64(10.0))
            val constraint = QuadraticInequalityOf(lhs, rhs, Comparison.LE, "p12_xy_le_10")
            assertTrue(model.addConstraint(constraint) is Ok, "quadratic constraint should be accepted")

            // Minimize x*y + x
            val objective = QuadraticPolynomial(
                monomials = listOf(
                    QuadraticMonomial.quadratic(Flt64.one, x, y),
                    QuadraticMonomial.linear(Flt64.one, x)
                ),
                constant = Flt64.zero
            )
            assertTrue(model.minimize(objective) is Ok)

            assertEquals(2, model.tokens.tokens.size, "MetaModel should have 2 tokens")
            assertTrue(model.constraints.isNotEmpty(),
                "MetaModel should have at least 1 constraint")

            @Suppress("DEPRECATION")
            val mechanismRet = runBlocking {
                QuadraticMechanismModel.invoke<Flt64>(metaModel = model)
            }
            assertTrue(mechanismRet is Ok, "QuadraticMechanismModel.invoke should succeed")
            val mechanismModel = requireNotNull(mechanismRet.value)

            assertEquals(2, mechanismModel.numVariables, "MechanismModel should have 2 variables")
            assertTrue(mechanismModel.constraints.isNotEmpty(),
                "MechanismModel should have at least 1 constraint")
            assertTrue(mechanismModel.objectFunction.subObjects.isNotEmpty(),
                "MechanismModel objective should have sub-objects")
        } finally {
            model.close()
        }
    }
}