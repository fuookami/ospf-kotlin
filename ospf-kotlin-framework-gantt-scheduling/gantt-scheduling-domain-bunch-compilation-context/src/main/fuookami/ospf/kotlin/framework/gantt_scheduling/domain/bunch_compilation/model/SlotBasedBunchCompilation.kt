@file:OptIn(kotlin.time.ExperimentalTime::class)

/** 分时隙任务束编译模型 / Slot-based bunch compilation model */
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.model

import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.symbol.LinearExpressionSymbol
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeSlot
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*

/**
 * 分时隙任务束编译类
 * Slot-based bunch compilation class
 *
 * 继承 BunchCompilation，增加时隙相关功能。
 * Extends BunchCompilation with slot-related functionality.
 *
 * 每个 bunch 只能属于一个时隙，时隙对应关系由 bunch 生成器保证。
 * Each bunch can only belong to one time slot, ensured by the bunch generator.
 *
 * @param B 任务束类型 / Bunch type
 * @param V 数值类型 / Numeric type
 * @param T 任务类型 / Task type
 * @param E 执行器类型 / Executor type
 * @param A 分配策略类型 / Assignment policy type
 * @param tasks 任务列表 / List of tasks
 * @param executors 执行器列表 / List of executors
 * @param slots 时隙列表 / List of time slots
 * @param lockCancelTasks 锁定取消任务集合 / Set of locked cancel tasks
 * @param withExecutorLeisure 是否包含执行器空闲 / Whether to include executor leisure
*/
open class SlotBasedBunchCompilation<
        B,
        V : RealNumber<V>,
        T : AbstractTask<E, A>,
        E : Executor,
        A : AssignmentPolicy<E>
        >(
    private val tasks: List<T>,
    private val executors: List<E>,
    private val slots: List<TimeSlot>,
    private val lockCancelTasks: Set<T> = emptySet(),
    override val withExecutorLeisure: Boolean = true
) : BunchCompilation<B, V, T, E, A>(
    tasks = tasks,
    executors = executors,
    lockCancelTasks = lockCancelTasks,
    withExecutorLeisure = withExecutorLeisure,
    bunchAggregation = SlotBasedBunchAggregation()
)
        where B : AbstractTaskBunch<T, E, A, V>, B : SlotBasedBunch<T, E, A> {

    /**
     * 按时隙分组的 bunches
     * Bunches grouped by slot
    */
    val bunchesBySlot: Map<TimeSlot, List<B>>
        get() = bunches.groupBy { it.slot }

    /**
     * 按时隙分组的 bunch 变量
     * Bunch variables grouped by slot
    */
    private val allVariablesByBunch = LinkedHashMap<B, CombinationVariableItem<UInt8, Binary>>()

    /**
     * 当前有效 bunch 到真实列变量的映射
     * Mapping from active bunches to their actual column variables
     */
    val variableByBunch: Map<B, CombinationVariableItem<UInt8, Binary>>
        get() = bunches.mapNotNull { bunch ->
            allVariablesByBunch[bunch]?.let { variable -> bunch to variable }
        }.toMap(LinkedHashMap())

    val xBySlot: Map<TimeSlot, List<CombinationVariableItem<UInt8, Binary>>>
        get() = bunchesBySlot.mapValues { (_, slotBunches) ->
            slotBunches.mapNotNull { allVariablesByBunch[it] }
        }

    /**
     * 执行器-时隙选列表达式
     * Executor-slot column selection expressions
     */
    val executorSlotCompilation: Map<Pair<E, TimeSlot>, LinearExpressionSymbol<Flt64>> = buildMap {
        for (executor in executors) {
            for (slot in slots) {
                put(
                    executor to slot,
                    LinearExpressionSymbol(
                        Flt64,
                        name = "executor_slot_compilation_${executor}_${slot}"
                    )
                )
            }
        }
    }

    /**
     * 注册分时隙编译模型
     * Register the slot-based compilation model
     *
     * @param model 元模型 / Meta model
     * @return 操作结果 / Operation result
     */
    override fun register(model: MetaModel<Flt64>): Try {
        when (val result = super.register(model)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        for (compilation in executorSlotCompilation.values) {
            when (val result = model.add(compilation)) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }
        return ok
    }

    /**
     * 添加列并建立 bunch、时隙和真实变量之间的权威映射
     * Add columns and establish the authoritative mapping among bunches, slots, and actual variables
     *
     * @param iteration 迭代次数 / Iteration count
     * @param newBunches 新任务束列表 / List of new bunches
     * @param model 线性元模型 / Linear meta model
     * @return 去重后的任务束列表 / Deduplicated bunch list
     */
    override suspend fun addColumns(
        iteration: UInt64,
        newBunches: List<B>,
        model: AbstractLinearMetaModel<Flt64>
    ): Ret<List<B>> {
        val unknownSlotBunch = newBunches.firstOrNull { it.slot !in slots }
        if (unknownSlotBunch != null) {
            return Failed(
                ErrorCode.IllegalArgument,
                "任务束引用了未登记时隙：${unknownSlotBunch.slot} / Bunch references an unregistered slot: ${unknownSlotBunch.slot}"
            )
        }

        val unduplicatedBunches = when (val result = super.addColumns(
            iteration = iteration,
            newBunches = newBunches,
            model = model
        )) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        if (unduplicatedBunches.isEmpty()) {
            return Ok(emptyList())
        }

        val xi = x.lastOrNull() ?: return Failed(
            ErrorCode.ApplicationError,
            "新增列缺少对应变量容器 / Added columns have no corresponding variable container"
        )
        for (bunch in unduplicatedBunches) {
            val variable = xi[bunch]
            allVariablesByBunch[bunch] = variable

            val compilation = executorSlotCompilation[bunch.executor to bunch.slot] ?: return Failed(
                ErrorCode.ApplicationError,
                "新增列缺少执行器-时隙表达式：${bunch.executor}, ${bunch.slot} / Added column has no executor-slot expression: ${bunch.executor}, ${bunch.slot}"
            )
            compilation.flush()
            compilation.asMutable() += LinearMonomial(Flt64.one, variable)
        }
        return Ok(unduplicatedBunches)
    }

    /**
     * 按时隙添加列
     * Add columns by slot
     *
     * @param iteration Current iteration number / 当前迭代号
     * @param newBunches New bunches to add / 要添加的新 bunch
     * @param model Linear meta model / 线性元模型 (solver boundary — Flt64)
     * @return Added bunches grouped by slot / 按时隙分组的已添加 bunch
    */
    open suspend fun addColumnsBySlot(
        iteration: UInt64,
        newBunches: List<B>,
        model: AbstractLinearMetaModel<Flt64>
    ): Ret<Map<TimeSlot, List<B>>> {
        val unduplicatedBunches = when (val result = addColumns(
            iteration = iteration,
            newBunches = newBunches,
            model = model
        )) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        return Ok(unduplicatedBunches.groupBy { it.slot })
    }

    /**
     * 获取指定时隙的所有 bunch
     * Get all bunches for specified slot
     *
     * @param slot The time slot / 时隙
     * @return List of bunches in this slot / 该时隙的 bunch 列表
    */
    fun bunchesInSlot(slot: TimeSlot): List<B> {
        return bunchesBySlot[slot] ?: emptyList()
    }

    /**
     * 获取指定时隙和执行器的所有 bunch
     * Get all bunches for specified slot and executor
     *
     * @param slot The time slot / 时隙
     * @param executor The executor / 执行器
     * @return List of bunches in this slot for this executor / 该时隙该执行器的 bunch 列表
    */
    fun bunchesInSlot(slot: TimeSlot, executor: E): List<B> {
        return bunchesBySlot[slot]?.filter { it.executor == executor } ?: emptyList()
    }
}
