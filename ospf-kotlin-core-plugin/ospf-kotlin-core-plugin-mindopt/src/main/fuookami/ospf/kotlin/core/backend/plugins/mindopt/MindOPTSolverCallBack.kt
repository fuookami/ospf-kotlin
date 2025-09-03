package fuookami.ospf.kotlin.core.backend.plugins.mindopt

import java.util.*
import com.alibaba.damo.mindopt.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.backend.solver.output.*

typealias CreatingEnvironmentFunction = (MDOEnv) -> Try
typealias NativeCallback = MDOCallback.() -> Unit
typealias LinearFunction = (SolverStatus?, MDOModel, List<MDOVar>, List<MDOConstr>) -> Try
typealias QuadraticFunction = (SolverStatus?, MDOModel, List<MDOVar>, List<MDOQConstr>) -> Try

enum class Point {
    AfterModeling,
    Configuration,
    AnalyzingSolution,
    AfterFailure
}

class MindOPTLinearSolverCallBack(
    internal var nativeCallback: NativeCallback? = null,
    internal var creatingEnvironmentFunction: CreatingEnvironmentFunction? = null,
    private val map: MutableMap<Point, MutableList<LinearFunction>> = HashMap()
) : Copyable<MindOPTLinearSolverCallBack> {
    @JvmName("setNativeCallback")
    fun set(function: NativeCallback) {
        nativeCallback = function
    }

    @JvmName("setCreatingEnvironmentFunction")
    fun set(function: CreatingEnvironmentFunction) {
        creatingEnvironmentFunction = function
    }

    operator fun set(point: Point, function: LinearFunction): MindOPTLinearSolverCallBack {
        map.getOrPut(point) { ArrayList() }.add(function)
        return this
    }

    fun creatingEnvironment(function: CreatingEnvironmentFunction) = set(function)
    fun afterModeling(function: LinearFunction) = set(Point.AfterModeling, function)
    fun configuration(function: LinearFunction) = set(Point.Configuration, function)
    fun analyzingSolution(function: LinearFunction) = set(Point.AnalyzingSolution, function)
    fun afterFailure(function: LinearFunction) = set(Point.AfterFailure, function)

    fun contain(point: Point) = map.containsKey(point)
    fun get(point: Point): List<LinearFunction>? = map[point]

    fun execIfContain(env: MDOEnv): Try? {
        return creatingEnvironmentFunction?.invoke(env)
    }

    fun execIfContain(point: Point, status: SolverStatus?, mindopt: MDOModel, variables: List<MDOVar>, constraints: List<MDOConstr>): Try? {
        return if (!map[point].isNullOrEmpty()) {
            run(map[point]!!.map {
                { it(status, mindopt, variables, constraints) }
            })
        } else {
            null
        }
    }

    override fun copy(): MindOPTLinearSolverCallBack {
        return MindOPTLinearSolverCallBack(nativeCallback, creatingEnvironmentFunction, map.toMutableMap())
    }
}

class MindOPTQuadraticSolverCallBack(
    internal var nativeCallback: NativeCallback? = null,
    internal var creatingEnvironmentFunction: CreatingEnvironmentFunction? = null,
    private val map: MutableMap<Point, MutableList<QuadraticFunction>> = HashMap()
) : Copyable<MindOPTQuadraticSolverCallBack> {
    @JvmName("setNativeCallback")
    fun set(function: NativeCallback) {
        nativeCallback = function
    }

    @JvmName("setCreatingEnvironmentFunction")
    fun set(function: CreatingEnvironmentFunction) {
        creatingEnvironmentFunction = function
    }

    operator fun set(point: Point, function: QuadraticFunction): MindOPTQuadraticSolverCallBack {
        map.getOrPut(point) { ArrayList() }.add(function)
        return this
    }

    fun creatingEnvironment(function: CreatingEnvironmentFunction) = set(function)
    fun afterModeling(function: QuadraticFunction) = set(Point.AfterModeling, function)
    fun configuration(function: QuadraticFunction) = set(Point.Configuration, function)
    fun analyzingSolution(function: QuadraticFunction) = set(Point.AnalyzingSolution, function)
    fun afterFailure(function: QuadraticFunction) = set(Point.AfterFailure, function)

    fun contain(point: Point) = map.containsKey(point)
    fun get(point: Point): List<QuadraticFunction>? = map[point]

    fun execIfContain(env: MDOEnv): Try? {
        return creatingEnvironmentFunction?.invoke(env)
    }

    fun execIfContain(point: Point, status: SolverStatus?, mindopt: MDOModel, variables: List<MDOVar>, constraints: List<MDOQConstr>): Try? {
        return if (!map[point].isNullOrEmpty()) {
            run(map[point]!!.map {
                { it(status, mindopt, variables, constraints) }
            })
        } else {
            null
        }
    }

    override fun copy(): MindOPTQuadraticSolverCallBack {
        return MindOPTQuadraticSolverCallBack(nativeCallback, creatingEnvironmentFunction, map.toMutableMap())
    }
}
