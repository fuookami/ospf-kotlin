@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.activity.model

import kotlin.time.Duration
import kotlin.time.Instant
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.quantities.unit.PhysicalUnit
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeRange
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*

/** 时变资源容量覆盖，区间之间不得重叠。 / Non-overlapping time-varying capacity override.
 * @property time 半开区间 / Half-open interval
 * @property capacity 区间内容量 / Capacity within the interval
 */
data class ActivityCapacity<V : RealNumber<V>>(val time: TimeRange, val capacity: Quantity<V>)

/** 可同时占用的具名资源，个体互斥可用容量一表达。 / Named concurrent resource; unit capacity expresses individual exclusivity.
 * @property id 复用执行器身份 / Existing executor identity
 * @property capacity 默认容量及单位 / Default capacity and unit
 * @param unavailableTimes 不可用时间，可取自 WorkingCalendar / Unavailable intervals, including WorkingCalendar output
 * @param capacities 时变容量覆盖 / Time-varying capacity overrides
 */
class ActivityResource<V : RealNumber<V>>(
    val id: ExecutorId,
    val capacity: Quantity<V>,
    unavailableTimes: List<TimeRange> = emptyList(),
    capacities: List<ActivityCapacity<V>> = emptyList()
) {
    val unavailableTimes = unavailableTimes.toList()
    val capacities = capacities.toList()
}

/** 模式内资源占用段，不隐式暂停或跨休息执行。 / Resource phase within a mode, without implicit preemption.
 * @property resource 资源身份 / Resource identity
 * @property quantity 占用量，不同资源不混加 / Usage, never summed across different resources
 * @property from 相对活动开始的占用起点 / Usage start relative to activity start
 * @property until 相对占用终点，null 表示模式结束 / Relative usage end; null means mode end
 */
data class ActivityResourceUse<V : RealNumber<V>>(
    val resource: ExecutorId,
    val quantity: Quantity<V>,
    val from: Duration = Duration.ZERO,
    val until: Duration? = null
)

/** 一个不可抢占执行模式，候选起点是业务域而不是搜索预算。 / Non-preemptive mode; candidate starts define the business domain, not a search budget.
 * @property id 活动内稳定模式身份 / Stable mode identity within the activity
 * @property duration 执行时长，零表示里程碑 / Duration; zero denotes a milestone
 * @param starts 完整的允许起点集合 / Complete set of allowed starts
 * @param resources 分阶段联合资源占用 / Phased joint resource usage
 * @property cost 模式费用 / Mode cost
 * @param materialFlows 内部物料事件 / Internal material events
 */
class ActivityMode<V : RealNumber<V>>(
    val id: String,
    val duration: Duration,
    starts: List<Instant>,
    resources: List<ActivityResourceUse<V>>,
    val cost: Quantity<V>,
    materialFlows: List<ActivityMaterialFlow<V>> = emptyList()
) {
    val starts = starts.toList()
    val resources = resources.toList()
    val materialFlows = materialFlows.toList()
}

/** 待排活动，null 取消费用表示必须执行。 / Pending activity; null cancellation cost makes it mandatory.
 * @property id 复用任务身份 / Existing task identity
 * @param modes 替代模式 / Alternative modes
 * @property cancellationCost 显式取消费用 / Explicit cancellation cost
 */
class Activity<V : RealNumber<V>>(
    val id: TaskId,
    modes: List<ActivityMode<V>>,
    val cancellationCost: Quantity<V>? = null
) {
    val modes = modes.toList()
}

/** 完整候选身份，可关联原始任务与分配策略。 / Complete choice identity, linkable to original tasks and assignment policies.
 * @property task 任务身份 / Task identity
 * @property mode 模式身份 / Mode identity
 * @property start 候选起点 / Candidate start
 */
data class ActivityChoiceKey(val task: TaskId, val mode: String, val start: Instant)

