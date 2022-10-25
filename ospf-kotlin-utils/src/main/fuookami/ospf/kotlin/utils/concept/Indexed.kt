package fuookami.ospf.kotlin.utils.concept

import kotlin.reflect.KClass
import java.util.concurrent.*
import java.util.concurrent.atomic.*

val impls = ConcurrentHashMap<KClass<*>, AtomicInteger>()

sealed class IndexedImpl {
    inline fun <reified T> nextIndex(): Int {
        return nextIndex(T::class)
    }

    fun nextIndex(cls: KClass<*>): Int {
        return impls.getOrPut(cls) { AtomicInteger(0) }.getAndIncrement()
    }

    inline fun <reified T> flush() {
        flush(T::class)
    }

    fun flush(cls: KClass<*>) {
        impls[cls] = AtomicInteger(0)
    }
}

interface Indexed {
    val index: Int

    companion object: IndexedImpl()
}

open class ManualIndexed internal constructor(
    private var mIndex: Int? = null
): Indexed {
    val indexed = mIndex != null
    override val index: Int get() {
        assert(indexed)
        return this.mIndex!!
    }

    constructor(): this(null)

    companion object: IndexedImpl()

    fun setIndexed() {
        assert(!indexed)
        mIndex = nextIndex(this::class)
    }

    fun setIndexed(cls: KClass<*>) {
        assert(!indexed)
        mIndex = nextIndex(cls)
    }
}

open class AutoIndexed internal constructor(
    override val index: Int
): Indexed {
    constructor(cls: KClass<*>): this(nextIndex(cls))

    companion object: IndexedImpl()
}
