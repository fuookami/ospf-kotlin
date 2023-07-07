package fuookami.ospf.kotlin.example.framework_demo.demo1

import kotlin.io.path.*
import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.parallel.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.core.backend.intermediate_model.*
import fuookami.ospf.kotlin.core.backend.solver.config.*
import fuookami.ospf.kotlin.core.backend.plugins.gurobi.*
import fuookami.ospf.kotlin.core.backend.plugins.scip.*
import fuookami.ospf.kotlin.core.backend.plugins.cplex.*
import fuookami.ospf.kotlin.example.framework_demo.demo1.infrastructure.*
import fuookami.ospf.kotlin.example.framework_demo.demo1.domain.route_context.*
import fuookami.ospf.kotlin.example.framework_demo.demo1.domain.bandwidth_context.*

class SSP {
    lateinit var routeContext: RouteContext
    lateinit var bandwidthContext: BandwidthContext

    operator fun invoke(input: Input): Result<Output, Error> {
        when (val result = init(input)) {
            is Failed -> {
                return Failed(result.error)
            }

            is Ok -> {}
        }
        val model = LinearMetaModel("demo1")
        when (val result = construct(model)) {
            is Failed -> {
                return Failed(result.error)
            }

            is Ok -> {}
        }
        val result = solve(model)
        if (result is Failed) {
            return Failed(result.error)
        }
        val solution = bandwidthContext.analyze(model, result.value()!!)
        if (solution is Failed) {
            return Failed(solution.error)
        }

        return Ok(Output(solution.value()!!.map { list -> list.map { it.id } }))
    }

    private fun init(input: Input): Try<Error> {
        routeContext = RouteContext()
        bandwidthContext = BandwidthContext(routeContext)

        when (val result = routeContext.init(input)) {
            is Failed -> {
                return Failed(result.error)
            }

            is Ok -> {}
        }
        when (val result = bandwidthContext.init(input)) {
            is Failed -> {
                return Failed(result.error)
            }

            is Ok -> {}
        }

        return Ok(success)
    }

    private fun construct(model: LinearMetaModel): Try<Error> {
        when (val result = routeContext.register(model)) {
            is Failed -> {
                return Failed(result.error)
            }

            is Ok -> {}
        }
        when (val result = bandwidthContext.register(model)) {
            is Failed -> {
                return Failed(result.error)
            }

            is Ok -> {}
        }

        when (val result = routeContext.construct(model)) {
            is Failed -> {
                return Failed(result.error)
            }

            is Ok -> {}
        }
        when (val result = bandwidthContext.construct(model)) {
            is Failed -> {
                return Failed(result.error)
            }

            is Ok -> {}
        }

        return Ok(success)
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun solve(metaModel: LinearMetaModel): Result<List<Flt64>, Error> {
        GlobalScope.launch(Dispatchers.IO) { metaModel.export("demo1.opm") }

        // val solver = GurobiLinearSolver()
        // val solver = SCIPLinearSolver(LinearSolverConfig())
        val solver = CplexLinearSolver(LinearSolverConfig())
        val model = runBlocking { LinearTriadModel(LinearModel(metaModel)) }
        GlobalScope.launch(Dispatchers.IO) { model.export(Path("."), ModelFileFormat.LP) }

        return when (val ret = solver(model)) {
            is Ok -> {
                metaModel.tokens.setSolution(ret.value.results)
                Ok(ret.value.results)
            }

            is Failed -> {
                Failed(ret.error)
            }
        }
    }
}
