@file:Suppress("DEPRECATION")

/** 生产任务模型 / Production task model */
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce.model

import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AbstractTask
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AbstractTaskBunch
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AssignmentPolicy
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.Executor
import fuookami.ospf.kotlin.utils.concept.Indexed
import fuookami.ospf.kotlin.utils.functional.sumOf
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.NoneUnit
import fuookami.ospf.kotlin.quantities.unit.PhysicalUnit

/** 材料接口 / Material interface */
interface Material : AbstractMaterial {
    override val material get() = this
}

/**
 * 抽象材料接口 / Abstract material interface
 *
 * @property material 材料 / Material
 */
interface AbstractMaterial : Indexed {
    val material: Material
}

/** 产品接口 / Product interface */
interface Product : Material
/** 半产品接口 / Semi-product interface */
interface SemiProduct : Material
/** 原材料接口 / Raw material interface */
interface RawMaterial : Material

/**
 * 材料需求 / Material demand
 *
 * @property quantityRangeValue 数量范围物理量 / Quantity range value
 * @property lessQuantityValue 不足数量物理量 / Less quantity value
 * @property overQuantityValue 超量数量物理量 / Over quantity value
 */
data class MaterialDemand<V>(
    val quantityRangeValue: MaterialQuantityRange<V>,
    val lessQuantityValue: MaterialQuantity<V>? = null,
    val overQuantityValue: MaterialQuantity<V>? = null
) where V : RealNumber<V>, V : NumberField<V> {
    @Deprecated(
        message = "Use the Quantity-typed primary constructor instead",
        replaceWith = ReplaceWith("MaterialDemand(Quantity(quantity, NoneUnit), lessQuantity?.let { Quantity(it, NoneUnit) }, overQuantity?.let { Quantity(it, NoneUnit) })")
    )
    constructor(
        quantity: ValueRange<V>,
        lessQuantity: V? = null,
        overQuantity: V? = null
    ) : this(
        quantityRangeValue = Quantity(quantity, NoneUnit),
        lessQuantityValue = lessQuantity?.let { Quantity(it, NoneUnit) },
        overQuantityValue = overQuantity?.let { Quantity(it, NoneUnit) }
    )

    @Deprecated(
        message = "Use the Quantity-typed property instead",
        replaceWith = ReplaceWith("quantityRangeValue.value")
    )
    val quantity: ValueRange<V> get() = quantityRangeValue.value
    @Deprecated(
        message = "Use the Quantity-typed property instead",
        replaceWith = ReplaceWith("lessQuantityValue?.value")
    )
    val lessQuantity: V? get() = lessQuantityValue?.value
    @Deprecated(
        message = "Use the Quantity-typed property instead",
        replaceWith = ReplaceWith("overQuantityValue?.value")
    )
    val overQuantity: V? get() = overQuantityValue?.value
    val lessEnabled: Boolean get() = lessQuantityValue != null
    val overEnabled: Boolean get() = overQuantityValue != null

    /**
     * 数量范围物理量 / Quantity range as a physical quantity
     *
     * @param unit 数量单位 / Quantity unit
     * @return 数量范围物理量 / Quantity range quantity
     */
    fun quantityRange(unit: PhysicalUnit = NoneUnit): MaterialQuantityRange<V> {
        return Quantity(quantityRangeValue.value, unit)
    }

    /**
     * 不足数量物理量 / Less quantity as a physical quantity
     *
     * @param unit 数量单位 / Quantity unit
     * @return 不足数量物理量 / Less quantity
     */
    fun lessQuantity(unit: PhysicalUnit = NoneUnit): MaterialQuantity<V>? {
        return lessQuantityValue?.let { Quantity(it.value, unit) }
    }

    /**
     * 超量数量物理量 / Over quantity as a physical quantity
     *
     * @param unit 数量单位 / Quantity unit
     * @return 超量数量物理量 / Over quantity
     */
    fun overQuantity(unit: PhysicalUnit = NoneUnit): MaterialQuantity<V>? {
        return overQuantityValue?.let { Quantity(it.value, unit) }
    }
}

