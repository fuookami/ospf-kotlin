package fuookami.ospf.kotlin.framework.network_scheduling.domain.route_generation.model

import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.model.CustomerId

/**
 * 不可达客户位集。 / Bitset of unreachable customers.
 *
 * ForbiddenCustomers = visited + 已证明不可达客户（Feillet 2004 标记）。
 * 不可变。每次添加时返回新的副本。 / ForbiddenCustomers = visited + customers proven unreachable (Feillet 2004 marking).
 * Immutable. Returns a new copy each time a customer is added.
 *
 * @property bits 位集 / Bitset
 * @property size 客户总数 / Total customer count
 */
class ForbiddenCustomers(
    bits: LongArray,
    val size: Int
) {
    private val bits: LongArray = bits.copyOf()

    /** 是否包含指定客户 / Whether the specified customer is forbidden */
    fun contains(customerIndex: Int): Boolean {
        val word = customerIndex ushr 6
        val bit = customerIndex and 63
        return word < bits.size && (bits[word] and (1L shl bit)) != 0L
    }

    /** 添加客户，返回新的副本 / Add a customer, returning a new copy */
    fun add(customerIndex: Int): ForbiddenCustomers {
        if (contains(customerIndex)) return this
        val newBits = bits.copyOf()
        val word = customerIndex ushr 6
        val bit = customerIndex and 63
        newBits[word] = newBits[word] or (1L shl bit)
        return ForbiddenCustomers(newBits, size)
    }

    /** 子集判定：this 是否为 other 的子集 / Subset check: whether this is a subset of other */
    fun isSubsetOf(other: ForbiddenCustomers): Boolean {
        for (i in bits.indices) {
            if (bits[i] and other.bits.getOrElse(i) { 0L }.inv() != 0L) {
                return false
            }
        }
        return true
    }

    /** 合并：返回包含两者所有客户的新位集 / Union: return a new bitset containing all customers from both */
    fun union(other: ForbiddenCustomers): ForbiddenCustomers {
        val newBits = LongArray(maxOf(bits.size, other.bits.size))
        for (i in bits.indices) newBits[i] = newBits[i] or bits[i]
        for (i in other.bits.indices) newBits[i] = newBits[i] or other.bits[i]
        return ForbiddenCustomers(newBits, size)
    }

    companion object {
        /** 从 VisitedCustomers 创建 / Create from VisitedCustomers */
        fun fromVisited(visited: VisitedCustomers, size: Int): ForbiddenCustomers {
            val wordCount = (size + 63) ushr 6
            val bits = LongArray(wordCount)
            for (i in 0 until size) {
                if (visited.contains(i)) {
                    val word = i ushr 6
                    val bit = i and 63
                    bits[word] = bits[word] or (1L shl bit)
                }
            }
            return ForbiddenCustomers(bits, size)
        }

        /** 创建空位集 / Create an empty bitset */
        fun empty(size: Int): ForbiddenCustomers {
            val wordCount = (size + 63) ushr 6
            return ForbiddenCustomers(LongArray(wordCount), size)
        }
    }
}
