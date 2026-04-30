package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.loading_effectiveness.model


import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.multiarray.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.intermediate_symbol.*
import fuookami.ospf.kotlin.core.intermediate_symbol.function.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.Position

class TrailerLoading(
    private val items: List<Item>,
    private val positions: List<Position>,
    private val trailers: List<Trailer>,
    internal val orderedItemsInTrailers: List<ItemPair>,
    private val adjacentPositions: List<PositionPair>,
    internal val orderedTrailers: List<Pair<Trailer, Trailer>>,
    private val stowage: Stowage,
    private val load: Load
) {
    companion object {
        operator fun invoke(
            items: List<Item>,
            positions: List<Position>,
            trailers: List<Trailer>,
            adjacentPositions: List<PositionPair>,
            stowage: Stowage,
            load: Load
        ): TrailerLoading {
            TODO("not implemented yet")
        }
    }

    lateinit var trailerChange: LinearIntermediateSymbols2Flt64
    lateinit var trailerCircling: LinearIntermediateSymbols2Flt64

    fun register(
        model: AbstractLinearMetaModelFlt64
    ): Try {
        if (!::trailerChange.isInitialized) {
            trailerChange = LinearIntermediateSymbols2Flt64("trailer_change", Shape2(orderedTrailers.size, adjacentPositions.size)) { _, v ->
                val (trailer1, trailer2) = orderedTrailers[v[0]]
                val (position1, position2) = adjacentPositions[v[1]]

                val loadAmount1 = load.loadAmountOf(position1) { item ->
                    item in trailer2.items
                }
                val loadAmount2 = load.loadAmountOf(position2) { item ->
                    item in trailer1.items
                }

                IfFunction(
                    (loadAmount1 + loadAmount2) geq Flt64.two,
                    name = "trailer_change_${trailer1}_${trailer2}_${position1}_${position2}"
                )
            }
        }
        when (val result = model.add(trailerChange)) {
            is Ok -> {}

            is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
        }

        if (!::trailerCircling.isInitialized) {
            trailerCircling = LinearIntermediateSymbols2Flt64("trailer_circling", Shape2(orderedItemsInTrailers.size, adjacentPositions.size)) { _, v ->
                val (item1, item2) = orderedItemsInTrailers[v[0]]
                val i1 = items.indexOf(item1)
                val i2 = items.indexOf(item2)
                val (position1, position2) = adjacentPositions[v[1]]
                val j1 = positions.indexOf(position1)
                val j2 = positions.indexOf(position2)

                if (Stowage.stowageNeeded(item2, position1) && Stowage.stowageNeeded(item1, position2)) {
                    IfFunction(
                        (stowage.stowage[i2, j1] + stowage.stowage[i1, j2]) geq Flt64.two,
                        name = "trailer_circling_${item1}_${item2}_${position1}_${position2}"
                    )
                } else {
                    LinearExpressionSymbol(
                        Flt64.zero,
                        name = "trailer_circling_${item1}_${item2}_${position1}_${position2}"
                    )
                }
            }
        }
        when (val result = model.add(trailerCircling)) {
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
}













