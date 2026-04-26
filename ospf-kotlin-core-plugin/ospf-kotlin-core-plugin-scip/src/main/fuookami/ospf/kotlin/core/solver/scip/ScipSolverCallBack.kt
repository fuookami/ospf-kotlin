package fuookami.ospf.kotlin.core.solver.scip

import fuookami.ospf.kotlin.core.solver.output.SolverStatus
import fuookami.ospf.kotlin.utils.concept.Copyable
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.syncRun
import jscip.Constraint
import jscip.Event
import jscip.EventHandler
import jscip.EventHandlerRef
import jscip.EventMask
import jscip.Scip
import jscip.Variable
import java.util.*

typealias Function = suspend (SolverStatus?, Scip, List<Variable>, List<Constraint>) -> Try
typealias NativeCallBack = EventHandler.(Scip, EventHandlerRef, Event) -> Unit

enum class Point {
    AfterModeling,
    Configuration,
    AnalyzingSolution,
    AfterFailure
}

class ScipSolverCallBack(
    internal var nativeEventMask: Long = EventMask.LP_EVENT or EventMask.NODE_EVENT or EventMask.SOL_EVENT,
    internal var nativeCallback: NativeCallBack? = null,
    private val map: MutableMap<Point, MutableList<Function>> = EnumMap(Point::class.java)
) : Copyable<ScipSolverCallBack> {
    @JvmName("setNativeEventMask")
    fun set(eventMask: Long) {
        nativeEventMask = eventMask
    }

    @JvmName("setNativeCallback")
    fun set(function: NativeCallBack) {
        nativeCallback = function
    }

    @JvmName("setNativeCallbackWithEventMask")
    fun set(eventMask: Long, function: NativeCallBack) {
        nativeEventMask = eventMask
        nativeCallback = function
    }

    fun set(point: Point, function: Function): ScipSolverCallBack {
        map.getOrPut(point) { ArrayList() }.add(function)
        return this
    }

    fun native(eventMask: Long = nativeEventMask, function: NativeCallBack): ScipSolverCallBack {
        set(eventMask, function)
        return this
    }

    fun afterModeling(function: Function) = set(Point.AfterModeling, function)
    fun configuration(function: Function) = set(Point.Configuration, function)
    fun analyzingSolution(function: Function) = set(Point.AnalyzingSolution, function)
    fun afterFailure(function: Function) = set(Point.AfterFailure, function)

    fun contains(point: Point) = map.containsKey(point)
    fun get(point: Point): List<Function>? = map[point]

    suspend fun execIfContain(
        point: Point,
        status: SolverStatus?,
        scip: Scip,
        variables: List<Variable>,
        constraints: List<Constraint>
    ): Try? {
        return if (!map[point].isNullOrEmpty()) {
            syncRun(map[point]!!.map {
                { it(status, scip, variables, constraints) }
            })
        } else {
            null
        }
    }

    override fun copy(): ScipSolverCallBack {
        return ScipSolverCallBack(
            nativeEventMask = nativeEventMask,
            nativeCallback = nativeCallback,
            map.toMutableMap()
        )
    }
}
