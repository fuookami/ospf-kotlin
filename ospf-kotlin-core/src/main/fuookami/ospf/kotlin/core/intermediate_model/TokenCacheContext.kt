@file:Suppress("DEPRECATION")

package fuookami.ospf.kotlin.core.intermediate_model

import fuookami.ospf.kotlin.core.intermediate_model.ExpressionRange
import fuookami.ospf.kotlin.core.intermediate_model.monomial.LinearMonomial
import fuookami.ospf.kotlin.core.intermediate_model.monomial.LinearMonomialCellF64
import fuookami.ospf.kotlin.core.intermediate_model.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.core.intermediate_model.monomial.QuadraticMonomialCellF64
import fuookami.ospf.kotlin.core.intermediate_symbol.IntermediateSymbol
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial as UtilsLinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial as UtilsQuadraticMonomial
import java.util.Collections
import java.util.WeakHashMap

/**
 * Generic linear flatten data - monomials + constant.
 * T is the numeric type (e.g., Flt64).
 */
data class LinearFlattenData<T : RealNumber<T>>(
    val monomials: List<UtilsLinearMonomial<T>>,
    val constant: T
)

/**
 * Legacy typealias for Flt64-specific LinearFlattenDataF64.
 */
typealias LinearFlattenDataF64 = LinearFlattenData<Flt64>

/**
 * Generic quadratic flatten data - monomials + constant.
 * T is the numeric type (e.g., Flt64).
 */
data class QuadraticFlattenData<T : RealNumber<T>>(
    val monomials: List<UtilsQuadraticMonomial<T>>,
    val constant: T
)

/**
 * Legacy typealias for Flt64-specific QuadraticFlattenDataF64.
 */
typealias QuadraticFlattenDataF64 = QuadraticFlattenData<Flt64>

class LinearFlattenContext(
    private val cache: MutableMap<Any, LinearFlattenDataF64?> = HashMap()
) {
    fun contains(cacheKey: Any): Boolean {
        return cache.containsKey(cacheKey)
    }

    fun get(cacheKey: Any): LinearFlattenDataF64? {
        return cache[cacheKey]
    }

    fun put(cacheKey: Any, value: LinearFlattenDataF64?) {
        cache[cacheKey] = value
    }

    fun remove(cacheKey: Any): LinearFlattenDataF64? {
        return cache.remove(cacheKey)
    }

    fun keys(): Set<Any> {
        return cache.keys.toSet()
    }

    fun clear() {
        cache.clear()
    }
}

class QuadraticFlattenContext(
    private val cache: MutableMap<Any, QuadraticFlattenDataF64?> = HashMap()
) {
    fun contains(cacheKey: Any): Boolean {
        return cache.containsKey(cacheKey)
    }

    fun get(cacheKey: Any): QuadraticFlattenDataF64? {
        return cache[cacheKey]
    }

    fun put(cacheKey: Any, value: QuadraticFlattenDataF64?) {
        cache[cacheKey] = value
    }

    fun remove(cacheKey: Any): QuadraticFlattenDataF64? {
        return cache.remove(cacheKey)
    }

    fun keys(): Set<Any> {
        return cache.keys.toSet()
    }

    fun clear() {
        cache.clear()
    }
}

