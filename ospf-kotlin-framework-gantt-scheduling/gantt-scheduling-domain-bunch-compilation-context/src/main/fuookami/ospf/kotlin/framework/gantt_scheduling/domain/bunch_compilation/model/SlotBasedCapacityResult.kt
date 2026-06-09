@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.model

import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model.ActionAllocation
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model.ProductionAction
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.SchedulingSolverValueAdapter
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeSlot
import fuookami.ospf.kotlin.math.algebra.concept.PlusGroup
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.NoneUnit
import fuookami.ospf.kotlin.quantities.unit.PhysicalUnit

/** 时隙成本物理量 / Slot cost quantity */
typealias SlotCostQuantity<V> = Quantity<V>

/** 时隙数量物理量 / Slot quantity */
typealias SlotQuantity<V> = Quantity<V>

/**
 * 分时隙产能结果
 * Slot-based capacity result
 *
 * 存储单个时隙的产能分配结果和中间数值。
 * Stores capacity allocation results and intermediate values for a single time slot.
 *
 * @param A 生产动作类型 / Production action type
 * @param M 物料类型 / Material type
 * @param R 资源容量类型 / Resource capacity type
 * @param V 数值类型 / Numeric type for quantity values
 * @property slot 所属时隙 / The time slot
 * @property slotIndex 时隙索引 / Slot index
 * @property actionAllocations 该时隙内的动作分配 / Action allocations in this slot
 * @property totalCostQuantityValue 该时隙的总成本物理量 / Total cost quantity for this slot
 * @property produceQuantityByProduct 该时隙的产品产量物理量 / Product production quantities in this slot
 * @property consumptionQuantityByMaterial 该时隙的原料消耗物理量 / Material consumption quantities in this slot
 * @property resourceUsageQuantityByResource 该时隙的资源使用量物理量 / Resource usage quantities in this slot
 */
