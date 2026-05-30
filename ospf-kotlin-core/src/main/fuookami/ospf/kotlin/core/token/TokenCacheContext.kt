@file:Suppress("DEPRECATION")
/**
 * 符号缓存上下文 / Token cache context
 */
package fuookami.ospf.kotlin.core.token

import fuookami.ospf.kotlin.core.model.basic.ExpressionRange
import fuookami.ospf.kotlin.core.symbol.IntermediateSymbol
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.Symbol
import java.util.*

/**
 * 线性展开缓存数据。
 * Linear flatten cache data.
 *
 * @param T 数值环类型 / The ring type
 * @property monomials 线性单项式列表 / List of linear monomials
 * @property constant 常数项 / Constant term
 */
data class LinearFlattenData<T : Ring<T>>(
    val monomials: List<LinearMonomial<T>>,
    val constant: T
)

/**
 * 二次展开缓存数据。
 * Quadratic flatten cache data.
 *
 * @param T 数值环类型 / The ring type
 * @property monomials 二次单项式列表 / List of quadratic monomials
 * @property constant 常数项 / Constant term
 */
data class QuadraticFlattenData<T : Ring<T>>(
    val monomials: List<QuadraticMonomial<T>>,
    val constant: T
)

/**
 * 线性展开缓存上下文。
 * Linear flatten cache context.
 *
 * @property cache 内部缓存映射 / Internal cache map
 */
class LinearFlattenContext<V : Ring<V>>(
    private val cache: MutableMap<Any, LinearFlattenData<V>?> = HashMap()
) {
    /**
     * 检查缓存中是否包含指定键 / Checks whether the cache contains the specified key
     *
     * @param cacheKey 缓存键 / Cache key
     * @return 是否包含 / Whether contains
     */
    fun contains(cacheKey: Any): Boolean {
        return cache.containsKey(cacheKey)
    }

    /**
     * 获取指定键的缓存数据 / Gets the cached data for the specified key
     *
     * @param cacheKey 缓存键 / Cache key
     * @return 缓存数据，不存在返回 null / Cached data, or null if absent
     */
    fun get(cacheKey: Any): LinearFlattenData<V>? {
        return cache[cacheKey]
    }

    /**
     * 存入指定键的缓存数据 / Puts the cached data for the specified key
     *
     * @param cacheKey 缓存键 / Cache key
     * @param value 缓存数据 / Cached data
     */
    fun put(cacheKey: Any, value: LinearFlattenData<V>?) {
        cache[cacheKey] = value
    }

    /**
     * 移除指定键的缓存数据 / Removes the cached data for the specified key
     *
     * @param cacheKey 缓存键 / Cache key
     * @return 被移除的数据，不存在返回 null / Removed data, or null if absent
     */
    fun remove(cacheKey: Any): LinearFlattenData<V>? {
        return cache.remove(cacheKey)
    }

    /**
     * 获取所有缓存键的快照 / Gets a snapshot of all cache keys
     *
     * @return 缓存键集合 / Set of cache keys
     */
    fun keys(): Set<Any> {
        return cache.keys.toSet()
    }

    /** 清空所有缓存 / Clears all caches */
    fun clear() {
        cache.clear()
    }
}

/**
 * 二次展开缓存上下文。
 * Quadratic flatten cache context.
 *
 * @property cache 内部缓存映射 / Internal cache map
 */
