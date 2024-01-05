package fuookami.ospf.kotlin.core.frontend.variable

import java.util.*
import fuookami.ospf.kotlin.utils.math.*

sealed class TokenList {
    private var currentIndex: Int = 0
    protected val list: HashMap<ItemKey, Token> = HashMap()
    val tokens: Collection<Token> get() = list.values

    private var _solverIndexMap: MutableMap<Int, Int> = hashMapOf()
    internal val solverIndexMap: Map<Int, Int>
        get() {
            if (_solverIndexMap.isEmpty()) {
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
        for (item in items) {
            add(item!!)
        }
        return this
    }

    fun add(items: CombinationView<*, *>): TokenList {
        for (item in items) {
            add(item!!)
        }
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
            token._result = result[solverIndexes[token.solverIndex]!!]
        }
        return this
    }

    fun setResults(result: Map<Item<*, *>, Flt64>): TokenList {
        for ((variable, value) in result) {
            find(variable)?._result = value
        }
        return this
    }

    fun clearResults(): TokenList {
        for (token in tokens) {
            token._result = null
        }
        return this
    }

    private fun solverIndexMap(): MutableMap<Int, Int> {
        val solverIndexes = ArrayList<Int>()
        for (token in tokens) {
            solverIndexes.add(token.solverIndex)
        }
        solverIndexes.sort()

        val ret = TreeMap<Int, Int>()
        for ((index, solverIndex) in solverIndexes.withIndex()) {
            ret[solverIndex] = index
        }
        return ret
    }
}

class AutoAddTokenTokenList() : TokenList() {
    override fun find(item: Item<*, *>): Token? {
        add(item)
        return list[item.key]
    }
}

class ManualAddTokenTokenList() : TokenList() {
    override fun find(item: Item<*, *>): Token? {
        return list[item.key]
    }
}
