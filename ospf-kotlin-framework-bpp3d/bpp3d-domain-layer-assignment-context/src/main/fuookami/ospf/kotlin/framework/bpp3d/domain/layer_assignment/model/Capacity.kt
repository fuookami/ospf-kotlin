package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.multi_array.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.*

interface Capacity {
    val loadWeight: LinearIntermediateSymbols1
    val loadVolume: LinearIntermediateSymbols1
    val loadDepth: LinearIntermediateSymbols1

    val loadingRate: LinearIntermediateSymbols1
    val tailLoadingRate: LinearIntermediateSymbols1
}

class PreciseLoadCapacity(
    private val bins: List<Bin<BinLayer>>,
    private val layers: List<BinLayer>,
    private val assignment: PreciseAssignment
) : Capacity {
    override lateinit var loadWeight: LinearIntermediateSymbols1
    override lateinit var loadVolume: LinearIntermediateSymbols1
    override lateinit var loadDepth: LinearIntermediateSymbols1

    override lateinit var loadingRate: LinearIntermediateSymbols1
    override lateinit var tailLoadingRate: LinearIntermediateSymbols1

    fun register(model: MetaModel): Try {
        if (!::loadWeight.isInitialized) {
            loadWeight = LinearIntermediateSymbols1("load_weight", Shape1(bins.size)) { i, _ ->
                LinearExpressionSymbol(
                    sum(layers.mapIndexed { j, layer ->
                        layer.weight * assignment.x[i, j]
                    })
                )
            }
        }
        when (val result = model.add(loadWeight)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        if (!::loadVolume.isInitialized) {
            loadVolume = LinearIntermediateSymbols1("load_volume", Shape1(bins.size)) { i, _ ->
                LinearExpressionSymbol(
                    sum(layers.mapIndexed { j, layer ->
                        layer.volume * assignment.x[i, j]
                    })
                )
            }
        }
        when (val result = model.add(loadVolume)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        if (!::loadDepth.isInitialized) {
            loadDepth = LinearIntermediateSymbols1("load_depth", Shape1(bins.size)) { i, _ ->
                LinearExpressionSymbol(
                    sum(layers.mapIndexed { j, layer ->
                        layer.depth * assignment.x[i, j]
                    })
                )
            }
        }
        when (val result = model.add(loadDepth)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        if (!::loadingRate.isInitialized) {
            loadingRate = LinearIntermediateSymbols1("loading_rate", Shape1(bins.size)) { i, _ ->
                LinearExpressionSymbol(
                    sum(layers.mapIndexed { j, layer ->
                        layer.volume * assignment.x[i, j]
                    }) / bins[i].volume
                )
            }
        }
        when (val result = model.add(loadDepth)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        if (!::tailLoadingRate.isInitialized) {
            tailLoadingRate = LinearIntermediateSymbols1("tail_loading_rate", Shape1(bins.size)) { i, _ ->
                SemiRealFunction(
                    x = LinearPolynomial(loadingRate[i]),
                    flag = assignment.tail[i],
                    name = "tail_loading_rate_${i}"
                )
            }
        }
        when (val result = model.add(tailLoadingRate)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        return ok
    }
}
