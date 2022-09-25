package fuookami.ospf.kotlin.utils.concept

import kotlin.reflect.KClass

val impls = HashMap<KClass<*>, Int>()

interface Indexed {
    val index: Int

    companion object {
        inline fun <reified T> nextIndex(): Int {
            val ret = impls[T::class] ?: 0
            impls[T::class] = (impls[T::class] ?: 0) + 1
            return ret
        }

        inline fun <reified T> flush() {
            impls[T::class] = 0
        }
    }
}
