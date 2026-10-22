@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.activity.model

import kotlin.math.abs
import kotlin.math.round
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.variable.BinVar
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.TaskId
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.activity.service.*

/** 独立模型注册态，部分失败后禁止复用。 / Independent model state, poisoned after any partial registration failure.
 * @property compilation 变量与费用组件 / Variable and cost component
 * @param data 已规范化的问题 / Normalized problem
 * @param boundary 数值边界 / Numeric boundary
 * @param name 命名空间 / Namespace
 */
class ActivitySchedulingAggregation<V : RealNumber<V>> internal constructor(
    private val data: CompiledActivityProblem<V>,
    private val boundary: ActivityValueBoundary<V>,
    private val name: String
) {
    val compilation = ActivityCompilation(data.choices.size, data.choices.map { it.cost }, name)
    private var attempted = false
    private var usable = false
    private var owner: LinearMetaModel<Flt64>? = null

    /** 读取候选变量以追加约束。 / Read a choice variable for additional constraints.
     * @param key 完整候选身份 / Complete choice identity
     * @return 变量，未知候选返回 null / Variable, or null for an unknown choice
     */
    fun variable(key: ActivityChoiceKey): BinVar? = data.indices[key]?.let { compilation.variables[it] }

    /** 读取显式取消变量。 / Read an explicit cancellation variable.
     * @param task 待排任务 / Pending task
     * @return 取消变量，必做任务返回 null / Cancellation variable, or null for a mandatory task
     */
    fun cancellationVariable(task: TaskId): BinVar? = data.cancellations[task]?.let { compilation.variables[it] }

    /** 注册变量、约束和可选目标，不启动 solver。 / Register variables, constraints and optional objective, without invoking a solver.
     * @param model 目标模型 / Target model
     * @param minimizeCost 是否添加费用最小化目标 / Whether to add cost minimization
     * @return 注册结果 / Registration result
     */
    fun register(model: LinearMetaModel<Flt64>, minimizeCost: Boolean = true): Try {
        if (attempted) return activityFailure("上下文只能注册一次，请为新模型创建新上下文 / Register a context only once; create a fresh context for another model")
        if (minimizeCost && model.objectCategory != ObjectCategory.Minimum) {
            return activityFailure("费用目标要求最小化模型 / Cost objective requires a minimization model")
        }
        attempted = true
        owner = model
        for (variable in compilation.variables) {
            when (val result = model.add(variable)) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }
        for (row in ActivitySchedulingRows(data).rows()) {
            when (val result = ActivityRowPipeline(row).register(model, compilation, name)) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }
        if (minimizeCost) {
            when (val result = model.minimize(compilation.cost, name = "${name}_cost")) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }
        usable = true
        return ok
    }

    /** 独立复核候选组合，不依赖注册态。 / Independently validate choices, without requiring registration.
     * @param choices 完整候选身份 / Complete choice identities
     * @return 结构化可行性结论 / Structured feasibility result
     */
    fun validate(choices: List<ActivityChoiceKey>): ActivityScheduleValidation = ActivityScheduleValidator(data).validate(choices)

    /** 从当前模型提取整数可行解并独立校验。 / Extract and independently validate an integer feasible solution from the owning model.
     * @param model 已注册模型 / Registered model
     * @return 排程或提取错误；不附带最优性证明 / Schedule or extraction failure; no optimality proof
     */
    fun extractSolution(model: LinearMetaModel<Flt64>): Ret<ActivitySchedule<V>> {
        if (!usable || owner !== model) return activityFailure("模型未成功注册或不属于此上下文 / Model not successfully registered or not owned by this context")
        val selected = mutableListOf<Int>()
        for ((index, variable) in compilation.variables.withIndex()) {
            val value = model.tokens.find(variable)?.result?.toDouble()
                ?: return activityFailure("缺少活动选择解值 / Missing activity-choice solution value")
            if (!value.isFinite() || abs(value - round(value)) > 1e-6 || value < -1e-6 || value > 1.0 + 1e-6) {
                return activityFailure("活动选择不是有限二元解 / Activity choice is not a finite binary solution")
            }
            if (value > 0.5) selected.add(index)
        }
        for (task in data.problem.activities) {
            if (selected.count { data.choices[it].task == task.id } != 1) {
                return activityFailure("每个活动必须恰好选择执行或显式取消 / Each activity must select exactly one execution or explicit cancellation")
            }
        }
        val choices = selected.mapNotNull { data.choices[it].placement?.key }
        val validation = validate(choices)
        if (!validation.feasible) {
            return activityFailure("排程独立校验失败：${validation.violations} / Independent schedule validation failed: ${validation.violations}")
        }
        val results = model.tokens.tokensInSolver.map { token ->
            val value = token.result ?: return activityFailure("模型存在未赋值变量 / Model contains an unassigned variable")
            val number = value.toDouble()
            val rounded = round(number)
            // Double 的负零在数值域边界中不等同正零，整数校验统一为正零。 / Canonicalize signed zero before integer-bound validation.
            val normalized = if (token.type.isIntegerType) Flt64(if (rounded == 0.0) 0.0 else rounded) else value
            if (!number.isFinite() || token.range?.contains(normalized) == false ||
                token.type.isIntegerType && abs(number - round(number)) > 1e-6
            ) {
                return activityFailure("模型变量不满足值域或整数性：${token.name}=$value, ${token.range} / Model variable violates bounds or integrality: ${token.name}=$value, ${token.range}")
            }
            normalized
        }
        if (model.constraints.any { it.isTrue(results, model.tokens, false) != true }) {
            return activityFailure("模型约束复核失败，含调用方追加约束 / Model constraint validation failed, including caller extensions")
        }
        val cost = selected.fold(Flt64.zero) { sum, index -> sum + data.choices[index].cost }
        val quantity = when (val result = boundary.quantity(cost, data.problem.costUnit)) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val inventory = when (val result = ActivityMaterialAudit(data).extract(selected.toSet(), boundary)) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        return ok(ActivitySchedule(
            placements = selected.mapNotNull { data.choices[it].placement },
            executions = data.problem.executions,
            cancelled = selected.filter { data.choices[it].placement == null }.map { data.choices[it].task }.toSet(),
            cost = quantity,
            candidateScope = data.indices.keys,
            materialInventory = inventory
        ))
    }
}
