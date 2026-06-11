/**
 * Token 数据结构，持有变量求解结果的双视图访问。
 * Token data structure holding dual-view access to variable solve results.
 *
 * 内部存储为 Flt64?（求解器后端始终产出 Flt64），公开 API 通过 IntoValue<V> 转换器
 * 提供类型安全的 V? 视图，同时保留 resultFlt64 供求解器内部使用。
 * Internally stores Flt64? (solver backends always produce Flt64); the public API
 * provides a type-safe V? view via IntoValue<V> converter while retaining resultFlt64
 * for solver-internal use.
 */
package fuookami.ospf.kotlin.core.token

import kotlin.random.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.core.solver.value.*
import fuookami.ospf.kotlin.core.variable.*

/**
 * 泛型 Token<V>，提供双视图访问变量求解结果。
 * Generic Token<V> with dual-view access to variable solve results.
 *
 * 内部存储为 Flt64?（求解器后端始终产出 Flt64），公开 API 通过 IntoValue<V> 转换器
 * 提供类型安全的 V? 视图，同时保留 resultFlt64 供求解器内部使用。
 * Internally stores Flt64? (solver backends always produce Flt64); the public API
 * provides a type-safe V? view via IntoValue<V> converter while retaining resultFlt64
 * for solver-internal use.
 *
 * 双视图模式 / Dual-view pattern:
 *   - Flt64 视图：`resultFlt64`（求解器边界，内部）/ Flt64 view: `resultFlt64` (solver-boundary, internal)
 *   - V 类型视图：`result`（类型安全，公开 API）/ V-type view: `result` (type-safe, public API)
 *
 * @param V 数值类型 / The number type
 * @property variable 关联的变量项 / Associated variable item
 * @property solverIndex 求解器中的索引 / Index in solver
 * @property converter 值转换器（Flt64 -> V）/ Value converter (Flt64 -> V)
 */
