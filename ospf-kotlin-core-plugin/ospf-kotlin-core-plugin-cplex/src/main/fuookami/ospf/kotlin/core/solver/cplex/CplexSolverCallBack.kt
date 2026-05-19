package fuookami.ospf.kotlin.core.solver.cplex

import fuookami.ospf.kotlin.core.solver.output.SolverStatus
import fuookami.ospf.kotlin.utils.concept.Copyable
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.syncRun
import ilog.concert.IloNumVar
import ilog.concert.IloRange
import ilog.cplex.IloCplex
import java.util.*

typealias NativeCallback = IloCplex.Callback.() -> Unit
typealias Function = suspend (SolverStatus?, IloCplex, List<IloNumVar>, List<IloRange>) -> Try

enum class Point {
    AfterModeling,
    Configuration,
    Solving,
    AnalyzingSolution,
    AfterFailure
}

class CplexSolverCallBack(
    internal var nativeCallback: NativeCallback? = null,
    private val map: MutableMap<Point, MutableList<Function>> = EnumMap(Point::class.java)
) : Copyable<CplexSolverCallBack> {
    fun set(function: NativeCallback) {
        nativeCallback = function
    }

    fun set(point: Point, function: Function): CplexSolverCallBack {
        map.getOrPut(point) { ArrayList() }.add(function)
        return this
    }

    fun afterModeling(function: Function) = set(Point.AfterModeling, function)
    fun configuration(function: Function) = set(Point.Configuration, function)
    fun solving(function: Function) = set(Point.Solving, function)
    fun analyzingSolution(function: Function) = set(Point.AnalyzingSolution, function)
    fun afterFailure(function: Function) = set(Point.AfterFailure, function)

    fun contains(point: Point) = map.containsKey(point)
    fun get(point: Point): List<Function>? = map[point]

    suspend fun execIfContain(
        point: Point,
        status: SolverStatus?,
        cplex: IloCplex,
        variables: List<IloNumVar>,
        constraints: List<IloRange>
    ): Try? {
        return if (!map[point].isNullOrEmpty()) {
            syncRun(map[point]!!.map {
                { it(status, cplex, variables, constraints) }
            })
        } else {
            null
        }
    }

    override fun copy(): CplexSolverCallBack {
        return CplexSolverCallBack(
            nativeCallback = nativeCallback,
            map = map.toMutableMap()
        )
    }
}