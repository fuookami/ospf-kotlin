package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model

import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.InfraNumber
import fuookami.ospf.kotlin.math.Scale
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.PhysicalUnit
import fuookami.ospf.kotlin.quantities.unit.QuantityUnit
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
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.ActualItem
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Material
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.MaterialKey
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Item
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.noWeightDemandValue
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.statistics
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.toConcreteMode
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.toLegacyItemDemands
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.toLegacyItemRanges
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.toLegacyMaterialDemands
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.toLegacyMaterialWeightDemandsByKey
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.toLegacyItems
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.toLegacyLayers
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.Bpp3dDemandValueAdapter
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.Bpp3dSolverValueAdapter
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.DefaultBpp3dDemandValueAdapter
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.Item as QuantityItem
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.BinLayer as QuantityBinLayer
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.Material as QuantityMaterial
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.neq
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.utils.functional.*
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

private fun defaultDemandUnit(): PhysicalUnit {
    return DemandCountUnit
}

private fun defaultDemandDomain(unit: PhysicalUnit): Bpp3dDemandDomain {
    return resolveUnitDomain(unit, Bpp3dDemandDomain.Continuous)
}

data class Bpp3dDemandEntry(
    val mode: Bpp3dDemandMode,
    val key: Bpp3dDemandKey,
    val demand: InfraNumber,
    val demandRange: ValueRange<InfraNumber>,
    val quantityUnit: PhysicalUnit = defaultDemandUnit(),
    val quantityDomain: Bpp3dDemandDomain = defaultDemandDomain(quantityUnit)
)

private fun defaultDemandValue(
    domain: Bpp3dDemandDomain = Bpp3dDemandDomain.Discrete
): Bpp3dDemandValue {
    return if (domain == Bpp3dDemandDomain.Discrete) {
        Bpp3dDemandValue.Amount(UInt64.zero)
    } else {
        noWeightDemandValue()
    }
}

