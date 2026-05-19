@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.rule.model


import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.*

enum class RestrictionType {
    Weak,
    ViolableStrong,
    Strong,
}

sealed interface Restriction {
    val type: RestrictionType

    fun related(aircraft: Aircraft): Boolean

    fun check(task: FlightTask): RestrictionCheckingResult
    fun check(task: FlightTask, aircraft: Aircraft): RestrictionCheckingResult
    fun check(task: FlightTask, recoveryPolicy: FlightTaskAssignment): RestrictionCheckingResult
}

sealed interface RestrictionCheckingResult {
    val restriction: Restriction

    val type get() = restriction.type
}

data class NotMatter(
    override val restriction: Restriction
) : RestrictionCheckingResult

data class Violate(
    override val restriction: Restriction
) : RestrictionCheckingResult

data class NotViolate(
    override val restriction: Restriction
) : RestrictionCheckingResult

data class ViolableViolate(
    override val restriction: Restriction
) : RestrictionCheckingResult

enum class RelationRestrictionCategory {
    BlackList,
    WhiteList
}

class RelationRestriction(
    override val type: RestrictionType,
    val category: RelationRestrictionCategory,
    val dep: Airport,
    val arr: Airport,
    val aircrafts: Set<Aircraft>,
    val weight: Flt64 = Flt64.one,
    val cost: Flt64? = null
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

typealias AbstractGeneralRestrictionCondition = (task: FlightTask, recoveryPolicy: FlightTaskAssignment?) -> Boolean

data class FlightGeneralRestrictionCondition(
    val flights: Set<TaskKey>
) : AbstractGeneralRestrictionCondition {
    override operator fun invoke(task: FlightTask, recoveryPolicy: FlightTaskAssignment?): Boolean {
        return flights.contains(task.key)
    }
}

data class DepartureAirportGeneralRestrictionCondition(
    val airports: Set<Airport>,
    val time: TimeRange? = null
) : AbstractGeneralRestrictionCondition {
    override operator fun invoke(task: FlightTask, recoveryPolicy: FlightTaskAssignment?): Boolean {
        return airports.contains(task.dep)
                && (time == null || (recoveryPolicy?.time ?: task.time)?.let { time.contains(it.start) } == true)
    }
}

data class ArrivalAirportGeneralRestrictionCondition(
    val airports: Set<Airport>,
    val time: TimeRange? = null
) : AbstractGeneralRestrictionCondition {
    override operator fun invoke(task: FlightTask, recoveryPolicy: FlightTaskAssignment?): Boolean {
        return airports.contains(task.arr)
                && (time == null || (recoveryPolicy?.time ?: task.time)?.let { time.contains(it.end) } == true)
    }
}

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

data class EnabledAircraftGeneralRestrictionCondition(
    val aircrafts: Set<Aircraft>
) : AbstractGeneralRestrictionCondition {
    override operator fun invoke(task: FlightTask, recoveryPolicy: FlightTaskAssignment?): Boolean {
        return aircrafts.contains(task.aircraft)
    }
}

data class DisabledAircraftGeneralRestrictionCondition(
    val aircrafts: Set<Aircraft>
) : AbstractGeneralRestrictionCondition {
    override operator fun invoke(task: FlightTask, recoveryPolicy: FlightTaskAssignment?): Boolean {
        return !aircrafts.contains(task.aircraft)
    }
}

class GeneralRestriction(
    override val type: RestrictionType,
    condition: AbstractGeneralRestrictionCondition? = null,
    val enabledAircrafts: Set<Aircraft>? = null,
    val disabledAircrafts: Set<Aircraft>? = null,
    val weight: Flt64 = Flt64.one,
    val cost: Flt64? = null
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
