package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.multi_array.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.*

class ImpreciseAssignment(
    private val items: Map<Item, UInt64>,
    private val aggregation: LayerAggregation
) {
    val layers: List<BinLayer> by aggregation::layers
    val lastIterationLayers: List<BinLayer> by aggregation::lastIterationLayers

    private val _x = ArrayList<UIntVariable1>()
    val x: List<UIntVariable1> by ::_x

    fun register(model: MetaModel): Try {
        return ok
    }

    suspend fun addColumns(
        iteration: UInt64,
        newLayers: List<BinLayer>,
        model: AbstractLinearMetaModel
    ): Ret<List<BinLayer>> {
        val unduplicatedLayers = aggregation.addColumns(newLayers)

        val xi = UIntVariable1("x_$iteration", Shape1(unduplicatedLayers.size))
        for (layer in unduplicatedLayers) {
            xi[layer].name = "${xi.name}_${layer.index}"
            val maxAmount = items.minOfOrNull {
                val amount = layer.amount(it.key)
                if (amount == UInt64.zero) {
                    UInt64.maximum
                } else {
                    it.value / amount + UInt64.one
                }
            }
            maxAmount?.let { xi[layer].range.leq(it) }
        }
        _x.add(xi)
        when (val result = model.add(xi)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        return Ok(unduplicatedLayers)
    }
}

class PreciseAssignment(
    private val items: Map<Item, UInt64>,
    val layers: List<BinLayer>
) {
    lateinit var x: UIntVariable1
}
