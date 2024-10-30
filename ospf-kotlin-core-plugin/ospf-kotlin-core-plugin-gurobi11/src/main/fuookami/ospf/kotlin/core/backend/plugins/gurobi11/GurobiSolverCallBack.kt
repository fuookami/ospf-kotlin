package fuookami.ospf.kotlin.core.backend.plugins.gurobi11

import java.util.*
import com.gurobi.gurobi.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.functional.*

typealias LinearFunction = (GRBModel, List<GRBVar>, List<GRBConstr>) -> Try
typealias NativeCallback = GRBCallback.() -> Unit
typealias QuadraticFunction = (GRBModel, List<GRBVar>, List<GRBQConstr>) -> Try

enum class Point {
    AfterModeling,
    Configuration,
    AnalyzingSolution,
    AfterFailure
}

class GurobiLinearSolverCallBack(
    internal var nativeCallback: NativeCallback? = null,
    private val map: MutableMap<Point, LinearFunction> = EnumMap(Point::class.java)
) : Copyable<GurobiLinearSolverCallBack> {
    fun set(function: NativeCallback) {
        nativeCallback = function
    }

    operator fun set(point: Point, function: LinearFunction): GurobiLinearSolverCallBack {
        map[point] = function
        return this
    }

    fun afterModeling(function: LinearFunction) = set(Point.AfterModeling, function)
    fun configuration(function: LinearFunction) = set(Point.Configuration, function)
    fun analyzingSolution(function: LinearFunction) = set(Point.AnalyzingSolution, function)
    fun afterFailure(function: LinearFunction) = set(Point.AfterFailure, function)

    fun contain(point: Point) = map.containsKey(point)
    fun get(point: Point): LinearFunction? = map[point]

    fun execIfContain(point: Point, gurobi: GRBModel, variables: List<GRBVar>, constraints: List<GRBConstr>): Try? {
        return map[point]?.invoke(gurobi, variables, constraints)
    }

    override fun copy(): GurobiLinearSolverCallBack {
        return GurobiLinearSolverCallBack(nativeCallback, map.toMutableMap())
    }
}

class GurobiQuadraticSolverCallBack(
    internal var nativeCallback: NativeCallback? = null,
    private val map: MutableMap<Point, QuadraticFunction> = EnumMap(Point::class.java)
) : Copyable<GurobiQuadraticSolverCallBack> {
    fun set(function: NativeCallback) {
        nativeCallback = function
    }

    operator fun set(point: Point, function: QuadraticFunction): GurobiQuadraticSolverCallBack {
        map[point] = function
        return this
    }

    fun afterModeling(function: QuadraticFunction) = set(Point.AfterModeling, function)
    fun configuration(function: QuadraticFunction) = set(Point.Configuration, function)
    fun analyzingSolution(function: QuadraticFunction) = set(Point.AnalyzingSolution, function)
    fun afterFailure(function: QuadraticFunction) = set(Point.AfterFailure, function)

    fun contain(point: Point) = map.containsKey(point)
    fun get(point: Point): QuadraticFunction? = map[point]

    fun execIfContain(point: Point, gurobi: GRBModel, variables: List<GRBVar>, constraints: List<GRBQConstr>): Try? {
        return map[point]?.invoke(gurobi, variables, constraints)
    }

    override fun copy(): GurobiQuadraticSolverCallBack {
        return GurobiQuadraticSolverCallBack(nativeCallback, map.toMutableMap())
    }
}
