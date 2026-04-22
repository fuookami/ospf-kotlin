package fuookami.ospf.kotlin.core.intermediate_model

import fuookami.ospf.kotlin.core.intermediate_symbol.LinearExpressionSymbol
import fuookami.ospf.kotlin.core.intermediate_symbol.FunctionSymbol
import fuookami.ospf.kotlin.core.intermediate_symbol.FunctionSymbolRegistrationScope
import fuookami.ospf.kotlin.core.intermediate_symbol.IntermediateSymbol
import fuookami.ospf.kotlin.core.intermediate_symbol.LinearIntermediateSymbol
import fuookami.ospf.kotlin.core.intermediate_symbol.QuadraticIntermediateSymbol
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.utils.concept.Copyable
import fuookami.ospf.kotlin.utils.error.Err
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.error.ExErr
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.symbol.Category
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.ord
import fuookami.ospf.kotlin.math.usize
import fuookami.ospf.kotlin.utils.memoryUseOver
import fuookami.ospf.kotlin.utils.functional.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

class RepeatedSymbolError(
    val repeatedSymbol: IntermediateSymbol<*>,
    val symbol: IntermediateSymbol<*>
) : Throwable() {
    override val message get() = "Repeated \"${symbol.name}\", old: $repeatedSymbol, new: $symbol."
}

interface AbstractTokenTable<V> : AutoCloseable where V : RealNumber<V>, V : NumberField<V> {
    val category: Category
    val tokenList: AbstractTokenList<V>
    val symbols: Collection<IntermediateSymbol<*>>

    val tokens: Collection<Token<V>> get() = tokenList.tokens
    val tokensInSolver: List<Token<V>> get() = tokenList.tokensInSolver
    val cachedSolution: Boolean get() = tokenList.cachedSolution

    fun find(item: AbstractVariableItem<*, *>): Token<V>? = tokenList.find(item)
    fun find(index: Int): Token<V>? = tokenList.find(index)
    operator fun get(index: Int): Token<V> = tokenList[index]
    fun indexOf(token: Token<V>): Int? = tokenList.indexOf(token)
    fun indexOf(item: AbstractVariableItem<*, *>): Int? = find(item)?.let { indexOf(it) }

    fun tokensInSolverWithout(items: Set<AbstractVariableItem<*, *>>): List<Token<V>> {
        val result = ArrayList<Token<V>>()
        for (token in this.tokensInSolver) {
            if (token.variable !in items) {
                result.add(token)
            }
        }
        return result
    }

    // Solver boundary methods (Flt64 signatures)
    fun setSolution(solution: List<Flt64>) {
        flush()
        tokenList.setSolution(solution)
    }

    fun setSolution(solution: Map<AbstractVariableItem<*, *>, Flt64>) {
        flush()
        tokenList.setSolution(solution)
    }

    fun flush() {}
    fun clearSolution() { flush(); tokenList.clearSolution() }

    fun cached(cacheKey: Any, solution: List<Flt64>? = null): Boolean? = null
    fun cached(cacheKey: Any, fixedValues: Map<Symbol, Flt64>): Boolean? = null
    fun cachedValue(cacheKey: Any, solution: List<Flt64>? = null): V? = null
    fun cachedValue(cacheKey: Any, fixedValues: Map<Symbol, Flt64>): V? = null
    fun cache(cacheKey: Any, solution: List<Flt64>? = null, value: V): V = value
    fun cache(cacheKey: Any, fixedValues: Map<Symbol, Flt64>, value: V): V = value

    // Generic cache methods (V-typed)
    fun cachedLinearFlatten(cacheKey: Any): Boolean? = null
    fun cachedLinearFlattenValue(cacheKey: Any): LinearFlattenData<V>? = null
    fun cacheLinearFlatten(cacheKey: Any, flatten: LinearFlattenData<V>?): LinearFlattenData<V>? = flatten
    fun clearLinearFlatten(cacheKey: Any): LinearFlattenData<V>? = null

    fun cachedQuadraticFlatten(cacheKey: Any): Boolean? = null
    fun cachedQuadraticFlattenValue(cacheKey: Any): QuadraticFlattenData<V>? = null
    fun cacheQuadraticFlatten(cacheKey: Any, flatten: QuadraticFlattenData<V>?): QuadraticFlattenData<V>? = flatten
    fun clearQuadraticFlatten(cacheKey: Any): QuadraticFlattenData<V>? = null

    fun cachedRange(cacheKey: Any): Boolean? = null
    fun cachedRangeValue(cacheKey: Any): ExpressionRange<V>? = null
    fun cacheRange(cacheKey: Any, range: ExpressionRange<V>?): ExpressionRange<V>? = range
    fun clearRange(cacheKey: Any): ExpressionRange<V>? = null

    fun clearValue(cacheKey: Any) {}

    // Lazy and batch cache methods (solver boundary, Flt64)
    fun cache(cacheKey: Any, solution: List<Flt64>? = null, value: () -> V?): V? {
        return value()?.let { cache(cacheKey = cacheKey, solution = solution, value = it) }
    }

    fun cache(cacheKey: Any, fixedValues: Map<Symbol, Flt64>, value: () -> V?): V? {
        return value()?.let { cache(cacheKey = cacheKey, fixedValues = fixedValues, value = it) }
    }

    fun cacheIfNotCached(cacheKey: Any, solution: List<Flt64>? = null, value: () -> V?): V? {
        var cachedValue = this.cachedValue(cacheKey, solution)
        if (cachedValue == null) {
            value()?.let {
                cachedValue = it
                cache(cacheKey = cacheKey, solution = solution, value = it)
            }
        }
        return cachedValue
    }

