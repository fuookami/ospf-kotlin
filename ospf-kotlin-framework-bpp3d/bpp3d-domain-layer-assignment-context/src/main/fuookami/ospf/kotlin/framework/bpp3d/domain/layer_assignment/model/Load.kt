/**
 * Layer assignment load model.
 * 层分配负载模型。
*/
package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model

import kotlin.math.ceil
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.symbol.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.*
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.value_range.*
import fuookami.ospf.kotlin.math.Scale
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.multiarray.Shape1
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

/**
 * BPP3D demand domain.
 * BPP3D 需求域。
*/
enum class Bpp3dDemandDomain {
    /** 离散域 / discrete domain */
    Discrete,
    /** 连续域 / continuous domain */
    Continuous
}

/** 需求计数单位对象 / Demand count unit object */
private object DemandCountUnit : PhysicalUnit() {

    /**
     * 获取需求域。
     * Gets the demand domain.
     *
     * @return the domain string / 域字符串
    */
    @Suppress("unused")
    fun getDomain(): String = "Discrete"

    override val name = "count"
    override val symbol = "cnt"
    override val quantity = QuantityUnit(name = "count", symbol = "cnt").quantity
    override val conversionRule = UnitConversionRule.Linear(Scale())
}

/**
 * 解析需求域。
 * Parse the demand domain from a raw value.
 *
 * @param raw the raw value to parse / 待解析的原始值
 * @return the parsed demand domain, or null if invalid / 解析后的需求域，无效时返回 null
*/
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

/**
 * 通过反射调用目标对象的 getter 方法。
 * Invoke a getter method on the target object via reflection.
 *
 * @param target the target object / 目标对象
 * @param methodName the getter method name / getter 方法名
 * @return the getter result, or null on failure / getter 结果，失败时返回 null
*/
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

/**
 * 解析物理单位对应的需求域。
 * Resolve the demand domain from a physical unit.
 *
 * @param unit the physical unit / 物理单位
 * @param fallback the fallback domain / 回退域
 * @return the resolved demand domain / 解析后的需求域
*/
private fun resolveUnitDomain(unit: PhysicalUnit, fallback: Bpp3dDemandDomain): Bpp3dDemandDomain {
    parseDemandDomain(invokeGetter(unit, "getDomain"))?.let { return it }
    parseDemandDomain(invokeGetter(invokeGetter(unit, "getQuantity"), "getDomain"))?.let { return it }
    return fallback
}

/** 返回默认需求单位 / Returns the default demand unit
 *
 * @return the default demand unit / 默认需求单位
*/
private fun defaultDemandUnit(): PhysicalUnit {
    return DemandCountUnit
}

/**
 * Item demand interface.
 * 货物需求接口。
 *
 * @param V 数值类型 / numeric type
*/
interface ItemDemand<V : FloatingNumber<V>> {

    /** 货物 / item */
    val item: Item

    /** 需求量 / quantity */
    val quantity: Quantity<V>

    /** 需求模式 / demand mode */
    val mode: Bpp3dDemandMode
}

/**
 * Material demand interface.
 * 物料需求接口。
 *
 * @param V 数值类型 / numeric type
*/
interface MaterialDemand<V : FloatingNumber<V>> {

    /** 物料键 / material key */
    val material: MaterialKey

    /** 需求量 / quantity */
    val quantity: Quantity<V>

    /** 需求模式 / demand mode */
    val mode: Bpp3dDemandMode
}

/**
 * BPP3D demand entry.
 * BPP3D 需求条目。
 *
 * @param V 数值类型 / numeric type
 * @property mode 需求模式 / demand mode
 * @property key 需求键 / demand key
 * @property demand 需求值 / demand value
 * @property demandRange 需求值域 / demand value range
 * @property quantityUnit 量纲单位 / quantity unit
*/
data class Bpp3dDemandEntry<V : FloatingNumber<V>>(
    val mode: Bpp3dDemandMode,
    val key: Bpp3dDemandKey,
    val demand: V,
    val demandRange: ValueRange<V>,
    val quantityUnit: PhysicalUnit = defaultDemandUnit()
) {
    val quantityDomain: Bpp3dDemandDomain
        get() = resolveUnitDomain(quantityUnit, Bpp3dDemandDomain.Continuous)
}

