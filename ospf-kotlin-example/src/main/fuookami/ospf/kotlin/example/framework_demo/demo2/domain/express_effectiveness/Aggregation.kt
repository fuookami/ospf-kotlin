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
 * 基于装载模式聚合用于项目优先级排序的快递效能模型。Aggregates express effectiveness models for item priority ordering based on stowage mode.
 *
 * @property stowageMode 参数。
 * @property internal val items 参数。
 * @property internal val positions 参数。
 * @property internal val stowage 参数。
 * @property mustShipIndices 参数。
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

    fun registerForBendersMP(
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        return register(stowageMode = StowageMode.FullLoad, model = model)
    }

    fun registerForBendersSP(
        model: AbstractLinearMetaModel<Flt64>,
        solution: List<Flt64>
    ): Try {
        return ok
    }

    private fun flushForBendersSP(
        model: AbstractLinearMetaModel<Flt64>,
        solution: List<Flt64>
    ): Try {
        return ok
    }
}