/** 绝对时间的资源占用段。 / Resource reservation in absolute time.
 * @property resource 资源身份 / Resource identity
 * @property time 半开占用区间 / Half-open reservation
 * @property quantity 占用量 / Reserved amount
 */
data class ActivityReservation<V : RealNumber<V>>(
    val resource: ExecutorId,
    val time: TimeRange,
    val quantity: Quantity<V>
)

/** 完整活动分配，保留内部资源占用段。 / Complete allocation retaining internal resource phases.
 * @property key 候选身份 / Choice identity
 * @property time 实际活动区间 / Activity interval
 * @param reservations 内部资源占用 / Internal reservations
 */
class ActivityPlacement<V : RealNumber<V>>(
    val key: ActivityChoiceKey,
    val time: TimeRange,
    reservations: List<ActivityReservation<V>>
) {
    val reservations = reservations.toList()
}

/** 依赖的时间端点。 / Temporal endpoint of a dependency. */
enum class ActivityEndpoint { Start, Finish }

/** 存在性规则，与纯时间关系显式分离。 / Presence semantics explicitly separated from timing. */
enum class ActivityDependencyPresence { WhenBothPresent, RequirePredecessor, RequireBoth }

/** 通用端点时间关系，涵盖 FS/SS/FF/SF。 / Endpoint timing relation covering FS/SS/FF/SF.
 * @property predecessor 前驱任务 / Predecessor task
 * @property successor 后继任务 / Successor task
 * @property from 前驱端点 / Predecessor endpoint
 * @property to 后继端点 / Successor endpoint
 * @property minimum 最小间隔，可为负 / Minimum lag, possibly negative
 * @property maximum 可选最大间隔 / Optional maximum lag
 * @property presence 活动存在性语义 / Activity presence semantics
 */
data class ActivityDependency(
    val predecessor: TaskId,
    val successor: TaskId,
    val from: ActivityEndpoint = ActivityEndpoint.Finish,
    val to: ActivityEndpoint = ActivityEndpoint.Start,
    val minimum: Duration = Duration.ZERO,
    val maximum: Duration? = null,
    val presence: ActivityDependencyPresence = ActivityDependencyPresence.RequirePredecessor
)

/** 待排活动的基线与冻结策略，费用按一次变更计。 / Baseline and freezing for pending activities; costs are per change.
 * @property choice 原基线选择 / Baseline choice
 * @property frozen 是否必须保持该选择 / Whether this choice must be retained
 * @property startChangeCost 改开始时间的一次费用 / One-off start-change cost
 * @property modeChangeCost 改执行模式的一次费用 / One-off mode-change cost
 */
data class ActivityBaseline<V : RealNumber<V>>(
    val choice: ActivityChoiceKey,
    val frozen: Boolean,
    val startChangeCost: Quantity<V>,
    val modeChangeCost: Quantity<V>
)

/** 执行事实类型；执行中事实必须提供显式预计结束和剩余占用。 / Fact kind; running facts require explicit expected completion and remaining reservations. */
enum class ActivityExecutionStatus { Completed, Running }

/** 不可重新选择的执行事实，与待排活动身份互斥。 / Execution fact, disjoint from pending activities and never rescheduled.
 * @property placement 完整实际/预计活动区间；Running 的占用仅描述剩余部分 / Actual/expected interval; Running reservations describe only the remainder
 * @property status 已完成或执行中 / Completed or running
 */
data class ActivityExecution<V : RealNumber<V>>(
    val placement: ActivityPlacement<V>,
    val status: ActivityExecutionStatus
)

/** 额外线性业务行，可用于覆盖、预算和额外约束。 / Additional linear business row for coverage, budgets and extensions.
 * @property id 稳定行标识 / Stable row identity
 * @property unit 边界与系数单位 / Unit of bounds and coefficients
 * @param coefficients 候选系数，省略即零 / Choice coefficients, zero when omitted
 * @param cancellationCoefficients 显式取消系数 / Explicit cancellation coefficients
 * @property lower 可选下界 / Optional lower bound
 * @property upper 可选上界 / Optional upper bound
 */
