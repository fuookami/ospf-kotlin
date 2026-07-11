/**
 * 生产任务模型 / Production task model
 *
 * 本文件定义材料接口、产品类型、材料需求与储备、以及生产任务接口。
 * This file defines material interfaces, product types, material demand and reserves, and production task interface.
*/
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce.model

import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.error.GanttSchedulingValidationError
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.NoneUnit
import fuookami.ospf.kotlin.quantities.unit.PhysicalUnit
import fuookami.ospf.kotlin.utils.concept.Indexed
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

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

    /** 生产量物理量映射 / Produce quantity map with units */
    val produceQuantityByProduct: Map<P, MaterialQuantity<V>>

    /** 消耗量物理量映射 / Consumption quantity map with units */
    val consumptionQuantityByMaterial: Map<C, MaterialQuantity<V>>

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

private fun <V : RealNumber<V>> AbstractTaskBunch<*, *, *, V>.quantityZero(): Ret<V> {
    for (task in tasks) {
        if (task is ProductionTask<*, *, *, *, *>) {
            task.produceQuantityByProduct.values.firstOrNull()?.let {
                @Suppress("UNCHECKED_CAST")
                return Ok((it.value as V).constants.zero)
            }
            task.consumptionQuantityByMaterial.values.firstOrNull()?.let {
                @Suppress("UNCHECKED_CAST")
                return Ok((it.value as V).constants.zero)
            }
        }
    }
    cost.costSum?.value?.let {
        return Ok(it.constants.zero)
    }
    return Failed(GanttSchedulingValidationError("production task bunch must contain at least one quantity or cost value to resolve zero."))
}

private fun <V : RealNumber<V>> List<V>.sumOrZero(zero: V): V {
    return fold(zero) { acc, value -> acc + value }
}

/**
 * Quantity.
 * Quantity。
 * @return Whether the quantity value is non-zero / 物理量值是否非零
*/
private fun Quantity<*>.isNonZero(): Boolean {
    val realValue = value as? RealNumber<*> ?: return false
    return realValue != realValue.constants.zero
}

/**
 * 读取非零生产物料 / Read non-zero produce materials
 *
 * @param P 生产材料类型 / Production material type
 * @return 非零生产物料 / Non-zero produce materials
*/
fun <P : AbstractMaterial> ProductionTask<*, *, *, *, *>.nonZeroProduceMaterials(): List<P> {
    return produceQuantityByProduct.mapNotNull { (product, quantity) ->
        if (quantity.isNonZero()) {
            @Suppress("UNCHECKED_CAST")
            product as P
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
    return consumptionQuantityByMaterial.mapNotNull { (material, quantity) ->
        if (quantity.isNonZero()) {
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
        > AbstractTaskBunch<T, E, A, V>.produce(product: P): Ret<V> {
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
    val zero = quantities.firstOrNull()?.constants?.zero
        ?: return when (val result = quantityZero()) {
            is Ok -> Ok(quantities.sumOrZero(result.value))
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    return Ok(quantities.sumOrZero(zero))
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
        > AbstractTaskBunch<T, E, A, V>.produceQuantity(
    product: P,
    unit: PhysicalUnit = NoneUnit
): MaterialQuantity<V> {
    return Quantity((produce(product) as Ok).value, unit)
}

/**
 * 计算任务束的消耗量 / Calculate bunch consumption quantity
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
        > AbstractTaskBunch<T, E, A, V>.consumption(material: C): Ret<V> {
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
    val zero = quantities.firstOrNull()?.constants?.zero
        ?: return when (val result = quantityZero()) {
            is Ok -> Ok(quantities.sumOrZero(result.value))
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    return Ok(quantities.sumOrZero(zero))
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
        > AbstractTaskBunch<T, E, A, V>.consumptionQuantity(
    material: C,
    unit: PhysicalUnit = NoneUnit
): MaterialQuantity<V> {
    return Quantity((consumption(material) as Ok).value, unit)
}
