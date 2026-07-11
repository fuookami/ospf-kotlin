/** COPT 求解器回调支持 / COPT solver callback support */
package fuookami.ospf.kotlin.core.solver.copt

import java.util.*
import copt.*
import fuookami.ospf.kotlin.utils.concept.Copyable
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.syncRun
import fuookami.ospf.kotlin.core.solver.output.SolverStatus

/** 创建环境函数类型 / Creating environment function type */
typealias CreatingEnvironmentFunction = (EnvrConfig) -> Try

/** COPT 原生回调函数类型 / COPT native callback function type */
typealias NativeCallback = CallbackBase.() -> Unit

/** 线性求解器回调函数类型 / Linear solver callback function type */
typealias LinearFunction = suspend (SolverStatus?, Model, List<Var>, List<Constraint>) -> Try

/** 二次求解器回调函数类型 / Quadratic solver callback function type */
typealias QuadraticFunction = suspend (SolverStatus?, Model, List<Var>, List<QConstraint>) -> Try

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
 * COPT 线性求解器回调管理器 / COPT linear solver callback manager
 *
 * @property nativeCallback COPT 原生回调函数 / COPT native callback function
 * @property creatingEnvironmentFunction 创建环境函数 / creating environment function
 * @property map 回调函数映射 / callback function mapping
