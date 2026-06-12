/** Hexaly 求解器回调支持 / Hexaly solver callback support */
package fuookami.ospf.kotlin.core.solver.hexaly

import java.util.*
import com.hexaly.optimizer.*
import fuookami.ospf.kotlin.utils.concept.Copyable
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.syncRun
import fuookami.ospf.kotlin.core.solver.output.SolverStatus

/** 创建环境函数类型 / Creating environment function type */
typealias CreatingEnvironmentFunction = (HexalyOptimizer) -> Try
/** Hexaly 原生回调函数类型 / Hexaly native callback function type */
typealias NativeCallBack = (HexalyOptimizer, HxCallbackType) -> Unit
/** Hexaly 求解器回调函数类型 / Hexaly solver callback function type */
typealias Function = suspend (SolverStatus?, HexalyOptimizer, List<HxExpression>, List<HxExpression>) -> Try

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

/** Hexaly 求解器回调管理器 / Hexaly solver callback manager */
class HexalySolverCallBack(
    internal var nativeCallback: NativeCallBack? = null,
    internal var creatingEnvironmentFunction: CreatingEnvironmentFunction? = null,
    private val map: MutableMap<Point, MutableList<Function>> = EnumMap(Point::class.java)
) : Copyable<HexalySolverCallBack> {
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
    operator fun set(point: Point, function: Function): HexalySolverCallBack {
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
    fun afterModeling(function: Function) = set(Point.AfterModeling, function)
    /**
     * 设置配置阶段的回调 / Set configuration callback
     *
     * @param function 回调函数 / Callback function
     */
    fun configuration(function: Function) = set(Point.Configuration, function)
    /**
     * 设置分析解阶段的回调 / Set analyzing solution callback
     *
     * @param function 回调函数 / Callback function
     */
    fun analyzingSolution(function: Function) = set(Point.AnalyzingSolution, function)
    /**
     * 设置求解失败后的回调 / Set after failure callback
     *
     * @param function 回调函数 / Callback function
     */
    fun afterFailure(function: Function) = set(Point.AfterFailure, function)

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
    fun get(point: Point): List<Function>? = map[point]

    /**
     * 如果包含创建环境函数则执行 / Execute creating environment function if contained
     *
     * @param env Hexaly 优化器 / Hexaly optimizer
     * @return 操作结果 / operation result
     */
    fun execIfContain(env: HexalyOptimizer): Try? {
        return creatingEnvironmentFunction?.invoke(env)
    }

    /**
     * 如果包含指定时机的回调则执行 / Execute callbacks at specified point if contained
     *
     * @param point 回调时机 / callback point
     * @param status 求解状态 / solving status
     * @param hexaly Hexaly 优化器 / Hexaly optimizer
     * @param variables 变量表达式列表 / variable expression list
     * @param constraints 约束表达式列表 / constraint expression list
     * @return 操作结果 / operation result
     */
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

    /**
     * 复制回调管理器 / Copy callback manager
     *
     * @return 回调管理器副本 / callback manager copy
     */
    override fun copy(): HexalySolverCallBack {
        return HexalySolverCallBack(
            nativeCallback = nativeCallback,
            creatingEnvironmentFunction = creatingEnvironmentFunction,
            map = map.toMutableMap()
        )
    }
}