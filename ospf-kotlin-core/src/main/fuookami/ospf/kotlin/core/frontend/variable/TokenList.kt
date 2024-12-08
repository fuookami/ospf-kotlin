package fuookami.ospf.kotlin.core.frontend.variable

import kotlin.collections.*
import io.michaelrocks.bimap.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.functional.*

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
    val cachedSolution: Boolean get() = tokens.any { it.result != null }

    operator fun get(index: Int): Token {
        return tokenIndexMap.inverse[index] ?: tokens.find { it.solverIndex == index }!!
    }

    fun indexOf(token: Token): Int {
        return tokenIndexMap[token] ?: token.solverIndex
    }

    fun indexOf(item: AbstractVariableItem<*, *>): Int? {
        return find(item)?.let { indexOf(it) }
    }

    fun find(item: AbstractVariableItem<*, *>): Token?

    fun find(index: Int): Token? {
        return tokenIndexMap.inverse[index]
    }

    fun setSolution(solution: List<Flt64>) {
        assert(solution.size >= tokens.size)
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

    private val lock = Any()

    private lateinit var _tokenIndexMap: BiMap<Token, Int>
    override val tokenIndexMap: BiMap<Token, Int>
        get() {
            if (!::_tokenIndexMap.isInitialized || _tokenIndexMap.isEmpty()) {
                _tokenIndexMap = tokenIndexMap(tokens)
            }
            return _tokenIndexMap
        }
    override val cachedSolution: Boolean
        get() {
            return synchronized(lock) {
                tokens.any { it.result != null }
            }
        }

    fun add(item: AbstractVariableItem<*, *>): Try {
        synchronized(lock) {
            if (list.containsKey(item.key)) {
                return Failed(Err(ErrorCode.TokenExisted))
            }
            list[item.key] = Token(item, currentIndex)
            ++currentIndex
        }
        return ok
    }

    fun add(items: Iterable<AbstractVariableItem<*, *>>): Try {
        synchronized(lock) {
            for (item in items) {
                if (list.containsKey(item.key)) {
                    return Failed(Err(ErrorCode.TokenExisted))
                }
                list[item.key] = Token(item, currentIndex)
                ++currentIndex
            }
        }
        return ok
    }

    fun remove(item: AbstractVariableItem<*, *>) {
        synchronized(lock) {
            _tokenIndexMap = HashBiMap()
            list.remove(item.key)
        }
    }
}

class AutoTokenList private constructor(
    list: MutableMap<VariableItemKey, Token>,
    currentIndex: Int
) : MutableTokenList(list, currentIndex) {
    companion object {
        operator fun invoke(tokenList: AbstractTokenList): MutableTokenList {
            return AutoTokenList(tokenList.tokens.associateBy { it.key }.toMutableMap(), tokenList.tokens.maxOf { it.solverIndex } + 1)
        }
    }

    constructor() : this(
        list = HashMap(),
        currentIndex = 0
    )

    override fun copy(): MutableTokenList {
        return AutoTokenList(list.toMutableMap(), currentIndex)
    }

    override fun find(item: AbstractVariableItem<*, *>): Token {
        return list.getOrPut(item.key) { Token(item, currentIndex) }
    }
}

class ManualTokenList private constructor(
    list: MutableMap<VariableItemKey, Token>,
    currentIndex: Int
) : MutableTokenList(list, currentIndex) {
    companion object {
        operator fun invoke(tokenList: AbstractTokenList): MutableTokenList {
            return ManualTokenList(tokenList.tokens.associateBy { it.key }.toMutableMap(), tokenList.tokens.maxOf { it.solverIndex } + 1)
        }
    }

    constructor() : this(
        list = HashMap(),
        currentIndex = 0
    )

    override fun copy(): MutableTokenList {
        return ManualTokenList(list.toMutableMap(), currentIndex)
    }

    override fun find(item: AbstractVariableItem<*, *>): Token? {
        return list[item.key]
    }
}
