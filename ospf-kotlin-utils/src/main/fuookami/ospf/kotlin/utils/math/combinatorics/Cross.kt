package fuookami.ospf.kotlin.utils.math.combinatorics

import fuookami.ospf.kotlin.utils.math.algebra.number.*
import fuookami.ospf.kotlin.utils.math.algebra.concept.*
import fuookami.ospf.kotlin.utils.math.algebra.value_range.*

import fuookami.ospf.kotlin.utils.parallel.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import org.apache.logging.log4j.kotlin.*

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

fun <A, B> cross2(lhs: List<A>, rhs: List<B>): List<Pair<A, B>> {
    return lhs.flatMap { l -> rhs.map { r -> l to r } }
}

fun <A, B> cross2Sequence(lhs: List<A>, rhs: List<B>): Sequence<Pair<A, B>> = sequence {
    for (l in lhs) {
        for (r in rhs) {
            yield(l to r)
        }
    }
}

fun <A, B, C> cross3(a: List<A>, b: List<B>, c: List<C>): List<Triple<A, B, C>> {
    return a.flatMap { x ->
        b.flatMap { y ->
            c.map { z -> Triple(x, y, z) }
        }
    }
}

fun <A, B, C> cross3Sequence(a: List<A>, b: List<B>, c: List<C>): Sequence<Triple<A, B, C>> = sequence {
    for (x in a) {
        for (y in b) {
            for (z in c) {
                yield(Triple(x, y, z))
            }
        }
    }
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




