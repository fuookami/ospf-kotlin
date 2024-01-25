package fuookami.ospf.kotlin.example.heuristic_demo

import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function.*
import fuookami.ospf.kotlin.core.frontend.model.*
import fuookami.ospf.kotlin.core.frontend.model.callback.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.core.backend.intermediate_model.*
import fuookami.ospf.kotlin.core.backend.solver.config.*
import fuookami.ospf.kotlin.core.backend.plugins.cplex.*
import fuookami.ospf.kotlin.core.backend.plugins.heuristic.pso.*

class Demo2 {
    operator fun invoke(): Try {
        val metaModel = LinearMetaModel()
        val x = URealVar("x")
        val y = URealVar("y")
        x.range.leq(Flt64.two)
        y.range.leq(Flt64.two)
        metaModel.addVar(x)
        metaModel.addVar(y)
//        val abs = AbsFunction(x - Flt64.one)
//        metaModel.addSymbol(abs)
//        metaModel.addObject(ObjectCategory.Minimum, LinearPolynomial(abs))
        metaModel.addObject(ObjectCategory.Maximum, x + y)
        val model = when (val result = runBlocking { LinearModel(metaModel) }) {
            is Ok -> {
                result.value
            }

            is Failed -> {
                return Failed(result.error)
            }
        }
//        val solver = CplexLinearSolver(LinearSolverConfig())
//        val result = when (val ret = solver(runBlocking { LinearTriadModel(model) })) {
//            is Ok -> {
//                metaModel.tokens.setSolution(ret.value.results)
//                ret.value.results
//            }
//
//            is Failed -> {
//                return Failed(ret.error)
//            }
//        }
        val callBackModel = CallBackModel(model)
        val solver = PSO(policy = CommonPSOPolicy(timeLimit = 10.seconds))
        val result = solver(callBackModel)
        return Ok(success)
    }
}
