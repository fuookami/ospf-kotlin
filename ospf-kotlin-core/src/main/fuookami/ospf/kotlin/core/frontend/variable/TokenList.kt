package fuookami.ospf.kotlin.core.frontend.variable

import fuookami.ospf.kotlin.utils.math.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

sealed class TokenList {
    private var currentIndex: Int = 0
    protected val list: HashMap<ItemKey, Token> = HashMap()
    val tokens: Collection<Token> get() = list.values

    private var _solverIndexMap: MutableMap<Int, Int> = hashMapOf()
    internal val solverIndexMap: Map<Int, Int>
        get() {
            if (this._solverIndexMap.isEmpty()) {
               _solverIndexMap = this.solverIndexMap()
            }
            return _solverIndexMap
        }

    abstract fun find(item: Item<*, *>): Token?

    fun add(item: Item<*, *>): TokenList {
        if (_solverIndexMap.isNotEmpty()) {
            _solverIndexMap.clear()
        }
        list[item.key] = Token(item, currentIndex)
        ++currentIndex
        return this
    }

    fun add(items: Combination<*, *, *>): TokenList {
        items.iterator().forEach { add(it!!) }
        return this
    }

    fun add(items: CombinationView<*, *>): TokenList {
        items.iterator().forEach { add(it!!) }
        return this
    }

    fun remove(item: Item<*, *>) {
        if (_solverIndexMap.isNotEmpty()) {
            _solverIndexMap.clear()
        }
        list.remove(item.key)
    }

    fun setResults(result: List<Flt64>): TokenList {
        assert(result.size == tokens.size)
        val solverIndexes = solverIndexMap
        for (token in tokens) {
            token.result = result[solverIndexes[token.solverIndex]!!]
        }
        return this
    }

    fun clearResults(): TokenList {
        for (token in tokens) {
            token.result = null
        }
        return this
    }

    private fun solverIndexMap(): MutableMap<Int, Int> {
        val solverIndexes = ArrayList<Int>()
        tokens.forEach { solverIndexes.add(it.solverIndex) }
        solverIndexes.sort()

        val ret = TreeMap<Int, Int>()
        solverIndexes.withIndex().forEach { (index, solverIndex) -> ret[solverIndex] = index }
        return ret
    }
}

class AutoAddTokenTokenList: TokenList() {
    override fun find(item: Item<*, *>): Token? {
        add(item)
        return list[item.key]
    }
}

class ManualAddTokenTokenList: TokenList() {
    override fun find(item: Item<*, *>): Token? {
        return list[item.key]
    }
}
