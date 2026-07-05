package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.loading_effectiveness

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.AircraftContext
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.loading_effectiveness.service.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.StowageContext
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto.*

/** 飞行器聚合类型别名。 */
typealias AircraftAggregation = fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.Aggregation
/** 配载聚合类型别名。 */
typealias StowageAggregation = fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.Aggregation

/**
 * 装车有效性上下文，管理装车有效性的初始化、注册与 Benders 分解流程。
 * Loading effectiveness context, managing initialization, registration, and Benders decomposition flow.
 */
class LoadingEffectivenessContext {
    /** 装车有效性聚合。 */
    lateinit var aggregation: Aggregation

    /**
     * 初始化装车有效性上下文。
     * Initialize the loading effectiveness context.
     *
     * @param aircraftContext 飞行器上下文
     * @param stowageContext 配载上下文
     * @param input 请求 DTO
     * @return 操作结果
     */
    fun init(
        aircraftContext: AircraftContext,
        stowageContext: StowageContext,
        input: RequestDTO
    ): Try {
        if (!::aggregation.isInitialized) {
            when (val result = AggregationInitializer.invoke(
                aircraftAggregation = aircraftContext.aggregation,
                stowageAggregation = stowageContext.aggregation,
                input = input
            )) {
                is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    aggregation = result.value!!
                }

                is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Failed(result.error)
                }

                is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Fatal(result.errors)
                }
            }
        }

        return ok
    }

    /**
     * 注册装车有效性约束与流水线到模型中。
     * Register loading effectiveness constraints and pipelines into the model.
     *
     * @param stowageMode 装载模式
     * @param parameter 参数配置
     * @param model 线性元模型
     * @return 操作结果
     */
    fun register(
        stowageMode: StowageMode,
        parameter: Parameter,
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        when (val result = aggregation.register(
            stowageMode = stowageMode,
            model = model
        )) {
            is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}

            is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Failed(result.error)
                }

                is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Fatal(result.errors)
                }
        }

        val generator = PipelineListGenerator(aggregation)
        val pipelines = when (val result = generator.invoke(
            stowageMode = stowageMode,
            parameter = parameter
        )) {
            is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                result.value!!
            }

            is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Failed(result.error)
                }

                is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Fatal(result.errors)
                }
        }

        for (pipeline in pipelines) {
            when (val result = pipeline(model)) {
                is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}

                is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Failed(result.error)
                }

                is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Fatal(result.errors)
                }
            }
        }

        return ok
    }

    /**
     * 注册 Benders 主问题的装车有效性约束。
     * Register loading effectiveness constraints for the Benders master problem.
     *
     * @param model 线性元模型
     * @return 操作结果
     */
    fun registerForBendersMP(
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        // Loading effectiveness constraints go into the master problem.
        return register(
            stowageMode = StowageMode.FullLoad,
            parameter = Parameter(),
            model = model
        )
    }

    /**
     * 注册 Benders 子问题的装车有效性（空实现，装车有效性不贡献给子问题）。
     * Register loading effectiveness for the Benders sub problem (no-op, loading effectiveness does not contribute to the sub problem).
     *
     * @param model 线性元模型
     * @return 操作结果
     */
    fun registerForBendersSP(
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        // Loading effectiveness does not contribute to the sub problem.
        return ok
    }

    /**
     * 刷新 Benders 子问题的装车有效性（空实现）。
     * Flush loading effectiveness for Benders sub problem (no-op).
     *
     * @param model 线性元模型
     * @param solution 解向量
     * @return 操作结果
     */
    fun flushForBendersSP(
        model: AbstractLinearMetaModel<Flt64>,
        solution: List<Flt64>
    ): Try {
        return ok
    }
}

