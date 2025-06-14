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

@OptIn(ExperimentalStdlibApi::class)
class TokenList(
    val list: Map<VariableItemKey, Token>
) : AbstractTokenList, AutoCloseable {
    constructor(tokens: MutableTokenList) : this(tokens.list.toMap())

    init {
        list.forEach { (key, value) ->
            value.refreshCallbacks[this] = {
                synchronized(lock) {
                    _cachedSolution = tokens.any { it.result != null }
                }
            }
        }
    }

    override val tokens by list::values

    private val lock = Any()

    override val tokenIndexMap: BiMap<Token, Int> by lazy {
        tokenIndexMap(tokens)
    }
    private var _cachedSolution: Boolean? = null
    override val cachedSolution: Boolean
        get() {
            return synchronized(lock) {
                if (_cachedSolution == null) {
                    _cachedSolution = tokens.any { it.result != null }
                }
                _cachedSolution!!
            }
        }

    override fun find(item: AbstractVariableItem<*, *>): Token? {
        return list[item.key]
    }

    override fun setSolution(solution: List<Flt64>) {
        synchronized(lock) {
            assert(solution.size >= tokens.size)
            val tokenIndexMap = tokenIndexMap(tokens)
            for (token in tokens) {
                token.__result = solution[tokenIndexMap[token]!!]
            }
            _cachedSolution = tokens.any { it.result != null }
        }
    }

    override fun setSolution(solution: Map<AbstractVariableItem<*, *>, Flt64>) {
        synchronized(lock) {
            for ((variable, value) in solution) {
                find(variable)?.__result = value
            }
            _cachedSolution = tokens.any { it.result != null }
        }
    }

    override fun clearSolution() {
        synchronized(lock) {
            for (token in tokens) {
                token.__result = null
            }
            _cachedSolution = tokens.any { it.result != null }
        }
    }

    override fun close() {
        list.forEach { (_, value) ->
            value.refreshCallbacks.remove(this)
        }
    }
}

@OptIn(ExperimentalStdlibApi::class)
sealed class MutableTokenList(
    internal val list: MutableMap<VariableItemKey, Token> = HashMap(),
    protected var currentIndex: Int = 0
) : AbstractTokenList, Copyable<MutableTokenList>, AutoCloseable {
    override val tokens by list::values

    protected val lock = Any()

    private lateinit var _tokenIndexMap: BiMap<Token, Int>
    override val tokenIndexMap: BiMap<Token, Int>
        get() {
            if (!::_tokenIndexMap.isInitialized || _tokenIndexMap.isEmpty()) {
                _tokenIndexMap = tokenIndexMap(tokens)
            }
            return _tokenIndexMap
        }
    protected var _cachedSolution: Boolean? = null
    override val cachedSolution: Boolean
        get() {
            return synchronized(lock) {
                if (_cachedSolution == null) {
                    _cachedSolution = tokens.any { it.result != null }
                }
                _cachedSolution!!
            }
        }

    fun add(item: AbstractVariableItem<*, *>): Try {
        synchronized(lock) {
            if (list.containsKey(item.key)) {
                return Failed(Err(ErrorCode.TokenExisted))
            }
            list[item.key] = Token(item, currentIndex, mutableMapOf(this to {
                synchronized(lock) {
                    _cachedSolution = tokens.any { it.result != null }
                }
            }))
            _cachedSolution = tokens.any { it.result != null }
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
                list[item.key] = Token(item, currentIndex, mutableMapOf(this to {
                    synchronized(lock) {
                        _cachedSolution = tokens.any { it.result != null }
                    }
                }))
                ++currentIndex
            }
            _cachedSolution = tokens.any { it.result != null }
        }
        return ok
    }

    fun remove(item: AbstractVariableItem<*, *>) {
        synchronized(lock) {
            _tokenIndexMap = HashBiMap()
            list.remove(item.key)
            _cachedSolution = tokens.any { it.result != null }
        }
    }

    override fun setSolution(solution: List<Flt64>) {
        synchronized(lock) {
            assert(solution.size >= tokens.size)
            val tokenIndexMap = tokenIndexMap(tokens)
            for (token in tokens) {
                token.__result = solution[tokenIndexMap[token]!!]
                _cachedSolution = tokens.any { it.result != null }
            }
        }
    }

    override fun setSolution(solution: Map<AbstractVariableItem<*, *>, Flt64>) {
        synchronized(lock) {
            for ((variable, value) in solution) {
                find(variable)?.__result = value
            }
            _cachedSolution = tokens.any { it.result != null }
        }
    }

    override fun clearSolution() {
        synchronized(lock) {
            for (token in tokens) {
                token.__result = null
            }
            _cachedSolution = tokens.any { it.result != null }
        }
    }

    override fun close() {
        list.forEach { (_, value) ->
            value.refreshCallbacks.remove(this)
        }
    }
}

class AutoTokenList private constructor(
    list: MutableMap<VariableItemKey, Token>,
    currentIndex: Int
) : MutableTokenList(list, currentIndex) {
    companion object {
        operator fun invoke(tokenList: AbstractTokenList): MutableTokenList {
            return AutoTokenList(
                tokenList.tokens.associateBy { it.key }.toMutableMap(),
                tokenList.tokens.maxOf { it.solverIndex } + 1
            )
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
        return synchronized(lock) {
            val token = list.getOrPut(item.key) {
                Token(item, currentIndex, mutableMapOf(this to {
                    synchronized(super.lock) {
                        super._cachedSolution = tokens.any { it.result != null }
                    }
                }))
            }
            super._cachedSolution = tokens.any { it.result != null }
            token
        }
    }
}

class ManualTokenList private constructor(
    list: MutableMap<VariableItemKey, Token>,
    currentIndex: Int
) : MutableTokenList(list, currentIndex) {
    companion object {
        operator fun invoke(tokenList: AbstractTokenList): MutableTokenList {
            return ManualTokenList(
                tokenList.tokens.associateBy { it.key }.toMutableMap(),
                tokenList.tokens.maxOf { it.solverIndex } + 1
            )
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
