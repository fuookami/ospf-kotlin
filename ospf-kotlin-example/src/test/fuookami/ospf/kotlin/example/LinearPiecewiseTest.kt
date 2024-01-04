package fuookami.ospf.kotlin.example

import kotlinx.coroutines.*
import org.junit.jupiter.api.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.geometry.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.core.backend.intermediate_model.*
import fuookami.ospf.kotlin.core.backend.solver.config.*
import fuookami.ospf.kotlin.core.backend.plugins.cplex.*

class LinearPiecewiseTest {
    @Test
    fun univariate() {
        val metaModel = LinearMetaModel()

        val x = URealVar("x")
        x.range.leq(Flt64.two)
        metaModel.addVar(x)

        val ulp = NormalUnivariateLinearPiecewiseFunction(
            x = LinearPolynomial(x),
            points = listOf(
                point2(),
                point2(x = Flt64.one, y = Flt64.two),
                point2(x = Flt64.two, y = Flt64.one)
            ),
            name = "y"
        )
        metaModel.addSymbol(ulp)

        metaModel.maximize(LinearPolynomial(ulp))

        val solver = CplexLinearSolver(LinearSolverConfig())
        val model = runBlocking { LinearTriadModel(LinearModel(metaModel)) }
        val result = runBlocking { solver(model) }
        assert(result.value!!.results[0] eq Flt64.one)
    }

    @Test
    fun bivariate() {
        val metaModel = LinearMetaModel()

        val x = URealVar("x")
        val y = URealVar("y")
        x.range.leq(Flt64.two)
        y.range.leq(Flt64.two)
        metaModel.addVar(x)
        metaModel.addVar(y)

        val blp = CommonBivariateLinearPiecewiseFunction(
            x = LinearPolynomial(x),
            y = LinearPolynomial(y),
            points = listOf(
                point3(),
                point3(x = Flt64.two),
                point3(y = Flt64.two),
                point3(x = Flt64.two, y = Flt64.two),
                point3(x = Flt64.one, y = Flt64.one, z = Flt64.one)
            ),
            name = "z"
        )
        metaModel.addSymbol(blp)

        metaModel.maximize(LinearPolynomial(blp))

        val solver = CplexLinearSolver(LinearSolverConfig())
        val model = runBlocking { LinearTriadModel(LinearModel(metaModel)) }
        val result = runBlocking { solver(model) }
        assert(result.value!!.results[0] eq Flt64.one)
        assert(result.value!!.results[1] eq Flt64.one)
    }
}