class ActivityRow<V : RealNumber<V>>(
    val id: String,
    val unit: PhysicalUnit,
    coefficients: Map<ActivityChoiceKey, Quantity<V>> = emptyMap(),
    cancellationCoefficients: Map<TaskId, Quantity<V>> = emptyMap(),
    val lower: Quantity<V>? = null,
    val upper: Quantity<V>? = null
) {
    val coefficients = coefficients.toMap()
    val cancellationCoefficients = cancellationCoefficients.toMap()
}

/** 模型状态无关的有限候选排程快照。 / Finite-candidate scheduling snapshot without model state.
 * @property horizon 有限优化区间 / Finite optimization horizon
 * @param activities 待排活动 / Pending activities
 * @param resources 联合资源 / Joint resources
 * @param dependencies 依赖 / Dependencies
 * @param baselines 基线 / Baselines
 * @param executions 执行事实 / Execution facts
 * @param rows 额外业务行 / Additional business rows
 * @property costUnit 统一费用单位 / Common cost unit
 * @property additionalCost 完整候选额外费用策略，可含交期、偏好等 / Extra complete-choice cost policy, including due dates and preferences
 * @param materials 期初库存和硬库存边界 / Opening inventory and hard stock bounds
 * @param materialEvents 未来固定事件；历史库存必须已计入期初量 / Fixed future events; historical inventory must already be reflected in opening stock
 */
class ActivitySchedulingProblem<V : RealNumber<V>>(
    val horizon: TimeRange,
    activities: List<Activity<V>>,
    resources: List<ActivityResource<V>>,
    dependencies: List<ActivityDependency> = emptyList(),
    baselines: List<ActivityBaseline<V>> = emptyList(),
    executions: List<ActivityExecution<V>> = emptyList(),
    rows: List<ActivityRow<V>> = emptyList(),
    val costUnit: PhysicalUnit,
    val additionalCost: ((ActivityPlacement<V>) -> Ret<Quantity<V>>)? = null,
    materials: List<ActivityMaterial<V>> = emptyList(),
    materialEvents: List<ActivityMaterialEvent<V>> = emptyList()
) {
    val activities = activities.toList()
    val resources = resources.toList()
    val dependencies = dependencies.toList()
    val baselines = baselines.toList()
    val executions = executions.toList()
    val rows = rows.toList()
    val materials = materials.toList()
    val materialEvents = materialEvents.toList()
}

/** 独立复核结果，不将不可行方案视作程序异常。 / Independent validation result; infeasibility is not a program error.
 * @property violations 违反的规则 / Violated rules
 */
data class ActivityScheduleValidation(val violations: List<String>) {
    val feasible: Boolean get() = violations.isEmpty()
}

/** 有限域排程结果，不含连续时间域或整数全局最优证明。 / Finite-domain schedule, without continuous-domain or global-integer proof.
 * @param placements 待排活动的选中分配 / Selected pending placements
 * @param executions 保留的执行事实 / Preserved execution facts
 * @param cancelled 取消任务 / Cancelled tasks
 * @property cost 待排活动及变更费用，不重复计算沉没执行成本 / Pending and change cost, excluding sunk execution costs
 * @param candidateScope 完整声明候选域 / Complete declared candidate scope
 * @param materialInventory 库存独立复核轨迹 / Independently audited inventory trace
 */
class ActivitySchedule<V : RealNumber<V>>(
    placements: List<ActivityPlacement<V>>,
    executions: List<ActivityExecution<V>>,
    cancelled: Set<TaskId>,
    val cost: Quantity<V>,
    candidateScope: Set<ActivityChoiceKey>,
    materialInventory: List<ActivityMaterialInventory<V>> = emptyList()
) {
    val placements = placements.toList()
    val executions = executions.toList()
    val cancelled = cancelled.toSet()
    val candidateScope = candidateScope.toSet()
    val materialInventory = materialInventory.toList()
}
