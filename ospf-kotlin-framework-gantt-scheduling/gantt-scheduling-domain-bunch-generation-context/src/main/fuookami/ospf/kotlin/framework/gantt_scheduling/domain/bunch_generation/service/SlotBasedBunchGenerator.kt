@file:Suppress("DEPRECATION")

@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_generation.service

import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.model.Flt64CapacityIntermediateValues
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.model.Flt64SlotConstraints
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.model.SlotBasedBunch
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model.ProductionAction
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AbstractTask
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AssignmentPolicy
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.Executor
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeSlot
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.UInt64

/**
 * 分时隙任务束生成器接�?
 * Slot-based bunch generator interface
 *
 * 生成满足时隙约束�?bunch�?
 * Generates bunches that satisfy slot constraints.
 *
 * 每个 bunch 只能属于一个时隙，时隙对应关系由具体实现保证�?
 * Each bunch can only belong to one time slot, ensured by concrete implementation.
 *
 * @param B 任务束类�?/ Task bunch type
 * @param T 任务类型 / Task type
 * @param E 执行器类�?/ Executor type
 * @param A 分配策略类型 / Assignment policy type
 * @param Action 生产动作类型 / Production action type
 * @param M 物料类型 / Material type
 * @param R 资源容量类型 / Resource capacity type
 * @param V 影子价格数值类型 / Shadow price numeric type
 */
interface SlotBasedBunchGenerator<
        B : SlotBasedBunch<T, E, A>,
        T : AbstractTask<E, A>,
        E : Executor,
        A : AssignmentPolicy<E>,
        Action : ProductionAction,
        M,
        R,
        V : RealNumber<V>
        > {
    /**
     * 支持的执行器列表
     * List of supported executors
     */
    val executors: List<E>

    /**
     * 为指定时隙生�?bunch
     * Generate bunches for specified slot
     *
     * @param iteration Current iteration number / 当前迭代�?
     * @param slot Target time slot / 目标时隙
     * @param constraints Slot constraints / 时隙约束
     * @param shadowPrices Task shadow prices / 任务影子价格
     * @return Generated bunches / 生成�?bunch 列表
     */
    suspend fun generate(
        iteration: UInt64,
        slot: TimeSlot,
        constraints: Flt64SlotConstraints<M, R>,
        shadowPrices: Map<T, V>
    ): Ret<List<B>>

    /**
     * 批量生成所有时隙的 bunch
     * Generate bunches for all slots in batch
     *
     * @param iteration Current iteration number / 当前迭代�?
     * @param intermediateValues Capacity intermediate values / 产能中间�?
     * @param shadowPrices Task shadow prices / 任务影子价格
     * @return Generated bunches / 生成�?bunch 列表
     */
    suspend fun generateAll(
        iteration: UInt64,
        intermediateValues: Flt64CapacityIntermediateValues<Action, M, R>,
        shadowPrices: Map<T, V>
    ): Ret<List<B>> {
        val allBunches = mutableListOf<B>()

        for (slot in intermediateValues.slots) {
            val constraints = intermediateValues.slotConstraints(slot)
            if (constraints != null) {
                when (val result = generate(iteration, slot, constraints, shadowPrices)) {
                    is Ok -> {
                        allBunches.addAll(result.value)
                    }

                    is Failed -> {
                        return Failed(result.error)
                    }

                    is Fatal -> {
                        return Fatal(result.errors)
                    }
                }
            }
        }

        return Ok(allBunches)
    }

    /**
     * 检查是否支持指定执行器
     * Check if specified executor is supported
     *
     * @param executor The executor / 执行�?
     * @return Whether the executor is supported / 是否支持该执行器
     */
    fun supportsExecutor(executor: E): Boolean {
        return executors.contains(executor)
    }
}

/**
 * 分时隙任务束生成器工厂接�?
 * Slot-based bunch generator factory interface
 *
 * 用于创建特定执行器的 bunch 生成器�?
 * Used to create bunch generators for specific executors.
 */
interface SlotBasedBunchGeneratorFactory<
        B : SlotBasedBunch<T, E, A>,
        T : AbstractTask<E, A>,
        E : Executor,
        A : AssignmentPolicy<E>,
        Action : ProductionAction,
        M,
        R,
        V : RealNumber<V>
        > {
    /**
     * 创建指定执行器的生成�?
     * Create generator for specified executor
     *
     * @param executor The executor / 执行�?
     * @return Bunch generator / 任务束生成器
     */
    fun create(executor: E): SlotBasedBunchGenerator<B, T, E, A, Action, M, R, V>?
}