/**
 * defaultDemandValue.
 * defaultDemandValue。
 *
 * @param domain the demand domain / 需求域
 * @return the default demand value / 默认需求值
*/
private fun defaultDemandValue(
    domain: Bpp3dDemandDomain = Bpp3dDemandDomain.Discrete
): Bpp3dDemandValue {
    return if (domain == Bpp3dDemandDomain.Discrete) {
        Bpp3dDemandValue.Amount(UInt64.zero)
    } else {
        noWeightDemandValue()
    }
}

/**
 * 将数量值转换为离散数量。
 * Convert a quantity value to a discrete amount.
 *
 * @param value the quantity value to convert / 待转换的数量值
 * @return the discrete amount / 离散数量
*/
private fun toDiscreteAmount(value: Quantity<FltX>): UInt64 {
    val rounded = ceil(value.value.toDouble()).toLong()
    return if (rounded <= 0L) {
        UInt64.zero
    } else {
        UInt64(rounded.toULong())
    }
}

/**
 * 检查物理单位是否为离散需求单位。
 * Check whether the physical unit represents a discrete demand unit.
 *
 * @param unit the physical unit to check / 待检查的物理单位
 * @return whether the unit represents a discrete demand / 是否为离散需求单位
*/
private fun isDiscreteDemandUnit(unit: PhysicalUnit): Boolean {
    return resolveUnitDomain(unit, Bpp3dDemandDomain.Continuous) == Bpp3dDemandDomain.Discrete
}

/**
 * Item demand data class.
 * 货物需求数据类。
 *
 * @param V 数值类型 / numeric type
*/
data class Bpp3dItemDemand<V : FloatingNumber<V>>(
    override val item: Item,
    override val quantity: Quantity<V>,
    override val mode: Bpp3dDemandMode = Bpp3dDemandMode.Item
) : ItemDemand<V>

/**
 * 物料需求数据类。
 * Material demand data class.
 *
 * @param V 数值类型 / numeric type
*/
data class Bpp3dMaterialDemand<V : FloatingNumber<V>>(
    override val material: MaterialKey,
    override val quantity: Quantity<V>,
    override val mode: Bpp3dDemandMode = Bpp3dDemandMode.Material
) : MaterialDemand<V>

/**
 * 校验并解析货物需求模式，返回 Ret 以替代抛出异常。
 * Validate and resolve item demand mode, returning Ret instead of throwing.
 *
 * @param mode 待校验的需求模式 / demand mode to validate
 * @return 成功时返回合法的货物需求模式，失败时返回错误 / valid item demand mode on success, error on failure
*/
private fun resolveItemDemandMode(mode: Bpp3dDemandMode): Ret<Bpp3dDemandMode> {
    return when (mode) {
        is Bpp3dDemandMode.Item,
        is Bpp3dDemandMode.ItemAmount,
        is Bpp3dDemandMode.ItemWeight -> ok(mode)

        else -> Failed(ErrorCode.IllegalArgument, "Item demand mode must be Item/ItemAmount/ItemWeight, but was $mode")
    }
}

/**
 * 校验并解析物料需求模式，返回 Ret 以替代抛出异常。
 * Validate and resolve material demand mode, returning Ret instead of throwing.
 *
 * @param mode 待校验的需求模式 / demand mode to validate
 * @return 成功时返回合法的物料需求模式，失败时返回错误 / valid material demand mode on success, error on failure
*/
private fun resolveMaterialDemandMode(mode: Bpp3dDemandMode): Ret<Bpp3dDemandMode> {
    return when (mode) {
        is Bpp3dDemandMode.Material,
        is Bpp3dDemandMode.ItemMaterialAmount,
        is Bpp3dDemandMode.ItemMaterialWeight -> ok(mode)

        else -> Failed(ErrorCode.IllegalArgument, "Material demand mode must be Material/ItemMaterialAmount/ItemMaterialWeight, but was $mode")
    }
}