class QuadraticFlattenContext<V : Ring<V>>(
    private val cache: MutableMap<Any, QuadraticFlattenData<V>?> = HashMap()
) {
    /**
     * 检查缓存中是否包含指定键 / Checks whether the cache contains the specified key
     *
     * @param cacheKey 缓存键 / Cache key
     * @return 是否包含 / Whether contains
     */
    fun contains(cacheKey: Any): Boolean {
        return cache.containsKey(cacheKey)
    }

    /**
     * 获取指定键的缓存数据 / Gets the cached data for the specified key
     *
     * @param cacheKey 缓存键 / Cache key
     * @return 缓存数据，不存在返回 null / Cached data, or null if absent
     */
    fun get(cacheKey: Any): QuadraticFlattenData<V>? {
        return cache[cacheKey]
    }

    /**
     * 存入指定键的缓存数据 / Puts the cached data for the specified key
     *
     * @param cacheKey 缓存键 / Cache key
     * @param value 缓存数据 / Cached data
     */
    fun put(cacheKey: Any, value: QuadraticFlattenData<V>?) {
        cache[cacheKey] = value
    }

    /**
     * 移除指定键的缓存数据 / Removes the cached data for the specified key
     *
     * @param cacheKey 缓存键 / Cache key
     * @return 被移除的数据，不存在返回 null / Removed data, or null if absent
     */
    fun remove(cacheKey: Any): QuadraticFlattenData<V>? {
        return cache.remove(cacheKey)
    }

    /**
     * 获取所有缓存键的快照 / Gets a snapshot of all cache keys
     *
     * @return 缓存键集合 / Set of cache keys
     */
    fun keys(): Set<Any> {
        return cache.keys.toSet()
    }

    /** 清空所有缓存 / Clears all caches */
    fun clear() {
        cache.clear()
    }
}

/**
 * 值缓存上下文，按 solution 或 fixedValues 维度分别缓存求解结果。
 * Value cache context, caching solve results separately by solution and fixedValues dimensions.
 *
 * @property solutionCache 按 solution 维度的缓存 / Cache by solution dimension
 * @property fixedValueCache 按 fixedValues 维度的缓存 / Cache by fixedValues dimension
 */
