/** Gurobi 求解器回调支持 / Gurobi solver callback support */
package fuookami.ospf.kotlin.core.solver.gurobi

import java.util.*
import gurobi.*
import fuookami.ospf.kotlin.utils.concept.Copyable
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.syncRun
import fuookami.ospf.kotlin.core.solver.output.SolverStatus

/** 创建环境函数类型 / Creating environment function type */
typealias CreatingEnvironmentFunction = (GRBEnv) -> Try

/** Gurobi 原生回调函数类型 / Gurobi native callback function type */
typealias NativeCallBack = GRBCallback.() -> Unit

/** 线性求解器回调函数类型 / Linear solver callback function type */
typealias LinearFunction = suspend (SolverStatus?, GRBModel, List<GRBVar>, List<GRBConstr>) -> Try

/** 二次求解器回调函数类型 / Quadratic solver callback function type */
typealias QuadraticFunction = suspend (SolverStatus?, GRBModel, List<GRBVar>, List<GRBQConstr>) -> Try

/** 求解器回调时机枚举 / Solver callback point enum */
enum class Point {
    /** 建模完成后 / After modeling */
    AfterModeling,
    /** 配置阶段 / Configuration phase */
    Configuration,
    /** 分析解阶段 / Analyzing solution phase */
    AnalyzingSolution,
    /** 求解失败后 / After failure */
    AfterFailure
}

/**
 * Gurobi 线性求解器回调管理器 / Gurobi linear solver callback manager
 *
 * @property nativeCallback Gurobi 原生回调函数 / Gurobi native callback function
 * @property creatingEnvironmentFunction 创建环境函数 / creating environment function
 * @property map 回调时机到回调函数列表的映射 / mapping from callback point to callback function list
*/
class GurobiLinearSolverCallBack(
    internal var nativeCallback: NativeCallBack? = null,
    internal var creatingEnvironmentFunction: CreatingEnvironmentFunction? = null,
    private val map: MutableMap<Point, MutableList<LinearFunction>> = EnumMap(Point::class.java)
) : Copyable<GurobiLinearSolverCallBack> {

    /**
     * 设置原生回调函数 / Set native callback function
     *
     * @param function 原生回调函数 / native callback function
    */
    @JvmName("setNativeCallback")
    fun set(function: NativeCallBack) {
        nativeCallback = function
    }

    /**
     * 设置创建环境函数 / Set creating environment function
     *
     * @param function 创建环境函数 / creating environment function
    */
    @JvmName("setCreatingEnvironmentFunction")
    fun set(function: CreatingEnvironmentFunction) {
        creatingEnvironmentFunction = function
    }

    /**
     * 在指定时机添加回调函数 / Add callback function at specified point
     *
     * @param point 回调时机 / callback point
     * @param function 回调函数 / callback function
     * @return 当前回调管理器实例 / current callback manager instance
    */
    operator fun set(point: Point, function: LinearFunction): GurobiLinearSolverCallBack {
        map.getOrPut(point) { ArrayList() }.add(function)
        return this
    }

    /**
     * 设置创建环境回调 / Set creating environment callback
     *
     * @param function 创建环境函数 / Creating environment function
    */
    fun creatingEnvironment(function: CreatingEnvironmentFunction) = set(function)

    /**
     * 设置建模完成后的回调 / Set after modeling callback
     *
     * @param function 回调函数 / Callback function
    */
    fun afterModeling(function: LinearFunction) = set(Point.AfterModeling, function)

    /**
     * 设置配置阶段的回调 / Set configuration callback
     *
     * @param function 回调函数 / Callback function
    */
    fun configuration(function: LinearFunction) = set(Point.Configuration, function)

    /**
     * 设置分析解阶段的回调 / Set analyzing solution callback
     *
     * @param function 回调函数 / Callback function
    */
    fun analyzingSolution(function: LinearFunction) = set(Point.AnalyzingSolution, function)

    /**
     * 设置求解失败后的回调 / Set after failure callback
     *
     * @param function 回调函数 / Callback function
    */
    fun afterFailure(function: LinearFunction) = set(Point.AfterFailure, function)

    /**
     * 检查是否包含指定时机的回调 / Check if callback at specified point is contained
     *
     * @param point 回调时机 / callback point
     * @return 是否包含 / whether contained
    */
    fun contains(point: Point) = map.containsKey(point)

    /**
     * 获取指定时机的回调函数列表 / Get callback function list at specified point
     *
     * @param point 回调时机 / callback point
     * @return 回调函数列表 / callback function list
    */
    fun get(point: Point): List<LinearFunction>? = map[point]

    /**
     * 如果包含创建环境函数则执行 / Execute creating environment function if contained
     *
     * @param env Gurobi 环境 / Gurobi environment
     * @return 操作结果 / operation result
    */
    fun execIfContain(env: GRBEnv): Try? {
        return creatingEnvironmentFunction?.invoke(env)
    }

    /**
     * 如果包含指定时机的回调则执行 / Execute callbacks at specified point if contained
     *
     * @param point 回调时机 / callback point
     * @param status 求解状态 / solving status
     * @param gurobi Gurobi 模型 / Gurobi model
     * @param variables 变量列表 / variable list
     * @param constraints 约束列表 / constraint list
     * @return 操作结果 / operation result
    */
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

    /**
     * 复制回调管理器 / Copy callback manager
     *
     * @return 回调管理器副本 / callback manager copy
    */
    override fun copy(): GurobiLinearSolverCallBack {
        return GurobiLinearSolverCallBack(
            nativeCallback = nativeCallback,
            creatingEnvironmentFunction = creatingEnvironmentFunction,
            map = map.toMutableMap()
        )
    }
}

