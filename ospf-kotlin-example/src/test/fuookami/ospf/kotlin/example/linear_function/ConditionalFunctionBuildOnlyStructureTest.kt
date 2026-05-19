package fuookami.ospf.kotlin.example.linear_function

import fuookami.ospf.kotlin.core.model.mechanism.LinearMechanismModel
import fuookami.ospf.kotlin.core.model.basic.ConstraintRelation
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.utils.functional.Ok
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ConditionalFunctionBuildOnlyStructureTest {
    private val flt64Converter = object : IntoValue<Flt64> {
        override fun intoValue(value: Flt64) = value
        override val zero get() = Flt64.zero
        override val one get() = Flt64.one
        override fun fromValue(value: Flt64) = value
    }

    @Test
    fun ifAndOneOfShouldAppendComparableConstraintsWithoutSolver() {
        val x = RealVar("example_cond_build_x")
        val y = RealVar("example_cond_build_y")
        val xPoly = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64.one, x)),
            constant = Flt64.zero
        )
        val yPoly = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64.one, y)),
            constant = Flt64.zero
        )

        val ifFunction = fuookami.ospf.kotlin.core.intermediate_symbol.function.IfFunction(
            condition = xPoly,
            converter = flt64Converter,
            name = "example_if_build"
        )
        val oneOfFunction = fuookami.ospf.kotlin.core.intermediate_symbol.function.OneOfFunction(
            polynomials = listOf(xPoly, yPoly, LinearPolynomial<Flt64>(emptyList(), Flt64.two)),
            converter = flt64Converter,
            name = "example_oneof_build"
        )

        val model = LinearMetaModel<Flt64>(
            name = "conditional-build-only",
            converter = flt64Converter
        )

        try {
            assertTrue(model.add(listOf(x, y)) is Ok)
            assertTrue(ifFunction.registerAuxiliaryTokens(model.tokens) is Ok)
            assertTrue(oneOfFunction.registerAuxiliaryTokens(model.tokens) is Ok)
            assertTrue(model.minimize(xPoly) is Ok)
            val mechanismRet = runBlocking {
                LinearMechanismModel.invoke<Flt64>(metaModel = model)
            }
            assertTrue(mechanismRet is Ok)
            val mechanismModel = requireNotNull(mechanismRet.value)

            val before = mechanismModel.constraints.size
            assertTrue(ifFunction.registerConstraints(mechanismModel) is Ok)
            assertTrue(oneOfFunction.registerConstraints(mechanismModel) is Ok)
            val appended = mechanismModel.constraints.subList(before, mechanismModel.constraints.size)

            val ifRows = appended.filter { it.name.startsWith("example_if_build") }
            val oneOfRows = appended.filter { it.name.startsWith("example_oneof_build") }
            assertTrue(ifRows.isNotEmpty())
            assertTrue(oneOfRows.isNotEmpty())

            assertTrue(ifRows.any { it.sign == ConstraintRelation.Equal || it.sign == ConstraintRelation.GreaterEqual || it.sign == ConstraintRelation.LessEqual })
            assertTrue(oneOfRows.any { it.sign == ConstraintRelation.Equal })
            assertEquals(Flt64.one, oneOfFunction.evaluate(mapOf(x to Flt64.zero, y to Flt64.zero)))
        } finally {
            model.close()
        }
    }
}