package fuookami.ospf.kotlin.utils.parallel

// RAII Thread Wrapper
class Async<T>(
    val task: () -> T
) : AutoCloseable {
    private var result: T? = null

    val thread = Thread {
        val result = task()
        this.result = result
    }

    override fun close() {
        thread.join()
    }

    init {
        thread.start()
    }

    fun join(): T {
        thread.join()
        return result!!
    }
}

typealias ThreadGuard = Async<Unit>
