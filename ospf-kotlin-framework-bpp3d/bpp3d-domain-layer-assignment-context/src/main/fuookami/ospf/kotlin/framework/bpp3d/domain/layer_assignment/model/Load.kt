package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model

import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.sum
import fuookami.ospf.kotlin.math.symbol.polynomial.plusAssign
import fuookami.ospf.kotlin.core.intermediate_symbol.LinearExpressionSymbol
import fuookami.ospf.kotlin.core.intermediate_symbol.LinearExpressionSymbols1
import fuookami.ospf.kotlin.core.intermediate_symbol.LinearIntermediateSymbols1
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BinLayer
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Item
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.multiarray.Shape1
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.model.mechanism.MetaModel

interface Load {
    val load: LinearIntermediateSymbols1<Flt64>
    val overLoad: LinearIntermediateSymbols1<Flt64>
    val lessLoad: LinearIntermediateSymbols1<Flt64>

    val overEnabled: Boolean
    val lessEnabled: Boolean
}

abstract class AbstractLoad : Load {
    override lateinit var overLoad: LinearIntermediateSymbols1<Flt64>
    override lateinit var lessLoad: LinearIntermediateSymbols1<Flt64>

    open fun register(model: MetaModel<Flt64>): Try {
        TODO("not implemented yet")
    }
}

class ImpreciseLoad(
    private val items: List<Pair<Item, UInt64>>,
    private val assignment: ImpreciseAssignment,
    override val overEnabled: Boolean = true,
    override val lessEnabled: Boolean = true
) : AbstractLoad() {
    override lateinit var load: LinearExpressionSymbols1<Flt64>

    override fun register(model: MetaModel<Flt64>): Try {
        if (!::load.isInitialized) {
            load = LinearExpressionSymbols1<Flt64>(
                "load",
                Shape1(items.size)
            ) { i, _ ->
                LinearExpressionSymbol(Flt64, name = "load_$i")
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
        model: AbstractLinearMetaModel<Flt64>
    ): Ret<List<BinLayer>> {
        assert(newLayers.isNotEmpty())

        val xi = assignment.x[iteration.toInt()]

        for ((i, item) in items.withIndex()) {
            val thisLayers = newLayers.filter { it.amount(item.first) != UInt64.zero }
            if (thisLayers.isNotEmpty()) {
                val thisLoad = load[i]
                thisLoad.flush()
                thisLoad.asMutable() += sum(thisLayers.map {
                    LinearMonomial(it.amount(item.first).toFlt64(), xi[it])
                })
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
    override lateinit var load: LinearIntermediateSymbols1<Flt64>

    override fun register(model: MetaModel<Flt64>): Try {
        if (!::load.isInitialized) {
            load = LinearIntermediateSymbols1<Flt64>(
                "load",
                Shape1(items.size)
            ) { i, _ ->
                LinearExpressionSymbol(
                    sum(layers.map {
                        LinearMonomial(it.amount(items[i].first).toFlt64(), assignment.x[it])
                    }),
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


