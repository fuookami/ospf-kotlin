/**
 * 线程保护器
 *
 * Thread guard wrapper providing RAII-style resource management for threads.
 * 为线程提供 RAII 风格资源管理的线程保护器包装类。
 *
 * 使用场景：
 * - 需要确保线程在作用域结束时完成执行
 * - 配合 try-with-resources 或 use 函数使用
 * - 异步任务执行并等待结果
 *
 * Usage scenarios:
 * - Ensuring thread completes execution when scope ends
 * - Used with try-with-resources or use function
 * - Async task execution with result waiting
*/
package fuookami.ospf.kotlin.utils.parallel

/**
 * 异步执行包装器
 *
 * RAII Thread Wrapper for async task execution with automatic join on scope exit.
 * 用于异步任务执行的 RAII 线程包装器，在作用域结束时自动等待线程完成。
 *
 * @param T 任务返回类型 / Task return type
 * @param task 要执行的任务函数 / Task function to execute
*/
// RAII Thread Wrapper
class Async<T>(
    val task: () -> T
) : AutoCloseable {

    /**
     * 任务执行结果
     *
     * Result of the task execution.
     * 任务执行的结果。
    */
    private var result: T? = null

    /**
     * 执行任务的线程
     *
     * Thread that executes the task.
     * 执行任务的线程实例。
    */
    val thread = Thread {
        val result = task()
        this.result = result
    }

    /**
     * 关闭资源（等待线程完成）
     *
     * Close the resource by joining the thread.
     * 通过等待线程完成来关闭资源。
     *
     * 实现 AutoCloseable 接口的关闭方法，用于 RAII 资源管理。
     * Implementation of AutoCloseable close method for RAII resource management.
    */
    override fun close() {
        thread.join()
    }

    init {
        /**
         * 初始化时启动线程
         *
         * Start the thread upon initialization.
         * 在初始化时启动线程执行任务。
        */
        thread.start()
    }

    /**
     * 等待线程完成并获取结果
     *
     * Join the thread and return the task result.
     * 等待线程完成并返回任务执行结果。
     *
     * @return 任务执行结果 / Task execution result
    */
    fun join(): T {
        thread.join()
        return result!!
    }
}

/**
 * 线程保护器（无返回值的异步执行）
 *
 * Thread guard type alias for async execution without return value.
 * 用于无返回值异步执行的线程保护器类型别名。
 *
 * ThreadGuard 是 Async<Unit> 的别名，适用于不需要返回值的后台任务。
 * ThreadGuard is an alias for Async<Unit>, suitable for background tasks that don't need return values.
*/
typealias ThreadGuard = Async<Unit>
