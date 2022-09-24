package fuookami.ospf.kotlin.core.backend.plugins.scip

import jscip.Scip
import java.util.*

typealias Function = (Scip, List<jscip.Variable>, List<jscip.Constraint>) -> Unit

enum class Point {
    AfterModeling,
    Configuration,
    AnalyzingSolution
}

class SCIPSolverCallBack(
    private val map: MutableMap<Point, Function> = EnumMap(Point::class.java)
) {
    fun set(point: Point, function: Function): SCIPSolverCallBack {
        map[point] = function
        return this
    }

    fun afterModeling(function: Function) = set(Point.AfterModeling, function)
    fun configuration(function: Function) = set(Point.Configuration, function)
    fun analyzingSolution(function: Function) = set(Point.AnalyzingSolution, function)

    fun contain(point: Point) = map.containsKey(point)
    fun get(point: Point): Function? = map[point]

    fun execIfContain(point: Point, scip: Scip, variables: List<jscip.Variable>, constraints: List<jscip.Constraint>) {
        map[point]?.invoke(scip, variables, constraints)
    }
}
