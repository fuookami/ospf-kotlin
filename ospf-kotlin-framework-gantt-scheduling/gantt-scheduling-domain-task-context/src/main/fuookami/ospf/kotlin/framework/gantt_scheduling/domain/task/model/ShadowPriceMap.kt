/**
 * 甘特调度影子价格映射及相关类型 / Gantt scheduling shadow price map and related types
 */
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.framework.model.*

/**
 * 甘特调度影子价格参数接口 / Gantt scheduling shadow price arguments interface
 *
 * @param E 执行者类型 / The executor type
 * @param A 分配策略类型 / The assignment policy type
 */
interface AbstractGanttSchedulingShadowPriceArguments<
        out E : Executor,
        out A : AssignmentPolicy<E>
        > {
    /** 执行者 / The executor */
    val executor: E
    /** 任务 / The task */
    val task: AbstractTask<E, A>?
}

/**
 * 任务级甘特调度影子价格参数 / Task-level Gantt scheduling shadow price arguments
 *
 * @param E 执行者类型 / The executor type
 * @param A 分配策略类型 / The assignment policy type
 * @property executor 执行者 / The executor
 * @property task 任务 / The task
 */
open class TaskGanttSchedulingShadowPriceArguments<
        out E : Executor,
        out A : AssignmentPolicy<E>
        >(
    override val executor: E,
    override val task: AbstractTask<E, A>? = null,
) : AbstractGanttSchedulingShadowPriceArguments<E, A>

/**
 * 任务束级甘特调度影子价格参数 / Bunch-level Gantt scheduling shadow price arguments
 *
 * @param E 执行者类型 / The executor type
 * @param A 分配策略类型 / The assignment policy type
 * @property executor 执行者 / The executor
 * @property task 任务 / The task
 * @property prevTask 前一个任务 / The previous task
 */
open class BunchGanttSchedulingShadowPriceArguments<
        out E : Executor,
        out A : AssignmentPolicy<E>
        >(
    override val executor: E,
    override val task: AbstractTask<E, A>? = null,
    open val prevTask: AbstractTask<E, A>? = null
) : AbstractGanttSchedulingShadowPriceArguments<E, A>

/**
 * 抽象甘特调度影子价格映射 / Abstract Gantt scheduling shadow price map
 *
 * @param Args 参数类型 / The arguments type
 * @param E 执行者类型 / The executor type
 * @param A 分配策略类型 / The assignment policy type
 */
open class AbstractGanttSchedulingShadowPriceMap<
        out Args : AbstractGanttSchedulingShadowPriceArguments<E, A>,
        out E : Executor,
        out A : AssignmentPolicy<E>
        > : AbstractShadowPriceMap<
        @UnsafeVariance Args, AbstractGanttSchedulingShadowPriceMap<@UnsafeVariance Args, @UnsafeVariance E, @UnsafeVariance A>
        >()

/** 甘特调度影子价格映射类型别名 / Gantt scheduling shadow price map type alias */
typealias GanttSchedulingShadowPriceMap<E, A> = AbstractGanttSchedulingShadowPriceMap<
        AbstractGanttSchedulingShadowPriceArguments<E, A>, E, A
        >

/**
 * 计算任务束的缩减成本 / Calculate the reduced cost of a task bunch
 *
 * @param T 任务类型 / The task type
 * @param E 执行者类型 / The executor type
 * @param A 分配策略类型 / The assignment policy type
 * @param bunch 任务束 / The task bunch
 * @return 缩减成本 / The reduced cost
 */
inline fun <
        reified V : RealNumber<V>,
        T : AbstractTask<E, A>,
        E : Executor,
        A : AssignmentPolicy<E>
        > AbstractGanttSchedulingShadowPriceMap<
        AbstractGanttSchedulingShadowPriceArguments<E, A>, E, A
        >.reducedCost(
    bunch: AbstractTaskBunch<T, E, A, V>
): Ret<V> {
    val solverCost = bunch.cost.solverCost()
    if (solverCost is Failed) {
        return Failed(solverCost.error)
    }
    if (solverCost is Fatal) {
        return Fatal(solverCost.errors)
    }
    var ret = (solverCost as Ok).value
    if (bunch.executor.indexed) {
        ret -= this(BunchGanttSchedulingShadowPriceArguments(bunch.executor))
        for ((index, task) in bunch.tasks.withIndex()) {
            val prevTask = if (index != 0) {
                bunch.tasks[index - 1]
            } else {
                bunch.lastTask
            }
            ret -= this(
                BunchGanttSchedulingShadowPriceArguments(
                    executor = bunch.executor,
                    prevTask = prevTask,
                    task = task
                )
            )
        }
        if (bunch.tasks.isNotEmpty()) {
            ret -= this(
                BunchGanttSchedulingShadowPriceArguments(
                    executor = bunch.executor,
                    prevTask = bunch.tasks.last(),
                    task = null
                )
            )
        }
    }
    return Ok(SchedulingSolverValueAdapter.create<V>().intoValue(ret))
}

/** 抽象甘特调度影子价格提取器类型别名 / Abstract Gantt scheduling shadow price extractor type alias */
typealias AbstractGanttSchedulingShadowPriceExtractor<Args, E, A> = ShadowPriceExtractor<
        Args, AbstractGanttSchedulingShadowPriceMap<Args, E, A>
        >

/** 甘特调度影子价格提取器类型别名 / Gantt scheduling shadow price extractor type alias */
typealias GanttSchedulingShadowPriceExtractor<E, A> = AbstractGanttSchedulingShadowPriceExtractor<
        AbstractGanttSchedulingShadowPriceArguments<E, A>, E, A
        >

/** 抽象甘特调度列生成管道类型别名 / Abstract Gantt scheduling CG pipeline type alias */
typealias AbstractGanttSchedulingCGPipeline<Args, E, A> = CGPipeline<
        Args, AbstractLinearMetaModel<Flt64>, AbstractGanttSchedulingShadowPriceMap<Args, E, A>
        >

/** 甘特调度列生成管道类型别名 / Gantt scheduling CG pipeline type alias */
typealias GanttSchedulingCGPipeline<E, A> = AbstractGanttSchedulingCGPipeline<
        AbstractGanttSchedulingShadowPriceArguments<E, A>, E, A
        >

/** 抽象甘特调度列生成管道列表类型别名 / Abstract Gantt scheduling CG pipeline list type alias */
typealias AbstractGanttSchedulingCGPipelineList<Args, E, A> = List<
        CGPipeline<Args, AbstractLinearMetaModel<Flt64>, AbstractGanttSchedulingShadowPriceMap<Args, E, A>>
        >

/** 甘特调度列生成管道列表类型别名 / Gantt scheduling CG pipeline list type alias */
typealias GanttSchedulingCGPipelineList<E, A> = AbstractGanttSchedulingCGPipelineList<
        AbstractGanttSchedulingShadowPriceArguments<E, A>, E, A
        >