private fun toDiscreteAmount(value: Quantity<InfraNumber>): UInt64 {
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

data class Bpp3dItemDemand(
    val item: Item,
    val quantity: Quantity<InfraNumber>,
    val mode: Bpp3dDemandMode = Bpp3dDemandMode.Item
)

data class Bpp3dMaterialDemand(
    val material: MaterialKey,
    val quantity: Quantity<InfraNumber>,
    val mode: Bpp3dDemandMode = Bpp3dDemandMode.Material
)

private fun resolveItemDemandMode(mode: Bpp3dDemandMode): Bpp3dDemandMode {
    return when (mode) {
        is Bpp3dDemandMode.Item,
        is Bpp3dDemandMode.ItemAmount,
        is Bpp3dDemandMode.ItemWeight -> mode

        else -> throw IllegalArgumentException("Item demand mode must be Item/ItemAmount/ItemWeight, but was $mode")
    }
}

private fun resolveMaterialDemandMode(mode: Bpp3dDemandMode): Bpp3dDemandMode {
    return when (mode) {
        is Bpp3dDemandMode.Material,
        is Bpp3dDemandMode.ItemMaterialAmount,
        is Bpp3dDemandMode.ItemMaterialWeight -> mode

        else -> throw IllegalArgumentException("Material demand mode must be Material/ItemMaterialAmount/ItemMaterialWeight, but was $mode")
    }
}

private fun demandValueFromQuantity(
    quantity: Quantity<InfraNumber>,
    demandValueAdapter: Bpp3dDemandValueAdapter
): InfraNumber {
    return if (isDiscreteDemandUnit(quantity.unit)) {
        demandValueAdapter.amountToSolver(toDiscreteAmount(quantity))
    } else {
        demandValueAdapter.weightToSolver(quantity)
    }
}

private fun exactDemandRange(value: InfraNumber): ValueRange<InfraNumber> {
    return ValueRange(
        value,
        value,
        Interval.Closed,
        Interval.Closed,
        layerAssignmentScalarProvider()
    ).value!!
}

fun demandEntriesFromItemDemands(
    items: List<Pair<Item, Quantity<InfraNumber>>>,
    demandValueAdapter: Bpp3dDemandValueAdapter = DefaultBpp3dDemandValueAdapter
): List<Bpp3dDemandEntry> {
    return demandEntriesFromLabeledItemDemands(
        items = items.map { (item, quantity) ->
            Bpp3dItemDemand(
                item = item,
                quantity = quantity
            )
        },
        demandValueAdapter = demandValueAdapter
    )
}

fun demandEntriesFromLabeledItemDemands(
    items: List<Bpp3dItemDemand>,
    demandValueAdapter: Bpp3dDemandValueAdapter = DefaultBpp3dDemandValueAdapter
): List<Bpp3dDemandEntry> {
    return items.map { demand ->
        val mode = resolveItemDemandMode(demand.mode)
        val demandValue = demandValueFromQuantity(demand.quantity, demandValueAdapter)
        Bpp3dDemandEntry(
            mode = mode,
            key = Bpp3dDemandKey.Item(demand.item),
            demand = demandValue,
            demandRange = exactDemandRange(demandValue),
            quantityUnit = demand.quantity.unit
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
    return demandEntriesFromLabeledItemDemands(
        items = toLegacyItemDemands(items, legacyItemCache, materialCache),
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
            demandRange = exactDemandRange(demandValue),
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

private fun demandEntriesFromMaterialDemandsByKey(
    materials: List<Bpp3dMaterialDemand>,
    demandValueAdapter: Bpp3dDemandValueAdapter = DefaultBpp3dDemandValueAdapter
): List<Bpp3dDemandEntry> {
    return materials.map { demand ->
        val mode = resolveMaterialDemandMode(demand.mode)
        val demandValue = demandValueFromQuantity(demand.quantity, demandValueAdapter)
        Bpp3dDemandEntry(
            mode = mode,
            key = Bpp3dDemandKey.Material(demand.material),
            demand = demandValue,
            demandRange = exactDemandRange(demandValue),
            quantityUnit = demand.quantity.unit
        )
    }
}

fun demandEntriesFromMaterialDemands(
    materials: List<Pair<Material, Quantity<InfraNumber>>>,
    demandValueAdapter: Bpp3dDemandValueAdapter = DefaultBpp3dDemandValueAdapter
): List<Bpp3dDemandEntry> {
    return demandEntriesFromMaterialDemandsByKey(
        materials = materials.map { (material, demand) ->
            Bpp3dMaterialDemand(
                material = material.key,
                quantity = demand
            )
        },
        demandValueAdapter = demandValueAdapter
    )
}

fun demandEntriesFromLabeledMaterialDemands(
    materials: List<Bpp3dMaterialDemand>,
    demandValueAdapter: Bpp3dDemandValueAdapter = DefaultBpp3dDemandValueAdapter
): List<Bpp3dDemandEntry> {
    return demandEntriesFromMaterialDemandsByKey(
        materials = materials,
        demandValueAdapter = demandValueAdapter
    )
}

@JvmName("demandEntriesFromGenericMaterialDemands")
fun <V : FloatingNumber<V>> demandEntriesFromMaterialDemands(
    materials: List<Pair<QuantityMaterial<V>, Quantity<V>>>,
    demandValueAdapter: Bpp3dDemandValueAdapter = DefaultBpp3dDemandValueAdapter
): List<Bpp3dDemandEntry> {
    return demandEntriesFromMaterialDemandsByKey(
        materials = toLegacyMaterialDemands(materials),
        demandValueAdapter = demandValueAdapter
    )
}

private fun demandEntriesFromMaterialAmountsByKey(
    materials: List<Pair<MaterialKey, UInt64>>,
    demandValueAdapter: Bpp3dDemandValueAdapter = DefaultBpp3dDemandValueAdapter
): List<Bpp3dDemandEntry> {
    return demandEntriesFromMaterialDemandsByKey(
        materials = materials.map { (material, demand) ->
            Bpp3dMaterialDemand(
                material = material,
                quantity = Quantity(InfraNumber(demand.toULong().toDouble()), DemandCountUnit)
            )
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
            Pair(
                MaterialKey(
                    no = material.no,
                    type = material.type,
                    manufacturer = material.manufacturer,
                    supplier = material.supplier
                ),
                demand
            )
        },
        demandValueAdapter = demandValueAdapter
    )
}

private fun demandEntriesFromMaterialWeightsByKey(
    materials: List<Pair<MaterialKey, Quantity<InfraNumber>>>,
    demandValueAdapter: Bpp3dDemandValueAdapter = DefaultBpp3dDemandValueAdapter
): List<Bpp3dDemandEntry> {
    return demandEntriesFromMaterialDemandsByKey(
        materials = materials.map { (material, demand) ->
            Bpp3dMaterialDemand(
                material = material,
                quantity = demand
            )
        },
        demandValueAdapter = demandValueAdapter
    )
}

fun demandEntriesFromMaterialWeights(
    materials: List<Pair<Material, Quantity<InfraNumber>>>,
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
        materials = toLegacyMaterialWeightDemandsByKey(materials),
        demandValueAdapter = demandValueAdapter
    )
}

interface Load {
    val demandEntries: List<Bpp3dDemandEntry>
    val demandValueAdapter: Bpp3dDemandValueAdapter

    val load: LinearIntermediateSymbols1<InfraNumber>
    val overLoad: LinearIntermediateSymbols1<InfraNumber>
    val lessLoad: LinearIntermediateSymbols1<InfraNumber>

    val overEnabled: Boolean
    val lessEnabled: Boolean
}

abstract class AbstractLoad : Load {
    override lateinit var overLoad: LinearIntermediateSymbols1<InfraNumber>
    override lateinit var lessLoad: LinearIntermediateSymbols1<InfraNumber>

    open fun register(model: MetaModel<InfraNumber>): Try {
        if (overEnabled && !::overLoad.isInitialized) {
            overLoad = LinearIntermediateSymbols1<InfraNumber>(
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
            lessLoad = LinearIntermediateSymbols1<InfraNumber>(
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
    ): InfraNumber {
        val concreteMode = demand.mode.toConcreteMode(demand.quantityDomain == Bpp3dDemandDomain.Discrete)
        val value = layer.statistics(concreteMode)[demand.key]
            ?: defaultDemandValue(demand.quantityDomain)
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

    override lateinit var load: LinearExpressionSymbols1<InfraNumber>

    override fun register(model: MetaModel<InfraNumber>): Try {
        if (!::load.isInitialized) {
            load = LinearExpressionSymbols1<InfraNumber>(
                "load",
                Shape1(demandEntries.size)
            ) { i, _ ->
                LinearExpressionSymbol(InfraNumber.zero, name = "load_$i")
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
        model: AbstractLinearMetaModel<InfraNumber>
    ): Ret<List<BinLayer>> {
        assert(newLayers.isNotEmpty())

        val xi = assignment.x[iteration.toInt()]

        for ((i, demand) in demandEntries.withIndex()) {
            val thisLayers = newLayers.filter { layer -> loadCoefficient(layer, demand) neq InfraNumber.zero }
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

    override lateinit var load: LinearIntermediateSymbols1<InfraNumber>

    override fun register(model: MetaModel<InfraNumber>): Try {
        if (!::load.isInitialized) {
            load = LinearIntermediateSymbols1<InfraNumber>(
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




