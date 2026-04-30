package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.div
import fuookami.ospf.kotlin.math.symbol.polynomial.sum
import fuookami.ospf.kotlin.core.intermediate_symbol.LinearExpressionSymbol
import fuookami.ospf.kotlin.core.intermediate_symbol.LinearIntermediateSymbols1
import fuookami.ospf.kotlin.core.intermediate_symbol.function.MaskingFunction
import fuookami.ospf.kotlin.core.model.mechanism.MetaModelFlt64
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Bin
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BinLayer
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.multiarray.Shape1

interface Capacity {
    val loadWeight: LinearIntermediateSymbols1<Flt64>
    val loadVolume: LinearIntermediateSymbols1<Flt64>
    val loadDepth: LinearIntermediateSymbols1<Flt64>

    val loadingRate: LinearIntermediateSymbols1<Flt64>
    val tailLoadingRate: LinearIntermediateSymbols1<Flt64>
}

class PreciseLoadCapacity(
    private val bins: List<Bin<BinLayer>>,
    private val layers: List<BinLayer>,
    private val assignment: PreciseAssignment
) : Capacity {
    override lateinit var loadWeight: LinearIntermediateSymbols1<Flt64>
    override lateinit var loadVolume: LinearIntermediateSymbols1<Flt64>
    override lateinit var loadDepth: LinearIntermediateSymbols1<Flt64>

    override lateinit var loadingRate: LinearIntermediateSymbols1<Flt64>
    override lateinit var tailLoadingRate: LinearIntermediateSymbols1<Flt64>

    fun register(model: MetaModelFlt64): Try {
        if (!::loadWeight.isInitialized) {
            loadWeight = LinearIntermediateSymbols1<Flt64>(
                name = "load_weight",
                shape = Shape1(bins.size)
            ) { i, _ ->
                LinearExpressionSymbol(
                    polynomial = sum(layers.mapIndexed { j, layer ->
                        LinearPolynomial(listOf(LinearMonomial(layer.weight.toFlt64(), assignment.x[i, j])), Flt64.zero)
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
            loadVolume = LinearIntermediateSymbols1<Flt64>(
                name = "load_volume",
                shape = Shape1(bins.size)
            ) { i, _ ->
                LinearExpressionSymbol(
                    polynomial = sum(layers.mapIndexed { j, layer ->
                        LinearPolynomial(listOf(LinearMonomial(layer.volume.toFlt64(), assignment.x[i, j])), Flt64.zero)
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
            loadDepth = LinearIntermediateSymbols1<Flt64>(
                name = "load_depth",
                shape = Shape1(bins.size)
            ) { i, _ ->
                LinearExpressionSymbol(
                    polynomial = sum(layers.mapIndexed { j, layer ->
                        LinearPolynomial(listOf(LinearMonomial(layer.depth.toFlt64(), assignment.x[i, j])), Flt64.zero)
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
            loadingRate = LinearIntermediateSymbols1<Flt64>(
                name = "loading_rate",
                shape = Shape1(bins.size)
            ) { i, _ ->
                LinearExpressionSymbol(
                    polynomial = sum(layers.mapIndexed { j, layer ->
                        LinearPolynomial(listOf(LinearMonomial(layer.volume.toFlt64(), assignment.x[i, j])), Flt64.zero)
                    }) / bins[i].volume.toFlt64(),
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
            tailLoadingRate = LinearIntermediateSymbols1<Flt64>(
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
