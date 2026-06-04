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
 * @property quantity 数量范围 / Quantity range
 * @property lessQuantity 不足数量 / Less quantity
 * @property overQuantity 超量数量 / Over quantity
 */
data class MaterialDemand<V>(
    val quantity: ValueRange<V>,
    val lessQuantity: V? = null,
    val overQuantity: V? = null
) where V : RealNumber<V>, V : NumberField<V> {
    val lessEnabled: Boolean get() = lessQuantity != null
    val overEnabled: Boolean get() = overQuantity != null
}

/** Flt64 材料需求类型别名 / Flt64 material demand type alias */
typealias Flt64MaterialDemand = MaterialDemand<Flt64>

/**
 * 材料储备 / Material reserves
 *
 * @property quantity 数量范围 / Quantity range
 * @property lessQuantity 不足数量 / Less quantity
 * @property overQuantity 超量数量 / Over quantity
 */
open class MaterialReserves<V>(
    val quantity: ValueRange<V>,
    val lessQuantity: V? = null,
    val overQuantity: V? = null,
) where V : RealNumber<V>, V : NumberField<V> {
    val lessEnabled: Boolean get() = lessQuantity != null
    val overEnabled: Boolean get() = overQuantity != null
}

/** Flt64 材料储备类型别名 / Flt64 material reserves type alias */
typealias Flt64MaterialReserves = MaterialReserves<Flt64>

/**
 * 生产任务接口 / Production task interface
 *
 * @param E 执行器类型 / Executor type
 * @param A 分配策略类型 / Assignment policy type
 * @param P 生产材料类型 / Production material type
 * @param C 消耗材料类型 / Consumption material type
 * @property produce 生产量映射 / Produce quantity map
 * @property consumption 消耗量映射 / Consumption quantity map
 */
interface ProductionTask<
        out E : Executor,
        out A : AssignmentPolicy<E>,
        P : AbstractMaterial,
        C : AbstractMaterial,
        V : RealNumber<V>
        > : AbstractTask<E, A> {
    val produce: Map<P, V>
    val consumption: Map<C, V>
}

/** Flt64 生产任务类型别名 / Flt64 production task type alias */
typealias Flt64ProductionTask<E, A, P, C> = ProductionTask<E, A, P, C, Flt64>

private fun <V : RealNumber<V>> AbstractTaskBunch<*, *, *, V>.quantityZero(): V {
    for (task in tasks) {
        if (task is ProductionTask<*, *, *, *, *>) {
            task.produce.values.firstOrNull()?.let {
                @Suppress("UNCHECKED_CAST")
                return (it as V).constants.zero
            }
            task.consumption.values.firstOrNull()?.let {
                @Suppress("UNCHECKED_CAST")
                return (it as V).constants.zero
            }
        }
    }
    cost.sum?.let {
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
                val value = it.produce.entries.firstOrNull { entry -> entry.key == product }?.value
                @Suppress("UNCHECKED_CAST")
                value as? V
            }
            else -> null
        }
    }
    return quantities.sumOrZero(quantities.firstOrNull()?.constants?.zero ?: quantityZero())
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
                val value = it.consumption.entries.firstOrNull { entry -> entry.key == material }?.value
                @Suppress("UNCHECKED_CAST")
                value as? V
            }
            else -> null
        }
    }
    return quantities.sumOrZero(quantities.firstOrNull()?.constants?.zero ?: quantityZero())
}
