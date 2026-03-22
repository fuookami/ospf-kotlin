package fuookami.ospf.kotlin.core.backend.plugins.mosek

import fuookami.ospf.kotlin.core.backend.solver.output.SolverStatus
import fuookami.ospf.kotlin.utils.concept.Copyable
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.syncRun
import mosek.Callback
import mosek.Task
import java.util.*

typealias CreatingEnvironmentFunction = (Task) -> Try
typealias NativeCallBack = (Task, Callback) -> Unit
typealias Function = suspend (SolverStatus?, Task) -> Try

enum class Point {
    AfterModeling,
    Configuration,
    AnalyzingSolution,
    AfterFailure
}

class MosekSolverCallBack(
    internal var nativeCallback: NativeCallBack? = null,
    internal var creatingEnvironmentFunction: CreatingEnvironmentFunction? = null,
    private val map: MutableMap<Point, MutableList<Function>> = EnumMap(Point::class.java)
) : Copyable<MosekSolverCallBack> {
    @JvmName("setNativeCallback")
    fun set(function: NativeCallBack) {
        nativeCallback = function
    }

    @JvmName("setCreatingEnvironmentFunction")
    fun set(function: CreatingEnvironmentFunction) {
        creatingEnvironmentFunction = function
    }

    operator fun set(point: Point, function: Function): MosekSolverCallBack {
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

    fun execIfContain(env: Task): Try? {
        return creatingEnvironmentFunction?.invoke(env)
    }

    suspend fun execIfContain(
        point: Point,
        status: SolverStatus?,
        mosekModel: Task
    ): Try? {
        return if (!map[point].isNullOrEmpty()) {
            syncRun(map[point]!!.map {
                { it(status, mosekModel) }
            })
        } else {
            null
        }
    }

    override fun copy(): MosekSolverCallBack {
        return MosekSolverCallBack(
            nativeCallback = nativeCallback,
            creatingEnvironmentFunction = creatingEnvironmentFunction,
            map = map.toMutableMap()
        )
    }
}
