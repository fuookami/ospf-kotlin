package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.mac

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.mac.service.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto.*

/** Type alias for the aircraft domain Aggregation. 中文：飞机域聚合的类型别名。 */
internal typealias AircraftAggregation = fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.Aggregation

/** Type alias for the stowage domain Aggregation. 中文：装载域聚合的类型别名。 */
internal typealias StowageAggregation = fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.Aggregation

/**
 * Context for managing MAC (Mean Aerodynamic Chord) computations from aircraft and stowage data.
 * 从飞机和装载数据管理 MAC（平均气动弦）计算的上下文。
 *
 * @property aggregation The MAC aggregation instance / MAC 聚合实例
*/
class MacContext {
    lateinit var aggregation: Aggregation

    /**
     * Initializes the MAC aggregation from aircraft, stowage, and request data.
     * 从飞机、装载和请求数据初始化 MAC 聚合。
     *
     * @param aircraftContext 提供飞机模型数据的飞机上下文 / The aircraft context providing aircraft model data
     * @param stowageContext 提供装载和位置数据的装载上下文 / The stowage context providing load and position data
     * @param input 包含 MAC 相关输入的请求 DTO / The request DTO containing MAC-related input
     * @return 表示成功或失败 / [Try] indicating success or failure
    */
    fun init(
        aircraftContext: AircraftContext,
        stowageContext: StowageContext,
        input: RequestDTO
    ): Try {
        when (val result = AggregationInitializer(
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

        return ok
    }

    /**
     * Registers the MAC aggregation symbols into the optimization model.
     * 将 MAC 聚合符号注册到优化模型中。
     *
     * @param stowageMode 控制注册行为的装载模式 / The stowage mode controlling registration
     * @param model 要注册的线性元模型 / The linear meta-model to register into
     * @return 表示成功或失败 / [Try] indicating success or failure
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

        return ok
    }

    /**
     * Registers the MAC aggregation for the Benders master problem using full-load stowage mode.
     * 使用满载装载模式为 Benders 主问题注册 MAC 聚合。
     *
     * @param model The linear meta-model for the master problem / Benders 主问题的线性元模型
     * @return 表示成功或失败 / [Try] indicating success or failure
    */
    fun registerForBendersMP(
        stowageMode: StowageMode,
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        when (val result = aggregation.register(
            stowageMode = stowageMode,
            model = model
        )) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        return ok
    }

    /**
     * Registers symbols for the Benders sub-problem.
     * 为 Benders 子问题注册符号。
     *
     * @param model The linear meta-model for the sub-problem / Benders 子问题的线性元模型
     * @return 表示成功或失败 / [Try] indicating success or failure
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
     * @param solution 来自主问题的解 / The solution from the master problem
     * @return 表示成功或失败 / [Try] indicating success or failure
    */
    fun flushForBendersSP(
        model: AbstractLinearMetaModel<Flt64>,
        solution: List<Flt64>
    ): Try {
        return ok
    }
}
