/** SCIP 求解器回调支持 / SCIP solver callback support */
package fuookami.ospf.kotlin.core.solver.scip

import java.util.*
import jscip.*
import fuookami.ospf.kotlin.utils.concept.Copyable
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.syncRun
import fuookami.ospf.kotlin.core.solver.output.SolverStatus

/** SCIP 求解器回调函数类型 / SCIP solver callback function type */
typealias Function = suspend (SolverStatus?, Scip, List<Variable>, List<Constraint>) -> Try

/** SCIP 原生回调函数类型 / SCIP native callback function type */
typealias NativeCallBack = EventHandler.(Scip, EventHandlerRef, Event) -> Unit

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
 * SCIP solver callback manager
 *
 * SCIP 求解器回调管理器
 *
 * @property nativeEventMask native event mask / 原生事件掩码
 * @property nativeCallback native callback function / 原生回调函数
 * @property map callback function map / 回调函数映射
*/
class ScipSolverCallBack(
    internal var nativeEventMask: Long = EventMask.LP_EVENT or EventMask.NODE_EVENT or EventMask.SOL_EVENT,
    internal var nativeCallback: NativeCallBack? = null,
    private val map: MutableMap<Point, MutableList<Function>> = EnumMap(Point::class.java)
) : Copyable<ScipSolverCallBack> {

    /**
     * 设置原生事件掩码 / Set native event mask
     *
     * @param eventMask 事件掩码 / event mask
    */
    @JvmName("setNativeEventMask")
    fun set(eventMask: Long) {
        nativeEventMask = eventMask
    }

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
     * 设置原生回调函数和事件掩码 / Set native callback function and event mask
     *
     * @param eventMask 事件掩码 / event mask
     * @param function 原生回调函数 / native callback function
    */
    @JvmName("setNativeCallbackWithEventMask")
    fun set(eventMask: Long, function: NativeCallBack) {
        nativeEventMask = eventMask
        nativeCallback = function
    }

    /**
     * 在指定时机添加回调函数 / Add callback function at specified point
     *
     * @param point 回调时机 / callback point
     * @param function 回调函数 / callback function
     * @return 当前回调管理器实例 / current callback manager instance
    */
    fun set(point: Point, function: Function): ScipSolverCallBack {
        map.getOrPut(point) { ArrayList() }.add(function)
        return this
    }

    /**
     * 设置原生事件处理器 / Set native event handler
     *
     * @param eventMask 事件掩码 / event mask
     * @param function 原生回调函数 / native callback function
     * @return 当前回调管理器实例 / current callback manager instance
    */
    fun native(eventMask: Long = nativeEventMask, function: NativeCallBack): ScipSolverCallBack {
        set(eventMask, function)
        return this
    }

    /**
     * Set after modeling callback
     *
     * 设置建模完成后的回调
     *
     * @param function callback function / 回调函数
     * @return current callback manager instance / 当前回调管理器实例
    */
    fun afterModeling(function: Function) = set(Point.AfterModeling, function)

    /**
     * Set configuration callback
     *
     * 设置配置阶段的回调
     *
     * @param function callback function / 回调函数
     * @return current callback manager instance / 当前回调管理器实例
    */
    fun configuration(function: Function) = set(Point.Configuration, function)

    /**
     * Set analyzing solution callback
     *
     * 设置分析解阶段的回调
     *
     * @param function callback function / 回调函数
     * @return current callback manager instance / 当前回调管理器实例
    */
    fun analyzingSolution(function: Function) = set(Point.AnalyzingSolution, function)

    /**
     * Set after failure callback
     *
     * 设置求解失败后的回调
     *
     * @param function callback function / 回调函数
     * @return current callback manager instance / 当前回调管理器实例
    */
    fun afterFailure(function: Function) = set(Point.AfterFailure, function)

    /**
     * Check if callback at specified point is contained
     *
     * 检查是否包含指定时机的回调
     *
     * @param point callback point / 回调时机
     * @return whether callbacks exist at the point / 是否存在该时机的回调
    */
    fun contains(point: Point) = map.containsKey(point)

    /**
     * Get callback function list at specified point
     *
     * 获取指定时机的回调函数列表
     *
     * @param point callback point / 回调时机
     * @return callback function list or null / 回调函数列表或 null
    */
    fun get(point: Point): List<Function>? = map[point]

    /**
     * 如果包含指定时机的回调则执行 / Execute callbacks at specified point if contained
     *
     * @param point 回调时机 / callback point
     * @param status 求解状态 / solving status
     * @param scip SCIP 求解器 / SCIP solver
     * @param variables 变量列表 / variable list
     * @param constraints 约束列表 / constraint list
     * @return 操作结果 / operation result
    */
    suspend fun execIfContain(
        point: Point,
        status: SolverStatus?,
        scip: Scip,
        variables: List<Variable>,
        constraints: List<Constraint>
    ): Try? {
        return if (!map[point].isNullOrEmpty()) {
            syncRun(map[point]!!.map {
                { it(status, scip, variables, constraints) }
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
    override fun copy(): ScipSolverCallBack {
        return ScipSolverCallBack(
            nativeEventMask = nativeEventMask,
            nativeCallback = nativeCallback,
            map.toMutableMap()
        )
    }
}
