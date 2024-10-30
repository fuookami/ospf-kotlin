package fuookami.ospf.kotlin.core.backend.plugins.scip

import java.util.*
import jscip.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.functional.*

typealias Function = (Scip, List<Variable>, List<Constraint>) -> Try

enum class Point {
    AfterModeling,
    Configuration,
    AnalyzingSolution
}

class ScipSolverCallBack(
    private val map: MutableMap<Point, Function> = EnumMap(Point::class.java)
) : Copyable<ScipSolverCallBack> {
    fun set(point: Point, function: Function): ScipSolverCallBack {
        map[point] = function
        return this
    }

    fun afterModeling(function: Function) = set(Point.AfterModeling, function)
    fun configuration(function: Function) = set(Point.Configuration, function)
    fun analyzingSolution(function: Function) = set(Point.AnalyzingSolution, function)

    fun contain(point: Point) = map.containsKey(point)
    fun get(point: Point): Function? = map[point]

    fun execIfContain(point: Point, scip: Scip, variables: List<Variable>, constraints: List<Constraint>): Try? {
        return map[point]?.invoke(scip, variables, constraints)
    }

    override fun copy(): ScipSolverCallBack {
        return ScipSolverCallBack(
            map.toMutableMap()
        )
    }
}
