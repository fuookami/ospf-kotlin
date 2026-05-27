package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model

import fuookami.ospf.kotlin.math.Scale
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.PhysicalUnit
import fuookami.ospf.kotlin.quantities.unit.QuantityUnit
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.sum
import fuookami.ospf.kotlin.math.symbol.polynomial.plusAssign
import fuookami.ospf.kotlin.core.symbol.LinearExpressionSymbol
import fuookami.ospf.kotlin.core.symbol.LinearExpressionSymbols1
import fuookami.ospf.kotlin.core.symbol.LinearIntermediateSymbols1
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BinLayer
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Bpp3dDemandKey
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Bpp3dDemandMode
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Bpp3dDemandValue
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.ActualItem
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Material
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.MaterialKey
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Item
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.noWeightDemandValue
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.statistics
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.toConcreteMode
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.compat.asScalarF64
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.compat.toLegacyItemRanges
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.compat.toLegacyItems
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.compat.toLegacyLayers
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.compat.Bpp3dDemandValueAdapter
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.compat.Bpp3dSolverValueAdapter
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.compat.DefaultBpp3dDemandValueAdapter
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.Item as QuantityItem
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.BinLayer as QuantityBinLayer
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.Material as QuantityMaterial
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.neq
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.algebra.value_range.Interval
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.multiarray.Shape1
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.model.mechanism.MetaModel
import kotlin.math.ceil

enum class Bpp3dDemandDomain {
    Discrete,
    Continuous
}

private object DemandCountUnit : PhysicalUnit() {
    @Suppress("unused")
    fun getDomain(): String = "Discrete"

    override val name = "count"
    override val symbol = "cnt"
    override val quantity = QuantityUnit(name = "count", symbol = "cnt").quantity
    override val scale = Scale()
}

private fun parseDemandDomain(raw: Any?): Bpp3dDemandDomain? {
    val token = when (raw) {
        null -> null
        is Enum<*> -> raw.name
        else -> raw.toString()
    } ?: return null
    return when {
        token.equals("Discrete", ignoreCase = true) -> Bpp3dDemandDomain.Discrete
        token.equals("Continuous", ignoreCase = true) -> Bpp3dDemandDomain.Continuous
        else -> null
    }
}

private fun invokeGetter(target: Any?, methodName: String): Any? {
    if (target == null) {
        return null
    }
    return runCatching {
        target.javaClass.methods
            .firstOrNull { it.name == methodName && it.parameterCount == 0 }
            ?.invoke(target)
    }.getOrNull()
}

private fun resolveUnitDomain(unit: PhysicalUnit, fallback: Bpp3dDemandDomain): Bpp3dDemandDomain {
    parseDemandDomain(invokeGetter(unit, "getDomain"))?.let { return it }
    parseDemandDomain(invokeGetter(invokeGetter(unit, "getQuantity"), "getDomain"))?.let { return it }
    return fallback
}

private fun defaultDemandUnit(mode: Bpp3dDemandMode): PhysicalUnit {
    return when (mode) {
        is Bpp3dDemandMode.Item -> DemandCountUnit
        is Bpp3dDemandMode.Material -> DemandCountUnit
        is Bpp3dDemandMode.ItemAmount -> DemandCountUnit
        is Bpp3dDemandMode.ItemWeight -> noWeightDemandValue().value.unit
        is Bpp3dDemandMode.ItemMaterialAmount -> DemandCountUnit
        is Bpp3dDemandMode.ItemMaterialWeight -> noWeightDemandValue().value.unit
    }
}

private fun defaultDemandDomain(
    mode: Bpp3dDemandMode,
    unit: PhysicalUnit
): Bpp3dDemandDomain {
    return when (mode) {
        is Bpp3dDemandMode.Item -> resolveUnitDomain(unit, Bpp3dDemandDomain.Continuous)
        is Bpp3dDemandMode.Material -> resolveUnitDomain(unit, Bpp3dDemandDomain.Continuous)
        is Bpp3dDemandMode.ItemAmount -> resolveUnitDomain(unit, Bpp3dDemandDomain.Discrete)
        is Bpp3dDemandMode.ItemWeight -> resolveUnitDomain(unit, Bpp3dDemandDomain.Continuous)
        is Bpp3dDemandMode.ItemMaterialAmount -> resolveUnitDomain(unit, Bpp3dDemandDomain.Discrete)
        is Bpp3dDemandMode.ItemMaterialWeight -> resolveUnitDomain(unit, Bpp3dDemandDomain.Continuous)
    }
}

