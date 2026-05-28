/**
 * Token 表接口与实现，管理中间符号的注册、缓存与求解结果。
 * Token table interfaces and implementations managing intermediate symbol registration, caching, and solve results.
 */
package fuookami.ospf.kotlin.core.token

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.concept.Copyable
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.usize
import fuookami.ospf.kotlin.math.symbol.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.model.basic.ExpressionRange
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.IntermediateSymbol
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem

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
 */
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

    fun setSolution(solution: List<V>) {
        flush()
        tokenList.setSolution(solution)
    }

    fun setSolution(solution: Map<AbstractVariableItem<*, *>, V>) {
        flush()
        tokenList.setSolution(solution)
    }

    /** Solver-boundary adapter: sets solution from Flt64 solver output directly into internal storage. */
    fun setSolverSolution(solution: List<fuookami.ospf.kotlin.math.algebra.number.Flt64>) {
        flush()
        tokenList.setSolverSolution(solution)
    }

    /** Solver-boundary adapter: sets solution from Flt64 solver output directly into internal storage. */
    fun setSolverSolution(solution: Map<AbstractVariableItem<*, *>, Flt64>) {
        flush()
        tokenList.setSolverSolution(solution)
    }

    fun flush() {}
    fun clearSolution() { flush(); tokenList.clearSolution() }

    fun cached(cacheKey: Any, solution: List<V>? = null): Boolean? = null
    fun cached(cacheKey: Any, fixedValues: Map<Symbol, V>): Boolean? = null
    fun cachedValue(cacheKey: Any, solution: List<V>? = null): V? = null
    fun cachedValue(cacheKey: Any, fixedValues: Map<Symbol, V>): V? = null
    fun cache(cacheKey: Any, solution: List<V>? = null, value: V): V = value
    fun cache(cacheKey: Any, fixedValues: Map<Symbol, V>, value: V): V = value

    // --- Solver-boundary cache adapters (Flt64 -> V via converter) ---

    fun cachedSolver(cacheKey: Any, solution: List<fuookami.ospf.kotlin.math.algebra.number.Flt64>? = null, converter: IntoValue<V>): Boolean? {
        return cached(cacheKey, solution?.map { converter.intoValue(it) })
    }

    fun cachedSolver(cacheKey: Any, fixedValues: Map<Symbol, Flt64>, converter: IntoValue<V>): Boolean? {
        return cached(cacheKey, fixedValues.mapValues { converter.intoValue(it.value) })
    }

    fun cachedSolverValue(cacheKey: Any, solution: List<fuookami.ospf.kotlin.math.algebra.number.Flt64>? = null, converter: IntoValue<V>): V? {
        return cachedValue(cacheKey, solution?.map { converter.intoValue(it) })
    }

    fun cachedSolverValue(cacheKey: Any, fixedValues: Map<Symbol, Flt64>, converter: IntoValue<V>): V? {
        return cachedValue(cacheKey, fixedValues.mapValues { converter.intoValue(it.value) })
    }

    fun cacheSolver(cacheKey: Any, solution: List<fuookami.ospf.kotlin.math.algebra.number.Flt64>? = null, value: V, converter: IntoValue<V>): V {
        return cache(cacheKey, solution?.map { converter.intoValue(it) }, value)
    }

    fun cacheSolver(cacheKey: Any, fixedValues: Map<Symbol, Flt64>, value: V, converter: IntoValue<V>): V {
        return cache(cacheKey, fixedValues.mapValues { converter.intoValue(it.value) }, value)
    }

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
    fun cache(cacheKey: Any, solution: List<V>? = null, value: () -> V?): V? {
        return value()?.let { cache(cacheKey = cacheKey, solution = solution, value = it) }
    }

    fun cache(cacheKey: Any, fixedValues: Map<Symbol, V>, value: () -> V?): V? {
        return value()?.let { cache(cacheKey = cacheKey, fixedValues = fixedValues, value = it) }
    }

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

    fun cacheSolverIfNotCached(cacheKey: Any, solution: List<fuookami.ospf.kotlin.math.algebra.number.Flt64>? = null, value: () -> V?, converter: IntoValue<V>): V? {
        return cacheIfNotCached(cacheKey, solution?.map { converter.intoValue(it) }, value)
    }

    fun cacheSolverIfNotCached(cacheKey: Any, fixedValues: Map<Symbol, Flt64>, value: () -> V?, converter: IntoValue<V>): V? {
        return cacheIfNotCached(cacheKey, fixedValues.mapValues { converter.intoValue(it.value) }, value)
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("cacheSymbols")
    fun cache(symbols: Map<IntermediateSymbol<*>, V>, solution: List<V>? = null) {
        for ((symbol, value) in symbols) { cache(cacheKey = symbol, solution = solution, value = value) }
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("cacheSymbols")
    fun cache(symbols: Map<IntermediateSymbol<*>, V>, fixedValues: Map<Symbol, V>) {
        for ((symbol, value) in symbols) { cache(cacheKey = symbol, fixedValues = fixedValues, value = value) }
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("lazyCacheSymbols")
    fun cache(symbols: Map<IntermediateSymbol<*>, () -> V?>, solution: List<V>? = null) {
        for ((symbol, value) in symbols) { cache(cacheKey = symbol, solution = solution, value = value) }
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("lazyCacheSymbols")
    fun cache(symbols: Map<IntermediateSymbol<*>, () -> V?>, fixedValues: Map<Symbol, V>) {
        for ((symbol, value) in symbols) { cache(cacheKey = symbol, fixedValues = fixedValues, value = value) }
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("cacheSolverSymbols")
    fun cacheSolver(symbols: Map<IntermediateSymbol<*>, V>, solution: List<fuookami.ospf.kotlin.math.algebra.number.Flt64>? = null, converter: IntoValue<V>) {
        cache(symbols, solution?.map { converter.intoValue(it) })
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("cacheSolverSymbols")
    fun cacheSolver(symbols: Map<IntermediateSymbol<*>, V>, fixedValues: Map<Symbol, Flt64>, converter: IntoValue<V>) {
        cache(symbols, fixedValues.mapValues { converter.intoValue(it.value) })
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("lazyCacheSolverSymbols")
    fun cacheSolver(symbols: Map<IntermediateSymbol<*>, () -> V?>, solution: List<fuookami.ospf.kotlin.math.algebra.number.Flt64>? = null, converter: IntoValue<V>) {
        cache(symbols, solution?.map { converter.intoValue(it) })
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("lazyCacheSolverSymbols")
    fun cacheSolver(symbols: Map<IntermediateSymbol<*>, () -> V?>, fixedValues: Map<Symbol, Flt64>, converter: IntoValue<V>) {
        cache(symbols, fixedValues.mapValues { converter.intoValue(it.value) })
    }

    override fun close() { tokenList.close() }

    val symbolDependencies: Map<IntermediateSymbol<*>, Set<IntermediateSymbol<*>>> get() = emptyMap()
}

/**
 * Generic mutable token table interface skeleton - C2-2.5a declaration layer.
 */
interface AbstractMutableTokenTable<V> : AbstractTokenTable<V>, AddableTokenCollection<V>, Copyable<AbstractMutableTokenTable<V>> where V : RealNumber<V>, V : NumberField<V> {
    override fun add(item: AbstractVariableItem<*, *>): Try

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addVariables")
    override fun add(items: Iterable<AbstractVariableItem<*, *>>): Try
    fun remove(item: AbstractVariableItem<*, *>)

    fun add(symbol: IntermediateSymbol<*>): Try

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addSymbols")
    fun add(symbols: Iterable<IntermediateSymbol<*>>): Try
    fun remove(symbol: IntermediateSymbol<*>)

    fun removeSymbol(symbol: IntermediateSymbol<*>) = remove(symbol)
}

/**
 * 不可变 token 表实现，封装已注册的 token 列表和符号集合。
 * Immutable token table implementation wrapping a registered token list and symbol collection.
 *
 * @property category 符号操作类别 / Symbol operation category
 * @property tokenList token 列表 / Token list
 * @property symbols 已注册的中间符号集合 / Registered intermediate symbol collection
 */
data class TokenTable<V>(
    override val category: Category,
    override val tokenList: AbstractTokenList<V>,
    override val symbols: List<IntermediateSymbol<*>>
) : AbstractTokenTable<V> where V : RealNumber<V>, V : NumberField<V> {
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
 * @property category 符号操作类别 / Symbol operation category
 * @property tokenList 可变 token 列表 / Mutable token list
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
 * @property category 符号操作类别 / Symbol operation category
 * @property checkTokenExists 是否检查 token 已存在 / Whether to check token existence
 */
class AutoTokenTable<V>(
    category: Category,
    private val checkTokenExists: Boolean,
    private val _tokenList: AutoTokenList<V>
) : MutableTokenTable<V>(category, _tokenList) where V : RealNumber<V>, V : NumberField<V> {
    constructor(category: Category, checkTokenExists: Boolean) : this(
        category = category,
        checkTokenExists = checkTokenExists,
        _tokenList = AutoTokenList(checkTokenExists)
    )

    override fun copy(): MutableTokenTable<V> {
        return AutoTokenTable(
            category = category,
            checkTokenExists = checkTokenExists,
            _tokenList = _tokenList.copy() as AutoTokenList<V>
        )
    }
}

/**
 * 手动 token 表，变量需显式添加后才能使用。
 * Manual token table where variables must be explicitly added before use.
 *
 * @property category 符号操作类别 / Symbol operation category
 * @property checkTokenExists 是否检查 token 已存在 / Whether to check token existence
 */
class ManualTokenTable<V>(
    category: Category,
    private val checkTokenExists: Boolean,
    private val _tokenList: ManualTokenList<V>
) : MutableTokenTable<V>(category, _tokenList) where V : RealNumber<V>, V : NumberField<V> {
    constructor(category: Category, checkTokenExists: Boolean = System.getProperty("env", "prod") != "prod") : this(
        category = category,
        checkTokenExists = checkTokenExists,
        _tokenList = ManualTokenList(checkTokenExists)
    )

    override fun copy(): MutableTokenTable<V> {
        return ManualTokenTable(
            category = category,
            checkTokenExists = checkTokenExists,
            _tokenList = _tokenList.copy() as ManualTokenList<V>
        )
    }
}
