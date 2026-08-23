package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.airworthiness_security

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.AircraftContext
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.airworthiness_security.service.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.mac.MacContext
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.StowageContext
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto.*

/** Type alias for the aircraft domain aggregation. / 飞机域聚合的类型别名。 */
internal typealias AircraftAggregation = fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.Aggregation

/** Type alias for the stowage domain aggregation. / 装载域聚合的类型别名。 */
internal typealias StowageAggregation = fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.Aggregation

/** Type alias for the MAC domain aggregation. / MAC 域聚合的类型别名。 */
internal typealias MACAggregation = fuookami.ospf.kotlin.example.framework_demo.demo2.domain.mac.Aggregation

/**
 * Context for managing airworthiness and safety constraints across aircraft, stowage, and MAC domains.
 * 跨飞机、装载和 MAC 域管理适航和安全约束的上下文。
*/
class AirworthinessSecurityContext {
    lateinit var aggregation: Aggregation

    /**
     * Initializes the airworthiness security aggregation from the given contexts and input.
     * 从给定的上下文和输入初始化适航安全聚合。
     *
     * @param aircraftContext 飞机上下文 / The aircraft context.
     * @param stowageContext 装载上下文 / The stowage context.
     * @param macContext The MAC context. / MAC 上下文
     * @param input 请求 DTO 输入数据 / The request DTO input data.
     * @return 成功或失败结果 / Success or failure result.
    */
    fun init(
        aircraftContext: AircraftContext,
        stowageContext: StowageContext,
        macContext: MacContext,
        input: RequestDTO
    ): Try {
        if (!::aggregation.isInitialized) {
            when (val result = AggregationInitializer.invoke(
                aircraftAggregation = aircraftContext.aggregation,
                stowageAggregation = stowageContext.aggregation,
                macAggregation = macContext.aggregation,
                input = input
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
     * Registers all airworthiness and safety constraints with the given model.
     * 将所有适航和安全约束注册到给定模型中。
     *
     * @param stowageMode 使用的装载模式 / The stowage mode to use.
     * @param model 要注册约束的线性元模型 / The linear meta model to register constraints with.
     * @return 成功或失败结果 / Success or failure result.
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
     * Registers constraints for the Benders decomposition master problem.
     * 为 Benders 分解主问题注册约束。
     *
     * @param model The linear meta model for the master problem. / Benders 主问题的线性元模型
     * @return 成功或失败结果 / Success or failure result.
    */
    fun registerForBendersMP(
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        return ok
    }

    /**
     * Registers constraints for the Benders decomposition sub-problem.
     * 为 Benders 分解子问题注册约束。
     *
     * @param model The linear meta model for the sub-problem. / Benders 子问题的线性元模型
     * @return 成功或失败结果 / Success or failure result.
    */
    fun registerForBendersSP(
        stowageMode: StowageMode,
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        return register(
            stowageMode = stowageMode,
            model = model
        )
    }

    /**
     * Flushes state for the Benders decomposition sub-problem.
     * 为 Benders 分解子问题刷新状态。
     *
     * @param model The linear meta model for the sub-problem. / Benders 子问题的线性元模型
     * @param solution 主问题的解 / The solution from the master problem.
     * @return 成功或失败结果 / Success or failure result.
    */
    fun flushForBendersSP(
        model: AbstractLinearMetaModel<Flt64>,
        solution: List<Flt64>
    ): Try {
        return ok
    }
}
