package fuookami.ospf.kotlin.example.framework_demo.demo1

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.core.backend.plugins.cplex.*
import fuookami.ospf.kotlin.core.backend.plugins.gurobi.*
import fuookami.ospf.kotlin.core.backend.plugins.scip.*
import fuookami.ospf.kotlin.example.framework_demo.demo1.infrastructure.*
import fuookami.ospf.kotlin.example.framework_demo.demo1.domain.route_context.*
import fuookami.ospf.kotlin.example.framework_demo.demo1.domain.bandwidth_context.*

class SSP {
    lateinit var routeContext: RouteContext
    lateinit var bandwidthContext: BandwidthContext

    suspend operator fun invoke(input: Input): Ret<Output> {
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
        when (result) {
            is Failed -> {
                return Failed(result.error)
            }

            is Ok -> {}
        }
        val solution = bandwidthContext.analyze(model, result.value)
        when (solution) {
            is Failed -> {
                return Failed(solution.error)
            }

            is Ok -> {}
        }

        return Ok(Output(solution.value.map { list -> list.map { it.id } }))
    }

    private fun init(input: Input): Try {
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

    private fun construct(model: LinearMetaModel): Try {
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

    private suspend fun solve(metaModel: LinearMetaModel): Ret<List<Flt64>> {
        val solver = SCIPLinearSolver()
        return when (val ret = solver(metaModel)) {
            is Ok -> {
                metaModel.tokens.setSolution(ret.value.solution)
                Ok(ret.value.solution)
            }

            is Failed -> {
                Failed(ret.error)
            }
        }
    }
}
