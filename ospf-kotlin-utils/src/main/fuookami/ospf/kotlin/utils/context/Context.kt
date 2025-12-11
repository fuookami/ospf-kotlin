package fuookami.ospf.kotlin.utils.context

data class ContextKey(
    val thread: Thread,
    val stackTree: Array<StackTraceElement>
) {
    companion object {
        private fun dump(stackTree: List<StackTraceElement>): Array<StackTraceElement> {
            if (stackTree.isEmpty()) {
                return emptyArray()
            }

            val topStack = stackTree[0]
            val normalizedTopStack = StackTraceElement(
                topStack.classLoaderName,
                topStack.moduleName,
                topStack.moduleVersion,
                topStack.className,
                topStack.methodName,
                topStack.fileName,
                -1
            )
            return (listOf(normalizedTopStack) + stackTree.toList().subList(1, stackTree.size)).toTypedArray()
        }

        operator fun invoke(): ContextKey {
            val stackTree = Thread.currentThread().stackTrace
            return ContextKey(
                thread = Thread.currentThread(),
                stackTree = dump(stackTree.toList().subList(3, stackTree.lastIndex))
            )
        }
    }

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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ContextKey) return false

        if (thread != other.thread) return false
        if (!(stackTree contentDeepEquals other.stackTree)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = thread.hashCode()
        result = 31 * result + stackTree.contentDeepHashCode()
        return result
    }
}

class ContextVar<T>(
    internal val defaultValue: T
) {
    @get:Synchronized
    internal val stackValues: MutableMap<ContextKey, T> = hashMapOf()
    @get:Synchronized
    internal val customValues: MutableMap<Any, T> = hashMapOf()

    @Synchronized
    fun set(value: T): Context<T> {
        stackValues[ContextKey()] = value
        return Context(this)
    }

    @Synchronized
    fun set(builder: () -> T): Context<T> {
        stackValues[ContextKey()] = builder()
        return Context(this)
    }

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
        return Context(this)
    }

    @Synchronized
    fun get(): T? {
        return get(ContextKey())
    }

    @Synchronized
    operator fun get(key: Any?): T? {
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
                customValues[key]
            }
        }
    }
}

class Context<T>(
    private val context: ContextVar<T>
) {
    @Synchronized
    protected fun finalize() {
        var key: ContextKey? = ContextKey()
        while (!context.stackValues.containsKey(key)) {
            key = key?.parent
        }
        if (key != null) {
            context.stackValues.remove(key)
        }
    }
}

fun <T : Cloneable> context(contextVar: ContextVar<T>, value: T? = null): Context<T> {
    return if (value != null) {
        contextVar.set(value)
    } else {
        contextVar.set(contextVar.defaultValue)
    }
}

fun <T : Cloneable> context(contextVar: ContextVar<T>, builder: (() -> T)? = null): Context<T> {
    return if (builder != null) {
        contextVar.set(builder)
    } else {
        contextVar.set(contextVar.defaultValue)
    }
}
