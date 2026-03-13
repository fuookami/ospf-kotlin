package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_generation.service

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.model.*

/**
 * 分时隙任务束生成器接口
 * Slot-based bunch generator interface
 *
 * 生成满足时隙约束的 bunch。
 * Generates bunches that satisfy slot constraints.
 *
 * 每个 bunch 只能属于一个时隙，时隙对应关系由具体实现保证。
 * Each bunch can only belong to one time slot, ensured by concrete implementation.
 */
interface SlotBasedBunchGenerator<
    B : SlotBasedBunch<T, E, A>,
    T : AbstractTask<E, A>,
    E : Executor,
    A : AssignmentPolicy<E>,
    Action : ProductionAction
> {
    /**
     * 支持的执行器列表
     * List of supported executors
     */
    val executors: List<E>

    /**
     * 为指定时隙生成 bunch
     * Generate bunches for specified slot
     *
     * @param iteration Current iteration number / 当前迭代数
     * @param slot Target time slot / 目标时隙
     * @param constraints Slot constraints / 时隙约束
     * @param shadowPrices Task shadow prices / 任务影子价格
     * @return Generated bunches / 生成的 bunch 列表
     */
    suspend fun generate(
        iteration: UInt64,
        slot: TimeSlot,
        constraints: SlotConstraints,
        shadowPrices: Map<T, Flt64>
    ): Ret<List<B>>

    /**
     * 批量生成所有时隙的 bunch
     * Generate bunches for all slots in batch
     *
     * @param iteration Current iteration number / 当前迭代数
     * @param intermediateValues Capacity intermediate values / 产能中间值
     * @param shadowPrices Task shadow prices / 任务影子价格
     * @return Generated bunches / 生成的 bunch 列表
     */
    suspend fun generateAll(
        iteration: UInt64,
        intermediateValues: CapacityIntermediateValues<Action>,
        shadowPrices: Map<T, Flt64>
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
                        // Log error but continue with other slots
                        // 记录错误但继续处理其他时隙
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
     * @param executor The executor / 执行器
     * @return Whether the executor is supported / 是否支持该执行器
     */
    fun supportsExecutor(executor: E): Boolean {
        return executors.contains(executor)
    }
}

/**
 * 分时隙任务束生成器工厂接口
 * Slot-based bunch generator factory interface
 *
 * 用于创建特定执行器的 bunch 生成器。
 * Used to create bunch generators for specific executors.
 */
interface SlotBasedBunchGeneratorFactory<
    B : SlotBasedBunch<T, E, A>,
    T : AbstractTask<E, A>,
    E : Executor,
    A : AssignmentPolicy<E>,
    Action : ProductionAction
> {
    /**
     * 创建指定执行器的生成器
     * Create generator for specified executor
     *
     * @param executor The executor / 执行器
     * @return Bunch generator / 任务束生成器
     */
    fun create(executor: E): SlotBasedBunchGenerator<B, T, E, A, Action>?
}