/**
 * 从数量值提取需求值。
 * Extract the demand value from a quantity.
 *
 * @param quantity the quantity value / 数量值
 * @param demandValueAdapter the demand value adapter / 需求值适配器
 * @return the demand value in solver representation / 求解器表示的需求值
*/
private fun demandValueFromQuantity(
    quantity: Quantity<FltX>,
    demandValueAdapter: Bpp3dSolverValueAdapter
): FltX {
    return if (isDiscreteDemandUnit(quantity.unit)) {
        demandValueAdapter.amountToSolver(toDiscreteAmount(quantity))
    } else {
        demandValueAdapter.weightToSolver(quantity)
    }
}

/**
 * 精确需求范围 / Exact demand range
 *
 * @param value the demand value / 需求值
 * @return the exact value range / 精确值范围
*/
private fun exactDemandRange(value: FltX): ValueRange<FltX> {
    return ValueRange(
        value,
        value,
        Interval.Closed,
        Interval.Closed,
        layerAssignmentScalarProvider()
    ).value!!
}

/**
 * 从货物-数量对列表构建需求条目，直接映射为 Item 模式。
 * Build demand entries from item-quantity pairs, mapping directly to Item mode.
 *
 * @param items 货物与数量的配对列表 / list of item-quantity pairs
 * @param demandValueAdapter 需求值适配器 / demand value adapter
 * @return 需求条目列表 / list of demand entries
*/
fun demandEntriesFromItemDemands(
    items: List<Pair<Item, Quantity<FltX>>>,
    demandValueAdapter: Bpp3dSolverValueAdapter = DefaultBpp3dSolverValueAdapter
): List<Bpp3dDemandEntry<FltX>> {
    return items.map { (item, quantity) ->
        val demandValue = demandValueFromQuantity(quantity, demandValueAdapter)
        Bpp3dDemandEntry(
            mode = Bpp3dDemandMode.Item,
            key = Bpp3dDemandKey.Item(item),
            demand = demandValue,
            demandRange = exactDemandRange(demandValue),
            quantityUnit = quantity.unit
        )
    }
}

/**
 * 从带标签的货物需求列表构建需求条目，校验需求模式合法性。
 * Build demand entries from labeled item demands, validating demand mode legality.
 *
 * @param items 带标签的货物需求列表 / list of labeled item demands
 * @param demandValueAdapter 需求值适配器 / demand value adapter
 * @return 成功时返回需求条目列表，失败时返回错误 / demand entry list on success, error on failure
*/
fun demandEntriesFromLabeledItemDemands(
    items: List<ItemDemand<FltX>>,
    demandValueAdapter: Bpp3dSolverValueAdapter = DefaultBpp3dSolverValueAdapter
): Ret<List<Bpp3dDemandEntry<FltX>>> {
    val entries = ArrayList<Bpp3dDemandEntry<FltX>>()
    for (demand in items) {
        val mode = when (val result = resolveItemDemandMode(demand.mode)) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val demandValue = demandValueFromQuantity(demand.quantity, demandValueAdapter)
        entries.add(Bpp3dDemandEntry(
            mode = mode,
            key = Bpp3dDemandKey.Item(demand.item),
            demand = demandValue,
            demandRange = exactDemandRange(demandValue),
            quantityUnit = demand.quantity.unit
        ))
    }
    return ok(entries)
}

