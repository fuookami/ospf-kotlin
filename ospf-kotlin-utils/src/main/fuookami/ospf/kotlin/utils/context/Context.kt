/**
 * This file implements a context management system for thread-local and coroutine-scoped variable storage.
 * 本文件实现上下文管理系统，支持线程局部和协程作用域的变量存储。
*/
package fuookami.ospf.kotlin.utils.context

/**
 * A unique key for identifying context variables, containing thread information and call stack information.
 * 上下文键，用于标识上下文变量的唯一键，包含线程信息和调用栈信息。
 *
 * @property thread current thread / 当前线程
 * @property stackTree call stack array / 调用栈数组
*/
data class ContextKey(
    val thread: Thread,
    val stackTree: Array<StackTraceElement>
) {
    companion object {
        /**
         * Normalize the call stack by setting the line number of the top stack frame to -1.
         * 规范化调用栈，将顶层栈帧的行号设为 -1。
         *
         * @param stackTree original call stack list / 原始调用栈列表
         * @return normalized call stack array / 规范化后的调用栈数组
        */
        private fun dump(stackTree: List<StackTraceElement>): Array<StackTraceElement> {
            if (stackTree.isEmpty()) {
                return emptyArray()
            }

            val topStack = stackTree[0]
            val normalizedTopStack = StackTraceElement(
                topStack.className,
                topStack.methodName,
                topStack.fileName,
                -1
            )
            return (listOf(normalizedTopStack) + stackTree.toList().subList(1, stackTree.size)).toTypedArray()
        }

        /**
         * Create a context key for the current context.
         * 创建当前上下文键。
         *
         * @return current context key / 当前上下文键
        */
        operator fun invoke(): ContextKey {
            val stackTree = Thread.currentThread().stackTrace
            return ContextKey(
                thread = Thread.currentThread(),
                stackTree = dump(stackTree.toList().subList(3, stackTree.lastIndex))
            )
        }
    }

    /**
     * The parent context key, derived by removing the top stack frame.
     * 父上下文键，通过移除顶层栈帧派生。
    */
    val parent by lazy {
        if (stackTree.isEmpty()) {
            null
        } else {
            ContextKey(
                thread = this.thread,
                stackTree = dump(stackTree.toList().subList(1, stackTree.size))
            )
        }
    }

    /**
     * Checks equality using [contentDeepEquals] for the stack tree array,
     * ensuring structural comparison of array contents rather than reference equality.
     * 判断两个上下文键是否相等，使用 [contentDeepEquals] 对栈树数组进行结构化比较，
     * 确保比较的是数组内容而非引用。
     *
     * @param other the object to compare against / 待比较的对象
     * @return whether the two keys are equal / 是否相等
    */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ContextKey) return false

        if (thread != other.thread) return false
        if (!(stackTree contentDeepEquals other.stackTree)) return false

        return true
    }

    /**
     * Computes the hash code using [contentDeepHashCode] for the stack tree array,
     * consistent with the structural equality defined in [equals].
     * 计算上下文键的哈希码，使用 [contentDeepHashCode] 计算栈树数组的哈希码，
     * 与 [equals] 中定义的结构化相等性保持一致。
     *
     * @return the hash code / 哈希码
    */
    override fun hashCode(): Int {
        var result = thread.hashCode()
        result = 31 * result + stackTree.contentDeepHashCode()
        return result
    }
}