    fun cacheIfNotCached(cacheKey: Any, fixedValues: Map<Symbol, Flt64>, value: () -> V?): V? {
        var cachedValue = this.cachedValue(cacheKey, fixedValues)
        if (cachedValue == null) {
            value()?.let {
                cachedValue = it
                cache(cacheKey = cacheKey, fixedValues = fixedValues, value = it)
            }
        }
        return cachedValue
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("cacheSymbols")
    fun cache(symbols: Map<IntermediateSymbol<*>, V>, solution: List<Flt64>? = null) {
        for ((symbol, value) in symbols) { cache(cacheKey = symbol, solution = solution, value = value) }
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("cacheSymbols")
    fun cache(symbols: Map<IntermediateSymbol<*>, V>, fixedValues: Map<Symbol, Flt64>) {
        for ((symbol, value) in symbols) { cache(cacheKey = symbol, fixedValues = fixedValues, value = value) }
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("lazyCacheSymbols")
    fun cache(symbols: Map<IntermediateSymbol<*>, () -> V?>, solution: List<Flt64>? = null) {
        for ((symbol, value) in symbols) { cache(cacheKey = symbol, solution = solution, value = value) }
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("lazyCacheSymbols")
    fun cache(symbols: Map<IntermediateSymbol<*>, () -> V?>, fixedValues: Map<Symbol, Flt64>) {
        for ((symbol, value) in symbols) { cache(cacheKey = symbol, fixedValues = fixedValues, value = value) }
    }

    override fun close() { tokenList.close() }

    val symbolDependencies: Map<IntermediateSymbol<*>, Set<IntermediateSymbol<*>>> get() = emptyMap()
}

typealias AbstractTokenTableF64 = AbstractTokenTable<Flt64>

/**
 * Generic mutable token table interface skeleton - C2-2.5a declaration layer.
 */
interface AbstractMutableTokenTable<V> : AbstractTokenTable<V>, AddableTokenCollection<V>, Copyable<AbstractMutableTokenTable<V>> where V : RealNumber<V>, V : NumberField<V> {
    override fun add(item: AbstractVariableItem<*, *>): Try

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addVariables")
    override fun add(items: Iterable<AbstractVariableItem<*, *>>): Try
    fun remove(item: AbstractVariableItem<*, *>)

    fun add(scope: FunctionSymbolRegistrationScope): Try = add(scope.tokens)
    fun add(symbol: IntermediateSymbol<*>): Try

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addSymbols")
    fun add(symbols: Iterable<IntermediateSymbol<*>>): Try
    fun remove(symbol: IntermediateSymbol<*>)

    fun removeSymbol(symbol: IntermediateSymbol<*>) = remove(symbol)
}

typealias AbstractMutableTokenTableF64 = AbstractMutableTokenTable<Flt64>

@Deprecated("Use AbstractTokenTable<Flt64> instead", ReplaceWith("AbstractTokenTable<Flt64>"))
typealias LegacyAbstractTokenTable = AbstractTokenTable<Flt64>

@Deprecated("Use AbstractMutableTokenTable<Flt64> instead", ReplaceWith("AbstractMutableTokenTable<Flt64>"))
typealias LegacyAbstractMutableTokenTable = AbstractMutableTokenTable<Flt64>

data class TokenTable(
    override val category: Category,
    override val tokenList: TokenListF64,
    override val symbols: List<IntermediateSymbol<*>>
) : AbstractTokenTable<Flt64> {
    constructor(tokenTable: MutableTokenTable) : this(
        category = tokenTable.category,
        tokenList = TokenList(tokenTable.tokenList),
        symbols = tokenTable.symbols.toList()
    )

    override val tokens by tokenList::tokens

    private val cacheContexts = TokenCacheContexts<Flt64>()

    override fun flush() {
        cacheContexts.clearAll()
    }

    override fun cached(cacheKey: Any, solution: List<Flt64>?): Boolean {
        return cacheContexts.value.cached(cacheKey, solution)
    }

    override fun cached(cacheKey: Any, fixedValues: Map<Symbol, Flt64>): Boolean {
        return cacheContexts.value.cached(cacheKey, fixedValues)
    }

    override fun cachedValue(cacheKey: Any, solution: List<Flt64>?): Flt64? {
        return cacheContexts.value.value(cacheKey, solution)
    }

    override fun cachedValue(cacheKey: Any, fixedValues: Map<Symbol, Flt64>): Flt64? {
        return cacheContexts.value.value(cacheKey, fixedValues)
    }

    override fun cache(cacheKey: Any, solution: List<Flt64>?, value: Flt64): Flt64 {
        return cacheContexts.value.put(cacheKey, solution, value)
    }

    override fun cache(cacheKey: Any, fixedValues: Map<Symbol, Flt64>, value: Flt64): Flt64 {
        return cacheContexts.value.put(cacheKey, fixedValues, value)
    }

    override fun cachedLinearFlatten(cacheKey: Any): Boolean {
        return cacheContexts.linearFlatten.contains(cacheKey)
    }

    override fun cachedLinearFlattenValue(cacheKey: Any): LinearFlattenDataF64? {
        return cacheContexts.linearFlatten.get(cacheKey)
    }

    override fun cacheLinearFlatten(cacheKey: Any, flatten: LinearFlattenDataF64?): LinearFlattenDataF64? {
        cacheContexts.linearFlatten.put(cacheKey, flatten)
        return flatten
    }

    override fun clearLinearFlatten(cacheKey: Any): LinearFlattenDataF64? {
        return cacheContexts.linearFlatten.remove(cacheKey)
    }

    override fun cachedQuadraticFlatten(cacheKey: Any): Boolean {
        return cacheContexts.quadraticFlatten.contains(cacheKey)
    }

    override fun cachedQuadraticFlattenValue(cacheKey: Any): QuadraticFlattenDataF64? {
        return cacheContexts.quadraticFlatten.get(cacheKey)
    }

    override fun cacheQuadraticFlatten(cacheKey: Any, flatten: QuadraticFlattenDataF64?): QuadraticFlattenDataF64? {
        cacheContexts.quadraticFlatten.put(cacheKey, flatten)
        return flatten
    }

    override fun clearQuadraticFlatten(cacheKey: Any): QuadraticFlattenDataF64? {
        return cacheContexts.quadraticFlatten.remove(cacheKey)
    }

    override fun cachedRange(cacheKey: Any): Boolean {
        return cacheContexts.range.contains(cacheKey)
    }

    override fun cachedRangeValue(cacheKey: Any): ExpressionRange<Flt64>? {
        return cacheContexts.range.get(cacheKey)
    }

    override fun cacheRange(cacheKey: Any, range: ExpressionRange<Flt64>?): ExpressionRange<Flt64>? {
        cacheContexts.range.put(cacheKey, range)
        return range
    }

    override fun clearRange(cacheKey: Any): ExpressionRange<Flt64>? {
        return cacheContexts.range.remove(cacheKey)
    }

    override fun clearValue(cacheKey: Any) {
        cacheContexts.value.remove(cacheKey)
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("cacheSymbols")
    override fun cache(symbols: Map<IntermediateSymbol<*>, Flt64>, solution: List<Flt64>?) {
        cacheContexts.value.putAll(symbols, solution)
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("cacheSymbols")
    override fun cache(symbols: Map<IntermediateSymbol<*>, Flt64>, fixedValues: Map<Symbol, Flt64>) {
        cacheContexts.value.putAll(symbols, fixedValues)
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("lazyCacheSymbols")
    override fun cache(symbols: Map<IntermediateSymbol<*>, () -> Flt64?>, solution: List<Flt64>?) {
        cacheContexts.value.putAllLazy(symbols, solution)
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("lazyCacheSymbols")
    override fun cache(symbols: Map<IntermediateSymbol<*>, () -> Flt64?>, fixedValues: Map<Symbol, Flt64>) {
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

sealed class MutableTokenTable(
    override val category: Category,
    override val tokenList: MutableTokenListF64,
    protected val _symbols: MutableList<IntermediateSymbol<*>> = ArrayList()
) : AbstractMutableTokenTable<Flt64> {
    private val _symbolsMap: MutableMap<String, IntermediateSymbol<*>> = _symbols.associateBy { it.name }.toMutableMap()
    override val symbols by ::_symbols

    internal val cacheContexts = TokenCacheContexts<Flt64>()

    private val _symbolDependencies: MutableMap<IntermediateSymbol<*>, MutableSet<IntermediateSymbol<*>>> = mutableMapOf()
    override val symbolDependencies: Map<IntermediateSymbol<*>, Set<IntermediateSymbol<*>>> get() = _symbolDependencies

    fun addSymbolDependency(symbol: IntermediateSymbol<*>, dependsOn: IntermediateSymbol<*>) {
        _symbolDependencies.getOrPut(symbol) { mutableSetOf() }.add(dependsOn)
    }

    fun addSymbolWithDependencies(symbol: IntermediateSymbol<*>, dependencies: Set<IntermediateSymbol<*>>) {
        _symbolDependencies.getOrPut(symbol) { mutableSetOf() }.addAll(dependencies)
    }

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

        // B1: 解绑和缓存清理
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

    override fun cached(cacheKey: Any, solution: List<Flt64>?): Boolean {
        return cacheContexts.value.cached(cacheKey, solution)
    }

    override fun cached(cacheKey: Any, fixedValues: Map<Symbol, Flt64>): Boolean? {
        return cacheContexts.value.cached(cacheKey, fixedValues)
    }

    override fun cachedValue(cacheKey: Any, solution: List<Flt64>?): Flt64? {
        return cacheContexts.value.value(cacheKey, solution)
    }

    override fun cachedValue(cacheKey: Any, fixedValues: Map<Symbol, Flt64>): Flt64? {
        return cacheContexts.value.value(cacheKey, fixedValues)
    }

    override fun cache(cacheKey: Any, solution: List<Flt64>?, value: Flt64): Flt64 {
        return cacheContexts.value.put(cacheKey, solution, value)
    }

    override fun cache(cacheKey: Any, fixedValues: Map<Symbol, Flt64>, value: Flt64): Flt64 {
        return cacheContexts.value.put(cacheKey, fixedValues, value)
    }

    override fun cachedLinearFlatten(cacheKey: Any): Boolean {
        return cacheContexts.linearFlatten.contains(cacheKey)
    }

    override fun cachedLinearFlattenValue(cacheKey: Any): LinearFlattenDataF64? {
        return cacheContexts.linearFlatten.get(cacheKey)
    }

    override fun cacheLinearFlatten(cacheKey: Any, flatten: LinearFlattenDataF64?): LinearFlattenDataF64? {
        cacheContexts.linearFlatten.put(cacheKey, flatten)
        return flatten
    }

    override fun clearLinearFlatten(cacheKey: Any): LinearFlattenDataF64? {
        return cacheContexts.linearFlatten.remove(cacheKey)
    }

    override fun cachedQuadraticFlatten(cacheKey: Any): Boolean {
        return cacheContexts.quadraticFlatten.contains(cacheKey)
    }

    override fun cachedQuadraticFlattenValue(cacheKey: Any): QuadraticFlattenDataF64? {
        return cacheContexts.quadraticFlatten.get(cacheKey)
    }

    override fun cacheQuadraticFlatten(cacheKey: Any, flatten: QuadraticFlattenDataF64?): QuadraticFlattenDataF64? {
        cacheContexts.quadraticFlatten.put(cacheKey, flatten)
        return flatten
    }

    override fun clearQuadraticFlatten(cacheKey: Any): QuadraticFlattenDataF64? {
        return cacheContexts.quadraticFlatten.remove(cacheKey)
    }

    override fun cachedRange(cacheKey: Any): Boolean {
        return cacheContexts.range.contains(cacheKey)
    }

    override fun cachedRangeValue(cacheKey: Any): ExpressionRange<Flt64>? {
        return cacheContexts.range.get(cacheKey)
    }

    override fun cacheRange(cacheKey: Any, range: ExpressionRange<Flt64>?): ExpressionRange<Flt64>? {
        cacheContexts.range.put(cacheKey, range)
        return range
    }

    override fun clearRange(cacheKey: Any): ExpressionRange<Flt64>? {
        return cacheContexts.range.remove(cacheKey)
    }

    override fun clearValue(cacheKey: Any) {
        cacheContexts.value.remove(cacheKey)
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("cacheSymbols")
    override fun cache(symbols: Map<IntermediateSymbol<*>, Flt64>, solution: List<Flt64>?) {
        cacheContexts.value.putAll(symbols, solution)
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("cacheSymbols")
    override fun cache(symbols: Map<IntermediateSymbol<*>, Flt64>, fixedValues: Map<Symbol, Flt64>) {
        cacheContexts.value.putAll(symbols, fixedValues)
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("lazyCacheSymbols")
    override fun cache(symbols: Map<IntermediateSymbol<*>, () -> Flt64?>, solution: List<Flt64>?) {
        cacheContexts.value.putAllLazy(symbols, solution)
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("lazyCacheSymbols")
    override fun cache(symbols: Map<IntermediateSymbol<*>, () -> Flt64?>, fixedValues: Map<Symbol, Flt64>) {
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

typealias MutableTokenTableF64 = MutableTokenTable
typealias ConcurrentMutableTokenTableF64 = ConcurrentMutableTokenTable

private fun LegacyAbstractTokenTable.cacheSymbolContext(symbol: IntermediateSymbol<*>) {
    bindTokenTableContext(symbol, this)
    when (symbol) {
        is LinearIntermediateSymbol<*> -> {
            cacheLinearFlatten(symbol, symbol.flattenedMonomials)
        }

        is QuadraticIntermediateSymbol<*> -> {
            cacheQuadraticFlatten(symbol, symbol.flattenedMonomials)
        }
    }
    cacheRange(symbol, symbol.range)
}

private fun LegacyAbstractTokenTable.cacheSymbolContexts(symbols: Iterable<IntermediateSymbol<*>>) {
    for (symbol in symbols) {
        cacheSymbolContext(symbol)
    }
}

@Suppress("USELESS_CAST")
fun Collection<IntermediateSymbol<*>>.register(
    tokenTable: MutableTokenTable,
    fixedValues: Map<Symbol, Flt64>? = null,
    callBack: RegistrationStatusCallBack? = null
): Try {
    val (emptySymbols, notEmptySymbols) = this@register.partition {
        it is LinearExpressionSymbol && it.flattenedMonomials.monomials.isEmpty() && it.flattenedMonomials.constant eq Flt64.zero
    }
    tokenTable.cache(emptySymbols.associateWith { Flt64.zero }.mapKeys { it.key as IntermediateSymbol<*> })
    tokenTable.cacheSymbolContexts(emptySymbols)

    val completedSymbols = emptySymbols.toMutableSet()
    callBack?.invoke(
        RegistrationStatus(
            emptySymbolAmount = emptySymbols.usize,
            readySymbolAmount = completedSymbols.usize,
            totalSymbolAmount = tokenTable.symbols.usize
        )
    )
    var dependencies = notEmptySymbols.associateWith { symbol ->
        symbol.dependencies.filter { dependency ->
            dependency !in completedSymbols
        }.toMutableSet()
    }.toMap()
    for ((symbol, deps) in dependencies) {
        tokenTable.addSymbolWithDependencies(symbol, deps)
    }
    var readySymbols = dependencies.filter { it.value.isEmpty() }.keys
    dependencies = dependencies.filterValues { it.isNotEmpty() }.toMap()
    while (readySymbols.isNotEmpty()) {
        val scope = FunctionSymbolRegistrationScope(origin = tokenTable)
        for (symbol in readySymbols) {
            when (val result = (symbol as? FunctionSymbol)?.let {
                if (fixedValues.isNullOrEmpty()) {
                    it.register(scope)
                } else {
                    it.register(scope, fixedValues)
                }
            }) {
                null -> {}

                is Ok -> {
                    // Value cache is written once by the batch cache(symbols=...) below.
                    // No per-symbol prepareAndCache here to avoid duplicate value computation.
                }

                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
            }
            // Allow symbols to register auxiliary tokens (e.g. slack variables, binaryzation helpers).
            // Default is no-op, so this is backward compatible.
            when (val result = symbol.registerAuxiliaryTokens(scope)) {
                is Ok -> {}
                is Failed -> { return Failed(result.error) }
                is Fatal -> { return Fatal(result.errors) }
            }
        }
        when (val result = tokenTable.add(scope)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }
        // Batch write value cache for all ready symbols (single computation, aligned with concurrent path)
        // 为所有就绪符号批量写入 value 缓存（单次计算，与并发链路一致）
        tokenTable.cache(
            symbols = readySymbols.associateWithNotNull {
                if (fixedValues.isNullOrEmpty()) {
                    it.prepare(null, tokenTable)
                } else {
                    it.prepare(fixedValues, tokenTable)
                }
            }.mapKeys { it.key as IntermediateSymbol<*> }
        )
        tokenTable.cacheSymbolContexts(readySymbols)
        if (memoryUseOver()) {
            System.gc()
        }
        callBack?.invoke(
            RegistrationStatus(
                emptySymbolAmount = emptySymbols.usize,
                readySymbolAmount = completedSymbols.usize + readySymbols.usize,
                totalSymbolAmount = tokenTable.symbols.usize
            )
        )

        completedSymbols.addAll(readySymbols)
        val newReadySymbols = dependencies.filter {
            !completedSymbols.contains(it.key) && it.value.all { dependency ->
                completedSymbols.contains(
                    dependency
                )
            }
        }.keys.toSet()
        dependencies = dependencies.filter { !newReadySymbols.contains(it.key) }
        for ((_, dependency) in dependencies) {
            dependency.removeAll(readySymbols)
        }
        readySymbols = newReadySymbols
        if (memoryUseOver()) {
            System.gc()
        }
    }

    return ok
}

data class ConcurrentTokenTable(
    override val category: Category,
    override val tokenList: TokenListF64,
    override val symbols: List<IntermediateSymbol<*>>
) : LegacyAbstractTokenTable {
    constructor(tokenTable: ConcurrentMutableTokenTable) : this(
        category = tokenTable.category,
        tokenList = TokenList(tokenTable.tokenList),
        symbols = tokenTable.symbols.toList()
    )

    override val tokens by tokenList::tokens

    private val lock = Any()
    private val cacheContexts = TokenCacheContexts<Flt64>()

    override fun flush() {
        synchronized(lock) {
            cacheContexts.clearAll()
        }
    }

    override fun cached(cacheKey: Any, solution: List<Flt64>?): Boolean {
        return synchronized(lock) {
            cacheContexts.value.cached(cacheKey, solution)
        }
    }

    override fun cached(cacheKey: Any, fixedValues: Map<Symbol, Flt64>): Boolean {
        return synchronized(lock) {
            cacheContexts.value.cached(cacheKey, fixedValues)
        }
    }

    override fun cachedValue(cacheKey: Any, solution: List<Flt64>?): Flt64? {
        return synchronized(lock) {
            cacheContexts.value.value(cacheKey, solution)
        }
    }

    override fun cachedValue(cacheKey: Any, fixedValues: Map<Symbol, Flt64>): Flt64? {
        return synchronized(lock) {
            cacheContexts.value.value(cacheKey, fixedValues)
        }
    }

    override fun cache(cacheKey: Any, solution: List<Flt64>?, value: Flt64): Flt64 {
        return synchronized(lock) {
            cacheContexts.value.put(cacheKey, solution, value)
        }
    }

    override fun cache(cacheKey: Any, fixedValues: Map<Symbol, Flt64>, value: Flt64): Flt64 {
        return synchronized(lock) {
            cacheContexts.value.put(cacheKey, fixedValues, value)
        }
    }

    override fun cachedLinearFlatten(cacheKey: Any): Boolean {
        return synchronized(lock) {
            cacheContexts.linearFlatten.contains(cacheKey)
        }
    }

    override fun cachedLinearFlattenValue(cacheKey: Any): LinearFlattenDataF64? {
        return synchronized(lock) {
            cacheContexts.linearFlatten.get(cacheKey)
        }
    }

    override fun cacheLinearFlatten(cacheKey: Any, flatten: LinearFlattenDataF64?): LinearFlattenDataF64? {
        return synchronized(lock) {
            cacheContexts.linearFlatten.put(cacheKey, flatten)
            flatten
        }
    }

    override fun clearLinearFlatten(cacheKey: Any): LinearFlattenDataF64? {
        return synchronized(lock) {
            cacheContexts.linearFlatten.remove(cacheKey)
        }
    }

    override fun cachedQuadraticFlatten(cacheKey: Any): Boolean {
        return synchronized(lock) {
            cacheContexts.quadraticFlatten.contains(cacheKey)
        }
    }

    override fun cachedQuadraticFlattenValue(cacheKey: Any): QuadraticFlattenDataF64? {
        return synchronized(lock) {
            cacheContexts.quadraticFlatten.get(cacheKey)
        }
    }

    override fun cacheQuadraticFlatten(cacheKey: Any, flatten: QuadraticFlattenDataF64?): QuadraticFlattenDataF64? {
        return synchronized(lock) {
            cacheContexts.quadraticFlatten.put(cacheKey, flatten)
            flatten
        }
    }

    override fun clearQuadraticFlatten(cacheKey: Any): QuadraticFlattenDataF64? {
        return synchronized(lock) {
            cacheContexts.quadraticFlatten.remove(cacheKey)
        }
    }

    override fun cachedRange(cacheKey: Any): Boolean {
        return synchronized(lock) {
            cacheContexts.range.contains(cacheKey)
        }
    }

    override fun cachedRangeValue(cacheKey: Any): ExpressionRange<Flt64>? {
        return synchronized(lock) {
            cacheContexts.range.get(cacheKey)
        }
    }

    override fun cacheRange(cacheKey: Any, range: ExpressionRange<Flt64>?): ExpressionRange<Flt64>? {
        return synchronized(lock) {
            cacheContexts.range.put(cacheKey, range)
            range
        }
    }

    override fun clearRange(cacheKey: Any): ExpressionRange<Flt64>? {
        return synchronized(lock) {
            cacheContexts.range.remove(cacheKey)
        }
    }

    override fun clearValue(cacheKey: Any) {
        synchronized(lock) {
            cacheContexts.value.remove(cacheKey)
        }
    }

    override fun cacheIfNotCached(cacheKey: Any, solution: List<Flt64>?, value: () -> Flt64?): Flt64? {
        return synchronized(lock) {
            cacheContexts.value.getOrPut(cacheKey, solution, value)
        }
    }

    override fun cacheIfNotCached(cacheKey: Any, fixedValues: Map<Symbol, Flt64>, value: () -> Flt64?): Flt64? {
        return synchronized(lock) {
            cacheContexts.value.getOrPut(cacheKey, fixedValues, value)
        }
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("cacheSymbols")
    override fun cache(symbols: Map<IntermediateSymbol<*>, Flt64>, solution: List<Flt64>?) {
        synchronized(lock) {
            cacheContexts.value.putAll(symbols, solution)
        }
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("cacheSymbols")
    override fun cache(symbols: Map<IntermediateSymbol<*>, Flt64>, fixedValues: Map<Symbol, Flt64>) {
        synchronized(lock) {
            cacheContexts.value.putAll(symbols, fixedValues)
        }
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("lazyCacheSymbols")
    override fun cache(symbols: Map<IntermediateSymbol<*>, () -> Flt64?>, solution: List<Flt64>?) {
        synchronized(lock) {
            cacheContexts.value.putAllLazy(symbols, solution)
        }
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("lazyCacheSymbols")
    override fun cache(symbols: Map<IntermediateSymbol<*>, () -> Flt64?>, fixedValues: Map<Symbol, Flt64>) {
        synchronized(lock) {
            cacheContexts.value.putAllLazy(symbols, fixedValues)
        }
    }

    override fun close() {
        val boundSymbols = synchronized(lock) {
            val boundSymbols = cacheContexts.boundIntermediateSymbols() + symbols
            cacheContexts.clearAll()
            boundSymbols
        }
        for (symbol in boundSymbols) {
            unbindTokenTableContext(symbol, this)
        }
        super.close()
    }
}

class AutoTokenTable private constructor(
    category: Category,
    tokenList: MutableTokenListF64,
    symbols: List<IntermediateSymbol<*>>
) : MutableTokenTable(
    category = category,
    tokenList = tokenList,
    _symbols = symbols.toMutableList()
) {
    companion object {
        operator fun invoke(
            tokenTable: LegacyAbstractTokenTable,
            checkTokenExists: Boolean
        ): AutoTokenTable {
            return AutoTokenTable(
                category = tokenTable.category,
                tokenList = AutoTokenList(tokenTable.tokenList, checkTokenExists),
                symbols = tokenTable.symbols.toMutableList()
            )
        }
    }

    constructor(
        category: Category,
        checkTokenExists: Boolean
    ) : this(
        category = category,
        tokenList = AutoTokenList(checkTokenExists),
        symbols = ArrayList()
    )

    override fun copy(): MutableTokenTable {
        return AutoTokenTable(
            category = category,
            tokenList = tokenList.copy(),
            symbols = _symbols.toMutableList()
        )
    }
}

class ManualTokenTable private constructor(
    category: Category,
    tokenList: MutableTokenListF64,
    symbols: List<IntermediateSymbol<*>>
) : MutableTokenTable(
    category = category,
    tokenList = tokenList,
    _symbols = symbols.toMutableList()
) {
    companion object {
        operator fun invoke(
            tokenTable: LegacyAbstractTokenTable,
            checkTokenExists: Boolean = System.getProperty("env", "prod") != "prod"
        ): ManualTokenTable {
            return ManualTokenTable(
                category = tokenTable.category,
                tokenList = ManualTokenList(tokenTable.tokenList, checkTokenExists),
                symbols = tokenTable.symbols.toMutableList()
            )
        }
    }

    constructor(
        category: Category,
        checkTokenExists: Boolean = System.getProperty("env", "prod") != "prod"
    ) : this(
        category = category,
        tokenList = ManualTokenList(checkTokenExists),
        symbols = ArrayList()
    )

    override fun copy(): MutableTokenTable {
        return ManualTokenTable(
            category = category,
            tokenList = tokenList.copy(),
            symbols = _symbols.toMutableList()
        )
    }
}

sealed class ConcurrentMutableTokenTable(
    override val category: Category,
    override val tokenList: MutableTokenListF64,
    protected val _symbols: MutableList<IntermediateSymbol<*>> = ArrayList()
) : AbstractMutableTokenTable<Flt64> {
    private val _symbolsMap: MutableMap<String, IntermediateSymbol<*>> = _symbols.associateBy { it.name }.toMutableMap()
    override val symbols by ::_symbols

    private val lock = Any()
    internal val cacheContexts = TokenCacheContexts<Flt64>()

    private val _symbolDependencies: MutableMap<IntermediateSymbol<*>, MutableSet<IntermediateSymbol<*>>> = mutableMapOf()
    override val symbolDependencies: Map<IntermediateSymbol<*>, Set<IntermediateSymbol<*>>> get() = synchronized(lock) { _symbolDependencies.toMap() }

    fun addSymbolDependency(symbol: IntermediateSymbol<*>, dependsOn: IntermediateSymbol<*>) {
        synchronized(lock) {
            _symbolDependencies.getOrPut(symbol) { mutableSetOf() }.add(dependsOn)
        }
    }

    fun addSymbolWithDependencies(symbol: IntermediateSymbol<*>, dependencies: Set<IntermediateSymbol<*>>) {
        synchronized(lock) {
            _symbolDependencies.getOrPut(symbol) { mutableSetOf() }.addAll(dependencies)
        }
    }

    fun validateNoCycles(): Boolean {
        synchronized(lock) {
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
                Err(
                    ErrorCode.ApplicationError,
                    "${symbol.name} over $category"
                )
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
        }

        symbols.add(symbol)
        _symbolsMap[symbol.name] = symbol
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
        synchronized(lock) {
            _symbols.remove(symbol)
            _symbolsMap.remove(symbol.name)

            // B1: 解绑和缓存清理
            // B1: Unbind and clear caches for removed symbol
            unbindTokenTableContext(symbol, this)
            cacheContexts.linearFlatten.remove(symbol)
            cacheContexts.quadraticFlatten.remove(symbol)
            cacheContexts.range.remove(symbol)
            cacheContexts.value.remove(symbol)
            _symbolDependencies.remove(symbol)
            _symbolDependencies.values.forEach { it.remove(symbol) }
        }
    }

    override fun flush() {
        synchronized(lock) {
            tokenList.flush()
            cacheContexts.clearAll()
        }
    }

    override fun cached(cacheKey: Any, solution: List<Flt64>?): Boolean {
        return synchronized(lock) {
            cacheContexts.value.cached(cacheKey, solution)
        }
    }

    override fun cached(cacheKey: Any, fixedValues: Map<Symbol, Flt64>): Boolean {
        return synchronized(lock) {
            cacheContexts.value.cached(cacheKey, fixedValues)
        }
    }

    override fun cachedValue(cacheKey: Any, solution: List<Flt64>?): Flt64? {
        return synchronized(lock) {
            cacheContexts.value.value(cacheKey, solution)
        }
    }

    override fun cachedValue(cacheKey: Any, fixedValues: Map<Symbol, Flt64>): Flt64? {
        return synchronized(lock) {
            cacheContexts.value.value(cacheKey, fixedValues)
        }
    }

    override fun cache(cacheKey: Any, solution: List<Flt64>?, value: Flt64): Flt64 {
        return synchronized(lock) {
            cacheContexts.value.put(cacheKey, solution, value)
        }
    }

    override fun cache(cacheKey: Any, fixedValues: Map<Symbol, Flt64>, value: Flt64): Flt64 {
        return synchronized(lock) {
            cacheContexts.value.put(cacheKey, fixedValues, value)
        }
    }

    override fun cachedLinearFlatten(cacheKey: Any): Boolean {
        return synchronized(lock) {
            cacheContexts.linearFlatten.contains(cacheKey)
        }
    }

    override fun cachedLinearFlattenValue(cacheKey: Any): LinearFlattenDataF64? {
        return synchronized(lock) {
            cacheContexts.linearFlatten.get(cacheKey)
        }
    }

    override fun cacheLinearFlatten(cacheKey: Any, flatten: LinearFlattenDataF64?): LinearFlattenDataF64? {
        return synchronized(lock) {
            cacheContexts.linearFlatten.put(cacheKey, flatten)
            flatten
        }
    }

    override fun clearLinearFlatten(cacheKey: Any): LinearFlattenDataF64? {
        return synchronized(lock) {
            cacheContexts.linearFlatten.remove(cacheKey)
        }
    }

    override fun cachedQuadraticFlatten(cacheKey: Any): Boolean {
        return synchronized(lock) {
            cacheContexts.quadraticFlatten.contains(cacheKey)
        }
    }

    override fun cachedQuadraticFlattenValue(cacheKey: Any): QuadraticFlattenDataF64? {
        return synchronized(lock) {
            cacheContexts.quadraticFlatten.get(cacheKey)
        }
    }

    override fun cacheQuadraticFlatten(cacheKey: Any, flatten: QuadraticFlattenDataF64?): QuadraticFlattenDataF64? {
        return synchronized(lock) {
            cacheContexts.quadraticFlatten.put(cacheKey, flatten)
            flatten
        }
    }

    override fun clearQuadraticFlatten(cacheKey: Any): QuadraticFlattenDataF64? {
        return synchronized(lock) {
            cacheContexts.quadraticFlatten.remove(cacheKey)
        }
    }

    override fun cachedRange(cacheKey: Any): Boolean {
        return synchronized(lock) {
            cacheContexts.range.contains(cacheKey)
        }
    }

    override fun cachedRangeValue(cacheKey: Any): ExpressionRange<Flt64>? {
        return synchronized(lock) {
            cacheContexts.range.get(cacheKey)
        }
    }

    override fun cacheRange(cacheKey: Any, range: ExpressionRange<Flt64>?): ExpressionRange<Flt64>? {
        return synchronized(lock) {
            cacheContexts.range.put(cacheKey, range)
            range
        }
    }

    override fun clearRange(cacheKey: Any): ExpressionRange<Flt64>? {
        return synchronized(lock) {
            cacheContexts.range.remove(cacheKey)
        }
    }

    override fun clearValue(cacheKey: Any) {
        synchronized(lock) {
            cacheContexts.value.remove(cacheKey)
        }
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("cacheSymbols")
    override fun cache(symbols: Map<IntermediateSymbol<*>, Flt64>, solution: List<Flt64>?) {
        synchronized(lock) {
            cacheContexts.value.putAll(symbols, solution)
        }
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("cacheSymbols")
    override fun cache(symbols: Map<IntermediateSymbol<*>, Flt64>, fixedValues: Map<Symbol, Flt64>) {
        synchronized(lock) {
            cacheContexts.value.putAll(symbols, fixedValues)
        }
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("lazyCacheSymbols")
    override fun cache(symbols: Map<IntermediateSymbol<*>, () -> Flt64?>, solution: List<Flt64>?) {
        synchronized(lock) {
            cacheContexts.value.putAllLazy(symbols, solution)
        }
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("lazyCacheSymbols")
    override fun cache(symbols: Map<IntermediateSymbol<*>, () -> Flt64?>, fixedValues: Map<Symbol, Flt64>) {
        synchronized(lock) {
            cacheContexts.value.putAllLazy(symbols, fixedValues)
        }
    }

    override fun cacheIfNotCached(cacheKey: Any, solution: List<Flt64>?, value: () -> Flt64?): Flt64? {
        return synchronized(lock) {
            cacheContexts.value.getOrPut(cacheKey, solution, value)
        }
    }

    override fun cacheIfNotCached(cacheKey: Any, fixedValues: Map<Symbol, Flt64>, value: () -> Flt64?): Flt64? {
        return synchronized(lock) {
            cacheContexts.value.getOrPut(cacheKey, fixedValues, value)
        }
    }

    override fun close() {
        val boundSymbols = synchronized(lock) {
            val boundSymbols = cacheContexts.boundIntermediateSymbols() + symbols
            cacheContexts.clearAll()
            boundSymbols
        }
        for (symbol in boundSymbols) {
            unbindTokenTableContext(symbol, this)
        }
        _symbolsMap.clear()
        _symbols.clear()
        _symbolDependencies.clear()
        super.close()
    }
}

@Suppress("USELESS_CAST")
suspend fun Collection<IntermediateSymbol<*>>.register(
    tokenTable: ConcurrentMutableTokenTable,
    fixedValues: Map<Symbol, Flt64>? = null,
    callBack: RegistrationStatusCallBack? = null
): Try {
    return coroutineScope {
        val (emptySymbols, notEmptySymbols) = this@register.partition {
            it is LinearExpressionSymbol
                    && it.flattenedMonomials.monomials.isEmpty()
                    && it.flattenedMonomials.constant eq Flt64.zero
        }
        tokenTable.cache(emptySymbols.associateWith { Flt64.zero }.mapKeys { it.key as IntermediateSymbol<*> })
        tokenTable.cacheSymbolContexts(emptySymbols)

        val completedSymbols = emptySymbols.toMutableSet()
        callBack?.invoke(
            RegistrationStatus(
                emptySymbolAmount = emptySymbols.usize,
                readySymbolAmount = completedSymbols.usize,
                totalSymbolAmount = tokenTable.symbols.usize
            )
        )
        var dependencies = notEmptySymbols.associateWith { symbol ->
            symbol.dependencies.filter { dependency ->
                dependency !in completedSymbols
            }.toMutableSet()
        }.toMap()
        for ((symbol, deps) in dependencies) {
            tokenTable.addSymbolWithDependencies(symbol, deps)
        }
        var readySymbols = dependencies.filter { it.value.isEmpty() }.keys
        dependencies = dependencies.filterValues { it.isNotEmpty() }.toMap()
        while (readySymbols.isNotEmpty()) {
            val scope = FunctionSymbolRegistrationScope(origin = tokenTable)
            for (symbol in readySymbols) {
                when (val result = (symbol as? FunctionSymbol)?.let {
                    if (fixedValues.isNullOrEmpty()) {
                        it.register(scope)
                    } else {
                        it.register(scope, fixedValues)
                    }
                }) {
                    null -> {}

                    is Ok -> {}

                    is Failed -> {
                        return@coroutineScope Failed(result.error)
                    }

                    is Fatal -> {
                        return@coroutineScope Fatal(result.errors)
                    }
                }
                // Allow symbols to register auxiliary tokens (e.g. slack variables, binaryzation helpers).
                // Default is no-op, so this is backward compatible.
                when (val result = symbol.registerAuxiliaryTokens(scope)) {
                    is Ok -> {}
                    is Failed -> { return@coroutineScope Failed(result.error) }
                    is Fatal -> { return@coroutineScope Fatal(result.errors) }
                }
            }
            when (val result = tokenTable.add(scope)) {
                is Ok -> {}

                is Failed -> {
                    return@coroutineScope Failed(result.error)
                }

                is Fatal -> {
                    return@coroutineScope Fatal(result.errors)
                }
            }
            if (memoryUseOver()) {
                System.gc()
            }

            if (Runtime.getRuntime().availableProcessors() > 1) {
                val thisCompletedSymbolAmountLock = Any()
                var thisCompletedSymbolAmount = completedSymbols.usize
                val jobs = if (Runtime.getRuntime().availableProcessors() > 2) {
                    val segment = (Flt64(readySymbols.size) / Flt64(Runtime.getRuntime().availableProcessors() - 1))
                        .ceil()
                        .toUInt64()
                        .toInt()
                    val readySymbolList = readySymbols.toList().shuffled()
                    (0..(readySymbolList.size / segment)).map { i ->
                        launch(Dispatchers.Default) {
                            val thisReadSymbol = readySymbolList
                                .subList((i * segment), minOf(readySymbolList.size, (i + 1) * segment))
                            // B2: Batch write value cache via prepare + cache
                            // B2: 通过 prepare + cache 批量写入 value 缓存
                            tokenTable.cache(
                                symbols = thisReadSymbol.associateWithNotNull {
                                    if (fixedValues.isNullOrEmpty()) {
                                        it.prepare(null, tokenTable)
                                    } else {
                                        it.prepare(fixedValues, tokenTable)
                                    }
                                }.mapKeys { it.key as IntermediateSymbol<*> }
                            )
                            // B2: Batch write flatten/range cache
                            // B2: 批量写入 flatten/range 缓存
                            tokenTable.cacheSymbolContexts(thisReadSymbol)
                            if (memoryUseOver()) {
                                System.gc()
                            }

                            if (callBack != null) {
                                synchronized(thisCompletedSymbolAmountLock) {
                                    thisCompletedSymbolAmount += thisReadSymbol.usize
                                    callBack(
                                        RegistrationStatus(
                                            emptySymbolAmount = emptySymbols.usize,
                                            readySymbolAmount = thisCompletedSymbolAmount,
                                            totalSymbolAmount = tokenTable.symbols.usize
                                        )
                                    )
                                }
                            }
                        }
                    }
                } else {
                    listOf(
                        launch(Dispatchers.Default) {
                            // B2: Batch write value cache via prepare + cache
                            // B2: 通过 prepare + cache 批量写入 value 缓存
                            tokenTable.cache(
                                symbols = readySymbols.associateWithNotNull {
                                    if (fixedValues.isNullOrEmpty()) {
                                        it.prepare(null, tokenTable)
                                    } else {
                                        it.prepare(fixedValues, tokenTable)
                                    }
                                }.mapKeys { it.key as IntermediateSymbol<*> }
                            )
                            // B2: Batch write flatten/range cache
                            // B2: 批量写入 flatten/range 缓存
                            tokenTable.cacheSymbolContexts(readySymbols)

                            if (callBack != null) {
                                synchronized(thisCompletedSymbolAmountLock) {
                                    thisCompletedSymbolAmount += readySymbols.usize
                                    callBack(
                                        RegistrationStatus(
                                            emptySymbolAmount = emptySymbols.usize,
                                            readySymbolAmount = thisCompletedSymbolAmount,
                                            totalSymbolAmount = tokenTable.symbols.usize
                                        )
                                    )
                                }
                            }
                        }
                    )
                }

                completedSymbols.addAll(readySymbols)
                val newReadySymbols = dependencies.filter {
                    !completedSymbols.contains(it.key) && it.value.all { dependency ->
                        completedSymbols.contains(
                            dependency
                        )
                    }
                }.keys.toSet()
                dependencies = dependencies.filter { !newReadySymbols.contains(it.key) }
                for ((_, dependency) in dependencies) {
                    dependency.removeAll(readySymbols)
                }
                readySymbols = newReadySymbols
                jobs.joinAll()
            } else {
                tokenTable.cache(
                    symbols = readySymbols.associateWithNotNull {
                        if (fixedValues.isNullOrEmpty()) {
                            it.prepare(null, tokenTable)
                        } else {
                            it.prepare(fixedValues, tokenTable)
                        }
                    }.mapKeys { it.key as IntermediateSymbol<*> }
                )
                tokenTable.cacheSymbolContexts(readySymbols)

                callBack?.invoke(
                    RegistrationStatus(
                        emptySymbolAmount = emptySymbols.usize,
                        readySymbolAmount = completedSymbols.usize + readySymbols.usize,
                        totalSymbolAmount = tokenTable.symbols.usize
                    )
                )

                completedSymbols.addAll(readySymbols)
                val newReadySymbols = dependencies.filter {
                    !completedSymbols.contains(it.key) && it.value.all { dependency ->
                        completedSymbols.contains(
                            dependency
                        )
                    }
                }.keys.toSet()
                dependencies = dependencies.filter { !newReadySymbols.contains(it.key) }
                for ((_, dependency) in dependencies) {
                    dependency.removeAll(readySymbols)
                }
                readySymbols = newReadySymbols
            }
            if (memoryUseOver()) {
                System.gc()
            }
        }

        ok
    }
}

class ConcurrentAutoTokenTable private constructor(
    category: Category,
    tokenList: MutableTokenListF64,
    symbols: List<IntermediateSymbol<*>>
) : ConcurrentMutableTokenTable(
    category = category,
    tokenList = tokenList,
    _symbols = symbols.toMutableList()
) {
    companion object {
        operator fun invoke(
            tokenTable: LegacyAbstractTokenTable,
            checkTokenExists: Boolean
        ): ConcurrentMutableTokenTable {
            return ConcurrentAutoTokenTable(
                category = tokenTable.category,
                tokenList = AutoTokenList(tokenTable.tokenList, checkTokenExists),
                symbols = tokenTable.symbols.toMutableList()
            )
        }
    }

    constructor(
        category: Category,
        checkTokenExists: Boolean = System.getProperty("env", "prod") != "prod"
    ) : this(
        category = category,
        tokenList = AutoTokenList(checkTokenExists),
        symbols = ArrayList()
    )

    override fun copy(): ConcurrentMutableTokenTable {
        return ConcurrentAutoTokenTable(
            category = category,
            tokenList = tokenList.copy(),
            symbols = _symbols.toMutableList()
        )
    }
}

class ConcurrentManualAddTokenTable private constructor(
    category: Category,
    tokenList: MutableTokenListF64,
    symbols: List<IntermediateSymbol<*>>
) : ConcurrentMutableTokenTable(
    category = category,
    tokenList = tokenList,
    _symbols = symbols.toMutableList()
) {
    companion object {
        operator fun invoke(
            tokenTable: LegacyAbstractTokenTable,
            checkTokenExists: Boolean = System.getProperty("env", "prod") != "prod"
        ): ConcurrentMutableTokenTable {
            return ConcurrentManualAddTokenTable(
                category = tokenTable.category,
                tokenList = ManualTokenList(tokenTable.tokenList, checkTokenExists),
                symbols = tokenTable.symbols.toMutableList()
            )
        }
    }

    constructor(
        category: Category,
        checkTokenExists: Boolean = System.getProperty("env", "prod") != "prod"
    ) : this(
        category = category,
        tokenList = ManualTokenList(checkTokenExists),
        symbols = ArrayList()
    )

    override fun copy(): ConcurrentMutableTokenTable {
        return ConcurrentManualAddTokenTable(
            category = category,
            tokenList = tokenList.copy(),
            symbols = _symbols.toMutableList()
        )
    }
}



