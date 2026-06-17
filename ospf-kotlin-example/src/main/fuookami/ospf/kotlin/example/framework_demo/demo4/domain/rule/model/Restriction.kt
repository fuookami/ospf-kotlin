@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.rule.model

import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.*

/** 枚举限制严重性级别。Enumerates the restriction severity levels. */
enum class RestrictionType {
    Weak,
    ViolableStrong,
    Strong,
}

/** 可以针对航班任务检查的限制的密封接口。Sealed interface for restrictions that can be checked against flight tasks. */
sealed interface Restriction {
    val type: RestrictionType

    /**
     * Checks whether this restriction is related to the given aircraft.
 *
     * @param aircraft 参数。
     * @return 返回结果。
     */
    fun related(aircraft: Aircraft): Boolean

    /**
     * Checks this restriction against a flight task.
 *
     * @param task 参数。
     * @return 返回结果。
     */
    fun check(task: FlightTask): RestrictionCheckingResult

    /**
     * Checks this restriction against a flight task with a specific aircraft.
 *
     * @param task 参数。
     * @param aircraft 参数。
     * @return 返回结果。
     */
    fun check(task: FlightTask, aircraft: Aircraft): RestrictionCheckingResult

    /**
     * Checks this restriction against a flight task with a recovery policy.
 *
     * @param task 参数。
     * @param recoveryPolicy 参数。
     * @return 返回结果。
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
 */
data class NotMatter(
    override val restriction: Restriction
) : RestrictionCheckingResult

/**
 * 表示强违反的结果。Result indicating a strong violation.
 *
 */
data class Violate(
    override val restriction: Restriction
) : RestrictionCheckingResult

/**
 * 表示无违反的结果。Result indicating no violation.
 *
 */
data class NotViolate(
    override val restriction: Restriction
) : RestrictionCheckingResult

/**
 * 表示可违反（软）违反的结果。Result indicating a violable (soft) violation.
 *
 */
data class ViolableViolate(
    override val restriction: Restriction
) : RestrictionCheckingResult

/** 枚举关系限制类别。Enumerates the relation restriction categories. */
enum class RelationRestrictionCategory {
    BlackList,
    WhiteList
}

/**
 * 基于机场对和飞机集关系的限制。A restriction based on airport pair and aircraft set relationships.
 *
 * @property category 参数。
 * @property dep 参数。
 * @property arr 参数。
 * @property aircrafts 参数。
 * @property weight 参数。
 * @property cost 参数。
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

    private fun check(dep: Airport, arr: Airport, aircraft: Aircraft?): RestrictionCheckingResult {
        if (dep != this.dep || arr != this.arr) {
            return NotMatter(this)
        }
        return aircraft?.let { dump(aircrafts.contains(aircraft)) } ?: NotMatter(this)
    }

    private fun violated(hit: Boolean): Boolean {
        return when (category) {
            RelationRestrictionCategory.BlackList -> hit
            RelationRestrictionCategory.WhiteList -> !hit
        }
    }

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

/** 通用限制条件函数的类型别名。Type alias for a general restriction condition function. */
typealias AbstractGeneralRestrictionCondition = (task: FlightTask, recoveryPolicy: FlightTaskAssignment?) -> Boolean

/**
 * 按特定航班任务键的条件过滤。Condition filtering by specific flight task keys.
 *
 * @property flights 参数。
 */
data class FlightGeneralRestrictionCondition(
    val flights: Set<TaskKey>
) : AbstractGeneralRestrictionCondition {
    /**
     * 
     * @param task 航班任务。
     * @param recoveryPolicy 恢复策略。
     * @return 是否匹配条件。
     */
    override operator fun invoke(task: FlightTask, recoveryPolicy: FlightTaskAssignment?): Boolean {
        return flights.contains(task.key)
    }
}

/**
 * 按出发机场和可选时间范围的条件过滤。Condition filtering by departure airport and optional time range.
 *
 * @property airports 参数。
 * @property time 参数。
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
 * @property airports 参数。
 * @property time 参数。
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
 * @property airports1 参数。
 * @property airports2 参数。
 * @property time 参数。
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
 * @property aircrafts 参数。
 */
data class EnabledAircraftGeneralRestrictionCondition(
    val aircrafts: Set<Aircraft>
) : AbstractGeneralRestrictionCondition {
    /**
     * 
     * @param task 航班任务。
     * @param recoveryPolicy 恢复策略。
     * @return 是否匹配条件。
     */
    override operator fun invoke(task: FlightTask, recoveryPolicy: FlightTaskAssignment?): Boolean {
        return aircrafts.contains(task.aircraft)
    }
}

/**
 * 按禁用飞机集的条件过滤。Condition filtering by disabled aircraft set.
 *
 * @property aircrafts 参数。
 */
data class DisabledAircraftGeneralRestrictionCondition(
    val aircrafts: Set<Aircraft>
) : AbstractGeneralRestrictionCondition {
    /**
     * 
     * @param task 航班任务。
     * @param recoveryPolicy 恢复策略。
     * @return 是否匹配条件。
     */
    override operator fun invoke(task: FlightTask, recoveryPolicy: FlightTaskAssignment?): Boolean {
        return !aircrafts.contains(task.aircraft)
    }
}

/**
 * 具有可配置条件和飞机过滤器的通用限制。A general restriction with configurable conditions and aircraft filters.
 *
 * @property condition 参数。
 * @property enabledAircrafts 参数。
 * @property disabledAircrafts 参数。
 * @property weight 参数。
 * @property cost 参数。
 */
class GeneralRestriction(
    override val type: RestrictionType,
    condition: AbstractGeneralRestrictionCondition? = null,
    val enabledAircrafts: Set<Aircraft>? = null,
    val disabledAircrafts: Set<Aircraft>? = null,
    val weight: FltX = FltX.one,
    val cost: FltX? = null
) : Restriction {
    val condition by lazy {
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
            { task: FlightTask, recoveryPolicy: FlightTaskAssignment? ->
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
