package fuookami.ospf.kotlin.core.frontend.model.mechanism

import java.util.concurrent.*
import kotlin.collections.*
import kotlinx.coroutines.*
import io.michaelrocks.bimap.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.operator.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*

class RepeatedSymbolError(
    val repeatedSymbol: Symbol,
    val symbol: Symbol
) : Throwable() {
    override val message get() = "Repeated \"${symbol.name}\", old: $repeatedSymbol, new: $symbol."
}

sealed interface AbstractTokenTable {
    val category: Category
    val tokenList: AbstractTokenList
    val tokens: Collection<Token> get() = tokenList.tokens
    val tokenIndexMap: BiMap<Token, Int> get() = tokenList.tokenIndexMap
    val symbols: Collection<Symbol>
    val cachedSolution: Boolean get() = tokenList.tokens.any { it._result != null }

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

    fun cached(symbol: Symbol, solution: List<Flt64>? = null): Boolean? {
        return null
    }
}

data class TokenTable(
    override val category: Category,
    override val tokenList: TokenList,
    override val symbols: List<Symbol>
) : AbstractTokenTable {
    constructor(tokenTable: MutableTokenTable) : this(
        category = tokenTable.category,
        tokenList = TokenList(tokenTable.tokenList),
        symbols = tokenTable.symbols.toList()
    )

    override val tokens by tokenList::tokens

    internal val cachedSymbolValue: MutableMap<Pair<Symbol, List<Flt64>?>, Flt64?> = ConcurrentHashMap()

    override fun flush() {
        cachedSymbolValue.clear()
    }
}

sealed class MutableTokenTable(
    override val category: Category,
    override val tokenList: MutableTokenList,
    protected val _symbols: MutableList<Symbol> = ArrayList()
) : Copyable<MutableTokenTable>, AbstractTokenTable {
    private val _symbolsMap: MutableMap<String, Symbol> = _symbols.associateBy { it.name }.toMutableMap()
    override val symbols by ::_symbols

    internal val cachedSymbolValue: MutableMap<Pair<Symbol, List<Flt64>?>, Flt64?> = ConcurrentHashMap()

    fun add(item: AbstractVariableItem<*, *>): Try {
        return tokenList.add(item)
    }

    @JvmName("addVariables")
    fun add(items: Iterable<AbstractVariableItem<*, *>>): Try {
        return tokenList.add(items)
    }

    fun remove(item: AbstractVariableItem<*, *>) {
        return tokenList.remove(item)
    }

    fun add(symbol: Symbol): Try {
        if ((symbol.operationCategory ord category) is Order.Greater) {
            return Failed(Err(ErrorCode.ApplicationError, "${symbol.name} over $category"))
        }

        if (_symbolsMap.containsKey(symbol.name)) {
            return Failed(
                ExErr(
                    code = ErrorCode.SymbolRepetitive,
                    value = RepeatedSymbolError(_symbolsMap[symbol.name]!!, symbol)
                )
            )
        } else {
            symbols.add(symbol)
            _symbolsMap[symbol.name] = symbol
        }
        return ok
    }

    @JvmName("addSymbols")
    fun add(symbols: Iterable<Symbol>): Try {
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

    fun remove(symbol: Symbol) {
        _symbols.remove(symbol)
        _symbolsMap.remove(symbol.name)
    }

    override fun flush() {
        cachedSymbolValue.clear()
    }

    override fun cached(symbol: Symbol, solution: List<Flt64>?): Boolean {
         return !cachedSymbolValue.containsKey(symbol to solution)
    }
}

suspend fun Collection<Symbol>.register(tokenTable: MutableTokenTable): Try {
    return coroutineScope {
        val completedSymbols = HashSet<Symbol>()
        var dependencies = this@register.associateWith { it.dependencies.toMutableSet() }.toMap()
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

            val jobs = readySymbols.map {
                launch {
                    it.prepare(tokenTable)
                }
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
        }

        ok
    }
}

class AutoAddTokenTable private constructor(
    category: Category,
    tokenList: MutableTokenList,
    symbols: List<Symbol>
) : MutableTokenTable(category, tokenList, symbols.toMutableList()) {
    companion object {
        operator fun invoke(tokenTable: AbstractTokenTable): MutableTokenTable {
            return AutoAddTokenTable(
                category = tokenTable.category,
                tokenList = AutoAddTokenTokenList(tokenTable.tokenList),
                symbols = tokenTable.symbols.toMutableList()
            )
        }
    }

    constructor(category: Category) : this(
        category = category,
        tokenList = AutoAddTokenTokenList(),
        symbols = ArrayList()
    )

    override fun copy(): MutableTokenTable {
        return AutoAddTokenTable(category, tokenList.copy(), _symbols.toMutableList())
    }
}

class ManualAddTokenTable private constructor(
    category: Category,
    tokenList: MutableTokenList,
    symbols: List<Symbol>
) : MutableTokenTable(category, tokenList, symbols.toMutableList()) {
    companion object {
        operator fun invoke(tokenTable: AbstractTokenTable): MutableTokenTable {
            return ManualAddTokenTable(
                category = tokenTable.category,
                tokenList = ManualAddTokenTokenList(tokenTable.tokenList),
                symbols = tokenTable.symbols.toMutableList()
            )
        }
    }

    constructor(category: Category) : this(
        category = category,
        tokenList = ManualAddTokenTokenList(),
        symbols = ArrayList()
    )

    override fun copy(): MutableTokenTable {
        return ManualAddTokenTable(category, tokenList.copy(), _symbols.toMutableList())
    }
}