data class SlotBasedCapacityResult<A : ProductionAction, M, R, V>(
    /**
     * 所属时隙
     * The time slot
     */
    val slot: TimeSlot,

    /**
     * 时隙索引
     * Slot index
     */
    val slotIndex: Int,

    /**
     * 该时隙内的动作分配
     * Action allocations in this slot
     */
    val actionAllocations: List<ActionAllocation<A>>,

    /**
     * 该时隙的总成本物理量
     * Total cost quantity for this slot
     */
    val totalCostQuantityValue: SlotCostQuantity<V>,

    /**
     * 该时隙的产品产量（按产品）
     * Product production by product in this slot
     */
    val produceQuantityByProduct: Map<M, SlotQuantity<V>>,

    /**
     * 该时隙的原料消耗（按原料）
     * Material consumption by material in this slot
     */
    val consumptionQuantityByMaterial: Map<M, SlotQuantity<V>>,

    /**
     * 该时隙的资源使用量（按资源）
     * Resource usage by resource in this slot
     */
    val resourceUsageQuantityByResource: Map<R, SlotQuantity<V>>
) where V : RealNumber<V>, V : PlusGroup<V> {
    @Deprecated(
        message = "Use primary constructor with Quantity-typed fields",
        replaceWith = ReplaceWith(
            "SlotBasedCapacityResult(slot, slotIndex, actionAllocations, Quantity(totalCost, NoneUnit), produceByProduct.mapValues { (_, v) -> Quantity(v, NoneUnit) }, consumptionByMaterial.mapValues { (_, v) -> Quantity(v, NoneUnit) }, resourceUsageByResource.mapValues { (_, v) -> Quantity(v, NoneUnit) })",
            "fuookami.ospf.kotlin.quantities.quantity.Quantity",
            "fuookami.ospf.kotlin.quantities.unit.NoneUnit"
        )
    )
    constructor(
        slot: TimeSlot,
        slotIndex: Int,
        actionAllocations: List<ActionAllocation<A>>,
        totalCost: V,
        produceByProduct: Map<M, V>,
        consumptionByMaterial: Map<M, V>,
        resourceUsageByResource: Map<R, V>
    ) : this(
        slot = slot,
        slotIndex = slotIndex,
        actionAllocations = actionAllocations,
        totalCostQuantityValue = Quantity(totalCost, NoneUnit),
        produceQuantityByProduct = produceByProduct.mapValues { (_, value) -> Quantity(value, NoneUnit) },
        consumptionQuantityByMaterial = consumptionByMaterial.mapValues { (_, value) -> Quantity(value, NoneUnit) },
        resourceUsageQuantityByResource = resourceUsageByResource.mapValues { (_, value) -> Quantity(value, NoneUnit) }
    )

    /** 该时隙的总成本裸值兼容属性 / Raw total cost compatibility property */
    @Deprecated("Use totalCostQuantityValue instead", replaceWith = ReplaceWith("totalCostQuantityValue.value"))
    val totalCost: V get() = totalCostQuantityValue.value

    /** 该时隙的产品产量裸值兼容映射 / Raw produce quantity compatibility map */
    @Deprecated("Use produceQuantityByProduct instead", replaceWith = ReplaceWith("produceQuantityByProduct.mapValues { (_, q) -> q.value }"))
    val produceByProduct: Map<M, V> get() = produceQuantityByProduct.mapValues { (_, quantity) -> quantity.value }

    /** 该时隙的原料消耗裸值兼容映射 / Raw consumption quantity compatibility map */
    @Deprecated("Use consumptionQuantityByMaterial instead", replaceWith = ReplaceWith("consumptionQuantityByMaterial.mapValues { (_, q) -> q.value }"))
    val consumptionByMaterial: Map<M, V> get() = consumptionQuantityByMaterial.mapValues { (_, quantity) -> quantity.value }

    /** 该时隙的资源使用量裸值兼容映射 / Raw resource usage quantity compatibility map */
    @Deprecated("Use resourceUsageQuantityByResource instead", replaceWith = ReplaceWith("resourceUsageQuantityByResource.mapValues { (_, q) -> q.value }"))
    val resourceUsageByResource: Map<R, V> get() = resourceUsageQuantityByResource.mapValues { (_, quantity) -> quantity.value }

    /**
     * 时隙总成本物理量 / Slot total cost quantity
     *
     * @param unit 成本单位 / Cost unit
     * @return 时隙总成本物理量 / Slot total cost quantity
     */
    fun totalCostQuantity(unit: PhysicalUnit = NoneUnit): SlotCostQuantity<V> {
        return Quantity(totalCostQuantityValue.value, unit)
    }
}

/**
 * 将 Flt64 时隙产能结果转换为泛型物理量结果 / Convert a Flt64 slot capacity result to a generic quantity result
 *
 * @param A 生产动作类型 / Production action type
 * @param M 物料类型 / Material type
 * @param R 资源容量类型 / Resource capacity type
 * @param V 目标数值类型 / Target numeric type
 * @param adapter solver 数值适配器 / Solver value adapter
 * @return 泛型时隙产能结果 / Generic slot capacity result
 */
fun <A : ProductionAction, M, R, V> SlotBasedCapacityResult<A, M, R, Flt64>.toGeneric(
    adapter: SchedulingSolverValueAdapter<V>
): SlotBasedCapacityResult<A, M, R, V> where V : RealNumber<V>, V : PlusGroup<V> {
    return SlotBasedCapacityResult(
        slot = slot,
        slotIndex = slotIndex,
        actionAllocations = actionAllocations,
        totalCostQuantityValue = totalCostQuantityValue.toGeneric(adapter),
        produceQuantityByProduct = produceQuantityByProduct.mapValues { (_, quantity) -> quantity.toGeneric(adapter) },
        consumptionQuantityByMaterial = consumptionQuantityByMaterial.mapValues { (_, quantity) -> quantity.toGeneric(adapter) },
        resourceUsageQuantityByResource = resourceUsageQuantityByResource.mapValues { (_, quantity) -> quantity.toGeneric(adapter) }
    )
}

