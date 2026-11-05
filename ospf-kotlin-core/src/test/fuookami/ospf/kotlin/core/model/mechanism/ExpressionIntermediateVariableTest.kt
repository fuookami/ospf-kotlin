package fuookami.ospf.kotlin.core.model.mechanism

import kotlin.test.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.model.basic.ConstraintRelation
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.*
import fuookami.ospf.kotlin.core.variable.RealVar

/**
 * 验证表达式符号的“中间变量实现”配置。
 * Verify the intermediate-variable realization configuration of expression symbols.
 */
class ExpressionIntermediateVariableTest {

    private val converter = IntoValue.Identity

    private suspend fun dumpLinear(metaModel: LinearMetaModel<Flt64>): LinearMechanismModel<Flt64> {
        return when (val result = LinearMechanismModel.invoke(
            metaModel = metaModel,
            concurrent = false,
            blocking = true
        )) {
            is Ok -> result.value
            is Failed -> fail(result.error.message)
            is Fatal -> fail(result.errors.joinToString { it.message })
        }
    }

    private suspend fun dumpQuadratic(metaModel: QuadraticMetaModel<Flt64>): QuadraticMechanismModel<Flt64> {
        return when (val result = QuadraticMechanismModel.invoke(
            metaModel = metaModel,
            concurrent = false,
            blocking = true
        )) {
            is Ok -> result.value
            is Failed -> fail(result.error.message)
            is Fatal -> fail(result.errors.joinToString { it.message })
        }
    }

    private fun linearExpression(
        name: String,
        monomials: List<LinearMonomial<Flt64>>,
        useIntermediateVariable: Boolean = false
    ): LinearExpressionSymbol<Flt64> {
        val symbol = LinearExpressionSymbol<Flt64>(
            _utilsPolynomial = MutableLinearPolynomial(
                monomials = monomials,
                constant = Flt64.zero
            ),
            name = name
        )
        symbol.useIntermediateVariable = useIntermediateVariable
        return symbol
    }

    private fun quadraticExpression(
        name: String,
        monomials: List<QuadraticMonomial<Flt64>>,
        useIntermediateVariable: Boolean = false
    ): QuadraticExpressionSymbol<Flt64> {
        val symbol = QuadraticExpressionSymbol<Flt64>(
            _utilsPolynomial = MutableQuadraticPolynomial(
                monomials = monomials,
                constant = Flt64.zero
            ),
            name = name
        )
        symbol.useIntermediateVariable = useIntermediateVariable
        return symbol
    }

    @Test
    fun linearSymbolInlinedByDefault() = runBlocking {
        val x = RealVar("realize_default_x")
        val y = RealVar("realize_default_y")
        val metaModel = LinearMetaModel<Flt64>(name = "realize-default", converter = converter)
        val expression = linearExpression(
            name = "realize_default_e",
            monomials = listOf(
                LinearMonomial(Flt64(2.0), x),
                LinearMonomial(Flt64(3.0), y)
            )
        )

        assertTrue(metaModel.add(x) is Ok)
        assertTrue(metaModel.add(y) is Ok)
        assertTrue(metaModel.add(expression) is Ok)
        assertTrue(metaModel.addConstraint(constraint = expression) is Ok)

        val model = dumpLinear(metaModel)
        try {
            assertNull(expression.intermediateVariable)
            assertEquals(1, model.linearConstraints.size)
            val cells = model.linearConstraints.single().lhs
            assertEquals(2, cells.size)
            assertTrue(cells.any { it.token.variable === x && it.coefficient eq Flt64(2.0) })
            assertTrue(cells.any { it.token.variable === y && it.coefficient eq Flt64(3.0) })
        } finally {
            model.close()
        }
    }

