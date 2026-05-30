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
 * 规范单项式幂向量的优化键
 * Optimized key for canonical monomial powers comparison.
 *
 * 根据稀疏度支持两种模式：
 * - 稠密模式：长度等于总符号数的单个 IntArray（适用于符号数较少的情况）
 * - 稀疏模式：两个 IntArray（索引 + 幂次）（适用于符号数较多、单项式符号较少的情况）
 * 稀疏度阈值：0.5（powers.size / totalSymbols）。
 *
 * Supports two modes based on sparsity:
 * - Dense mode: single IntArray of length = total symbols (fast for small n)
 * - Sparse mode: two IntArrays (indices + powers) (fast for large n, few symbols)
 * Sparsity threshold: 0.5 (powers.size / totalSymbols)
 *
 * @property hash 预计算的哈希值 / Pre-computed hash value
 */
class PowerVectorKey private constructor(
    private val denseVec: IntArray?,       // dense mode: [power0, power1, ...]
    private val sparseIndices: IntArray?,  // sparse mode: [symbolIndex0, symbolIndex1, ...]
    private val sparsePowers: IntArray?,   // sparse mode: [power0, power1, ...]
    val hash: Int
) {
    companion object {
        /**
         * 模式选择的稀疏度阈值
         * Sparsity threshold for mode selection.
         *
         * 当 powers.size / totalSymbols < 阈值时使用稀疏模式。
         * When powers.size / totalSymbols < threshold, use sparse mode.
         */
        const val SPARSITY_THRESHOLD = 0.5

        /**
         * 创建稠密模式键（单个 IntArray）
         * Create dense mode key (single IntArray).
         *
         * 适用于稀疏度 >= 阈值或 totalSymbols 较小（<= 5）的情况。
         * Use when sparsity >= threshold or totalSymbols is small (<= 5).
         *
         * @param vec 幂次数组 / Power array
         * @return 稠密模式的 PowerVectorKey / PowerVectorKey in dense mode
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
         * 创建稀疏模式键（两个 IntArray）
         * Create sparse mode key (two IntArrays).
         *
         * 适用于稀疏度 < 阈值且 totalSymbols > 5 的情况。
         * 注意：indices 必须按升序排列以保证规范化。
         *
         * Use when sparsity < threshold and totalSymbols > 5.
         * IMPORTANT: indices must be sorted ascending for normalization.
         *
         * @param indices 符号索引数组（升序） / Symbol index array (ascending)
         * @param powers 对应的幂次数组 / Corresponding power array
         * @return 稀疏模式的 PowerVectorKey / PowerVectorKey in sparse mode
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
         * 根据稀疏度自动选择最优模式创建键
         * Auto-select mode based on sparsity.
         *
         * @param powers 符号到幂次的映射 / Map of symbol to power
         * @param symbolIndex 符号到顺序索引的映射 / Map of symbol to its index in the order
         * @param totalSymbols 唯一符号总数 / Total number of unique symbols
         * @return 最优模式的 PowerVectorKey / PowerVectorKey with optimal mode
         */
        fun create(
            powers: Map<Symbol, Int32>,
            symbolIndex: Map<Symbol, Int>,
            totalSymbols: Int
        ): PowerVectorKey {
            val size = powers.size
            val sparsity = size.toDouble() / totalSymbols

            // Auto-select: dense for small n or high sparsity / 自动选择：符号数少或稀疏度高时使用稠密模式
            if (totalSymbols <= 5 || sparsity >= SPARSITY_THRESHOLD) {
                // Dense mode / 稠密模式
                val vec = IntArray(totalSymbols)
                for ((s, p) in powers) {
                    vec[symbolIndex[s]!!] = p.toInt()
                }
                return dense(vec)
            } else {
                // Sparse mode / 稀疏模式
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

    /** 是否为稠密模式 / Whether this key is in dense mode */
    val isDense: Boolean = denseVec != null
    /** 是否为稀疏模式 / Whether this key is in sparse mode */
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
     * 从键重建幂次映射
     * Reconstruct powers map from key.
     *
     * 需要 symbolList（索引到符号的映射）。
     * Requires symbolList (index to symbol mapping).
     *
     * @param symbolList 索引到符号的映射列表 / Index to symbol mapping list
     * @return 符号到幂次的映射 / Map of symbol to power
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
