/**
 * Token 列表，管理变量与求解器索引之间的映射关系。 / Token lists managing the mapping between variables and solver indices.
*/
package fuookami.ospf.kotlin.core.token

import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.concept.Copyable
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.core.variable.*

/**
 * 通用抽象 token 列表，带有实数类型参数 T。 / Generic abstract token list with real type parameter T.
 *
 * @param T 数值类型 / The number type
*/
sealed class AbstractTokenList<T : RealNumber<T>> : AutoCloseable {

    /** 所有已注册的 token 集合 / Collection of all registered tokens */
    abstract val tokens: Collection<Token<T>>

    /** 按求解器索引排序的 token 列表 / Token list sorted by solver index */
    abstract val tokensInSolver: List<Token<T>>

    /** 是否存在已缓存的求解结果 / Whether cached solve results exist */
    open val cachedSolution: Boolean get() = tokens.any { it.resultFlt64 != null }

    /**
     * 按求解器索引获取 token / Gets token by solver index
     *
     * @param index 求解器索引 / Solver index
     * @return 对应的 token / The corresponding token
    */
    operator fun get(index: Int): Token<T> {
        return find(index)!!
    }

    private val cache = HashMap<Token<T>, Int?>()

    /** 清除 token 索引缓存 / Clears the token index cache */
    protected fun clearIndexCache() {
        cache.clear()
    }