    @Test
    fun linearSymbolRealizedAsIntermediateVariable() = runBlocking {
        val x = RealVar("realize_linear_x")
        val y = RealVar("realize_linear_y")
        val metaModel = LinearMetaModel<Flt64>(name = "realize-linear", converter = converter)
        val expression = linearExpression(
            name = "realize_linear_e",
            monomials = listOf(
                LinearMonomial(Flt64(2.0), x),
                LinearMonomial(Flt64(3.0), y)
            ),
            useIntermediateVariable = true
        )

        assertTrue(metaModel.add(x) is Ok)
        assertTrue(metaModel.add(y) is Ok)
        assertTrue(metaModel.add(expression) is Ok)
        assertTrue(metaModel.addConstraint(constraint = expression) is Ok)

        val model = dumpLinear(metaModel)
        try {
            val realization = expression.intermediateVariable
            assertNotNull(realization)
            assertNotNull(model.tokens.find(realization))
            assertNotNull(metaModel.tokens.find(realization), "辅助变量应回写到元模型令牌表 / auxiliary variable should be synchronized")

            assertEquals(2, model.linearConstraints.size)

            val original = model.linearConstraints[0]
            assertEquals(ConstraintRelation.Equal, original.sign)
            assertTrue(original.rhs eq Flt64.one)
            assertEquals(1, original.lhs.size)
            assertSame(realization, original.lhs.single().token.variable)
            assertTrue(original.lhs.single().coefficient eq Flt64.one)

            val definition = model.linearConstraints[1]
            assertEquals(ConstraintRelation.Equal, definition.sign)
            assertTrue(definition.rhs eq Flt64.zero)
            assertEquals(3, definition.lhs.size)
            assertTrue(definition.lhs.any { it.token.variable === realization && it.coefficient eq Flt64.one })
            assertTrue(definition.lhs.any { it.token.variable === x && it.coefficient eq Flt64(-2.0) })
            assertTrue(definition.lhs.any { it.token.variable === y && it.coefficient eq Flt64(-3.0) })
        } finally {
            model.close()
        }
    }

    @Test
    fun linearObjectiveRealizedAsIntermediateVariable() = runBlocking {
        val x = RealVar("realize_objective_x")
        val y = RealVar("realize_objective_y")
        val metaModel = LinearMetaModel<Flt64>(name = "realize-objective", converter = converter)
        val expression = linearExpression(
            name = "realize_objective_e",
            monomials = listOf(
                LinearMonomial(Flt64(2.0), x),
                LinearMonomial(Flt64(3.0), y)
            ),
            useIntermediateVariable = true
        )

        assertTrue(metaModel.add(x) is Ok)
        assertTrue(metaModel.add(y) is Ok)
        assertTrue(metaModel.add(expression) is Ok)
        assertTrue(metaModel.minimize(expression) is Ok)

        val model = dumpLinear(metaModel)
        try {
            val realization = expression.intermediateVariable
            assertNotNull(realization)
            val cells = model.objectFunction.subObjects.flatMap { it.cells }
            assertEquals(1, cells.size)
            assertSame(realization, cells.single().token.variable)
            assertTrue(cells.single().coefficient eq Flt64.one)

            // 目标中不再内联定义，只剩一条定义约束。
            // The objective no longer inlines the definition; only the definition constraint remains.
            assertEquals(1, model.linearConstraints.size)
            assertEquals(3, model.linearConstraints.single().lhs.size)
        } finally {
            model.close()
        }
    }

