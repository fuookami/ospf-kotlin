package fuookami.ospf.kotlin.utils.parallel

// RAII Thread Wrapper
class ThreadGuard(
    val thread: Thread
): AutoCloseable {
    override fun close() {
        thread.join()
    }

    init {
        thread.start()
    }

    fun join() {
        thread.join()
    }
}
