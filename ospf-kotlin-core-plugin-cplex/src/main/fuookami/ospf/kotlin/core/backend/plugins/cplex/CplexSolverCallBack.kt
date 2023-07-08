package fuookami.ospf.kotlin.core.backend.plugins.cplex

import java.util.*
import ilog.concert.*
import ilog.cplex.*

typealias Function = (IloCplex, List<IloNumVar>, List<IloRange>) -> Unit

enum class Point {
    AfterModeling,
    Configuration,
    AnalyzingSolution
}

class CplexSolverCallBack(
    private val map: MutableMap<Point, Function> = EnumMap(Point::class.java)
) {
    fun set(point: Point, function: Function): CplexSolverCallBack {
        map[point] = function
        return this
    }

    fun afterModeling(function: Function) = set(Point.AfterModeling, function)
    fun configuration(function: Function) = set(Point.Configuration, function)
    fun analyzingSolution(function: Function) = set(Point.AnalyzingSolution, function)

    fun contain(point: Point) = map.containsKey(point)
    fun get(point: Point): Function? = map[point]

    fun execIfContain(point: Point, cplex: IloCplex, variables: List<IloNumVar>, constraints: List<IloRange>) {
        map[point]?.invoke(cplex, variables, constraints)
    }
}
