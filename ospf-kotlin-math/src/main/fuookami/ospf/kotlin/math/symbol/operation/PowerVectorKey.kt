/**
 * 幂向量键
 * Power Vector Key
 *
 * 提供规范单项式幂向量的优化键实现，用于高效的同类项合并。
 * 支持两种模式：稠密模式（卌IntArray）和稀疏模式（两个 IntArray），
 * 根据稀疏度自动选择最优模式。
 * Provides optimized key implementation for canonical monomial power vectors,
 * used for efficient like-term combination.
 * Supports two modes: dense mode (single IntArray) and sparse mode (two IntArrays),
 * automatically selecting the optimal mode based on sparsity.
 */
package fuookami.ospf.kotlin.math.symbol.operation

import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.algebra.number.Int32

/**
 * Optimized key for canonical monomial powers comparison.
 *
 * Supports two modes based on sparsity:
 * - Dense mode: single IntArray of length = total symbols (fast for small n)
 * - Sparse mode: two IntArrays (indices + powers) (fast for large n, few symbols)
 *
 * Sparsity threshold: 0.5 (powers.size / totalSymbols)
 */
class PowerVectorKey private constructor(
    private val denseVec: IntArray?,       // dense mode: [power0, power1, ...]
    private val sparseIndices: IntArray?,  // sparse mode: [symbolIndex0, symbolIndex1, ...]
    private val sparsePowers: IntArray?,   // sparse mode: [power0, power1, ...]
    val hash: Int
) {
    companion object {
        /**
         * Sparsity threshold for mode selection.
         * When powers.size / totalSymbols < threshold, use sparse mode.
         */
        const val SPARSITY_THRESHOLD = 0.5

        /**
         * Create dense mode key (single IntArray).
         * Use when sparsity >= threshold or totalSymbols is small (<= 5).
         */
        fun dense(vec: IntArray): PowerVectorKey {
            return PowerVectorKey(
                denseVec = vec,
                sparseIndices = null,
                sparsePowers = null,
                hash = vec.contentHashCode()
            )
        }

        /**
         * Create sparse mode key (two IntArrays).
         * Use when sparsity < threshold and totalSymbols > 5.
         *
         * IMPORTANT: indices must be sorted ascending for normalization.
         */
        fun sparse(indices: IntArray, powers: IntArray): PowerVectorKey {
            require(indices.size == powers.size) { "Indices and powers size mismatch" }
            var h = 1
            for (i in indices.indices) {
                h = 31 * h + indices[i]
                h = 31 * h + powers[i]
            }
            return PowerVectorKey(
                denseVec = null,
                sparseIndices = indices,
                sparsePowers = powers,
                hash = h
            )
        }

        /**
         * Auto-select mode based on sparsity.
         *
         * @param powers Map of symbol to power
         * @param symbolIndex Map of symbol to its index in the order
         * @param totalSymbols Total number of unique symbols
         * @return PowerVectorKey with optimal mode
         */
        fun create(
            powers: Map<Symbol, Int32>,
            symbolIndex: Map<Symbol, Int>,
            totalSymbols: Int
        ): PowerVectorKey {
            val size = powers.size
            val sparsity = size.toDouble() / totalSymbols

            // Auto-select: dense for small n or high sparsity
            if (totalSymbols <= 5 || sparsity >= SPARSITY_THRESHOLD) {
                // Dense mode
                val vec = IntArray(totalSymbols)
                for ((s, p) in powers) {
                    vec[symbolIndex[s]!!] = p.toInt()
                }
                return dense(vec)
            } else {
                // Sparse mode
                val entries = powers.entries.sortedBy { symbolIndex[it.key] }
                val indices = IntArray(size) { symbolIndex[entries[it].key]!! }
                val powersArr = IntArray(size) { entries[it].value.toInt() }
                return sparse(indices, powersArr)
            }
        }

        /**
         * Compute hash for two IntArrays.
         */
        private fun computeSparseHash(indices: IntArray, powers: IntArray): Int {
            var h = 1
            for (i in indices.indices) {
                h = 31 * h + indices[i]
                h = 31 * h + powers[i]
            }
            return h
        }
    }

    val isDense: Boolean = denseVec != null
    val isSparse: Boolean = sparseIndices != null

    override fun hashCode(): Int = hash

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PowerVectorKey) return false
        if (hash != other.hash) return false  // Fast inequality check

        return when {
            isDense && other.isDense -> denseVec!!.contentEquals(other.denseVec!!)
            isSparse && other.isSparse ->
                sparseIndices!!.contentEquals(other.sparseIndices!!) &&
                sparsePowers!!.contentEquals(other.sparsePowers!!)
            else -> false  // Different modes cannot be equal
        }
    }

    /**
     * Reconstruct powers map from key.
     * Requires symbolList (index ↌symbol mapping).
     */
    fun toPowers(symbolList: List<Symbol>): Map<Symbol, Int32> {
        return when {
            isDense -> {
                val result = LinkedHashMap<Symbol, Int32>()
                for (i in denseVec!!.indices) {
                    if (denseVec[i] != 0) {
                        result[symbolList[i]] = Int32(denseVec[i])
                    }
                }
                result
            }
            isSparse -> {
                val result = LinkedHashMap<Symbol, Int32>()
                for (i in sparseIndices!!.indices) {
                    result[symbolList[sparseIndices[i]]] = Int32(sparsePowers!![i])
                }
                result
            }
            else -> emptyMap()
        }
    }

    override fun toString(): String {
        return when {
            isDense -> "PowerVectorKey(dense=${denseVec!!.contentToString()})"
            isSparse -> "PowerVectorKey(sparse=indices=${sparseIndices!!.contentToString()}, powers=${sparsePowers!!.contentToString()})"
            else -> "PowerVectorKey(empty)"
        }
    }
}