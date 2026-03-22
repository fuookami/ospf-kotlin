package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model

import fuookami.ospf.kotlin.core.frontend.expression.monomial.times
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.sum
import fuookami.ospf.kotlin.core.frontend.expression.symbol.LinearExpressionSymbol
import fuookami.ospf.kotlin.core.frontend.expression.symbol.LinearIntermediateSymbols1
import fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function.MaskingFunction
import fuookami.ospf.kotlin.core.frontend.model.mechanism.MetaModel
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Bin
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BinLayer
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.multi_array.Shape1

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
            loadWeight = LinearIntermediateSymbols1(
                name = "load_weight",
                shape = Shape1(bins.size)
            ) { i, _ ->
                LinearExpressionSymbol(
                    polynomial = sum(layers.mapIndexed { j, layer ->
                        layer.weight * assignment.x[i, j]
                    }),
                    name = "load_weight_${i}"
                )
            }
        }
        when (val result = model.add(loadWeight)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        if (!::loadVolume.isInitialized) {
            loadVolume = LinearIntermediateSymbols1(
                name = "load_volume",
                shape = Shape1(bins.size)
            ) { i, _ ->
                LinearExpressionSymbol(
                    polynomial = sum(layers.mapIndexed { j, layer ->
                        layer.volume * assignment.x[i, j]
                    }),
                    name = "load_volume_${i}"
                )
            }
        }
        when (val result = model.add(loadVolume)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        if (!::loadDepth.isInitialized) {
            loadDepth = LinearIntermediateSymbols1(
                name = "load_depth",
                shape = Shape1(bins.size)
            ) { i, _ ->
                LinearExpressionSymbol(
                    polynomial = sum(layers.mapIndexed { j, layer ->
                        layer.depth * assignment.x[i, j]
                    }),
                    name = "load_depth_${i}"
                )
            }
        }
        when (val result = model.add(loadDepth)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        if (!::loadingRate.isInitialized) {
            loadingRate = LinearIntermediateSymbols1(
                name = "loading_rate",
                shape = Shape1(bins.size)
            ) { i, _ ->
                LinearExpressionSymbol(
                    polynomial = sum(layers.mapIndexed { j, layer ->
                        layer.volume * assignment.x[i, j]
                    }) / bins[i].volume,
                    name = "loading_rate_${i}"
                )
            }
        }
        when (val result = model.add(loadingRate)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        if (!::tailLoadingRate.isInitialized) {
            tailLoadingRate = LinearIntermediateSymbols1(
                name = "tail_loading_rate",
                shape = Shape1(bins.size)
            ) { i, _ ->
                MaskingFunction(
                    x = loadingRate[i],
                    mask = assignment.tail[i],
                    name = "tail_loading_rate_${i}"
                )
            }
        }
        when (val result = model.add(tailLoadingRate)) {
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
