package fuookami.ospf.kotlin.utils.math.combinatorics

import java.util.Collections.*
import org.apache.logging.log4j.kotlin.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
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

@OptIn(ExperimentalCoroutinesApi::class)
fun <T> permuteAsync(input: List<T>, scope: CoroutineScope): ChannelGuard<List<T>> {
    val logger = logger("Permutations")

    val promise = Channel<List<T>>(Channel.UNLIMITED)
    scope.launch {
        try {
            val a = input.toList()
            val p = input.indices.map { 0 }.toMutableList()
            promise.send(a.toList())

            var i = 1;
            while (i < input.size && !promise.isClosedForSend) {
                if (p[i] < i) {
                    val j = i % 2 * p[i]
                    swap(a, i, j)
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
        }finally {
            promise.close()
        }
    }
    return ChannelGuard(promise)
}
