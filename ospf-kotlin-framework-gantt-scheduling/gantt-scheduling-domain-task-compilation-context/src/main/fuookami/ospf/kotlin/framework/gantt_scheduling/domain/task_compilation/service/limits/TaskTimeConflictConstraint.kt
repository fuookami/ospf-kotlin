@file:OptIn(kotlin.time.ExperimentalTime::class)

/** 任务时间冲突约束 / Task time conflict constraint */
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.service.limits

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model.*

/**
 * 任务时间冲突约束 / Task time conflict constraint
 *
 * @param T 任务类型 / Task type
 * @param E 执行器类型 / Executor type
 * @param A 分配策略类型 / Assignment policy type
 * @param tasks 任务列表 / List of tasks
 * @param executors 执行器列表 / List of executors
 * @param compilation 任务编译结果 / Task compilation result
 * @param name 管道名称 / Pipeline name
*/
class TaskTimeConflictConstraint<
        T : AbstractTask<E, A>,
        E : Executor,
        A : AssignmentPolicy<E>
        >(
    tasks: List<T>,
    private val executors: List<E>,
    private val compilation: TaskCompilation<T, E, A>,
    override val name: String = "task_time_conflict"
) : Pipeline<AbstractLinearMetaModel<Flt64>> {

    /** 经过筛选和排序的任务列表（仅含不可提前/延迟且有固定时间的任务） / Filtered and sorted task list (only tasks with fixed time, no advance/delay) */
    val tasks = tasks
        .filter { it.time != null && !it.advanceEnabled && !it.delayEnabled }
        .sortedBy {
            requireNotNull(it.time) {
                "TaskTimeConflictConstraint 初始化失败：任务时间为空: $it"
            }.start
        }

    override operator fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        val x = compilation.x

        for (executor in executors) {
            for (i in tasks.indices) {
                for (j in (i + 1) until tasks.size) {
                    val leftTime = requireNotNull(tasks[i].time) {
                        "TaskTimeConflictConstraint.invoke 要求 tasks[$i].time 非空: ${tasks[i]}"
                    }
                    val rightTime = requireNotNull(tasks[j].time) {
                        "TaskTimeConflictConstraint.invoke 要求 tasks[$j].time 非空: ${tasks[j]}"
                    }
                    if (leftTime.withIntersection(rightTime)) {
                        when (val result = model.addConstraint(
                            (LinearPolynomial(x[tasks[i], executor]) + LinearPolynomial(x[tasks[j], executor])) leq Flt64.one,
                            name = "${name}_${tasks[i]}_${tasks[j]}"
                        )) {
                            is Ok -> {}

                            is Failed -> {
                                return Failed(result.error)
                            }

                            is Fatal -> {
                                return Fatal(result.errors)
                            }
                        }
                    } else {
                        continue
                    }
                }
            }
        }

        return ok
    }
}
