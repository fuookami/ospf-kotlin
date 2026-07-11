/**
 * 函数类型别名和组合
 *
 * Type aliases for common function types and composition operators.
 * Provides a functional programming vocabulary for predicates, extractors, and comparators.
 * 常用函数类型的类型别名和组合操作符。
 * 为谓词、提取器和比较器提供函数式编程词汇。
 *
 * Key type categories:
 * - Predicates: Functions returning Boolean
 * - Extractors: Functions extracting a value from another
 * - Mappers: Functions transforming values
 * - Comparators: Functions comparing two values
 * - Generators: Functions producing optional values
 *
 * 主要类型分类：
 * - 谓词：返回 Boolean 的函数
 * - 提取器：从另一个值中提取值的函数
 * - 映射器：转换值的函数
 * - 比较器：比较两个值的函数
 * - 生成器：生成可选值的函数
*/
package fuookami.ospf.kotlin.utils.functional

/**
 * 谓词类型别名
 *
 * Type alias for a predicate function that takes a value and returns a Boolean.
 * 接受一个值并返回 Boolean 的谓词函数类型别名。
*/
typealias Predicate<T> = (T) -> Boolean

/**
 * 可能失败的谓词类型别名
 *
 * Type alias for a predicate function that may fail and return a Result.
 * 可能失败并返回 Result 的谓词函数类型别名。
*/
typealias TryPredicate<T> = (T) -> Ret<Boolean>

/**
 * 挂起谓词类型别名
 *
 * Type alias for a suspend predicate function.
 * 挂起谓词函数类型别名。
*/
typealias SuspendPredicate<T> = suspend (T) -> Boolean

/**
 * 可能失败的挂起谓词类型别名
 *
 * Type alias for a suspend predicate function that may fail.
 * 可能失败的挂起谓词函数类型别名。
*/
typealias SuspendTryPredicate<T> = suspend (T) -> Ret<Boolean>

/**
 * 带索引的谓词类型别名
 *
 * Type alias for a predicate function that takes an index and a value.
 * 接受索引和值的谓词函数类型别名。
*/
typealias IndexedPredicate<T> = (Int, T) -> Boolean

/**
 * 可能失败的带索引谓词类型别名
 *
 * Type alias for an indexed predicate function that may fail.
 * 可能失败的带索引谓词函数类型别名。
*/
typealias TryIndexedPredicate<T> = (Int, T) -> Ret<Boolean>

/**
 * 带索引的挂起谓词类型别名
 *
 * Type alias for a suspend indexed predicate function.
 * 挂起的带索引谓词函数类型别名。
*/
typealias SuspendIndexedPredicate<T> = suspend (Int, T) -> Boolean

/**
 * 可能失败的带索引挂起谓词类型别名
 *
 * Type alias for a suspend indexed predicate function that may fail.
 * 可能失败的挂起带索引谓词函数类型别名。
*/
typealias SuspendTryIndexedPredicate<T> = suspend (Int, T) -> Ret<Boolean>

/**
 * 提取器类型别名
 *
 * Type alias for a function that extracts a value of type R from type T.
 * 从类型 T 中提取类型 R 值的函数类型别名。
*/
typealias Extractor<R, T> = (T) -> R

/**
 * 可能失败的提取器类型别名
 *
 * Type alias for an extractor function that may fail.
 * 可能失败的提取器函数类型别名。
*/
typealias TryExtractor<R, T> = (T) -> Ret<R>

/**
 * 挂起提取器类型别名
 *
 * Type alias for a suspend extractor function.
 * 挂起的提取器函数类型别名。
*/
typealias SuspendExtractor<R, T> = suspend (T) -> R

/**
 * 可能失败的挂起提取器类型别名
 *
 * Type alias for a suspend extractor function that may fail.
 * 可能失败的挂起提取器函数类型别名。
*/
typealias SuspendTryExtractor<R, T> = suspend (T) -> Ret<R>

/**
 * 带索引的提取器类型别名
 *
 * Type alias for an extractor function that takes an index and a value.
 * 接受索引和值的提取器函数类型别名。
*/
typealias IndexedExtractor<R, T> = (Int, T) -> R

/**
 * 可能失败的带索引提取器类型别名
 *
 * Type alias for an indexed extractor function that may fail.
 * 可能失败的带索引提取器函数类型别名。
*/
typealias TryIndexedExtractor<R, T> = (Int, T) -> Ret<R>

/**
 * 带索引的挂起提取器类型别名
 *
 * Type alias for a suspend indexed extractor function.
 * 挂起的带索引提取器函数类型别名。
*/
typealias SuspendIndexedExtractor<R, T> = suspend (Int, T) -> R

/**
 * 可能失败的带索引挂起提取器类型别名
 *
 * Type alias for a suspend indexed extractor function that may fail.
 * 可能失败的挂起带索引提取器函数类型别名。
*/
typealias SuspendTryIndexedExtractor<R, T> = suspend (Int, T) -> Ret<R>

/**
 * 映射器类型别名
 *
 * Type alias for a function that maps a value of type T to type R.
 * 将类型 T 的值映射到类型 R 的函数类型别名。
*/
typealias Mapper<R, T> = (T) -> R

