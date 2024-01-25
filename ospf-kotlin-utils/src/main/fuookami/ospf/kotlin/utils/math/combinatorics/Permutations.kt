package fuookami.ospf.kotlin.utils.math.combinatorics

import java.util.Collections.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import org.apache.logging.log4j.kotlin.*
import fuookami.ospf.kotlin.utils.parallel.*

// A Counting QuickPerm Algorithm
fun <T> permute(input: List<T>): List<List<T>> {
    val a = input.toList()
    val p = input.indices.map { 0 }.toMutableList()

    val perms = ArrayList<List<T>>()
    perms.add(a.toList())

    var i = 1;
    while (i < input.size) {
        if (p[i] < i) {
            val j = i % 2 * p[i]
            swap(a, i, j)
            perms.add(a.toList())
            p[i] += 1;
            i = 1
        } else {
            p[i] = 0;
            ++i;
        }
    }
    return perms
}

fun <T> permuteCallBack(
    input: List<T>,
    callBack: (List<T>) -> Unit,
    stopped: ((List<T>) -> Boolean)? = null
) {
    var value: List<T>

    val a = input.toList()
    val p = input.indices.map { 0 }.toMutableList()
    value = a.toList()
    callBack(value)
    if (stopped?.invoke(value) == true) {
        return
    }

    var i = 1;
    while (i < input.size) {
        if (p[i] < i) {
            val j = i % 2 * p[i]
            swap(a, i, j)
            value = a.toList()
            callBack(value)
            if (stopped?.invoke(value) == true) {
                return
            }
            p[i] += 1;
            i = 1
        } else {
            p[i] = 0;
            ++i;
        }
    }
}

@OptIn(DelicateCoroutinesApi::class)
fun <T> permuteAsync(input: List<T>, scope: CoroutineScope = GlobalScope): ChannelGuard<List<T>> {
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
