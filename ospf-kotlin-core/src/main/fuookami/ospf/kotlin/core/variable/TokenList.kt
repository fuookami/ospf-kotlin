package fuookami.ospf.kotlin.core.variable

import fuookami.ospf.kotlin.utils.concept.Copyable
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64

/**
 * Generic abstract token list - phantom type parameter T for API signature.
 */
sealed class AbstractTokenListOf<T : RealNumber<T>> : AutoCloseable {
    abstract val tokens: Collection<TokenOf<T>>
    abstract val tokensInSolver: List<TokenOf<T>>
    open val cachedSolution: Boolean get() = tokens.any { it.result != null }

    operator fun get(index: Int): TokenOf<T> {
        return find(index)!!
    }

    private val cache = HashMap<TokenOf<T>, Int?>()

    open fun indexOf(token: TokenOf<T>): Int? {
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

    abstract fun find(item: AbstractVariableItem<*, *>): TokenOf<T>?

    fun find(index: Int): TokenOf<T>? {
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

    override fun close() {
        cache.clear()
    }
}

/**
 * Legacy typealias for Flt64-specific AbstractTokenList.
 */
typealias AbstractTokenList = AbstractTokenListOf<Flt64>

@OptIn(ExperimentalStdlibApi::class)
class TokenListOf<T : RealNumber<T>>(
    val list: Map<VariableItemKey, TokenOf<T>>
) : AbstractTokenListOf<T>() {
    constructor(tokens: MutableTokenListOf<T>) : this(tokens.list.toMap())

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

    override val tokensInSolver: List<TokenOf<T>> by lazy {
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

    override fun find(item: AbstractVariableItem<*, *>): TokenOf<T>? {
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
        super.close()
    }
}

/**
 * Legacy typealias for Flt64-specific TokenList.
 */
typealias TokenList = TokenListOf<Flt64>

interface AddableTokenCollectionOf<T : RealNumber<T>> {
    fun add(item: AbstractVariableItem<*, *>): Try
    fun add(items: Iterable<AbstractVariableItem<*, *>>): Try
}

/**
 * Legacy typealias for Flt64-specific AddableTokenCollection.
 */
typealias AddableTokenCollection = AddableTokenCollectionOf<Flt64>

abstract class AbstractMutableTokenListOf<T : RealNumber<T>> : AbstractTokenListOf<T>(), AddableTokenCollectionOf<T> {
    abstract fun remove(item: AbstractVariableItem<*, *>)
}

/**
 * Legacy typealias for Flt64-specific AbstractMutableTokenList.
 */
typealias AbstractMutableTokenList = AbstractMutableTokenListOf<Flt64>

sealed class MutableTokenListOf<T : RealNumber<T>>(
    internal val list: MutableMap<VariableItemKey, TokenOf<T>> = HashMap(),
    protected val checkTokenExisted: Boolean = System.getProperty("env", "prod") != "prod",
    protected var currentIndex: Int = 0
) : AbstractMutableTokenListOf<T>(), Copyable<MutableTokenListOf<T>> {
    override val tokens by list::values

    protected val lock = Any()

    private lateinit var _tokensInSolver: List<TokenOf<T>>
    override val tokensInSolver: List<TokenOf<T>>
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
            return Failed(code = ErrorCode.TokenExisted)
        }
        list[item.key] = TokenOf(item, currentIndex, mutableMapOf(this as AbstractTokenListOf<T> to {
            synchronized(lock) {
                _cachedSolution = tokens.any { it.result != null }
            }
        }))
        ++currentIndex
        return ok
    }

    override fun add(items: Iterable<AbstractVariableItem<*, *>>): Try {
        for (item in items) {
            when (val result = add(item)) {
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
        clearSolution()
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
            value.refreshCallbacks.remove(this as AbstractTokenListOf<T>)
        }
        super.close()
        list.clear()
        _tokensInSolver = emptyList()
    }
}

/**
 * Legacy typealias for Flt64-specific MutableTokenList.
 */
typealias MutableTokenList = MutableTokenListOf<Flt64>

class AutoTokenListOf<T : RealNumber<T>> private constructor(
    list: MutableMap<VariableItemKey, TokenOf<T>>,
    checkTokenExisted: Boolean,
    currentIndex: Int
) : MutableTokenListOf<T>(
    list = list,
    checkTokenExisted = checkTokenExisted,
    currentIndex = currentIndex
) {
    companion object {
        operator fun <T : RealNumber<T>> invoke(
            tokenList: AbstractTokenListOf<T>,
            checkTokenExisted: Boolean
        ): MutableTokenListOf<T> {
            return AutoTokenListOf<T>(
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

    override fun copy(): MutableTokenListOf<T> {
        return AutoTokenListOf<T>(
            list = list.toMutableMap(),
            checkTokenExisted = checkTokenExisted,
            currentIndex = currentIndex
        )
    }

    override fun find(item: AbstractVariableItem<*, *>): TokenOf<T> {
        return synchronized(lock) {
            val token = list.getOrPut(item.key) {
                TokenOf<T>(item, currentIndex, mutableMapOf(this as AbstractTokenListOf<T> to {
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

/**
 * Legacy typealias for Flt64-specific AutoTokenList.
 */
typealias AutoTokenList = AutoTokenListOf<Flt64>

class ManualTokenListOf<T : RealNumber<T>> private constructor(
    list: MutableMap<VariableItemKey, TokenOf<T>>,
    checkTokenExisted: Boolean,
    currentIndex: Int
) : MutableTokenListOf<T>(
    list = list,
    checkTokenExisted = checkTokenExisted,
    currentIndex = currentIndex
) {
    companion object {
        operator fun <T : RealNumber<T>> invoke(
            tokenList: AbstractTokenListOf<T>,
            checkTokenExisted: Boolean
        ): MutableTokenListOf<T> {
            return ManualTokenListOf<T>(
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

    override fun copy(): MutableTokenListOf<T> {
        return ManualTokenListOf<T>(
            list = list.toMutableMap(),
            checkTokenExisted = checkTokenExisted,
            currentIndex = currentIndex
        )
    }

    override fun find(item: AbstractVariableItem<*, *>): TokenOf<T>? {
        return list[item.key]
    }
}

/**
 * Legacy typealias for Flt64-specific ManualTokenList.
 */
typealias ManualTokenList = ManualTokenListOf<Flt64>