/** Flt64 材料需求类型别名 / Flt64 material demand type alias */
@Deprecated("Use MaterialDemand<Flt64> directly") typealias Flt64MaterialDemand = MaterialDemand<Flt64>

/**
 * 材料储备 / Material reserves
 *
 * @property quantityRangeValue 数量范围物理量 / Quantity range value
 * @property lessQuantityValue 不足数量物理量 / Less quantity value
 * @property overQuantityValue 超量数量物理量 / Over quantity value
 */
open class MaterialReserves<V>(
    val quantityRangeValue: MaterialQuantityRange<V>,
    val lessQuantityValue: MaterialQuantity<V>? = null,
    val overQuantityValue: MaterialQuantity<V>? = null,
) where V : RealNumber<V>, V : NumberField<V> {
    @Deprecated(
        message = "Use the Quantity-typed primary constructor instead",
        replaceWith = ReplaceWith("MaterialReserves(Quantity(quantity, NoneUnit), lessQuantity?.let { Quantity(it, NoneUnit) }, overQuantity?.let { Quantity(it, NoneUnit) })")
    )
    constructor(
        quantity: ValueRange<V>,
        lessQuantity: V? = null,
        overQuantity: V? = null
    ) : this(
        quantityRangeValue = Quantity(quantity, NoneUnit),
        lessQuantityValue = lessQuantity?.let { Quantity(it, NoneUnit) },
        overQuantityValue = overQuantity?.let { Quantity(it, NoneUnit) }
    )

    @Deprecated(
        message = "Use the Quantity-typed property instead",
        replaceWith = ReplaceWith("quantityRangeValue.value")
    )
    val quantity: ValueRange<V> get() = quantityRangeValue.value
    @Deprecated(
        message = "Use the Quantity-typed property instead",
        replaceWith = ReplaceWith("lessQuantityValue?.value")
    )
    val lessQuantity: V? get() = lessQuantityValue?.value
    @Deprecated(
        message = "Use the Quantity-typed property instead",
        replaceWith = ReplaceWith("overQuantityValue?.value")
    )
    val overQuantity: V? get() = overQuantityValue?.value
    val lessEnabled: Boolean get() = lessQuantityValue != null
    val overEnabled: Boolean get() = overQuantityValue != null

    /**
     * 数量范围物理量 / Quantity range as a physical quantity
     *
     * @param unit 数量单位 / Quantity unit
     * @return 数量范围物理量 / Quantity range quantity
     */
    fun quantityRange(unit: PhysicalUnit = NoneUnit): MaterialQuantityRange<V> {
        return Quantity(quantityRangeValue.value, unit)
    }

    /**
     * 不足数量物理量 / Less quantity as a physical quantity
     *
     * @param unit 数量单位 / Quantity unit
     * @return 不足数量物理量 / Less quantity
     */
    fun lessQuantity(unit: PhysicalUnit = NoneUnit): MaterialQuantity<V>? {
        return lessQuantityValue?.let { Quantity(it.value, unit) }
    }

    /**
     * 超量数量物理量 / Over quantity as a physical quantity
     *
     * @param unit 数量单位 / Quantity unit
     * @return 超量数量物理量 / Over quantity
     */
    fun overQuantity(unit: PhysicalUnit = NoneUnit): MaterialQuantity<V>? {
        return overQuantityValue?.let { Quantity(it.value, unit) }
    }
}

/** Flt64 材料储备类型别名 / Flt64 material reserves type alias */
@Deprecated("Use MaterialReserves<Flt64> directly") typealias Flt64MaterialReserves = MaterialReserves<Flt64>

/**
 * 生产任务接口 / Production task interface
 *
 * @param E 执行器类型 / Executor type
 * @param A 分配策略类型 / Assignment policy type
 * @param P 生产材料类型 / Production material type
 * @param C 消耗材料类型 / Consumption material type
 * @property produce 生产量映射 / Produce quantity map
 * @property consumption 消耗量映射 / Consumption quantity map
 * @property produceQuantityByProduct 生产量物理量映射 / Produce quantity map with units
 * @property consumptionQuantityByMaterial 消耗量物理量映射 / Consumption quantity map with units
 */
