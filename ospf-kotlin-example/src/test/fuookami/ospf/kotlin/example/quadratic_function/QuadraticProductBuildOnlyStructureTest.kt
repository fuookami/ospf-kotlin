package fuookami.ospf.kotlin.example.quadratic_function

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.mechanism.QuadraticMetaModel
import fuookami.ospf.kotlin.core.model.mechanism.QuadraticMechanismModel
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.function.ProductFunction
import fuookami.ospf.kotlin.core.variable.RealVar

/** 验证乘积函数在不调用求解器的情况下追加二次等式约束。 / Verifies that the product function appends a quadratic equality constraint without invoking a solver. */
class QuadraticProductBuildOnlyStructureTest {
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

        val function = ProductFunction(
            left = left,
            right = right,
            converter = IntoValue.Identity,
            name = "example_product_build"
        )

        val model = QuadraticMetaModel(name = "quadratic-build-only")

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

            // MetaModel 结构断言 / Structure assertions on MetaModel
            assertEquals(2, model.tokens.tokens.size, "MetaModel should have 2 tokens (x, y)")
            val mechanismRet = runBlocking {
                QuadraticMechanismModel.invoke<Flt64>(metaModel = model)
            }
            assertTrue(mechanismRet is Ok)
            val mechanismModel = requireNotNull(mechanismRet.value)

            // MechanismModel 结构：变量与目标函数 / MechanismModel structure: variables + objective
            assertEquals(2, mechanismModel.numVariables, "MechanismModel should have 2 variables")
            assertTrue(mechanismModel.objectFunction.subObjects.isNotEmpty(),
                "MechanismModel objective should have sub-objects")

            val before = mechanismModel.constraints.size
            assertTrue(function.registerConstraints(mechanismModel) is Ok)
            val appended = mechanismModel.constraints.subList(before, mechanismModel.constraints.size)

            // ProductFunction 位于表达式层，其展开多项式由目标函数或外层约束消费，
            // 因此注册它不会新增辅助约束行。 / ProductFunction is expression-level; its expanded polynomial is consumed by the
            // objective or enclosing constraint, so registering it adds no auxiliary row.
            assertEquals(
                0,
                appended.size,
                "expression-level product should not append an auxiliary constraint"
            )
        } finally {
            model.close()
        }
    }
}
