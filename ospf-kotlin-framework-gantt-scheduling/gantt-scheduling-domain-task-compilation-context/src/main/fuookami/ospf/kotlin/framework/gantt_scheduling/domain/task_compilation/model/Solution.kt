/** 任务调度解 / Task scheduling solution */
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model

import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AbstractTask
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AssignmentPolicy
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.Executor

/**
 * 任务调度解汇总 / Task scheduling solution summary
 *
 * @property assignedTaskCount 已分配任务数 / Assigned task count
 * @property canceledTaskCount 已取消任务数 / Canceled task count
 * @property totalTaskCount 总任务数 / Total task count
 */
data class TaskSolutionSummary(
    val assignedTaskCount: UInt64,
    val canceledTaskCount: UInt64,
    val totalTaskCount: UInt64
)

/**
 * 任务调度解 / Task scheduling solution
 *
 * @param T 任务类型 / Task type
 * @param E 执行器类型 / Executor type
 * @param A 分配策略类型 / Assignment policy type
 * @param assignedTasks 已分配任务列表 / List of assigned tasks
 * @param canceledTasks 已取消任务列表 / List of canceled tasks
 */
data class TaskSolution<
        out T : AbstractTask<E, A>,
        out E : Executor,
        out A : AssignmentPolicy<E>
        >(
    val assignedTasks: List<T>,
    val canceledTasks: List<T>
) {
    companion object {}

    /** 解汇总 / Solution summary */
    val summary: TaskSolutionSummary
        get() {
            val assignedTaskCount = UInt64(assignedTasks.size.toULong())
            val canceledTaskCount = UInt64(canceledTasks.size.toULong())
            return TaskSolutionSummary(
                assignedTaskCount = assignedTaskCount,
                canceledTaskCount = canceledTaskCount,
                totalTaskCount = assignedTaskCount + canceledTaskCount
            )
        }
}
