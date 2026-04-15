@file:Suppress("DEPRECATION")

package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model

import fuookami.ospf.kotlin.core.intermediate_model.times
import fuookami.ospf.kotlin.core.intermediate_model.sum
import fuookami.ospf.kotlin.core.intermediate_symbol.LinearExpressionSymbol
import fuookami.ospf.kotlin.core.intermediate_symbol.LinearExpressionSymbols1
import fuookami.ospf.kotlin.core.intermediate_symbol.LinearIntermediateSymbols1
import fuookami.ospf.kotlin.core.intermediate_model.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.intermediate_model.MetaModel
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BinLayer
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Item
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.multiarray.Shape1

interface Load {
    val load: LinearIntermediateSymbols1
    val overLoad: LinearIntermediateSymbols1
    val lessLoad: LinearIntermediateSymbols1

    val overEnabled: Boolean
    val lessEnabled: Boolean
}

abstract class AbstractLoad : Load {
    override lateinit var overLoad: LinearIntermediateSymbols1
    override lateinit var lessLoad: LinearIntermediateSymbols1

    open fun register(model: MetaModel): Try {
        TODO("not implemented yet")
    }
}

class ImpreciseLoad(
    private val items: List<Pair<Item, UInt64>>,
    private val assignment: ImpreciseAssignment,
    override val overEnabled: Boolean = true,
    override val lessEnabled: Boolean = true
) : AbstractLoad() {
    override lateinit var load: LinearExpressionSymbols1

    override fun register(model: MetaModel): Try {
        if (!::load.isInitialized) {
            load = LinearExpressionSymbols1(
                "load",
                Shape1(items.size)
            ) { i, _ ->
                LinearExpressionSymbol(name = "load_$i")
            }
        }
        when (val result = model.add(load)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        return super.register(model)
    }

    suspend fun addColumns(
        iteration: UInt64,
        newLayers: List<BinLayer>,
        model: AbstractLinearMetaModel
    ): Ret<List<BinLayer>> {
        assert(newLayers.isNotEmpty())

        val xi = assignment.x[iteration.toInt()]

        for ((i, item) in items.withIndex()) {
            val thisLayers = newLayers.filter { it.amount(item.first) != UInt64.zero }
            if (thisLayers.isNotEmpty()) {
                val thisLoad = load[i]
                thisLoad.flush()
                thisLoad.asMutable() += sum(thisLayers.map { it.amount(item.first) * xi[it] })
            }
        }

        return Ok(newLayers)
    }
}

class PreciseLoad(
    private val items: List<Pair<Item, UInt64>>,
    private val layers: List<BinLayer>,
    private val assignment: PreciseAssignment,
    override val overEnabled: Boolean = false,
    override val lessEnabled: Boolean = true
) : AbstractLoad() {
    override lateinit var load: LinearIntermediateSymbols1

    override fun register(model: MetaModel): Try {
        if (!::load.isInitialized) {
            load = LinearExpressionSymbols1(
                "load",
                Shape1(items.size)
            ) { i, _ ->
                LinearExpressionSymbol(
                    sum(layers.map { it.amount(items[i].first) * assignment.x[it] }),
                    name = "load_$i"
                )
            }
        }
        when (val result = model.add(load)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        return super.register(model)
    }
}



