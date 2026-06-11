/**
 * Token 表接口与实现，管理中间符号的注册、缓存与求解结果。
 * Token table interfaces and implementations managing intermediate symbol registration, caching, and solve results.
 */
package fuookami.ospf.kotlin.core.token

import fuookami.ospf.kotlin.core.model.basic.ExpressionRange
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.IntermediateSymbol
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.*
import fuookami.ospf.kotlin.math.usize
import fuookami.ospf.kotlin.utils.concept.Copyable
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

/**
 * 重复符号错误，当尝试注册已存在的符号时抛出。
 * Repeated symbol error thrown when attempting to register an already-existing symbol.
 *
 * @property repeatedSymbol 已存在的重复符号 / The pre-existing repeated symbol
 * @property symbol 新注册的冲突符号 / The new conflicting symbol being registered
 */
class RepeatedSymbolError(
    val repeatedSymbol: IntermediateSymbol<*>,
    val symbol: IntermediateSymbol<*>
) : Throwable() {
    override val message get() = "Repeated \"${symbol.name}\", old: $repeatedSymbol, new: $symbol."
}

/**
 * Token 表的抽象接口，定义符号注册、缓存查询和求解结果管理的契约。
 * Abstract interface for token tables, defining the contract for symbol registration, cache queries, and solution management.
 *
 * @param V 数值类型 / The number type
 */
interface AbstractTokenTable<V> : AutoCloseable where V : RealNumber<V>, V : NumberField<V> {
    /** 符号操作类别 / Symbol operation category */
    val category: Category
    /** token 列表 / Token list */
    val tokenList: AbstractTokenList<V>
    /** 已注册的中间符号集合 / Registered intermediate symbol collection */
    val symbols: Collection<IntermediateSymbol<*>>

    /** 所有 token 集合 / All tokens collection */
    val tokens: Collection<Token<V>> get() = tokenList.tokens
    /** 求解器中的 token 列表 / Tokens in solver */
    val tokensInSolver: List<Token<V>> get() = tokenList.tokensInSolver
    /** 是否已缓存求解结果 / Whether solution is cached */
    val cachedSolution: Boolean get() = tokenList.cachedSolution

    /**
     * 按变量项查找 token / Find token by variable item
     *
     * @param item 变量项 / Variable item
     * @return 对应 token，不存在返回 null / Corresponding token, or null
     */
    fun find(item: AbstractVariableItem<*, *>): Token<V>? = tokenList.find(item)

    /**
     * 按索引查找 token / Find token by index
     *
     * @param index 索引 / Index
     * @return 对应 token，不存在返回 null / Corresponding token, or null
     */
    fun find(index: Int): Token<V>? = tokenList.find(index)

    /**
     * 按索引获取 token / Get token by index
     *
     * @param index 索引 / Index
     * @return 对应 token / Corresponding token
     */
    operator fun get(index: Int): Token<V> = tokenList[index]

    /**
     * 获取 token 的索引 / Get the index of a token
     *
     * @param token token 实例 / Token instance
     * @return 索引，不存在返回 null / Index, or null
     */
    fun indexOf(token: Token<V>): Int? = tokenList.indexOf(token)

    /**
     * 获取变量项对应 token 的索引 / Get the index of the token for a variable item
     *
     * @param item 变量项 / Variable item
     * @return 索引，不存在返回 null / Index, or null
     */
    fun indexOf(item: AbstractVariableItem<*, *>): Int? = find(item)?.let { indexOf(it) }

    /**
     * 获取排除指定变量项后的求解器 token 列表 / Get solver tokens excluding specified variable items
     *
     * @param items 要排除的变量项集合 / Variable items to exclude
     * @return 过滤后的 token 列表 / Filtered token list
     */
    fun tokensInSolverWithout(items: Set<AbstractVariableItem<*, *>>): List<Token<V>> {
        val result = ArrayList<Token<V>>()
        for (token in this.tokensInSolver) {
            if (token.variable !in items) {
                result.add(token)
            }
        }
        return result
    }

    /**
     * 设置求解结果（列表形式）/ Set solution (list form)
     *
     * @param solution 求解结果列表 / Solution list
     */
    fun setSolution(solution: List<V>) {
        flush()
        tokenList.setSolution(solution)
    }

    /**
     * 设置求解结果（映射形式）/ Set solution (map form)
     *
     * @param solution 变量到值的映射 / Variable-to-value map
     */
    fun setSolution(solution: Map<AbstractVariableItem<*, *>, V>) {
        flush()
        tokenList.setSolution(solution)
    }

    /**
     * 设置求解器原始求解结果（Flt64 列表）/ Set solver solution from Flt64 list
     *
     * @param solution Flt64 求解结果列表 / Flt64 solution list
     */
    fun setSolverSolution(solution: List<Flt64>) {
        flush()
        tokenList.setSolverSolution(solution)
    }

    /**
     * 设置求解器原始求解结果（Flt64 映射）/ Set solver solution from Flt64 map
     *
     * @param solution 变量到 Flt64 值的映射 / Variable-to-Flt64 map
     */
    fun setSolverSolution(solution: Map<AbstractVariableItem<*, *>, Flt64>) {
        flush()
        tokenList.setSolverSolution(solution)
    }

    /** 刷新缓存 / Flush caches */
    fun flush() {}

    /** 清空求解结果 / Clear solution */
    fun clearSolution() { flush(); tokenList.clearSolution() }

