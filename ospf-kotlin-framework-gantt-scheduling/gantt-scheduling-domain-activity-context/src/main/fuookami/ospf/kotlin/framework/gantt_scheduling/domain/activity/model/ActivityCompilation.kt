@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.activity.model

import kotlin.time.Duration
import kotlin.time.Instant
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.quantities.unit.PhysicalUnit
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.convertTo
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeRange
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*

internal fun <T> activityFailure(message: String): Ret<T> = Failed(ErrorCode.IllegalArgument, message)

/** 模型组件，每次上下文创建独立变量与费用表达式。 / Model component with fresh variables and cost per context.
 * @param size 有限选择数，含显式取消选项 / Number of finite choices including explicit cancellations
 * @param costs 注册边界统一后的费用 / Costs normalized at registration boundary
 * @param name 命名空间 / Namespace
 * @property variables 可供扩展建模读取的选择变量 / Choice variables exposed for model extensions
 * @property cost 总费用表达式 / Total cost expression
 */
class ActivityCompilation internal constructor(size: Int, costs: List<Flt64>, name: String) {
    val variables: List<BinVar> = List(size) { BinVar("${name}_choice_$it") }
    val cost = LinearPolynomial(
        monomials = variables.mapIndexed { index, variable -> LinearMonomial(costs[index], variable) },
        constant = Flt64.zero
    )
}

internal data class CompiledActivityReservation(val resource: ExecutorId, val time: TimeRange, val quantity: Flt64)

internal data class CompiledActivityMaterialEvent(val material: String, val time: Instant, val quantity: Flt64)

internal data class CompiledActivityMaterial<V : RealNumber<V>>(
    val source: ActivityMaterial<V>,
    val initial: Flt64,
    val minimum: Flt64,
    val maximum: Flt64?
)

internal data class CompiledActivityChoice<V : RealNumber<V>>(
    val task: TaskId,
    val placement: ActivityPlacement<V>?,
    val reservations: List<CompiledActivityReservation>,
    val cost: Flt64,
    val materialEvents: List<CompiledActivityMaterialEvent> = emptyList()
)

internal data class CompiledActivityResource<V : RealNumber<V>>(
    val source: ActivityResource<V>,
    val capacity: Flt64,
    val capacities: List<Pair<TimeRange, Flt64>>
) {
    fun capacityAt(time: Instant): Flt64 {
        if (source.unavailableTimes.any { time >= it.start && time < it.end }) {
            return Flt64.zero
        }
        return capacities.firstOrNull { time >= it.first.start && time < it.first.end }?.second ?: capacity
    }
}

internal data class CompiledActivityRow(
    val id: String,
    val coefficients: Map<Int, Flt64>,
    val lower: Flt64? = null,
    val upper: Flt64? = null
)

internal class CompiledActivityProblem<V : RealNumber<V>>(
    val problem: ActivitySchedulingProblem<V>,
    val choices: List<CompiledActivityChoice<V>>,
    val resources: Map<ExecutorId, CompiledActivityResource<V>>,
    val fixedReservations: List<CompiledActivityReservation>,
    val extraRows: List<CompiledActivityRow>,
    val materials: Map<String, CompiledActivityMaterial<V>>,
    val materialEvents: List<CompiledActivityMaterialEvent>
) {
    val indices = choices.mapIndexedNotNull { index, choice -> choice.placement?.key?.let { it to index } }.toMap()
    val cancellations = choices.mapIndexedNotNull { index, choice -> if (choice.placement == null) choice.task to index else null }.toMap()

    fun endpoint(time: TimeRange, endpoint: ActivityEndpoint): Instant =
        if (endpoint == ActivityEndpoint.Start) time.start else time.end

    fun compatible(dependency: ActivityDependency, predecessor: TimeRange, successor: TimeRange): Boolean {
        val lag = endpoint(successor, dependency.to) - endpoint(predecessor, dependency.from)
        return lag >= dependency.minimum && (dependency.maximum == null || lag <= dependency.maximum)
    }
}

