package fuookami.ospf.kotlin.example.quadratic_function

import fuookami.ospf.kotlin.core.model.mechanism.QuadraticMechanismModel
import fuookami.ospf.kotlin.core.model.basic.ConstraintRelation
import fuookami.ospf.kotlin.core.model.mechanism.QuadraticMetaModel
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.utils.functional.Ok
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class QuadraticProductBuildOnlyStructureTest {
    private val flt64Converter = object : IntoValue<Flt64> {
        override fun intoValue(value: Flt64) = value
        override val zero get() = Flt64.zero
        override val one get() = Flt64.one
        override fun fromValue(value: Flt64) = value
    }

    @Test
    fun productFunctionShouldAppendQuadraticEqualityWithoutSolver() {
        val x = RealVar("example_quad_build_x")
        val y = RealVar("example_quad_build_y")

        val left = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64.one, x)),
            constant = Flt64.one
        )
        val right = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64.one, y)),
            constant = Flt64.zero
        )

        val function = fuookami.ospf.kotlin.core.intermediate_symbol.function.ProductFunction(
            left = left,
            right = right,
            converter = flt64Converter,
            name = "example_product_build"
        )

        val model = QuadraticMetaModel<Flt64>(
            name = "quadratic-build-only",
            converter = flt64Converter
        )

        try {
            assertTrue(model.add(listOf(x, y)) is Ok)
            assertTrue(function.registerAuxiliaryTokens(model.tokens) is Ok)
            assertTrue(
                model.minimize(
                    QuadraticPolynomial(
                        monomials = listOf(QuadraticMonomial.quadratic(Flt64.one, x, y)),
                        constant = Flt64.zero
                    )
                ) is Ok
            )

            @Suppress("DEPRECATION")
            val mechanismRet = runBlocking {
                QuadraticMechanismModel.invoke<Flt64>(metaModel = model)
            }
            assertTrue(mechanismRet is Ok)
            val mechanismModel = requireNotNull(mechanismRet.value)

            val before = mechanismModel.constraints.size
            assertTrue(function.registerConstraints(mechanismModel) is Ok)
            val appended = mechanismModel.constraints.subList(before, mechanismModel.constraints.size)

            assertEquals(1, appended.size, "product 应只追加 1 条等式约束")
            assertEquals(ConstraintRelation.Equal, appended.first().sign)
            assertTrue(appended.first().name.contains("example_product_build"))
        } finally {
            model.close()
        }
    }
}