data class Bpp3dDemandEntry(
    val mode: Bpp3dDemandMode,
    val key: Bpp3dDemandKey,
    val demand: Flt64,
    val demandRange: ValueRange<Flt64>,
    val quantityUnit: PhysicalUnit = defaultDemandUnit(mode),
    val quantityDomain: Bpp3dDemandDomain = defaultDemandDomain(mode, quantityUnit)
)

private fun defaultDemandValue(
    mode: Bpp3dDemandMode,
    domain: Bpp3dDemandDomain = Bpp3dDemandDomain.Discrete
): Bpp3dDemandValue {
    return when (mode) {
        is Bpp3dDemandMode.Item -> if (domain == Bpp3dDemandDomain.Discrete) Bpp3dDemandValue.Amount(UInt64.zero) else noWeightDemandValue()
        is Bpp3dDemandMode.Material -> if (domain == Bpp3dDemandDomain.Discrete) Bpp3dDemandValue.Amount(UInt64.zero) else noWeightDemandValue()
        is Bpp3dDemandMode.ItemAmount -> Bpp3dDemandValue.Amount(UInt64.zero)
        is Bpp3dDemandMode.ItemWeight -> noWeightDemandValue()
        is Bpp3dDemandMode.ItemMaterialAmount -> Bpp3dDemandValue.Amount(UInt64.zero)
        is Bpp3dDemandMode.ItemMaterialWeight -> noWeightDemandValue()
    }
}

private fun toDiscreteAmount(value: Quantity<Flt64>): UInt64 {
    val rounded = ceil(value.value.toDouble()).toLong()
    return if (rounded <= 0L) {
        UInt64.zero
    } else {
        UInt64(rounded.toULong())
    }
}

private fun isDiscreteDemandUnit(unit: PhysicalUnit): Boolean {
    return resolveUnitDomain(unit, Bpp3dDemandDomain.Continuous) == Bpp3dDemandDomain.Discrete
}

private fun modeFromItemDemand(quantity: Quantity<Flt64>): Bpp3dDemandMode {
    return Bpp3dDemandMode.Item
}

private fun modeFromMaterialDemand(quantity: Quantity<Flt64>): Bpp3dDemandMode {
    return Bpp3dDemandMode.Material
}

private fun demandValueFromQuantity(
    quantity: Quantity<Flt64>,
    demandValueAdapter: Bpp3dDemandValueAdapter
): Flt64 {
    return if (isDiscreteDemandUnit(quantity.unit)) {
        demandValueAdapter.amountToSolver(toDiscreteAmount(quantity))
    } else {
        demandValueAdapter.weightToSolver(quantity)
    }
}

fun demandEntriesFromItemDemands(
    items: List<Pair<Item, Quantity<Flt64>>>,
    demandValueAdapter: Bpp3dDemandValueAdapter = DefaultBpp3dDemandValueAdapter
): List<Bpp3dDemandEntry> {
    return items.map { (item, quantity) ->
        val mode = modeFromItemDemand(quantity)
        val demandValue = demandValueFromQuantity(quantity, demandValueAdapter)
        Bpp3dDemandEntry(
            mode = mode,
            key = Bpp3dDemandKey.Item(item),
            demand = demandValue,
            demandRange = ValueRange(
                demandValue,
                demandValue,
                Interval.Closed,
                Interval.Closed,
                Flt64
            ).value!!,
            quantityUnit = quantity.unit
        )
    }
}

@JvmName("demandEntriesFromGenericItemDemands")
fun <V : FloatingNumber<V>> demandEntriesFromItemDemands(
    items: List<Pair<QuantityItem<V>, Quantity<V>>>,
    legacyItemCache: MutableMap<QuantityItem<V>, ActualItem> = LinkedHashMap(),
    materialCache: MutableMap<QuantityMaterial<V>, Material> = LinkedHashMap(),
    demandValueAdapter: Bpp3dDemandValueAdapter = DefaultBpp3dDemandValueAdapter
): List<Bpp3dDemandEntry> {
    return demandEntriesFromItemDemands(
        items = items.map { (item, quantity) ->
            Pair(
                item.toLegacy(materialCache, legacyItemCache),
                quantity.asScalarF64()
            )
        },
        demandValueAdapter = demandValueAdapter
    )
}

