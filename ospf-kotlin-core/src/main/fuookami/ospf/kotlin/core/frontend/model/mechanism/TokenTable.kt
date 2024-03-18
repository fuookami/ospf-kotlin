package fuookami.ospf.kotlin.core.frontend.model.mechanism

import java.util.*
import kotlin.collections.*
import io.michaelrocks.bimap.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*

class RepeatedSymbolError(
    val repeatedSymbol: Symbol<*, *>,
    val symbol: Symbol<*, *>
) : Throwable() {
    override val message get() = "Repeated \"${symbol.name}\", old: $repeatedSymbol, new: $symbol."
}

sealed interface AbstractTokenTable<Cell : MonomialCell<Cell, C>, C : Category> {
    val tokenList: AbstractTokenList
    val tokens: Collection<Token> get() = tokenList.tokens
    val tokenIndexMap: BiMap<Token, Int> get() = tokenList.tokenIndexMap
    val symbols: Collection<Symbol<Cell, C>>

    fun find(item: AbstractVariableItem<*, *>): Token? {
        return tokenList.find(item)
    }

    fun find(index: Int): Token? {
        return tokenList.find(index)
    }

    fun setSolution(solution: List<Flt64>) {
        tokenList.setSolution(solution)
    }

    fun setSolution(solution: Map<AbstractVariableItem<*, *>, Flt64>) {
        tokenList.setSolution(solution)
    }

    fun clearSolution() {
        tokenList.clearSolution()
    }
}

data class TokenTable<Cell : MonomialCell<Cell, C>, C : Category>(
    override val tokenList: TokenList,
    override val symbols: List<Symbol<Cell, C>>
) : AbstractTokenTable<Cell, C> {
    constructor(tokenTable: MutableTokenTable<Cell, C>) : this(
        tokenList = TokenList(tokenTable.tokenList),
        symbols = tokenTable.symbols.toList()
    )

    override val tokens by tokenList::tokens
}

sealed class MutableTokenTable<Cell : MonomialCell<Cell, C>, C : Category>(
    override val tokenList: MutableTokenList,
    protected val _symbols: MutableList<Symbol<Cell, C>> = ArrayList()
) : Copyable<MutableTokenTable<Cell, C>>, AbstractTokenTable<Cell, C> {
    private val _symbolsMap: MutableMap<String, Symbol<Cell, C>> = _symbols.associateBy { it.name }.toMutableMap()
    override val symbols by ::_symbols

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

    fun add(symbol: Symbol<Cell, C>): Try {
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
    fun add(symbols: Iterable<Symbol<Cell, C>>): Try {
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

    fun remove(symbol: Symbol<Cell, C>) {
        _symbols.remove(symbol)
        _symbolsMap.remove(symbol.name)
    }
}

typealias LinearAbstractTokenTable = AbstractTokenTable<LinearMonomialCell, Linear>
typealias LinearTokenTable = TokenTable<LinearMonomialCell, Linear>
typealias LinearMutableTokenTable = MutableTokenTable<LinearMonomialCell, Linear>

typealias QuadraticAbstractTokenTable = AbstractTokenTable<QuadraticMonomialCell, Quadratic>
typealias QuadraticTokenTable = TokenTable<QuadraticMonomialCell, Quadratic>
typealias QuadraticMutableTokenTable = MutableTokenTable<QuadraticMonomialCell, Quadratic>

class AutoAddTokenTable<Cell : MonomialCell<Cell, C>, C : Category> private constructor(
    tokenList: MutableTokenList,
    symbols: List<Symbol<Cell, C>>
) : MutableTokenTable<Cell, C>(tokenList, symbols.toMutableList()) {
    constructor() : this(
        tokenList = AutoAddTokenTokenList(),
        symbols = ArrayList()
    )

    override fun copy(): MutableTokenTable<Cell, C> {
        return AutoAddTokenTable(tokenList.copy(), _symbols.toMutableList())
    }
}

class ManualAddTokenTable<Cell : MonomialCell<Cell, C>, C : Category>(
    tokenList: MutableTokenList,
    symbols: List<Symbol<Cell, C>>
) : MutableTokenTable<Cell, C>(tokenList, symbols.toMutableList()) {
    constructor() : this(
        tokenList = ManualAddTokenTokenList(),
        symbols = ArrayList()
    )

    override fun copy(): MutableTokenTable<Cell, C> {
        return ManualAddTokenTable(tokenList.copy(), _symbols.toMutableList())
    }
}

typealias LinearAutoAddTokenTable = AutoAddTokenTable<LinearMonomialCell, Linear>
typealias LinearManualAddTokenTable = ManualAddTokenTable<LinearMonomialCell, Linear>

typealias QuadraticAutoAddTokenTable = AutoAddTokenTable<QuadraticMonomialCell, Quadratic>
typealias QuadraticManualAddTokenTable = ManualAddTokenTable<QuadraticMonomialCell, Quadratic>
