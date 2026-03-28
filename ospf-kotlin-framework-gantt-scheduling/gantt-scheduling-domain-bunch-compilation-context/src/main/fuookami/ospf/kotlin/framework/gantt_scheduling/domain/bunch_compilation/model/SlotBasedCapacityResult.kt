@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.model

import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model.ActionAllocation
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model.ProductionAction
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeSlot
import fuookami.ospf.kotlin.utils.math.algebra.number.Flt64

/**
 * 分时隙产能结�?
 * Slot-based capacity result
 *
 * 存储单个时隙的产能分配结果和中间值�?
 * Stores capacity allocation results and intermediate values for a single time slot.
 *
 * @param A 生产动作类型 / Production action type
 * @param M 物料类型 / Material type
 * @param R 资源容量类型 / Resource capacity type
 */
data class SlotBasedCapacityResult<A : ProductionAction, M, R>(
    /**
     * 所属时�?
     * The time slot
     */
    val slot: TimeSlot,

    /**
     * 时隙索引
     * Slot index
     */
    val slotIndex: Int,

    /**
     * 该时隙内的动作分�?
     * Action allocations in this slot
     */
    val actionAllocations: List<ActionAllocation<A>>,

    /**
     * 该时隙的总成�?
     * Total cost for this slot
     */
    val totalCost: Flt64,

    /**
     * 该时隙的产品产量（按产品�?
     * Product production by product in this slot
     */
    val produceByProduct: Map<M, Flt64>,

    /**
     * 该时隙的原料消耗（按原料）
     * Material consumption by material in this slot
     */
    val consumptionByMaterial: Map<M, Flt64>,

    /**
     * 该时隙的资源使用量（按资源）
     * Resource usage by resource in this slot
     */
    val resourceUsageByResource: Map<R, Flt64>
)

/**
 * 产能中间值集�?
 * Capacity intermediate values collection
 *
 * 聚合所有时隙的产能结果，提供查询接口�?
 * Aggregates capacity results for all slots, provides query interface.
 *
 * @param A 生产动作类型 / Production action type
 * @param M 物料类型 / Material type
 * @param R 资源容量类型 / Resource capacity type
 */
class CapacityIntermediateValues<A : ProductionAction, M, R>(
    /**
     * 时隙列表
     * List of time slots
     */
    val slots: List<TimeSlot>,

    /**
     * 各时隙的产能结果
     * Capacity results by slot
     */
    val results: Map<TimeSlot, SlotBasedCapacityResult<A, M, R>>
) {
    /**
     * 获取指定时隙的产品产�?
     * Get product production for specified slot
     *
     * @param slot The time slot / 时隙
     * @param product The product / 产品
     * @return Production amount / 产量
     */
    fun produce(slot: TimeSlot, product: M): Flt64 {
        return results[slot]?.produceByProduct?.get(product) ?: Flt64.zero
    }

    /**
     * 获取指定时隙的原料消�?
     * Get material consumption for specified slot
     *
     * @param slot The time slot / 时隙
     * @param material The material / 原料
     * @return Consumption amount / 消耗量
     */
    fun consumption(slot: TimeSlot, material: M): Flt64 {
        return results[slot]?.consumptionByMaterial?.get(material) ?: Flt64.zero
    }

    /**
     * 获取指定时隙的资源使用量
     * Get resource usage for specified slot
     *
     * @param slot The time slot / 时隙
     * @param resource The resource capacity / 资源容量
     * @return Resource usage amount / 资源使用�?
     */
    fun resourceUsage(slot: TimeSlot, resource: R): Flt64 {
        return results[slot]?.resourceUsageByResource?.get(resource) ?: Flt64.zero
    }

    /**
     * 获取指定时隙的所有约�?
     * Get all constraints for specified slot
     *
     * @param slot The time slot / 时隙
     * @param tolerance Tolerance for constraint bounds / 约束边界的容�?
     * @return Slot constraints / 时隙约束
     */
    fun slotConstraints(slot: TimeSlot, tolerance: Flt64 = Flt64.zero): SlotConstraints<M, R>? {
        val result = results[slot] ?: return null
        return SlotConstraints.from(result, tolerance)
    }
}

/**
 * 时隙约束
 * Slot constraints
 *
 * 描述单个时隙的资源、产量、消耗约束边界�?
 * Describes resource, produce, consumption constraint boundaries for a single slot.
 *
 * @param M 物料类型 / Material type
 * @param R 资源容量类型 / Resource capacity type
 */
data class SlotConstraints<M, R>(
    /**
     * 所属时�?
     * The time slot
     */
    val slot: TimeSlot,

    /**
     * 时隙索引
     * Slot index
     */
    val slotIndex: Int,

    /**
     * 产品产量上限
     * Maximum production by product
     */
    val maxProduce: Map<M, Flt64>,

    /**
     * 产品产量下限
     * Minimum production by product
     */
    val minProduce: Map<M, Flt64>,

    /**
     * 原料消耗上�?
     * Maximum consumption by material
     */
    val maxConsumption: Map<M, Flt64>,

    /**
     * 原料消耗下�?
     * Minimum consumption by material
     */
    val minConsumption: Map<M, Flt64>,

    /**
     * 资源使用量上�?
     * Maximum resource usage by resource
     */
    val maxResourceUsage: Map<R, Flt64>,

    /**
     * 资源使用量下�?
     * Minimum resource usage by resource
     */
    val minResourceUsage: Map<R, Flt64>
) {
    companion object {
        /**
         * 从产能结果创建约�?
         * Create constraints from capacity result
         *
         * @param result The slot capacity result / 时隙产能结果
         * @param tolerance Tolerance added to bounds / 添加到边界的容差
         * @return Slot constraints / 时隙约束
         */
        fun <A : ProductionAction, M, R> from(
            result: SlotBasedCapacityResult<A, M, R>,
            tolerance: Flt64 = Flt64.zero
        ): SlotConstraints<M, R> {
            val maxProduce = result.produceByProduct.mapValues { (_, v) -> v + tolerance }
            val minProduce = result.produceByProduct.mapValues { (_, v) -> maxOf(Flt64.zero, v - tolerance) }
            val maxConsumption = result.consumptionByMaterial.mapValues { (_, v) -> v + tolerance }
            val minConsumption = result.consumptionByMaterial.mapValues { (_, v) -> maxOf(Flt64.zero, v - tolerance) }
            val maxResourceUsage = result.resourceUsageByResource.mapValues { (_, v) -> v + tolerance }
            val minResourceUsage = result.resourceUsageByResource.mapValues { (_, v) -> maxOf(Flt64.zero, v - tolerance) }

            return SlotConstraints(
                slot = result.slot,
                slotIndex = result.slotIndex,
                maxProduce = maxProduce,
                minProduce = minProduce,
                maxConsumption = maxConsumption,
                minConsumption = minConsumption,
                maxResourceUsage = maxResourceUsage,
                minResourceUsage = minResourceUsage
            )
        }
    }
}


