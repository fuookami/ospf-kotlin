package fuookami.ospf.kotlin.core.frontend.variable

import fuookami.ospf.kotlin.utils.math.*

sealed class TokenList {
    private var currentIndex: Int = 0
    protected val list: HashMap<ItemKey, Token> = HashMap()
    val tokens: Collection<Token> get() = list.values

    internal val solverIndexMap: Map<Int, Int>
        get() {
            val solverIndexes = ArrayList<Int>()
            for (token in tokens) {
                solverIndexes.add(token.solverIndex)
            }
            solverIndexes.sort()

            val map = HashMap<Int, Int>()
            for (i in 0 until solverIndexes.size) {
                map[solverIndexes[i]] = i
            }
            return map
        }

    abstract fun find(item: Item<*, *>): Token?

    fun add(item: Item<*, *>): TokenList {
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
        list.remove(item.key)
    }

    fun setResults(result: List<Flt64>): TokenList {
        assert(result.size == tokens.size)
        for (token in tokens) {
            token.result = result[solverIndexMap[token.solverIndex]!!]
        }
        return this
    }

    fun clearResults(): TokenList {
        for (token in tokens) {
            token.result = null
        }
        return this
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