    /**
     * 检查 solution 维度缓存中是否已缓存 / Check if cached by solution dimension
     *
     * @param cacheKey 缓存键 / Cache key
     * @param solution 解的列表 / Solution list
     * @return 是否已缓存 / Whether cached
     */
    fun cached(cacheKey: Any, solution: List<V>? = null): Boolean? = null

    /**
     * 检查 fixedValues 维度缓存中是否已缓存 / Check if cached by fixedValues dimension
     *
     * @param cacheKey 缓存键 / Cache key
     * @param fixedValues 固定值映射 / Fixed values map
     * @return 是否已缓存 / Whether cached
     */
    fun cached(cacheKey: Any, fixedValues: Map<Symbol, V>): Boolean? = null

    /**
     * 获取 solution 维度的缓存值 / Get cached value by solution dimension
     *
     * @param cacheKey 缓存键 / Cache key
     * @param solution 解的列表 / Solution list
     * @return 缓存值 / Cached value
     */
    fun cachedValue(cacheKey: Any, solution: List<V>? = null): V? = null

    /**
     * 获取 fixedValues 维度的缓存值 / Get cached value by fixedValues dimension
     *
     * @param cacheKey 缓存键 / Cache key
     * @param fixedValues 固定值映射 / Fixed values map
     * @return 缓存值 / Cached value
     */
    fun cachedValue(cacheKey: Any, fixedValues: Map<Symbol, V>): V? = null

    /**
     * 存入 solution 维度的缓存值 / Put cached value by solution dimension
     *
     * @param cacheKey 缓存键 / Cache key
     * @param solution 解的列表 / Solution list
     * @param value 要缓存的值 / Value to cache
     * @return 存入的值 / The stored value
     */
    fun cache(cacheKey: Any, solution: List<V>? = null, value: V): V = value

    /**
     * 存入 fixedValues 维度的缓存值 / Put cached value by fixedValues dimension
     *
     * @param cacheKey 缓存键 / Cache key
     * @param fixedValues 固定值映射 / Fixed values map
     * @param value 要缓存的值 / Value to cache
     * @return 存入的值 / The stored value
     */
    fun cache(cacheKey: Any, fixedValues: Map<Symbol, V>, value: V): V = value

    // --- Solver-boundary cache adapters (Flt64 -> V via converter) ---

    /**
     * 检查求解器 solution 维度缓存是否已缓存 / Check if cached by solver solution dimension
     *
     * @param cacheKey 缓存键 / Cache key
     * @param solution Flt64 解的列表 / Flt64 solution list
     * @param converter 值转换器 / Value converter
     * @return 是否已缓存 / Whether cached
     */
    fun cachedSolver(cacheKey: Any, solution: List<Flt64>? = null, converter: IntoValue<V>): Boolean? {
        return cached(cacheKey, solution?.map { converter.intoValue(it) })
    }

    /**
     * 检查求解器 fixedValues 维度缓存是否已缓存 / Check if cached by solver fixedValues dimension
     *
     * @param cacheKey 缓存键 / Cache key
     * @param fixedValues Flt64 固定值映射 / Flt64 fixed values map
     * @param converter 值转换器 / Value converter
     * @return 是否已缓存 / Whether cached
     */
    fun cachedSolver(cacheKey: Any, fixedValues: Map<Symbol, Flt64>, converter: IntoValue<V>): Boolean? {
        return cached(cacheKey, fixedValues.mapValues { converter.intoValue(it.value) })
    }

    /**
     * 获取求解器 solution 维度的缓存值 / Get cached value by solver solution dimension
     *
     * @param cacheKey 缓存键 / Cache key
     * @param solution Flt64 解的列表 / Flt64 solution list
     * @param converter 值转换器 / Value converter
     * @return 缓存值 / Cached value
     */
    fun cachedSolverValue(cacheKey: Any, solution: List<Flt64>? = null, converter: IntoValue<V>): V? {
        return cachedValue(cacheKey, solution?.map { converter.intoValue(it) })
    }

    /**
     * 获取求解器 fixedValues 维度的缓存值 / Get cached value by solver fixedValues dimension
     *
     * @param cacheKey 缓存键 / Cache key
     * @param fixedValues Flt64 固定值映射 / Flt64 fixed values map
     * @param converter 值转换器 / Value converter
     * @return 缓存值 / Cached value
     */
    fun cachedSolverValue(cacheKey: Any, fixedValues: Map<Symbol, Flt64>, converter: IntoValue<V>): V? {
        return cachedValue(cacheKey, fixedValues.mapValues { converter.intoValue(it.value) })
    }

    /**
     * 存入求解器 solution 维度的缓存值 / Put cached value by solver solution dimension
     *
     * @param cacheKey 缓存键 / Cache key
     * @param solution Flt64 解的列表 / Flt64 solution list
     * @param value 要缓存的值 / Value to cache
     * @param converter 值转换器 / Value converter
     * @return 存入的值 / The stored value
     */
    fun cacheSolver(cacheKey: Any, solution: List<Flt64>? = null, value: V, converter: IntoValue<V>): V {
        return cache(cacheKey, solution?.map { converter.intoValue(it) }, value)
    }

    /**
     * 存入求解器 fixedValues 维度的缓存值 / Put cached value by solver fixedValues dimension
     *
     * @param cacheKey 缓存键 / Cache key
     * @param fixedValues Flt64 固定值映射 / Flt64 fixed values map
     * @param value 要缓存的值 / Value to cache
     * @param converter 值转换器 / Value converter
     * @return 存入的值 / The stored value
     */
    fun cacheSolver(cacheKey: Any, fixedValues: Map<Symbol, Flt64>, value: V, converter: IntoValue<V>): V {
        return cache(cacheKey, fixedValues.mapValues { converter.intoValue(it.value) }, value)
    }