fun demandEntriesFromItems(
    items: List<Pair<Item, UInt64>>,
    demandValueAdapter: Bpp3dDemandValueAdapter = DefaultBpp3dDemandValueAdapter
): List<Bpp3dDemandEntry> {
    return items.map { (item, demand) ->
        val demandValue = demandValueAdapter.amountToSolver(demand)
        Bpp3dDemandEntry(
            mode = Bpp3dDemandMode.Item,
            key = Bpp3dDemandKey.Item(item),
            demand = demandValue,
            demandRange = ValueRange(demandValue, demandValue).value!!,
            quantityUnit = DemandCountUnit
        )
    }
}

fun <V : FloatingNumber<V>> demandEntriesFromItems(
    items: List<Pair<QuantityItem<V>, UInt64>>,
    legacyItemCache: MutableMap<QuantityItem<V>, ActualItem> = LinkedHashMap(),
    materialCache: MutableMap<QuantityMaterial<V>, Material> = LinkedHashMap(),
    demandValueAdapter: Bpp3dDemandValueAdapter = DefaultBpp3dDemandValueAdapter
): List<Bpp3dDemandEntry> {
    return demandEntriesFromItems(
        items = toLegacyItems(items, legacyItemCache, materialCache),
        demandValueAdapter = demandValueAdapter
    )
}

fun demandEntriesFromItemRanges(
    items: List<Triple<Item, UInt64, ValueRange<UInt64>>>,
    demandValueAdapter: Bpp3dDemandValueAdapter = DefaultBpp3dDemandValueAdapter
): List<Bpp3dDemandEntry> {
    return items.map { (item, demand, demandRange) ->
        Bpp3dDemandEntry(
            mode = Bpp3dDemandMode.Item,
            key = Bpp3dDemandKey.Item(item),
            demand = demandValueAdapter.amountToSolver(demand),
            demandRange = demandValueAdapter.amountRangeToSolver(demandRange),
            quantityUnit = DemandCountUnit
        )
    }
}

fun <V : FloatingNumber<V>> demandEntriesFromItemRanges(
    items: List<Triple<QuantityItem<V>, UInt64, ValueRange<UInt64>>>,
    legacyItemCache: MutableMap<QuantityItem<V>, ActualItem> = LinkedHashMap(),
    materialCache: MutableMap<QuantityMaterial<V>, Material> = LinkedHashMap(),
    demandValueAdapter: Bpp3dDemandValueAdapter = DefaultBpp3dDemandValueAdapter
): List<Bpp3dDemandEntry> {
    return demandEntriesFromItemRanges(
        items = toLegacyItemRanges(items, legacyItemCache, materialCache),
        demandValueAdapter = demandValueAdapter
    )
}

private fun <V : FloatingNumber<V>> toLegacyMaterialKey(material: QuantityMaterial<V>): MaterialKey {
    return MaterialKey(
        no = material.no,
        type = material.type,
        manufacturer = material.manufacturer,
        supplier = material.supplier
    )
}

private fun demandEntriesFromMaterialDemandsByKey(
    materials: List<Pair<MaterialKey, Quantity<Flt64>>>,
    demandValueAdapter: Bpp3dDemandValueAdapter = DefaultBpp3dDemandValueAdapter
): List<Bpp3dDemandEntry> {
    return materials.map { (material, demand) ->
        val mode = modeFromMaterialDemand(demand)
        val demandValue = demandValueFromQuantity(demand, demandValueAdapter)
        Bpp3dDemandEntry(
            mode = mode,
            key = Bpp3dDemandKey.Material(material),
            demand = demandValue,
            demandRange = ValueRange(
                demandValue,
                demandValue,
                Interval.Closed,
                Interval.Closed,
                Flt64
            ).value!!,
            quantityUnit = demand.unit
        )
    }
}

