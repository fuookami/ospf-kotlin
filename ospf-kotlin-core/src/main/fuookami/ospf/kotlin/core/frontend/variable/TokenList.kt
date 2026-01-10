package fuookami.ospf.kotlin.core.frontend.variable

import kotlin.collections.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.functional.*

sealed class AbstractTokenList {
    abstract val tokens: Collection<Token>
    abstract val tokensInSolver: List<Token>
    open val cachedSolution: Boolean get() = tokens.any { it.result != null }

    operator fun get(index: Int): Token {
        return find(index)!!
    }

    private val cache = HashMap<Token, Int?>()

    open fun indexOf(token: Token): Int? {
        return if (tokensInSolver.isNotEmpty()) {
            cache.getOrPut(token) {
                tokensInSolver.indexOf(token).let {
                    if (it != -1) {
                        it
                    } else {
                        null
                    }
                }
            }
        } else {
            null
        }
    }

    open fun indexOf(item: AbstractVariableItem<*, *>): Int? {
        return find(item)?.let { indexOf(it) }
    }

    abstract fun find(item: AbstractVariableItem<*, *>): Token?

    fun find(index: Int): Token? {
        return if (tokensInSolver.isNotEmpty() && index in tokensInSolver.indices) {
            tokensInSolver[index]
        } else {
            tokens.find { it.solverIndex == index }
        }
    }

    open fun setSolution(solution: List<Flt64>) {
        assert(solution.size >= tokens.size)
        for ((index, token) in tokensInSolver.withIndex()) {
            token._result = solution[index]
        }
    }

    open fun setSolution(solution: Map<AbstractVariableItem<*, *>, Flt64>) {
        for ((variable, value) in solution) {
            find(variable)?._result = value
        }
    }

    open fun clearSolution() {
        for (token in tokens) {
            token._result = null
        }
    }
}

@OptIn(ExperimentalStdlibApi::class)
class TokenList(
    val list: Map<VariableItemKey, Token>
) : AbstractTokenList(), AutoCloseable {
    constructor(tokens: MutableTokenList) : this(tokens.list.toMap())

    init {
        list.forEach { (_, value) ->
            value.refreshCallbacks[this] = {
                synchronized(lock) {
                    _cachedSolution = tokens.any { it.result != null }
                }
            }
        }
    }

    override val tokens by list::values

    private val lock = Any()

    override val tokensInSolver: List<Token> by lazy {
        tokens.sortedBy { it.solverIndex }
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
            assert(solution.size >= tokensInSolver.size)
            for ((index, token) in tokensInSolver.withIndex()) {
                token.__result = solution[index]
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

interface AddableTokenCollection {
    fun add(item: AbstractVariableItem<*, *>): Try
    fun add(items: Iterable<AbstractVariableItem<*, *>>): Try
}

abstract class AbstractMutableTokenList : AbstractTokenList(), AddableTokenCollection {
    abstract fun remove(item: AbstractVariableItem<*, *>)
}

sealed class MutableTokenList(
    internal val list: MutableMap<VariableItemKey, Token> = HashMap(),
    protected val checkTokenExisted: Boolean = System.getProperty("env", "prod") != "prod",
    protected var currentIndex: Int = 0
) : AbstractMutableTokenList(), Copyable<MutableTokenList>, AutoCloseable {
    override val tokens by list::values

    protected val lock = Any()

    private lateinit var _tokensInSolver: List<Token>
    override val tokensInSolver: List<Token>
        get() {
            if (!::_tokensInSolver.isInitialized || _tokensInSolver.isEmpty()) {
                _tokensInSolver = tokens.sortedBy { it.solverIndex }
            }
            return _tokensInSolver
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

    override fun add(item: AbstractVariableItem<*, *>): Try {
        if (checkTokenExisted && list.containsKey(item.key)) {
            return Failed(Err(ErrorCode.TokenExisted))
        }
        list[item.key] = Token(item, currentIndex, mutableMapOf(this to {
            synchronized(lock) {
                _cachedSolution = tokens.any { it.result != null }
            }
        }))
        ++currentIndex
        return ok
    }

    override fun add(items: Iterable<AbstractVariableItem<*, *>>): Try {
        for (item in items) {
            if (checkTokenExisted && list.containsKey(item.key)) {
                return Failed(Err(ErrorCode.TokenExisted))
            }
            list[item.key] = Token(item, currentIndex, mutableMapOf(this to {
                synchronized(lock) {
                    _cachedSolution = tokens.any { it.result != null }
                }
            }))
            ++currentIndex
        }
        return ok
    }

    override fun remove(item: AbstractVariableItem<*, *>) {
        synchronized(lock) {
            _tokensInSolver = ArrayList()
            val removedToken = list.remove(item.key)
            if (removedToken?.result != null) {
                _cachedSolution = tokens.any { it.result != null }
            }
        }
    }

    internal fun flush() {
        _tokensInSolver = ArrayList()
        synchronized(lock) {
            for (token in tokens) {
                token.__result = null
            }
            _cachedSolution = false
        }
    }

    override fun setSolution(solution: List<Flt64>) {
        synchronized(lock) {
            assert(solution.size >= tokensInSolver.size)
            for ((index, token) in tokensInSolver.withIndex()) {
                token.__result = solution[index]
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
            _cachedSolution = false
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
    checkTokenExisted: Boolean,
    currentIndex: Int
) : MutableTokenList(list, checkTokenExisted, currentIndex) {
    companion object {
        operator fun invoke(
            tokenList: AbstractTokenList,
            checkTokenExisted: Boolean
        ): MutableTokenList {
            return AutoTokenList(
                list = tokenList.tokens.associateBy { it.key }.toMutableMap(),
                checkTokenExisted = checkTokenExisted,
                currentIndex = tokenList.tokens.maxOf { it.solverIndex } + 1
            )
        }
    }

    constructor(checkTokenExisted: Boolean) : this(
        list = HashMap(),
        checkTokenExisted = checkTokenExisted,
        currentIndex = 0
    )

    override fun copy(): MutableTokenList {
        return AutoTokenList(list.toMutableMap(), checkTokenExisted, currentIndex)
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
    checkTokenExisted: Boolean,
    currentIndex: Int
) : MutableTokenList(list, checkTokenExisted, currentIndex) {
    companion object {
        operator fun invoke(
            tokenList: AbstractTokenList,
            checkTokenExisted: Boolean
        ): MutableTokenList {
            return ManualTokenList(
                list = tokenList.tokens.associateBy { it.key }.toMutableMap(),
                checkTokenExisted = checkTokenExisted,
                currentIndex = tokenList.tokens.maxOf { it.solverIndex } + 1
            )
        }
    }

    constructor(checkTokenExisted: Boolean) : this(
        list = HashMap(),
        checkTokenExisted = checkTokenExisted,
        currentIndex = 0
    )

    override fun copy(): MutableTokenList {
        return ManualTokenList(list.toMutableMap(), checkTokenExisted, currentIndex)
    }

    override fun find(item: AbstractVariableItem<*, *>): Token? {
        return list[item.key]
    }
}