class ValueCacheContext<V : RealNumber<V>>(
    private val solutionCache: MutableMap<Pair<Any, List<V>?>, V?> = HashMap(),
    private val fixedValueCache: MutableMap<Pair<Any, Map<Symbol, V>>, V?> = HashMap()
) {
    /** 清空所有缓存 / Clears all caches */
    fun clear() {
        solutionCache.clear()
        fixedValueCache.clear()
    }

    /**
     * 检查 solution 维度缓存中是否包含指定键 / Checks whether the solution cache contains the specified key
     *
     * @param cacheKey 缓存键 / Cache key
     * @param solution 解的列表 / Solution list
     * @return 是否包含 / Whether contains
     */
    fun cached(cacheKey: Any, solution: List<V>? = null): Boolean {
        return solutionCache.containsKey(cacheKey to solution)
    }

    /**
     * 检查 fixedValues 维度缓存中是否包含指定键 / Checks whether the fixedValues cache contains the specified key
     *
     * @param cacheKey 缓存键 / Cache key
     * @param fixedValues 固定值映射 / Fixed values map
     * @return 是否包含 / Whether contains
     */
    fun cached(cacheKey: Any, fixedValues: Map<Symbol, V>): Boolean {
        return fixedValueCache.containsKey(cacheKey to fixedValues)
    }

    /**
     * 获取 solution 维度的缓存值 / Gets the cached value by solution dimension
     *
     * @param cacheKey 缓存键 / Cache key
     * @param solution 解的列表 / Solution list
     * @return 缓存值 / Cached value
     */
    fun value(cacheKey: Any, solution: List<V>? = null): V? {
        return solutionCache[cacheKey to solution]
    }

    /**
     * 获取 fixedValues 维度的缓存值 / Gets the cached value by fixedValues dimension
     *
     * @param cacheKey 缓存键 / Cache key
     * @param fixedValues 固定值映射 / Fixed values map
     * @return 缓存值 / Cached value
     */
    fun value(cacheKey: Any, fixedValues: Map<Symbol, V>): V? {
        return fixedValueCache[cacheKey to fixedValues]
    }

    /**
     * 存入 solution 维度的缓存值 / Puts a cached value by solution dimension
     *
     * @param cacheKey 缓存键 / Cache key
     * @param solution 解的列表 / Solution list
     * @param value 要缓存的值 / Value to cache
     * @return 存入的值 / The stored value
     */
    fun put(cacheKey: Any, solution: List<V>? = null, value: V): V {
        solutionCache[cacheKey to solution] = value
        return value
    }

    /**
     * 存入 fixedValues 维度的缓存值 / Puts a cached value by fixedValues dimension
     *
     * @param cacheKey 缓存键 / Cache key
     * @param fixedValues 固定值映射 / Fixed values map
     * @param value 要缓存的值 / Value to cache
     * @return 存入的值 / The stored value
     */
    fun put(cacheKey: Any, fixedValues: Map<Symbol, V>, value: V): V {
        fixedValueCache[cacheKey to fixedValues] = value
        return value
    }

    /**
     * 批量存入 solution 维度的缓存值 / Batch puts cached values by solution dimension
     *
     * @param symbols 符号到值的映射 / Symbol-to-value map
     * @param solution 解的列表 / Solution list
     */
    fun putAll(symbols: Map<out Any, V>, solution: List<V>? = null) {
        solutionCache.putAll(symbols.map { (cacheKey, value) ->
            (cacheKey to solution) to value
        })
    }

    /**
     * 批量存入 fixedValues 维度的缓存值 / Batch puts cached values by fixedValues dimension
     *
     * @param symbols 符号到值的映射 / Symbol-to-value map
     * @param fixedValues 固定值映射 / Fixed values map
     */
    fun putAll(symbols: Map<out Any, V>, fixedValues: Map<Symbol, V>) {
        fixedValueCache.putAll(symbols.map { (cacheKey, value) ->
            (cacheKey to fixedValues) to value
        })
    }

    /**
     * 惯性批量存入 solution 维度的缓存值，跳过 null 结果 / Lazy batch puts by solution dimension, skipping null results
     *
     * @param symbols 符号到惰性值的映射 / Symbol-to-lazy-value map
     * @param solution 解的列表 / Solution list
     */
    fun putAllLazy(symbols: Map<out Any, () -> V?>, solution: List<V>? = null) {
        solutionCache.putAll(symbols.mapNotNull { (cacheKey, value) ->
            value()?.let {
                (cacheKey to solution) to it
            }
        })
    }

    /**
     * 惯性批量存入 fixedValues 维度的缓存值，跳过 null 结果 / Lazy batch puts by fixedValues dimension, skipping null results
     *
     * @param symbols 符号到惰性值的映射 / Symbol-to-lazy-value map
     * @param fixedValues 固定值映射 / Fixed values map
     */
    fun putAllLazy(symbols: Map<out Any, () -> V?>, fixedValues: Map<Symbol, V>) {
        fixedValueCache.putAll(symbols.mapNotNull { (cacheKey, value) ->
            value()?.let {
                (cacheKey to fixedValues) to it
            }
        })
    }

    /**
     * 获取或存入 solution 维度的缓存值 / Gets or puts a cached value by solution dimension
     *
     * @param cacheKey 缓存键 / Cache key
     * @param solution 解的列表 / Solution list
     * @param value 惰性求值工厂 / Lazy value factory
     * @return 缓存值 / Cached value
     */
    fun getOrPut(cacheKey: Any, solution: List<V>? = null, value: () -> V?): V? {
        var cachedValue = solutionCache[cacheKey to solution]
        if (cachedValue == null) {
            value()?.let {
                cachedValue = it
                solutionCache[cacheKey to solution] = it
            }
        }
        return cachedValue
    }

    /**
     * 获取或存入 fixedValues 维度的缓存值 / Gets or puts a cached value by fixedValues dimension
     *
     * @param cacheKey 缓存键 / Cache key
     * @param fixedValues 固定值映射 / Fixed values map
     * @param value 惰性求值工厂 / Lazy value factory
     * @return 缓存值 / Cached value
     */
    fun getOrPut(cacheKey: Any, fixedValues: Map<Symbol, V>, value: () -> V?): V? {
        var cachedValue = fixedValueCache[cacheKey to fixedValues]
        if (cachedValue == null) {
            value()?.let {
                cachedValue = it
                fixedValueCache[cacheKey to fixedValues] = it
            }
        }
        return cachedValue
    }

    /**
     * 移除指定键在所有维度的缓存 / Removes caches for the specified key across all dimensions
     *
     * @param cacheKey 缓存键 / Cache key
     */
    fun remove(cacheKey: Any) {
        solutionCache.keys.removeAll { it.first == cacheKey }
        fixedValueCache.keys.removeAll { it.first == cacheKey }
    }
}

