package fuookami.ospf.kotlin.core.backend.plugins.mindopt

import java.util.*
import com.alibaba.damo.mindopt.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.functional.*

typealias CreatingEnvironmentFunction = (MDOEnv) -> Try
typealias NativeCallback = MDOCallback.() -> Unit
typealias LinearFunction = (MDOModel, List<MDOVar>, List<MDOConstr>) -> Try
typealias QuadraticFunction = (MDOModel, List<MDOVar>, List<MDOQConstr>) -> Try

enum class Point {
    AfterModeling,
    Configuration,
    AnalyzingSolution,
    AfterFailure
}

class MindOPTLinearSolverCallBack(
    internal var nativeCallback: NativeCallback? = null,
    internal var creatingEnvironmentFunction: CreatingEnvironmentFunction? = null,
    private val map: MutableMap<Point, LinearFunction> = EnumMap(Point::class.java)
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

    fun execIfContain(env: MDOEnv): Try? {
        return creatingEnvironmentFunction?.invoke(env)
    }

    fun execIfContain(point: Point, mindopt: MDOModel, variables: List<MDOVar>, constraints: List<MDOConstr>): Try? {
        return map[point]?.invoke(mindopt, variables, constraints)
    }

    override fun copy(): MindOPTLinearSolverCallBack {
        return MindOPTLinearSolverCallBack(nativeCallback, creatingEnvironmentFunction, map.toMutableMap())
    }
}

class MindOPTQuadraticSolverCallBack(
    internal var nativeCallback: NativeCallback? = null,
    internal var creatingEnvironmentFunction: CreatingEnvironmentFunction? = null,
    private val map: MutableMap<Point, QuadraticFunction> = EnumMap(Point::class.java)
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

    fun execIfContain(env: MDOEnv): Try? {
        return creatingEnvironmentFunction?.invoke(env)
    }

    fun execIfContain(point: Point, mindopt: MDOModel, variables: List<MDOVar>, constraints: List<MDOQConstr>): Try? {
        return map[point]?.invoke(mindopt, variables, constraints)
    }

    override fun copy(): MindOPTQuadraticSolverCallBack {
        return MindOPTQuadraticSolverCallBack(nativeCallback, creatingEnvironmentFunction, map.toMutableMap())
    }
}
