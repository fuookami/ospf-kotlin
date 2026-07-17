/** 执行器-时隙编译约束 / Executor-slot compilation constraint */
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.service.limits

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeSlot
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.model.*

/**
 * 执行器-时隙编译影子价格键 / Executor-slot compilation shadow price key
 *
 * @param E 执行器类型 / Executor type
 * @property executor 执行器 / Executor
 * @property slot 时隙 / Time slot
 */
data class ExecutorSlotCompilationShadowPriceKey<E : Executor>(
    val executor: E,
    val slot: TimeSlot
) : ShadowPriceKey(ExecutorSlotCompilationShadowPriceKey::class)

/**
 * 每个执行器在每个时隙恰好选择一列的约束 / Exactly-one-column constraint per executor and slot
 *
 * @param Args 影子价格参数类型 / Shadow price arguments type
 * @param B 任务束类型 / Bunch type
 * @param V 数值类型 / Numeric type
 * @param T 任务类型 / Task type
 * @param E 执行器类型 / Executor type
 * @param A 分配策略类型 / Assignment policy type
 * @param executors 执行器列表 / Executors
 * @param slots 时隙列表 / Time slots
 * @param compilation 分时隙编译模型 / Slot-based compilation model
 * @param shadowPriceExtractor 自定义影子价格提取器 / Custom shadow price extractor
 * @param name 管道名称 / Pipeline name
 */
class ExecutorSlotCompilationConstraint<
        Args : AbstractGanttSchedulingShadowPriceArguments<E, A>,
        B,
        V : RealNumber<V>,
        T : AbstractTask<E, A>,
        E : Executor,
        A : AssignmentPolicy<E>
        >(
    private val executors: List<E>,
    private val slots: List<TimeSlot>,
    private val compilation: SlotBasedBunchCompilation<B, V, T, E, A>,
    private val shadowPriceExtractor: ((Args) -> Flt64?)? = null,
    override val name: String = "executor_slot_compilation"
) : AbstractGanttSchedulingCGPipeline<Args, E, A>
        where B : AbstractTaskBunch<T, E, A, V>, B : SlotBasedBunch<T, E, A> {

    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        for (executor in executors) {
            for (slot in slots) {
                val expression = compilation.executorSlotCompilation[executor to slot]
                    ?: return Failed(
                        ErrorCode.ApplicationError,
                        "缺少执行器-时隙编译表达式：$executor, $slot / Missing executor-slot compilation expression: $executor, $slot"
                    )
                when (val result = model.addConstraint(
                    expression eq 1,
                    name = "${name}_${executor}_$slot",
                    args = ExecutorSlotCompilationShadowPriceKey(executor, slot)
                )) {
                    is Ok -> {}
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
            }
        }
        return ok
    }

    override fun extractor(): AbstractGanttSchedulingShadowPriceExtractor<Args, E, A> {
        return { map, args ->
            shadowPriceExtractor?.invoke(args) ?: when (args) {
                is SlotBunchGanttSchedulingShadowPriceArguments<*, *> -> {
                    if (args.task == null && args.prevTask == null) {
                        map.map[ExecutorSlotCompilationShadowPriceKey(args.executor, args.slot)]?.price ?: Flt64.zero
                    } else {
                        Flt64.zero
                    }
                }
                else -> Flt64.zero
            }
        }
    }

    override fun refresh(
        shadowPriceMap: AbstractGanttSchedulingShadowPriceMap<Args, E, A>,
        model: AbstractLinearMetaModel<Flt64>,
        shadowPrices: MetaDualSolution
    ): Try {
        for (constraint in model.constraintsOfGroup()) {
            val key = constraint.args as? ExecutorSlotCompilationShadowPriceKey<*> ?: continue
            shadowPrices.constraints[constraint]?.let { price ->
                shadowPriceMap.put(ShadowPrice(key, price))
            }
        }
        return ok
    }
}