data class Token<V : RealNumber<V>>(
    val variable: AbstractVariableItem<*, *>,
    val solverIndex: Int,
    internal val refreshCallbacks: MutableMap<AbstractTokenList<V>, (Boolean) -> Unit>,
    private val converter: IntoValue<V>? = null
) {
    /** 变量唯一键 / Variable unique key */
    val key by variable::key
    internal var __result: Flt64? = null
    internal var _result: Flt64?
        get() = __result
        set(value) {
            __result = value
            refreshCallbacks.values.forEach { it(value != null) }
        }

    /** 结果的 Flt64 视图（求解器边界，内部使用）/ Flt64 view of result (solver-boundary, internal) */
    val resultFlt64: Flt64? by ::_result
    /** 结果的 Double 视图 / Double view of result */
    val doubleResult get() = _result?.toSolverDouble("token.result")

    /**
     * 结果的 V 类型视图（主要公开 API）
     * V-type view of result (primary public API)
     *
     * 设置 converter 时通过 IntoValue<V> 将 Flt64 转换为 V；
     * converter 为 null（V = Flt64）时通过 unchecked cast 返回 _result。
     * When converter is set, converts Flt64 -> V via IntoValue<V>.
     * When converter is null (V = Flt64), returns _result via unchecked cast.
     */
    @Suppress("UNCHECKED_CAST")
    val result: V?
        get() = if (converter != null) {
            _result?.let { converter.intoValue(it) }
        } else {
            _result as V?
        }

    /**
     * 设置泛型结果值
     * Set result from the generic value
     *
     * @param value 要设置的值 / Value to set
     */
    fun setResult(value: V) {
        _result = value.toFlt64()
    }

    /**
     * 通过给定转换器显式获取类型化结果
     * Explicit generic result conversion through the supplied converter
     *
     * @param converter 值转换器 / Value converter
     * @return 类型化的结果值 / Generic result value
     */
    fun result(converter: IntoValue<V>): V? = _result?.let { converter.intoValue(it) }

    /** 变量名称 / Variable name */
    val name by variable::name
    /** 变量类型 / Variable type */
    val type by variable::type

    /** 值范围的 Flt64 视图（求解器边界）/ Flt64 view of range (solver-boundary) */
    val range: ValueRange<Flt64>?
        get() = if (lowerBound != null && upperBound != null) {
            ValueRange(
                lowerBound = lowerBound!!,
                upperBound = upperBound!!,
                constants = Flt64
            )
        } else {
            null
        }

    /**
     * 检查 V 类型值是否在此 token 的边界范围内
     * Check if a V-type value is within this token's bounds
     *
     * 内部将 V 转换为 Flt64 进行范围检查。未设置边界时返回 true。
     * Converts V to Flt64 internally for range checking. Returns true if no bounds are set.
     *
     * @param value 待检查的值 / Value to check
     * @param converter 值转换器 / Value converter
     * @return 是否在范围内 / Whether within bounds
     */
    fun containsInBounds(value: V, converter: IntoValue<V>): Boolean {
        val r = range ?: return true
        return r.contains(converter.fromValue(value))
    }

    /** 下界的 Flt64 视图（求解器边界）/ Flt64 view of lower bound (solver-boundary) */
    val lowerBound by variable::lowerBound
    /** 上界的 Flt64 视图（求解器边界）/ Flt64 view of upper bound (solver-boundary) */
    val upperBound by variable::upperBound

    /**
     * 获取下界的类型化视图
     * Get generic view of lower bound
     *
     * @param converter 值转换器 / Value converter
     * @return 类型化的下界值 / Typed lower bound value
     */
    fun lowerBound(converter: IntoValue<V>): V? = lowerBound?.value?.toFlt64()?.let { converter.intoValue(it) }

    /**
     * 获取上界的类型化视图
     * Get generic view of upper bound
     *
     * @param converter 值转换器 / Value converter
     * @return 类型化的上界值 / Typed upper bound value
     */
    fun upperBound(converter: IntoValue<V>): V? = upperBound?.value?.toFlt64()?.let { converter.intoValue(it) }

    /**
     * 判断是否属于同一变量组
     * Check if this token belongs to the same group as a variable
     *
     * @param item 变量项 / Variable item
     * @return 是否属于同一组 / Whether in the same group
     */
    infix fun belongsTo(item: AbstractVariableItem<*, *>): Boolean {
        return variable.belongsTo(item)
    }

    /**
     * 判断是否属于指定组合
     * Check if this token belongs to the specified combination
     *
     * @param combination 变量组合 / Variable combination
     * @return 是否属于该组合 / Whether belongs to the combination
     */
    infix fun belongsTo(combination: VariableCombination<*, *, *>): Boolean {
        return variable.belongsTo(combination)
    }

    /**
     * 在边界范围内生成随机值
     * Generate a random value within bounds
     *
     * @param rng 随机数生成器 / Random number generator
     * @return 随机 Flt64 值 / Random Flt64 value
     */
    fun random(rng: Random): Flt64 {
        return if (variable.type.isUnsignedIntegerType) {
            val lower = lowerBound!!.value.unwrap().round().toUInt64().toULong()
            val upper = upperBound!!.value.unwrap().round().toUInt64().toULong()
            UInt64(rng.nextULong(lower, upper)).toFlt64()
        } else if (variable.type.isIntegerType) {
            val lower = lowerBound!!.value.unwrap().round().toInt64().toLong()
            val upper = upperBound!!.value.unwrap().round().toInt64().toLong()
            Int64(rng.nextLong(lower, upper)).toFlt64()
        } else {
            Flt64(
                rng.nextDouble(
                    lowerBound!!.value.unwrap().toSolverDouble("token.random.lowerBound"),
                    upperBound!!.value.unwrap().toSolverDouble("token.random.upperBound")
                )
            )
        }
    }

    /** @return 基于 key 的哈希值 / Hash code based on key */
    override fun hashCode(): Int = key.hashCode()

    /**
     * @param other 待比较对象 / Object to compare
     * @return 是否相同 / Whether equal
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Token<*>
        return key == other.key
    }

    /** @return 字符串表示 / String representation */
    override fun toString() = "$name: ${result ?: "?"}"
}
