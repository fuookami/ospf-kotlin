package fuookami.ospf.kotlin.core.frontend.model.mechanism

import fuookami.ospf.kotlin.core.frontend.expression.ExpressionRange
import fuookami.ospf.kotlin.core.frontend.expression.monomial.LinearMonomialCell
import fuookami.ospf.kotlin.core.frontend.expression.monomial.QuadraticMonomialCell
import fuookami.ospf.kotlin.core.frontend.expression.symbol.IntermediateSymbol
import fuookami.ospf.kotlin.core.frontend.variable.AbstractVariableItem
import fuookami.ospf.kotlin.utils.math.algebra.number.Flt64
import fuookami.ospf.kotlin.utils.math.symbol.Symbol
import fuookami.ospf.kotlin.utils.math.symbol.monomial.LinearMonomial as UtilsLinearMonomial
import fuookami.ospf.kotlin.utils.math.symbol.monomial.QuadraticMonomial as UtilsQuadraticMonomial
import java.util.Collections
import java.util.WeakHashMap

data class LinearFlattenData(
    val monomials: List<UtilsLinearMonomial<Flt64>>,
    val constant: Flt64
)

data class QuadraticFlattenData(
    val monomials: List<UtilsQuadraticMonomial<Flt64>>,
    val constant: Flt64
)

class LinearFlattenContext(
    private val cache: MutableMap<Any, LinearFlattenData?> = HashMap()
) {
    fun contains(cacheKey: Any): Boolean {
        return cache.containsKey(cacheKey)
    }

    fun get(cacheKey: Any): LinearFlattenData? {
        return cache[cacheKey]
    }

    fun put(cacheKey: Any, value: LinearFlattenData?) {
        cache[cacheKey] = value
    }

    fun remove(cacheKey: Any): LinearFlattenData? {
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
    private val cache: MutableMap<Any, QuadraticFlattenData?> = HashMap()
) {
    fun contains(cacheKey: Any): Boolean {
        return cache.containsKey(cacheKey)
    }

    fun get(cacheKey: Any): QuadraticFlattenData? {
        return cache[cacheKey]
    }

    fun put(cacheKey: Any, value: QuadraticFlattenData?) {
        cache[cacheKey] = value
    }

    fun remove(cacheKey: Any): QuadraticFlattenData? {
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

    fun boundIntermediateSymbols(): Set<IntermediateSymbol> {
        return boundSymbols().mapNotNull { it as? IntermediateSymbol }.toSet()
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
    WeakHashMap<IntermediateSymbol, AbstractTokenTable>()
)

internal fun bindTokenTableContext(symbol: IntermediateSymbol, tokenTable: AbstractTokenTable) {
    symbolTokenTableContext[symbol] = tokenTable
}

internal fun unbindTokenTableContext(symbol: IntermediateSymbol, tokenTable: AbstractTokenTable) {
    if (symbolTokenTableContext[symbol] == tokenTable) {
        symbolTokenTableContext.remove(symbol)
    }
}

internal fun boundTokenTableContext(symbol: IntermediateSymbol): AbstractTokenTable? {
    return symbolTokenTableContext[symbol]
}

internal fun List<LinearMonomialCell>.toLinearFlattenData(): LinearFlattenData {
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
    return LinearFlattenData(
        monomials = monomials,
        constant = constant
    )
}

internal fun LinearFlattenData.toLinearMonomialCells(): List<LinearMonomialCell> {
    val cells = monomials.map {
        LinearMonomialCell(
            coefficient = it.coefficient,
            variable = it.symbol as AbstractVariableItem<*, *>
        )
    }.toMutableList()
    if (constant != Flt64.zero) {
        cells.add(LinearMonomialCell(constant))
    }
    return cells
}

internal fun List<QuadraticMonomialCell>.toQuadraticFlattenData(): QuadraticFlattenData {
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
    return QuadraticFlattenData(
        monomials = monomials,
        constant = constant
    )
}

internal fun QuadraticFlattenData.toQuadraticMonomialCells(): List<QuadraticMonomialCell> {
    val cells = monomials.map {
        QuadraticMonomialCell(
            coefficient = it.coefficient,
            variable1 = it.symbol1 as AbstractVariableItem<*, *>,
            variable2 = it.symbol2 as AbstractVariableItem<*, *>?
        )
    }.toMutableList()
    if (constant != Flt64.zero) {
        cells.add(QuadraticMonomialCell(constant))
    }
    return cells
}