/**
 * 产能中间值集合
 * Capacity intermediate values collection
 *
 * 聚合所有时隙的产能结果，提供查询接口。
 * Aggregates capacity results for all slots, provides query interface.
 *
 * @param A 生产动作类型 / Production action type
 * @param M 物料类型 / Material type
 * @param R 资源容量类型 / Resource capacity type
 * @param V 数值类型 / Numeric type for quantity values
 */
class CapacityIntermediateValues<A : ProductionAction, M, R, V>(
    /**
     * 时隙列表
     * List of time slots
     */
    val slots: List<TimeSlot>,

    /**
     * 各时隙的产能结果
     * Capacity results by slot
     */
    val results: Map<TimeSlot, SlotBasedCapacityResult<A, M, R, V>>
) where V : RealNumber<V>, V : PlusGroup<V> {
    /**
     * 获取指定时隙的产品产量
     * Get product production for specified slot
     *
     * @param slot The time slot / 时隙
     * @param product The product / 产品
     * @return Production amount / 产量
     */
    @Deprecated("Use produceQuantity instead", replaceWith = ReplaceWith("produceQuantity(slot, product)?.value"))
    fun produce(slot: TimeSlot, product: M): V? {
        return results[slot]?.produceByProduct?.get(product)
    }

    /**
     * 获取指定时隙的产品产量物理量
     * Get product production quantity for specified slot
     *
     * @param slot The time slot / 时隙
     * @param product The product / 产品
     * @return Production quantity / 产量物理量
     */
    fun produceQuantity(slot: TimeSlot, product: M): SlotQuantity<V>? {
        return results[slot]?.produceQuantityByProduct?.get(product)
    }

    /**
     * 获取指定时隙的原料消耗
     * Get material consumption for specified slot
     *
     * @param slot The time slot / 时隙
     * @param material The material / 原料
     * @return Consumption amount / 消耗量
     */
    @Deprecated("Use consumptionQuantity instead", replaceWith = ReplaceWith("consumptionQuantity(slot, material)?.value"))
    fun consumption(slot: TimeSlot, material: M): V? {
        return results[slot]?.consumptionByMaterial?.get(material)
    }

    /**
     * 获取指定时隙的原料消耗物理量
     * Get material consumption quantity for specified slot
     *
     * @param slot The time slot / 时隙
     * @param material The material / 原料
     * @return Consumption quantity / 消耗物理量
     */
    fun consumptionQuantity(slot: TimeSlot, material: M): SlotQuantity<V>? {
        return results[slot]?.consumptionQuantityByMaterial?.get(material)
    }

    /**
     * 获取指定时隙的资源使用量
     * Get resource usage for specified slot
     *
     * @param slot The time slot / 时隙
     * @param resource The resource capacity / 资源容量
     * @return Resource usage amount / 资源使用量
     */
    @Deprecated("Use resourceUsageQuantity instead", replaceWith = ReplaceWith("resourceUsageQuantity(slot, resource)?.value"))
    fun resourceUsage(slot: TimeSlot, resource: R): V? {
        return results[slot]?.resourceUsageByResource?.get(resource)
    }

    /**
     * 获取指定时隙的资源使用量物理量
     * Get resource usage quantity for specified slot
     *
     * @param slot The time slot / 时隙
     * @param resource The resource capacity / 资源容量
     * @return Resource usage quantity / 资源使用量物理量
     */
    fun resourceUsageQuantity(slot: TimeSlot, resource: R): SlotQuantity<V>? {
        return results[slot]?.resourceUsageQuantityByResource?.get(resource)
    }

    /**
     * 获取指定时隙的所有约束
     * Get all constraints for specified slot
     *
     * @param slot The time slot / 时隙
     * @param tolerance Tolerance for constraint bounds / 约束边界的容差
     * @return Slot constraints / 时隙约束
     */
    fun slotConstraints(slot: TimeSlot, tolerance: V? = null): SlotConstraints<M, R, V>? {
        val result = results[slot] ?: return null
        return SlotConstraints.from(result, tolerance)
    }
}