fun demandEntriesFromMaterialDemands(
    materials: List<Pair<Material, Quantity<Flt64>>>,
    demandValueAdapter: Bpp3dDemandValueAdapter = DefaultBpp3dDemandValueAdapter
): List<Bpp3dDemandEntry> {
    return demandEntriesFromMaterialDemandsByKey(
        materials = materials.map { (material, demand) -> Pair(material.key, demand) },
        demandValueAdapter = demandValueAdapter
    )
}

@JvmName("demandEntriesFromGenericMaterialDemands")
fun <V : FloatingNumber<V>> demandEntriesFromMaterialDemands(
    materials: List<Pair<QuantityMaterial<V>, Quantity<V>>>,
    demandValueAdapter: Bpp3dDemandValueAdapter = DefaultBpp3dDemandValueAdapter
): List<Bpp3dDemandEntry> {
    return demandEntriesFromMaterialDemandsByKey(
        materials = materials.map { (material, demand) ->
            Pair(toLegacyMaterialKey(material), demand.asScalarF64())
        },
        demandValueAdapter = demandValueAdapter
    )
}

private fun demandEntriesFromMaterialAmountsByKey(
    materials: List<Pair<MaterialKey, UInt64>>,
    demandValueAdapter: Bpp3dDemandValueAdapter = DefaultBpp3dDemandValueAdapter
): List<Bpp3dDemandEntry> {
    return demandEntriesFromMaterialDemandsByKey(
        materials = materials.map { (material, demand) ->
            Pair(material, Quantity(Flt64(demand.toULong().toDouble()), DemandCountUnit))
        },
        demandValueAdapter = demandValueAdapter
    )
}

fun demandEntriesFromMaterialAmounts(
    materials: List<Pair<Material, UInt64>>,
    demandValueAdapter: Bpp3dDemandValueAdapter = DefaultBpp3dDemandValueAdapter
): List<Bpp3dDemandEntry> {
    return demandEntriesFromMaterialAmountsByKey(
        materials = materials.map { (material, demand) -> Pair(material.key, demand) },
        demandValueAdapter = demandValueAdapter
    )
}

@JvmName("demandEntriesFromGenericMaterialAmounts")
fun <V : FloatingNumber<V>> demandEntriesFromMaterialAmounts(
    materials: List<Pair<QuantityMaterial<V>, UInt64>>,
    demandValueAdapter: Bpp3dDemandValueAdapter = DefaultBpp3dDemandValueAdapter
): List<Bpp3dDemandEntry> {
    return demandEntriesFromMaterialAmountsByKey(
        materials = materials.map { (material, demand) ->
            Pair(toLegacyMaterialKey(material), demand)
        },
        demandValueAdapter = demandValueAdapter
    )
}

private fun demandEntriesFromMaterialWeightsByKey(
    materials: List<Pair<MaterialKey, Quantity<Flt64>>>,
    demandValueAdapter: Bpp3dDemandValueAdapter = DefaultBpp3dDemandValueAdapter
): List<Bpp3dDemandEntry> {
    return demandEntriesFromMaterialDemandsByKey(
        materials = materials,
        demandValueAdapter = demandValueAdapter
    )
}

fun demandEntriesFromMaterialWeights(
    materials: List<Pair<Material, Quantity<Flt64>>>,
    demandValueAdapter: Bpp3dDemandValueAdapter = DefaultBpp3dDemandValueAdapter
): List<Bpp3dDemandEntry> {
    return demandEntriesFromMaterialWeightsByKey(
        materials = materials.map { (material, demand) -> Pair(material.key, demand) },
        demandValueAdapter = demandValueAdapter
    )
}

