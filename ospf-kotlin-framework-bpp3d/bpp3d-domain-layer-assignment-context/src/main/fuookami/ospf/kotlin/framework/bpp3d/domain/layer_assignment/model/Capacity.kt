package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.multi_array.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.*

interface Capacity {
    val loadWeight: LinearSymbols1
    val loadVolume: LinearSymbols1
    val loadDepth: LinearSymbols1
    val tailLoadingRate: LinearSymbols1
}

class PreciseLoadCapacity(
    private val bins: List<Bin<BinLayer>>,
    private val layers: List<BinLayer>,
    private val assignment: PreciseAssignment
) : Capacity {
    override lateinit var loadWeight: LinearSymbols1
    override lateinit var loadVolume: LinearSymbols1
    override lateinit var loadDepth: LinearSymbols1
    override lateinit var tailLoadingRate: LinearSymbols1

    fun register(model: MetaModel): Try {
        if (!::loadWeight.isInitialized) {
            loadWeight = LinearSymbols1("load_weight", Shape1(bins.size)) { j, _ ->
                LinearExpressionSymbol(
                    sum(layers.mapIndexed { i, layer ->
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
            loadVolume = LinearSymbols1("load_volume", Shape1(bins.size)) { j, _ ->
                LinearExpressionSymbol(
                    sum(layers.mapIndexed { i, layer ->
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
            loadDepth = LinearSymbols1("load_depth", Shape1(bins.size)) { j, _ ->
                LinearExpressionSymbol(
                    sum(layers.mapIndexed { i, layer ->
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

        if (!::tailLoadingRate.isInitialized) {
            TODO("not implemented yet")
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