/**
 * 范围缓存上下文。
 * Range cache context.
 *
 * @property cache 内部缓存映射 / Internal cache map
 */
class RangeCacheContext<V>(
    private val cache: MutableMap<Any, ExpressionRange<V>?> = HashMap()
) where V : RealNumber<V>, V : NumberField<V> {
    /**
     * 检查缓存中是否包含指定键 / Checks whether the cache contains the specified key
     *
     * @param cacheKey 缓存键 / Cache key
     * @return 是否包含 / Whether contains
     */
    fun contains(cacheKey: Any): Boolean {
        return cache.containsKey(cacheKey)
    }

    /**
     * 获取指定键的缓存数据 / Gets the cached data for the specified key
     *
     * @param cacheKey 缓存键 / Cache key
     * @return 缓存数据，不存在返回 null / Cached data, or null if absent
     */
    fun get(cacheKey: Any): ExpressionRange<V>? {
        return cache[cacheKey]
    }

    /**
     * 存入指定键的缓存数据 / Puts the cached data for the specified key
     *
     * @param cacheKey 缓存键 / Cache key
     * @param value 缓存数据 / Cached data
     */
    fun put(cacheKey: Any, value: ExpressionRange<V>?) {
        cache[cacheKey] = value
    }

    /**
     * 移除指定键的缓存数据 / Removes the cached data for the specified key
     *
     * @param cacheKey 缓存键 / Cache key
     * @return 被移除的数据，不存在返回 null / Removed data, or null if absent
     */
    fun remove(cacheKey: Any): ExpressionRange<V>? {
        return cache.remove(cacheKey)
    }

    /**
     * 获取所有缓存键的快照 / Gets a snapshot of all cache keys
     *
     * @return 缓存键集合 / Set of cache keys
     */
    fun keys(): Set<Any> {
        return cache.keys.toSet()
    }

    /** 清空所有缓存 / Clears all caches */
    fun clear() {
        cache.clear()
    }
}

/**
 * 聚合所有 token 缓存上下文的容器。
 * Container aggregating all token cache contexts.
 *
 * @property linearFlatten 线性展开缓存 / Linear flatten cache
 * @property quadraticFlatten 二次展开缓存 / Quadratic flatten cache
 * @property value 值缓存 / Value cache
 * @property range 范围缓存 / Range cache
 */