@JvmName("demandEntriesFromGenericMaterialWeights")
fun <V : FloatingNumber<V>> demandEntriesFromMaterialWeights(
    materials: List<Pair<QuantityMaterial<V>, Quantity<V>>>,
    demandValueAdapter: Bpp3dDemandValueAdapter = DefaultBpp3dDemandValueAdapter
): List<Bpp3dDemandEntry> {
    return demandEntriesFromMaterialWeightsByKey(
        materials = materials.map { (material, demand) ->
            Pair(toLegacyMaterialKey(material), demand.asScalarF64())
        },
        demandValueAdapter = demandValueAdapter
    )
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
        val concreteMode = demand.mode.toConcreteMode(demand.quantityDomain == Bpp3dDemandDomain.Discrete)
        val value = layer.statistics(concreteMode)[demand.key]
            ?: defaultDemandValue(demand.mode, demand.quantityDomain)
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

        fun <V : FloatingNumber<V>> fromItems(
            items: List<Pair<QuantityItem<V>, UInt64>>,
            assignment: ImpreciseAssignment,
            legacyItemCache: MutableMap<QuantityItem<V>, ActualItem> = LinkedHashMap(),
            materialCache: MutableMap<QuantityMaterial<V>, Material> = LinkedHashMap(),
            overEnabled: Boolean = true,
            lessEnabled: Boolean = true,
            demandValueAdapter: Bpp3dDemandValueAdapter = DefaultBpp3dDemandValueAdapter
        ): ImpreciseLoad {
            return ImpreciseLoad(
                demandEntries = demandEntriesFromItems(
                    items = items,
                    legacyItemCache = legacyItemCache,
                    materialCache = materialCache,
                    demandValueAdapter = demandValueAdapter
                ),
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

        fun <V : FloatingNumber<V>> fromItemRanges(
            items: List<Triple<QuantityItem<V>, UInt64, ValueRange<UInt64>>>,
            assignment: ImpreciseAssignment,
            legacyItemCache: MutableMap<QuantityItem<V>, ActualItem> = LinkedHashMap(),
            materialCache: MutableMap<QuantityMaterial<V>, Material> = LinkedHashMap(),
            overEnabled: Boolean = true,
            lessEnabled: Boolean = true,
            demandValueAdapter: Bpp3dDemandValueAdapter = DefaultBpp3dDemandValueAdapter
        ): ImpreciseLoad {
            return ImpreciseLoad(
                demandEntries = demandEntriesFromItemRanges(
                    items = items,
                    legacyItemCache = legacyItemCache,
                    materialCache = materialCache,
                    demandValueAdapter = demandValueAdapter
                ),
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

        fun <V : FloatingNumber<V>> fromItems(
            items: List<Pair<QuantityItem<V>, UInt64>>,
            layers: List<QuantityBinLayer<V>>,
            assignment: PreciseAssignment,
            legacyItemCache: MutableMap<QuantityItem<V>, ActualItem> = LinkedHashMap(),
            materialCache: MutableMap<QuantityMaterial<V>, Material> = LinkedHashMap(),
            overEnabled: Boolean = false,
            lessEnabled: Boolean = true,
            demandValueAdapter: Bpp3dDemandValueAdapter = DefaultBpp3dDemandValueAdapter
        ): PreciseLoad {
            val legacyLayers = toLegacyLayers(layers, legacyItemCache, materialCache)
            return PreciseLoad(
                demandEntries = demandEntriesFromItems(
                    items = items,
                    legacyItemCache = legacyItemCache,
                    materialCache = materialCache,
                    demandValueAdapter = demandValueAdapter
                ),
                layers = legacyLayers,
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

        fun <V : FloatingNumber<V>> fromItemRanges(
            items: List<Triple<QuantityItem<V>, UInt64, ValueRange<UInt64>>>,
            layers: List<QuantityBinLayer<V>>,
            assignment: PreciseAssignment,
            legacyItemCache: MutableMap<QuantityItem<V>, ActualItem> = LinkedHashMap(),
            materialCache: MutableMap<QuantityMaterial<V>, Material> = LinkedHashMap(),
            overEnabled: Boolean = false,
            lessEnabled: Boolean = true,
            demandValueAdapter: Bpp3dDemandValueAdapter = DefaultBpp3dDemandValueAdapter
        ): PreciseLoad {
            val legacyLayers = toLegacyLayers(layers, legacyItemCache, materialCache)
            return PreciseLoad(
                demandEntries = demandEntriesFromItemRanges(
                    items = items,
                    legacyItemCache = legacyItemCache,
                    materialCache = materialCache,
                    demandValueAdapter = demandValueAdapter
                ),
                layers = legacyLayers,
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
                val binAmount = assignment.x.shape[0]
                LinearExpressionSymbol(
                    sum((0 until binAmount).flatMap { binIndex ->
                        layers.mapIndexed { layerIndex, layer ->
                            LinearMonomial(
                                loadCoefficient(layer, demandEntries[i]),
                                assignment.x[binIndex, layerIndex]
                            )
                        }
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
