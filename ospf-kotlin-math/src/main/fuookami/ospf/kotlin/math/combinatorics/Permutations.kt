package fuookami.ospf.kotlin.math.combinatorics

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*

import fuookami.ospf.kotlin.utils.parallel.ChannelGuard
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedSendChannelException
import org.apache.logging.log4j.kotlin.logger
import java.util.Collections.swap

// A Counting QuickPerm Algorithm
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

fun <T> permuteSequence(input: List<T>): Sequence<List<T>> = sequence {
    yieldAll(permuteSequence(input, input.size))
}

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

@OptIn(DelicateCoroutinesApi::class)
fun <T> permuteAsync(
    input: List<T>,
    scope: CoroutineScope = GlobalScope
): ChannelGuard<List<T>> {
    val logger = logger("Permutations")

    val promise = Channel<List<T>>(Channel.UNLIMITED)
    scope.launch(Dispatchers.Default) {
        try {
            val a = input.toList()
            val p = input.indices.map { 0 }.toMutableList()
            if (!promise.isClosedForSend) {
                promise.send(a.toList())
            }

            var i = 1;
            while (i < input.size && !promise.isClosedForSend) {
                if (p[i] < i) {
                    val j = i % 2 * p[i]
                    swap(a, i, j)
                    if (promise.isClosedForSend) {
                        break
                    }
                    promise.send(a.toList())
                    p[i] += 1;
                    i = 1
                } else {
                    p[i] = 0;
                    ++i;
                }
            }
        } catch (e: ClosedSendChannelException) {
            logger.debug { "Permutation generation was stopped by controller." }
        } catch (e: Exception) {
            logger.debug { "Permutation generation Error ${e.message}" }
        } finally {
            promise.close()
        }
    }
    return ChannelGuard(promise)
}




