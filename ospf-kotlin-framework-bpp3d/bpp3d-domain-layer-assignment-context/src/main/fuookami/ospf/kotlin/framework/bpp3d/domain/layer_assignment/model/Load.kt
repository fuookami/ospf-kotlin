package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model

import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.sum
import fuookami.ospf.kotlin.math.symbol.polynomial.plusAssign
import fuookami.ospf.kotlin.core.intermediate_symbol.LinearExpressionSymbol
import fuookami.ospf.kotlin.core.intermediate_symbol.LinearExpressionSymbols1
import fuookami.ospf.kotlin.core.intermediate_symbol.LinearIntermediateSymbols1
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BinLayer
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Bpp3dDemandKey
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Bpp3dDemandMode
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Bpp3dDemandValue
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Item
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.noWeightDemandValue
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.statistics
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.QuantityFlt64
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.neq
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.toFlt64
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.multiarray.Shape1
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.model.mechanism.MetaModel

interface Bpp3dSolverValueAdapter {
    fun amountToSolver(value: UInt64): Flt64
    fun amountRangeToSolver(value: ValueRange<UInt64>): ValueRange<Flt64> = value.toFlt64()
    fun lengthToSolver(value: QuantityFlt64): Flt64
    fun areaToSolver(value: QuantityFlt64): Flt64
    fun volumeToSolver(value: QuantityFlt64): Flt64
    fun depthToSolver(value: QuantityFlt64): Flt64 = lengthToSolver(value)
    fun weightToSolver(value: QuantityFlt64): Flt64

    fun toSolver(value: Bpp3dDemandValue): Flt64 {
        return when (value) {
            is Bpp3dDemandValue.Amount -> amountToSolver(value.value)
            is Bpp3dDemandValue.Weight -> weightToSolver(value.value)
        }
    }
}

typealias Bpp3dDemandValueAdapter = Bpp3dSolverValueAdapter

data object DefaultBpp3dDemandValueAdapter : Bpp3dSolverValueAdapter {
    override fun amountToSolver(value: UInt64): Flt64 = value.toFlt64()
    override fun lengthToSolver(value: QuantityFlt64): Flt64 = value.toFlt64()
    override fun areaToSolver(value: QuantityFlt64): Flt64 = value.toFlt64()
    override fun volumeToSolver(value: QuantityFlt64): Flt64 = value.toFlt64()
    override fun weightToSolver(value: QuantityFlt64): Flt64 = value.toFlt64()
}

val DefaultBpp3dSolverValueAdapter: Bpp3dSolverValueAdapter = DefaultBpp3dDemandValueAdapter

data class Bpp3dDemandEntry(
    val mode: Bpp3dDemandMode,
    val key: Bpp3dDemandKey,
    val demand: Flt64,
    val demandRange: ValueRange<Flt64>
)

private fun defaultDemandValue(mode: Bpp3dDemandMode): Bpp3dDemandValue {
    return when (mode) {
        is Bpp3dDemandMode.ItemAmount -> Bpp3dDemandValue.Amount(UInt64.zero)
        is Bpp3dDemandMode.ItemMaterialAmount -> Bpp3dDemandValue.Amount(UInt64.zero)
        is Bpp3dDemandMode.ItemMaterialWeight -> noWeightDemandValue()
    }
}

fun demandEntriesFromItems(
    items: List<Pair<Item, UInt64>>,
    demandValueAdapter: Bpp3dDemandValueAdapter = DefaultBpp3dDemandValueAdapter
): List<Bpp3dDemandEntry> {
    return items.map { (item, demand) ->
        val demandValue = demandValueAdapter.amountToSolver(demand)
        Bpp3dDemandEntry(
            mode = Bpp3dDemandMode.ItemAmount,
            key = Bpp3dDemandKey.Item(item),
            demand = demandValue,
            demandRange = ValueRange(demandValue, demandValue).value!!
        )
    }
}

fun demandEntriesFromItemRanges(
    items: List<Triple<Item, UInt64, ValueRange<UInt64>>>,
    demandValueAdapter: Bpp3dDemandValueAdapter = DefaultBpp3dDemandValueAdapter
): List<Bpp3dDemandEntry> {
    return items.map { (item, demand, demandRange) ->
        Bpp3dDemandEntry(
            mode = Bpp3dDemandMode.ItemAmount,
            key = Bpp3dDemandKey.Item(item),
            demand = demandValueAdapter.amountToSolver(demand),
            demandRange = demandValueAdapter.amountRangeToSolver(demandRange)
        )
    }
}

interface Load {
    val demandEntries: List<Bpp3dDemandEntry>
    val demandValueAdapter: Bpp3dDemandValueAdapter

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
        if (overEnabled && !::overLoad.isInitialized) {
            overLoad = LinearIntermediateSymbols1<Flt64>(
                "over_load",
                Shape1(demandEntries.size)
            ) { i, _ ->
                LinearExpressionSymbol(
                    load[i].toLinearPolynomial(),
                    name = "over_load_$i"
                )
            }
        }
        if (lessEnabled && !::lessLoad.isInitialized) {
            lessLoad = LinearIntermediateSymbols1<Flt64>(
                "less_load",
                Shape1(demandEntries.size)
            ) { i, _ ->
                LinearExpressionSymbol(
                    load[i].toLinearPolynomial(),
                    name = "less_load_$i"
                )
            }
        }

