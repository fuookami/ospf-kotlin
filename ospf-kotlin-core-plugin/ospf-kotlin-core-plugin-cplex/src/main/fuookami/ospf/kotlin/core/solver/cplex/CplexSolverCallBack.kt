/** CPLEX 求解器回调支持 / CPLEX solver callback support */
package fuookami.ospf.kotlin.core.solver.cplex

import java.util.*
import ilog.concert.IloNumVar
import ilog.concert.IloRange
import ilog.cplex.IloCplex
import fuookami.ospf.kotlin.utils.concept.Copyable
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.syncRun
import fuookami.ospf.kotlin.core.solver.output.SolverStatus

/** CPLEX 原生回调函数类型 / CPLEX native callback function type */
typealias NativeCallback = IloCplex.Callback.() -> Unit
/** CPLEX 求解器回调函数类型 / CPLEX solver callback function type */
typealias Function = suspend (SolverStatus?, IloCplex, List<IloNumVar>, List<IloRange>) -> Try

/** 求解器回调时机枚举 / Solver callback point enum */
enum class Point {
    /** 建模完成后 / After modeling */
    AfterModeling,
    /** 配置阶段 / Configuration phase */
    Configuration,
    /** 求解阶段 / Solving phase */
    Solving,
    /** 分析解阶段 / Analyzing solution phase */
    AnalyzingSolution,
    /** 求解失败后 / After failure */
    AfterFailure
}

/**
 * CPLEX 求解器回调管理器 / CPLEX solver callback manager
 *
 * @property nativeCallback CPLEX 原生回调函数 / CPLEX native callback function
 * @property map 回调时机到回调函数列表的映射 / mapping from callback point to callback function list
 */
class CplexSolverCallBack(
    internal var nativeCallback: NativeCallback? = null,
    private val map: MutableMap<Point, MutableList<Function>> = EnumMap(Point::class.java)
) : Copyable<CplexSolverCallBack> {
    /**
     * 设置原生回调函数 / Set native callback function
     *
     * @param function 原生回调函数 / native callback function
     */
    fun set(function: NativeCallback) {
        nativeCallback = function
    }

    /**
     * 在指定时机添加回调函数 / Add callback function at specified point
     *
     * @param point 回调时机 / callback point
     * @param function 回调函数 / callback function
     * @return 当前回调管理器实例 / current callback manager instance
     */
    fun set(point: Point, function: Function): CplexSolverCallBack {
        map.getOrPut(point) { ArrayList() }.add(function)
        return this
    }

    /**
     * 设置建模完成后的回调 / Set after modeling callback
     *
     * @param function 回调函数 / callback function
     * @return 当前回调管理器实例 / current callback manager instance
     */
    fun afterModeling(function: Function) = set(Point.AfterModeling, function)
    /**
     * 设置配置阶段的回调 / Set configuration callback
     *
     * @param function 回调函数 / callback function
     * @return 当前回调管理器实例 / current callback manager instance
     */
    fun configuration(function: Function) = set(Point.Configuration, function)
    /**
     * 设置求解阶段的回调 / Set solving callback
     *
     * @param function 回调函数 / callback function
     * @return 当前回调管理器实例 / current callback manager instance
     */
    fun solving(function: Function) = set(Point.Solving, function)
    /**
     * 设置分析解阶段的回调 / Set analyzing solution callback
     *
     * @param function 回调函数 / callback function
     * @return 当前回调管理器实例 / current callback manager instance
     */
    fun analyzingSolution(function: Function) = set(Point.AnalyzingSolution, function)
    /**
     * 设置求解失败后的回调 / Set after failure callback
     *
     * @param function 回调函数 / callback function
     * @return 当前回调管理器实例 / current callback manager instance
     */
    fun afterFailure(function: Function) = set(Point.AfterFailure, function)

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
    fun get(point: Point): List<Function>? = map[point]

    /**
     * 如果包含指定时机的回调则执行 / Execute callbacks at specified point if contained
     *
     * @param point 回调时机 / callback point
     * @param status 求解状态 / solving status
     * @param cplex CPLEX 求解器 / CPLEX solver
     * @param variables 变量列表 / variable list
     * @param constraints 约束列表 / constraint list
     * @return 操作结果 / operation result
     */
    suspend fun execIfContain(
        point: Point,
        status: SolverStatus?,
        cplex: IloCplex,
        variables: List<IloNumVar>,
        constraints: List<IloRange>
    ): Try? {
        return if (!map[point].isNullOrEmpty()) {
            syncRun(map[point]!!.map {
                { it(status, cplex, variables, constraints) }
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
    override fun copy(): CplexSolverCallBack {
        return CplexSolverCallBack(
            nativeCallback = nativeCallback,
            map = map.toMutableMap()
        )
    }
}