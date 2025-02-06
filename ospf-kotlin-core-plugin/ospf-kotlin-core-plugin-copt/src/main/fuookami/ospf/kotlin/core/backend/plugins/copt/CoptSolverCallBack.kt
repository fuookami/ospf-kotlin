package fuookami.ospf.kotlin.core.backend.plugins.copt

import java.util.*
import copt.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.functional.*

typealias CreatingEnvironmentFunction = (EnvrConfig) -> Try
typealias NativeCallback = CallbackBase.() -> Unit
typealias LinearFunction = (Model, List<Var>, List<Constraint>) -> Try
typealias QuadraticFunction = (Model, List<Var>, List<QConstraint>) -> Try

enum class Point {
    AfterModeling,
    Configuration,
    AnalyzingSolution,
    AfterFailure
}

class CoptLinearSolverCallBack(
    internal var nativeCallback: NativeCallback? = null,
    internal var creatingEnvironmentFunction: CreatingEnvironmentFunction? = null,
    private val map: MutableMap<Point, LinearFunction> = EnumMap(Point::class.java)
) : Copyable<CoptLinearSolverCallBack> {
    @JvmName("setNativeCallback")
    fun set(function: NativeCallback) {
        nativeCallback = function
    }

    @JvmName("setCreatingEnvironmentFunction")
    fun set(function: CreatingEnvironmentFunction) {
        creatingEnvironmentFunction = function
    }

    operator fun set(point: Point, function: LinearFunction): CoptLinearSolverCallBack {
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

    fun execIfContain(env: EnvrConfig): Try? {
        return creatingEnvironmentFunction?.invoke(env)
    }

    fun execIfContain(point: Point, copt: Model, variables: List<Var>, constraints: List<Constraint>): Try? {
        return map[point]?.invoke(copt, variables, constraints)
    }

    override fun copy(): CoptLinearSolverCallBack {
        return CoptLinearSolverCallBack(nativeCallback, creatingEnvironmentFunction, map.toMutableMap())
    }
}

class CoptQuadraticSolverCallBack(
    internal var nativeCallback: NativeCallback? = null,
    internal var creatingEnvironmentFunction: CreatingEnvironmentFunction? = null,
    private val map: MutableMap<Point, QuadraticFunction> = EnumMap(Point::class.java)
) : Copyable<CoptQuadraticSolverCallBack> {
    @JvmName("setNativeCallback")
    fun set(function: NativeCallback) {
        nativeCallback = function
    }

    @JvmName("setCreatingEnvironmentFunction")
    fun set(function: CreatingEnvironmentFunction) {
        creatingEnvironmentFunction = function
    }

    operator fun set(point: Point, function: QuadraticFunction): CoptQuadraticSolverCallBack {
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

    fun execIfContain(env: EnvrConfig): Try? {
        return creatingEnvironmentFunction?.invoke(env)
    }

    fun execIfContain(point: Point, copt: Model, variables: List<Var>, constraints: List<QConstraint>): Try? {
        return map[point]?.invoke(copt, variables, constraints)
    }

    override fun copy(): CoptQuadraticSolverCallBack {
        return CoptQuadraticSolverCallBack(nativeCallback, creatingEnvironmentFunction, map.toMutableMap())
    }
}