data class TokenCacheContexts<V>(
    val linearFlatten: LinearFlattenContext<V> = LinearFlattenContext(),
    val quadraticFlatten: QuadraticFlattenContext<V> = QuadraticFlattenContext(),
    val value: ValueCacheContext<V> = ValueCacheContext(),
    val range: RangeCacheContext<V> = RangeCacheContext()
) where V : RealNumber<V>, V : NumberField<V> {
    /**
     * 获取所有已绑定符号的集合 / Gets the set of all bound symbols
     *
     * @return 符号集合 / Set of symbols
     */
    fun boundSymbols(): Set<Any> {
        return linearFlatten.keys() + quadraticFlatten.keys() + range.keys()
    }

    /**
     * 获取所有已绑定的中间符号 / Gets all bound intermediate symbols
     *
     * @return 中间符号集合 / Set of intermediate symbols
     */
    fun boundIntermediateSymbols(): Set<IntermediateSymbol<*>> {
        return boundSymbols().mapNotNull { it as? IntermediateSymbol<*> }.toSet()
    }

    /** 清空线性展开缓存 / Clears the linear flatten cache */
    fun clearLinearFlatten() {
        linearFlatten.clear()
    }

    /** 清空二次展开缓存 / Clears the quadratic flatten cache */
    fun clearQuadraticFlatten() {
        quadraticFlatten.clear()
    }

    /** 清空所有展开缓存 / Clears all flatten caches */
    fun clearFlatten() {
        clearLinearFlatten()
        clearQuadraticFlatten()
    }

    /** 清空值缓存 / Clears the value cache */
    fun clearValue() {
        value.clear()
    }

    /** 清空范围缓存 / Clears the range cache */
    fun clearRange() {
        range.clear()
    }

    /** 清空所有缓存 / Clears all caches */
    fun clearAll() {
        clearFlatten()
        clearValue()
        clearRange()
    }
}

/** 符号到符号表的弱引用绑定表 / Weak-reference binding table from symbols to token tables */
private val symbolTokenTableContext = Collections.synchronizedMap(
    WeakHashMap<IntermediateSymbol<*>, AbstractTokenTable<*>>()
)

/**
 * 绑定符号到符号表，若已有不同绑定则清除旧缓存 / Binds a symbol to a token table, clearing old caches if a different binding exists
 *
 * @param symbol 中间符号 / Intermediate symbol
 * @param tokenTable 符号表 / Token table
 */
internal fun <V> bindTokenTableContext(symbol: IntermediateSymbol<*>, tokenTable: AbstractTokenTable<V>) where V : RealNumber<V>, V : NumberField<V> {
    val oldTokenTable = symbolTokenTableContext[symbol]
    if (oldTokenTable != null && oldTokenTable != tokenTable) {
        oldTokenTable.clearLinearFlatten(symbol)
        oldTokenTable.clearQuadraticFlatten(symbol)
        oldTokenTable.clearRange(symbol)
        oldTokenTable.clearValue(symbol)
    }
    symbolTokenTableContext[symbol] = tokenTable
}

/**
 * 解除符号与指定符号表的绑定 / Unbinds the symbol from the specified token table
 *
 * @param symbol 中间符号 / Intermediate symbol
 * @param tokenTable 符号表 / Token table
 */
internal fun <V> unbindTokenTableContext(symbol: IntermediateSymbol<*>, tokenTable: AbstractTokenTable<V>) where V : RealNumber<V>, V : NumberField<V> {
    if (symbolTokenTableContext[symbol] == tokenTable) {
        symbolTokenTableContext.remove(symbol)
    }
}

/**
 * 获取符号绑定的符号表，若不存在则返回 null / Gets the token table bound to the symbol, or null if none exists
 *
 * @param symbol 中间符号 / Intermediate symbol
 * @return 绑定的符号表，或 null / Bound token table, or null
 */
internal fun boundTokenTableContext(symbol: IntermediateSymbol<*>): AbstractTokenTable<*>? {
    return symbolTokenTableContext[symbol]
}

/**
 * 将线性展开数据转换为二次展开数据 / Converts linear flatten data to quadratic flatten data
 *
 * @return 二次展开数据 / Quadratic flatten data
 */
internal fun <V> LinearFlattenData<V>.toQuadraticFlattenData(): QuadraticFlattenData<V> where V : RealNumber<V>, V : NumberField<V> {
    val monomials = this.monomials.mapNotNull {
        val sym = it.symbol as? AbstractVariableItem<*, *> ?: return@mapNotNull null
        QuadraticMonomial(
            coefficient = it.coefficient,
            symbol1 = sym,
            symbol2 = null
        )
    }
    return QuadraticFlattenData<V>(
        monomials = monomials,
        constant = this.constant
    )
}