internal class ActivityValueBoundary<V : RealNumber<V>>(val adapter: SchedulingSolverValueAdapter<V>) {
    // 容差相等会掩盖极小非零量，转换边界必须精确判零。 / Approximate equality hides tiny nonzero values; conversion requires exact zero tests.
    private fun isZero(value: V): Boolean = value.abs().partialEq(adapter.zero.abs()) == true

    fun value(quantity: Quantity<V>, unit: PhysicalUnit, label: String): Ret<Flt64> {
        if (quantity.unit.isAffine || unit.isAffine) {
            return activityFailure("线性排程不支持仿射单位：$label / Linear scheduling does not support affine units: $label")
        }
        val normalized = quantity.convertTo(unit)
            ?: return activityFailure("单位不兼容：$label / Incompatible units: $label")
        if (isZero(normalized.value) && !isZero(quantity.value)) {
            return activityFailure("单位换算下溢为零：$label / Unit conversion underflow to zero: $label")
        }
        val value = try {
            adapter.fromValue(normalized.value)
        } catch (error: Exception) {
            return activityFailure("数值转换失败：$label / Numeric conversion failed: $label (${error.message})")
        }
        if (!value.toDouble().isFinite() || value.toDouble() == 0.0 && !isZero(normalized.value)) {
            return activityFailure("数值非有限或下溢为零：$label / Non-finite numeric value or underflow to zero: $label")
        }
        return ok(value)
    }

    fun quantity(value: Flt64, unit: PhysicalUnit): Ret<Quantity<V>> {
        if (!value.toDouble().isFinite()) {
            return activityFailure("费用溢出 / Cost overflow")
        }
        return try {
            val converted = adapter.intoValue(value)
            val roundTrip = adapter.fromValue(converted)
            if (!roundTrip.toDouble().isFinite() || value.toDouble() != 0.0 && roundTrip.toDouble() == 0.0) {
                activityFailure("结果转换非有限或下溢 / Non-finite result conversion or underflow")
            } else {
                ok(Quantity(converted, unit))
            }
        } catch (error: Exception) {
            activityFailure("结果转换失败 / Result conversion failed (${error.message})")
        }
    }
}

internal fun validActivityTime(time: TimeRange): Boolean =
    time.start != Instant.DISTANT_PAST && time.end != Instant.DISTANT_FUTURE &&
        time.start <= time.end && time.duration.isFinite()

