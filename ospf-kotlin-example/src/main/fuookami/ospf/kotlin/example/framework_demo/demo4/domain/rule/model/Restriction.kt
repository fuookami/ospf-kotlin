@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.rule.model

import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.*

/** 枚举限制严重性级别。Enumerates the restriction severity levels. */
enum class RestrictionType {
    /** Weak (soft) restriction / 弱（软）限制 */
    Weak,
    /** Violable strong restriction with penalty / 可违反的强限制（带惩罚） */
    ViolableStrong,
    /** Strong (hard) restriction / 强（硬）限制 */
    Strong,
}

/** 可以针对航班任务检查的限制的密封接口。Sealed interface for restrictions that can be checked against flight tasks. */
sealed interface Restriction {
    val type: RestrictionType

    /**
     * 检查此限制是否与给定飞机相关。Checks whether this restriction is related to the given aircraft.
     *
     * @param aircraft 要检查关联关系的飞机 / The aircraft to check relation for
     * @return 此限制是否与给定飞机相关 / Whether this restriction is related to the given aircraft
    */
    fun related(aircraft: Aircraft): Boolean

    /**
     * 针对航班任务检查此限制。Checks this restriction against a flight task.
     *
     * @param task 要检查的航班任务 / The flight task to check against
     * @return 限制检查结果 / The restriction checking result
    */
    fun check(task: FlightTask): RestrictionCheckingResult

    /**
     * 针对指定飞机的航班任务检查此限制。Checks this restriction against a flight task with a specific aircraft.
     *
     * @param task 要检查的航班任务 / The flight task to check against
     * @param aircraft 用于检查的指定飞机 / The specific aircraft to check with
     * @return 限制检查结果 / The restriction checking result
    */
    fun check(task: FlightTask, aircraft: Aircraft): RestrictionCheckingResult

    /**
     * 针对具有恢复策略的航班任务检查此限制。Checks this restriction against a flight task with a recovery policy.
     *
     * @param task 要检查的航班任务 / The flight task to check against
     * @param recoveryPolicy 恢复策略分配 / The recovery policy assignment
     * @return 限制检查结果 / The restriction checking result
    */
    fun check(task: FlightTask, recoveryPolicy: FlightTaskAssignment): RestrictionCheckingResult
}

/** 限制检查结果的密封接口。Sealed interface for restriction checking results. */
sealed interface RestrictionCheckingResult {
    val restriction: Restriction

    val type get() = restriction.type
}

/**
 * 表示限制不适用的结果。Result indicating the restriction does not apply.
 *
 * @param restriction 限制 / Restriction
*/
data class NotMatter(
    override val restriction: Restriction
) : RestrictionCheckingResult

/**
 * 表示强违反的结果。Result indicating a strong violation.
 *
 * @param restriction 限制 / Restriction
*/
data class Violate(
    override val restriction: Restriction
) : RestrictionCheckingResult

/**
 * 表示无违反的结果。Result indicating no violation.
 *
 * @param restriction 限制 / Restriction
*/
data class NotViolate(
    override val restriction: Restriction
) : RestrictionCheckingResult

/**
 * 表示可违反（软）违反的结果。Result indicating a violable (soft) violation.
 *
 * @param restriction 限制 / Restriction
*/
data class ViolableViolate(
    override val restriction: Restriction
) : RestrictionCheckingResult

/** 枚举关系限制类别。Enumerates the relation restriction categories. */
enum class RelationRestrictionCategory {
    /** Blacklist restriction / 黑名单限制 */
    BlackList,
    /** Whitelist restriction / 白名单限制 */
    WhiteList
}

