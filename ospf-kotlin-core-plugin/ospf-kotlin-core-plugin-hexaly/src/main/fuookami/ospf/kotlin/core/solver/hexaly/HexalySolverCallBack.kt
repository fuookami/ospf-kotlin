/**
 * Hexaly solver callback support
 * Hexaly 求解器回调支持
*/
package fuookami.ospf.kotlin.core.solver.hexaly

import java.util.*
import com.hexaly.optimizer.*
import fuookami.ospf.kotlin.utils.concept.Copyable
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.syncRun
import fuookami.ospf.kotlin.core.solver.output.SolverStatus

/**
 * Creating environment function
 * 创建环境函数
*/
fun interface CreatingEnvironmentFunction {
    operator fun invoke(optimizer: HexalyOptimizer): Try
}

/**
 * Hexaly native callback function
 * Hexaly 原生回调函数
*/
fun interface NativeCallBack {
    operator fun invoke(optimizer: HexalyOptimizer, callbackType: HxCallbackType)
}

/**
 * Hexaly solver callback function type
 * Hexaly 求解器回调函数类型
*/
typealias Function = suspend (SolverStatus?, HexalyOptimizer, List<HxExpression>, List<HxExpression>) -> Try

/**
 * Solver callback point enum
 * 求解器回调时机枚举
*/
enum class Point {
    /** After modeling / 中文 建模完成后 */
    AfterModeling,
    /** Configuration phase / 中文 配置阶段 */
    Configuration,
    /** Analyzing solution phase / 中文 分析解阶段 */
    AnalyzingSolution,
    /** After failure / 中文 求解失败后 */
    AfterFailure
}

/**
 * Hexaly solver callback manager
 * Hexaly 求解器回调管理器
 *
 * @property nativeCallback 中文 原生回调函数 / native callback function
 * @property creatingEnvironmentFunction 中文 创建环境函数 / creating environment function
 * @property map 中文 回调时机到回调函数列表的映射 / mapping from callback point to callback function list
*/
class HexalySolverCallBack(
    internal var nativeCallback: NativeCallBack? = null,
    internal var creatingEnvironmentFunction: CreatingEnvironmentFunction? = null,
    private val map: MutableMap<Point, MutableList<Function>> = EnumMap(Point::class.java)
) : Copyable<HexalySolverCallBack> {

    /**
     * Set native callback function
     * 设置原生回调函数
     *
     * @param function 中文 原生回调函数 / native callback function
    */
    @JvmName("setNativeCallback")
    fun set(function: NativeCallBack) {
        nativeCallback = function
    }

    /**
     * Set creating environment function
     * 设置创建环境函数
     *
     * @param function 中文 创建环境函数 / creating environment function
    */
    @JvmName("setCreatingEnvironmentFunction")
    fun set(function: CreatingEnvironmentFunction) {
        creatingEnvironmentFunction = function
    }

    /**
     * Add callback function at specified point
     * 在指定时机添加回调函数
     *
     * @param point 中文 回调时机 / callback point
     * @param function 中文 回调函数 / callback function
     * @return 中文 当前回调管理器实例 / current callback manager instance
    */
    operator fun set(point: Point, function: Function): HexalySolverCallBack {
        map.getOrPut(point) { ArrayList() }.add(function)
        return this
    }

    /**
     * Set creating environment callback
     * 设置创建环境回调
     *
     * @param function 中文 创建环境函数 / creating environment function
    */
    fun creatingEnvironment(function: CreatingEnvironmentFunction) = set(function)

    /**
     * Set after modeling callback
     * 设置建模完成后的回调
     *
     * @param function 中文 回调函数 / callback function
    */
    fun afterModeling(function: Function) = set(Point.AfterModeling, function)

    /**
     * Set configuration callback
     * 设置配置阶段的回调
     *
     * @param function 中文 回调函数 / callback function
    */
    fun configuration(function: Function) = set(Point.Configuration, function)

    /**
     * Set analyzing solution callback
     * 设置分析解阶段的回调
     *
     * @param function 中文 回调函数 / callback function
    */
    fun analyzingSolution(function: Function) = set(Point.AnalyzingSolution, function)

    /**
     * Set after failure callback
     * 设置求解失败后的回调
     *
     * @param function 中文 回调函数 / callback function
    */
    fun afterFailure(function: Function) = set(Point.AfterFailure, function)

    /**
     * Check if callback at specified point is contained
     * 检查是否包含指定时机的回调
     *
     * @param point 中文 回调时机 / callback point
     * @return 中文 是否包含 / whether contained
    */
    fun contains(point: Point) = map.containsKey(point)

    /**
     * Get callback function list at specified point
     * 获取指定时机的回调函数列表
     *
     * @param point 中文 回调时机 / callback point
     * @return 中文 回调函数列表 / callback function list
    */
    fun get(point: Point): List<Function>? = map[point]

    /**
     * Execute creating environment function if contained
     * 如果包含创建环境函数则执行
     *
     * @param env 中文 Hexaly 优化器 / Hexaly optimizer
     * @return 中文 操作结果 / operation result
    */
    fun execIfContain(env: HexalyOptimizer): Try? {
        return creatingEnvironmentFunction?.invoke(env)
    }

    /**
     * Execute callbacks at specified point if contained
     * 如果包含指定时机的回调则执行
     *
     * @param point 中文 回调时机 / callback point
     * @param status 中文 求解状态 / solving status
     * @param hexaly 中文 Hexaly 优化器 / Hexaly optimizer
     * @param variables 中文 变量表达式列表 / variable expression list
     * @param constraints 中文 约束表达式列表 / constraint expression list
     * @return 中文 操作结果 / operation result
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
     * Copy callback manager
     * 复制回调管理器
     *
     * @return 中文 回调管理器副本 / callback manager copy
    */
    override fun copy(): HexalySolverCallBack {
        return HexalySolverCallBack(
            nativeCallback = nativeCallback,
            creatingEnvironmentFunction = creatingEnvironmentFunction,
            map = map.toMutableMap()
        )
    }
}