    /**
     * 查找 token 的求解器索引 / Finds the solver index of a token
     *
     * @param token 要查找的 token / The token to look up
     * @return 求解器索引，未找到返回 null / Solver index, or null if not found
    */
    open fun indexOf(token: Token<T>): Int? {
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

    /**
     * 查找变量项的求解器索引 / Finds the solver index of a variable item
     *
     * @param item 变量项 / The variable item
     * @return 求解器索引，未找到返回 null / Solver index, or null if not found
    */
    open fun indexOf(item: AbstractVariableItem<*, *>): Int? {
        return find(item)?.let { indexOf(it) }
    }

    /**
     * 按变量项查找对应的 token / Finds the token corresponding to a variable item
     *
     * @param item 变量项 / The variable item
     * @return 对应的 token，未找到返回 null / The corresponding token, or null if not found
    */
    abstract fun find(item: AbstractVariableItem<*, *>): Token<T>?

    /**
     * 按求解器索引查找 token / Finds a token by solver index
     *
     * @param index 求解器索引 / Solver index
     * @return 对应的 token，未找到返回 null / The corresponding token, or null if not found
    */
    fun find(index: Int): Token<T>? {
        return if (tokensInSolver.isNotEmpty() && index in tokensInSolver.indices) {
            tokensInSolver[index]
        } else {
            tokens.find { it.solverIndex == index }
        }
    }

    /**
     * 按索引列表设置求解结果 / Sets solve results by index list
     *
     * @param solution 按索引排列的求解结果列表 / Solve result list ordered by index
    */
    open fun setSolution(solution: List<T>) {
        assert(solution.size >= tokens.size)
        for ((index, token) in tokensInSolver.withIndex()) {
            token.setResult(solution[index])
        }
    }

    /**
     * 按变量映射设置求解结果 / Sets solve results by variable map
     *
     * @param solution 变量到值的映射 / Mapping from variables to values
    */
    open fun setSolution(solution: Map<AbstractVariableItem<*, *>, T>) {
        for ((variable, value) in solution) {
            find(variable)?.setResult(value)
        }
    }

    /**
     * 按索引列表设置求解器原始结果 / Sets raw solver results by index list
     *
     * @param solution 按索引排列的原始结果列表 / Raw result list ordered by index
    */
    open fun setSolverSolution(solution: List<Flt64>) {
        assert(solution.size >= tokens.size)
        for ((index, token) in tokensInSolver.withIndex()) {
            token._result = solution[index]
        }
    }

    /**
     * 按变量映射设置求解器原始结果 / Sets raw solver results by variable map
     *
     * @param solution 变量到原始值的映射 / Mapping from variables to raw values
    */
    open fun setSolverSolution(solution: Map<AbstractVariableItem<*, *>, Flt64>) {
        for ((variable, value) in solution) {
            find(variable)?._result = value
        }
    }

    /** 清除所有求解结果 / Clears all solve results */
    open fun clearSolution() {
        for (token in tokens) {
            token._result = null
        }
    }

    /** 关闭并释放内部缓存 / Closes and releases internal cache */
    override fun close() {
        cache.clear()
    }
}

/**
 * 不可变 token 列表，线程安全地管理已有的 token 集合。 / Immutable token list managing an existing token collection in a thread-safe manner.
 *
 * @param T 数值类型 / The number type
 * @property list 变量键到 token 的映射 / Mapping from variable keys to tokens
*/
@OptIn(ExperimentalStdlibApi::class)
class TokenList<T : RealNumber<T>>(
    val list: Map<VariableItemKey, Token<T>>
) : AbstractTokenList<T>() {

    /**
     * 从可变 token 列表构造不可变副本 / Constructs an immutable copy from a mutable token list
     *
     * @param tokens 源可变 token 列表 / Source mutable token list
    */
    constructor(tokens: MutableTokenList<T>) : this(tokens.list.toMap())

    init {
        list.forEach { (_, value) ->
            value.refreshCallbacks[this] = {
                synchronized(lock) {
                    _cachedSolution = tokens.any { it.resultFlt64 != null }
                }
            }
        }
    }

    override val tokens by list::values

    private val lock = Any()

    override val tokensInSolver: List<Token<T>> by lazy {
        tokens.sortedBy { it.solverIndex }
    }
    private var _cachedSolution: Boolean? = null
    override val cachedSolution: Boolean
        get() {
            return synchronized(lock) {
                if (_cachedSolution == null) {
                    _cachedSolution = tokens.any { it.resultFlt64 != null }
                }
                _cachedSolution!!
            }
        }

    override fun find(item: AbstractVariableItem<*, *>): Token<T>? {
        return list[item.key]
    }

    override fun setSolution(solution: List<T>) {
        synchronized(lock) {
            assert(solution.size >= tokensInSolver.size)
            for ((index, token) in tokensInSolver.withIndex()) {
                token.setResult(solution[index])
            }
            _cachedSolution = tokens.any { it.resultFlt64 != null }
        }
    }

    override fun setSolution(solution: Map<AbstractVariableItem<*, *>, T>) {
        synchronized(lock) {
            for ((variable, value) in solution) {
                find(variable)?.setResult(value)
            }
            _cachedSolution = tokens.any { it.resultFlt64 != null }
        }
    }

    override fun setSolverSolution(solution: List<Flt64>) {
        synchronized(lock) {
            assert(solution.size >= tokensInSolver.size)
            for ((index, token) in tokensInSolver.withIndex()) {
                token.__result = solution[index]
            }
            _cachedSolution = tokens.any { it.resultFlt64 != null }
        }
    }

    override fun setSolverSolution(solution: Map<AbstractVariableItem<*, *>, Flt64>) {
        synchronized(lock) {
            for ((variable, value) in solution) {
                find(variable)?.__result = value
            }
            _cachedSolution = tokens.any { it.resultFlt64 != null }
        }
    }

    override fun clearSolution() {
        synchronized(lock) {
            for (token in tokens) {
                token.__result = null
            }
            _cachedSolution = tokens.any { it.resultFlt64 != null }
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
 * 可添加 token 的集合接口。 / Interface for token-addable collections.
 *
 * @param T 数值类型 / The number type
*/
interface AddableTokenCollection<T : RealNumber<T>> {

    /**
     * 添加单个变量项的 token / Adds a token for a single variable item
     *
     * @param item 变量项 / The variable item
     * @return 操作结果 / Operation result
    */
    fun add(item: AbstractVariableItem<*, *>): Try

    /**
     * 批量添加变量项的 token / Adds tokens for multiple variable items
     *
     * @param items 变量项集合 / The variable item collection
     * @return 操作结果 / Operation result
     *
     * 失败时集合必须保持调用前状态；该操作具有 all-or-nothing 语义。
     * The collection must remain unchanged on failure; this operation is all-or-nothing.
     */
    fun add(items: Iterable<AbstractVariableItem<*, *>>): Try
}

/**
 * 可变 token 列表的抽象基类。 / Abstract base class for mutable token lists.
 *
 * @param T 数值类型 / The number type
*/
abstract class AbstractMutableTokenList<T : RealNumber<T>> : AbstractTokenList<T>(), AddableTokenCollection<T> {

    /**
     * 移除变量项对应的 token / Removes the token corresponding to a variable item
     *
     * @param item 要移除的变量项 / The variable item to remove
    */
    abstract fun remove(item: AbstractVariableItem<*, *>)

    /** 刷新内部状态并清除求解结果 / Refreshes internal state and clears solve results */
    open fun flush() {}

    /**
     * 创建 token 状态快照；不支持快照的自定义实现返回 null。
     * Creates a token-state snapshot; custom implementations without snapshot support return null.
     */
    internal open fun snapshotState(): TokenListSnapshot<T>? = null

    /**
     * 恢复 token 状态快照 / Restores a token-state snapshot.
     *
     * 该能力供需要事务语义的核心注册流程使用。
     * This is used by core registration flows that require transaction semantics.
     */
    internal open fun restoreState(snapshot: TokenListSnapshot<T>): Try {
        return Failed(
            ErrorCode.ApplicationError,
            "当前 token 列表不支持状态恢复。 / This token list does not support state restoration."
        )
    }
}

/**
 * 可变 token 列表的精确状态快照。 / Exact state snapshot for a mutable token list.
 *
 * 快照包含 token 身份、下一个 solver index、缓存标记和回调，
 * 从而可以恢复替换 token 而不只删除新增 token。
 * The snapshot includes token identity, the next solver index, cache state, and callbacks,
 * so replaced tokens can be restored instead of only removing newly added tokens.
 */
internal data class TokenListSnapshot<T : RealNumber<T>>(
    val list: Map<VariableItemKey, Token<T>>,
    val nextSolverIndex: Int,
    val cachedSolution: Boolean?,
    val callbacks: List<Pair<Token<T>, ((Boolean) -> Unit)?>>
)

/**
 * 可变 token 列表的密封基类，支持 token 的增删和求解结果管理。 / Sealed base class for mutable token lists, supporting token add/remove and solution management.
 *
 * @param T 数值类型 / The number type
 * @param list 变量键到 token 的可变映射 / Mutable mapping from variable keys to tokens
 * @param checkTokenExisted 是否检查 token 已存在 / Whether to check token existence
 * @param currentIndex 当前分配的求解器索引 / Current allocated solver index
*/
sealed class MutableTokenList<T : RealNumber<T>>(
    internal val list: MutableMap<VariableItemKey, Token<T>> = HashMap(),
    protected val checkTokenExisted: Boolean = System.getProperty("env", "prod") != "prod",
    protected var currentIndex: Int = 0
) : AbstractMutableTokenList<T>(), Copyable<MutableTokenList<T>> {
    override val tokens by list::values

    /** 线程同步锁 / Thread synchronization lock */
    protected val lock = Any()

    private lateinit var _tokensInSolver: List<Token<T>>
    override val tokensInSolver: List<Token<T>>
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
                    _cachedSolution = tokens.any { it.resultFlt64 != null }
                }
                _cachedSolution!!
            }
        }

    override fun snapshotState(): TokenListSnapshot<T> {
        return synchronized(lock) {
            val snapshot = list.toMap()
            val listOwner = this as AbstractTokenList<T>
            TokenListSnapshot(
                list = snapshot,
                nextSolverIndex = currentIndex,
                cachedSolution = _cachedSolution,
                callbacks = snapshot.values.map { token ->
                    token to token.refreshCallbacks[listOwner]
                }
            )
        }
    }

    override fun restoreState(snapshot: TokenListSnapshot<T>): Try {
        return synchronized(lock) {
            try {
                val listOwner = this as AbstractTokenList<T>
                for (token in list.values) {
                    if (snapshot.list.values.none { original -> original === token }) {
                        token.refreshCallbacks.remove(listOwner)
                    }
                }
                for ((token, callback) in snapshot.callbacks) {
                    if (callback == null) {
                        token.refreshCallbacks.remove(listOwner)
                    } else {
                        token.refreshCallbacks[listOwner] = callback
                    }
                }
                list.clear()
                list.putAll(snapshot.list)
                currentIndex = snapshot.nextSolverIndex
                invalidateTokensInSolver()
                clearIndexCache()
                _cachedSolution = snapshot.cachedSolution
                ok
            } catch (error: RuntimeException) {
                val detail = error.message?.takeIf { it.isNotBlank() }
                    ?: error::class.simpleName
                    ?: "unknown runtime error"
                Failed(
                    ErrorCode.ApplicationError,
                    "恢复 token 状态失败：$detail / Token state restoration failed: $detail"
                )
            }
        }
    }

    /** 清除按求解器排序的 token 缓存 / Invalidates the solver-order token cache. */
    protected fun invalidateTokensInSolver() {
        _tokensInSolver = ArrayList()
    }

    /** 创建带当前列表回调的 token / Creates a token with the current-list refresh callback. */
    protected fun createToken(item: AbstractVariableItem<*, *>): Token<T> {
        return Token(item, currentIndex, mutableMapOf(this as AbstractTokenList<T> to {
            synchronized(lock) {
                _cachedSolution = tokens.any { it.resultFlt64 != null }
            }
        }))
    }

    /** 将 token 注册异常转换为失败结果 / Convert a token-registration exception into a failure result. */
    private fun runtimeRegistrationFailure(error: RuntimeException): Try {
        val detail = error.message?.takeIf { it.isNotBlank() } ?: error::class.simpleName ?: "unknown runtime error"
        return Failed(
            ErrorCode.ApplicationError,
            "注册 token 失败：$detail / Token registration failed: $detail"
        )
    }

    /** 恢复 token 列表及其回调状态 / Restore token-list contents and callback state. */
    private fun rollbackTo(
        originalList: Map<VariableItemKey, Token<T>>,
        originalIndex: Int,
        originalCachedSolution: Boolean?,
        originalCallbacks: List<Pair<Token<T>, ((Boolean) -> Unit)?>>
    ) {
        val listOwner = this as AbstractTokenList<T>
        for (token in list.values) {
            if (originalList.values.none { it === token }) {
                token.refreshCallbacks.remove(listOwner)
            }
        }
        for ((token, callback) in originalCallbacks) {
            if (callback == null) {
                token.refreshCallbacks.remove(listOwner)
            } else {
                token.refreshCallbacks[listOwner] = callback
            }
        }
        list.clear()
        list.putAll(originalList)
        currentIndex = originalIndex
        invalidateTokensInSolver()
        clearIndexCache()
        _cachedSolution = originalCachedSolution
    }

    override fun add(item: AbstractVariableItem<*, *>): Try {
        return synchronized(lock) {
            val originalList = list.toMap()
            val originalIndex = currentIndex
            val originalCachedSolution = _cachedSolution
            val listOwner = this as AbstractTokenList<T>
            val originalCallbacks = originalList.values.map { token ->
                token to token.refreshCallbacks[listOwner]
            }
            try {
                addOne(item)
            } catch (error: RuntimeException) {
                rollbackTo(
                    originalList = originalList,
                    originalIndex = originalIndex,
                    originalCachedSolution = originalCachedSolution,
                    originalCallbacks = originalCallbacks
                )
                runtimeRegistrationFailure(error)
            }
        }
    }

    private fun addOne(
        item: AbstractVariableItem<*, *>,
        addedTokens: MutableList<Token<T>>? = null
    ): Try {
        if (checkTokenExisted && list.containsKey(item.key)) {
            return Failed(code = ErrorCode.TokenExisted)
        }
        val token = createToken(item)
        val replacedToken = list.put(item.key, token)
        replacedToken?.refreshCallbacks?.remove(this as AbstractTokenList<T>)
        addedTokens?.add(token)
        ++currentIndex
        invalidateTokensInSolver()
        clearIndexCache()
        if (replacedToken?.resultFlt64 != null) {
            _cachedSolution = tokens.any { it.resultFlt64 != null }
        }
        return ok
    }

    override fun add(items: Iterable<AbstractVariableItem<*, *>>): Try {
        return synchronized(lock) {
            val originalList = list.toMap()
            val originalIndex = currentIndex
            val originalCachedSolution = _cachedSolution
            val addedTokens = ArrayList<Token<T>>()
            val listOwner = this as AbstractTokenList<T>
            val originalCallbacks = originalList.values.map { token ->
                token to token.refreshCallbacks[listOwner]
            }

            fun rollback() {
                for (token in addedTokens) {
                    token.refreshCallbacks.remove(listOwner)
                }
                for ((token, callback) in originalCallbacks) {
                    if (callback == null) {
                        token.refreshCallbacks.remove(listOwner)
                    } else {
                        token.refreshCallbacks[listOwner] = callback
                    }
                }
                list.clear()
                list.putAll(originalList)
                currentIndex = originalIndex
                invalidateTokensInSolver()
                clearIndexCache()
                _cachedSolution = originalCachedSolution
            }

            try {
                for (item in items) {
                    when (val result = addOne(item, addedTokens)) {
                        is Ok -> {}

                        is Failed -> {
                            rollback()
                            return@synchronized Failed(result.error)
                        }

                        is Fatal -> {
                            rollback()
                            return@synchronized Fatal(result.errors)
                        }
                    }
                }
            } catch (error: RuntimeException) {
                rollback()
                return@synchronized runtimeRegistrationFailure(error)
            }
            ok
        }
    }

    override fun remove(item: AbstractVariableItem<*, *>) {
        synchronized(lock) {
            invalidateTokensInSolver()
            val removedToken = list.remove(item.key)
            removedToken?.refreshCallbacks?.remove(this as AbstractTokenList<T>)
            clearIndexCache()
            if (removedToken?.resultFlt64 != null) {
                _cachedSolution = tokens.any { it.resultFlt64 != null }
            }
        }
    }

    override fun flush() {
        invalidateTokensInSolver()
        clearSolution()
    }

    override fun setSolution(solution: List<T>) {
        synchronized(lock) {
            assert(solution.size >= tokensInSolver.size)
            for ((index, token) in tokensInSolver.withIndex()) {
                token.setResult(solution[index])
                _cachedSolution = tokens.any { it.resultFlt64 != null }
            }
        }
    }

    override fun setSolution(solution: Map<AbstractVariableItem<*, *>, T>) {
        synchronized(lock) {
            for ((variable, value) in solution) {
                find(variable)?.setResult(value)
            }
            _cachedSolution = tokens.any { it.resultFlt64 != null }
        }
    }

    override fun setSolverSolution(solution: List<Flt64>) {
        synchronized(lock) {
            assert(solution.size >= tokensInSolver.size)
            for ((index, token) in tokensInSolver.withIndex()) {
                token.__result = solution[index]
                _cachedSolution = tokens.any { it.resultFlt64 != null }
            }
        }
    }

    override fun setSolverSolution(solution: Map<AbstractVariableItem<*, *>, Flt64>) {
        synchronized(lock) {
            for ((variable, value) in solution) {
                find(variable)?.__result = value
            }
            _cachedSolution = tokens.any { it.resultFlt64 != null }
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
            value.refreshCallbacks.remove(this as AbstractTokenList<T>)
        }
        super.close()
        list.clear()
        _tokensInSolver = emptyList()
    }
}

/**
 * 自动 token 列表，查询时若 token 不存在则自动创建。 / Auto token list that creates tokens on-the-fly when queried and not found.
 *
 * @param T 数值类型 / The number type
 * @param list 变量键到 token 的可变映射 / Mutable mapping from variable keys to tokens
 * @param checkTokenExisted 是否检查 token 已存在 / Whether to check token existence
 * @param currentIndex 当前分配的求解器索引 / Current allocated solver index
*/
class AutoTokenList<T : RealNumber<T>> private constructor(
    list: MutableMap<VariableItemKey, Token<T>>,
    checkTokenExisted: Boolean,
    currentIndex: Int
) : MutableTokenList<T>(
    list = list,
    checkTokenExisted = checkTokenExisted,
    currentIndex = currentIndex
) {
    companion object {
        /**
         * 从已有 token 列表创建自动 token 列表 / Creates an auto token list from an existing token list
         *
         * @param tokenList 源 token 列表 / Source token list
         * @param checkTokenExisted 是否检查 token 已存在 / Whether to check token existence
         * @return 新的自动 token 列表 / New auto token list
        */
        operator fun <T : RealNumber<T>> invoke(
            tokenList: AbstractTokenList<T>,
            checkTokenExisted: Boolean
        ): MutableTokenList<T> {
            return AutoTokenList<T>(
                list = tokenList.tokens.associateBy { it.key }.toMutableMap(),
                checkTokenExisted = checkTokenExisted,
                currentIndex = tokenList.tokens.maxOf { it.solverIndex } + 1
            )
        }
    }

    /**
     * 创建空的自动 token 列表 / Creates an empty auto token list
     *
     * @param checkTokenExisted 是否检查 token 已存在 / Whether to check token existence
    */
    constructor(checkTokenExisted: Boolean) : this(
        list = HashMap(),
        checkTokenExisted = checkTokenExisted,
        currentIndex = 0
    )

    override fun copy(): MutableTokenList<T> {
        return AutoTokenList<T>(
            list = list.toMutableMap(),
            checkTokenExisted = checkTokenExisted,
            currentIndex = currentIndex
        )
    }

    override fun find(item: AbstractVariableItem<*, *>): Token<T> {
        return synchronized(lock) {
            val token = list[item.key] ?: createToken(item).also {
                list[item.key] = it
                ++currentIndex
                invalidateTokensInSolver()
                clearIndexCache()
            }
            _cachedSolution = tokens.any { it.resultFlt64 != null }
            token
        }
    }
}

/**
 * 手动 token 列表，变量需显式添加后才能查询。 / Manual token list where variables must be explicitly added before querying.
 *
 * @param T 数值类型 / The number type
 * @param list 变量键到 token 的可变映射 / Mutable mapping from variable keys to tokens
 * @param checkTokenExisted 是否检查 token 已存在 / Whether to check token existence
 * @param currentIndex 当前分配的求解器索引 / Current allocated solver index
*/
class ManualTokenList<T : RealNumber<T>> private constructor(
    list: MutableMap<VariableItemKey, Token<T>>,
    checkTokenExisted: Boolean,
    currentIndex: Int
) : MutableTokenList<T>(
    list = list,
    checkTokenExisted = checkTokenExisted,
    currentIndex = currentIndex
) {
    companion object {
        /**
         * 从已有 token 列表创建手动 token 列表 / Creates a manual token list from an existing token list
         *
         * @param tokenList 源 token 列表 / Source token list
         * @param checkTokenExisted 是否检查 token 已存在 / Whether to check token existence
         * @return 新的手动 token 列表 / New manual token list
        */
        operator fun <T : RealNumber<T>> invoke(
            tokenList: AbstractTokenList<T>,
            checkTokenExisted: Boolean
        ): MutableTokenList<T> {
            return ManualTokenList<T>(
                list = tokenList.tokens.associateBy { it.key }.toMutableMap(),
                checkTokenExisted = checkTokenExisted,
                currentIndex = tokenList.tokens.maxOf { it.solverIndex } + 1
            )
        }
    }

    /**
     * 创建空的手动 token 列表 / Creates an empty manual token list
     *
     * @param checkTokenExisted 是否检查 token 已存在 / Whether to check token existence
    */
    constructor(checkTokenExisted: Boolean) : this(
        list = HashMap(),
        checkTokenExisted = checkTokenExisted,
        currentIndex = 0
    )

    override fun copy(): MutableTokenList<T> {
        return ManualTokenList<T>(
            list = list.toMutableMap(),
            checkTokenExisted = checkTokenExisted,
            currentIndex = currentIndex
        )
    }

    override fun find(item: AbstractVariableItem<*, *>): Token<T>? {
        return list[item.key]
    }
}