internal fun <V : RealNumber<V>> prepareActivityProblem(
    problem: ActivitySchedulingProblem<V>,
    boundary: ActivityValueBoundary<V>
): Ret<CompiledActivityProblem<V>> {
    if (!validActivityTime(problem.horizon) || problem.horizon.empty) {
        return activityFailure("排程区间必须有限且非空 / Scheduling horizon must be finite and nonempty")
    }
    val tasks = problem.activities.associateBy { it.id }
    val resources = problem.resources.associateBy { it.id }
    val facts = problem.executions.associateBy { it.placement.key.task }
    if (tasks.size != problem.activities.size || resources.size != problem.resources.size ||
        facts.size != problem.executions.size || tasks.keys.any { it in facts }
    ) {
        return activityFailure("任务、资源或执行事实身份重复 / Duplicate task, resource or execution identity")
    }
    val materials = linkedMapOf<String, CompiledActivityMaterial<V>>()
    for (material in problem.materials) {
        if (material.id.isBlank() || material.id in materials) {
            return activityFailure("物料身份为空或重复 / Blank or duplicate material identity")
        }
        val values = mutableListOf<Flt64?>()
        for (quantity in listOf(material.initial, material.minimum, material.maximum)) {
            if (quantity == null) {
                values.add(null)
            } else {
                when (val result = boundary.value(quantity, material.initial.unit, "material ${material.id}")) {
                    is Ok -> values.add(result.value)
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
            }
        }
        val initial = values[0] ?: return activityFailure("缺少期初库存 / Missing opening stock")
        val minimum = values[1] ?: Flt64.zero
        val maximum = values[2]
        if ((minimum ls Flt64.zero) || maximum != null && (maximum ls minimum)) {
            return activityFailure("库存边界非法，不允许隐式负库存 / Invalid stock bounds; implicit negative inventory is not allowed")
        }
        materials[material.id] = CompiledActivityMaterial(
            source = material,
            initial = initial,
            minimum = minimum,
            maximum = maximum
        )
    }
    val materialEvents = mutableListOf<CompiledActivityMaterialEvent>()
    for (event in problem.materialEvents) {
        val material = materials[event.material]
            ?: return activityFailure("固定事件引用未知物料 / Fixed event references unknown material")
        if (event.time < problem.horizon.start || event.time > problem.horizon.end) {
            return activityFailure("固定物料事件超出优化区间 / Fixed material event outside the horizon")
        }
        when (val result = boundary.value(event.quantity, material.source.initial.unit, "material event ${event.material}")) {
            is Ok -> materialEvents.add(CompiledActivityMaterialEvent(
                material = event.material,
                time = event.time,
                quantity = result.value
            ))
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
    }
    val normalizedResources = linkedMapOf<ExecutorId, CompiledActivityResource<V>>()
    for (resource in problem.resources) {
        val capacity = when (val result = boundary.value(resource.capacity, resource.capacity.unit, "capacity ${resource.id}")) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        if (capacity ls Flt64.zero) {
            return activityFailure("资源容量不能为负 / Resource capacity cannot be negative")
        }
        if (resource.unavailableTimes.any { !validActivityTime(it) || it.empty } ||
            resource.capacities.any { !validActivityTime(it.time) || it.time.empty }
        ) {
            return activityFailure("资源日历区间非法 / Invalid resource calendar interval")
        }
        val sorted = resource.capacities.sortedBy { it.time.start }
        if (sorted.zipWithNext().any { it.first.time.end > it.second.time.start }) {
            return activityFailure("时变容量覆盖不能重叠 / Capacity overrides cannot overlap")
        }
        val capacities = mutableListOf<Pair<TimeRange, Flt64>>()
        for (override in sorted) {
            val amount = when (val result = boundary.value(override.capacity, resource.capacity.unit, "capacity override ${resource.id}")) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            if (amount ls Flt64.zero) {
                return activityFailure("时变容量不能为负 / Capacity override cannot be negative")
            }
            capacities.add(override.time to amount)
        }
        normalizedResources[resource.id] = CompiledActivityResource(resource, capacity, capacities)
    }
    fun normalizeReservations(placement: ActivityPlacement<V>): Ret<List<CompiledActivityReservation>> {
        if (!validActivityTime(placement.time) || placement.key.start != placement.time.start || placement.key.mode.isBlank()) {
            return activityFailure("活动分配时间或模式非法 / Invalid placement time or mode")
        }
        val result = mutableListOf<CompiledActivityReservation>()
        for (reservation in placement.reservations) {
            val resource = resources[reservation.resource]
                ?: return activityFailure("占用引用未知资源 / Reservation references an unknown resource")
            if (!validActivityTime(reservation.time) || reservation.time.start < placement.time.start ||
                reservation.time.end > placement.time.end
            ) {
                return activityFailure("占用必须位于活动区间内 / Reservation must lie within the activity interval")
            }
            val amount = when (val normalized = boundary.value(reservation.quantity, resource.capacity.unit, "reservation ${reservation.resource}")) {
                is Ok -> normalized.value
                is Failed -> return Failed(normalized.error)
                is Fatal -> return Fatal(normalized.errors)
            }
            if (amount ls Flt64.zero) {
                return activityFailure("占用不能为负 / Reservation cannot be negative")
            }
            result.add(CompiledActivityReservation(reservation.resource, reservation.time, amount))
        }
        return ok(result)
    }
    val fixed = mutableListOf<CompiledActivityReservation>()
    for (fact in problem.executions) {
        val placement = fact.placement
        val invalid = when (fact.status) {
            ActivityExecutionStatus.Completed -> placement.time.end > problem.horizon.start
            ActivityExecutionStatus.Running -> placement.time.start > problem.horizon.start ||
                placement.time.end <= problem.horizon.start ||
                placement.reservations.any { it.time.start < problem.horizon.start }
        }
        if (invalid) {
            return activityFailure("执行事实与优化起点不一致，执行中占用必须显式给出剩余部分 / Execution fact inconsistent with horizon; running reservations must explicitly describe the remainder")
        }
        when (val result = normalizeReservations(placement)) {
            is Ok -> fixed.addAll(result.value)
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
    }
    val baselines = problem.baselines.associateBy { it.choice.task }
    if (baselines.size != problem.baselines.size || baselines.keys.any { it !in tasks }) {
        return activityFailure("基线重复或引用非待排任务 / Duplicate baseline or baseline referencing a non-pending task")
    }
    val baselineCosts = linkedMapOf<TaskId, Pair<Flt64, Flt64>>()
    for (baseline in problem.baselines) {
        if (baseline.choice.mode.isBlank() || baseline.choice.start == Instant.DISTANT_PAST || baseline.choice.start == Instant.DISTANT_FUTURE) {
            return activityFailure("基线身份或时间非法 / Invalid baseline identity or time")
        }
        val costs = mutableListOf<Flt64>()
        for (quantity in listOf(baseline.startChangeCost, baseline.modeChangeCost)) {
            val cost = when (val result = boundary.value(quantity, problem.costUnit, "baseline cost")) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            if (cost ls Flt64.zero) {
                return activityFailure("基线变更费用不能为负 / Baseline change costs cannot be negative")
            }
            costs.add(cost)
        }
        baselineCosts[baseline.choice.task] = costs[0] to costs[1]
    }
    val choices = mutableListOf<CompiledActivityChoice<V>>()
    for (task in problem.activities) {
        if (task.modes.map { it.id }.toSet().size != task.modes.size) {
            return activityFailure("同一活动的模式身份重复 / Duplicate mode identity within an activity")
        }
        for (mode in task.modes) {
            if (mode.id.isBlank() || !mode.duration.isFinite() || mode.duration < Duration.ZERO ||
                mode.starts.toSet().size != mode.starts.size || mode.resources.any {
                    val end = it.until ?: mode.duration
                    !it.from.isFinite() || !end.isFinite() || it.from < Duration.ZERO || end < it.from || end > mode.duration
                }
            ) {
                return activityFailure("模式时长、起点或占用段非法 / Invalid mode duration, starts or resource phases")
            }
            val modeCost = when (val result = boundary.value(mode.cost, problem.costUnit, "mode cost ${mode.id}")) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            // 即使模式没有候选也验证单位及引用。 / Validate references and units even for modes without starts.
            for (use in mode.resources) {
                val resource = resources[use.resource]
                    ?: return activityFailure("模式引用未知资源 / Mode references an unknown resource")
                when (val result = boundary.value(use.quantity, resource.capacity.unit, "mode reservation")) {
                    is Ok -> if (result.value ls Flt64.zero) return activityFailure("占用不能为负 / Usage cannot be negative")
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
            }
            val flows = mutableListOf<Pair<ActivityMaterialFlow<V>, Flt64>>()
            for (flow in mode.materialFlows) {
                val material = materials[flow.material]
                    ?: return activityFailure("模式引用未知物料 / Mode references unknown material")
                if (!flow.at.isFinite() || flow.at < Duration.ZERO || flow.at > mode.duration) {
                    return activityFailure("物料事件必须位于模式执行区间内 / Material event must lie within the mode duration")
                }
                when (val result = boundary.value(flow.quantity, material.source.initial.unit, "mode material ${flow.material}")) {
                    is Ok -> flows.add(flow to result.value)
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
            }
            for (start in mode.starts) {
                if (start < problem.horizon.start || start > problem.horizon.end || mode.duration > problem.horizon.end - start) {
                    return activityFailure("候选超出声明时域 / Candidate outside the declared horizon")
                }
                val placement = ActivityPlacement(
                    key = ActivityChoiceKey(task.id, mode.id, start),
                    time = TimeRange(start, start + mode.duration),
                    reservations = mode.resources.map { use -> ActivityReservation(
                        resource = use.resource,
                        time = TimeRange(start + use.from, start + (use.until ?: mode.duration)),
                        quantity = use.quantity
                    ) }
                )
                val reservations = when (val result = normalizeReservations(placement)) {
                    is Ok -> result.value
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
                var cost = modeCost
                val baseline = baselines[task.id]
                if (baseline != null) {
                    val change = baselineCosts.getValue(task.id)
                    if (baseline.choice.start != start) cost += change.first
                    if (baseline.choice.mode != mode.id) cost += change.second
                }
                val extra = problem.additionalCost?.invoke(placement)
                if (extra != null) {
                    val quantity = when (extra) {
                        is Ok -> extra.value
                        is Failed -> return Failed(extra.error)
                        is Fatal -> return Fatal(extra.errors)
                    }
                    when (val result = boundary.value(quantity, problem.costUnit, "additional cost")) {
                        is Ok -> cost += result.value
                        is Failed -> return Failed(result.error)
                        is Fatal -> return Fatal(result.errors)
                    }
                }
                if (!cost.toDouble().isFinite()) {
                    return activityFailure("候选费用溢出 / Candidate cost overflow")
                }
                choices.add(CompiledActivityChoice(
                    task = task.id,
                    placement = placement,
                    reservations = reservations,
                    cost = cost,
                    materialEvents = flows.map { (flow, quantity) -> CompiledActivityMaterialEvent(
                        material = flow.material,
                        time = start + flow.at,
                        quantity = quantity
                    ) }
                ))
            }
        }
        if (task.cancellationCost != null) {
            val cost = when (val result = boundary.value(task.cancellationCost, problem.costUnit, "cancellation")) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            if (cost ls Flt64.zero) return activityFailure("取消费用不能为负 / Cancellation cost cannot be negative")
            choices.add(CompiledActivityChoice(task.id, null, emptyList(), cost))
        }
    }
    val indices = choices.mapIndexedNotNull { index, choice -> choice.placement?.key?.let { it to index } }.toMap()
    val cancellations = choices.mapIndexedNotNull { index, choice -> if (choice.placement == null) choice.task to index else null }.toMap()
    if (problem.baselines.any { it.frozen && it.choice !in indices }) {
        return activityFailure("冻结基线不在候选域内 / Frozen baseline is outside the candidate domain")
    }
    for (dependency in problem.dependencies) {
        if (dependency.predecessor !in tasks && dependency.predecessor !in facts ||
            dependency.successor !in tasks && dependency.successor !in facts ||
            !dependency.minimum.isFinite() || dependency.maximum?.isFinite() == false ||
            dependency.maximum != null && dependency.maximum < dependency.minimum
        ) {
            return activityFailure("依赖引用或时间间隔非法 / Invalid dependency references or lags")
        }
    }
    if (problem.rows.map { it.id }.toSet().size != problem.rows.size) {
        return activityFailure("扩展行身份重复 / Duplicate extension row identity")
    }
    val extraRows = mutableListOf<CompiledActivityRow>()
    for (row in problem.rows) {
        if (row.id.isBlank() || row.coefficients.keys.any { it !in indices } ||
            row.cancellationCoefficients.keys.any { it !in cancellations } || row.lower == null && row.upper == null
        ) {
            return activityFailure("扩展行身份、引用或边界非法 / Invalid extension row identity, references or bounds")
        }
        val coefficients = linkedMapOf<Int, Flt64>()
        for ((index, quantity) in row.coefficients.mapKeys { indices.getValue(it.key) } +
            row.cancellationCoefficients.mapKeys { cancellations.getValue(it.key) }
        ) {
            when (val result = boundary.value(quantity, row.unit, "row ${row.id}")) {
                is Ok -> coefficients[index] = result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }
        val bounds = mutableListOf<Flt64?>()
        for (quantity in listOf(row.lower, row.upper)) {
            if (quantity == null) {
                bounds.add(null)
            } else {
                when (val result = boundary.value(quantity, row.unit, "bound ${row.id}")) {
                    is Ok -> bounds.add(result.value)
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
            }
        }
        val lower = bounds[0]
        val upper = bounds[1]
        if (lower != null && upper != null && (lower gr upper)) {
            return activityFailure("扩展行下界超过上界 / Extension row lower bound exceeds upper bound")
        }
        extraRows.add(CompiledActivityRow(row.id, coefficients, lower, upper))
    }
    return ok(CompiledActivityProblem(
        problem = problem,
        choices = choices,
        resources = normalizedResources,
        fixedReservations = fixed,
        extraRows = extraRows,
        materials = materials,
        materialEvents = materialEvents
    ))
}
