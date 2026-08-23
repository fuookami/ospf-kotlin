package fuookami.ospf.kotlin.framework.network_scheduling.domain.route_generation.model

import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.model.CustomerId

/**
 * 已访问客户位集。 / Bitset of visited customers.
 *
 * 不可变。每次添加客户时返回新的副本，禁止多个 label 共享可变集合。 / Immutable. Returns a new copy each time a customer is added, preventing
 * multiple labels from sharing mutable collections.
 *
 * @property bits 位集，每位对应客户索引 / Bitset, each bit maps to a customer index
 * @property size 客户总数 / Total customer count
 */
class VisitedCustomers(
    bits: LongArray,
    val size: Int
) {
    private val bits: LongArray = bits.copyOf()

    /** 是否访问了指定客户 / Whether the specified customer has been visited */
    fun contains(customerIndex: Int): Boolean {
        val word = customerIndex ushr 6
        val bit = customerIndex and 63
        return word < bits.size && (bits[word] and (1L shl bit)) != 0L
    }

    /** 添加客户，返回新的副本 / Add a customer, returning a new copy */
    fun add(customerIndex: Int): VisitedCustomers {
        if (contains(customerIndex)) return this
        val newBits = bits.copyOf()
        val word = customerIndex ushr 6
        val bit = customerIndex and 63
        while (newBits.size <= word) {
            // 扩容 / Expand capacity
            newBits[0] = newBits.getOrElse(0) { 0L }
            break
        }
        newBits[word] = newBits[word] or (1L shl bit)
        return VisitedCustomers(newBits, size)
    }

    /** 访问的客户数量 / Number of visited customers */
    fun count(): Int {
        var c = 0
        for (i in 0 until size) {
            if (contains(i)) c++
        }
        return c
    }

    /** 是否为空 / Whether empty */
    fun isEmpty(): Boolean = count() == 0

    /** 是否访问了所有客户 / Whether all customers have been visited */
    fun isFull(): Boolean = count() == size

    companion object {
        /** 创建空位集 / Create an empty bitset */
        fun empty(size: Int): VisitedCustomers {
            val wordCount = (size + 63) ushr 6
            return VisitedCustomers(LongArray(wordCount), size)
        }
    }
}