/**
 * 从货物-数量对列表构建需求条目，直接映射为 Item 模式。
 * Build demand entries from item-amount pairs, mapping directly to Item mode.
 *
 * @param items 货物与数量的配对列表 / list of item-amount pairs
 * @param demandValueAdapter 需求值适配器 / demand value adapter
 * @return 需求条目列表 / list of demand entries
*/
fun demandEntriesFromItems(
    items: List<Pair<Item, UInt64>>,
    demandValueAdapter: Bpp3dSolverValueAdapter = DefaultBpp3dSolverValueAdapter
): List<Bpp3dDemandEntry<FltX>> {
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

/**
 * 从货物-数量-范围三元组列表构建需求条目。
 * Build demand entries from item-amount-range triple list.
 *
 * @param items 货物、数量和范围的三元组列表 / list of item-amount-range triples
 * @param demandValueAdapter 需求值适配器 / demand value adapter
 * @return 需求条目列表 / list of demand entries
*/
fun demandEntriesFromItemRanges(
    items: List<Triple<Item, UInt64, ValueRange<UInt64>>>,
    demandValueAdapter: Bpp3dSolverValueAdapter = DefaultBpp3dSolverValueAdapter
): List<Bpp3dDemandEntry<FltX>> {
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

/**
 * 从物料需求列表按 MaterialKey 构建需求条目，校验需求模式合法性。
 * Build demand entries from material demands by MaterialKey, validating demand mode legality.
 *
 * @param materials 物料需求列表 / list of material demands
 * @param demandValueAdapter 需求值适配器 / demand value adapter
 * @return 成功时返回需求条目列表，失败时返回错误 / demand entry list on success, error on failure
*/
private fun demandEntriesFromMaterialDemandsByKey(
    materials: List<MaterialDemand<FltX>>,
    demandValueAdapter: Bpp3dSolverValueAdapter = DefaultBpp3dSolverValueAdapter
): Ret<List<Bpp3dDemandEntry<FltX>>> {
    val entries = ArrayList<Bpp3dDemandEntry<FltX>>()
    for (demand in materials) {
        val mode = when (val result = resolveMaterialDemandMode(demand.mode)) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val demandValue = demandValueFromQuantity(demand.quantity, demandValueAdapter)
        entries.add(Bpp3dDemandEntry(
            mode = mode,
            key = Bpp3dDemandKey.Material(demand.material),
            demand = demandValue,
            demandRange = exactDemandRange(demandValue),
            quantityUnit = demand.quantity.unit
        ))
    }
    return ok(entries)
}

/**
 * 从物料-数量对列表构建需求条目，直接映射为 Material 模式。
 * Build demand entries from material-quantity pairs, mapping directly to Material mode.
 *
 * @param materials 物料与数量的配对列表 / list of material-quantity pairs
 * @param demandValueAdapter 需求值适配器 / demand value adapter
 * @return 需求条目列表 / list of demand entries
*/
fun demandEntriesFromMaterialDemands(
    materials: List<Pair<Material<FltX>, Quantity<FltX>>>,
    demandValueAdapter: Bpp3dSolverValueAdapter = DefaultBpp3dSolverValueAdapter
): List<Bpp3dDemandEntry<FltX>> {
    return materials.map { (material, demand) ->
        val demandValue = demandValueFromQuantity(demand, demandValueAdapter)
        Bpp3dDemandEntry(
            mode = Bpp3dDemandMode.Material,
            key = Bpp3dDemandKey.Material(material.key),
            demand = demandValue,
            demandRange = exactDemandRange(demandValue),
            quantityUnit = demand.unit
        )
    }
}

/**
 * 从带标签的物料需求列表构建需求条目，校验需求模式合法性。
 * Build demand entries from labeled material demands, validating demand mode legality.
 *
 * @param materials 带标签的物料需求列表 / list of labeled material demands
 * @param demandValueAdapter 需求值适配器 / demand value adapter
 * @return 成功时返回需求条目列表，失败时返回错误 / demand entry list on success, error on failure
*/
fun demandEntriesFromLabeledMaterialDemands(
    materials: List<MaterialDemand<FltX>>,
    demandValueAdapter: Bpp3dSolverValueAdapter = DefaultBpp3dSolverValueAdapter
): Ret<List<Bpp3dDemandEntry<FltX>>> {
    return demandEntriesFromMaterialDemandsByKey(
        materials = materials,
        demandValueAdapter = demandValueAdapter
    )
}

/**
 * 从物料键-数量对列表构建需求条目，直接使用数量适配器转换。
 * Build demand entries from material key-amount pairs, converting directly via amount adapter.
 *
 * @param materials 物料键与数量的配对列表 / list of material key-amount pairs
 * @param demandValueAdapter 需求值适配器 / demand value adapter
 * @return 需求条目列表 / list of demand entries
*/
private fun demandEntriesFromMaterialAmountsByKey(
    materials: List<Pair<MaterialKey, UInt64>>,
    demandValueAdapter: Bpp3dSolverValueAdapter = DefaultBpp3dSolverValueAdapter
): List<Bpp3dDemandEntry<FltX>> {
    return materials.map { (material, demand) ->
        val demandValue = demandValueAdapter.amountToSolver(demand)
        Bpp3dDemandEntry(
            mode = Bpp3dDemandMode.Material,
            key = Bpp3dDemandKey.Material(material),
            demand = demandValue,
            demandRange = exactDemandRange(demandValue),
            quantityUnit = DemandCountUnit
        )
    }
}

/**
 * 从物料-数量对列表构建需求条目，直接映射为 Material 模式。
 * Build demand entries from material-amount pairs, mapping directly to Material mode.
 *
 * @param materials 物料与数量的配对列表 / list of material-amount pairs
 * @param demandValueAdapter 需求值适配器 / demand value adapter
 * @return 需求条目列表 / list of demand entries
*/
fun demandEntriesFromMaterialAmounts(
    materials: List<Pair<Material<FltX>, UInt64>>,
    demandValueAdapter: Bpp3dSolverValueAdapter = DefaultBpp3dSolverValueAdapter
): List<Bpp3dDemandEntry<FltX>> {
    return demandEntriesFromMaterialAmountsByKey(
        materials = materials.map { (material, demand) -> Pair(material.key, demand) },
        demandValueAdapter = demandValueAdapter
    )
}

/**
 * 从物料键-重量对列表构建需求条目，直接映射为 Material 模式。
 * Build demand entries from material key-weight pairs, mapping directly to Material mode.
 *
 * @param materials 物料键与重量的配对列表 / list of material key-weight pairs
 * @param demandValueAdapter 需求值适配器 / demand value adapter
 * @return 需求条目列表 / list of demand entries
*/
private fun demandEntriesFromMaterialWeightsByKey(
    materials: List<Pair<MaterialKey, Quantity<FltX>>>,
    demandValueAdapter: Bpp3dSolverValueAdapter = DefaultBpp3dSolverValueAdapter
): List<Bpp3dDemandEntry<FltX>> {
    return materials.map { (material, demand) ->
        val demandValue = demandValueFromQuantity(demand, demandValueAdapter)
        Bpp3dDemandEntry(
            mode = Bpp3dDemandMode.Material,
            key = Bpp3dDemandKey.Material(material),
            demand = demandValue,
            demandRange = exactDemandRange(demandValue),
            quantityUnit = demand.unit
        )
    }
}

/**
 * 从物料-重量对列表构建需求条目，直接映射为 Material 模式。
 * Build demand entries from material-weight pairs, mapping directly to Material mode.
 *
 * @param materials 物料与重量的配对列表 / list of material-weight pairs
 * @param demandValueAdapter 需求值适配器 / demand value adapter
 * @return 需求条目列表 / list of demand entries
*/
fun demandEntriesFromMaterialWeights(
    materials: List<Pair<Material<FltX>, Quantity<FltX>>>,
    demandValueAdapter: Bpp3dSolverValueAdapter = DefaultBpp3dSolverValueAdapter
): List<Bpp3dDemandEntry<FltX>> {
    return demandEntriesFromMaterialWeightsByKey(
        materials = materials.map { (material, demand) -> Pair(material.key, demand) },
        demandValueAdapter = demandValueAdapter
    )
}

/**
 * 负载接口，管理需求约束的符号表达。
 * Load interface, manages symbolic expressions for demand constraints.
 *
 * @param V 数值类型 / numeric type
*/
interface Load<V : FloatingNumber<V>> {

    /** 需求条目列表 / demand entry list */
    val demandEntries: List<Bpp3dDemandEntry<V>>

    /** 需求值适配器 / demand value adapter */
    val demandValueAdapter: Bpp3dSolverValueAdapter

    /** 负载符号 / load symbols */
    val load: LinearIntermediateSymbols1<FltX>

    /** 超载符号 / over-load symbols */
    val overLoad: LinearIntermediateSymbols1<FltX>

    /** 欠载符号 / less-load symbols */
    val lessLoad: LinearIntermediateSymbols1<FltX>

    /** 是否启用超载 / whether over-load is enabled */
    val overEnabled: Boolean

    /** 是否启用欠载 / whether less-load is enabled */
    val lessEnabled: Boolean
}

/**
 * 抽象负载基类，提供超载和欠载符号的注册。
 * Abstract load base class, provides registration of over-load and less-load symbols.
*/
abstract class AbstractLoad : Load<FltX> {
    override lateinit var overLoad: LinearIntermediateSymbols1<FltX>
    override lateinit var lessLoad: LinearIntermediateSymbols1<FltX>

    /**
     * 向元模型注册超载和欠载符号。
     * Register over-load and less-load symbols into the meta model.
     *
     * @param model the meta model to register into / 要注册的元模型
     * @return the operation result / 操作结果
    */
    open fun register(model: MetaModel<FltX>): Try {
        if (overEnabled && !::overLoad.isInitialized) {
            overLoad = LinearIntermediateSymbols1<FltX>(
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
            lessLoad = LinearIntermediateSymbols1<FltX>(
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

    /**
     * 计算层对需求的负载系数。
     * Calculate load coefficient of layer for demand.
     *
     * @param layer 层 / layer
     * @param demand 需求条目 / demand entry
     * @return 负载系数 / load coefficient
    */
    protected fun loadCoefficient(
        layer: BinLayer,
        demand: Bpp3dDemandEntry<FltX>
    ): FltX {
        val concreteMode = demand.mode.toConcreteMode(
            key = demand.key,
            isDiscrete = demand.quantityDomain == Bpp3dDemandDomain.Discrete
        )
        val value = layer.statistics(concreteMode)[demand.key]
            ?: defaultDemandValue(demand.quantityDomain)
        return demandValueAdapter.toSolver(value)
    }
}

/**
 * 不精确负载，用于列生成 RMP 阶段。
 * Imprecise load, used for column generation RMP phase.
 *
 * @property demandEntries 需求条目列表 / demand entry list
 * @property assignment 不精确赋值 / imprecise assignment
 * @property overEnabled 是否启用超载 / whether over-load is enabled
 * @property lessEnabled 是否启用欠载 / whether less-load is enabled
 * @property demandValueAdapter 需求值适配器 / demand value adapter
*/
class ImpreciseLoad(
    override val demandEntries: List<Bpp3dDemandEntry<FltX>>,
    private val assignment: ImpreciseAssignment,
    override val overEnabled: Boolean = true,
    override val lessEnabled: Boolean = true,
    override val demandValueAdapter: Bpp3dSolverValueAdapter = DefaultBpp3dSolverValueAdapter
 ) : AbstractLoad() {
    companion object {
        /**
         * 从货物-数量对列表创建不精确负载。
         * Create an imprecise load from item-amount pairs.
         *
         * @param items the item-amount pairs / 货物-数量配对列表
         * @param assignment the imprecise assignment / 不精确赋值
         * @param overEnabled whether over-load is enabled / 是否启用超载
         * @param lessEnabled whether less-load is enabled / 是否启用欠载
         * @param demandValueAdapter the demand value adapter / 需求值适配器
         * @return the imprecise load instance / 不精确负载实例
        */
        fun fromItems(
            items: List<Pair<Item, UInt64>>,
            assignment: ImpreciseAssignment,
            overEnabled: Boolean = true,
            lessEnabled: Boolean = true,
            demandValueAdapter: Bpp3dSolverValueAdapter = DefaultBpp3dSolverValueAdapter
        ): ImpreciseLoad {
            return ImpreciseLoad(
                demandEntries = demandEntriesFromItems(items, demandValueAdapter),
                assignment = assignment,
                overEnabled = overEnabled,
                lessEnabled = lessEnabled,
                demandValueAdapter = demandValueAdapter
            )
        }

        /**
         * 从货物-数量-范围三元组列表创建不精确负载。
         * Create an imprecise load from item-amount-range triples.
         *
         * @param items the item-amount-range triples / 货物-数量-范围三元组列表
         * @param assignment the imprecise assignment / 不精确赋值
         * @param overEnabled whether over-load is enabled / 是否启用超载
         * @param lessEnabled whether less-load is enabled / 是否启用欠载
         * @param demandValueAdapter the demand value adapter / 需求值适配器
         * @return the imprecise load instance / 不精确负载实例
        */
        fun fromItemRanges(
            items: List<Triple<Item, UInt64, ValueRange<UInt64>>>,
            assignment: ImpreciseAssignment,
            overEnabled: Boolean = true,
            lessEnabled: Boolean = true,
            demandValueAdapter: Bpp3dSolverValueAdapter = DefaultBpp3dSolverValueAdapter
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

    override lateinit var load: LinearExpressionSymbols1<FltX>

    override fun register(model: MetaModel<FltX>): Try {
        if (!::load.isInitialized) {
            load = LinearExpressionSymbols1<FltX>(
                "load",
                Shape1(demandEntries.size)
            ) { i, _ ->
                LinearExpressionSymbol(FltX.zero, name = "load_$i")
            }
        }
        when (val result = model.add(load)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        return super.register(model)
    }

    /**
     * 向模型添加新列。
     * Add new columns to the model.
     *
     * @param iteration the current iteration / 当前迭代
     * @param newLayers the new layers to add / 要添加的新层列表
     * @param model the abstract linear meta model / 抽象线性元模型
     * @return the added layers on success, error on failure / 成功时返回添加的层列表，失败时返回错误
    */
    suspend fun addColumns(
        iteration: UInt64,
        newLayers: List<BinLayer>,
        model: AbstractLinearMetaModel<FltX>
    ): Ret<List<BinLayer>> {
        assert(newLayers.isNotEmpty())

        val xi = assignment.x[iteration.toInt()]

        for ((i, demand) in demandEntries.withIndex()) {
            val thisLayers = newLayers.filter { layer -> loadCoefficient(layer, demand) neq FltX.zero }
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

/**
 * 精确负载，用于最终 MILP 求解阶段。
 * Precise load, used for final MILP solving phase.
 *
 * @property demandEntries 需求条目列表 / demand entry list
 * @property layers 层列表 / layer list
 * @property assignment 精确赋值 / precise assignment
 * @property overEnabled 是否启用超载 / whether over-load is enabled
 * @property lessEnabled 是否启用欠载 / whether less-load is enabled
 * @property demandValueAdapter 需求值适配器 / demand value adapter
*/
class PreciseLoad(
    override val demandEntries: List<Bpp3dDemandEntry<FltX>>,
    private val layers: List<BinLayer>,
    private val assignment: PreciseAssignment,
    override val overEnabled: Boolean = false,
    override val lessEnabled: Boolean = true,
    override val demandValueAdapter: Bpp3dSolverValueAdapter = DefaultBpp3dSolverValueAdapter
 ) : AbstractLoad() {
    companion object {
        /**
         * 从货物-数量对列表创建精确负载。
         * Create a precise load from item-amount pairs.
         *
         * @param items the item-amount pairs / 货物-数量配对列表
         * @param layers the layer list / 层列表
         * @param assignment the precise assignment / 精确赋值
         * @param overEnabled whether over-load is enabled / 是否启用超载
         * @param lessEnabled whether less-load is enabled / 是否启用欠载
         * @param demandValueAdapter the demand value adapter / 需求值适配器
         * @return the precise load instance / 精确负载实例
        */
        fun fromItems(
            items: List<Pair<Item, UInt64>>,
            layers: List<BinLayer>,
            assignment: PreciseAssignment,
            overEnabled: Boolean = false,
            lessEnabled: Boolean = true,
            demandValueAdapter: Bpp3dSolverValueAdapter = DefaultBpp3dSolverValueAdapter
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

        /**
         * 从货物-数量-范围三元组列表创建精确负载。
         * Create a precise load from item-amount-range triples.
         *
         * @param items the item-amount-range triples / 货物-数量-范围三元组列表
         * @param layers the layer list / 层列表
         * @param assignment the precise assignment / 精确赋值
         * @param overEnabled whether over-load is enabled / 是否启用超载
         * @param lessEnabled whether less-load is enabled / 是否启用欠载
         * @param demandValueAdapter the demand value adapter / 需求值适配器
         * @return the precise load instance / 精确负载实例
        */
        fun fromItemRanges(
            items: List<Triple<Item, UInt64, ValueRange<UInt64>>>,
            layers: List<BinLayer>,
            assignment: PreciseAssignment,
            overEnabled: Boolean = false,
            lessEnabled: Boolean = true,
            demandValueAdapter: Bpp3dSolverValueAdapter = DefaultBpp3dSolverValueAdapter
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

    override lateinit var load: LinearIntermediateSymbols1<FltX>

    override fun register(model: MetaModel<FltX>): Try {
        if (!::load.isInitialized) {
            load = LinearIntermediateSymbols1<FltX>(
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
