package fuookami.ospf.kotlin.utils.functional

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.operator.*

typealias Predicate<T> = (T) -> Boolean
typealias TryPredicate<T> = (T) -> Result<Boolean, Error>
typealias IndexedPredicate<T> = (Int, T) -> Boolean
typealias TryIndexedPredicate<T> = (Int, T) -> Result<Boolean, Error>

typealias Extractor<Ret, T> = (T) -> Ret
typealias TryExtractor<Ret, T> = (T) -> Result<Ret, Error>
typealias IndexedExtractor<Ret, T> = (Int, T) -> Ret
typealias TryIndexedExtractor<Ret, T> = (Int, T) -> Result<Ret, Error>

typealias Mapper<Ret, T> = (T) -> Ret
typealias TryMapper<Ret, T> = (T) -> Result<Ret, Error>

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
