package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.mac_optimization

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.AircraftContext
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.mac.MacContext
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.mac_optimization.service.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.StowageContext
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto.*

/** Type alias for the aircraft domain Aggregation. 中文：飞机域聚合的类型别名。 */
internal typealias AircraftAggregation = fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.Aggregation

/** Type alias for the stowage domain Aggregation. 中文：装载域聚合的类型别名。 */
internal typealias StowageAggregation = fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.Aggregation

/** Type alias for the MAC domain Aggregation. 中文：MAC 域聚合的类型别名。 */
internal typealias MACAggregation = fuookami.ospf.kotlin.example.framework_demo.demo2.domain.mac.Aggregation

/**
 * Context for managing MAC optimization including longitudinal and lateral balance constraints.
 * 管理 MAC 优化（包括纵向和横向平衡约束）的上下文。
 *
 * @property aggregation The MAC optimization aggregation instance / MAC 优化聚合实例
*/
class MacOptimizationContext {
    lateinit var aggregation: Aggregation

    /**
     * Initializes the MAC optimization aggregation from aircraft, stowage, and MAC contexts.
     * 从飞机、装载和 MAC 上下文初始化 MAC 优化聚合。
     *
     * @param aircraftContext The aircraft context providing aircraft model data / 提供飞机模型数据的飞机上下文
     * @param stowageContext The stowage context providing load and position data / 提供装载和位置数据的装载上下文
     * @param macContext The MAC context providing torque and MAC data / 提供扭矩和 MAC 数据的 MAC 上下文
     * @param input The request DTO containing optimization input / 包含优化输入的请求 DTO
     * @return [Try] indicating success or failure / 表示成功或失败
    */
    fun init(
        aircraftContext: AircraftContext,
        stowageContext: StowageContext,
        macContext: MacContext,
        input: RequestDTO
    ): Try {
        if (!::aggregation.isInitialized) {
            when (val result = AggregationInitializer(
                aircraftAggregation = aircraftContext.aggregation,
                stowageAggregation = stowageContext.aggregation,
                macAggregation = macContext.aggregation,
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
     * Registers the MAC optimization aggregation and constraint pipelines into the optimization model.
     * 将 MAC 优化聚合和约束管线注册到优化模型中。
     *
     * @param stowageMode The stowage mode controlling registration / 控制注册行为的装载模式
     * @param parameter The parameter for pipeline generation / 管线生成的参数
     * @param model The linear meta-model to register into / 要注册的线性元模型
     * @return [Try] indicating success or failure / 表示成功或失败
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
     * Registers the MAC optimization for the Benders master problem using full-load stowage mode.
     * 使用满载装载模式为 Benders 主问题注册 MAC 优化。
     *
     * @param model The linear meta-model for the master problem / Benders 主问题的线性元模型
     * @return [Try] indicating success or failure / 表示成功或失败
    */
    fun registerForBendersMP(
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        return register(
            stowageMode = StowageMode.FullLoad,
            parameter = Parameter(),
            model = model
        )
    }

    /**
     * Registers symbols for the Benders sub-problem.
     * 为 Benders 子问题注册符号。
     *
     * @param model The linear meta-model for the sub-problem / Benders 子问题的线性元模型
     * @return [Try] indicating success or failure / 表示成功或失败
    */
    fun registerForBendersSP(
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        return ok
    }

    /**
     * Flushes state for the Benders sub-problem after solving.
     * 求解后刷新 Benders 子问题的状态。
     *
     * @param model The linear meta-model for the sub-problem / Benders 子问题的线性元模型
     * @param solution The solution from the master problem / 来自主问题的解
     * @return [Try] indicating success or failure / 表示成功或失败
    */
    fun flushForBendersSP(
        model: AbstractLinearMetaModel<Flt64>,
        solution: List<Flt64>
    ): Try {
        return ok
    }
}