interface ProductionTask<
        out E : Executor,
        out A : AssignmentPolicy<E>,
        P : AbstractMaterial,
        C : AbstractMaterial,
        V : RealNumber<V>
        > : AbstractTask<E, A> {
    @Deprecated(
        message = "Use the Quantity-typed property instead",
        replaceWith = ReplaceWith("produceQuantityByProduct")
    )
    val produce: Map<P, V>
    @Deprecated(
        message = "Use the Quantity-typed property instead",
        replaceWith = ReplaceWith("consumptionQuantityByMaterial")
    )
    val consumption: Map<C, V>

    /** 生产量物理量映射 / Produce quantity map with units */
    val produceQuantityByProduct: Map<P, MaterialQuantity<V>>
        get() = produce.mapValues { (_, value) -> Quantity(value, NoneUnit) }

    /** 消耗量物理量映射 / Consumption quantity map with units */
    val consumptionQuantityByMaterial: Map<C, MaterialQuantity<V>>
        get() = consumption.mapValues { (_, value) -> Quantity(value, NoneUnit) }

    /**
     * 生产量物理量 / Produce quantity as a physical quantity
     *
     * @param product 产品 / Product
     * @param unit 数量单位 / Quantity unit
     * @return 生产量物理量 / Produce quantity
     */
    fun produceQuantity(product: P, unit: PhysicalUnit = NoneUnit): MaterialQuantity<V>? {
        return produceQuantityByProduct[product]?.let { Quantity(it.value, unit) }
    }

    /**
     * 消耗量物理量 / Consumption quantity as a physical quantity
     *
     * @param material 材料 / Material
     * @param unit 数量单位 / Quantity unit
     * @return 消耗量物理量 / Consumption quantity
     */
    fun consumptionQuantity(material: C, unit: PhysicalUnit = NoneUnit): MaterialQuantity<V>? {
        return consumptionQuantityByMaterial[material]?.let { Quantity(it.value, unit) }
    }
}

/** Flt64 生产任务类型别名 / Flt64 production task type alias */
@Deprecated("Use ProductionTask<E, A, P, C, Flt64> directly") typealias Flt64ProductionTask<E, A, P, C> = ProductionTask<E, A, P, C, Flt64>

private fun <V : RealNumber<V>> AbstractTaskBunch<*, *, *, V>.quantityZero(): V {
    for (task in tasks) {
        if (task is ProductionTask<*, *, *, *, *>) {
            task.produceQuantityByProduct.values.firstOrNull()?.let {
                @Suppress("UNCHECKED_CAST")
                return (it.value as V).constants.zero
            }
            task.consumptionQuantityByMaterial.values.firstOrNull()?.let {
                @Suppress("UNCHECKED_CAST")
                return (it.value as V).constants.zero
            }
        }
    }
    cost.costSum?.value?.let {
        return it.constants.zero
    }
    throw IllegalArgumentException("production task bunch must contain at least one quantity or cost value to resolve zero.")
}

private fun <V : RealNumber<V>> List<V>.sumOrZero(zero: V): V {
    return fold(zero) { acc, value -> acc + value }
}

/**
 * 读取 Flt64 兼容生产量 / Read Flt64-compatible produce quantity
 *
 * @param P 生产材料类型 / Production material type
 * @param product 产品 / Product
 * @return 生产量 / Produce quantity
 */
fun <P : AbstractMaterial> ProductionTask<*, *, *, *, *>.flt64Produce(product: P): Flt64? {
    return produce.entries.firstOrNull { it.key == product }?.value as? Flt64
}

/**
 * 读取非零 Flt64 兼容生产物料 / Read non-zero Flt64-compatible produce materials
 *
 * @param P 生产材料类型 / Production material type
 * @return 非零生产物料 / Non-zero produce materials
 */
fun <P : AbstractMaterial> ProductionTask<*, *, *, *, *>.nonZeroFlt64ProduceMaterials(): List<P> {
    return produce.mapNotNull { (product, quantity) ->
        val value = quantity as? Flt64 ?: return@mapNotNull null
        if (value neq Flt64.zero) {
            @Suppress("UNCHECKED_CAST")
            product as P
        } else {
            null
        }
    }
}

