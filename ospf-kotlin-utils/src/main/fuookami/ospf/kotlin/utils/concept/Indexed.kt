package fuookami.ospf.kotlin.utils.concept

import kotlin.reflect.KClass

val impls = HashMap<KClass<*>, Int>()

abstract class Indexed(
    private var mIndex: Int?
) {
    val index: Int? get() = this.mIndex

    fun indexed(): Boolean = mIndex != null
    fun setIndexed() {
        assert(!indexed())
        mIndex = nextIndex(this::class)
    }

    companion object {
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
}

abstract class AutoIndexed: Indexed(nextIndex(this::class)) {
    companion object {
        fun flush() {
            flush(this::class)
        }
    }
}
