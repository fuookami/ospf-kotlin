package fuookami.ospf.kotlin.utils.functional

import fuookami.ospf.kotlin.utils.operator.*

typealias Predicate<T> = (T) -> Boolean
typealias TryPredicate<T> = (T) -> Ret<Boolean>
typealias SuspendPredicate<T> = suspend (T) -> Boolean
typealias SuspendTryPredicate<T> = suspend (T) -> Ret<Boolean>

typealias IndexedPredicate<T> = (Int, T) -> Boolean
typealias TryIndexedPredicate<T> = (Int, T) -> Ret<Boolean>
typealias SuspendIndexedPredicate<T> = suspend (Int, T) -> Boolean
typealias SuspendTryIndexedPredicate<T> = suspend (Int, T) -> Ret<Boolean>

typealias Extractor<R, T> = (T) -> R
typealias TryExtractor<R, T> = (T) -> Ret<R>
typealias SuspendExtractor<R, T> = suspend (T) -> R
typealias SuspendTryExtractor<R, T> = suspend (T) -> Ret<R>

typealias IndexedExtractor<R, T> = (Int, T) -> R
typealias TryIndexedExtractor<R, T> = (Int, T) -> Ret<R>
typealias SuspendIndexedExtractor<R, T> = suspend (Int, T) -> R
typealias SuspendTryIndexedExtractor<R, T> = suspend (Int, T) -> Ret<R>

typealias Mapper<R, T> = (T) -> R
typealias TryMapper<R, T> = (T) -> Ret<R>

typealias KComparator<T> = kotlin.Comparator<T>

typealias Comparator<T> = (T, T) -> Boolean
typealias PartialComparator<T> = (T, T) -> Boolean?
typealias TryComparator<T> = (T, T) -> Ret<Boolean>

typealias ThreeWayComparator<T> = (T, T) -> Order
typealias PartialThreeWayComparator<T> = (T, T) -> Order?
typealias TryThreeWayComparator<T> = (T, T) -> Ret<Order>

typealias Generator<R> = () -> R?

inline infix fun <T, U : T> Predicate<T>.and(crossinline rhs: Predicate<U>) = { it: U -> this(it as T) and rhs(it) }
inline infix fun <T, U : T> Predicate<T>.or(crossinline rhs: Predicate<U>) = { it: U -> this(it) or rhs(it) }
inline infix fun <T, U : T> Predicate<T>.xor(crossinline rhs: Predicate<U>) = { it: U -> this(it) xor rhs(it) }

operator fun <T> Predicate<T>.not(): Predicate<T> = { it: T -> !this(it) }
operator fun <T> ThreeWayComparator<T>.not(): ThreeWayComparator<T> = { lhs, rhs: T -> orderOf(-this(lhs, rhs).value) }

fun <T> kotlin.Comparator<T>.threeWay(): ThreeWayComparator<T> {
    return { lhs, rhs -> orderOf(this.compare(lhs, rhs)) }
}
