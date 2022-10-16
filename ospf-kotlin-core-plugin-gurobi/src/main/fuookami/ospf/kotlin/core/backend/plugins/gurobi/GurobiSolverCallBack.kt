package fuookami.ospf.kotlin.core.backend.plugins.gurobi

import java.util.*
import gurobi.*

typealias Function = (GRBModel, List<GRBVar>, List<GRBConstr>) -> Unit

enum class Point {
    AfterModeling,
    Configuration,
    AnalyzingSolution
}

class GurobiSolverCallBack(
    private val map: MutableMap<Point, Function> = EnumMap(Point::class.java)
) {
    fun set(point: Point, function: Function): GurobiSolverCallBack {
        map[point] = function
        return this
    }

    fun afterModeling(function: Function) = set(Point.AfterModeling, function)
    fun configuration(function: Function) = set(Point.Configuration, function)
    fun analyzingSolution(function: Function) = set(Point.AnalyzingSolution, function)

    fun contain(point: Point) = map.containsKey(point)
    fun get(point: Point): Function? = map[point]

    fun execIfContain(point: Point, gurobi: GRBModel, variables: List<GRBVar>, constraints: List<GRBConstr>) {
        map[point]?.invoke(gurobi, variables, constraints)
    }
}
