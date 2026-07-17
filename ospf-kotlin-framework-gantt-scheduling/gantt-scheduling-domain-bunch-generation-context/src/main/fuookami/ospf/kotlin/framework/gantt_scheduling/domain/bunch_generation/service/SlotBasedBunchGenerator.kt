@file:OptIn(kotlin.time.ExperimentalTime::class)

/** 分时隙任务束生成器服务 / Slot-based bunch generator service */
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_generation.service

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeSlot
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model.ProductionAction
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
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
 *
 * @param B 任务束类型 / Task bunch type
 * @param T 任务类型 / Task type
 * @param E 执行器类型 / Executor type
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
        V
        > where V : RealNumber<V>, V : PlusGroup<V> {

    /**
     * 分时隙定价请求，保留入口状态和分支限制以供精确定价器消费。
     * Slot pricing request carrying entry state and branch restrictions for exact pricing.
     *
     * @property iteration 当前迭代 / Current iteration
     * @property slot 目标时隙 / Target slot
     * @property constraints 时隙约束 / Slot constraints
     * @property shadowPrices 影子价格 / Shadow prices
     * @property entryState 时隙入口状态 / Slot entry state
     * @property branchRestrictions 分支限制 / Branch restrictions
     */
    data class PricingRequest<T, M, R, V>(
        val iteration: UInt64,
        val slot: TimeSlot,
        val constraints: SlotConstraints<M, R, V>,
        val shadowPrices: Map<T, V>,
        val entryState: Any? = null,
        val branchRestrictions: Set<String> = emptySet()
    ) where V : RealNumber<V>, V : PlusGroup<V>

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
        constraints: SlotConstraints<M, R, V>,
        shadowPrices: Map<T, V>
    ): Ret<List<B>>

    /**
     * 使用完整定价请求生成 bunch；旧实现默认忽略新增上下文。
     * Generate bunches with a complete pricing request; legacy implementations ignore added context by default.
     *
     * @param request 定价请求 / Pricing request
     * @return Generated bunches / 生成的 bunch 列表
     */
    suspend fun generate(request: PricingRequest<T, M, R, V>): Ret<List<B>> {
        return generate(
            iteration = request.iteration,
            slot = request.slot,
            constraints = request.constraints,
            shadowPrices = request.shadowPrices
        )
    }

    /**
     * 批量生成所有时隙的 bunch
     * Generate bunches for all slots in batch
     *
     * @param iteration Current iteration number / 当前迭代数
     * @param intermediateValues Capacity intermediate values / 产能中间值
     * @param shadowPrices Task shadow prices / 任务影子价格
     * @param entryStateProvider Slot entry-state provider / 时隙入口状态提供器
     * @param branchRestrictionsProvider Slot branch-restriction provider / 时隙分支限制提供器
     * @return Generated bunches / 生成的 bunch 列表
    */
    suspend fun generateAll(
        iteration: UInt64,
        intermediateValues: CapacityIntermediateValues<Action, M, R, V>,
        shadowPrices: Map<T, V>,
        entryStateProvider: (TimeSlot) -> Any? = { null },
        branchRestrictionsProvider: (TimeSlot) -> Set<String> = { emptySet() }
    ): Ret<List<B>> {
        val allBunches = mutableListOf<B>()

        for (slot in intermediateValues.slots) {
            val constraints = intermediateValues.slotConstraints(slot)
            if (constraints != null) {
                when (val result = generate(
                    PricingRequest(
                        iteration = iteration,
                        slot = slot,
                        constraints = constraints,
                        shadowPrices = shadowPrices,
                        entryState = entryStateProvider(slot),
                        branchRestrictions = branchRestrictionsProvider(slot)
                    )
                )) {
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
        Action : ProductionAction,
        M,
        R,
        V
        > where V : RealNumber<V>, V : PlusGroup<V> {

    /**
     * 创建指定执行器的生成器
     * Create generator for specified executor
     *
     * @param executor The executor / 执行器
     * @return Bunch generator / 任务束生成器
    */
    fun create(executor: E): SlotBasedBunchGenerator<B, T, E, A, Action, M, R, V>?
}