    // Generic cache methods (V-generic)

    /**
     * 检查线性展开缓存是否已缓存 / Check if linear flatten is cached
     *
     * @param cacheKey 缓存键 / Cache key
     * @return 是否已缓存 / Whether cached
     */
    fun cachedLinearFlatten(cacheKey: Any): Boolean? = null

    /**
     * 获取线性展开缓存值 / Get cached linear flatten value
     *
     * @param cacheKey 缓存键 / Cache key
     * @return 缓存数据 / Cached data
     */
    fun cachedLinearFlattenValue(cacheKey: Any): LinearFlattenData<V>? = null

    /**
     * 存入线性展开缓存 / Put linear flatten cache
     *
     * @param cacheKey 缓存键 / Cache key
     * @param flatten 展开数据 / Flatten data
     * @return 存入的数据 / The stored data
     */
    fun cacheLinearFlatten(cacheKey: Any, flatten: LinearFlattenData<V>?): LinearFlattenData<V>? = flatten

    /**
     * 清除线性展开缓存 / Clear linear flatten cache
     *
     * @param cacheKey 缓存键 / Cache key
     * @return 被清除的数据 / Cleared data
     */
    fun clearLinearFlatten(cacheKey: Any): LinearFlattenData<V>? = null

    /**
     * 检查二次展开缓存是否已缓存 / Check if quadratic flatten is cached
     *
     * @param cacheKey 缓存键 / Cache key
     * @return 是否已缓存 / Whether cached
     */
    fun cachedQuadraticFlatten(cacheKey: Any): Boolean? = null

    /**
     * 获取二次展开缓存值 / Get cached quadratic flatten value
     *
     * @param cacheKey 缓存键 / Cache key
     * @return 缓存数据 / Cached data
     */
    fun cachedQuadraticFlattenValue(cacheKey: Any): QuadraticFlattenData<V>? = null

    /**
     * 存入二次展开缓存 / Put quadratic flatten cache
     *
     * @param cacheKey 缓存键 / Cache key
     * @param flatten 展开数据 / Flatten data
     * @return 存入的数据 / The stored data
     */
    fun cacheQuadraticFlatten(cacheKey: Any, flatten: QuadraticFlattenData<V>?): QuadraticFlattenData<V>? = flatten

    /**
     * 清除二次展开缓存 / Clear quadratic flatten cache
     *
     * @param cacheKey 缓存键 / Cache key
     * @return 被清除的数据 / Cleared data
     */
    fun clearQuadraticFlatten(cacheKey: Any): QuadraticFlattenData<V>? = null

    /**
     * 检查范围缓存是否已缓存 / Check if range is cached
     *
     * @param cacheKey 缓存键 / Cache key
     * @return 是否已缓存 / Whether cached
     */
    fun cachedRange(cacheKey: Any): Boolean? = null

    /**
     * 获取范围缓存值 / Get cached range value
     *
     * @param cacheKey 缓存键 / Cache key
     * @return 缓存范围 / Cached range
     */
    fun cachedRangeValue(cacheKey: Any): ExpressionRange<V>? = null

    /**
     * 存入范围缓存 / Put range cache
     *
     * @param cacheKey 缓存键 / Cache key
     * @param range 范围数据 / Range data
     * @return 存入的范围 / The stored range
     */
    fun cacheRange(cacheKey: Any, range: ExpressionRange<V>?): ExpressionRange<V>? = range

    /**
     * 清除范围缓存 / Clear range cache
     *
     * @param cacheKey 缓存键 / Cache key
     * @return 被清除的范围 / Cleared range
     */
    fun clearRange(cacheKey: Any): ExpressionRange<V>? = null

    /**
     * 清除值缓存 / Clear value cache
     *
     * @param cacheKey 缓存键 / Cache key
     */
    fun clearValue(cacheKey: Any) {}

    // Lazy and batch cache methods (solver boundary, Flt64)

    /**
     * 惰性存入 solution 维度的缓存值 / Lazy put cached value by solution dimension
     *
     * @param cacheKey 缓存键 / Cache key
     * @param solution 解的列表 / Solution list
     * @param value 惰性求值工厂 / Lazy value factory
     * @return 缓存值 / Cached value
     */
    fun cache(cacheKey: Any, solution: List<V>? = null, value: () -> V?): V? {
        return value()?.let { cache(cacheKey = cacheKey, solution = solution, value = it) }
    }

    /**
     * 惰性存入 fixedValues 维度的缓存值 / Lazy put cached value by fixedValues dimension
     *
     * @param cacheKey 缓存键 / Cache key
     * @param fixedValues 固定值映射 / Fixed values map
     * @param value 惰性求值工厂 / Lazy value factory
     * @return 缓存值 / Cached value
     */
    fun cache(cacheKey: Any, fixedValues: Map<Symbol, V>, value: () -> V?): V? {
        return value()?.let { cache(cacheKey = cacheKey, fixedValues = fixedValues, value = it) }
    }

