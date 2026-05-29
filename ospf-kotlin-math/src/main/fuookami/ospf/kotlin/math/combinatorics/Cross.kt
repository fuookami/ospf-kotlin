/**
 * Cross（笛卡尔积）
 * Cartesian Product (Cross Product)
 *
 * 提供笛卡尔积（交叉积）生成算法，用于计算多个集合的所有组合。
 * Provides Cartesian product (cross product) generation algorithms for computing all combinations across multiple sets.
 *
 * 主要功能：
 * Main features:
 * - cross: 计算多集合的笛卡尔积 / Calculate Cartesian product of multiple sets
 * - crossSequence: 惰性序列生成笛卡尔积 / Lazy sequence generation of Cartesian product
 * - crossCount: 计算笛卡尔积元素总数 / Calculate total count of Cartesian product elements
 * - cross2/cross3: 计算两个或三个集合的笛卡尔积 / Calculate Cartesian product of two or three sets
 * - crossAsync: 异步生成笛卡尔积（使用协程通道）/ Async Cartesian product generation (using coroutine channels)
 *
 * 应用场景：参数空间遍历、多维决策变量组合、状态空间搜索等。
 * Applications: parameter space traversal, multi-dimensional decision variable combinations, state space search, etc.
 */
package fuookami.ospf.kotlin.math.combinatorics

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import org.apache.logging.log4j.kotlin.*
import fuookami.ospf.kotlin.utils.parallel.*

/**
 * 计算多个集合的笛卡尔积
 * Calculate Cartesian product of multiple sets
 *
 * @param input 多个集合的列表 / List of multiple sets
 * @param callBack 每个组合生成时的回调函数（可选） / Callback function invoked for each generated combination (optional)
 * @param stopped 判断是否提前终止的函数（可选） / Function to determine whether to stop early (optional)
 * @return 笛卡尔积结果列表 / List of Cartesian product results
 */
fun <T> cross(
    input: List<List<T>>,
    callBack: ((List<T>) -> Unit)? = null,
    stopped: ((List<T>) -> Boolean)? = null
): List<List<T>> {
    val result = ArrayList<List<T>>()
    val n = input.size
    val indices = IntArray(n)
    for (i in 0 until n) {
        indices[i] = 0
    }
    while (true) {
        val row = ArrayList<T>()
        for (i in 0 until n) {
            row.add(input[i][indices[i]])
        }
        result.add(row)
        if (callBack != null) {
            callBack(row)
        }
        if (stopped != null && stopped(row)) {
            break
        }
        var i = n - 1
        while (i >= 0 && indices[i] == input[i].lastIndex) {
            i--
        }
        if (i < 0) {
            break
        }
        indices[i]++
        for (j in i + 1 until n) {
            indices[j] = 0
        }
    }
    return result
}

/**
 * 计算笛卡尔积的元素总数
 * Calculate total count of Cartesian product elements
 *
 * @param input 多个集合的列表 / List of multiple sets
 * @return 笛卡尔积的元素总数 / Total count of Cartesian product elements
 */
fun <T> crossCount(input: List<List<T>>): Long {
    if (input.isEmpty()) {
        return 0L
    }
    var value = 1L
    for (values in input) {
        if (values.isEmpty()) {
            return 0L
        }
        value *= values.size.toLong()
    }
    return value
}

/**
 * 惰性序列生成笛卡尔积
 * Lazy sequence generation of Cartesian product
 *
 * @param input 多个集合的列表 / List of multiple sets
 * @return 笛卡尔积的惰性序列 / Lazy sequence of Cartesian product
 */
fun <T> crossSequence(input: List<List<T>>): Sequence<List<T>> = sequence {
    if (input.isEmpty() || input.any { it.isEmpty() }) {
        return@sequence
    }
    val n = input.size
    val indices = IntArray(n)
    while (true) {
        yield(List(n) { input[it][indices[it]] })
        var i = n - 1
        while (i >= 0 && indices[i] == input[i].lastIndex) {
            i -= 1
        }
        if (i < 0) {
            break
        }
        indices[i] += 1
        for (j in i + 1 until n) {
            indices[j] = 0
        }
    }
}

