@file:Suppress("DEPRECATION")

package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model

import fuookami.ospf.kotlin.core.intermediate_symbol.LinearExpressionSymbol
import fuookami.ospf.kotlin.core.intermediate_symbol.LinearIntermediateSymbols1
import fuookami.ospf.kotlin.core.intermediate_symbol.LinearIntermediateSymbols2
import fuookami.ospf.kotlin.core.intermediate_symbol.function.BinaryzationFunction
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModelF64
import fuookami.ospf.kotlin.core.model.mechanism.MetaModelF64
import fuookami.ospf.kotlin.core.variable.BinVariable1
import fuookami.ospf.kotlin.core.variable.UIntVariable1
import fuookami.ospf.kotlin.core.variable.UIntVariable2
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Bin
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BinLayer
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Item
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.sum
import fuookami.ospf.kotlin.math.symbol.polynomial.plusAssign
import fuookami.ospf.kotlin.multiarray.Shape1
import fuookami.ospf.kotlin.multiarray.Shape2
import fuookami.ospf.kotlin.multiarray._a

class ImpreciseAssignment(
    private val items: Map<Item, UInt64>,
    private val aggregation: LayerAggregation
) {
    val layers: List<BinLayer> by aggregation::layers
    val lastIterationLayers: List<BinLayer> by aggregation::lastIterationLayers

    private val _x = ArrayList<UIntVariable1>()
    val x: List<UIntVariable1> by ::_x

    lateinit var volume: LinearExpressionSymbol

    fun register(model: MetaModelF64): Try {
        if (!::volume.isInitialized) {
            volume = LinearExpressionSymbol(name = "volume")
        }
        when (val result = model.add(volume)) {
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

    suspend fun addColumns(
        iteration: UInt64,
        newLayers: List<BinLayer>,
        model: AbstractLinearMetaModelF64
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

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        if (unduplicatedLayers.isEmpty()) {
            return Ok(emptyList())
        }

        volume.flush()
        volume.asMutable() += sum(unduplicatedLayers.map {
            LinearMonomial(it.volume.toFlt64(), xi[it])
        })

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

    fun register(model: MetaModelF64): Try {
        if (!::x.isInitialized) {
            x = UIntVariable2(
                "x",
                Shape2(bins.size, layers.size)
            )
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

                        is Fatal -> {
                            return Fatal(result.errors)
                        }
                    }
                }
            }
        }

        if (!::u.isInitialized) {
            u = LinearIntermediateSymbols2(
                name = "u",
                shape = Shape2(bins.size, layers.size)
            ) { _, v ->
                BinaryzationFunction(
                    input = LinearMonomial(Flt64.one, x[v[0], v[1]]).toLinearPolynomial(),
                    name = "u_$v",
                )
            }
        }
        when (val result = model.add(u)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        if (!::v.isInitialized) {
            v = LinearIntermediateSymbols1(
                name = "v",
                shape = Shape1(bins.size)
            ) { i, _ ->
                BinaryzationFunction(
                    input = sum(x[i, _a].map { LinearMonomial(Flt64.one, it) }),
                    name = "v_$i",
                )
            }
        }
        when (val result = model.add(v)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        if (!::tail.isInitialized) {
            tail = BinVariable1(
                "tail",
                Shape1(bins.size)
            )
        }
        when (val result = model.add(tail)) {
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
