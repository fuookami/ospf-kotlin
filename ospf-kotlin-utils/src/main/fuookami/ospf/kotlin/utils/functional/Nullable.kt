/**
 * 空值处理扩展
 *
 * Extension functions for nullable value handling.
 * 可空值处理的扩展函数。
 */
package fuookami.ospf.kotlin.utils.functional

/**
 * 如果为空则使用默认值
 *
 * Returns the value if non-null, otherwise returns the result of the default provider.
 * 如果值非空则返回值，否则返回默认提供者的结果。
 *
 * @param T 值的类型 / The type of the value
 * @param default 默认值提供者 / The default value provider
 * @return 非空时的原值，否则为默认值 / The original value if non-null, otherwise the default value
 */
fun <T> T?.ifNull(default: () -> T): T = this ?: default()

/**
 * 如果集合为空或 null 则使用默认集合
 *
 * Returns the collection if non-null and non-empty, otherwise returns the default collection.
 * 如果集合非空且不为 null 则返回集合，否则返回默认集合。
 *
 * @param T 集合元素的类型 / The type of collection elements
 * @param default 默认集合 / The default collection
 * @return 非空非 null 时的原集合，否则为默认集合 / The original collection if non-null and non-empty, otherwise the default collection
 */
fun <T> Collection<T>?.ifNullOrEmpty(default: Collection<T>): Collection<T> = this?.ifEmpty { default } ?: default

/**
 * 如果集合为空或 null 则使用默认值提供者
 *
 * Returns the collection if non-null and non-empty, otherwise returns the result of the default provider.
 * 如果集合非空且不为 null 则返回集合，否则返回默认提供者的结果。
 *
 * @param T 集合元素的类型 / The type of collection elements
 * @param default 默认集合提供者 / The default collection provider
 * @return 非空非 null 时的原集合，否则为默认值 / The original collection if non-null and non-empty, otherwise the default value
 */
fun <T> Collection<T>?.ifNullOrEmpty(default: () -> Collection<T>): Collection<T> = this?.ifEmpty { default() } ?: default()