/**
 * A thread-safe variable container that supports stack-level lookup.
 * Can set different values in different contexts and automatically finds the nearest value through stack levels.
 * 上下文变量，一个线程安全的、支持栈层级查找的变量容器。
 * 可以在不同上下文中设置不同的值，并通过栈层级自动查找最近的值。
 *
 * @param T type of the variable value / 变量值的类型
 * @property defaultValue default value returned when no matching context exists / 默认值，当没有匹配的上下文时返回
*/
class ContextVar<T>(
    internal val defaultValue: T
) {

    /**
     * Map of values keyed by stack-level context keys.
     * 栈层级值映射表。
    */
    @get:Synchronized
    internal val stackValues: MutableMap<ContextKey, T> = hashMapOf()

    /**
     * Map of values keyed by custom keys.
     * 自定义键值映射表。
    */
    @get:Synchronized
    internal val customValues: MutableMap<Any, T> = hashMapOf()

    /**
     * Set a value in the current context.
     * 在当前上下文中设置值。
     *
     * @param value value to set / 要设置的值
     * @return context object for automatic cleanup / 上下文对象，用于自动清理
    */
    @Synchronized
    fun set(value: T): Context<T> {
        val key = ContextKey()
        stackValues[key] = value
        return Context(key, this)
    }

    /**
     * Set a value in the current context using a builder function.
     * 在当前上下文中通过构建器设置值。
     *
     * @param builder value builder function / 值构建器
     * @return context object for automatic cleanup / 上下文对象，用于自动清理
    */
    @Synchronized
    fun set(builder: () -> T): Context<T> {
        val key = ContextKey()
        stackValues[key] = builder()
        return Context(key, this)
    }

    /**
     * Set a value with a specified key.
     * 通过指定键设置值。
     *
     * @param key key, can be ContextKey or custom key / 键，可以是 ContextKey 或自定义键
     * @param value value to set / 要设置的值
     * @return context object for automatic cleanup / 上下文对象，用于自动清理
    */
    @Synchronized
    operator fun set(key: Any, value: T): Context<T> {
        when (key) {
            is ContextKey -> {
                stackValues[key] = value
            }

            else -> {
                customValues[key] = value
            }
        }
        return Context(key, this)
    }

    /**
     * Get the value in the current context.
     * 获取当前上下文的值。
     *
     * @return value in current context or default value / 当前上下文的值或默认值
    */
    @Synchronized
    fun get(): T {
        return get(ContextKey())
    }

    /**
     * 通过指定键获取值
     *
     * Get a value with a specified key.
     * For ContextKey, traverses up the stack to find the nearest matching value.
     *
     * @param key 键，可以是 ContextKey、自定义键或 null / Key, can be ContextKey, custom key, or null
     * @return 对应的值或默认值 / Corresponding value or default value
    */
    @Synchronized
    operator fun get(key: Any?): T {
        return when (key) {
            is ContextKey -> {
                var k: ContextKey? = key
                while (k != null && !stackValues.containsKey(k)) {
                    k = k.parent
                }
                k?.let { stackValues[it] } ?: defaultValue
            }

            null -> {
                defaultValue
            }

            else -> {
                customValues[key] ?: defaultValue
            }
        }
    }

    /**
     * 移除指定键及其所有子键的值
     *
     * Remove the value for the specified key and all its child keys.
     * For ContextKey, removes all values in the subtree rooted at the specified key.
     * For null, removes the current context key.
     * For custom keys, removes only the specified key.
     *
     * @param key 要移除的键，默认为当前上下文键 / Key to remove, defaults to current context key
    */
    @Synchronized
    fun remove(key: Any? = null) {
        when (key) {
            is ContextKey -> {
                val removedKeys = HashSet<ContextKey>()
                for (ckey in stackValues.keys) {
                    var childKey: ContextKey? = ckey
                    while (childKey != null && childKey != key) {
                        childKey = childKey.parent
                    }
                    if (childKey == key) {
                        // BUG FIX: 应添加原始的 ckey（子键），而不是 childKey（此时等于 key）
                        // FIX: Should add the original ckey (child key), not childKey (which equals key at this point)
                        removedKeys.add(ckey)
                    }
                }
                removedKeys.forEach {
                    stackValues.remove(it)?.let { value ->
                        if (value is AutoCloseable) {
                            value.close()
                        }
                    }
                }
            }

            null -> {
                remove(ContextKey())
            }

            else -> {
                customValues.remove(key)?.let { value ->
                    if (value is AutoCloseable) {
                        value.close()
                    }
                }
            }
        }
    }
}

/**
 * 上下文
 *
 * 一个自动清理的上下文持有者，实现了 AutoCloseable 接口。
 * 使用 `use {}` 或在作用域结束时自动调用 close() 清理上下文值。
 *
 * An auto-cleanup context holder that implements AutoCloseable interface.
 * Automatically calls close() to cleanup context value when using `use {}` or at scope end.
 *
 * @param T 上下文值的类型 / Type of context value
 * @property key 关联的键 / Associated key
 * @property context 关联的上下文变量 / Associated context variable
*/
class Context<T>(
    private val key: Any,
    private val context: ContextVar<T>
) : AutoCloseable {

    /**
     * 关闭上下文，移除关联的值
     *
     * Closes this context by removing the associated value from the context variable.
     * Typically called automatically via `use {}` or at the end of a scope.
     *
     * 关闭此上下文，从上下文变量中移除关联的值。
     * 通常通过 `use {}` 或在作用域结束时自动调用。
    */
    @Synchronized
    override fun close() {
        context.remove(key)
    }
}

/**
 * 创建一个上下文并设置值
 *
 * Create a context and set a value.
 *
 * @param T 上下文值的类型，必须实现 Cloneable / Type of context value, must implement Cloneable
 * @param contextVar 上下文变量 / Context variable
 * @param value 要设置的值，如果为 null 则使用默认值 / Value to set, uses default value if null
 * @return 上下文对象 / Context object
*/
fun <T : Cloneable> context(contextVar: ContextVar<T>, value: T? = null): Context<T> {
    return if (value != null) {
        contextVar.set(value)
    } else {
        contextVar.set(contextVar.defaultValue)
    }
}

/**
 * 创建一个上下文并通过构建器设置值
 *
 * Create a context and set a value using a builder function.
 *
 * @param T 上下文值的类型，必须实现 Cloneable / Type of context value, must implement Cloneable
 * @param contextVar 上下文变量 / Context variable
 * @param builder 值构建器，如果为 null 则使用默认值 / Value builder, uses default value if null
 * @return 上下文对象 / Context object
*/
fun <T : Cloneable> context(contextVar: ContextVar<T>, builder: (() -> T)? = null): Context<T> {
    return if (builder != null) {
        contextVar.set(builder)
    } else {
        contextVar.set(contextVar.defaultValue)
    }
}
