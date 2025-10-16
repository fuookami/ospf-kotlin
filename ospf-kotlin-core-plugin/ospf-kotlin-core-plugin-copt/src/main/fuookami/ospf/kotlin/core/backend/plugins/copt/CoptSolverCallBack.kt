package fuookami.ospf.kotlin.core.backend.plugins.copt

import java.util.*
import copt.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.backend.solver.output.*

typealias CreatingEnvironmentFunction = (EnvrConfig) -> Try
typealias NativeCallback = CallbackBase.() -> Unit
typealias LinearFunction = suspend (SolverStatus?, Model, List<Var>, List<Constraint>) -> Try
typealias QuadraticFunction = suspend (SolverStatus?, Model, List<Var>, List<QConstraint>) -> Try

enum class Point {
    AfterModeling,
    Configuration,
    AnalyzingSolution,
    AfterFailure
}

class CoptLinearSolverCallBack(
    internal var nativeCallback: NativeCallback? = null,
    internal var creatingEnvironmentFunction: CreatingEnvironmentFunction? = null,
    private val map: MutableMap<Point, MutableList<LinearFunction>> = HashMap()
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
        map.getOrPut(point) { ArrayList() }.add(function)
        return this
    }

    fun creatingEnvironment(function: CreatingEnvironmentFunction) = set(function)
    fun afterModeling(function: LinearFunction) = set(Point.AfterModeling, function)
    fun configuration(function: LinearFunction) = set(Point.Configuration, function)
    fun analyzingSolution(function: LinearFunction) = set(Point.AnalyzingSolution, function)
    fun afterFailure(function: LinearFunction) = set(Point.AfterFailure, function)

    fun contains(point: Point) = map.containsKey(point)
    fun get(point: Point): List<LinearFunction>? = map[point]

    fun execIfContain(env: EnvrConfig): Try? {
        return creatingEnvironmentFunction?.invoke(env)
    }

    suspend fun execIfContain(point: Point, status: SolverStatus?, copt: Model, variables: List<Var>, constraints: List<Constraint>): Try? {
        return if (!map[point].isNullOrEmpty()) {
            syncRun(map[point]!!.map {
                { it(status, copt, variables, constraints) }
            })
        } else {
            null
        }
    }

    override fun copy(): CoptLinearSolverCallBack {
        return CoptLinearSolverCallBack(nativeCallback, creatingEnvironmentFunction, map.toMutableMap())
    }
}

class CoptQuadraticSolverCallBack(
    internal var nativeCallback: NativeCallback? = null,
    internal var creatingEnvironmentFunction: CreatingEnvironmentFunction? = null,
    private val map: MutableMap<Point, MutableList<QuadraticFunction>> = HashMap()
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
        map.getOrPut(point) { ArrayList() }.add(function)
        return this
    }

    fun creatingEnvironment(function: CreatingEnvironmentFunction) = set(function)
    fun afterModeling(function: QuadraticFunction) = set(Point.AfterModeling, function)
    fun configuration(function: QuadraticFunction) = set(Point.Configuration, function)
    fun analyzingSolution(function: QuadraticFunction) = set(Point.AnalyzingSolution, function)
    fun afterFailure(function: QuadraticFunction) = set(Point.AfterFailure, function)

    fun contains(point: Point) = map.containsKey(point)
    fun get(point: Point): List<QuadraticFunction>? = map[point]

    fun execIfContain(env: EnvrConfig): Try? {
        return creatingEnvironmentFunction?.invoke(env)
    }

    suspend fun execIfContain(point: Point, status: SolverStatus?, copt: Model, variables: List<Var>, constraints: List<QConstraint>): Try? {
        return if (!map[point].isNullOrEmpty()) {
            syncRun(map[point]!!.map {
                { it(status, copt, variables, constraints) }
            })
        } else {
            null
        }
    }

    override fun copy(): CoptQuadraticSolverCallBack {
        return CoptQuadraticSolverCallBack(nativeCallback, creatingEnvironmentFunction, map.toMutableMap())
    }
}