/**
 * 将 Flt64 产能中间值集合转换为泛型物理量集合 / Convert Flt64 capacity intermediate values to generic quantity values
 *
 * @param A 生产动作类型 / Production action type
 * @param M 物料类型 / Material type
 * @param R 资源容量类型 / Resource capacity type
 * @param V 目标数值类型 / Target numeric type
 * @param adapter solver 数值适配器 / Solver value adapter
 * @return 泛型产能中间值集合 / Generic capacity intermediate values
 */
fun <A : ProductionAction, M, R, V> CapacityIntermediateValues<A, M, R, Flt64>.toGeneric(
    adapter: SchedulingSolverValueAdapter<V>
): CapacityIntermediateValues<A, M, R, V> where V : RealNumber<V>, V : PlusGroup<V> {
    return CapacityIntermediateValues(
        slots = slots,
        results = results.mapValues { (_, result) -> result.toGeneric(adapter) }
    )
}

/**
 * 时隙约束
 * Slot constraints
 *
 * 描述单个时隙的资源、产量、消耗约束边界。
 * Describes resource, produce, consumption constraint boundaries for a single slot.
 *
 * @param M 物料类型 / Material type
 * @param R 资源容量类型 / Resource capacity type
 * @param V 数值类型 / Numeric type for quantity values
 * @property slot 所属时隙 / The time slot
 * @property slotIndex 时隙索引 / Slot index
 * @property maxProduceQuantity 产品产量上限物理量 / Maximum production quantities by product
 * @property minProduceQuantity 产品产量下限物理量 / Minimum production quantities by product
 * @property maxConsumptionQuantity 原料消耗上限物理量 / Maximum consumption quantities by material
 * @property minConsumptionQuantity 原料消耗下限物理量 / Minimum consumption quantities by material
 * @property maxResourceUsageQuantity 资源使用量上限物理量 / Maximum resource usage quantities by resource
 * @property minResourceUsageQuantity 资源使用量下限物理量 / Minimum resource usage quantities by resource
 */
