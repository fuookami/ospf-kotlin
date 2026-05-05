package fuookami.ospf.kotlin.example.linear_function

import fuookami.ospf.kotlin.core.intermediate_symbol.function.UnivariateLinearPiecewiseFunction
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.solver.scip.ScipLinearSolver
import fuookami.ospf.kotlin.core.variable.URealVar
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.geometry.point2
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.inequality.eq
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assumptions.assumeTrue
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel

private val flt64Converter = object : IntoValue<Flt64> {
        override fun intoValue(value: Flt64) = value
        override val zero get() = Flt64.zero
        override val one get() = Flt64.one
        override fun fromValue(value: Flt64) = value
    }

class ULPTest {
    @Test
    fun univariate() {
        assumeTrue(isScipAvailable(), "SCIP runtime not available in current environment")

        val x = URealVar("x")
        x.range.leq(Flt64.two)
        val px = LinearPolynomial(listOf(LinearMonomial(Flt64.one, x)), Flt64.zero)
        val ulp = UnivariateLinearPiecewiseFunction(
            x = px,
            points = listOf(
                point2(),
                point2(x = Flt64.one, y = Flt64.two),
                point2(x = Flt64.two, y = Flt64.one)
            ),
            name = "y"
        )

        val model = LinearMetaModel<Flt64>(converter = flt64Converter)
        model.add(x)
        ulp.register(model)
        model.maximize(ulp.result)
        val solver = ScipLinearSolver()
        val result = runBlocking { solver(model) }
        assert(result.value!!.obj eq Flt64.two)
        assert(result.value!!.solution[0] eq Flt64.one)
    }

    private fun isScipAvailable(): Boolean {
        return runCatching { Class.forName("jscip.Scip") }.isSuccess
    }
}
