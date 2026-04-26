package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.express_effectiveness


import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.Position
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.PositionPair
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.express_effectiveness.model.*

class Aggregation(
    stowageMode: StowageMode,
    internal val items: List<Item>,
    internal val positions: List<Position>,
    internal val stowage: Stowage
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
        model: AbstractLinearMetaModelF64
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

    fun registerForBendersMP(
        model: AbstractLinearMetaModelF64
    ): Try {
        TODO("not implemented yet")
    }

    fun registerForBendersSP(
        model: AbstractLinearMetaModelF64,
        solution: List<Flt64>
    ): Try {
        TODO("not implemented yet")
    }

    private fun flushForBendersSP(
        model: AbstractLinearMetaModelF64,
        solution: List<Flt64>
    ): Try {
        TODO("not implemented yet")
    }
}













