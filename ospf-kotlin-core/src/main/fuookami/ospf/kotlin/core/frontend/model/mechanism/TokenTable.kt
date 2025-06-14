package fuookami.ospf.kotlin.core.frontend.model.mechanism

import kotlin.collections.*
import kotlinx.coroutines.*
import io.michaelrocks.bimap.*
import fuookami.ospf.kotlin.utils.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.symbol.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.operator.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*

class RepeatedSymbolError(
    val repeatedSymbol: IntermediateSymbol,
    val symbol: IntermediateSymbol
) : Throwable() {
    override val message get() = "Repeated \"${symbol.name}\", old: $repeatedSymbol, new: $symbol."
}

sealed interface AbstractTokenTable {
    val category: Category
    val tokenList: AbstractTokenList
    val tokens: Collection<Token> get() = tokenList.tokens
    val tokenIndexMap: BiMap<Token, Int> get() = tokenList.tokenIndexMap
    val symbols: Collection<IntermediateSymbol>
    val cachedSolution: Boolean get() = tokenList.cachedSolution

    fun find(item: AbstractVariableItem<*, *>): Token? {
        return tokenList.find(item)
    }

    fun find(index: Int): Token? {
        return tokenList.find(index)
    }

    operator fun get(index: Int): Token {
        return tokenIndexMap.inverse[index] ?: tokenList.tokens.find { it.solverIndex == index }!!
    }

    fun indexOf(token: Token): Int {
        return tokenIndexMap[token] ?: token.solverIndex
    }

    fun indexOf(item: AbstractVariableItem<*, *>): Int? {
        return find(item)?.let { indexOf(it) }
    }

    fun setSolution(solution: List<Flt64>) {
        flush()
        tokenList.setSolution(solution)
    }

    fun setSolution(solution: Map<AbstractVariableItem<*, *>, Flt64>) {
        flush()
        tokenList.setSolution(solution)
    }

    fun flush() {}

    fun clearSolution() {
        flush()
        tokenList.clearSolution()
    }

    fun cached(symbol: IntermediateSymbol, solution: List<Flt64>? = null): Boolean? {
        return null
    }

    fun cachedValue(symbol: IntermediateSymbol, solution: List<Flt64>? = null): Flt64? {
        return null
    }

    fun cache(symbol: IntermediateSymbol, solution: List<Flt64>? = null, value: Flt64): Flt64 {
        return value
    }

    fun cache(symbol: IntermediateSymbol, solution: List<Flt64>? = null, value: () -> Flt64?): Flt64? {
        return value()?.let {
            cache(symbol, solution, it)
        }
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("cacheSymbols")
    fun cache(symbols: Map<IntermediateSymbol, Flt64>, solution: List<Flt64>? = null) {}

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("lazyCacheSymbols")
    fun cache(symbols: Map<IntermediateSymbol, () -> Flt64?>, solution: List<Flt64>? = null) {}
}

sealed interface AbstractMutableTokenTable : Copyable<AbstractMutableTokenTable>, AbstractTokenTable {
    fun add(item: AbstractVariableItem<*, *>): Try

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addVariables")
    fun add(items: Iterable<AbstractVariableItem<*, *>>): Try
    fun remove(item: AbstractVariableItem<*, *>)

