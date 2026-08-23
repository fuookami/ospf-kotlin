package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.express_effectiveness

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.AircraftContext
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.express_effectiveness.service.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.StowageContext
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto.*

/**
 * Type alias for the aircraft domain aggregation.
 * 飞行器域聚合类型别名
*/
internal typealias AircraftAggregation = fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.Aggregation

/**
 * Type alias for the stowage domain aggregation.
 * 配载域聚合类型别名
*/
internal typealias StowageAggregation = fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.Aggregation

/**
 * Context for managing express effectiveness constraints that optimize item priority ordering.
 * 管理优化项目优先级排序的快递效能约束的上下文。
*/
class ExpressEffectivenessContext {
    lateinit var aggregation: Aggregation

    /**
     * Initializes the express effectiveness context from aircraft and stowage contexts.
     * 从飞行器和配载上下文初始化快递效能上下文。
     *
     * @param aircraftContext 飞行器域上下文 / The aircraft domain context.
     * @param stowageContext 配载域上下文 / The stowage domain context.
     * @param input 包含输入参数的请求 DTO / The request DTO containing input parameters.
     * @param stowageMode 当前配载模式 / The current stowage mode.
     * @return 初始化操作的结果 / The result of the initialization operation.
    */
    fun init(
        aircraftContext: AircraftContext,
        stowageContext: StowageContext,
        input: RequestDTO,
        stowageMode: StowageMode
    ): Try {
        if (!::aggregation.isInitialized) {
            when (val result = AggregationInitializer.invoke(
                aircraftAggregation = aircraftContext.aggregation,
                stowageAggregation = stowageContext.aggregation,
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
        }

        return ok
    }

    /**
     * Registers express effectiveness constraints and pipelines into the model.
     * 将快递效能约束和管线注册到模型中。
     *
     * @param stowageMode 决定应用哪些约束的装载模式 / The stowage mode determining which constraints to apply.
     * @param parameter 约束生成的参数配置 / The parameter configuration for constraint generation.
     * @param model 要注册到的线性元模型 / The linear meta model to register into.
     * @return 注册操作的结果 / The result of the registration operation.
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
            is Ok -> {}

            is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
        }

        val generator = PipelineListGenerator(aggregation)
        val pipelines = when (val result = generator.invoke(
            stowageMode = stowageMode,
            parameter = parameter
        )) {
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
     * Registers express effectiveness constraints for the Benders master problem.
     * 为 Benders 主问题注册快递效能约束。
     *
     * @param model 要注册到的线性元模型 / The linear meta model to register into.
     * @return 注册操作的结果 / The result of the registration operation.
    */
    fun registerForBendersMP(
        stowageMode: StowageMode,
        parameter: Parameter,
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        return register(
            stowageMode = stowageMode,
            parameter = parameter,
            model = model
        )
    }

    /**
     * Registers express effectiveness for the Benders sub problem (no-op).
     * 为 Benders 子问题注册快递效能（空实现）。
     *
     * @param model 要注册到的线性元模型 / The linear meta model to register into.
     * @return 注册操作的结果 / The result of the registration operation.
    */
    fun registerForBendersSP(
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        return ok
    }

    /**
     * Flushes express effectiveness for the Benders sub problem (no-op).
     * 刷新 Benders 子问题的快递效能（空实现）。
     *
     * @param model 线性元模型 / The linear meta model.
     * @param solution 当前解向量 / The current solution vector.
     * @return 刷新操作的结果 / The result of the flush operation.
    */
    fun flushForBendersSP(
        model: AbstractLinearMetaModel<Flt64>,
        solution: List<Flt64>
    ): Try {
        return ok
    }
}
