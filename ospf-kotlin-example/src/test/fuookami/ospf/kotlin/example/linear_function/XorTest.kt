package fuookami.ospf.kotlin.example.linear_function

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.plus
import fuookami.ospf.kotlin.math.symbol.inequality.eq
import fuookami.ospf.kotlin.core.intermediate_symbol.function.XorFunction
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModelFlt64
import fuookami.ospf.kotlin.core.solver.scip.ScipLinearSolver
import fuookami.ospf.kotlin.core.variable.BinVar
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assumptions.assumeTrue

class XorTest {
    @Test
    fun xor2() {
        assumeTrue(isScipAvailable(), "SCIP runtime not available in current environment")

        val x = BinVar("x")
        val y = BinVar("y")
        val px = LinearPolynomial(listOf(LinearMonomial(Flt64.one, x)), Flt64.zero)
        val py = LinearPolynomial(listOf(LinearMonomial(Flt64.one, y)), Flt64.zero)
        val xor = XorFunction(listOf(px, py), name = "xor")
        val solver = ScipLinearSolver()

        val model1 = LinearMetaModelFlt64()
        model1.add(x)
        model1.add(y)
        xor.register(model1)
        model1.addConstraint(xor.resultVar eq Flt64.one)
        model1.minimize(px + py)
        val result1 = runBlocking { solver(model1) }
        assert(result1.value!!.obj eq Flt64.one)

        val model2 = LinearMetaModelFlt64()
        model2.add(x)
        model2.add(y)
        xor.register(model2)
        model2.addConstraint(xor.resultVar eq Flt64.zero)
        model2.maximize(px + py)
        val result2 = runBlocking { solver(model2) }
        assert(result2.value!!.obj geq Flt64.two)
    }

    private fun isScipAvailable(): Boolean {
        return runCatching { Class.forName("jscip.Scip") }.isSuccess
    }
}