/**
 * 读取非零生产物料 / Read non-zero produce materials
 *
 * @param P 生产材料类型 / Production material type
 * @return 非零生产物料 / Non-zero produce materials
 */
fun <P : AbstractMaterial> ProductionTask<*, *, *, *, *>.nonZeroProduceMaterials(): List<P> {
    return produce.mapNotNull { (product, quantity) ->
        if (quantity.toFlt64() neq Flt64.zero) {
            @Suppress("UNCHECKED_CAST")
            product as P
        } else {
            null
        }
    }
}

/**
 * 读取 Flt64 兼容消耗量 / Read Flt64-compatible consumption quantity
 *
 * @param C 消耗材料类型 / Consumption material type
 * @param material 材料 / Material
 * @return 消耗量 / Consumption quantity
 */
fun <C : AbstractMaterial> ProductionTask<*, *, *, *, *>.flt64Consumption(material: C): Flt64? {
    return consumption.entries.firstOrNull { it.key == material }?.value as? Flt64
}

/**
 * 读取非零 Flt64 兼容消耗物料 / Read non-zero Flt64-compatible consumption materials
 *
 * @param C 消耗材料类型 / Consumption material type
 * @return 非零消耗物料 / Non-zero consumption materials
 */
fun <C : AbstractMaterial> ProductionTask<*, *, *, *, *>.nonZeroFlt64ConsumptionMaterials(): List<C> {
    return consumption.mapNotNull { (material, quantity) ->
        val value = quantity as? Flt64 ?: return@mapNotNull null
        if (value neq Flt64.zero) {
            @Suppress("UNCHECKED_CAST")
            material as C
        } else {
            null
        }
    }
}

/**
 * 读取非零消耗物料 / Read non-zero consumption materials
 *
 * @param C 消耗材料类型 / Consumption material type
 * @return 非零消耗物料 / Non-zero consumption materials
 */
fun <C : AbstractMaterial> ProductionTask<*, *, *, *, *>.nonZeroConsumptionMaterials(): List<C> {
    return consumption.mapNotNull { (material, quantity) ->
        if (quantity.toFlt64() neq Flt64.zero) {
            @Suppress("UNCHECKED_CAST")
            material as C
        } else {
            null
        }
    }
}

/**
 * 计算任务束的生产量 / Calculate bunch produce quantity
 *
 * @param T 任务类型 / Task type
 * @param E 执行器类型 / Executor type
 * @param A 分配策略类型 / Assignment policy type
 * @param P 生产材料类型 / Production material type
 * @param product 产品 / Product
 * @return 生产量 / Produce quantity
 */
fun <
        T : AbstractTask<E, A>,
        E : Executor,
        A : AssignmentPolicy<E>,
        P : AbstractMaterial
        > AbstractTaskBunch<T, E, A, Flt64>.produce(product: P): Flt64 {
    return tasks.mapNotNull {
        when (it) {
            is ProductionTask<*, *, *, *, *> -> it.flt64Produce(product)

            else -> {
                null
            }
        }
    }.sumOf { it }
}

/**
 * 计算任务束的生产量（泛型版本）/ Calculate bunch produce quantity (generic version)
 *
 * @param T 任务类型 / Task type
 * @param E 执行器类型 / Executor type
 * @param A 分配策略类型 / Assignment policy type
 * @param P 生产材料类型 / Production material type
 * @param V 值类型 / Value type
 * @param product 产品 / Product
 * @return 生产量 / Produce quantity
 */
fun <
        T : AbstractTask<E, A>,
        E : Executor,
        A : AssignmentPolicy<E>,
        P : AbstractMaterial,
        V : RealNumber<V>
        > AbstractTaskBunch<T, E, A, V>.produceV(product: P): V {
    val quantities = tasks.mapNotNull {
        when (it) {
            is ProductionTask<*, *, *, *, *> -> {
                val value = it.produceQuantityByProduct.entries.firstOrNull { entry -> entry.key == product }?.value?.value
                @Suppress("UNCHECKED_CAST")
                value as? V
            }
            else -> null
        }
    }
    return quantities.sumOrZero(quantities.firstOrNull()?.constants?.zero ?: quantityZero())
}