        if (overEnabled) {
            when (val result = model.add(overLoad)) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }

        if (lessEnabled) {
            when (val result = model.add(lessLoad)) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }

        return ok
    }

    protected fun loadCoefficient(
        layer: BinLayer,
        demand: Bpp3dDemandEntry
    ): Flt64 {
        val value = layer.statistics(demand.mode)[demand.key] ?: defaultDemandValue(demand.mode)
        return demandValueAdapter.toSolver(value)
    }
}

class ImpreciseLoad(
    override val demandEntries: List<Bpp3dDemandEntry>,
    private val assignment: ImpreciseAssignment,
    override val overEnabled: Boolean = true,
    override val lessEnabled: Boolean = true,
    override val demandValueAdapter: Bpp3dDemandValueAdapter = DefaultBpp3dDemandValueAdapter
) : AbstractLoad() {
    companion object {
        fun fromItems(
            items: List<Pair<Item, UInt64>>,
            assignment: ImpreciseAssignment,
            overEnabled: Boolean = true,
            lessEnabled: Boolean = true,
            demandValueAdapter: Bpp3dDemandValueAdapter = DefaultBpp3dDemandValueAdapter
        ): ImpreciseLoad {
            return ImpreciseLoad(
                demandEntries = demandEntriesFromItems(items, demandValueAdapter),
                assignment = assignment,
                overEnabled = overEnabled,
                lessEnabled = lessEnabled,
                demandValueAdapter = demandValueAdapter
            )
        }

        fun fromItemRanges(
            items: List<Triple<Item, UInt64, ValueRange<UInt64>>>,
            assignment: ImpreciseAssignment,
            overEnabled: Boolean = true,
            lessEnabled: Boolean = true,
            demandValueAdapter: Bpp3dDemandValueAdapter = DefaultBpp3dDemandValueAdapter
        ): ImpreciseLoad {
            return ImpreciseLoad(
                demandEntries = demandEntriesFromItemRanges(items, demandValueAdapter),
                assignment = assignment,
                overEnabled = overEnabled,
                lessEnabled = lessEnabled,
                demandValueAdapter = demandValueAdapter
            )
        }
    }

    override lateinit var load: LinearExpressionSymbols1<Flt64>

    override fun register(model: MetaModel<Flt64>): Try {
        if (!::load.isInitialized) {
            load = LinearExpressionSymbols1<Flt64>(
                "load",
                Shape1(demandEntries.size)
            ) { i, _ ->
                LinearExpressionSymbol(Flt64.zero, name = "load_$i")
            }
        }
        when (val result = model.add(load)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
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

        for ((i, demand) in demandEntries.withIndex()) {
            val thisLayers = newLayers.filter { layer -> loadCoefficient(layer, demand) neq Flt64.zero }
            if (thisLayers.isNotEmpty()) {
                val thisLoad = load[i]
                thisLoad.flush()
                thisLoad.asMutable() += sum(thisLayers.map {
                    LinearMonomial(loadCoefficient(it, demand), xi[it])
                })
            }
        }

        return Ok(newLayers)
    }
}

class PreciseLoad(
    override val demandEntries: List<Bpp3dDemandEntry>,
    private val layers: List<BinLayer>,
    private val assignment: PreciseAssignment,
    override val overEnabled: Boolean = false,
    override val lessEnabled: Boolean = true,
    override val demandValueAdapter: Bpp3dDemandValueAdapter = DefaultBpp3dDemandValueAdapter
) : AbstractLoad() {
    companion object {
        fun fromItems(
            items: List<Pair<Item, UInt64>>,
            layers: List<BinLayer>,
            assignment: PreciseAssignment,
            overEnabled: Boolean = false,
            lessEnabled: Boolean = true,
            demandValueAdapter: Bpp3dDemandValueAdapter = DefaultBpp3dDemandValueAdapter
        ): PreciseLoad {
            return PreciseLoad(
                demandEntries = demandEntriesFromItems(items, demandValueAdapter),
                layers = layers,
                assignment = assignment,
                overEnabled = overEnabled,
                lessEnabled = lessEnabled,
                demandValueAdapter = demandValueAdapter
            )
        }

        fun fromItemRanges(
            items: List<Triple<Item, UInt64, ValueRange<UInt64>>>,
            layers: List<BinLayer>,
            assignment: PreciseAssignment,
            overEnabled: Boolean = false,
            lessEnabled: Boolean = true,
            demandValueAdapter: Bpp3dDemandValueAdapter = DefaultBpp3dDemandValueAdapter
        ): PreciseLoad {
            return PreciseLoad(
                demandEntries = demandEntriesFromItemRanges(items, demandValueAdapter),
                layers = layers,
                assignment = assignment,
                overEnabled = overEnabled,
                lessEnabled = lessEnabled,
                demandValueAdapter = demandValueAdapter
            )
        }
    }

    override lateinit var load: LinearIntermediateSymbols1<Flt64>

    override fun register(model: MetaModel<Flt64>): Try {
        if (!::load.isInitialized) {
            load = LinearIntermediateSymbols1<Flt64>(
                "load",
                Shape1(demandEntries.size)
            ) { i, _ ->
                LinearExpressionSymbol(
                    sum(layers.map {
                        LinearMonomial(loadCoefficient(it, demandEntries[i]), assignment.x[it])
                    }),
                    name = "load_$i"
                )
            }
        }
        when (val result = model.add(load)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        return super.register(model)
    }
}