/**
 * 可能失败的映射器类型别名
 *
 * Type alias for a mapper function that may fail.
 * 可能失败的映射器函数类型别名。
*/
typealias TryMapper<R, T> = (T) -> Ret<R>

/**
 * Kotlin 标准比较器类型别名
 *
 * Type alias for Kotlin's standard Comparator interface.
 * Kotlin 标准 Comparator 接口的类型别名。
*/
typealias KComparator<T> = kotlin.Comparator<T>

/**
 * 比较器类型别名
 *
 * Type alias for a comparator function that returns true if first is less than second.
 * 比较器函数类型别名，如果第一个参数小于第二个参数则返回 true。
*/
typealias Comparator<T> = (T, T) -> Boolean

/**
 * 部分比较器类型别名
 *
 * Type alias for a partial comparator function that may return null if values cannot be compared.
 * 部分比较器函数类型别名，如果值无法比较则可能返回 null。
*/
typealias PartialComparator<T> = (T, T) -> Boolean?

/**
 * 可能失败的比较器类型别名
 *
 * Type alias for a comparator function that may fail.
 * 可能失败的比较器函数类型别名。
*/
typealias TryComparator<T> = (T, T) -> Ret<Boolean>

/**
 * 生成器类型别名
 *
 * Type alias for a generator function that produces an optional value.
 * 生成可选值的生成器函数类型别名。
*/
typealias Generator<R> = () -> R?

/**
 * 三路比较器类型别名
 *
 * Type alias for a three-way comparator function that returns an Order.
 * 返回 Order 的三路比较器函数类型别名。
*/
typealias ThreeWayComparator<T> = (T, T) -> Order

/**
 * 部分三路比较器类型别名
 *
 * Type alias for a partial three-way comparator that may return null if values cannot be compared.
 * 部分三路比较器类型别名，如果值无法比较则可能返回 null。
*/
typealias PartialThreeWayComparator<T> = (T, T) -> Order?

/**
 * 可能失败的三路比较器类型别名
 *
 * Type alias for a three-way comparator function that may fail.
 * 可能失败的三路比较器函数类型别名。
*/
typealias TryThreeWayComparator<T> = (T, T) -> Ret<Order>

/**
 * 谓词的逻辑与组合
 *
 * Combines two predicates with logical AND.
 * 使用逻辑与组合两个谓词。
 *
 * @param T 输入类型 / The input type
 * @param U 输入类型的子类型 / The subtype of input type
 * @param rhs 右侧谓词 / The right-hand side predicate
 * @return 组合后的谓词 / The combined predicate
*/
inline infix fun <T, U : T> Predicate<T>.and(crossinline rhs: Predicate<U>) = { it: U -> this(it as T) and rhs(it) }

/**
 * 谓词的逻辑或组合
 *
 * Combines two predicates with logical OR.
 * 使用逻辑或组合两个谓词。
 *
 * @param T 输入类型 / The input type
 * @param U 输入类型的子类型 / The subtype of input type
 * @param rhs 右侧谓词 / The right-hand side predicate
 * @return 组合后的谓词 / The combined predicate
*/
inline infix fun <T, U : T> Predicate<T>.or(crossinline rhs: Predicate<U>) = { it: U -> this(it as T) or rhs(it) }

/**
 * 谓词的逻辑异或组合
 *
 * Combines two predicates with logical XOR.
 * 使用逻辑异或组合两个谓词。
 *
 * @param T 输入类型 / The input type
 * @param U 输入类型的子类型 / The subtype of input type
 * @param rhs 右侧谓词 / The right-hand side predicate
 * @return 组合后的谓词 / The combined predicate
*/
inline infix fun <T, U : T> Predicate<T>.xor(crossinline rhs: Predicate<U>) = { it: U -> this(it as T) xor rhs(it) }

/**
 * 谓词的逻辑非操作
 *
 * Negates a predicate.
 * 对谓词取反。
 *
 * @param T 输入类型 / The input type
 * @return 取反后的谓词 / The negated predicate
*/
operator fun <T> Predicate<T>.not(): Predicate<T> = { it: T -> !this(it) }

/**
 * 三路比较器的逻辑非操作
 *
 * Negates a three-way comparator (reverses the order).
 * 对三路比较器取反（反转顺序）。
 *
 * @param T 输入类型 / The input type
 * @return 取反后的三路比较器 / The negated three-way comparator
*/
operator fun <T> ThreeWayComparator<T>.not(): ThreeWayComparator<T> = { lhs, rhs: T -> orderOf(-this(lhs, rhs).value) }

/**
 * 将 Kotlin 比较器转换为三路比较器
 *
 * Converts a Kotlin Comparator to a three-way comparator.
 * 将 Kotlin Comparator 转换为三路比较器。
 *
 * @param T 输入类型 / The input type
 * @return 转换后的三路比较器 / The converted three-way comparator
*/
fun <T> kotlin.Comparator<T>.threeWay(): ThreeWayComparator<T> {
    return { lhs, rhs -> orderOf(this.compare(lhs, rhs)) }
}
