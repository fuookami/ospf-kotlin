/**
 * Permutations（排列算法）
 * Permutations Algorithm
 *
 * 提供排列生成算法，用于生成元素的所有可能排列顺序。
 * Provides permutation generation algorithms for generating all possible orderings of elements.
 *
 * 主要功能：
 * Main features:
 * - permute: 生成全排列或部分排列 / Generate full permutations or partial permutations (permutations of subsets)
 * - permuteSequence: 惰性序列生成排列 / Lazy sequence generation of permutations
 * - permuteCount: 计算排列数 P(n, k) / Calculate permutation count P(n, k)
 * - permuteAsync: 异步生成排列（使用协程通道）/ Async permutation generation (using coroutine channels)
 *
 * 使用 QuickPerm 算法实现高效排列生成。
 * Uses QuickPerm algorithm for efficient permutation generation.
 *
 * 应用场景：调度问题、排序优化、序列搜索等。
 * Applications: scheduling problems, ordering optimization, sequence search, etc.
 */
package fuookami.ospf.kotlin.math.combinatorics

import java.util.Collections.swap
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.apache.logging.log4j.kotlin.logger
import fuookami.ospf.kotlin.utils.parallel.ChannelGuard

/** 使用 QuickPerm 算法生成输入列表的所有全排列 / Generate all full permutations of input list using QuickPerm algorithm */
fun <T> permute(
    input: List<T>,
    callBack: ((List<T>) -> Unit)? = null,
    stopped: ((List<T>) -> Boolean)? = null
): List<List<T>> {
    val a = input.toList()
    val p = input.indices.map { 0 }.toMutableList()

    val perms = ArrayList<List<T>>()
    perms.add(a.toList())
    callBack?.invoke(a.toList())
    if (stopped?.invoke(a.toList()) == true) {
        return perms
    }

    var i = 1;
    while (i < input.size) {
        if (p[i] < i) {
            val j = i % 2 * p[i]
            swap(a, i, j)
            perms.add(a.toList())
            callBack?.invoke(a.toList())
            if (stopped?.invoke(a) == true) {
                return perms
            }
            p[i] += 1;
            i = 1
        } else {
            p[i] = 0;
            ++i;
        }
    }
    return perms
}

/** 计算排列数 P(n, choose) / Calculate permutation count P(n, choose) */
fun permuteCount(n: Int, choose: Int = n): Long {
    if (choose < 0 || choose > n) {
        return 0L
    }
    var value = 1L
    for (i in 0 until choose) {
        value *= (n - i).toLong()
    }
    return value
}

/** 惰性序列生成所有全排列 / Lazy sequence generation of all full permutations */
fun <T> permuteSequence(input: List<T>): Sequence<List<T>> = sequence {
    yieldAll(permuteSequence(input, input.size))
}

/** 惰性序列生成指定大小的排列 / Lazy sequence generation of permutations of specified size */
fun <T> permuteSequence(input: List<T>, choose: Int): Sequence<List<T>> = sequence {
    if (choose < 0 || choose > input.size) {
        return@sequence
    }
    if (choose == 0) {
        yield(emptyList())
        return@sequence
    }

    val used = BooleanArray(input.size)
    val path = ArrayList<T>(choose)

    suspend fun SequenceScope<List<T>>.dfs() {
        if (path.size == choose) {
            yield(path.toList())
            return
        }
        for (i in input.indices) {
            if (used[i]) {
                continue
            }
            used[i] = true
            path.add(input[i])
            dfs()
            path.removeAt(path.lastIndex)
            used[i] = false
        }
    }
    dfs()
}

/** 生成指定大小的所有排列 / Generate all permutations of specified size */
fun <T> permute(
    input: List<T>,
    choose: Int,
    callBack: ((List<T>) -> Unit)? = null,
    stopped: ((List<T>) -> Boolean)? = null
): List<List<T>> {
    val result = ArrayList<List<T>>()
    for (permutation in permuteSequence(input, choose)) {
        result.add(permutation)
        callBack?.invoke(permutation)
        if (stopped?.invoke(permutation) == true) {
            break
        }
    }
    return result
}

/** 异步生成所有排列，通过协程通道返回 / Async permutation generation via coroutine channel */
fun <T> permuteAsync(
    input: List<T>,
    scope: CoroutineScope = combinatoricsAsyncScope
): ChannelGuard<List<T>> {
    val logger = logger("Permutations")

    val promise = Channel<List<T>>(Channel.UNLIMITED)
    scope.launch(Dispatchers.Default) {
        try {
            val a = input.toList()
            val p = input.indices.map { 0 }.toMutableList()
            if (promise.trySend(a.toList()).isFailure) {
                return@launch
            }

            var i = 1;
            while (i < input.size) {
                if (p[i] < i) {
                    val j = i % 2 * p[i]
                    swap(a, i, j)
                    if (promise.trySend(a.toList()).isFailure) {
                        break
                    }
                    p[i] += 1;
                    i = 1
                } else {
                    p[i] = 0;
                    ++i;
                }
            }
        } catch (e: Exception) {
            logger.debug { "Permutation generation Error ${e.message}" }
        } finally {
            promise.close()
        }
    }
    return ChannelGuard(promise)
}
