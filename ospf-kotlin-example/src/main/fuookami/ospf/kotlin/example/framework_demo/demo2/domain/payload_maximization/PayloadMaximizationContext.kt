package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.payload_maximization

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.AircraftContext
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.payload_maximization.service.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.StowageContext
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto.*

/**
 * Context for managing payload maximization optimization across aircraft and stowage domains.
 * 跨飞机和装载域管理载荷最大化优化的上下文。
*/
class PayloadMaximizationContext {
    lateinit var aggregation: Aggregation

    /**
     * Initializes the payload maximization context with aircraft and stowage data.
     * 使用飞机和装载数据初始化载荷最大化上下文。
     *
     * @param aircraftContext The aircraft context providing aircraft model data / 提供飞机模型数据的飞机上下文
     * @param stowageContext The stowage context providing payload data / 提供载荷数据的装载上下文
     * @param input The request DTO input data / 请求 DTO 输入数据
     * @return Success or failure result / 成功或失败结果
    */
    fun init(
        aircraftContext: AircraftContext,
        stowageContext: StowageContext,
        input: RequestDTO
    ): Try {
        if (!::aggregation.isInitialized) {
            aggregation = Aggregation(
                aircraftModel = aircraftContext.aggregation.aircraftModel,
                payload = stowageContext.aggregation.payload
            )
        }

        return ok
    }

    /**
     * Registers payload maximization constraints and objectives into the optimization model.
     * 将载荷最大化约束和目标注册到优化模型中。
     *
     * @param stowageMode The stowage mode for the optimization / 优化的装载模式
     * @param parameter The optimization parameter / 优化参数
     * @param model The linear meta model to register into / 要注册到的线性元模型
     * @return Success or failure result / 成功或失败结果
    */
    fun register(
        stowageMode: StowageMode,
        parameter: Parameter,
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
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
     * Registers payload maximization constraints for the Benders master problem.
     * 为 Benders 主问题注册载荷最大化约束。
     *
     * @param model The linear meta model for the master problem / 主问题的线性元模型
     * @return Success or failure result / 成功或失败结果
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
     * Registers payload maximization constraints for the Benders sub-problem.
     * 为 Benders 子问题注册载荷最大化约束。
     *
     * @param model The linear meta model for the sub-problem / 子问题的线性元模型
     * @return Success or failure result / 成功或失败结果
    */
    fun registerForBendersSP(
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        return ok
    }

    /**
     * Flushes the Benders sub-problem solution into the payload maximization context.
     * 将 Benders 子问题解刷新到载荷最大化上下文中。
     *
     * @param model The linear meta model for the sub-problem / 子问题的线性元模型
     * @param solution The solution values from the sub-problem / 子问题的解值
     * @return Success or failure result / 成功或失败结果
    */
    fun flushForBendersSP(
        model: AbstractLinearMetaModel<Flt64>,
        solution: List<Flt64>
    ): Try {
        return ok
    }
}
