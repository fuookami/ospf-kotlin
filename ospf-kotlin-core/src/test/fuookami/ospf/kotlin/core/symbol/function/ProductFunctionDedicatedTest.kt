package fuookami.ospf.kotlin.core.symbol.function

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.Test
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.Quadratic
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.mechanism.SingleObject
import fuookami.ospf.kotlin.core.model.mechanism.QuadraticMetaModel
import fuookami.ospf.kotlin.core.model.mechanism.QuadraticSubObject
import fuookami.ospf.kotlin.core.model.mechanism.QuadraticMechanismModel
import fuookami.ospf.kotlin.core.token.AutoTokenTable
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.RealVar

/** [ProductFunction] 契约测试。 / Dedicated contract tests. */
class ProductFunctionDedicatedTest {
    @Test
    fun symbolImplementsMathFunctionContract() {
        assertTrue(ProductFunction::class.simpleName == "ProductFunction")
    }

    @Test
    fun productIsExpressionOnlyWithNoHelpersOrStandaloneRows() {
        val x = RealVar("product_dedicated_x")
        val y = RealVar("product_dedicated_y")
        val product = ProductFunction(
            left = LinearPolynomial(
                listOf(LinearMonomial(Flt64.one, x)),
                Flt64.one
            ),
            right = LinearPolynomial(
                listOf(LinearMonomial(Flt64.one, y)),
                -Flt64.one
            ),
            converter = IntoValue.Identity,
            name = "product_dedicated"
        )
        val tokens = AutoTokenTable<Flt64>(Quadratic, false)
        val metaModel = QuadraticMetaModel<Flt64>(
            name = "product-dedicated",
            converter = IntoValue.Identity
        )
        val model = QuadraticMechanismModel<Flt64>(
            parent = metaModel,
            name = "product-dedicated-mechanism",
            constraints = mutableListOf(),
            objectFunction = SingleObject(
                ObjectCategory.Minimum,
                emptyList<QuadraticSubObject<Flt64>>()
            ),
            tokens = tokens
        )
        try {
            assertTrue(tokens.add(listOf(x, y)) is Ok)
            assertTrue(product.registerAuxiliaryTokens(tokens) is Ok)
            assertEquals(2, tokens.tokens.size)
            val beforeRows = model.constraints.size
            assertTrue(product.registerConstraints(model) is Ok)
            assertEquals(beforeRows, model.constraints.size)
            assertTrue(product.polynomial.monomials.any { it.symbol2 != null })
        } finally {
            model.close()
            metaModel.close()
        }
    }
}
