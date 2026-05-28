/**
 * Token 缓存上下文，管理 flatten/value/range 缓存及符号绑定关系。
 * Token cache contexts managing flatten/value/range caches and symbol binding relationships.
 */
@file:Suppress("DEPRECATION")
package fuookami.ospf.kotlin.core.token

import java.util.Collections
import java.util.WeakHashMap
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.core.model.basic.ExpressionRange
import fuookami.ospf.kotlin.core.symbol.IntermediateSymbol
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem

/**
 * 线性展开缓存数据。
 * Linear flatten cache data.
 *
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
 */
class LinearFlattenContext<V : Ring<V>>(
    private val cache: MutableMap<Any, LinearFlattenData<V>?> = HashMap()
) {
    fun contains(cacheKey: Any): Boolean {
        return cache.containsKey(cacheKey)
    }

    fun get(cacheKey: Any): LinearFlattenData<V>? {
        return cache[cacheKey]
    }

    fun put(cacheKey: Any, value: LinearFlattenData<V>?) {
        cache[cacheKey] = value
    }

    fun remove(cacheKey: Any): LinearFlattenData<V>? {
        return cache.remove(cacheKey)
    }

    fun keys(): Set<Any> {
        return cache.keys.toSet()
    }

    fun clear() {
        cache.clear()
    }
}

/**
 * 二次展开缓存上下文。
 * Quadratic flatten cache context.
 */
class QuadraticFlattenContext<V : Ring<V>>(
    private val cache: MutableMap<Any, QuadraticFlattenData<V>?> = HashMap()
) {
    fun contains(cacheKey: Any): Boolean {
        return cache.containsKey(cacheKey)
    }

    fun get(cacheKey: Any): QuadraticFlattenData<V>? {
        return cache[cacheKey]
    }

    fun put(cacheKey: Any, value: QuadraticFlattenData<V>?) {
        cache[cacheKey] = value
    }

    fun remove(cacheKey: Any): QuadraticFlattenData<V>? {
        return cache.remove(cacheKey)
    }

    fun keys(): Set<Any> {
        return cache.keys.toSet()
    }

    fun clear() {
        cache.clear()
    }
}

/**
 * 值缓存上下文，按 solution 或 fixedValues 维度分别缓存求解结果。
 * Value cache context, caching solve results separately by solution and fixedValues dimensions.
 */
class ValueCacheContext<V : RealNumber<V>>(
    private val solutionCache: MutableMap<Pair<Any, List<V>?>, V?> = HashMap(),
    private val fixedValueCache: MutableMap<Pair<Any, Map<Symbol, V>>, V?> = HashMap()
) {
    fun clear() {
        solutionCache.clear()
        fixedValueCache.clear()
    }

    fun cached(cacheKey: Any, solution: List<V>? = null): Boolean {
        return solutionCache.containsKey(cacheKey to solution)
    }

    fun cached(cacheKey: Any, fixedValues: Map<Symbol, V>): Boolean {
        return fixedValueCache.containsKey(cacheKey to fixedValues)
    }

    fun value(cacheKey: Any, solution: List<V>? = null): V? {
        return solutionCache[cacheKey to solution]
    }

    fun value(cacheKey: Any, fixedValues: Map<Symbol, V>): V? {
        return fixedValueCache[cacheKey to fixedValues]
    }

    fun put(cacheKey: Any, solution: List<V>? = null, value: V): V {
        solutionCache[cacheKey to solution] = value
        return value
    }

    fun put(cacheKey: Any, fixedValues: Map<Symbol, V>, value: V): V {
        fixedValueCache[cacheKey to fixedValues] = value
        return value
    }

    fun putAll(symbols: Map<out Any, V>, solution: List<V>? = null) {
        solutionCache.putAll(symbols.map { (cacheKey, value) ->
            (cacheKey to solution) to value
        })
    }

    fun putAll(symbols: Map<out Any, V>, fixedValues: Map<Symbol, V>) {
        fixedValueCache.putAll(symbols.map { (cacheKey, value) ->
            (cacheKey to fixedValues) to value
        })
    }

    fun putAllLazy(symbols: Map<out Any, () -> V?>, solution: List<V>? = null) {
        solutionCache.putAll(symbols.mapNotNull { (cacheKey, value) ->
            value()?.let {
                (cacheKey to solution) to it
            }
        })
    }

    fun putAllLazy(symbols: Map<out Any, () -> V?>, fixedValues: Map<Symbol, V>) {
        fixedValueCache.putAll(symbols.mapNotNull { (cacheKey, value) ->
            value()?.let {
                (cacheKey to fixedValues) to it
            }
        })
    }

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

    fun remove(cacheKey: Any) {
        solutionCache.keys.removeAll { it.first == cacheKey }
        fixedValueCache.keys.removeAll { it.first == cacheKey }
    }
}

/**
 * 范围缓存上下文。
 * Range cache context.
 */
class RangeCacheContext<V>(
    private val cache: MutableMap<Any, ExpressionRange<V>?> = HashMap()
) where V : RealNumber<V>, V : NumberField<V> {
    fun contains(cacheKey: Any): Boolean {
        return cache.containsKey(cacheKey)
    }

    fun get(cacheKey: Any): ExpressionRange<V>? {
        return cache[cacheKey]
    }

    fun put(cacheKey: Any, value: ExpressionRange<V>?) {
        cache[cacheKey] = value
    }

    fun remove(cacheKey: Any): ExpressionRange<V>? {
        return cache.remove(cacheKey)
    }

    fun keys(): Set<Any> {
        return cache.keys.toSet()
    }

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
    fun boundSymbols(): Set<Any> {
        return linearFlatten.keys() + quadraticFlatten.keys() + range.keys()
    }

    fun boundIntermediateSymbols(): Set<IntermediateSymbol<*>> {
        return boundSymbols().mapNotNull { it as? IntermediateSymbol<*> }.toSet()
    }

    fun clearLinearFlatten() {
        linearFlatten.clear()
    }

    fun clearQuadraticFlatten() {
        quadraticFlatten.clear()
    }

    fun clearFlatten() {
        clearLinearFlatten()
        clearQuadraticFlatten()
    }

    fun clearValue() {
        value.clear()
    }

    fun clearRange() {
        range.clear()
    }

    fun clearAll() {
        clearFlatten()
        clearValue()
        clearRange()
    }
}

private val symbolTokenTableContext = Collections.synchronizedMap(
    WeakHashMap<IntermediateSymbol<*>, AbstractTokenTable<*>>()
)

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

internal fun <V> unbindTokenTableContext(symbol: IntermediateSymbol<*>, tokenTable: AbstractTokenTable<V>) where V : RealNumber<V>, V : NumberField<V> {
    if (symbolTokenTableContext[symbol] == tokenTable) {
        symbolTokenTableContext.remove(symbol)
    }
}

internal fun boundTokenTableContext(symbol: IntermediateSymbol<*>): AbstractTokenTable<*>? {
    return symbolTokenTableContext[symbol]
}

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