/**
 * Gurobi 二次求解器回调管理器 / Gurobi quadratic solver callback manager
 *
 * @property nativeCallback Gurobi 原生回调函数 / Gurobi native callback function
 * @property creatingEnvironmentFunction 创建环境函数 / creating environment function
 * @property map 回调时机到回调函数列表的映射 / mapping from callback point to callback function list
*/
class GurobiQuadraticSolverCallBack(
    internal var nativeCallback: NativeCallBack? = null,
    internal var creatingEnvironmentFunction: CreatingEnvironmentFunction? = null,
    private val map: MutableMap<Point, MutableList<QuadraticFunction>> = EnumMap(Point::class.java)
) : Copyable<GurobiQuadraticSolverCallBack> {

    /**
     * 设置原生回调函数 / Set native callback function
     *
     * @param function 原生回调函数 / Native callback function
    */
    @JvmName("setNativeCallback")
    fun set(function: NativeCallBack) {
        nativeCallback = function
    }

    /**
     * 设置创建环境函数 / Set creating environment function
     *
     * @param function 创建环境函数 / Creating environment function
    */
    @JvmName("setCreatingEnvironmentFunction")
    fun set(function: CreatingEnvironmentFunction) {
        creatingEnvironmentFunction = function
    }

    /**
     * 在指定时机添加回调函数 / Add callback function at specified point
     *
     * @param point 回调时机 / callback point
     * @param function 回调函数 / callback function
     * @return 当前回调管理器实例 / current callback manager instance
    */
    operator fun set(point: Point, function: QuadraticFunction): GurobiQuadraticSolverCallBack {
        map.getOrPut(point) { ArrayList() }.add(function)
        return this
    }

    /**
     * 设置创建环境回调 / Set creating environment callback
     *
     * @param function 创建环境函数 / Creating environment function
    */
    fun creatingEnvironment(function: CreatingEnvironmentFunction) = set(function)

    /**
     * 设置建模完成后的回调 / Set after modeling callback
     *
     * @param function 回调函数 / Callback function
    */
    fun afterModeling(function: QuadraticFunction) = set(Point.AfterModeling, function)

    /**
     * 设置配置阶段的回调 / Set configuration callback
     *
     * @param function 回调函数 / Callback function
    */
    fun configuration(function: QuadraticFunction) = set(Point.Configuration, function)

    /**
     * 设置分析解阶段的回调 / Set analyzing solution callback
     *
     * @param function 回调函数 / Callback function
    */
    fun analyzingSolution(function: QuadraticFunction) = set(Point.AnalyzingSolution, function)

    /**
     * 设置求解失败后的回调 / Set after failure callback
     *
     * @param function 回调函数 / Callback function
    */
    fun afterFailure(function: QuadraticFunction) = set(Point.AfterFailure, function)

    /**
     * 检查是否包含指定时机的回调 / Check if callback at specified point is contained
     *
     * @param point 回调时机 / Callback point
     * @return 是否包含 / Whether contained
    */
    fun contains(point: Point) = map.containsKey(point)

    /**
     * 获取指定时机的回调函数列表 / Get callback function list at specified point
     *
     * @param point 回调时机 / Callback point
     * @return 回调函数列表 / Callback function list
    */
    fun get(point: Point): List<QuadraticFunction>? = map[point]

    /**
     * 如果包含创建环境函数则执行 / Execute creating environment function if contained
     *
     * @param env Gurobi 环境 / Gurobi environment
     * @return 操作结果 / operation result
    */
    fun execIfContain(env: GRBEnv): Try? {
        return creatingEnvironmentFunction?.invoke(env)
    }

    /**
     * 如果包含指定时机的回调则执行 / Execute callbacks at specified point if contained
     *
     * @param point 回调时机 / callback point
     * @param status 求解状态 / solving status
     * @param gurobi Gurobi 模型 / Gurobi model
     * @param variables 变量列表 / variable list
     * @param constraints 二次约束列表 / quadratic constraint list
     * @return 操作结果 / operation result
    */
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

    /**
     * 复制回调管理器 / Copy callback manager
     *
     * @return 回调管理器副本 / callback manager copy
    */
    override fun copy(): GurobiQuadraticSolverCallBack {
        return GurobiQuadraticSolverCallBack(
            nativeCallback = nativeCallback,
            creatingEnvironmentFunction = creatingEnvironmentFunction,
            map = map.toMutableMap()
        )
    }
}