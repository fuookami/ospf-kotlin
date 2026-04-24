@file:Suppress("DEPRECATION")

package fuookami.ospf.kotlin.core.token

import fuookami.ospf.kotlin.core.model.basic.ExpressionRange
import fuookami.ospf.kotlin.core.intermediate_symbol.IntermediateSymbol
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.concept.Ring
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial as UtilsLinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial as UtilsQuadraticMonomial
import java.util.Collections
import java.util.WeakHashMap

data class LinearFlattenData<T : Ring<T>>(
    val monomials: List<UtilsLinearMonomial<T>>,
    val constant: T
)

typealias LinearFlattenDataF64 = LinearFlattenData<Flt64>

data class QuadraticFlattenData<T : Ring<T>>(
    val monomials: List<UtilsQuadraticMonomial<T>>,
    val constant: T
)

typealias QuadraticFlattenDataF64 = QuadraticFlattenData<Flt64>

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

typealias LinearFlattenContextF64 = LinearFlattenContext<Flt64>

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

typealias QuadraticFlattenContextF64 = QuadraticFlattenContext<Flt64>

class ValueCacheContext<V : RealNumber<V>>(
    private val solutionCache: MutableMap<Pair<Any, List<Flt64>?>, V?> = HashMap(),
    private val fixedValueCache: MutableMap<Pair<Any, Map<Symbol, Flt64>>, V?> = HashMap()
) {
    fun clear() {
        solutionCache.clear()
        fixedValueCache.clear()
    }

    fun cached(cacheKey: Any, solution: List<Flt64>? = null): Boolean {
        return solutionCache.containsKey(cacheKey to solution)
    }

    fun cached(cacheKey: Any, fixedValues: Map<Symbol, Flt64>): Boolean {
        return fixedValueCache.containsKey(cacheKey to fixedValues)
    }

    fun value(cacheKey: Any, solution: List<Flt64>? = null): V? {
        return solutionCache[cacheKey to solution]
    }

    fun value(cacheKey: Any, fixedValues: Map<Symbol, Flt64>): V? {
        return fixedValueCache[cacheKey to fixedValues]
    }

    fun put(cacheKey: Any, solution: List<Flt64>? = null, value: V): V {
        solutionCache[cacheKey to solution] = value
        return value
    }

    fun put(cacheKey: Any, fixedValues: Map<Symbol, Flt64>, value: V): V {
        fixedValueCache[cacheKey to fixedValues] = value
        return value
    }

    fun putAll(symbols: Map<out Any, V>, solution: List<Flt64>? = null) {
        solutionCache.putAll(symbols.map { (cacheKey, value) ->
            (cacheKey to solution) to value
        })
    }

    fun putAll(symbols: Map<out Any, V>, fixedValues: Map<Symbol, Flt64>) {
        fixedValueCache.putAll(symbols.map { (cacheKey, value) ->
            (cacheKey to fixedValues) to value
        })
    }

    fun putAllLazy(symbols: Map<out Any, () -> V?>, solution: List<Flt64>? = null) {
        solutionCache.putAll(symbols.mapNotNull { (cacheKey, value) ->
            value()?.let {
                (cacheKey to solution) to it
            }
        })
    }

    fun putAllLazy(symbols: Map<out Any, () -> V?>, fixedValues: Map<Symbol, Flt64>) {
        fixedValueCache.putAll(symbols.mapNotNull { (cacheKey, value) ->
            value()?.let {
                (cacheKey to fixedValues) to it
            }
        })
    }

    fun getOrPut(cacheKey: Any, solution: List<Flt64>? = null, value: () -> V?): V? {
        var cachedValue = solutionCache[cacheKey to solution]
        if (cachedValue == null) {
            value()?.let {
                cachedValue = it
                solutionCache[cacheKey to solution] = it
            }
        }
        return cachedValue
    }

    fun getOrPut(cacheKey: Any, fixedValues: Map<Symbol, Flt64>, value: () -> V?): V? {
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

typealias ValueCacheContextF64 = ValueCacheContext<Flt64>

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

typealias RangeCacheContextF64 = RangeCacheContext<Flt64>

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

typealias TokenCacheContextsF64 = TokenCacheContexts<Flt64>

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

internal fun LinearFlattenDataF64.toQuadraticFlattenData(): QuadraticFlattenDataF64 {
    val monomials = this.monomials.map {
        UtilsQuadraticMonomial(
            coefficient = it.coefficient,
            symbol1 = it.symbol as AbstractVariableItem<*, *>,
            symbol2 = null
        )
    }
    return QuadraticFlattenDataF64(
        monomials = monomials,
        constant = this.constant
    )
}
