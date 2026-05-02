package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.soft_security


import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
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
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.soft_security.model.*

class Aggregation(
    internal val aircraftModel: AircraftModel,
    internal val mainDeck: Deck,
    internal val items: List<Item>,
    internal val positions: List<Position>,
    internal val stowage: Stowage,
    internal val load: Load,
    internal val ballast: Ballast?
) {
    val divideEmptyLoading = DivideEmptyLoading(
        positions = positions,
        load = load
    )

    fun register(
        stowageMode: StowageMode,
        model: AbstractLinearMetaModelFlt64
    ): Try {
        when (val result = divideEmptyLoading.register(model)) {
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

    fun registerForBendersMP(
        model: AbstractLinearMetaModelFlt64
    ): Try {
        // Soft security constraints go into the master problem.
        return register(stowageMode = StowageMode.FullLoad, model = model)
    }

    fun registerForBendersSP(
        model: AbstractLinearMetaModelFlt64,
        solution: List<Flt64>
    ): Try {
        // Soft security does not contribute to the sub problem.
        return ok
    }

    private fun flushForBendersSP(
        model: AbstractLinearMetaModelFlt64,
        solution: List<Flt64>
    ): Try {
        return ok
    }
}













