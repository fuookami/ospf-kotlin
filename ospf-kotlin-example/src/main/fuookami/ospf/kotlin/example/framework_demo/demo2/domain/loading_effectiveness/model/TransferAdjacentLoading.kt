package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.loading_effectiveness.model


import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.multiarray.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.math.symbol.adapter.flt64.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.intermediate_symbol.*
import fuookami.ospf.kotlin.core.intermediate_symbol.function.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64

private val flt64Converter = object : IntoValue<Flt64> {
        override fun intoValue(value: Flt64) = value
        override val zero get() = Flt64.zero
        override val one get() = Flt64.one
        override fun fromValue(value: Flt64) = value
    }

class TransferAdjacentLoading(
    private val adjacentPositions: List<PositionPair>,
    private val sources: List<FlightNo>,
    private val destinations: List<IATA>,
    private val load: Load
) {
    lateinit var sameSourceAdjacent: LinearIntermediateSymbols2<Flt64>
    lateinit var sameDestinationAdjacent: LinearIntermediateSymbols2<Flt64>

    fun register(
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        if (!::sameSourceAdjacent.isInitialized) {
            sameSourceAdjacent = LinearIntermediateSymbols2<Flt64>("same_source_adjacent", Shape2(sources.size, adjacentPositions.size)) { _, v ->
                val source = sources[v[0]]
                val position1 = adjacentPositions[v[1]].first
                val position2 = adjacentPositions[v[1]].second

                val loadAmount1 = load.loadAmountOf(position1) { item ->
                    item.source == source
                }
                val loadAmount2 = load.loadAmountOf(position2) { item ->
                    item.source == source
                }

                if (position1.status.stowageNeeded || position1.status.adjustmentNeeded) {
                    LinearFunctionSymbolAdapter(
                        delegate = IfFunction(
                            condition = loadAmount1 + loadAmount2 - Flt64.two,
                            converter = flt64Converter,
                            name = "same_source_adjacent_${source}_${position1}_${position2}",
                        ),
                        converter = flt64Converter
                    )
                } else if (loadAmount1.range.fixedValue?.let { it eq Flt64.zero } == true) {
                    LinearExpressionSymbol(
                        Flt64.zero,
                        name = "same_source_adjacent_${source}_${position1}_${position2}"
                    )
                } else {
                    LinearExpressionSymbol(
                        LinearPolynomial(loadAmount1),
                        name = "same_source_adjacent_${source}_${position1}_${position2}"
                    )
                }
            }
        }
        when (val result = model.add(sameSourceAdjacent)) {
            is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}

            is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Failed(result.error)
                }

                is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Fatal(result.errors)
                }
        }

        if (!::sameDestinationAdjacent.isInitialized) {
            sameDestinationAdjacent = LinearIntermediateSymbols2<Flt64>("same_destination_adjacent", Shape2(destinations.size, adjacentPositions.size)) { _, v ->
                val destination = destinations[v[0]]
                val position1 = adjacentPositions[v[1]].first
                val position2 = adjacentPositions[v[1]].second

                val loadAmount1 = load.loadAmountOf(position1) { item ->
                    item.destination == destination
                }
                val loadAmount2 = load.loadAmountOf(position2) { item ->
                    item.destination == destination
                }

                if (position1.status.stowageNeeded || position1.status.adjustmentNeeded) {
                    LinearFunctionSymbolAdapter(
                        delegate = IfFunction(
                            condition = loadAmount1 + loadAmount2 - Flt64.two,
                            converter = flt64Converter,
                            name = "same_destination_adjacent_${destination}_${position1}_${position2}",
                        ),
                        converter = flt64Converter
                    )
                } else if (loadAmount1.range.fixedValue?.let { it eq Flt64.zero } == true) {
                    LinearExpressionSymbol(
                        Flt64.zero,
                        name = "same_destination_adjacent_${destination}_${position1}_${position2}"
                    )
                } else {
                    LinearExpressionSymbol(
                        LinearPolynomial(loadAmount1),
                        name = "same_destination_adjacent_${destination}_${position1}_${position2}"
                    )
                }
            }
        }
        when (val result = model.add(sameDestinationAdjacent)) {
            is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}

            is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Failed(result.error)
                }

                is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Fatal(result.errors)
                }
        }

        return ok
    }
}