/**
 * 计算两个集合的笛卡尔积，返回 Pair 列表
 * Calculate Cartesian product of two sets, returning list of Pairs
 *
 * @param lhs 左集合 / Left set
 * @param rhs 右集合 / Right set
 * @return 两个集合的笛卡尔积 Pair 列表 / List of Pairs representing the Cartesian product
 */
fun <A, B> cross2(lhs: List<A>, rhs: List<B>): List<Pair<A, B>> {
    return lhs.flatMap { l -> rhs.map { r -> l to r } }
}

/**
 * 惰性序列生成两个集合的笛卡尔积
 * Lazy sequence generation of Cartesian product of two sets
 *
 * @param lhs 左集合 / Left set
 * @param rhs 右集合 / Right set
 * @return 两个集合笛卡尔积的惰性序列 / Lazy sequence of Cartesian product of two sets
 */
fun <A, B> cross2Sequence(lhs: List<A>, rhs: List<B>): Sequence<Pair<A, B>> = sequence {
    for (l in lhs) {
        for (r in rhs) {
            yield(l to r)
        }
    }
}

/**
 * 计算三个集合的笛卡尔积，返回 Triple 列表
 * Calculate Cartesian product of three sets, returning list of Triples
 *
 * @param a 第一个集合 / First set
 * @param b 第二个集合 / Second set
 * @param c 第三个集合 / Third set
 * @return 三个集合的笛卡尔积 Triple 列表 / List of Triples representing the Cartesian product
 */
fun <A, B, C> cross3(a: List<A>, b: List<B>, c: List<C>): List<Triple<A, B, C>> {
    return a.flatMap { x ->
        b.flatMap { y ->
            c.map { z -> Triple(x, y, z) }
        }
    }
}

/**
 * 惰性序列生成三个集合的笛卡尔积
 * Lazy sequence generation of Cartesian product of three sets
 *
 * @param a 第一个集合 / First set
 * @param b 第二个集合 / Second set
 * @param c 第三个集合 / Third set
 * @return 三个集合笛卡尔积的惰性序列 / Lazy sequence of Cartesian product of three sets
 */
fun <A, B, C> cross3Sequence(a: List<A>, b: List<B>, c: List<C>): Sequence<Triple<A, B, C>> = sequence {
    for (x in a) {
        for (y in b) {
            for (z in c) {
                yield(Triple(x, y, z))
            }
        }
    }
}

/**
 * 异步生成笛卡尔积，通过协程通道返回
 * Async Cartesian product generation via coroutine channel
 *
 * @param input 多个集合的列表 / List of multiple sets
 * @param scope 协程作用域（默认使用组合异步作用域） / Coroutine scope (defaults to combinatorics async scope)
 * @return 通道守护，用于异步接收笛卡尔积结果 / Channel guard for receiving Cartesian product results asynchronously
 */
fun <T> crossAsync(
    input: List<List<T>>,
    scope: CoroutineScope = combinatoricsAsyncScope,
): ChannelGuard<List<T>> {
    val logger = logger("Cross")

    val promise = Channel<List<T>>(Channel.UNLIMITED)
    scope.launch(Dispatchers.Default) {
        try {
            val n = input.size
            val indices = IntArray(n)
            for (i in 0 until n) {
                indices[i] = 0
            }
            while (true) {
                val row = ArrayList<T>()
                for (i in 0 until n) {
                    row.add(input[i][indices[i]])
                }
                if (promise.trySend(row).isFailure) {
                    break
                }
                var i = n - 1
                while (i >= 0 && indices[i] == input[i].lastIndex) {
                    i--
                }
                if (i < 0) {
                    break
                }
                indices[i]++
                for (j in i + 1 until n) {
                    indices[j] = 0
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