data class SlotConstraints<M, R, V>(
    /**
     * 所属时隙
     * The time slot
     */
    val slot: TimeSlot,

    /**
     * 时隙索引
     * Slot index
     */
    val slotIndex: Int,

    /**
     * 产品产量上限物理量
     * Maximum production quantities by product
     */
    val maxProduceQuantity: Map<M, SlotQuantity<V>>,

    /**
     * 产品产量下限物理量
     * Minimum production quantities by product
     */
    val minProduceQuantity: Map<M, SlotQuantity<V>>,

    /**
     * 原料消耗上限物理量
     * Maximum consumption quantities by material
     */
    val maxConsumptionQuantity: Map<M, SlotQuantity<V>>,

    /**
     * 原料消耗下限物理量
     * Minimum consumption quantities by material
     */
    val minConsumptionQuantity: Map<M, SlotQuantity<V>>,

    /**
     * 资源使用量上限物理量
     * Maximum resource usage quantities by resource
     */
    val maxResourceUsageQuantity: Map<R, SlotQuantity<V>>,

    /**
     * 资源使用量下限物理量
     * Minimum resource usage quantities by resource
     */
    val minResourceUsageQuantity: Map<R, SlotQuantity<V>>
) where V : RealNumber<V>, V : PlusGroup<V> {
    /** 产品产量上限裸值兼容映射 / Raw max produce compatibility map */
    @Deprecated("Use maxProduceQuantity instead", replaceWith = ReplaceWith("maxProduceQuantity.mapValues { (_, q) -> q.value }"))
    val maxProduce: Map<M, V> get() = maxProduceQuantity.mapValues { (_, quantity) -> quantity.value }

    /** 产品产量下限裸值兼容映射 / Raw min produce compatibility map */
    @Deprecated("Use minProduceQuantity instead", replaceWith = ReplaceWith("minProduceQuantity.mapValues { (_, q) -> q.value }"))
    val minProduce: Map<M, V> get() = minProduceQuantity.mapValues { (_, quantity) -> quantity.value }

    /** 原料消耗上限裸值兼容映射 / Raw max consumption compatibility map */
    @Deprecated("Use maxConsumptionQuantity instead", replaceWith = ReplaceWith("maxConsumptionQuantity.mapValues { (_, q) -> q.value }"))
    val maxConsumption: Map<M, V> get() = maxConsumptionQuantity.mapValues { (_, quantity) -> quantity.value }

    /** 原料消耗下限裸值兼容映射 / Raw min consumption compatibility map */
    @Deprecated("Use minConsumptionQuantity instead", replaceWith = ReplaceWith("minConsumptionQuantity.mapValues { (_, q) -> q.value }"))
    val minConsumption: Map<M, V> get() = minConsumptionQuantity.mapValues { (_, quantity) -> quantity.value }

    /** 资源使用量上限裸值兼容映射 / Raw max resource usage compatibility map */
    @Deprecated("Use maxResourceUsageQuantity instead", replaceWith = ReplaceWith("maxResourceUsageQuantity.mapValues { (_, q) -> q.value }"))
    val maxResourceUsage: Map<R, V> get() = maxResourceUsageQuantity.mapValues { (_, quantity) -> quantity.value }

    /** 资源使用量下限裸值兼容映射 / Raw min resource usage compatibility map */
    @Deprecated("Use minResourceUsageQuantity instead", replaceWith = ReplaceWith("minResourceUsageQuantity.mapValues { (_, q) -> q.value }"))
    val minResourceUsage: Map<R, V> get() = minResourceUsageQuantity.mapValues { (_, quantity) -> quantity.value }

    companion object {
        /**
         * 通过裸值创建时隙约束 / Create slot constraints from raw values
         *
         * @param M 物料类型 / Material type
         * @param R 资源容量类型 / Resource capacity type
         * @param V 数值类型 / Numeric type
         * @param slot 时隙 / Time slot
         * @param slotIndex 时隙索引 / Slot index
         * @param maxProduce 产品产量上限 / Maximum production by product
         * @param minProduce 产品产量下限 / Minimum production by product
         * @param maxConsumption 原料消耗上限 / Maximum consumption by material
         * @param minConsumption 原料消耗下限 / Minimum consumption by material
         * @param maxResourceUsage 资源使用量上限 / Maximum resource usage by resource
         * @param minResourceUsage 资源使用量下限 / Minimum resource usage by resource
         * @return 时隙约束 / Slot constraints
         */
        @Deprecated(
            message = "Use primary constructor with Quantity-typed maps",
            replaceWith = ReplaceWith(
                "SlotConstraints(slot, slotIndex, maxProduce.mapValues { (_, v) -> Quantity(v, NoneUnit) }, minProduce.mapValues { (_, v) -> Quantity(v, NoneUnit) }, maxConsumption.mapValues { (_, v) -> Quantity(v, NoneUnit) }, minConsumption.mapValues { (_, v) -> Quantity(v, NoneUnit) }, maxResourceUsage.mapValues { (_, v) -> Quantity(v, NoneUnit) }, minResourceUsage.mapValues { (_, v) -> Quantity(v, NoneUnit) })",
                "fuookami.ospf.kotlin.quantities.quantity.Quantity",
                "fuookami.ospf.kotlin.quantities.unit.NoneUnit"
            )
        )
        operator fun <M, R, V> invoke(
            slot: TimeSlot,
            slotIndex: Int,
            maxProduce: Map<M, V>,
            minProduce: Map<M, V>,
            maxConsumption: Map<M, V>,
            minConsumption: Map<M, V>,
            maxResourceUsage: Map<R, V>,
            minResourceUsage: Map<R, V>
        ): SlotConstraints<M, R, V> where V : RealNumber<V>, V : PlusGroup<V> {
            return SlotConstraints(
                slot = slot,
                slotIndex = slotIndex,
                maxProduceQuantity = maxProduce.mapValues { (_, value) -> Quantity(value, NoneUnit) },
                minProduceQuantity = minProduce.mapValues { (_, value) -> Quantity(value, NoneUnit) },
                maxConsumptionQuantity = maxConsumption.mapValues { (_, value) -> Quantity(value, NoneUnit) },
                minConsumptionQuantity = minConsumption.mapValues { (_, value) -> Quantity(value, NoneUnit) },
                maxResourceUsageQuantity = maxResourceUsage.mapValues { (_, value) -> Quantity(value, NoneUnit) },
                minResourceUsageQuantity = minResourceUsage.mapValues { (_, value) -> Quantity(value, NoneUnit) }
            )
        }

        /**
         * 从产能结果创建约束
         * Create constraints from capacity result
         *
         * @param result The slot capacity result / 时隙产能结果
         * @param tolerance Tolerance added to bounds / 添加到边界的容差
         * @return Slot constraints / 时隙约束
         */
        fun <A : ProductionAction, M, R, V> from(
            result: SlotBasedCapacityResult<A, M, R, V>,
            tolerance: V? = null
        ): SlotConstraints<M, R, V> where V : RealNumber<V>, V : PlusGroup<V> {
            if (tolerance == null) {
                return SlotConstraints(
                    slot = result.slot,
                    slotIndex = result.slotIndex,
                    maxProduceQuantity = result.produceQuantityByProduct,
                    minProduceQuantity = result.produceQuantityByProduct,
                    maxConsumptionQuantity = result.consumptionQuantityByMaterial,
                    minConsumptionQuantity = result.consumptionQuantityByMaterial,
                    maxResourceUsageQuantity = result.resourceUsageQuantityByResource,
                    minResourceUsageQuantity = result.resourceUsageQuantityByResource
                )
            }

            val zero = tolerance.constants.zero
            val maxProduceQuantity = result.produceQuantityByProduct.mapValues { (_, q) ->
                Quantity(q.value + tolerance, q.unit)
            }
            val minProduceQuantity = result.produceQuantityByProduct.mapValues { (_, q) ->
                val value = q.value - tolerance
                Quantity(if (value ls zero) zero else value, q.unit)
            }
            val maxConsumptionQuantity = result.consumptionQuantityByMaterial.mapValues { (_, q) ->
                Quantity(q.value + tolerance, q.unit)
            }
            val minConsumptionQuantity = result.consumptionQuantityByMaterial.mapValues { (_, q) ->
                val value = q.value - tolerance
                Quantity(if (value ls zero) zero else value, q.unit)
            }
            val maxResourceUsageQuantity = result.resourceUsageQuantityByResource.mapValues { (_, q) ->
                Quantity(q.value + tolerance, q.unit)
            }
            val minResourceUsageQuantity = result.resourceUsageQuantityByResource.mapValues { (_, q) ->
                val value = q.value - tolerance
                Quantity(if (value ls zero) zero else value, q.unit)
            }

            return SlotConstraints(
                slot = result.slot,
                slotIndex = result.slotIndex,
                maxProduceQuantity = maxProduceQuantity,
                minProduceQuantity = minProduceQuantity,
                maxConsumptionQuantity = maxConsumptionQuantity,
                minConsumptionQuantity = minConsumptionQuantity,
                maxResourceUsageQuantity = maxResourceUsageQuantity,
                minResourceUsageQuantity = minResourceUsageQuantity
            )
        }
    }
}

private fun <V : RealNumber<V>> Quantity<Flt64>.toGeneric(
    adapter: SchedulingSolverValueAdapter<V>
): Quantity<V> {
    return Quantity(adapter.intoValue(value), unit)
}
