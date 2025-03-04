package fuookami.ospf.kotlin.utils.math.combinatorics

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import org.apache.logging.log4j.kotlin.*
import fuookami.ospf.kotlin.utils.parallel.*

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

@OptIn(DelicateCoroutinesApi::class)
fun <T> combineAsync(
    input: List<T>,
    scope: CoroutineScope = GlobalScope,
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
                if (promise.isClosedForSend) {
                    break
                }
                promise.send(combination)
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
