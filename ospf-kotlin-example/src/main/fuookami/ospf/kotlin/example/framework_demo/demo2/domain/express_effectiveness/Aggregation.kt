package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.express_effectiveness

import fuookami.ospf.kotlin.utils.error.*
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
 * @property items 待排序的货物项列表 / The list of cargo items to be prioritized.
 * @property positions 可用配载位置列表 / The list of available stowage positions.
 * @property stowage 配载分配模型 / The stowage assignment model.
 * @property mustShipIndices 必须发货的货物项索引列表 / Indices of items that must be shipped.
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
     * @param stowageMode 决定注册哪些子模型的装载模式 / The stowage mode determining which sub-models to register.
     * @param model 要注册到的线性元模型 / The linear meta model to register into.
     * @return 注册操作的结果 / The result of the registration operation.
    */
    fun register(
        stowageMode: StowageMode,
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        if (relativeOrder != null) {
            when (val result = relativeOrder.register(model)) {
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
     * Registers express effectiveness models for the Benders master problem.
     * 为 Benders 主问题注册快递效能模型。
     *
     * @param stowageMode 优化的装载模式 / The stowage mode for the optimization.
     * @param model 要注册到的线性元模型 / The linear meta model to register into.
     * @return 注册操作的结果 / The result of the registration operation.
    */
    fun registerForBendersMP(
        stowageMode: StowageMode,
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        return register(stowageMode = stowageMode, model = model)
    }

    /**
     * Registers express effectiveness models for the Benders sub problem.
     * 为 Benders 子问题注册快递效能模型。
     *
     * @param model 要注册到的线性元模型 / The linear meta model to register into.
     * @param solution 来自主问题的当前解向量 / The current solution vector from the master problem.
     * @return 注册操作的结果 / The result of the registration operation.
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
     * @param model 线性元模型 / The linear meta model.
     * @param solution 当前解向量 / The current solution vector.
     * @return 刷新操作的结果 / The result of the flush operation.
    */
    private fun flushForBendersSP(
        model: AbstractLinearMetaModel<Flt64>,
        solution: List<Flt64>
    ): Try {
        return ok
    }
}