    /**
     * 未缓存时存入 solution 维度的缓存值 / Cache by solution dimension if not already cached
     *
     * @param cacheKey 缓存键 / Cache key
     * @param solution 解的列表 / Solution list
     * @param value 惰性求值工厂 / Lazy value factory
     * @return 缓存值 / Cached value
     */
    fun cacheIfNotCached(cacheKey: Any, solution: List<V>? = null, value: () -> V?): V? {
        var cachedValue = this.cachedValue(cacheKey, solution)
        if (cachedValue == null) {
            value()?.let {
                cachedValue = it
                cache(cacheKey = cacheKey, solution = solution, value = it)
            }
        }
        return cachedValue
    }

    /**
     * 未缓存时存入 fixedValues 维度的缓存值 / Cache by fixedValues dimension if not already cached
     *
     * @param cacheKey 缓存键 / Cache key
     * @param fixedValues 固定值映射 / Fixed values map
     * @param value 惰性求值工厂 / Lazy value factory
     * @return 缓存值 / Cached value
     */
    fun cacheIfNotCached(cacheKey: Any, fixedValues: Map<Symbol, V>, value: () -> V?): V? {
        var cachedValue = this.cachedValue(cacheKey, fixedValues)
        if (cachedValue == null) {
            value()?.let {
                cachedValue = it
                cache(cacheKey = cacheKey, fixedValues = fixedValues, value = it)
            }
        }
        return cachedValue
    }

    /**
     * 未缓存时存入求解器 solution 维度的缓存值 / Cache by solver solution dimension if not already cached
     *
     * @param cacheKey 缓存键 / Cache key
     * @param solution Flt64 解的列表 / Flt64 solution list
     * @param value 惰性求值工厂 / Lazy value factory
     * @param converter 值转换器 / Value converter
     * @return 缓存值 / Cached value
     */
    fun cacheSolverIfNotCached(cacheKey: Any, solution: List<Flt64>? = null, value: () -> V?, converter: IntoValue<V>): V? {
        return cacheIfNotCached(cacheKey, solution?.map { converter.intoValue(it) }, value)
    }

    /**
     * 未缓存时存入求解器 fixedValues 维度的缓存值 / Cache by solver fixedValues dimension if not already cached
     *
     * @param cacheKey 缓存键 / Cache key
     * @param fixedValues Flt64 固定值映射 / Flt64 fixed values map
     * @param value 惰性求值工厂 / Lazy value factory
     * @param converter 值转换器 / Value converter
     * @return 缓存值 / Cached value
     */
    fun cacheSolverIfNotCached(cacheKey: Any, fixedValues: Map<Symbol, Flt64>, value: () -> V?, converter: IntoValue<V>): V? {
        return cacheIfNotCached(cacheKey, fixedValues.mapValues { converter.intoValue(it.value) }, value)
    }

    /**
     * 批量存入 solution 维度的符号缓存 / Batch put symbol cache by solution dimension
     *
     * @param symbols 符号到值的映射 / Symbol-to-value map
     * @param solution 解的列表 / Solution list
     */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("cacheSymbols")
    fun cache(symbols: Map<IntermediateSymbol<*>, V>, solution: List<V>? = null) {
        for ((symbol, value) in symbols) { cache(cacheKey = symbol, solution = solution, value = value) }
    }

    /**
     * 批量存入 fixedValues 维度的符号缓存 / Batch put symbol cache by fixedValues dimension
     *
     * @param symbols 符号到值的映射 / Symbol-to-value map
     * @param fixedValues 固定值映射 / Fixed values map
     */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("cacheSymbols")
    fun cache(symbols: Map<IntermediateSymbol<*>, V>, fixedValues: Map<Symbol, V>) {
        for ((symbol, value) in symbols) { cache(cacheKey = symbol, fixedValues = fixedValues, value = value) }
    }

    /**
     * 惰性批量存入 solution 维度的符号缓存 / Lazy batch put symbol cache by solution dimension
     *
     * @param symbols 符号到惰性值的映射 / Symbol-to-lazy-value map
     * @param solution 解的列表 / Solution list
     */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("lazyCacheSymbols")
    fun cache(symbols: Map<IntermediateSymbol<*>, () -> V?>, solution: List<V>? = null) {
        for ((symbol, value) in symbols) { cache(cacheKey = symbol, solution = solution, value = value) }
    }

    /**
     * 惰性批量存入 fixedValues 维度的符号缓存 / Lazy batch put symbol cache by fixedValues dimension
     *
     * @param symbols 符号到惰性值的映射 / Symbol-to-lazy-value map
     * @param fixedValues 固定值映射 / Fixed values map
     */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("lazyCacheSymbols")
    fun cache(symbols: Map<IntermediateSymbol<*>, () -> V?>, fixedValues: Map<Symbol, V>) {
        for ((symbol, value) in symbols) { cache(cacheKey = symbol, fixedValues = fixedValues, value = value) }
    }

    /**
     * 批量存入求解器 solution 维度的符号缓存 / Batch put symbol cache by solver solution dimension
     *
     * @param symbols 符号到值的映射 / Symbol-to-value map
     * @param solution Flt64 解的列表 / Flt64 solution list
     * @param converter 值转换器 / Value converter
     */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("cacheSolverSymbols")
    fun cacheSolver(symbols: Map<IntermediateSymbol<*>, V>, solution: List<Flt64>? = null, converter: IntoValue<V>) {
        cache(symbols, solution?.map { converter.intoValue(it) })
    }

