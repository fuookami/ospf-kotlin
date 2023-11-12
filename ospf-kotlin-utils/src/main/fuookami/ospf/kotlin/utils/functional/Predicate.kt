package fuookami.ospf.kotlin.utils.functional

import fuookami.ospf.kotlin.utils.operator.*

typealias Predicate<T> = (T) -> Boolean
typealias IndexedPredicate<T> = (Int, T) -> Boolean
typealias Extractor<Ret, T> = (T) -> Ret
typealias Mapper<Ret, T> = (T) -> Ret
typealias Comparator<T> = (T, T) -> Boolean
typealias PartialComparator<T> = (T, T) -> Boolean?
typealias ThreeWayComparator<T> = (T, T) -> Order
typealias PartialThreeWayComparator<T> = (T, T) -> Order?
typealias Generator<Ret> = () -> Ret?

infix fun <T, U : T> Predicate<T>.and(rhs: Predicate<U>) = { it: U -> this(it as T) and rhs(it) }
infix fun <T, U : T> Predicate<T>.or(rhs: Predicate<U>) = { it: U -> this(it) or rhs(it) }
infix fun <T, U : T> Predicate<T>.xor(rhs: Predicate<U>) = { it: U -> this(it) xor rhs(it) }
operator fun <T> Predicate<T>.not(): Predicate<T> = { it: T -> !this(it) }
operator fun <T> ThreeWayComparator<T>.not(): ThreeWayComparator<T> = { lhs, rhs: T -> orderOf(-this(lhs, rhs).value) }
