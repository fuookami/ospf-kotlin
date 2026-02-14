package fuookami.ospf.kotlin.core.backend.plugins.scip

import java.util.*
import jscip.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.backend.solver.output.*

typealias Function = suspend (SolverStatus?, Scip, List<Variable>, List<Constraint>) -> Try

enum class Point {
    AfterModeling,
    Configuration,
    AnalyzingSolution,
    AfterFailure
}

class ScipSolverCallBack(
    private val map: MutableMap<Point, MutableList<Function>> = EnumMap(Point::class.java)
) : Copyable<ScipSolverCallBack> {
    fun set(point: Point, function: Function): ScipSolverCallBack {
        map.getOrPut(point) { ArrayList() }.add(function)
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
            map.toMutableMap()
        )
    }
}
