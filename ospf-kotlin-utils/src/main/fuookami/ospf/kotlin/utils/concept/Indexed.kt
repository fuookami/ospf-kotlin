package fuookami.ospf.kotlin.utils.concept

import kotlin.reflect.KClass

val impls = HashMap<KClass<*>, Int>()

sealed class IndexedImpl {
    inline fun <reified T> nextIndex(): Int {
        val ret = impls[T::class] ?: 0
        impls[T::class] = (impls[T::class] ?: 0) + 1
        return ret
    }

    fun nextIndex(cls: KClass<*>): Int {
        val ret = impls[cls] ?: 0
        impls[cls] = (impls[cls] ?: 0) + 1
        return ret
    }

    inline fun <reified T> flush() {
        impls[T::class] = 0
    }

    fun flush(cls: KClass<*>) {
        impls[cls] = 0
    }
}

interface Indexed {
    val index: Int
}

open class ManualIndexed internal constructor(
    private var mIndex: Int? = null
): Indexed {
    override val index: Int get() = this.mIndex!!

    constructor(): this(null)

    companion object: IndexedImpl()

    fun indexed(): Boolean = mIndex != null
    fun setIndexed() {
        assert(!indexed())
        mIndex = nextIndex(this::class)
    }
}

open class AutoIndexed internal constructor(
    override val index: Int
): Indexed {
    constructor(cls: KClass<*>): this(nextIndex(cls))

    companion object: IndexedImpl()
}
