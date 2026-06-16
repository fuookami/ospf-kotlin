package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.loading_effectiveness

import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.loading_effectiveness.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.Position
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.PositionPair
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.*

import fuookami.ospf.kotlin.utils.functional.*

import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*

import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.token.*

class Aggregation(
    aircraftModel: AircraftModel,
    stowageMode: StowageMode,
    internal val flight: Flight,
    internal val items: List<Item>,
    internal val positions: List<Position>,
    val trailers: List<Trailer>,
    internal val stowage: Stowage,
    load: Load
){
    companion object {
        private fun combineAdjacentByLoadingOrder(positions: List<Position>): List<PositionPair> {
            TODO("not implemented yet")
        }

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

    fun registerForBendersMP(
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        // Loading effectiveness constraints go into the master problem.
        return register(stowageMode = StowageMode.FullLoad, model = model)
    }

    fun registerForBendersSP(
        model: AbstractLinearMetaModel<Flt64>,
        solution: List<Flt64>
    ): Try {
        // Loading effectiveness does not contribute to the sub problem.
        return ok
    }

    private fun flushForBendersSP(
        model: AbstractLinearMetaModel<Flt64>,
        solution: List<Flt64>
    ): Try {
        return ok
    }
}

