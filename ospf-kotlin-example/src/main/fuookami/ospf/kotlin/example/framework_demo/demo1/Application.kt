package fuookami.ospf.kotlin.example.framework_demo.demo1

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.scip.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.example.framework_demo.demo1.bandwidth_context.*
import fuookami.ospf.kotlin.example.framework_demo.demo1.infrastructure.*
import fuookami.ospf.kotlin.example.framework_demo.demo1.route_context.*
import fuookami.ospf.kotlin.example.solveLinearMetaModel

/**
 * Value converter for Flt64 that provides identity transformations and zero/one constants.
 * Flt64 值转换器，提供恒等变换和零/一常量。
*/
private val flt64Converter = object : IntoValue<Flt64> {
    override fun intoValue(value: Flt64) = value
    override val zero get() = Flt64.zero
    override val one get() = Flt64.one
    override fun fromValue(value: Flt64) = value
}

/**
 * Shortest Service Path (SSP) solver that models route assignment and bandwidth allocation
 * as a linear optimization problem on a network graph.
 * 最短服务路径（SSP）求解器，将路由分配和带宽分配建模为网络图上的线性优化问题。
 *
 * @see https://fuookami.github.io/ospf/examples/framework-example1.html
*/
class SSP {
    lateinit var routeContext: RouteContext
    lateinit var bandwidthContext: BandwidthContext

    suspend operator fun invoke(input: Input): Ret<Output> {
        when (val result = init(input)) {
            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }

            is Ok -> {}
        }
        val model = LinearMetaModel<Flt64>("demo1", converter = flt64Converter)
        when (val result = construct(model)) {
            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }

            is Ok -> {}
        }
        val result = solve(model)
        when (result) {
            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }

            is Ok -> {}
        }
        val solution = bandwidthContext.analyze(model, result.value)
        when (solution) {
            is Failed -> {
                return Failed(solution.error)
            }

            is Fatal -> {
                return Fatal(solution.errors)
            }

            is Ok -> {}
        }

        return Ok(Output(solution.value.map { list -> list.map { it.id } }))
    }

    /**
     * Initializes the route and bandwidth contexts from the input data.
     * 从输入数据初始化路由和带宽上下文。
     *
     * @param input the aggregated input data / 聚合输入数据
     * @return the initialization result / 初始化结果
    */
    private fun init(input: Input): Try {
        routeContext = RouteContext()
        bandwidthContext = BandwidthContext(routeContext)

        when (val result = routeContext.init(input)) {
            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }

            is Ok -> {}
        }
        when (val result = bandwidthContext.init(input)) {
            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }

            is Ok -> {}
        }

        return ok
    }

    /**
     * Constructs the optimization model by registering and building route and bandwidth constraints.
     * 通过注册和构建路由与带宽约束来构建优化模型。
     *
     * @param model the linear meta model / 线性元模型
     * @return the construction result / 构建结果
    */
    private fun construct(model: LinearMetaModel<Flt64>): Try {
        when (val result = routeContext.register(model)) {
            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }

            is Ok -> {}
        }
        when (val result = bandwidthContext.register(model)) {
            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }

            is Ok -> {}
        }

        when (val result = routeContext.construct(model)) {
            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }

            is Ok -> {}
        }
        when (val result = bandwidthContext.construct(model)) {
            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }

            is Ok -> {}
        }

        return ok
    }

    /**
     * Solves the constructed optimization model using the SCIP linear solver.
     * 使用 SCIP 线性求解器求解已构建的优化模型。
     *
     * @param metaModel the linear meta model to solve / 待求解的线性元模型
     * @return the solution values / 求解值列表
    */
    private suspend fun solve(metaModel: LinearMetaModel<Flt64>): Ret<List<Flt64>> {
        val solver = ScipLinearSolver()
        return when (val ret = solveLinearMetaModel(solver, metaModel)) {
            is Ok -> {
                metaModel.tokens.setSolution(ret.value.solution)
                Ok(ret.value.solution)
            }

            is Failed -> {
                Failed(ret.error)
            }

            is Fatal -> {
                Fatal(ret.errors)
            }
        }
    }
}
