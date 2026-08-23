package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.recommended_weight_equalization

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.AircraftContext
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.recommended_weight_equalization.service.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.StowageContext
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto.*

/** Type alias for the aircraft domain aggregation. / 飞机域聚合的类型别名。 */
internal typealias AircraftAggregation = fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.Aggregation

/** Type alias for the stowage domain aggregation. / 装载域聚合的类型别名。 */
internal typealias StowageAggregation = fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.Aggregation

/**
 * Context for managing recommended weight equalization across aircraft and stowage domains.
 * 跨飞机和装载域管理推荐重量均衡的上下文。
*/
class RecommendedWeightEqualizationContext {
    lateinit var aggregation: Aggregation

    /**
     * Initializes the recommended weight equalization context with aircraft and stowage data.
     * 使用飞机和装载数据初始化推荐重量均衡上下文。
     *
     * @param aircraftContext 提供飞机模型数据的飞机上下文 / The aircraft context providing aircraft model data
     * @param stowageContext 提供装载分配数据的装载上下文 / The stowage context providing stowage assignment data
     * @param input 请求 DTO 输入数据 / The request DTO input data
     * @return 成功或失败结果 / Success or failure result
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
     * Registers recommended weight equalization constraints into the optimization model.
     * 将推荐重量均衡约束注册到优化模型中。
     *
     * @param stowageMode 优化的装载模式 / The stowage mode for the optimization
     * @param model 要注册到的线性元模型 / The linear meta model to register into
     * @return 成功或失败结果 / Success or failure result
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
     * Registers recommended weight equalization constraints for the Benders master problem.
     * 为 Benders 主问题注册推荐重量均衡约束。
     *
     * @param model 主问题的线性元模型 / The linear meta model for the master problem
     * @return 成功或失败结果 / Success or failure result
    */
    fun registerForBendersMP(
        parameter: Parameter,
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        return register(
            stowageMode = StowageMode.WeightRecommendation,
            parameter = parameter,
            model = model
        )
    }

    /**
     * Registers recommended weight equalization constraints for the Benders sub-problem.
     * 为 Benders 子问题注册推荐重量均衡约束。
     *
     * @param model 子问题的线性元模型 / The linear meta model for the sub-problem
     * @return 成功或失败结果 / Success or failure result
    */
    fun registerForBendersSP(
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        return ok
    }

    /**
     * Flushes the Benders sub-problem solution into the recommended weight equalization context.
     * 将 Benders 子问题解刷新到推荐重量均衡上下文中。
     *
     * @param model 子问题的线性元模型 / The linear meta model for the sub-problem
     * @param solution 子问题的解值 / The solution values from the sub-problem
     * @return 成功或失败结果 / Success or failure result
    */
    fun flushForBendersSP(
        model: AbstractLinearMetaModel<Flt64>,
        solution: List<Flt64>
    ): Try {
        return ok
    }
}
