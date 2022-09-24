package fuookami.ospf.kotlin.utils.math.combinatorics

import java.util.Collections.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import fuookami.ospf.kotlin.utils.parallel.*

// A Counting QuickPerm Algorithm
fun <T> permute(input: List<T>): List<List<T>> {
    val a = input.toList()
    val p = input.indices.map { 0 }.toMutableList()

    val perms = ArrayList<List<T>>()
    perms.add(a.toList())

    var i  = 1;
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

@OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
fun <T> permuteAsync(input: List<T>): ChannelGuard<List<T>> {
    val promise = Channel<List<T>>()
    GlobalScope.launch {
        val a = input.toList()
        val p = input.indices.map { 0 }.toMutableList()
        if (!promise.isClosedForSend) {
            promise.send(a.toList())
        }

        var i  = 1;
        while (i < input.size && !promise.isClosedForSend) {
            if (p[i] < i) {
                val j = i % 2 * p[i]
                swap(a, i, j)
                if (!promise.isClosedForSend) {
                    promise.send(a.toList())
                }
                p[i] += 1;
                i = 1
            } else {
                p[i] = 0;
                ++i;
            }
        }
        promise.close()
    }
    return ChannelGuard(promise)
}
