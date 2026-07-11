package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.express_effectiveness

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.express_effectiveness.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.Position
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.*

/**
 * Aggregates express effectiveness models for item priority ordering based on stowage mode.
 * 基于装载模式聚合用于项目优先级排序的快递效能模型。
 *
 * @property items The list of cargo items to be prioritized. / 待排序的货物项列表
 * @property positions The list of available stowage positions. / 可用配载位置列表
 * @property stowage The stowage assignment model. / 配载分配模型
 * @property mustShipIndices Indices of items that must be shipped. / 必须发货的货物项索引列表
*/
class Aggregation(
    stowageMode: StowageMode,
    internal val items: List<Item>,
    internal val positions: List<Position>,
    internal val stowage: Stowage,
    val mustShipIndices: List<Int> = emptyList()
) {
    val absoluteOrder = when (stowageMode) {
        StowageMode.Predistribution -> {
            AbsoluteOrder(
                items = items,
                positions = positions
            )
        }

        StowageMode.FullLoad, StowageMode.WeightRecommendation -> {
            null
        }
    }

    val relativeOrder = when (stowageMode) {
        StowageMode.FullLoad -> {
            RelativeOrder(
                items = items,
                positions = positions,
                stowage = stowage
            )
        }

        StowageMode.Predistribution, StowageMode.WeightRecommendation -> {
            null
        }
    }

    /**
     * Registers express effectiveness models into the optimization model.
     * 将快递效能模型注册到优化模型中。
     *
     * @param stowageMode The stowage mode determining which sub-models to register. / 决定注册哪些子模型的装载模式
     * @param model The linear meta model to register into. / 要注册到的线性元模型
     * @return The result of the registration operation. / 注册操作的结果
    */
    fun register(
        stowageMode: StowageMode,
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        if (relativeOrder != null) {
            when (val result = relativeOrder.register(model)) {
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
     * Registers express effectiveness models for the Benders master problem.
     * 为 Benders 主问题注册快递效能模型。
     *
     * @param model The linear meta model to register into. / 要注册到的线性元模型
     * @return The result of the registration operation. / 注册操作的结果
    */
    fun registerForBendersMP(
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        return register(stowageMode = StowageMode.FullLoad, model = model)
    }

    /**
     * Registers express effectiveness models for the Benders sub problem.
     * 为 Benders 子问题注册快递效能模型。
     *
     * @param model The linear meta model to register into. / 要注册到的线性元模型
     * @param solution The current solution vector from the master problem. / 来自主问题的当前解向量
     * @return The result of the registration operation. / 注册操作的结果
    */
    fun registerForBendersSP(
        model: AbstractLinearMetaModel<Flt64>,
        solution: List<Flt64>
    ): Try {
        return ok
    }

    /**
     * Flushes express effectiveness for the Benders sub problem (no-op).
     * 刷新 Benders 子问题的快递效能（空实现）。
     *
     * @param model The linear meta model. / 线性元模型
     * @param solution The current solution vector. / 当前解向量
     * @return The result of the flush operation. / 刷新操作的结果
    */
    private fun flushForBendersSP(
        model: AbstractLinearMetaModel<Flt64>,
        solution: List<Flt64>
    ): Try {
        return ok
    }
}