    /**
     * 批量存入求解器 fixedValues 维度的符号缓存 / Batch put symbol cache by solver fixedValues dimension
     *
     * @param symbols 符号到值的映射 / Symbol-to-value map
     * @param fixedValues Flt64 固定值映射 / Flt64 fixed values map
     * @param converter 值转换器 / Value converter
     */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("cacheSolverSymbols")
    fun cacheSolver(symbols: Map<IntermediateSymbol<*>, V>, fixedValues: Map<Symbol, Flt64>, converter: IntoValue<V>) {
        cache(symbols, fixedValues.mapValues { converter.intoValue(it.value) })
    }

    /**
     * 惰性批量存入求解器 solution 维度的符号缓存 / Lazy batch put symbol cache by solver solution dimension
     *
     * @param symbols 符号到惰性值的映射 / Symbol-to-lazy-value map
     * @param solution Flt64 解的列表 / Flt64 solution list
     * @param converter 值转换器 / Value converter
     */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("lazyCacheSolverSymbols")
    fun cacheSolver(symbols: Map<IntermediateSymbol<*>, () -> V?>, solution: List<Flt64>? = null, converter: IntoValue<V>) {
        cache(symbols, solution?.map { converter.intoValue(it) })
    }

    /**
     * 惰性批量存入求解器 fixedValues 维度的符号缓存 / Lazy batch put symbol cache by solver fixedValues dimension
     *
     * @param symbols 符号到惰性值的映射 / Symbol-to-lazy-value map
     * @param fixedValues Flt64 固定值映射 / Flt64 fixed values map
     * @param converter 值转换器 / Value converter
     */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("lazyCacheSolverSymbols")
    fun cacheSolver(symbols: Map<IntermediateSymbol<*>, () -> V?>, fixedValues: Map<Symbol, Flt64>, converter: IntoValue<V>) {
        cache(symbols, fixedValues.mapValues { converter.intoValue(it.value) })
    }

    /** 关闭并释放资源 / Close and release resources */
    override fun close() { tokenList.close() }

    /** 符号依赖关系映射 / Symbol dependency mapping */
    val symbolDependencies: Map<IntermediateSymbol<*>, Set<IntermediateSymbol<*>>> get() = emptyMap()
}

/**
 * 通用可变 Token 表接口骨架（C2-2.5a 声明层）。
 * Generic mutable token table interface skeleton - C2-2.5a declaration layer.
 *
 * @param V 数值类型 / The number type
 */
interface AbstractMutableTokenTable<V> : AbstractTokenTable<V>, AddableTokenCollection<V>, Copyable<AbstractMutableTokenTable<V>> where V : RealNumber<V>, V : NumberField<V> {
    /**
     * 添加变量项 / Add variable item
     *
     * @param item 变量项 / Variable item
     * @return 操作结果 / Operation result
     */
    override fun add(item: AbstractVariableItem<*, *>): Try

    /**
     * 批量添加变量项 / Batch add variable items
     *
     * @param items 变量项集合 / Variable items
     * @return 操作结果 / Operation result
     */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addVariables")
    override fun add(items: Iterable<AbstractVariableItem<*, *>>): Try

    /**
     * 移除变量项 / Remove variable item
     *
     * @param item 变量项 / Variable item
     */
    fun remove(item: AbstractVariableItem<*, *>)

    /**
     * 添加中间符号 / Add intermediate symbol
     *
     * @param symbol 中间符号 / Intermediate symbol
     * @return 操作结果 / Operation result
     */
    fun add(symbol: IntermediateSymbol<*>): Try

    /**
     * 批量添加中间符号 / Batch add intermediate symbols
     *
     * @param symbols 中间符号集合 / Intermediate symbols
     * @return 操作结果 / Operation result
     */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addSymbols")
    fun add(symbols: Iterable<IntermediateSymbol<*>>): Try

    /**
     * 移除中间符号 / Remove intermediate symbol
     *
     * @param symbol 中间符号 / Intermediate symbol
     */
    fun remove(symbol: IntermediateSymbol<*>)

    /**
     * 移除中间符号（别名）/ Remove intermediate symbol (alias)
     *
     * @param symbol 中间符号 / Intermediate symbol
     */
    fun removeSymbol(symbol: IntermediateSymbol<*>) = remove(symbol)
}

/**
 * 不可变 token 表实现，封装已注册的 token 列表和符号集合。
 * Immutable token table implementation wrapping a registered token list and symbol collection.
 *
 * @param V 数值类型 / The number type
 * @property category 符号操作类别 / Symbol operation category
 * @property tokenList token 列表 / Token list
 * @property symbols 已注册的中间符号集合 / Registered intermediate symbol collection
 */
