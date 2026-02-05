package fuookami.ospf.kotlin.core.backend.plugins.gurobi11

import java.util.*
import com.gurobi.gurobi.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.backend.solver.output.*

typealias CreatingEnvironmentFunction = (GRBEnv) -> Try
typealias NativeCallback = GRBCallback.() -> Unit
typealias LinearFunction = suspend (SolverStatus?, GRBModel, List<GRBVar>, List<GRBConstr>) -> Try
typealias QuadraticFunction = suspend (SolverStatus?, GRBModel, List<GRBVar>, List<GRBQConstr>) -> Try

enum class Point {
    AfterModeling,
    Configuration,
    AnalyzingSolution,
    AfterFailure
}

class GurobiLinearSolverCallBack(
    internal var nativeCallback: NativeCallback? = null,
    internal var creatingEnvironmentFunction: CreatingEnvironmentFunction? = null,
    private val map: MutableMap<Point, MutableList<LinearFunction>> = EnumMap(Point::class.java)
) : Copyable<GurobiLinearSolverCallBack> {
    @JvmName("setNativeCallback")
    fun set(function: NativeCallback) {
        nativeCallback = function
    }

    @JvmName("setCreatingEnvironmentFunction")
    fun set(function: CreatingEnvironmentFunction) {
        creatingEnvironmentFunction = function
    }

    operator fun set(point: Point, function: LinearFunction): GurobiLinearSolverCallBack {
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

    fun execIfContain(env: GRBEnv): Try? {
        return creatingEnvironmentFunction?.invoke(env)
    }

    suspend fun execIfContain(
        point: Point,
        status: SolverStatus?,
        gurobi: GRBModel,
        variables: List<GRBVar>,
        constraints: List<GRBConstr>
    ): Try? {
        return if (!map[point].isNullOrEmpty()) {
            syncRun(map[point]!!.map {
                { it(status, gurobi, variables, constraints) }
            })
        } else {
            null
        }
    }

    override fun copy(): GurobiLinearSolverCallBack {
        return GurobiLinearSolverCallBack(
            nativeCallback = nativeCallback,
            creatingEnvironmentFunction = creatingEnvironmentFunction,
            map = map.toMutableMap()
        )
    }
}

class GurobiQuadraticSolverCallBack(
    internal var nativeCallback: NativeCallback? = null,
    internal var creatingEnvironmentFunction: CreatingEnvironmentFunction? = null,
    private val map: MutableMap<Point, MutableList<QuadraticFunction>> = EnumMap(Point::class.java)
) : Copyable<GurobiQuadraticSolverCallBack> {
    @JvmName("setNativeCallback")
    fun set(function: NativeCallback) {
        nativeCallback = function
    }

    @JvmName("setCreatingEnvironmentFunction")
    fun set(function: CreatingEnvironmentFunction) {
        creatingEnvironmentFunction = function
    }

    operator fun set(point: Point, function: QuadraticFunction): GurobiQuadraticSolverCallBack {
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

    fun execIfContain(env: GRBEnv): Try? {
        return creatingEnvironmentFunction?.invoke(env)
    }

    suspend fun execIfContain(
        point: Point,
        status: SolverStatus?,
        gurobi: GRBModel,
        variables: List<GRBVar>,
        constraints: List<GRBQConstr>
    ): Try? {
        return if (!map[point].isNullOrEmpty()) {
            syncRun(map[point]!!.map {
                { it(status, gurobi, variables, constraints) }
            })
        } else {
            null
        }
    }

    override fun copy(): GurobiQuadraticSolverCallBack {
        return GurobiQuadraticSolverCallBack(
            nativeCallback = nativeCallback,
            creatingEnvironmentFunction = creatingEnvironmentFunction,
            map = map.toMutableMap()
        )
    }
}
