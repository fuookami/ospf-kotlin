/**
 * Combinations（组合算法）
 * Combinations Algorithm
 *
 * 提供组合生成算法，用于从集合中选取元素的所有可能组合。
 * Provides combination generation algorithms for selecting all possible combinations from a set.
 *
 * 主要功能：
 * Main features:
 * - combine: 生成所有组合或指定数量的组合 / Generate all combinations or combinations of specified size
 * - combineSequence: 惰性序列生成组合 / Lazy sequence generation of combinations
 * - combineCount: 计算组合数 C(n, k) / Calculate combination count C(n, k)
 * - combineAsync: 异步生成组合（使用协程通道）/ Async combination generation (using coroutine channels)
 *
 * 应用场景：优化问题遍历、约束求解、特征选择等。
 * Applications: optimization problem traversal, constraint solving, feature selection, etc.
 */
package fuookami.ospf.kotlin.math.combinatorics

import kotlin.math.min
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*
import fuookami.ospf.kotlin.utils.parallel.ChannelGuard
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.apache.logging.log4j.kotlin.logger

/** 生成输入列表的所有子集组合 / Generate all subset combinations of the input list */
fun <T> combine(
    input: List<T>,
    callBack: ((List<T>) -> Unit)? = null,
    stopped: ((List<T>) -> Boolean)? = null
): List<List<T>> {
    val result = ArrayList<List<T>>()
    val totalCombinations = 1 shl input.size
    for (i in 1 until totalCombinations) {
        val combination = ArrayList<T>()
        for (j in input.indices) {
            if (i and (1 shl j) != 0) {
                combination.add(input[j])
            }
        }
        result.add(combination)
        callBack?.invoke(combination)
        if (stopped?.invoke(combination) == true) {
            break
        }
    }
    return result
}

/** 计算组合数 C(n, choose) / Calculate combination count C(n, choose) */
fun combineCount(n: Int, choose: Int): Long {
    if (choose < 0 || choose > n) {
        return 0L
    }
    if (choose == 0 || choose == n) {
        return 1L
    }
    val k = min(choose, n - choose)
    var value = 1L
    for (i in 1..k) {
        value = (value * (n - k + i)) / i
    }
    return value
}

/** 惰性序列生成所有子集组合 / Lazy sequence generation of all subset combinations */
fun <T> combineSequence(input: List<T>): Sequence<List<T>> = sequence {
    for (k in 1..input.size) {
        yieldAll(combineSequence(input, k))
    }
}

/** 惰性序列生成指定大小的组合 / Lazy sequence generation of combinations of specified size */
fun <T> combineSequence(input: List<T>, choose: Int): Sequence<List<T>> = sequence {
    if (choose < 0 || choose > input.size) {
        return@sequence
    }
    if (choose == 0) {
        yield(emptyList())
        return@sequence
    }
    val indices = IntArray(choose) { it }
    while (true) {
        yield(indices.map { input[it] })
        var i = choose - 1
        while (i >= 0 && indices[i] == input.size - choose + i) {
            i -= 1
        }
        if (i < 0) {
            break
        }
        indices[i] += 1
        for (j in i + 1 until choose) {
            indices[j] = indices[j - 1] + 1
        }
    }
}

/** 生成指定大小的所有组合 / Generate all combinations of specified size */
fun <T> combine(
    input: List<T>,
    choose: Int,
    callBack: ((List<T>) -> Unit)? = null,
    stopped: ((List<T>) -> Boolean)? = null
): List<List<T>> {
    val result = ArrayList<List<T>>()
    for (combination in combineSequence(input, choose)) {
        result.add(combination)
        callBack?.invoke(combination)
        if (stopped?.invoke(combination) == true) {
            break
        }
    }
    return result
}

/** 异步生成所有子集组合，通过协程通道返回 / Async generation of all subset combinations via coroutine channel */
fun <T> combineAsync(
    input: List<T>,
    scope: CoroutineScope = combinatoricsAsyncScope,
): ChannelGuard<List<T>> {
    val logger = logger("Combinations")

    val promise = Channel<List<T>>(Channel.UNLIMITED)
    scope.launch(Dispatchers.Default) {
        try {
            val totalCombinations = 1 shl input.size
            for (i in 1 until totalCombinations) {
                val combination = ArrayList<T>()
                for (j in input.indices) {
                    if (i and (1 shl j) != 0) {
                        combination.add(input[j])
                    }
                }
                if (promise.trySend(combination).isFailure) {
                    break
                }
            }
        } catch (e: Exception) {
            logger.debug { "Combination generation Error ${e.message}" }
        } finally {
            promise.close()
        }
    }
    return ChannelGuard(promise)
}
