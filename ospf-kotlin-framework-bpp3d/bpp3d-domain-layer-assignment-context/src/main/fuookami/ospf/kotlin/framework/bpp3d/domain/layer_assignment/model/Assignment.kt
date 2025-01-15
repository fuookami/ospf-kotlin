package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.multi_array.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.*

class ImpreciseAssignment(
    private val items: Map<Item, UInt64>,
    private val aggregation: LayerAggregation
) {
    val layers: List<BinLayer> by aggregation::layers
    val lastIterationLayers: List<BinLayer> by aggregation::lastIterationLayers

    private val _x = ArrayList<UIntVariable1>()
    val x: List<UIntVariable1> by ::_x

    lateinit var volume: LinearExpressionSymbol

    fun register(model: MetaModel): Try {
        if (!::volume.isInitialized) {
            volume = LinearExpressionSymbol(LinearPolynomial(), "volume")
        }
        when (val result = model.add(volume)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

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

        if (unduplicatedLayers.isEmpty()) {
            return Ok(emptyList())
        }

        volume.flush()
        volume.asMutable() += sum(unduplicatedLayers.map { it.volume * xi[it] })

        return Ok(unduplicatedLayers)
    }
}

class PreciseAssignment(
    private val bins: List<Bin<BinLayer>>,
    private val layers: List<BinLayer>
) {
    lateinit var x: UIntVariable2

    lateinit var u: LinearIntermediateSymbols2
    lateinit var v: LinearIntermediateSymbols1
    lateinit var tail: BinVariable1

    fun register(model: MetaModel): Try {
        if (!::x.isInitialized) {
            x = UIntVariable2("x", Shape2(bins.size, layers.size))
            for ((i, bin) in bins.withIndex()) {
                for ((j, layer) in layers.withIndex()) {
                    if (layer.bin != bin.shape || !bin.enabled(layer)) {
                        x[i, j].range.eq(UInt64.zero)
                    } else {
                        x[i, j].range.leq((bin.depth / layer.depth).ceil().toUInt64())
                    }
                }
            }
        }
        for ((i, bin) in bins.withIndex()) {
            for ((j, layer) in layers.withIndex()) {
                if (layer.bin == bin.shape && bin.enabled(layer)) {
                    when (val result = model.add(x[i, j])) {
                        is Ok -> {}

                        is Failed -> {
                            return Failed(result.error)
                        }
                    }
                }
            }
        }

        if (!::u.isInitialized) {
            u = LinearIntermediateSymbols2("u", Shape2(bins.size, layers.size)) { _, v ->
                BinaryzationFunction(
                    x = LinearPolynomial(x[v[0], v[1]]),
                    name = "u_$v",
                )
            }
        }
        when (val result = model.add(u)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        if (!::v.isInitialized) {
            v = LinearIntermediateSymbols1("v", Shape1(bins.size)) { i, _ ->
                BinaryzationFunction(
                    x = sum(x[i, _a]),
                    name = "v_$i",
                )
            }
        }
        when (val result = model.add(v)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        if (!::tail.isInitialized) {
            tail = BinVariable1("tail", Shape1(bins.size))
        }
        when (val result = model.add(tail)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        return ok
    }
}
