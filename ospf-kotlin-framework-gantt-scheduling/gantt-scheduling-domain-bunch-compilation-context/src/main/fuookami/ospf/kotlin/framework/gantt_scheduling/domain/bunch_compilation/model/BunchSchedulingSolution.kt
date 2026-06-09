
/** 任务束调度解 / Bunch scheduling solution */
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.bunch_compilation.model

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AbstractTask
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AbstractTaskBunch
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AssignmentPolicy
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.Executor
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model.TaskSolution

/**
 * 任务束调度解汇总 / Bunch scheduling solution summary
 *
 * @property bunchCount 任务束数 / Bunch count
 * @property assignedTaskCount 已分配任务数 / Assigned task count
 * @property canceledTaskCount 已取消任务数 / Canceled task count
 * @property totalTaskCount 总任务数 / Total task count
 */
data class BunchSolutionSummary(
    val bunchCount: UInt64,
    val assignedTaskCount: UInt64,
    val canceledTaskCount: UInt64,
    val totalTaskCount: UInt64
)

/**
 * 任务束调度解 / Bunch scheduling solution
 *
 * @param B 任务束类型 / Task bunch type
 * @param T 任务类型 / Task type
 * @param E 执行器类型 / Executor type
 * @param A 分配策略类型 / Assignment policy type
 * @param bunches 任务束列表 / List of bunches
 * @param canceledTasks 已取消任务列表 / List of canceled tasks
 */
data class BunchSolution<
        B : AbstractTaskBunch<T, E, A, V>,
        V : RealNumber<V>,
        out T : AbstractTask<E, A>,
        out E : Executor,
        out A : AssignmentPolicy<E>
        >(
    val bunches: List<B>,
    val canceledTasks: List<T>
) {
    /** 解汇总 / Solution summary */
    val summary: BunchSolutionSummary
        get() {
            val bunchCount = UInt64(bunches.size.toULong())
            val assignedTaskCount = UInt64(bunches.sumOf { it.tasks.size }.toULong())
            val canceledTaskCount = UInt64(canceledTasks.size.toULong())
            return BunchSolutionSummary(
                bunchCount = bunchCount,
                assignedTaskCount = assignedTaskCount,
                canceledTaskCount = canceledTaskCount,
                totalTaskCount = assignedTaskCount + canceledTaskCount
            )
        }
}

/**
 * 从任务束解创建任务解 / Create task solution from bunch solution
 *
 * @param B 任务束类型 / Task bunch type
 * @param T 任务类型 / Task type
 * @param E 执行器类型 / Executor type
 * @param A 分配策略类型 / Assignment policy type
 * @param bunchSolution 任务束解 / Bunch solution
 * @return 任务解 / Task solution
 */
fun <
        B : AbstractTaskBunch<T, E, A, V>,
        V : RealNumber<V>,
        T : AbstractTask<E, A>,
        E : Executor,
        A : AssignmentPolicy<E>
        > TaskSolution.Companion.invoke(
    bunchSolution: BunchSolution<B, V, T, E, A>
): TaskSolution<T, E, A> {
    val assignedTasks = ArrayList<T>()
    for (bunch in bunchSolution.bunches) {
        for (task in bunch.tasks) {
            when (val policy = task.assignmentPolicy) {
                null -> {}

                else -> {
                    if (!policy.empty) {
                        assignedTasks.add(task)
                    }
                }
            }
        }
    }
    return TaskSolution(assignedTasks, bunchSolution.canceledTasks)
}