/**
 * 计算任务束的生产量物理量 / Calculate bunch produce quantity as a physical quantity
 *
 * @param T 任务类型 / Task type
 * @param E 执行器类型 / Executor type
 * @param A 分配策略类型 / Assignment policy type
 * @param P 生产材料类型 / Production material type
 * @param V 值类型 / Value type
 * @param product 产品 / Product
 * @param unit 数量单位 / Quantity unit
 * @return 生产量物理量 / Produce quantity
 */
fun <
        T : AbstractTask<E, A>,
        E : Executor,
        A : AssignmentPolicy<E>,
        P : AbstractMaterial,
        V : RealNumber<V>
        > AbstractTaskBunch<T, E, A, V>.produceQuantityV(
    product: P,
    unit: PhysicalUnit = NoneUnit
): MaterialQuantity<V> {
    return Quantity(produceV(product), unit)
}

/**
 * 计算任务束的消耗量 / Calculate bunch consumption quantity
 *
 * @param T 任务类型 / Task type
 * @param E 执行器类型 / Executor type
 * @param A 分配策略类型 / Assignment policy type
 * @param C 消耗材料类型 / Consumption material type
 * @param material 材料 / Material
 * @return 消耗量 / Consumption quantity
 */
fun <
        T : AbstractTask<E, A>,
        E : Executor,
        A : AssignmentPolicy<E>,
        C : AbstractMaterial
        > AbstractTaskBunch<T, E, A, Flt64>.consumption(material: C): Flt64 {
    return tasks.mapNotNull {
        when (it) {
            is ProductionTask<*, *, *, *, *> -> it.flt64Consumption(material)

            else -> {
                null
            }
        }
    }.sumOf { it }
}

/**
 * 计算任务束的消耗量（泛型版本）/ Calculate bunch consumption quantity (generic version)
 *
 * @param T 任务类型 / Task type
 * @param E 执行器类型 / Executor type
 * @param A 分配策略类型 / Assignment policy type
 * @param C 消耗材料类型 / Consumption material type
 * @param V 值类型 / Value type
 * @param material 材料 / Material
 * @return 消耗量 / Consumption quantity
 */
fun <
        T : AbstractTask<E, A>,
        E : Executor,
        A : AssignmentPolicy<E>,
        C : AbstractMaterial,
        V : RealNumber<V>
        > AbstractTaskBunch<T, E, A, V>.consumptionV(material: C): V {
    val quantities = tasks.mapNotNull {
        when (it) {
            is ProductionTask<*, *, *, *, *> -> {
                val value = it.consumptionQuantityByMaterial.entries.firstOrNull { entry -> entry.key == material }?.value?.value
                @Suppress("UNCHECKED_CAST")
                value as? V
            }
            else -> null
        }
    }
    return quantities.sumOrZero(quantities.firstOrNull()?.constants?.zero ?: quantityZero())
}

/**
 * 计算任务束的消耗量物理量 / Calculate bunch consumption quantity as a physical quantity
 *
 * @param T 任务类型 / Task type
 * @param E 执行器类型 / Executor type
 * @param A 分配策略类型 / Assignment policy type
 * @param C 消耗材料类型 / Consumption material type
 * @param V 值类型 / Value type
 * @param material 材料 / Material
 * @param unit 数量单位 / Quantity unit
 * @return 消耗量物理量 / Consumption quantity
 */
fun <
        T : AbstractTask<E, A>,
        E : Executor,
        A : AssignmentPolicy<E>,
        C : AbstractMaterial,
        V : RealNumber<V>
        > AbstractTaskBunch<T, E, A, V>.consumptionQuantityV(
    material: C,
    unit: PhysicalUnit = NoneUnit
): MaterialQuantity<V> {
    return Quantity(consumptionV(material), unit)
}