*/
class CoptLinearSolverCallBack(
    internal var nativeCallback: NativeCallback? = null,
    internal var creatingEnvironmentFunction: CreatingEnvironmentFunction? = null,
    private val map: MutableMap<Point, MutableList<LinearFunction>> = EnumMap(Point::class.java)
) : Copyable<CoptLinearSolverCallBack> {

    /**
     * 设置原生回调函数 / Set native callback function
     *
     * @param function 原生回调函数 / native callback function
    */
    @JvmName("setNativeCallback")
    fun set(function: NativeCallback) {
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
    operator fun set(point: Point, function: LinearFunction): CoptLinearSolverCallBack {
        map.getOrPut(point) { ArrayList() }.add(function)
        return this
    }

    /**
     * 设置创建环境回调 / Set creating environment callback
     *
     * @param function 创建环境函数 / creating environment function
     * @return 当前回调管理器实例 / current callback manager instance
    */
    fun creatingEnvironment(function: CreatingEnvironmentFunction) = set(function)

    /**
     * 设置建模完成后的回调 / Set after modeling callback
     *
     * @param function 回调函数 / callback function
     * @return 当前回调管理器实例 / current callback manager instance
    */
    fun afterModeling(function: LinearFunction) = set(Point.AfterModeling, function)

    /**
     * 设置配置阶段的回调 / Set configuration callback
     *
     * @param function 回调函数 / callback function
     * @return 当前回调管理器实例 / current callback manager instance
    */
    fun configuration(function: LinearFunction) = set(Point.Configuration, function)

    /**
     * 设置分析解阶段的回调 / Set analyzing solution callback
     *
     * @param function 回调函数 / callback function
     * @return 当前回调管理器实例 / current callback manager instance
    */
    fun analyzingSolution(function: LinearFunction) = set(Point.AnalyzingSolution, function)

    /**
     * 设置求解失败后的回调 / Set after failure callback
     *
     * @param function 回调函数 / callback function
     * @return 当前回调管理器实例 / current callback manager instance
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
     * @param env 环境配置 / environment configuration
     * @return 操作结果 / operation result
    */
    fun execIfContain(env: EnvrConfig): Try? {
        return creatingEnvironmentFunction?.invoke(env)
    }

    /**
     * 如果包含指定时机的回调则执行 / Execute callbacks at specified point if contained
     *
     * @param point 回调时机 / callback point
     * @param status 求解状态 / solving status
     * @param copt COPT 模型 / COPT model
     * @param variables 变量列表 / variable list
     * @param constraints 约束列表 / constraint list
     * @return 操作结果 / operation result
    */
    suspend fun execIfContain(
        point: Point,
        status: SolverStatus?,
        copt: Model,
        variables: List<Var>,
        constraints: List<Constraint>
    ): Try? {
        return if (!map[point].isNullOrEmpty()) {
            syncRun(map[point]!!.map {
                { it(status, copt, variables, constraints) }
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
    override fun copy(): CoptLinearSolverCallBack {
        return CoptLinearSolverCallBack(
            nativeCallback = nativeCallback,
            creatingEnvironmentFunction = creatingEnvironmentFunction,
            map = map.toMutableMap()
        )
    }
}

/**
 * COPT 二次求解器回调管理器 / COPT quadratic solver callback manager
 *
 * @property nativeCallback COPT 原生回调函数 / COPT native callback function
 * @property creatingEnvironmentFunction 创建环境函数 / creating environment function
 * @property map 回调函数映射 / callback function mapping
*/
class CoptQuadraticSolverCallBack(
    internal var nativeCallback: NativeCallback? = null,
    internal var creatingEnvironmentFunction: CreatingEnvironmentFunction? = null,
    private val map: MutableMap<Point, MutableList<QuadraticFunction>> = EnumMap(Point::class.java)
) : Copyable<CoptQuadraticSolverCallBack> {

    /**
     * 设置原生回调函数 / Set native callback function
     *
     * @param function 原生回调函数 / native callback function
    */
    @JvmName("setNativeCallback")
    fun set(function: NativeCallback) {
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
    operator fun set(point: Point, function: QuadraticFunction): CoptQuadraticSolverCallBack {
        map.getOrPut(point) { ArrayList() }.add(function)
        return this
    }

    /**
     * 设置创建环境回调 / Set creating environment callback
     *
     * @param function 创建环境函数 / creating environment function
     * @return 当前回调管理器实例 / current callback manager instance
    */
    fun creatingEnvironment(function: CreatingEnvironmentFunction) = set(function)

    /**
     * 设置建模完成后的回调 / Set after modeling callback
     *
     * @param function 回调函数 / callback function
     * @return 当前回调管理器实例 / current callback manager instance
    */
    fun afterModeling(function: QuadraticFunction) = set(Point.AfterModeling, function)

    /**
     * 设置配置阶段的回调 / Set configuration callback
     *
     * @param function 回调函数 / callback function
     * @return 当前回调管理器实例 / current callback manager instance
    */
    fun configuration(function: QuadraticFunction) = set(Point.Configuration, function)

    /**
     * 设置分析解阶段的回调 / Set analyzing solution callback
     *
     * @param function 回调函数 / callback function
     * @return 当前回调管理器实例 / current callback manager instance
    */
    fun analyzingSolution(function: QuadraticFunction) = set(Point.AnalyzingSolution, function)

    /**
     * 设置求解失败后的回调 / Set after failure callback
     *
     * @param function 回调函数 / callback function
     * @return 当前回调管理器实例 / current callback manager instance
    */
    fun afterFailure(function: QuadraticFunction) = set(Point.AfterFailure, function)

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
    fun get(point: Point): List<QuadraticFunction>? = map[point]

    /**
     * 如果包含创建环境函数则执行 / Execute creating environment function if contained
     *
     * @param env 环境配置 / environment configuration
     * @return 操作结果 / operation result
    */
    fun execIfContain(env: EnvrConfig): Try? {
        return creatingEnvironmentFunction?.invoke(env)
    }

    /**
     * 如果包含指定时机的回调则执行 / Execute callbacks at specified point if contained
     *
     * @param point 回调时机 / callback point
     * @param status 求解状态 / solving status
     * @param copt COPT 模型 / COPT model
     * @param variables 变量列表 / variable list
     * @param constraints 二次约束列表 / quadratic constraint list
     * @return 操作结果 / operation result
    */
    suspend fun execIfContain(
        point: Point,
        status: SolverStatus?,
        copt: Model,
        variables: List<Var>,
        constraints: List<QConstraint>
    ): Try? {
        return if (!map[point].isNullOrEmpty()) {
            syncRun(map[point]!!.map {
                { it(status, copt, variables, constraints) }
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
    override fun copy(): CoptQuadraticSolverCallBack {
        return CoptQuadraticSolverCallBack(
            nativeCallback = nativeCallback,
            creatingEnvironmentFunction = creatingEnvironmentFunction,
            map = map.toMutableMap()
        )
    }
}