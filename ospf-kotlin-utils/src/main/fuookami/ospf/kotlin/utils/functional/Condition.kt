package fuookami.ospf.kotlin.utils.functional

data class Condition<T>(
    val value: T,
    val predicate: Predicate<T>
) {
    val result: Boolean by lazy { predicate(value) }

    fun asNull(): T? = if (predicate(value)) null else value
}

fun <T> T.ifTrue(predicate: Predicate<T>) = Condition(this, predicate)