    fun add(symbol: IntermediateSymbol): Try

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addSymbols")
    fun add(symbols: Iterable<IntermediateSymbol>): Try
    fun remove(symbol: IntermediateSymbol)
}

data class TokenTable(
    override val category: Category,
    override val tokenList: TokenList,
    override val symbols: List<IntermediateSymbol>
) : AbstractTokenTable {
    constructor(tokenTable: MutableTokenTable) : this(
        category = tokenTable.category,
        tokenList = TokenList(tokenTable.tokenList),
        symbols = tokenTable.symbols.toList()
    )

    override val tokens by tokenList::tokens

    private val cachedSymbolValue: MutableMap<Pair<IntermediateSymbol, List<Flt64>?>, Flt64?> = HashMap()

    override fun flush() {
        cachedSymbolValue.clear()
    }

    override fun cached(symbol: IntermediateSymbol, solution: List<Flt64>?): Boolean {
        return cachedSymbolValue.containsKey(symbol to solution)
    }

    override fun cachedValue(symbol: IntermediateSymbol, solution: List<Flt64>?): Flt64? {
        return cachedSymbolValue[symbol to solution]
    }

    override fun cache(symbol: IntermediateSymbol, solution: List<Flt64>?, value: Flt64): Flt64 {
        cachedSymbolValue[symbol to solution] = value
        return value
    }
}

sealed class MutableTokenTable(
    override val category: Category,
    override val tokenList: MutableTokenList,
    protected val _symbols: MutableList<IntermediateSymbol> = ArrayList()
) : AbstractMutableTokenTable {
    private val _symbolsMap: MutableMap<String, IntermediateSymbol> = _symbols.associateBy { it.name }.toMutableMap()
    override val symbols by ::_symbols

    internal val cachedSymbolValue: MutableMap<Pair<IntermediateSymbol, List<Flt64>?>, Flt64?> = HashMap()

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

    override fun add(symbol: IntermediateSymbol): Try {
        if ((symbol.operationCategory ord category) is Order.Greater) {
            return Failed(Err(ErrorCode.ApplicationError, "${symbol.name} over $category"))
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
    override fun add(symbols: Iterable<IntermediateSymbol>): Try {
        for (symbol in symbols) {
            when (val result = add(symbol)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }
        return ok
    }

    override fun remove(symbol: IntermediateSymbol) {
        _symbols.remove(symbol)
        _symbolsMap.remove(symbol.name)
    }

    override fun flush() {
        cachedSymbolValue.clear()
    }

    override fun cached(symbol: IntermediateSymbol, solution: List<Flt64>?): Boolean {
        return cachedSymbolValue.containsKey(symbol to solution)
    }

    override fun cachedValue(symbol: IntermediateSymbol, solution: List<Flt64>?): Flt64? {
        return cachedSymbolValue[symbol to solution]
    }

    override fun cache(symbol: IntermediateSymbol, solution: List<Flt64>?, value: Flt64): Flt64 {
        cachedSymbolValue[symbol to solution] = value
        return value
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("cacheSymbols")
    override fun cache(symbols: Map<IntermediateSymbol, Flt64>, solution: List<Flt64>?) {
        cachedSymbolValue.putAll(symbols.map { (symbol, value) ->
            Pair(symbol, solution) to value
        })
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("lazyCacheSymbols")
    override fun cache(symbols: Map<IntermediateSymbol, () -> Flt64?>, solution: List<Flt64>?) {
        cachedSymbolValue.putAll(symbols.mapNotNull { (symbol, value) ->
            value()?.let {
                Pair(symbol, solution) to it
            }
        })
    }
}

fun Collection<IntermediateSymbol>.register(
    tokenTable: MutableTokenTable,
    callBack: RegistrationStatusCallBack? = null
): Try {
    val (emptySymbols, notEmptySymbols) = this@register.partition {
        it is ExpressionSymbol && it.polynomial.monomials.isEmpty() && it.polynomial.constant eq Flt64.zero
    }
    tokenTable.cache(emptySymbols.associateWith { Flt64.zero } as Map<IntermediateSymbol, Flt64>)

    val completedSymbols = emptySymbols.toMutableSet()
    callBack?.invoke(
        RegistrationStatus(
            emptySymbolAmount = emptySymbols.usize,
            readySymbolAmount = completedSymbols.usize,
            totalSymbolAmount = tokenTable.symbols.usize
        )
    )
    var dependencies = notEmptySymbols.associateWith { it.dependencies.toMutableSet() }.toMap()
    var readySymbols = dependencies.filter { it.value.isEmpty() }.keys
    dependencies = dependencies.filterValues { it.isNotEmpty() }.toMap()
    while (readySymbols.isNotEmpty()) {
        for (symbol in readySymbols) {
            when (val result = (symbol as? FunctionSymbol)?.register(tokenTable)) {
                null -> {}

                is Ok -> {
                    symbol.prepareAndCache(tokenTable)
                }

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }
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
    override val tokenList: TokenList,
    override val symbols: List<IntermediateSymbol>
) : AbstractTokenTable {
    constructor(tokenTable: ConcurrentMutableTokenTable) : this(
        category = tokenTable.category,
        tokenList = TokenList(tokenTable.tokenList),
        symbols = tokenTable.symbols.toList()
    )

    override val tokens by tokenList::tokens

    private val lock = Any()
    private val cachedSymbolValue: MutableMap<Pair<IntermediateSymbol, List<Flt64>?>, Flt64?> = HashMap()

    override fun flush() {
        synchronized(lock) {
            cachedSymbolValue.clear()
        }
    }

    override fun cached(symbol: IntermediateSymbol, solution: List<Flt64>?): Boolean {
        return synchronized(lock) {
            cachedSymbolValue.containsKey(symbol to solution)
        }
    }

    override fun cachedValue(symbol: IntermediateSymbol, solution: List<Flt64>?): Flt64? {
        return synchronized(lock) {
            cachedSymbolValue[symbol to solution]
        }
    }

    override fun cache(symbol: IntermediateSymbol, solution: List<Flt64>?, value: Flt64): Flt64 {
        return synchronized(lock) {
            cachedSymbolValue[symbol to solution] = value
            value
        }
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("cacheSymbols")
    override fun cache(symbols: Map<IntermediateSymbol, Flt64>, solution: List<Flt64>?) {
        synchronized(lock) {
            cachedSymbolValue.putAll(symbols.map { (symbol, value) ->
                Pair(symbol, solution) to value
            })
        }
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("lazyCacheSymbols")
    override fun cache(symbols: Map<IntermediateSymbol, () -> Flt64?>, solution: List<Flt64>?) {
        synchronized(lock) {
            cachedSymbolValue.putAll(symbols.mapNotNull { (symbol, value) ->
                value()?.let {
                    Pair(symbol, solution) to it
                }
            })
        }
    }
}

class AutoTokenTable private constructor(
    category: Category,
    tokenList: MutableTokenList,
    symbols: List<IntermediateSymbol>
) : MutableTokenTable(category, tokenList, symbols.toMutableList()) {
    companion object {
        operator fun invoke(tokenTable: AbstractTokenTable): AutoTokenTable {
            return AutoTokenTable(
                category = tokenTable.category,
                tokenList = AutoTokenList(tokenTable.tokenList),
                symbols = tokenTable.symbols.toMutableList()
            )
        }
    }

    constructor(category: Category) : this(
        category = category,
        tokenList = AutoTokenList(),
        symbols = ArrayList()
    )

    override fun copy(): MutableTokenTable {
        return AutoTokenTable(category, tokenList.copy(), _symbols.toMutableList())
    }
}

class ManualTokenTable private constructor(
    category: Category,
    tokenList: MutableTokenList,
    symbols: List<IntermediateSymbol>
) : MutableTokenTable(category, tokenList, symbols.toMutableList()) {
    companion object {
        operator fun invoke(tokenTable: AbstractTokenTable): ManualTokenTable {
            return ManualTokenTable(
                category = tokenTable.category,
                tokenList = ManualTokenList(tokenTable.tokenList),
                symbols = tokenTable.symbols.toMutableList()
            )
        }
    }

    constructor(category: Category) : this(
        category = category,
        tokenList = ManualTokenList(),
        symbols = ArrayList()
    )

    override fun copy(): MutableTokenTable {
        return ManualTokenTable(category, tokenList.copy(), _symbols.toMutableList())
    }
}

sealed class ConcurrentMutableTokenTable(
    override val category: Category,
    override val tokenList: MutableTokenList,
    protected val _symbols: MutableList<IntermediateSymbol> = ArrayList()
) : AbstractMutableTokenTable {
    private val _symbolsMap: MutableMap<String, IntermediateSymbol> = _symbols.associateBy { it.name }.toMutableMap()
    override val symbols by ::_symbols

    private val lock = Any()
    internal val cachedSymbolValue: MutableMap<Pair<IntermediateSymbol, List<Flt64>?>, Flt64?> = HashMap()

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

    override fun add(symbol: IntermediateSymbol): Try {
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
    override fun add(symbols: Iterable<IntermediateSymbol>): Try {
        for (symbol in symbols) {
            when (val result = add(symbol)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }
        return ok
    }

    override fun remove(symbol: IntermediateSymbol) {
        _symbols.remove(symbol)
        _symbolsMap.remove(symbol.name)
    }

    override fun flush() {
        synchronized(lock) {
            cachedSymbolValue.clear()
        }
    }

    override fun cached(symbol: IntermediateSymbol, solution: List<Flt64>?): Boolean {
        return synchronized(lock) {
            cachedSymbolValue.containsKey(symbol to solution)
        }
    }

    override fun cachedValue(symbol: IntermediateSymbol, solution: List<Flt64>?): Flt64? {
        return synchronized(lock) {
            cachedSymbolValue[symbol to solution]
        }
    }

    override fun cache(symbol: IntermediateSymbol, solution: List<Flt64>?, value: Flt64): Flt64 {
        return synchronized(lock) {
            cachedSymbolValue[symbol to solution] = value
            value
        }
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("cacheSymbols")
    override fun cache(symbols: Map<IntermediateSymbol, Flt64>, solution: List<Flt64>?) {
        synchronized(lock) {
            cachedSymbolValue.putAll(symbols.map { (symbol, value) ->
                Pair(symbol, solution) to value
            })
        }
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("lazyCacheSymbols")
    override fun cache(symbols: Map<IntermediateSymbol, () -> Flt64?>, solution: List<Flt64>?) {
        synchronized(lock) {
            cachedSymbolValue.putAll(symbols.mapNotNull { (symbol, value) ->
                value()?.let {
                    Pair(symbol, solution) to it
                }
            })
        }
    }
}


suspend fun Collection<IntermediateSymbol>.register(
    tokenTable: ConcurrentMutableTokenTable,
    callBack: RegistrationStatusCallBack? = null
): Try {
    return coroutineScope {
        val (emptySymbols, notEmptySymbols) = this@register.partition {
            it is ExpressionSymbol
                    && it.polynomial.monomials.isEmpty()
                    && it.polynomial.constant eq Flt64.zero
        }
        tokenTable.cache(emptySymbols.associateWith { Flt64.zero } as Map<IntermediateSymbol, Flt64>)

        val completedSymbols = emptySymbols.toMutableSet()
        callBack?.invoke(
            RegistrationStatus(
                emptySymbolAmount = emptySymbols.usize,
                readySymbolAmount = completedSymbols.usize,
                totalSymbolAmount = tokenTable.symbols.usize
            )
        )
        var dependencies = notEmptySymbols.associateWith { it.dependencies.toMutableSet() }.toMap()
        var readySymbols = dependencies.filter { it.value.isEmpty() }.keys
        dependencies = dependencies.filterValues { it.isNotEmpty() }.toMap()
        while (readySymbols.isNotEmpty()) {
            for (symbol in readySymbols) {
                when (val result = (symbol as? FunctionSymbol)?.register(tokenTable)) {
                    null -> {}

                    is Ok -> {}

                    is Failed -> {
                        return@coroutineScope Failed(result.error)
                    }
                }
            }
            if (memoryUseOver()) {
                System.gc()
            }

            if (Runtime.getRuntime().availableProcessors() > 1) {
                val thisCompletedSymbolAmountLock = Any()
                var thisCompletedSymbolAmount = completedSymbols.usize
                val jobs = if (Runtime.getRuntime().availableProcessors() > 2) {
//                    val factor = Flt64(readySymbols.size / (Runtime.getRuntime().availableProcessors() - 1)).lg()!!.ceil().toUInt64().toInt()
//                    val segment = if (factor >= 1) {
//                        pow(UInt64.ten, factor).toInt()
//                    } else {
//                        10
//                    }
                    val segment = (Flt64(readySymbols.size) / Flt64(Runtime.getRuntime().availableProcessors() - 1))
                        .ceil()
                        .toUInt64()
                        .toInt()
                    val readySymbolList = readySymbols.toList().shuffled()
                    (0..(readySymbolList.size / segment)).map { i ->
                        launch(Dispatchers.Default) {
                            val thisReadSymbol = readySymbolList
                                .subList((i * segment), minOf(readySymbolList.size, (i + 1) * segment))
                            tokenTable.cache(
                                thisReadSymbol.associateWithNotNull {
                                    it.prepare(tokenTable)
                                } as Map<IntermediateSymbol, Flt64>
                            )
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
                            tokenTable.cache(
                                readySymbols.associateWithNotNull {
                                    it.prepare(tokenTable)
                                } as Map<IntermediateSymbol, Flt64>
                            )

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
                    readySymbols.associateWithNotNull {
                        it.prepare(tokenTable)
                    } as Map<IntermediateSymbol, Flt64>
                )

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
    tokenList: MutableTokenList,
    symbols: List<IntermediateSymbol>
) : ConcurrentMutableTokenTable(category, tokenList, symbols.toMutableList()) {
    companion object {
        operator fun invoke(tokenTable: AbstractTokenTable): ConcurrentMutableTokenTable {
            return ConcurrentAutoTokenTable(
                category = tokenTable.category,
                tokenList = AutoTokenList(tokenTable.tokenList),
                symbols = tokenTable.symbols.toMutableList()
            )
        }
    }

    constructor(category: Category) : this(
        category = category,
        tokenList = AutoTokenList(),
        symbols = ArrayList()
    )

    override fun copy(): ConcurrentMutableTokenTable {
        return ConcurrentAutoTokenTable(category, tokenList.copy(), _symbols.toMutableList())
    }
}

class ConcurrentManualAddTokenTable private constructor(
    category: Category,
    tokenList: MutableTokenList,
    symbols: List<IntermediateSymbol>
) : ConcurrentMutableTokenTable(category, tokenList, symbols.toMutableList()) {
    companion object {
        operator fun invoke(tokenTable: AbstractTokenTable): ConcurrentMutableTokenTable {
            return ConcurrentManualAddTokenTable(
                category = tokenTable.category,
                tokenList = ManualTokenList(tokenTable.tokenList),
                symbols = tokenTable.symbols.toMutableList()
            )
        }
    }

    constructor(category: Category) : this(
        category = category,
        tokenList = ManualTokenList(),
        symbols = ArrayList()
    )

    override fun copy(): ConcurrentMutableTokenTable {
        return ConcurrentManualAddTokenTable(category, tokenList.copy(), _symbols.toMutableList())
    }
}
