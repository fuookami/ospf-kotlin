package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.AircraftContext
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.Solution as StowageSolution
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.service.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto.*

/** Type alias for the aircraft domain Aggregation class / 飞机域聚合类的类型别名 */
internal typealias AircraftAggregation = fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.Aggregation

/**
 * Stowage context that manages the stowage aggregation and provides
 * initialization, model registration, and solution analysis capabilities.
 * 配载上下文，管理配载聚合并提供初始化、模型注册和方案分析能力。
*/
class StowageContext {
    lateinit var aggregation: Aggregation

    /**
     * Initializes the stowage aggregation from the aircraft context and request input.
     * 根据飞机上下文和请求输入初始化配载聚合。
     *
     * @param aircraftContext 提供飞机聚合的飞机上下文 / the aircraft context providing aircraft aggregation
     * @param input 包含配载输入数据的请求 DTO / the request DTO containing stowage input data
     * @return 成功或失败 / success or failure
    */
    fun init(
        aircraftContext: AircraftContext,
        input: RequestDTO,
        stowageMode: StowageMode
    ): Try {
        when (val result = AggregationInitializer(
            aircraftAggregation = aircraftContext.aggregation,
            input = input,
            stowageMode = stowageMode
        )) {
            is Ok -> {
                aggregation = result.value!!
            }

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        return ok
    }

    /**
     * Registers the stowage aggregation and all constraint pipelines into the model.
     * 将配载聚合和所有约束管道注册到模型中。
     *
     * @param stowageMode 使用的装载模式 / the stowage mode to use
     * @param stowageMode 使用的装载模式 / the stowage mode to use
     * @param model 要注册到的线性元模型 / the linear meta-model to register into
     * @return 成功或失败 / success or failure
    */
    fun register(
        stowageMode: StowageMode,
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        when (val result = aggregation.register(
            stowageMode = stowageMode,
            model = model
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        val generator = PipelineListGenerator(aggregation)
        val pipelines = when (val result = generator.invoke(stowageMode)) {
            is Ok -> {
                result.value!!
            }

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        for (pipeline in pipelines) {
            when (val result = pipeline(model)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
            }
        }

        return ok
    }

    /**
     * Registers sub-models for the Benders master problem.
     * 为 Benders 主问题注册子模型。
     *
     * @param model 要注册到的线性元模型 / the linear meta-model to register into
     * @return 成功或失败 / success or failure
    */
    fun registerForBendersMP(
        stowageMode: StowageMode,
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        when (val result = aggregation.registerForBendersMP(stowageMode, model)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        return ok
    }

    /**
     * Registers sub-models for the Benders sub-problem.
     * 为 Benders 子问题注册子模型。
     *
     * @param stowageMode 使用的装载模式 / the stowage mode to use
     * @param model 要注册到的线性元模型 / the linear meta-model to register into
     * @param solution 主问题解 / the master problem solution
     * @return 成功或失败 / success or failure
    */
    fun registerForBendersSP(
        stowageMode: StowageMode,
        model: AbstractLinearMetaModel<Flt64>,
        solution: List<Flt64>
    ): Try {
        when (val result = aggregation.registerForBendersSP(stowageMode, model, solution)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        return ok
    }

    /**
     * Flushes master problem variable values into the Benders sub-problem model.
     * 将主问题变量值刷新到 Benders 子问题模型中。
     *
     * @param model 线性元模型 / the linear meta-model
     * @param solution 主问题解 / the master problem solution
     * @return 成功或失败 / success or failure
    */
    fun flushForBendersSP(
        stowageMode: StowageMode,
        model: AbstractLinearMetaModel<Flt64>,
        solution: List<Flt64>
    ): Try {
        when (val result = aggregation.registerForBendersSP(stowageMode, model, solution)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        return ok
    }

    /**
     * Analyzes the solver solution and produces a stowage domain solution.
     * 分析求解器解并生成配载域方案。
     *
     * @param solution 求解器解值 / the solver solution values
     * @param model 线性元模型 / the linear meta-model
     * @return 配载方案或失败 / the stowage solution or failure
    */
    fun analyze(
        solution: List<Flt64>,
        model: AbstractLinearMetaModel<Flt64>
    ): Ret<StowageSolution> {
        val analyzer = SolutionAnalyzer(aggregation)
        val stowageSolution = when (val result = analyzer(solution, model)) {
            is Ok -> {
                result.value!!
            }

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        return Ok(stowageSolution)
    }

    /**
     * Analyzes a stowage solution and produces a response DTO.
     * 分析配载方案并生成响应 DTO。
     *
     * @param solution 配载域方案 / the stowage domain solution
     * @param input 原始请求 DTO / the original request DTO
     * @return 响应 DTO 或失败 / the response DTO or failure
    */
    fun analyze(
        solution: StowageSolution,
        input: RequestDTO
    ): Ret<ResponseDTO> {
        val assignments = solution.stowage.entries
            .sortedWith(
                compareBy<Map.Entry<Position, List<Item>>>(
                    { it.key.loadingOrder.location.ordinal },
                    { it.key.loadingOrder.order.toString().toInt() },
                    { it.key.id.toString() }
                )
            )
            .flatMap { (position, items) ->
                items.sortedBy { it.id }.map { item ->
                    "${item.id} -> ${position.spaceName}"
                }
            }

        return Ok(ResponseDTO(
            succeed = true,
            status = "Optimal",
            assignments = assignments
        ))
    }
}
