package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.loading_effectiveness

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.loading_effectiveness.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.Position
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.PositionPair
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.*

/**
 * Aggregates loading effectiveness models for cargo loading optimization based on stowage mode.
 * 基于装载模式聚合用于货物装载优化的装车效能模型。
 *
 * @property flight The flight information for the current scheduling. / 当前调度的航班信息
 * @property items The list of cargo items to be loaded. / 待装载的货物项列表
 * @property positions The list of available stowage positions. / 可用配载位置列表
 * @property trailers The list of trailers used for loading. / 用于装载的拖车列表
 * @property stowage The stowage assignment model. / 配载分配模型
 * @property cargosBySource Mapping from source identifier to cargo item indices. / 来源标识到货物项索引的映射
 * @property earlyEnd The index of the last early position for source-early constraints. / 来源早装约束中最后一个早期位置的索引
*/
class Aggregation(
    aircraftModel: AircraftModel,
    stowageMode: StowageMode,
    internal val flight: Flight,
    internal val items: List<Item>,
    internal val positions: List<Position>,
    val trailers: List<Trailer>,
    internal val stowage: Stowage,
    load: Load,
    val cargosBySource: Map<String, List<Int>> = emptyMap(),
    val earlyEnd: Int = 0
){
    companion object {

    /**
     * Combines adjacent positions by loading order into position pairs.
     * 按装载顺序将相邻位置组合为位置对。
     *
     * @param positions The list of positions to combine. / 待组合的位置列表
     * @return The list of adjacent position pairs. / 相邻位置对列表
    */
    private fun combineAdjacentByLoadingOrder(positions: List<Position>): List<PositionPair> {
            TODO("not implemented yet")
        }

    /**
     * Combines positions by loading order into all ordered position pairs.
     * 按装载顺序将位置组合为所有有序位置对。
     *
     * @param positions The list of positions to combine. / 待组合的位置列表
     * @return The list of ordered position pairs. / 有序位置对列表
    */
    private fun combineByLoadingOrder(positions: List<Position>): List<PositionPair> {
            TODO("not implemented yet")
        }
    }

    val sources = items.mapNotNull { it.source }.distinct()
    val destinations = items.mapNotNull {
        if (it.destination != flight.arrival) {
            it.destination
        } else {
            null
        }
    }.distinct()
    val adjacentPositions = combineAdjacentByLoadingOrder(positions)
    val orderedPositions = combineByLoadingOrder(positions)

    val adviceLoading = when (stowageMode) {
        StowageMode.Predistribution -> {
            AdviceLoading(
                aircraftModel = aircraftModel,
                positions = positions,
                load = load
            )
        }

        StowageMode.FullLoad, StowageMode.WeightRecommendation -> {
            null
        }
    }

    val transferAdjacentLoading = TransferAdjacentLoading(
        adjacentPositions = adjacentPositions,
        sources = sources,
        destinations = destinations,
        load = load
    )

    val sequentialLoading = when (stowageMode) {
        StowageMode.Predistribution -> {
            SequentialLoading(
                items = items,
                positions = positions,
                orderedPositions = orderedPositions,
                stowage = stowage
            )
        }

        StowageMode.FullLoad, StowageMode.WeightRecommendation -> {
            null
        }
    }

    val trailerLoading = when (stowageMode) {
        StowageMode.FullLoad -> {
            TrailerLoading(
                items = items,
                positions = positions,
                adjacentPositions = adjacentPositions,
                trailers = trailers,
                stowage = stowage,
                load = load
            )
        }

        StowageMode.Predistribution, StowageMode.WeightRecommendation -> {
            null
        }
    }

    /**
     * Registers loading effectiveness models into the optimization model.
     * 将装车效能模型注册到优化模型中。
     *
     * @param stowageMode The stowage mode determining which sub-models to register. / 决定注册哪些子模型的装载模式
     * @param model The linear meta model to register into. / 要注册到的线性元模型
     * @return The result of the registration operation. / 注册操作的结果
    */
    fun register(
        stowageMode: StowageMode,
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        when (val result = transferAdjacentLoading.register(model)) {
            is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}

            is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Failed(result.error)
                }

                is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Fatal(result.errors)
                }
        }

        if (adviceLoading != null) {
            when (val result = adviceLoading.register(model)) {
                is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}

                is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Failed(result.error)
                }

                is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Fatal(result.errors)
                }
            }
        }

        if (sequentialLoading != null) {
            when (val result = sequentialLoading.register(model)) {
                is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}

                is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Failed(result.error)
                }

                is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Fatal(result.errors)
                }
            }
        }

        if (trailerLoading != null) {
            when (val result = trailerLoading.register(model)) {
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
     * Registers loading effectiveness models for the Benders master problem.
     * 为 Benders 主问题注册装车效能模型。
     *
     * @param model The linear meta model to register into. / 要注册到的线性元模型
     * @return The result of the registration operation. / 注册操作的结果
    */
    fun registerForBendersMP(
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        // Loading effectiveness constraints go into the master problem.
        return register(stowageMode = StowageMode.FullLoad, model = model)
    }

    /**
     * Registers loading effectiveness models for the Benders sub problem (no-op).
     * 为 Benders 子问题注册装车效能模型（空实现）。
     *
     * @param model The linear meta model to register into. / 要注册到的线性元模型
     * @param solution The current solution vector from the master problem. / 来自主问题的当前解向量
     * @return The result of the registration operation. / 注册操作的结果
    */
    fun registerForBendersSP(
        model: AbstractLinearMetaModel<Flt64>,
        solution: List<Flt64>
    ): Try {
        // Loading effectiveness does not contribute to the sub problem.
        return ok
    }

    /**
     * Flushes loading effectiveness for the Benders sub problem (no-op).
     * 刷新 Benders 子问题的装车效能（空实现）。
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

