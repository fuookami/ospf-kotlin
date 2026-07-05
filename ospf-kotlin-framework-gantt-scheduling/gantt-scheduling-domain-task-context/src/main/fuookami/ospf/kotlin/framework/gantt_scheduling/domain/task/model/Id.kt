/**
 * 任务域 id 语义标记接口 / Task domain id semantic marker interfaces
 *
 * 将原 String id 改为语义化标记接口，业务侧 value class 实现接口、子类协变 override，
 * 以保留类型安全（见 PoC：CovariantIdOverridePoCTest / NameClashPoCTest）。
 */
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model

/**
 * 任务 id 标记接口 / Task id marker interface
 */
interface TaskId

/**
 * 任务计划 id 标记接口 / Task plan id marker interface
 *
 * 继承 TaskId，因 AbstractPlannedTask.id = plan.id（TaskPlanId 须可赋给 TaskId 槽）。
 */
interface TaskPlanId : TaskId

/**
 * 任务步骤计划 id 标记接口 / Task step plan id marker interface
 *
 * 继承 TaskPlanId，因 TaskStepPlan override AbstractTaskPlan.id。
 */
interface TaskStepPlanId : TaskPlanId

/**
 * 执行者 id 标记接口 / Executor id marker interface
 */
interface ExecutorId

/**
 * 任务步骤 id 标记接口 / Task step id marker interface
 */
interface TaskStepId

/**
 * 任务步骤图 id 标记接口 / Task step graph id marker interface
 */
interface TaskStepGraphId

/**
 * TaskStepPlanId 的默认实现：由父任务 id 与步骤 id 组合 / Default TaskStepPlanId impl composed from parent task id and step id
 *
 * 用于 TaskStepPlan.id = "${parent.id}-${step.id}" 的拼接场景。
 * value class 仅允许单字段，故用 data class 承载双字段。
 */
data class CompositeTaskStepPlanId(
    val parent: TaskId,
    val step: TaskStepId
) : TaskStepPlanId {
    override fun toString(): String = "$parent-$step"
}