/**
 * 基于机场对和飞机集关系的限制。A restriction based on airport pair and aircraft set relationships.
 *
 * @property category 关系限制类别（黑名单/白名单） / The relation restriction category (blacklist/whitelist)
 * @property dep 出发机场 / The departure airport
 * @property arr 到达机场 / The arrival airport
 * @property aircrafts 受此限制约束的飞机集合 / The set of aircrafts subject to this restriction
 * @property weight 违反惩罚权重 / The penalty weight for violation
 * @property cost 违反惩罚代价，不适用时为 null / The penalty cost for violation, or null if not applicable
*/
class RelationRestriction(
    override val type: RestrictionType,
    val category: RelationRestrictionCategory,
    val dep: Airport,
    val arr: Airport,
    val aircrafts: Set<Aircraft>,
    val weight: FltX = FltX.one,
    val cost: FltX? = null
) : Restriction {
    override fun related(aircraft: Aircraft): Boolean {
        return aircrafts.contains(aircraft)
    }

    override fun check(task: FlightTask): RestrictionCheckingResult {
        assert(task.isFlight)
        return check(task.dep, task.arr, task.aircraft)
    }

    override fun check(task: FlightTask, aircraft: Aircraft): RestrictionCheckingResult {
        assert(task.isFlight)
        return check(task.dep, task.arr, aircraft)
    }

    override fun check(task: FlightTask, recoveryPolicy: FlightTaskAssignment): RestrictionCheckingResult {
        assert(task.isFlight)
        val dep = recoveryPolicy.route?.dep ?: task.dep
        val arr = recoveryPolicy.route?.arr ?: task.arr
        val aircraft = recoveryPolicy.aircraft ?: task.aircraft
        return check(dep, arr, aircraft)
    }

    /**
     * Checks this restriction against the given departure airport, arrival airport, and aircraft.
     * 根据给定的出发机场、到达机场和飞机检查此限制。
     *
     * @param dep 出发机场 / Departure airport
     * @param arr 到达机场 / Arrival airport
     * @param aircraft 待检查的飞机，未指定时为 null / Aircraft to check, or null if unspecified
     * @return 限制检查结果 / The restriction checking result
    */
    private fun check(dep: Airport, arr: Airport, aircraft: Aircraft?): RestrictionCheckingResult {
        if (dep != this.dep || arr != this.arr) {
            return NotMatter(this)
        }
        return aircraft?.let { dump(aircrafts.contains(aircraft)) } ?: NotMatter(this)
    }

    /**
     * Determines whether a hit constitutes a violation based on the restriction category.
     * 根据限制类别判断命中是否构成违反。
     *
     * @param hit 飞机是否在限制集合中 / Whether the aircraft is in the restricted set
     * @return 命中是否构成违反 / True if the hit is a violation, false otherwise
    */
    private fun violated(hit: Boolean): Boolean {
        return when (category) {
            RelationRestrictionCategory.BlackList -> hit
            RelationRestrictionCategory.WhiteList -> !hit
        }
    }

    /**
     * Produces the appropriate restriction checking result based on whether a violation occurred.
     * 根据是否发生违反生成相应的限制检查结果。
     *
     * @param hit 飞机是否匹配限制集合 / Whether the aircraft matches the restricted set
     * @return 限制检查结果 / The restriction checking result
    */
    private fun dump(hit: Boolean): RestrictionCheckingResult {
        return if (violated(hit)) {
            when (type) {
                RestrictionType.Strong -> {
                    if (cost != null) {
                        ViolableViolate(this)
                    } else {
                        Violate(this)
                    }
                }

                else -> {
                    ViolableViolate(this)
                }
            }
        } else {
            NotViolate(this)
        }
    }
}

/** 通用限制条件接口。Interface for a general restriction condition. */
fun interface AbstractGeneralRestrictionCondition {
    operator fun invoke(task: FlightTask, recoveryPolicy: FlightTaskAssignment?): Boolean
}