    @Test
    fun nestedRealizedSymbolsReferenceInnerIntermediateVariable() = runBlocking {
        val x = RealVar("realize_nested_x")
        val y = RealVar("realize_nested_y")
        val metaModel = LinearMetaModel<Flt64>(name = "realize-nested", converter = converter)
        val inner = linearExpression(
            name = "realize_nested_inner",
            monomials = listOf(LinearMonomial(Flt64.one, x)),
            useIntermediateVariable = true
        )
        val outer = linearExpression(
            name = "realize_nested_outer",
            monomials = listOf(
                LinearMonomial(Flt64(2.0), inner),
                LinearMonomial(Flt64.one, y)
            ),
            useIntermediateVariable = true
        )

        assertTrue(metaModel.add(x) is Ok)
        assertTrue(metaModel.add(y) is Ok)
        assertTrue(metaModel.add(inner) is Ok)
        assertTrue(metaModel.add(outer) is Ok)
        assertTrue(metaModel.addConstraint(constraint = outer) is Ok)

        val model = dumpLinear(metaModel)
        try {
            val innerVariable = inner.intermediateVariable
            val outerVariable = outer.intermediateVariable
            assertNotNull(innerVariable)
            assertNotNull(outerVariable)

            // 原始约束 + 两个符号各一条定义约束。
            // The original constraint plus one definition constraint per symbol.
            assertEquals(3, model.linearConstraints.size)

            val outerDefinition = model.linearConstraints.single { constraint ->
                constraint.lhs.any { it.token.variable === innerVariable && it.coefficient eq Flt64(-2.0) }
            }
            assertTrue(outerDefinition.rhs eq Flt64.zero)
            assertTrue(outerDefinition.lhs.any { it.token.variable === outerVariable && it.coefficient eq Flt64.one })
            assertTrue(outerDefinition.lhs.any { it.token.variable === y && it.coefficient eq Flt64(-1.0) })
        } finally {
            model.close()
        }
    }

    @Test
    fun quadraticSymbolRealizedAsIntermediateVariable() = runBlocking {
        val x = RealVar("realize_quadratic_x")
        val y = RealVar("realize_quadratic_y")
        val metaModel = QuadraticMetaModel<Flt64>(name = "realize-quadratic", converter = converter)
        val expression = quadraticExpression(
            name = "realize_quadratic_e",
            monomials = listOf(QuadraticMonomial(Flt64.one, x, y)),
            useIntermediateVariable = true
        )

        assertTrue(metaModel.add(x) is Ok)
        assertTrue(metaModel.add(y) is Ok)
        assertTrue(metaModel.add(expression) is Ok)
        assertTrue(metaModel.addConstraint(constraint = expression) is Ok)

        val model = dumpQuadratic(metaModel)
        try {
            val realization = expression.intermediateVariable
            assertNotNull(realization)
            assertNotNull(model.tokens.find(realization))
            assertEquals(2, model.quadraticConstraints.size)

            val original = model.quadraticConstraints[0]
            assertEquals(ConstraintRelation.Equal, original.sign)
            assertEquals(1, original.lhs.size)

            val definition = model.quadraticConstraints[1]
            assertEquals(ConstraintRelation.Equal, definition.sign)
            assertTrue(definition.rhs eq Flt64.zero)
            assertEquals(2, definition.lhs.size)
            assertTrue(definition.lhs.any {
                it.token1.variable === realization && it.token2 == null && it.coefficient eq Flt64.one
            })
            assertTrue(definition.lhs.any {
                it.coefficient eq Flt64(-1.0) &&
                    ((it.token1.variable === x && it.token2?.variable === y) ||
                        (it.token1.variable === y && it.token2?.variable === x))
            })
        } finally {
            model.close()
        }
    }

    @Test
    fun quadraticObjectiveRealizedAsIntermediateVariable() = runBlocking {
        val x = RealVar("realize_quadratic_objective_x")
        val y = RealVar("realize_quadratic_objective_y")
        val metaModel = QuadraticMetaModel<Flt64>(name = "realize-quadratic-objective", converter = converter)
        val expression = quadraticExpression(
            name = "realize_quadratic_objective_e",
            monomials = listOf(QuadraticMonomial(Flt64(2.0), x, y)),
            useIntermediateVariable = true
        )

        assertTrue(metaModel.add(x) is Ok)
        assertTrue(metaModel.add(y) is Ok)
        assertTrue(metaModel.add(expression) is Ok)
        assertTrue(metaModel.minimize(expression) is Ok)

        val model = dumpQuadratic(metaModel)
        try {
            val realization = expression.intermediateVariable
            assertNotNull(realization)
            val cells = model.objectFunction.subObjects.flatMap { it.cells }
            assertEquals(1, cells.size)
            assertSame(realization, cells.single().token1.variable)
            assertNull(cells.single().token2)
            assertTrue(cells.single().coefficient eq Flt64.one)
        } finally {
            model.close()
        }
    }
}
