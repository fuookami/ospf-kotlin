package fuookami.ospf.kotlin.utils.functional

fun <T> T?.ifNull(default: () -> T): T = this ?: default()

fun <T> Collection<T>?.ifNullOrEmpty(default: Collection<T>): Collection<T> = this?.ifEmpty { default } ?: default
fun <T> Collection<T>?.ifNullOrEmpty(default: () -> Collection<T>): Collection<T> = this?.ifEmpty { default() } ?: default()