class ValueCacheContext(
    private val solutionCache: MutableMap<Pair<Any, List<Flt64>?>, Flt64?> = HashMap(),
    private val fixedValueCache: MutableMap<Pair<Any, Map<Symbol, Flt64>>, Flt64?> = HashMap()
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

    fun value(cacheKey: Any, solution: List<Flt64>? = null): Flt64? {
        return solutionCache[cacheKey to solution]
    }

    fun value(cacheKey: Any, fixedValues: Map<Symbol, Flt64>): Flt64? {
        return fixedValueCache[cacheKey to fixedValues]
    }

    fun put(cacheKey: Any, solution: List<Flt64>? = null, value: Flt64): Flt64 {
        solutionCache[cacheKey to solution] = value
        return value
    }

    fun put(cacheKey: Any, fixedValues: Map<Symbol, Flt64>, value: Flt64): Flt64 {
        fixedValueCache[cacheKey to fixedValues] = value
        return value
    }

    fun putAll(symbols: Map<out Any, Flt64>, solution: List<Flt64>? = null) {
        solutionCache.putAll(symbols.map { (cacheKey, value) ->
            (cacheKey to solution) to value
        })
    }

    fun putAll(symbols: Map<out Any, Flt64>, fixedValues: Map<Symbol, Flt64>) {
        fixedValueCache.putAll(symbols.map { (cacheKey, value) ->
            (cacheKey to fixedValues) to value
        })
    }

    fun putAllLazy(symbols: Map<out Any, () -> Flt64?>, solution: List<Flt64>? = null) {
        solutionCache.putAll(symbols.mapNotNull { (cacheKey, value) ->
            value()?.let {
                (cacheKey to solution) to it
            }
        })
    }

    fun putAllLazy(symbols: Map<out Any, () -> Flt64?>, fixedValues: Map<Symbol, Flt64>) {
        fixedValueCache.putAll(symbols.mapNotNull { (cacheKey, value) ->
            value()?.let {
                (cacheKey to fixedValues) to it
            }
        })
    }

    fun getOrPut(cacheKey: Any, solution: List<Flt64>? = null, value: () -> Flt64?): Flt64? {
        var cachedValue = solutionCache[cacheKey to solution]
        if (cachedValue == null) {
            value()?.let {
                cachedValue = it
                solutionCache[cacheKey to solution] = it
            }
        }
        return cachedValue
    }

    fun getOrPut(cacheKey: Any, fixedValues: Map<Symbol, Flt64>, value: () -> Flt64?): Flt64? {
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
     * Remove all cache entries for a given cacheKey.
     * 清除指定 cacheKey 的所有缓存条目。
     *
     * This removes entries from both solutionCache and fixedValueCache
     * where the first element of the pair matches the cacheKey.
     */
    fun remove(cacheKey: Any) {
        solutionCache.keys.removeAll { it.first == cacheKey }
        fixedValueCache.keys.removeAll { it.first == cacheKey }
    }
}

class RangeCacheContext(
    private val cache: MutableMap<Any, ExpressionRange<Flt64>?> = HashMap()
) {
    fun contains(cacheKey: Any): Boolean {
        return cache.containsKey(cacheKey)
    }

    fun get(cacheKey: Any): ExpressionRange<Flt64>? {
        return cache[cacheKey]
    }

    fun put(cacheKey: Any, value: ExpressionRange<Flt64>?) {
        cache[cacheKey] = value
    }

    fun remove(cacheKey: Any): ExpressionRange<Flt64>? {
        return cache.remove(cacheKey)
    }

    fun keys(): Set<Any> {
        return cache.keys.toSet()
    }

    fun clear() {
        cache.clear()
    }
}

data class TokenCacheContexts(
    val linearFlatten: LinearFlattenContext = LinearFlattenContext(),
    val quadraticFlatten: QuadraticFlattenContext = QuadraticFlattenContext(),
    val value: ValueCacheContext = ValueCacheContext(),
    val range: RangeCacheContext = RangeCacheContext()
) {
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
    WeakHashMap<IntermediateSymbol<*>, LegacyAbstractTokenTable>()
)

internal fun bindTokenTableContext(symbol: IntermediateSymbol<*>, tokenTable: LegacyAbstractTokenTable) {
    val oldTokenTable = symbolTokenTableContext[symbol]
    if (oldTokenTable != null && oldTokenTable != tokenTable) {
        oldTokenTable.clearLinearFlatten(symbol)
        oldTokenTable.clearQuadraticFlatten(symbol)
        oldTokenTable.clearRange(symbol)
        oldTokenTable.clearValue(symbol)
    }
    symbolTokenTableContext[symbol] = tokenTable
}

internal fun unbindTokenTableContext(symbol: IntermediateSymbol<*>, tokenTable: LegacyAbstractTokenTable) {
    if (symbolTokenTableContext[symbol] == tokenTable) {
        symbolTokenTableContext.remove(symbol)
    }
}

internal fun boundTokenTableContext(symbol: IntermediateSymbol<*>): LegacyAbstractTokenTable? {
    return symbolTokenTableContext[symbol]
}

/**
 * 将旧 CellF64 列表转换为 LinearFlattenDataF64。
 * 已废弃：调用方应直接使用 FlattenData 主路径，而非通过 CellF64 中转。
 */
@Deprecated(
    message = "Use LinearFlattenDataF64 directly. CellF64-based conversion will be removed in M9.",
    level = DeprecationLevel.WARNING,
    replaceWith = ReplaceWith("polynomial.flattenedMonomials", "fuookami.ospf.kotlin.core.intermediate_model.LinearFlattenDataF64")
)
internal fun List<LinearMonomialCellF64>.toLinearFlattenData(): LinearFlattenDataF64 {
    var constant = Flt64.zero
    val monomials = mapNotNull { cell ->
        if (cell.isConstant) {
            constant += cell.constant!!
            null
        } else {
            UtilsLinearMonomial(
                coefficient = cell.pair!!.coefficient,
                symbol = cell.pair!!.variable
            )
        }
    }
    return LinearFlattenDataF64(
        monomials = monomials,
        constant = constant
    )
}

/**
 * 将 LinearFlattenDataF64 转换为旧 CellF64 列表。
 * 已废弃：仅用于 deprecated `cells` 属性的兼容层。
 */
@Deprecated(
    message = "Only for deprecated cells property compatibility. Will be removed in M9.",
    level = DeprecationLevel.WARNING
)
internal fun LinearFlattenDataF64.toLinearMonomialCells(): List<LinearMonomialCellF64> {
    val cells = monomials.map { m ->
        LinearMonomialCellF64.invoke<Flt64>(m.coefficient, m.symbol as AbstractVariableItem<*, *>)
    }.toMutableList()
    if (constant != Flt64.zero) {
        cells.add(LinearMonomialCellF64.invoke<Flt64>(constant))
    }
    return cells
}

/**
 * 将 LinearFlattenDataF64 转换为 QuadraticFlattenDataF64。
 * 用于 Linear → Quadratic 升级场景。
 */
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

/**
 * 将旧 QuadraticMonomialCellF64 列表转换为 QuadraticFlattenDataF64。
 * 已废弃：调用方应直接使用 FlattenData 主路径。
 */
@Deprecated(
    message = "Use QuadraticFlattenDataF64 directly. CellF64-based conversion will be removed in M9.",
    level = DeprecationLevel.WARNING,
    replaceWith = ReplaceWith("polynomial.flattenedMonomials", "fuookami.ospf.kotlin.core.intermediate_model.QuadraticFlattenDataF64")
)
internal fun List<QuadraticMonomialCellF64>.toQuadraticFlattenData(): QuadraticFlattenDataF64 {
    var constant = Flt64.zero
    val monomials = mapNotNull { cell ->
        if (cell.isConstant) {
            constant += cell.constant!!
            null
        } else {
            UtilsQuadraticMonomial(
                coefficient = cell.triple!!.coefficient,
                symbol1 = cell.triple!!.variable1,
                symbol2 = cell.triple!!.variable2
            )
        }
    }
    return QuadraticFlattenDataF64(
        monomials = monomials,
        constant = constant
    )
}

/**
 * 将 QuadraticFlattenDataF64 转换为旧 CellF64 列表。
 * 已废弃：仅用于 deprecated `cells` 属性的兼容层。
 */
@Deprecated(
    message = "Only for deprecated cells property compatibility. Will be removed in M9.",
    level = DeprecationLevel.WARNING
)
internal fun QuadraticFlattenDataF64.toQuadraticMonomialCells(): List<QuadraticMonomialCellF64> {
    val cells = monomials.map {
        QuadraticMonomialCellF64.invoke<Flt64>(
            coefficient = it.coefficient,
            variable1 = it.symbol1 as AbstractVariableItem<*, *>,
            variable2 = it.symbol2 as AbstractVariableItem<*, *>?
        )
    }.toMutableList()
    if (constant != Flt64.zero) {
        cells.add(QuadraticMonomialCellF64.invoke<Flt64>(constant))
    }
    return cells
}
