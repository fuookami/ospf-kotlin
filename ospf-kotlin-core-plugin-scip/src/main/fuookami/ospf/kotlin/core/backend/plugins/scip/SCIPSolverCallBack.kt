package fuookami.ospf.kotlin.core.backend.plugins.scip

import java.util.*
import jscip.*
import fuookami.ospf.kotlin.utils.concept.*

typealias Function = (Scip, List<Variable>, List<Constraint>) -> Unit

enum class Point {
    AfterModeling,
    Configuration,
    AnalyzingSolution
}

class SCIPSolverCallBack(
    private val map: MutableMap<Point, Function> = EnumMap(Point::class.java)
): Copyable<SCIPSolverCallBack> {
    fun set(point: Point, function: Function): SCIPSolverCallBack {
        map[point] = function
        return this
    }

    fun afterModeling(function: Function) = set(Point.AfterModeling, function)
    fun configuration(function: Function) = set(Point.Configuration, function)
    fun analyzingSolution(function: Function) = set(Point.AnalyzingSolution, function)

    fun contain(point: Point) = map.containsKey(point)
    fun get(point: Point): Function? = map[point]

    fun execIfContain(point: Point, scip: Scip, variables: List<Variable>, constraints: List<Constraint>) {
        map[point]?.invoke(scip, variables, constraints)
    }

    override fun copy(): SCIPSolverCallBack {
        return SCIPSolverCallBack(
            map.toMutableMap()
        )
    }
}