data class TokenTable<V>(
    override val category: Category,
    override val tokenList: AbstractTokenList<V>,
    override val symbols: List<IntermediateSymbol<*>>
) : AbstractTokenTable<V> where V : RealNumber<V>, V : NumberField<V> {
    /**
     * 从可变 token 表构造不可变副本 / Construct immutable copy from mutable token table
     *
     * @param tokenTable 可变 token 表 / Mutable token table
     */
    constructor(tokenTable: MutableTokenTable<V>) : this(
        category = tokenTable.category,
        tokenList = TokenList(tokenTable.tokenList as MutableTokenList<V>),
        symbols = tokenTable.symbols.toList()
    )

    override val tokens by tokenList::tokens

    private val cacheContexts = TokenCacheContexts<V>()

    override fun flush() {
        cacheContexts.clearAll()
    }

    override fun cached(cacheKey: Any, solution: List<V>?): Boolean {
        return cacheContexts.value.cached(cacheKey, solution)
    }

    override fun cached(cacheKey: Any, fixedValues: Map<Symbol, V>): Boolean {
        return cacheContexts.value.cached(cacheKey, fixedValues)
    }

    override fun cachedValue(cacheKey: Any, solution: List<V>?): V? {
        return cacheContexts.value.value(cacheKey, solution)
    }

    override fun cachedValue(cacheKey: Any, fixedValues: Map<Symbol, V>): V? {
        return cacheContexts.value.value(cacheKey, fixedValues)
    }

    override fun cache(cacheKey: Any, solution: List<V>?, value: V): V {
        return cacheContexts.value.put(cacheKey, solution, value)
    }

    override fun cache(cacheKey: Any, fixedValues: Map<Symbol, V>, value: V): V {
        return cacheContexts.value.put(cacheKey, fixedValues, value)
    }

    override fun cachedLinearFlatten(cacheKey: Any): Boolean {
        return cacheContexts.linearFlatten.contains(cacheKey)
    }

    override fun cachedLinearFlattenValue(cacheKey: Any): LinearFlattenData<V>? {
        return cacheContexts.linearFlatten.get(cacheKey)
    }

    override fun cacheLinearFlatten(cacheKey: Any, flatten: LinearFlattenData<V>?): LinearFlattenData<V>? {
        cacheContexts.linearFlatten.put(cacheKey, flatten)
        return flatten
    }

    override fun clearLinearFlatten(cacheKey: Any): LinearFlattenData<V>? {
        return cacheContexts.linearFlatten.remove(cacheKey)
    }

    override fun cachedQuadraticFlatten(cacheKey: Any): Boolean {
        return cacheContexts.quadraticFlatten.contains(cacheKey)
    }

    override fun cachedQuadraticFlattenValue(cacheKey: Any): QuadraticFlattenData<V>? {
        return cacheContexts.quadraticFlatten.get(cacheKey)
    }

    override fun cacheQuadraticFlatten(cacheKey: Any, flatten: QuadraticFlattenData<V>?): QuadraticFlattenData<V>? {
        cacheContexts.quadraticFlatten.put(cacheKey, flatten)
        return flatten
    }

    override fun clearQuadraticFlatten(cacheKey: Any): QuadraticFlattenData<V>? {
        return cacheContexts.quadraticFlatten.remove(cacheKey)
    }

    override fun cachedRange(cacheKey: Any): Boolean {
        return cacheContexts.range.contains(cacheKey)
    }

    override fun cachedRangeValue(cacheKey: Any): ExpressionRange<V>? {
        return cacheContexts.range.get(cacheKey)
    }

    override fun cacheRange(cacheKey: Any, range: ExpressionRange<V>?): ExpressionRange<V>? {
        cacheContexts.range.put(cacheKey, range)
        return range
    }

    override fun clearRange(cacheKey: Any): ExpressionRange<V>? {
        return cacheContexts.range.remove(cacheKey)
    }

    override fun clearValue(cacheKey: Any) {
        cacheContexts.value.remove(cacheKey)
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("cacheSymbols")
    override fun cache(symbols: Map<IntermediateSymbol<*>, V>, solution: List<V>?) {
        cacheContexts.value.putAll(symbols, solution)
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("cacheSymbols")
    override fun cache(symbols: Map<IntermediateSymbol<*>, V>, fixedValues: Map<Symbol, V>) {
        cacheContexts.value.putAll(symbols, fixedValues)
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("lazyCacheSymbols")
    override fun cache(symbols: Map<IntermediateSymbol<*>, () -> V?>, solution: List<V>?) {
        cacheContexts.value.putAllLazy(symbols, solution)
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("lazyCacheSymbols")
    override fun cache(symbols: Map<IntermediateSymbol<*>, () -> V?>, fixedValues: Map<Symbol, V>) {
        cacheContexts.value.putAllLazy(symbols, fixedValues)
    }

    override fun close() {
        val boundSymbols = cacheContexts.boundIntermediateSymbols() + symbols
        cacheContexts.clearAll()
        for (symbol in boundSymbols) {
            unbindTokenTableContext(symbol, this)
        }
        super.close()
    }
}

/**
 * 可变 token 表的密封基类，支持符号和变量的增删操作。
 * Sealed base class for mutable token tables, supporting add/remove of symbols and variables.
 *
 * @param V 数值类型 / The number type
 * @property category 符号操作类别 / Symbol operation category
 * @property tokenList 可变 token 列表 / Mutable token list
 * @property _symbols 已注册的中间符号列表 / Registered intermediate symbol list
 */
sealed class MutableTokenTable<V>(
    override val category: Category,
    override val tokenList: AbstractMutableTokenList<V>,
    protected val _symbols: MutableList<IntermediateSymbol<*>> = ArrayList()
) : AbstractMutableTokenTable<V> where V : RealNumber<V>, V : NumberField<V> {
    private val _symbolsMap: MutableMap<String, IntermediateSymbol<*>> = _symbols.associateBy { it.name }.toMutableMap()
    override val symbols by ::_symbols

    internal val cacheContexts = TokenCacheContexts<V>()

    private val _symbolDependencies: MutableMap<IntermediateSymbol<*>, MutableSet<IntermediateSymbol<*>>> = mutableMapOf()
    override val symbolDependencies: Map<IntermediateSymbol<*>, Set<IntermediateSymbol<*>>> get() = _symbolDependencies

    /**
     * 添加符号依赖关系 / Add symbol dependency
     *
     * @param symbol 依赖方符号 / Dependent symbol
     * @param dependsOn 被依赖的符号 / Symbol depended on
     */
    fun addSymbolDependency(symbol: IntermediateSymbol<*>, dependsOn: IntermediateSymbol<*>) {
        _symbolDependencies.getOrPut(symbol) { mutableSetOf() }.add(dependsOn)
    }

    /**
     * 批量添加符号依赖关系 / Batch add symbol dependencies
     *
     * @param symbol 依赖方符号 / Dependent symbol
     * @param dependencies 被依赖的符号集合 / Set of symbols depended on
     */
    fun addSymbolWithDependencies(symbol: IntermediateSymbol<*>, dependencies: Set<IntermediateSymbol<*>>) {
        _symbolDependencies.getOrPut(symbol) { mutableSetOf() }.addAll(dependencies)
    }

    /**
     * 验证符号依赖关系中无环 / Validate no cycles in symbol dependencies
     *
     * @return 是否无环 / Whether acyclic
     */
    fun validateNoCycles(): Boolean {
        val visited = mutableSetOf<IntermediateSymbol<*>>()
        val onStack = mutableSetOf<IntermediateSymbol<*>>()
        fun dfs(symbol: IntermediateSymbol<*>): Boolean {
            if (symbol in onStack) return false
            if (symbol in visited) return true
            visited.add(symbol)
            onStack.add(symbol)
            for (dep in _symbolDependencies[symbol] ?: emptySet()) {
                if (!dfs(dep)) return false
            }
            onStack.remove(symbol)
            return true
        }
        for (symbol in _symbolDependencies.keys) {
            if (symbol !in visited && !dfs(symbol)) return false
        }
        return true
    }

    override fun add(item: AbstractVariableItem<*, *>): Try {
        return tokenList.add(item)
    }

    @JvmName("addVariables")
    @Suppress("INAPPLICABLE_JVM_NAME")
    override fun add(items: Iterable<AbstractVariableItem<*, *>>): Try {
        return tokenList.add(items)
    }

    override fun remove(item: AbstractVariableItem<*, *>) {
        return tokenList.remove(item)
    }

    override fun add(symbol: IntermediateSymbol<*>): Try {
        if ((symbol.operationCategory ord category) is Order.Greater) {
            return Failed(
                code = ErrorCode.ApplicationError,
                message = "${symbol.name} over $category"
            )
        }

        if (_symbolsMap.containsKey(symbol.name)) {
            val value = RepeatedSymbolError(_symbolsMap[symbol.name]!!, symbol)
            return Failed(
                ExErr(
                    code = ErrorCode.SymbolRepetitive,
                    message = value.message,
                    value = value
                )
            )
        } else {
            symbols.add(symbol)
            _symbolsMap[symbol.name] = symbol
        }
        return ok
    }

    @JvmName("addSymbols")
    @Suppress("INAPPLICABLE_JVM_NAME")
    override fun add(symbols: Iterable<IntermediateSymbol<*>>): Try {
        for (symbol in symbols) {
            when (val result = add(symbol)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
            }
        }
        return ok
    }

    override fun remove(symbol: IntermediateSymbol<*>) {
        _symbols.remove(symbol)
        _symbolsMap.remove(symbol.name)

        // B1: 解绑并清理缓存
        // B1: Unbind and clear caches for removed symbol
        unbindTokenTableContext(symbol, this)
        cacheContexts.linearFlatten.remove(symbol)
        cacheContexts.quadraticFlatten.remove(symbol)
        cacheContexts.range.remove(symbol)
        cacheContexts.value.remove(symbol)
        _symbolDependencies.remove(symbol)
        _symbolDependencies.values.forEach { it.remove(symbol) }
    }

    override fun flush() {
        tokenList.flush()
        cacheContexts.clearAll()
    }

    override fun cached(cacheKey: Any, solution: List<V>?): Boolean {
        return cacheContexts.value.cached(cacheKey, solution)
    }

    override fun cached(cacheKey: Any, fixedValues: Map<Symbol, V>): Boolean? {
        return cacheContexts.value.cached(cacheKey, fixedValues)
    }

    override fun cachedValue(cacheKey: Any, solution: List<V>?): V? {
        return cacheContexts.value.value(cacheKey, solution)
    }

    override fun cachedValue(cacheKey: Any, fixedValues: Map<Symbol, V>): V? {
        return cacheContexts.value.value(cacheKey, fixedValues)
    }

    override fun cache(cacheKey: Any, solution: List<V>?, value: V): V {
        return cacheContexts.value.put(cacheKey, solution, value)
    }

    override fun cache(cacheKey: Any, fixedValues: Map<Symbol, V>, value: V): V {
        return cacheContexts.value.put(cacheKey, fixedValues, value)
    }

    override fun cachedLinearFlatten(cacheKey: Any): Boolean {
        return cacheContexts.linearFlatten.contains(cacheKey)
    }

    override fun cachedLinearFlattenValue(cacheKey: Any): LinearFlattenData<V>? {
        return cacheContexts.linearFlatten.get(cacheKey)
    }

    override fun cacheLinearFlatten(cacheKey: Any, flatten: LinearFlattenData<V>?): LinearFlattenData<V>? {
        cacheContexts.linearFlatten.put(cacheKey, flatten)
        return flatten
    }

    override fun clearLinearFlatten(cacheKey: Any): LinearFlattenData<V>? {
        return cacheContexts.linearFlatten.remove(cacheKey)
    }

    override fun cachedQuadraticFlatten(cacheKey: Any): Boolean {
        return cacheContexts.quadraticFlatten.contains(cacheKey)
    }

    override fun cachedQuadraticFlattenValue(cacheKey: Any): QuadraticFlattenData<V>? {
        return cacheContexts.quadraticFlatten.get(cacheKey)
    }

    override fun cacheQuadraticFlatten(cacheKey: Any, flatten: QuadraticFlattenData<V>?): QuadraticFlattenData<V>? {
        cacheContexts.quadraticFlatten.put(cacheKey, flatten)
        return flatten
    }

    override fun clearQuadraticFlatten(cacheKey: Any): QuadraticFlattenData<V>? {
        return cacheContexts.quadraticFlatten.remove(cacheKey)
    }

    override fun cachedRange(cacheKey: Any): Boolean {
        return cacheContexts.range.contains(cacheKey)
    }

    override fun cachedRangeValue(cacheKey: Any): ExpressionRange<V>? {
        return cacheContexts.range.get(cacheKey)
    }

    override fun cacheRange(cacheKey: Any, range: ExpressionRange<V>?): ExpressionRange<V>? {
        cacheContexts.range.put(cacheKey, range)
        return range
    }

    override fun clearRange(cacheKey: Any): ExpressionRange<V>? {
        return cacheContexts.range.remove(cacheKey)
    }

    override fun clearValue(cacheKey: Any) {
        cacheContexts.value.remove(cacheKey)
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("cacheSymbols")
    override fun cache(symbols: Map<IntermediateSymbol<*>, V>, solution: List<V>?) {
        cacheContexts.value.putAll(symbols, solution)
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("cacheSymbols")
    override fun cache(symbols: Map<IntermediateSymbol<*>, V>, fixedValues: Map<Symbol, V>) {
        cacheContexts.value.putAll(symbols, fixedValues)
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("lazyCacheSymbols")
    override fun cache(symbols: Map<IntermediateSymbol<*>, () -> V?>, solution: List<V>?) {
        cacheContexts.value.putAllLazy(symbols, solution)
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("lazyCacheSymbols")
    override fun cache(symbols: Map<IntermediateSymbol<*>, () -> V?>, fixedValues: Map<Symbol, V>) {
        cacheContexts.value.putAllLazy(symbols, fixedValues)
    }

    override fun close() {
        val boundSymbols = cacheContexts.boundIntermediateSymbols() + symbols
        cacheContexts.clearAll()
        for (symbol in boundSymbols) {
            unbindTokenTableContext(symbol, this)
        }
        _symbolsMap.clear()
        symbols.clear()
        _symbolDependencies.clear()
        super.close()
    }
}

/**
 * 自动 token 表，变量缺失时自动创建 token。
 * Auto token table that creates tokens on-the-fly for missing variables.
 *
 * @param V 数值类型 / The number type
 * @param category 符号操作类别 / Symbol operation category
 * @property checkTokenExists 是否检查 token 已存在 / Whether to check token existence
 * @property _tokenList 自动 token 列表 / Auto token list
 */
class AutoTokenTable<V>(
    category: Category,
    private val checkTokenExists: Boolean,
    private val _tokenList: AutoTokenList<V>,
    _symbols: MutableList<IntermediateSymbol<*>> = ArrayList()
) : MutableTokenTable<V>(category, _tokenList, _symbols) where V : RealNumber<V>, V : NumberField<V> {
    /**
     * 通过类别和检查标志构造 / Construct by category and check flag
     *
     * @param category 符号操作类别 / Symbol operation category
     * @param checkTokenExists 是否检查 token 已存在 / Whether to check token existence
     */
    constructor(category: Category, checkTokenExists: Boolean) : this(
        category = category,
        checkTokenExists = checkTokenExists,
        _tokenList = AutoTokenList(checkTokenExists)
    )

    /** @return 副本 / Copy */
    override fun copy(): MutableTokenTable<V> {
        return AutoTokenTable(
            category = category,
            checkTokenExists = checkTokenExists,
            _tokenList = _tokenList.copy() as AutoTokenList<V>,
            _symbols = _symbols.toMutableList()
        )
    }
}

/**
 * 手动 token 表，变量需显式添加后才能使用。
 * Manual token table where variables must be explicitly added before use.
 *
 * @param V 数值类型 / The number type
 * @param category 符号操作类别 / Symbol operation category
 * @property checkTokenExists 是否检查 token 已存在 / Whether to check token existence
 * @property _tokenList 手动 token 列表 / Manual token list
 */
class ManualTokenTable<V>(
    category: Category,
    private val checkTokenExists: Boolean,
    private val _tokenList: ManualTokenList<V>,
    _symbols: MutableList<IntermediateSymbol<*>> = ArrayList()
) : MutableTokenTable<V>(category, _tokenList, _symbols) where V : RealNumber<V>, V : NumberField<V> {
    /**
     * 通过类别和检查标志构造 / Construct by category and check flag
     *
     * @param category 符号操作类别 / Symbol operation category
     * @param checkTokenExists 是否检查 token 已存在 / Whether to check token existence
     */
    constructor(category: Category, checkTokenExists: Boolean = System.getProperty("env", "prod") != "prod") : this(
        category = category,
        checkTokenExists = checkTokenExists,
        _tokenList = ManualTokenList(checkTokenExists)
    )

    /** @return 副本 / Copy */
    override fun copy(): MutableTokenTable<V> {
        return ManualTokenTable(
            category = category,
            checkTokenExists = checkTokenExists,
            _tokenList = _tokenList.copy() as ManualTokenList<V>,
            _symbols = _symbols.toMutableList()
        )
    }
}