/**
 * 按特定航班任务键的条件过滤。Condition filtering by specific flight task keys.
 *
 * @property flights 用于过滤的航班任务键集合 / The set of flight task keys to filter by
*/
data class FlightGeneralRestrictionCondition(
    val flights: Set<TaskKey>
) : AbstractGeneralRestrictionCondition {

    /**
     * Evaluates whether the given flight task matches the filter condition.
     * 判断给定航班任务是否匹配过滤条件。
     *
     * @param task 要评估的航班任务 / The flight task to evaluate
     * @param recoveryPolicy 恢复策略分配，可为 null / The recovery policy assignment, or null
     * @return 任务是否匹配条件 / Whether the task matches the condition
    */
    override operator fun invoke(task: FlightTask, recoveryPolicy: FlightTaskAssignment?): Boolean {
        return flights.contains(task.key)
    }
}

/**
 * 按出发机场和可选时间范围的条件过滤。Condition filtering by departure airport and optional time range.
 *
 * @property airports 用于过滤的出发机场集合 / The set of departure airports to filter by
 * @property time 可选的过滤时间范围 / The optional time range for filtering
*/
data class DepartureAirportGeneralRestrictionCondition(
    val airports: Set<Airport>,
    val time: TimeRange? = null
) : AbstractGeneralRestrictionCondition {
    override operator fun invoke(task: FlightTask, recoveryPolicy: FlightTaskAssignment?): Boolean {
        return airports.contains(task.dep)
                && (time == null || (recoveryPolicy?.time ?: task.time)?.let { time.contains(it.start) } == true)
    }
}

/**
 * 按到达机场和可选时间范围的条件过滤。Condition filtering by arrival airport and optional time range.
 *
 * @property airports 用于过滤的到达机场集合 / The set of arrival airports to filter by
 * @property time 可选的过滤时间范围 / The optional time range for filtering
*/
data class ArrivalAirportGeneralRestrictionCondition(
    val airports: Set<Airport>,
    val time: TimeRange? = null
) : AbstractGeneralRestrictionCondition {
    override operator fun invoke(task: FlightTask, recoveryPolicy: FlightTaskAssignment?): Boolean {
        return airports.contains(task.arr)
                && (time == null || (recoveryPolicy?.time ?: task.time)?.let { time.contains(it.end) } == true)
    }
}

/**
 * 按双向机场对和可选时间范围的条件过滤。Condition filtering by bidirectional airport pairs and optional time range.
 *
 * @property airports1 双向机场对中的第一个机场集合 / The first set of airports in the bidirectional pair
 * @property airports2 双向机场对中的第二个机场集合 / The second set of airports in the bidirectional pair
 * @property time 可选的过滤时间范围 / The optional time range for filtering
*/
data class BidirectionalAirportGeneralRestrictionCondition(
    val airports1: Set<Airport>,
    val airports2: Set<Airport>,
    val time: TimeRange? = null
) : AbstractGeneralRestrictionCondition {
    override operator fun invoke(task: FlightTask, recoveryPolicy: FlightTaskAssignment?): Boolean {
        val from = airports1.contains(task.dep) && airports2.contains(task.arr)
        val to = airports1.contains(task.arr) && airports2.contains(task.dep)
        return (from || to)
                && (time == null || (recoveryPolicy?.time ?: task.time)?.let { time.contains(it) } == true)
    }
}

/**
 * 按启用飞机集的条件过滤。Condition filtering by enabled aircraft set.
 *
 * @property aircrafts 允许的启用飞机集合 / The set of enabled aircrafts to allow
*/
data class EnabledAircraftGeneralRestrictionCondition(
    val aircrafts: Set<Aircraft>
) : AbstractGeneralRestrictionCondition {

    /**
     * Evaluates whether the given flight task's aircraft is in the enabled set.
     * 判断给定航班任务的飞机是否在启用集合中。
     *
     * @param task 要评估的航班任务 / The flight task to evaluate
     * @param recoveryPolicy 恢复策略分配，可为 null / The recovery policy assignment, or null
     * @return 任务的飞机是否已启用 / Whether the task's aircraft is enabled
    */
    override operator fun invoke(task: FlightTask, recoveryPolicy: FlightTaskAssignment?): Boolean {
        return aircrafts.contains(task.aircraft)
    }
}

