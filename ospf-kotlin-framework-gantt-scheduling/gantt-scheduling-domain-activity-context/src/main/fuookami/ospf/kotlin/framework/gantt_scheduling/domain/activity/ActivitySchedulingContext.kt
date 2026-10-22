@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.activity

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.SchedulingSolverValueAdapter
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.activity.model.*

/** 活动级有限域排程入口，与任务束和 Network 无强制依赖。 / Finite-domain activity scheduling entry with no mandatory bunch or Network dependency.
 * @property problem 不可变输入 / Immutable input
 * @property aggregation 每个模型独立的注册态 / Independent per-model registration state
 * @param choices 完整业务候选 / Complete business candidates
 */
class ActivitySchedulingContext<V : RealNumber<V>> private constructor(
    val problem: ActivitySchedulingProblem<V>,
    val aggregation: ActivitySchedulingAggregation<V>,
    choices: List<ActivityPlacement<V>>
) {
    val choices = choices.toList()

    /** 注册到主模型，不自行启动求解。 / Register with a master model without starting a solve.
     * @param model 主模型 / Master model
     * @param minimizeCost 是否添加费用目标 / Whether to add cost objective
     * @return 注册结果 / Registration result
     */
    fun register(model: LinearMetaModel<Flt64>, minimizeCost: Boolean = true): Try = aggregation.register(model, minimizeCost)

    /** 独立复核组合，缺席的可选活动视为显式取消。 / Independently validate choices; absent optional activities are treated as explicit cancellations.
     * @param choices 候选身份 / Choice identities
     * @return 可行性和违反项 / Feasibility and violations
     */
    fun validate(choices: List<ActivityChoiceKey>): ActivityScheduleValidation = aggregation.validate(choices)

    /** 提取有限域排程。 / Extract a finite-domain schedule.
     * @param model 已求解的注册模型 / Solved owning model
     * @return 排程或错误 / Schedule or error
     */
    fun extractSolution(model: LinearMetaModel<Flt64>): Ret<ActivitySchedule<V>> = aggregation.extractSolution(model)

    companion object {
        /** 先校验完整输入和单位，再创建独立注册态。 / Validate complete input and units before creating fresh registration state.
         * @param problem 有限域问题 / Finite-domain problem
         * @param adapter 数值边界 / Numeric boundary
         * @param name 模型命名空间 / Model namespace
         * @return 上下文或错误 / Context or error
         */
        fun <V : RealNumber<V>> create(
            problem: ActivitySchedulingProblem<V>,
            adapter: SchedulingSolverValueAdapter<V>,
            name: String = "activity_scheduling"
        ): Ret<ActivitySchedulingContext<V>> {
            if (!name.matches(Regex("[A-Za-z][A-Za-z0-9_]*"))) {
                return activityFailure("模型命名空间非法 / Invalid model namespace")
            }
            val boundary = ActivityValueBoundary(adapter)
            val data = when (val result = prepareActivityProblem(problem, boundary)) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            return ok(ActivitySchedulingContext(
                problem = problem,
                aggregation = ActivitySchedulingAggregation(data, boundary, name),
                choices = data.choices.mapNotNull { it.placement }
            ))
        }
    }
}
