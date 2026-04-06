/**
 * Channel 保护器
 *
 * Channel guard wrapper providing RAII-style resource management for Kotlin Channels.
 * 为 Kotlin Channel 提供 RAII 风格资源管理的 Channel 保护器包装类。
 *
 * 使用场景：
 * - 需要确保 Channel 在使用后被正确关闭
 * - 配合 try-with-resources 或 use 函数使用
 *
 * Usage scenarios:
 * - Ensuring Channel is properly closed after use
 * - Used with try-with-resources or use function
 */
package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.channels.*

/**
 * Channel 保护器
 *
 * Channel guard that wraps a Channel and provides RAII-style resource management.
 * 包装 Channel 并提供 RAII 风格资源管理的 Channel 保护器。
 *
 * 实现了 AutoCloseable 接口，确保 Channel 在作用域结束时自动关闭。
 * Implements AutoCloseable interface, ensuring Channel is automatically closed when scope ends.
 *
 * @param T Channel 元素类型 / Channel element type
 * @param channel 被包装的 Channel 实例 / Wrapped Channel instance
 */
class ChannelGuard<T>(
    val channel: Channel<T>
) : AutoCloseable {
    /**
     * 关闭 Channel
     *
     * Close the underlying Channel.
     * 关闭底层 Channel。
     *
     * 实现 AutoCloseable 接口的关闭方法，用于 RAII 资源管理。
     * Implementation of AutoCloseable close method for RAII resource management.
     */
    override fun close() {
        channel.close()
    }

    /**
     * 获取 Channel 的迭代器
     *
     * Get the iterator for the underlying Channel.
     * 获取底层 Channel 的迭代器。
     *
     * @return Channel 的迭代器 / Channel iterator
     */
    operator fun iterator() = channel.iterator()

    /**
     * 接收元素
     *
     * Receive an element from the Channel (suspends if empty).
     * 从 Channel 接收一个元素（如果为空则挂起）。
     *
     * @return 接收到的元素 / Received element
     */
    suspend fun receive() = channel.receive()

    /**
     * 尝试接收元素
     *
     * Try to receive an element from the Channel (non-suspending).
     * 尝试从 Channel 接收一个元素（非挂起操作）。
     *
     * @return Channel 结果对象 / Channel result object
     */
    fun tryReceive() = channel.tryReceive()
}
