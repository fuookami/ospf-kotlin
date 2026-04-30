package fuookami.ospf.kotlin.example.linear_function

import fuookami.ospf.kotlin.core.intermediate_symbol.function.SemiFunction
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModelFlt64
import fuookami.ospf.kotlin.core.solver.scip.ScipLinearSolver
import fuookami.ospf.kotlin.core.variable.URealVar
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.minus
import fuookami.ospf.kotlin.math.symbol.inequality.eq
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assumptions.assumeTrue

class SemiTest {
    @Test
    fun semi() {
        assumeTrue(isScipAvailable(), "SCIP runtime not available in current environment")

        val x = URealVar("x")
        x.range.leq(Flt64.three)
        val y = URealVar("y")
        y.range.geq(Flt64.two)
        y.range.leq(Flt64.five)

        val px = LinearPolynomial(listOf(LinearMonomial(Flt64.one, x)), Flt64.zero)
        val py = LinearPolynomial(listOf(LinearMonomial(Flt64.one, y)), Flt64.zero)
        val semi = SemiFunction(px - py, name = "semi")
        val solver = ScipLinearSolver()

        val model1 = LinearMetaModelFlt64()
        model1.add(x)
        model1.add(y)
        semi.register(model1)
        model1.minimize(semi.y)
        val result1 = runBlocking { solver(model1) }
        assert(result1.value!!.obj eq Flt64.zero)
        assert(result1.value!!.solution[1] geq result1.value!!.solution[0])

        val model2 = LinearMetaModelFlt64()
        model2.add(x)
        model2.add(y)
        semi.register(model2)
        model2.maximize(semi.y)
        val result2 = runBlocking { solver(model2) }
        assert(result2.value!!.obj eq Flt64.one)
    }

    private fun isScipAvailable(): Boolean {
        return runCatching { Class.forName("jscip.Scip") }.isSuccess
    }
}
