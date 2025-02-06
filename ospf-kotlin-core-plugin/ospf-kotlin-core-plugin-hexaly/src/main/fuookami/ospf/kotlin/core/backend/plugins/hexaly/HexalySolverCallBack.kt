package fuookami.ospf.kotlin.core.backend.plugins.hexaly

import java.util.*
import com.hexaly.optimizer.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.functional.*

typealias CreatingEnvironmentFunction = (HexalyOptimizer) -> Try
typealias NativeCallBack = (HexalyOptimizer, HxCallbackType) -> Unit
typealias Function = (HexalyOptimizer, List<HxExpression>, List<HxExpression>) -> Try

enum class Point {
    AfterModeling,
    Configuration,
    AnalyzingSolution,
    AfterFailure
}

class HexalySolverCallBack(
    internal var nativeCallback: NativeCallBack? = null,
    internal var creatingEnvironmentFunction: CreatingEnvironmentFunction? = null,
    private val map: MutableMap<Point, Function> = EnumMap(Point::class.java)
) : Copyable<HexalySolverCallBack> {
    @JvmName("setNativeCallback")
    fun set(function: NativeCallBack) {
        nativeCallback = function
    }

    @JvmName("setCreatingEnvironmentFunction")
    fun set(function: CreatingEnvironmentFunction) {
        creatingEnvironmentFunction = function
    }

    operator fun set(point: Point, function: Function): HexalySolverCallBack {
        map[point] = function
        return this
    }

    fun creatingEnvironment(function: CreatingEnvironmentFunction) = set(function)
    fun afterModeling(function: Function) = set(Point.AfterModeling, function)
    fun configuration(function: Function) = set(Point.Configuration, function)
    fun analyzingSolution(function: Function) = set(Point.AnalyzingSolution, function)
    fun afterFailure(function: Function) = set(Point.AfterFailure, function)

    fun contain(point: Point) = map.containsKey(point)
    fun get(point: Point): Function? = map[point]

    fun execIfContain(env: HexalyOptimizer): Try? {
        return creatingEnvironmentFunction?.invoke(env)
    }

    fun execIfContain(point: Point, hexaly: HexalyOptimizer, variables: List<HxExpression>, constraints: List<HxExpression>): Try? {
        return map[point]?.invoke(hexaly, variables, constraints)
    }

    override fun copy(): HexalySolverCallBack {
        return HexalySolverCallBack(nativeCallback, creatingEnvironmentFunction, map.toMutableMap())
    }
}
