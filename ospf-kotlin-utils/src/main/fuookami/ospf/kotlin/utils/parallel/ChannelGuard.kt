package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.channels.*

class ChannelGuard<T>(
    val channel: Channel<T>
) : AutoCloseable {
    override fun close() {
        channel.close()
    }

    operator fun iterator() = channel.iterator()
    suspend fun receive() = channel.receive()
    fun tryReceive() = channel.tryReceive()
}
