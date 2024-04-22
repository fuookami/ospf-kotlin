package fuookami.ospf.kotlin.core.backend.plugins.cplex

import java.util.*
import ilog.concert.*
import ilog.cplex.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.functional.*

typealias NativeCallback = IloCplex.Callback.() -> Unit
typealias Function = (IloCplex, List<IloNumVar>, List<IloRange>) -> Try

enum class Point {
    AfterModeling,
    Configuration,
    Solving,
    AnalyzingSolution
}

class CplexSolverCallBack(
    internal var nativeCallback: NativeCallback? = null,
    private val map: MutableMap<Point, Function> = EnumMap(Point::class.java)
) : Copyable<CplexSolverCallBack> {
    fun set(function: NativeCallback) {
        nativeCallback = function
    }

    fun set(point: Point, function: Function): CplexSolverCallBack {
        map[point] = function
        return this
    }

    fun afterModeling(function: Function) = set(Point.AfterModeling, function)
    fun configuration(function: Function) = set(Point.Configuration, function)
    fun solving(function: Function) = set(Point.Solving, function)
    fun analyzingSolution(function: Function) = set(Point.AnalyzingSolution, function)

    fun contain(point: Point) = map.containsKey(point)
    fun get(point: Point): Function? = map[point]

    fun execIfContain(point: Point, cplex: IloCplex, variables: List<IloNumVar>, constraints: List<IloRange>): Try? {
        return map[point]?.invoke(cplex, variables, constraints)
    }

    override fun copy(): CplexSolverCallBack {
        return CplexSolverCallBack(nativeCallback, map.toMutableMap())
    }
}
