package fuookami.ospf.kotlin.core.frontend.variable

import fuookami.ospf.kotlin.core.frontend.model.mechanism.AutoAddTokenTable
import fuookami.ospf.kotlin.core.frontend.model.mechanism.MutableTokenTable
import java.util.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.functional.*
import io.michaelrocks.bimap.*
import kotlin.collections.HashMap

private fun tokenIndexMap(tokens: Collection<Token>): BiMap<Token, Int> {
    val ret = HashBiMap<Token, Int>()
    for ((index, token) in tokens.toList().sortedBy { it.solverIndex }.withIndex()) {
        ret[token] = index
    }
    return ret
}

sealed interface AbstractTokenList {
    val tokens: Collection<Token>
    val tokenIndexMap: BiMap<Token, Int>

    fun find(item: AbstractVariableItem<*, *>): Token?

    fun find(index: Int): Token? {
        return tokenIndexMap.inverse[index]
    }

    fun indexOf(item: AbstractVariableItem<*, *>): Int? {
        return find(item)?.let { tokenIndexMap[it] }
    }

    fun setSolution(solution: List<Flt64>) {
        assert(solution.size == tokens.size)
        val tokenIndexMap = tokenIndexMap(tokens)
        for (token in tokens) {
            token._result = solution[tokenIndexMap[token]!!]
        }
    }

    fun setSolution(solution: Map<AbstractVariableItem<*, *>, Flt64>) {
        for ((variable, value) in solution) {
            find(variable)?._result = value
        }
    }

    fun clearSolution() {
        for (token in tokens) {
            token._result = null
        }
    }
}

class TokenList(
    val list: Map<VariableItemKey, Token>
) : AbstractTokenList {
    constructor(tokens: MutableTokenList) : this(tokens.list.toMap())

    override val tokens by list::values
    override val tokenIndexMap: BiMap<Token, Int> by lazy { tokenIndexMap(tokens) }

    override fun find(item: AbstractVariableItem<*, *>): Token? {
        return list[item.key]
    }
}

sealed class MutableTokenList(
    internal val list: MutableMap<VariableItemKey, Token> = HashMap(),
    protected var currentIndex: Int = 0
) : AbstractTokenList, Copyable<MutableTokenList> {
    override val tokens by list::values

    private lateinit var _tokenIndexMap: BiMap<Token, Int>
    override val tokenIndexMap: BiMap<Token, Int>
        get() {
            if (!::_tokenIndexMap.isInitialized || _tokenIndexMap.isEmpty()) {
                _tokenIndexMap = tokenIndexMap(tokens)
            }
            return _tokenIndexMap
        }

    fun add(item: AbstractVariableItem<*, *>): Try {
        _tokenIndexMap = HashBiMap()
        if (list.containsKey(item.key)) {
            return Failed(Err(ErrorCode.TokenExisted))
        }
        list[item.key] = Token(item, currentIndex)
        ++currentIndex
        return Ok(success)
    }

    fun add(items: Iterable<AbstractVariableItem<*, *>>): Try {
        _tokenIndexMap = HashBiMap()
        for (item in items) {
            if (list.containsKey(item.key)) {
                return Failed(Err(ErrorCode.TokenExisted))
            }
            list[item.key] = Token(item, currentIndex)
            ++currentIndex
        }
        return Ok(success)
    }

    fun remove(item: AbstractVariableItem<*, *>) {
        _tokenIndexMap = HashBiMap()
        list.remove(item.key)
    }
}

class AutoAddTokenTokenList private constructor(
    list: MutableMap<VariableItemKey, Token>,
    currentIndex: Int
) : MutableTokenList(list, currentIndex) {
    constructor() : this(
        list = HashMap(),
        currentIndex = 0
    )

    override fun copy(): MutableTokenList {
        return AutoAddTokenTokenList(list.toMutableMap(), currentIndex)
    }

    override fun find(item: AbstractVariableItem<*, *>): Token {
        return list.getOrPut(item.key) { Token(item, currentIndex) }
    }
}

class ManualAddTokenTokenList private constructor(
    list: MutableMap<VariableItemKey, Token>,
    currentIndex: Int
) : MutableTokenList(list, currentIndex) {
    constructor() : this(
        list = HashMap(),
        currentIndex = 0
    )

    override fun copy(): MutableTokenList {
        return ManualAddTokenTokenList(list.toMutableMap(), currentIndex)
    }

    override fun find(item: AbstractVariableItem<*, *>): Token? {
        return list[item.key]
    }
}
