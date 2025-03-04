package fuookami.ospf.kotlin.utils.math.combinatorics

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import org.apache.logging.log4j.kotlin.*
import fuookami.ospf.kotlin.utils.parallel.*

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
        if (callBack!= null) {
            callBack(row)
        }
        if (stopped!= null && stopped(row)) {
            break
        }
        var i = n - 1
        while (i >= 0 && indices[i] == input[i].size - 1) {
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

@OptIn(DelicateCoroutinesApi::class)
fun <T> crossAsync(
    input: List<List<T>>,
    scope: CoroutineScope = GlobalScope,
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
                promise.send(row)
                var i = n - 1
                while (i >= 0 && indices[i] == input[i].size - 1) {
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
        } catch (e: ClosedSendChannelException) {
            logger.debug { "Combination generation was stopped by controller." }
        } catch (e: Exception) {
            logger.debug { "Combination generation Error ${e.message}" }
        } finally {
            promise.close()
        }
    }
    return ChannelGuard(promise)
}