/**
 * 按禁用飞机集的条件过滤。Condition filtering by disabled aircraft set.
 *
 * @property aircrafts 禁用的飞机集合 / The set of disabled aircrafts to block
*/
data class DisabledAircraftGeneralRestrictionCondition(
    val aircrafts: Set<Aircraft>
) : AbstractGeneralRestrictionCondition {

    /**
     * Evaluates whether the given flight task's aircraft is NOT in the disabled set.
     * 判断给定航班任务的飞机是否不在禁用集合中。
     *
     * @param task 要评估的航班任务 / The flight task to evaluate
     * @param recoveryPolicy 恢复策略分配，可为 null / The recovery policy assignment, or null
     * @return 任务的飞机是否未被禁用 / Whether the task's aircraft is not disabled
    */
    override operator fun invoke(task: FlightTask, recoveryPolicy: FlightTaskAssignment?): Boolean {
        return !aircrafts.contains(task.aircraft)
    }
}

/**
 * 具有可配置条件和飞机过滤器的通用限制。A general restriction with configurable conditions and aircraft filters.
 *
 * @property condition 限制条件函数，无则为 null / The restriction condition function, or null if none
 * @property enabledAircrafts 启用飞机集合，全部允许时为 null / The set of enabled aircrafts, or null if all are allowed
 * @property disabledAircrafts 禁用飞机集合，无禁用时为 null / The set of disabled aircrafts, or null if none are blocked
 * @property weight 违反惩罚权重 / The penalty weight for violation
 * @property cost 违反惩罚代价，不适用时为 null / The penalty cost for violation, or null if not applicable
*/
class GeneralRestriction(
    override val type: RestrictionType,
    condition: AbstractGeneralRestrictionCondition? = null,
    val enabledAircrafts: Set<Aircraft>? = null,
    val disabledAircrafts: Set<Aircraft>? = null,
    val weight: FltX = FltX.one,
    val cost: FltX? = null
) : Restriction {
    val condition: AbstractGeneralRestrictionCondition? by lazy {
        val condition1 = if (enabledAircrafts != null) {
            EnabledAircraftGeneralRestrictionCondition(enabledAircrafts)
        } else {
            null
        }
        val condition2 = if (disabledAircrafts != null) {
            DisabledAircraftGeneralRestrictionCondition(disabledAircrafts)
        } else {
            null
        }
        if (condition != null || condition1 != null || condition2 != null) {
            AbstractGeneralRestrictionCondition { task, recoveryPolicy ->
                condition?.invoke(task, recoveryPolicy) != false
                        && condition1?.invoke(task, recoveryPolicy) != false
                        && condition2?.invoke(task, recoveryPolicy) != false
            }
        } else {
            null
        }
    }

    override fun related(aircraft: Aircraft): Boolean {
        return enabledAircrafts?.contains(aircraft) == true || disabledAircrafts?.contains(aircraft) == true
    }

    override fun check(task: FlightTask): RestrictionCheckingResult {
        return dump(condition?.invoke(task, null) ?: false)
    }

    override fun check(task: FlightTask, aircraft: Aircraft): RestrictionCheckingResult {
        return dump(condition?.invoke(task, FlightTaskAssignment(aircraft = aircraft)) ?: false)
    }

    override fun check(task: FlightTask, recoveryPolicy: FlightTaskAssignment): RestrictionCheckingResult {
        return dump(condition?.invoke(task, recoveryPolicy) ?: false)
    }

    /**
     * Produces the appropriate restriction checking result based on whether a violation occurred.
     * 根据是否发生违反生成相应的限制检查结果。
     *
     * @param violated 条件是否被违反 / Whether the condition is violated
     * @return 限制检查结果 / The restriction checking result
    */
    private fun dump(violated: Boolean): RestrictionCheckingResult {
        return if (violated) {
            when (type) {
                RestrictionType.Strong -> {
                    if (cost != null) {
                        ViolableViolate(this)
                    } else {
                        Violate(this)
                    }
                }

                else -> {
                    ViolableViolate(this)
                }
            }
        } else {
            NotMatter(this)
        }
    }
}
