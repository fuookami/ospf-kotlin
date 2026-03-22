package fuookami.ospf.kotlin.core.backend.plugins.hexaly

import com.hexaly.optimizer.HexalyOptimizer
import com.hexaly.optimizer.HxCallbackType
import com.hexaly.optimizer.HxExpression
import fuookami.ospf.kotlin.core.backend.solver.output.SolverStatus
import fuookami.ospf.kotlin.utils.concept.Copyable
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.syncRun
import java.util.*

typealias CreatingEnvironmentFunction = (HexalyOptimizer) -> Try
typealias NativeCallBack = (HexalyOptimizer, HxCallbackType) -> Unit
typealias Function = suspend (SolverStatus?, HexalyOptimizer, List<HxExpression>, List<HxExpression>) -> Try

enum class Point {
    AfterModeling,
    Configuration,
    AnalyzingSolution,
    AfterFailure
}

class HexalySolverCallBack(
    internal var nativeCallback: NativeCallBack? = null,
    internal var creatingEnvironmentFunction: CreatingEnvironmentFunction? = null,
    private val map: MutableMap<Point, MutableList<Function>> = EnumMap(Point::class.java)
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
        map.getOrPut(point) { ArrayList() }.add(function)
        return this
    }

    fun creatingEnvironment(function: CreatingEnvironmentFunction) = set(function)
    fun afterModeling(function: Function) = set(Point.AfterModeling, function)
    fun configuration(function: Function) = set(Point.Configuration, function)
    fun analyzingSolution(function: Function) = set(Point.AnalyzingSolution, function)
    fun afterFailure(function: Function) = set(Point.AfterFailure, function)

    fun contains(point: Point) = map.containsKey(point)
    fun get(point: Point): List<Function>? = map[point]

    fun execIfContain(env: HexalyOptimizer): Try? {
        return creatingEnvironmentFunction?.invoke(env)
    }

    suspend fun execIfContain(
        point: Point,
        status: SolverStatus?,
        hexaly: HexalyOptimizer,
        variables: List<HxExpression>,
        constraints: List<HxExpression>
    ): Try? {
        return if (!map[point].isNullOrEmpty()) {
            syncRun(map[point]!!.map {
                { it(status, hexaly, variables, constraints) }
            })
        } else {
            null
        }
    }

    override fun copy(): HexalySolverCallBack {
        return HexalySolverCallBack(
            nativeCallback = nativeCallback,
            creatingEnvironmentFunction = creatingEnvironmentFunction,
            map = map.toMutableMap()
        )
    }
}
