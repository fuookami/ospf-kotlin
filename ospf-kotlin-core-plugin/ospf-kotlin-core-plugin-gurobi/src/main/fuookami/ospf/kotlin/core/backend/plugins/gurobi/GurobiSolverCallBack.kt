package fuookami.ospf.kotlin.core.backend.plugins.gurobi

import java.util.*
import gurobi.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.functional.*

typealias CreatingEnvironmentFunction = (GRBEnv) -> Try
typealias NativeCallBack = GRBCallback.() -> Unit
typealias LinearFunction = (GRBModel, List<GRBVar>, List<GRBConstr>) -> Try
typealias QuadraticFunction = (GRBModel, List<GRBVar>, List<GRBQConstr>) -> Try

enum class Point {
    AfterModeling,
    Configuration,
    AnalyzingSolution,
    AfterFailure
}

class GurobiLinearSolverCallBack(
    internal var nativeCallback: NativeCallBack? = null,
    internal var creatingEnvironmentFunction: CreatingEnvironmentFunction? = null,
    private val map: MutableMap<Point, LinearFunction> = EnumMap(Point::class.java)
) : Copyable<GurobiLinearSolverCallBack> {
    @JvmName("setNativeCallback")
    fun set(function: NativeCallBack) {
        nativeCallback = function
    }

    @JvmName("setCreatingEnvironmentFunction")
    fun set(function: CreatingEnvironmentFunction) {
        creatingEnvironmentFunction = function
    }

    operator fun set(point: Point, function: LinearFunction): GurobiLinearSolverCallBack {
        map[point] = function
        return this
    }

    fun creatingEnvironment(function: CreatingEnvironmentFunction) = set(function)
    fun afterModeling(function: LinearFunction) = set(Point.AfterModeling, function)
    fun configuration(function: LinearFunction) = set(Point.Configuration, function)
    fun analyzingSolution(function: LinearFunction) = set(Point.AnalyzingSolution, function)
    fun afterFailure(function: LinearFunction) = set(Point.AfterFailure, function)

    fun contain(point: Point) = map.containsKey(point)
    fun get(point: Point): LinearFunction? = map[point]

    fun execIfContain(env: GRBEnv): Try? {
        return creatingEnvironmentFunction?.invoke(env)
    }

    fun execIfContain(point: Point, gurobi: GRBModel, variables: List<GRBVar>, constraints: List<GRBConstr>): Try? {
        return map[point]?.invoke(gurobi, variables, constraints)
    }

    override fun copy(): GurobiLinearSolverCallBack {
        return GurobiLinearSolverCallBack(nativeCallback, creatingEnvironmentFunction, map.toMutableMap())
    }
}

class GurobiQuadraticSolverCallBack(
    internal var nativeCallback: NativeCallBack? = null,
    internal var creatingEnvironmentFunction: CreatingEnvironmentFunction? = null,
    private val map: MutableMap<Point, QuadraticFunction> = EnumMap(Point::class.java)
) : Copyable<GurobiQuadraticSolverCallBack> {
    @JvmName("setNativeCallback")
    fun set(function: NativeCallBack) {
        nativeCallback = function
    }

    @JvmName("setCreatingEnvironmentFunction")
    fun set(function: CreatingEnvironmentFunction) {
        creatingEnvironmentFunction = function
    }

    operator fun set(point: Point, function: QuadraticFunction): GurobiQuadraticSolverCallBack {
        map[point] = function
        return this
    }

    fun creatingEnvironment(function: CreatingEnvironmentFunction) = set(function)
    fun afterModeling(function: QuadraticFunction) = set(Point.AfterModeling, function)
    fun configuration(function: QuadraticFunction) = set(Point.Configuration, function)
    fun analyzingSolution(function: QuadraticFunction) = set(Point.AnalyzingSolution, function)
    fun afterFailure(function: QuadraticFunction) = set(Point.AfterFailure, function)

    fun contain(point: Point) = map.containsKey(point)
    fun get(point: Point): QuadraticFunction? = map[point]

    fun execIfContain(env: GRBEnv): Try? {
        return creatingEnvironmentFunction?.invoke(env)
    }

    fun execIfContain(point: Point, gurobi: GRBModel, variables: List<GRBVar>, constraints: List<GRBQConstr>): Try? {
        return map[point]?.invoke(gurobi, variables, constraints)
    }

    override fun copy(): GurobiQuadraticSolverCallBack {
        return GurobiQuadraticSolverCallBack(nativeCallback, creatingEnvironmentFunction, map.toMutableMap())
    }
}
