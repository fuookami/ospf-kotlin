package fuookami.ospf.kotlin.core.token

import fuookami.ospf.kotlin.core.intermediate_symbol.IntermediateSymbol
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.model.basic.ExpressionRange
import fuookami.ospf.kotlin.utils.error.Err
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.error.ExErr
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.symbol.Category
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.ord
import fuookami.ospf.kotlin.utils.functional.Order

/**
 * 并发 token table 读写实现。
 * Concurrent token-table read/write implementation.
 *
 * 说明：通过统一锁与 TokenCacheContexts 管理 value/flatten/range 缓存，并在 close/remove 时解绑上下文。
 * Note: value/flatten/range caches are managed with a unified lock and TokenCacheContexts, and symbol contexts are unbound on close/remove.
 *
 * 非目标：不在该层执行建模求解逻辑，仅提供线程安全的数据与缓存容器。
 * Non-goal: no modeling/solving logic is executed in this layer; it only provides thread-safe data and cache containers.
 */
data class ConcurrentTokenTable<V>(
    override val category: Category,
    override val tokenList: AbstractTokenList<V>,
    override val symbols: List<IntermediateSymbol<*>>
) : AbstractTokenTable<V> where V : RealNumber<V>, V : NumberField<V> {
    constructor(tokenTable: ConcurrentMutableTokenTable<V>) : this(
        category = tokenTable.category,
        tokenList = TokenList(tokenTable.tokenList as MutableTokenList<V>),
        symbols = tokenTable.symbols.toList()
    )

    override val tokens by tokenList::tokens

    private val lock = Any()
    private val cacheContexts = TokenCacheContexts<V>()

    override fun flush() {
        synchronized(lock) {
            cacheContexts.clearAll()
        }
    }

    override fun cached(cacheKey: Any, solution: List<V>?): Boolean {
        return synchronized(lock) {
            cacheContexts.value.cached(cacheKey, solution)
        }
    }

    override fun cached(cacheKey: Any, fixedValues: Map<Symbol, V>): Boolean {
        return synchronized(lock) {
            cacheContexts.value.cached(cacheKey, fixedValues)
        }
    }

    override fun cachedValue(cacheKey: Any, solution: List<V>?): V? {
        return synchronized(lock) {
            cacheContexts.value.value(cacheKey, solution)
        }
    }

    override fun cachedValue(cacheKey: Any, fixedValues: Map<Symbol, V>): V? {
        return synchronized(lock) {
            cacheContexts.value.value(cacheKey, fixedValues)
        }
    }

    override fun cache(cacheKey: Any, solution: List<V>?, value: V): V {
        return synchronized(lock) {
            cacheContexts.value.put(cacheKey, solution, value)
        }
    }

    override fun cache(cacheKey: Any, fixedValues: Map<Symbol, V>, value: V): V {
        return synchronized(lock) {
            cacheContexts.value.put(cacheKey, fixedValues, value)
        }
    }

    override fun cachedLinearFlatten(cacheKey: Any): Boolean {
        return synchronized(lock) {
            cacheContexts.linearFlatten.contains(cacheKey)
        }
    }

    override fun cachedLinearFlattenValue(cacheKey: Any): LinearFlattenData<V>? {
        return synchronized(lock) {
            cacheContexts.linearFlatten.get(cacheKey)
        }
    }

    override fun cacheLinearFlatten(cacheKey: Any, flatten: LinearFlattenData<V>?): LinearFlattenData<V>? {
        return synchronized(lock) {
            cacheContexts.linearFlatten.put(cacheKey, flatten)
            flatten
        }
    }

    override fun clearLinearFlatten(cacheKey: Any): LinearFlattenData<V>? {
        return synchronized(lock) {
            cacheContexts.linearFlatten.remove(cacheKey)
        }
    }

    override fun cachedQuadraticFlatten(cacheKey: Any): Boolean {
        return synchronized(lock) {
            cacheContexts.quadraticFlatten.contains(cacheKey)
        }
    }

    override fun cachedQuadraticFlattenValue(cacheKey: Any): QuadraticFlattenData<V>? {
        return synchronized(lock) {
            cacheContexts.quadraticFlatten.get(cacheKey)
        }
    }

    override fun cacheQuadraticFlatten(cacheKey: Any, flatten: QuadraticFlattenData<V>?): QuadraticFlattenData<V>? {
        return synchronized(lock) {
            cacheContexts.quadraticFlatten.put(cacheKey, flatten)
            flatten
        }
    }

    override fun clearQuadraticFlatten(cacheKey: Any): QuadraticFlattenData<V>? {
        return synchronized(lock) {
            cacheContexts.quadraticFlatten.remove(cacheKey)
        }
    }

    override fun cachedRange(cacheKey: Any): Boolean {
        return synchronized(lock) {
            cacheContexts.range.contains(cacheKey)
        }
    }

    override fun cachedRangeValue(cacheKey: Any): ExpressionRange<V>? {
        return synchronized(lock) {
            cacheContexts.range.get(cacheKey)
        }
    }

    override fun cacheRange(cacheKey: Any, range: ExpressionRange<V>?): ExpressionRange<V>? {
        return synchronized(lock) {
            cacheContexts.range.put(cacheKey, range)
            range
        }
    }

    override fun clearRange(cacheKey: Any): ExpressionRange<V>? {
        return synchronized(lock) {
            cacheContexts.range.remove(cacheKey)
        }
    }

    override fun clearValue(cacheKey: Any) {
        synchronized(lock) {
            cacheContexts.value.remove(cacheKey)
        }
    }

    override fun cacheIfNotCached(cacheKey: Any, solution: List<V>?, value: () -> V?): V? {
        return synchronized(lock) {
            cacheContexts.value.getOrPut(cacheKey, solution, value)
        }
    }

    override fun cacheIfNotCached(cacheKey: Any, fixedValues: Map<Symbol, V>, value: () -> V?): V? {
        return synchronized(lock) {
            cacheContexts.value.getOrPut(cacheKey, fixedValues, value)
        }
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("cacheSymbols")
    override fun cache(symbols: Map<IntermediateSymbol<*>, V>, solution: List<V>?) {
        synchronized(lock) {
            cacheContexts.value.putAll(symbols, solution)
        }
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("cacheSymbols")
    override fun cache(symbols: Map<IntermediateSymbol<*>, V>, fixedValues: Map<Symbol, V>) {
        synchronized(lock) {
            cacheContexts.value.putAll(symbols, fixedValues)
        }
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("lazyCacheSymbols")
    override fun cache(symbols: Map<IntermediateSymbol<*>, () -> V?>, solution: List<V>?) {
        synchronized(lock) {
            cacheContexts.value.putAllLazy(symbols, solution)
        }
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("lazyCacheSymbols")
    override fun cache(symbols: Map<IntermediateSymbol<*>, () -> V?>, fixedValues: Map<Symbol, V>) {
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

/**
 * 并发可变 token table 基类。
 * Base class for concurrent mutable token tables.
 */
sealed class ConcurrentMutableTokenTable<V>(
    override val category: Category,
    override val tokenList: AbstractMutableTokenList<V>,
    protected val _symbols: MutableList<IntermediateSymbol<*>> = ArrayList()
) : AbstractMutableTokenTable<V> where V : RealNumber<V>, V : NumberField<V> {
    private val _symbolsMap: MutableMap<String, IntermediateSymbol<*>> = _symbols.associateBy { it.name }.toMutableMap()
    override val symbols by ::_symbols

    private val lock = Any()
    internal val cacheContexts = TokenCacheContexts<V>()

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
    }

    override fun flush() {
        synchronized(lock) {
            tokenList.flush()
            cacheContexts.clearAll()
        }
    }

    override fun cached(cacheKey: Any, solution: List<V>?): Boolean {
        return synchronized(lock) {
            cacheContexts.value.cached(cacheKey, solution)
        }
    }

    override fun cached(cacheKey: Any, fixedValues: Map<Symbol, V>): Boolean {
        return synchronized(lock) {
            cacheContexts.value.cached(cacheKey, fixedValues)
        }
    }

    override fun cachedValue(cacheKey: Any, solution: List<V>?): V? {
        return synchronized(lock) {
            cacheContexts.value.value(cacheKey, solution)
        }
    }

    override fun cachedValue(cacheKey: Any, fixedValues: Map<Symbol, V>): V? {
        return synchronized(lock) {
            cacheContexts.value.value(cacheKey, fixedValues)
        }
    }

    override fun cache(cacheKey: Any, solution: List<V>?, value: V): V {
        return synchronized(lock) {
            cacheContexts.value.put(cacheKey, solution, value)
        }
    }

    override fun cache(cacheKey: Any, fixedValues: Map<Symbol, V>, value: V): V {
        return synchronized(lock) {
            cacheContexts.value.put(cacheKey, fixedValues, value)
        }
    }

    override fun cachedLinearFlatten(cacheKey: Any): Boolean {
        return synchronized(lock) {
            cacheContexts.linearFlatten.contains(cacheKey)
        }
    }

    override fun cachedLinearFlattenValue(cacheKey: Any): LinearFlattenData<V>? {
        return synchronized(lock) {
            cacheContexts.linearFlatten.get(cacheKey)
        }
    }

    override fun cacheLinearFlatten(cacheKey: Any, flatten: LinearFlattenData<V>?): LinearFlattenData<V>? {
        return synchronized(lock) {
            cacheContexts.linearFlatten.put(cacheKey, flatten)
            flatten
        }
    }

    override fun clearLinearFlatten(cacheKey: Any): LinearFlattenData<V>? {
        return synchronized(lock) {
            cacheContexts.linearFlatten.remove(cacheKey)
        }
    }

    override fun cachedQuadraticFlatten(cacheKey: Any): Boolean {
        return synchronized(lock) {
            cacheContexts.quadraticFlatten.contains(cacheKey)
        }
    }

    override fun cachedQuadraticFlattenValue(cacheKey: Any): QuadraticFlattenData<V>? {
        return synchronized(lock) {
            cacheContexts.quadraticFlatten.get(cacheKey)
        }
    }

    override fun cacheQuadraticFlatten(cacheKey: Any, flatten: QuadraticFlattenData<V>?): QuadraticFlattenData<V>? {
        return synchronized(lock) {
            cacheContexts.quadraticFlatten.put(cacheKey, flatten)
            flatten
        }
    }

    override fun clearQuadraticFlatten(cacheKey: Any): QuadraticFlattenData<V>? {
        return synchronized(lock) {
            cacheContexts.quadraticFlatten.remove(cacheKey)
        }
    }

    override fun cachedRange(cacheKey: Any): Boolean {
        return synchronized(lock) {
            cacheContexts.range.contains(cacheKey)
        }
    }

    override fun cachedRangeValue(cacheKey: Any): ExpressionRange<V>? {
        return synchronized(lock) {
            cacheContexts.range.get(cacheKey)
        }
    }

    override fun cacheRange(cacheKey: Any, range: ExpressionRange<V>?): ExpressionRange<V>? {
        return synchronized(lock) {
            cacheContexts.range.put(cacheKey, range)
            range
        }
    }

    override fun clearRange(cacheKey: Any): ExpressionRange<V>? {
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
    override fun cache(symbols: Map<IntermediateSymbol<*>, V>, solution: List<V>?) {
        synchronized(lock) {
            cacheContexts.value.putAll(symbols, solution)
        }
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("cacheSymbols")
    override fun cache(symbols: Map<IntermediateSymbol<*>, V>, fixedValues: Map<Symbol, V>) {
        synchronized(lock) {
            cacheContexts.value.putAll(symbols, fixedValues)
        }
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("lazyCacheSymbols")
    override fun cache(symbols: Map<IntermediateSymbol<*>, () -> V?>, solution: List<V>?) {
        synchronized(lock) {
            cacheContexts.value.putAllLazy(symbols, solution)
        }
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("lazyCacheSymbols")
    override fun cache(symbols: Map<IntermediateSymbol<*>, () -> V?>, fixedValues: Map<Symbol, V>) {
        synchronized(lock) {
            cacheContexts.value.putAllLazy(symbols, fixedValues)
        }
    }

    override fun cacheIfNotCached(cacheKey: Any, solution: List<V>?, value: () -> V?): V? {
        return synchronized(lock) {
            cacheContexts.value.getOrPut(cacheKey, solution, value)
        }
    }

    override fun cacheIfNotCached(cacheKey: Any, fixedValues: Map<Symbol, V>, value: () -> V?): V? {
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

class ConcurrentAutoTokenTable<V>(
    category: Category,
    private val checkTokenExists: Boolean = System.getProperty("env", "prod") != "prod",
    private val _tokenList: AutoTokenList<V>
) : ConcurrentMutableTokenTable<V>(category, _tokenList) where V : RealNumber<V>, V : NumberField<V> {
    constructor(category: Category, checkTokenExists: Boolean = System.getProperty("env", "prod") != "prod") : this(
        category = category,
        checkTokenExists = checkTokenExists,
        _tokenList = AutoTokenList(checkTokenExists)
    )

    override fun copy(): ConcurrentMutableTokenTable<V> {
        return ConcurrentAutoTokenTable(
            category = category,
            checkTokenExists = checkTokenExists,
            _tokenList = _tokenList.copy() as AutoTokenList<V>
        )
    }
}

class ConcurrentManualAddTokenTable<V>(
    category: Category,
    private val checkTokenExists: Boolean = System.getProperty("env", "prod") != "prod",
    private val _tokenList: ManualTokenList<V>
) : ConcurrentMutableTokenTable<V>(category, _tokenList) where V : RealNumber<V>, V : NumberField<V> {
    constructor(category: Category, checkTokenExists: Boolean = System.getProperty("env", "prod") != "prod") : this(
        category = category,
        checkTokenExists = checkTokenExists,
        _tokenList = ManualTokenList(checkTokenExists)
    )

    override fun copy(): ConcurrentMutableTokenTable<V> {
        return ConcurrentManualAddTokenTable(
            category = category,
            checkTokenExists = checkTokenExists,
            _tokenList = _tokenList.copy() as ManualTokenList<V>
        )
    }
}

