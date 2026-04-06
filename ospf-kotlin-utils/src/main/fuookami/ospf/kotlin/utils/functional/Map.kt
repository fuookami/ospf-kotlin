/**
 * 多维 Map 操作
 *
 * Multi-dimensional Map type aliases and extension functions for nested map operations.
 * Provides convenient access and modification operations for nested maps.
 * 多维 Map 类型别名和嵌套映射操作的扩展函数。
 * 为嵌套映射提供便捷的访问和修改操作。
 *
 * Key types:
 * - [MultiMap2]: Two-level nested Map (K1 -> K2 -> V)
 * - [MultiMap3]: Three-level nested Map (K1 -> K2 -> K3 -> V)
 * - [MultiMap4]: Four-level nested Map (K1 -> K2 -> K3 -> K4 -> V)
 *
 * 主要类型：
 * - [MultiMap2]: 二级嵌套 Map (K1 -> K2 -> V)
 * - [MultiMap3]: 三级嵌套 Map (K1 -> K2 -> K3 -> V)
 * - [MultiMap4]: 四级嵌套 Map (K1 -> K2 -> K3 -> K4 -> V)
 */
package fuookami.ospf.kotlin.utils.functional

/**
 * 二维 Map 类型别名
 *
 * Type alias for a two-level nested Map.
 * 二级嵌套 Map 的类型别名。
 */
typealias MultiMap2<K1, K2, V> = Map<K1, Map<K2, V>>

/**
 * 三维 Map 类型别名
 *
 * Type alias for a three-level nested Map.
 * 三级嵌套 Map 的类型别名。
 */
typealias MultiMap3<K1, K2, K3, V> = Map<K1, Map<K2, Map<K3, V>>>

/**
 * 四维 Map 类型别名
 *
 * Type alias for a four-level nested Map.
 * 四级嵌套 Map 的类型别名。
 */
typealias MultiMap4<K1, K2, K3, K4, V> = Map<K1, Map<K2, Map<K3, Map<K4, V>>>>

/**
 * 二维可变 Map 类型别名
 *
 * Type alias for a two-level nested mutable Map.
 * 二级嵌套可变 Map 的类型别名。
 */
typealias MutableMultiMap2<K1, K2, V> = MutableMap<K1, MutableMap<K2, V>>

/**
 * 三维可变 Map 类型别名
 *
 * Type alias for a three-level nested mutable Map.
 * 三级嵌套可变 Map 的类型别名。
 */
typealias MutableMultiMap3<K1, K2, K3, V> = MutableMap<K1, MutableMap<K2, MutableMap<K3, V>>>

/**
 * 四维可变 Map 类型别名
 *
 * Type alias for a four-level nested mutable Map.
 * 四级嵌套可变 Map 的类型别名。
 */
typealias MutableMultiMap4<K1, K2, K3, K4, V> = MutableMap<K1, MutableMap<K2, MutableMap<K3, MutableMap<K4, V>>>>

/**
 * 将 Pair 列表转换为二维 Map
 *
 * Converts a list of Triples to a two-level nested Map.
 * 将 Triple 列表转换为二级嵌套 Map。
 *
 * @param K1 第一级键的类型 / The type of the first level key
 * @param K2 第二级键的类型 / The type of the second level key
 * @param V 值的类型 / The type of the value
 * @return 转换后的二维 Map / The converted MultiMap2
 */
fun <K1, K2, V> List<Triple<K1, K2, V>>.toMap2(): MultiMap2<K1, K2, V> {
    return this.groupBy(
        { it.first },
        { it.second to it.third }
    ).mapValues { it.value.toMap() }
}

/**
 * 将 Quadruple 列表转换为三维 Map
 *
 * Converts a list of Quadruples to a three-level nested Map.
 * 将 Quadruple 列表转换为三级嵌套 Map。
 *
 * @param K1 第一级键的类型 / The type of the first level key
 * @param K2 第二级键的类型 / The type of the second level key
 * @param K3 第三级键的类型 / The type of the third level key
 * @param V 值的类型 / The type of the value
 * @return 转换后的三维 Map / The converted MultiMap3
 */
fun <K1, K2, K3, V> List<Quadruple<K1, K2, K3, V>>.toMap3(): MultiMap3<K1, K2, K3, V> {
    return this.groupBy(
        { it.first },
        { Triple(it.second, it.third, it.fourth) }
    ).mapValues { it.value.toMap2() }
}

/**
 * 从 Map 中获取指定键和索引的元素
 *
 * Gets an element from a Map with a List value by key and index.
 * 通过键和索引从值类型为 List 的 Map 中获取元素。
 *
 * @param K 键的类型 / The type of the key
 * @param T 列表元素的类型 / The type of list elements
 * @param key Map 的键 / The key of the map
 * @param i 列表中的索引 / The index in the list
 * @return 找到的元素，如果不存在则返回 null / The found element, or null if not found
 */
operator fun <K, T> Map<K, List<T>>.get(key: K, i: Int): T? {
    return this[key]?.get(i)
}

/**
 * 从 Map 中获取指定键和 ULong 索引的元素
 *
 * Gets an element from a Map with a List value by key and ULong index.
 * 通过键和 ULong 索引从值类型为 List 的 Map 中获取元素。
 *
 * @param K 键的类型 / The type of the key
 * @param T 列表元素的类型 / The type of list elements
 * @param key Map 的键 / The key of the map
 * @param i 列表中的 ULong 索引 / The ULong index in the list
 * @return 找到的元素，如果不存在则返回 null / The found element, or null if not found
 */
operator fun <K, T> Map<K, List<T>>.get(key: K, i: ULong): T? {
    return this[key]?.get(i.toInt())
}

/**
 * 从二维 Map 中获取指定键的值
 *
 * Gets a value from a two-level nested Map by two keys.
 * 通过两个键从二级嵌套 Map 中获取值。
 *
 * @param K1 第一级键的类型 / The type of the first level key
 * @param K2 第二级键的类型 / The type of the second level key
 * @param V 值的类型 / The type of the value
 * @param key1 第一级键 / The first level key
 * @param key2 第二级键 / The second level key
 * @return 找到的值，如果不存在则返回 null / The found value, or null if not found
 */
operator fun <K1, K2, V> MultiMap2<K1, K2, V>.get(key1: K1, key2: K2): V? {
    return this[key1]?.get(key2)
}

/**
 * 从二维可变 Map 中获取或设置值
 *
 * Gets a value from a two-level nested mutable Map, or puts and returns the default value if not found.
 * 从二级嵌套可变 Map 中获取值，如果未找到则设置并返回默认值。
 *
 * @param K1 第一级键的类型 / The type of the first level key
 * @param K2 第二级键的类型 / The type of the second level key
 * @param V 值的类型 / The type of the value
 * @param key1 第一级键 / The first level key
 * @param key2 第二级键 / The second level key
 * @param defaultValue 默认值提供者 / The default value provider
 * @param defaultContainer 默认容器提供者 / The default container provider
 * @return 找到的值或新设置的默认值 / The found value or the newly set default value
 */
fun <K1, K2, V> MutableMultiMap2<K1, K2, V>.getOrPut(
    key1: K1,
    key2: K2,
    defaultValue: () -> V,
    defaultContainer: () -> MutableMap<K2, V> = { mutableMapOf() }
): V {
    return this
        .getOrPut(key1) { defaultContainer() }
        .getOrPut(key2, defaultValue)
}

/**
 * 设置二维可变 Map 中的值（操作符形式）
 *
 * Sets a value in a two-level nested mutable Map using operator syntax.
 * 使用操作符语法设置二级嵌套可变 Map 中的值。
 *
 * @param K1 第一级键的类型 / The type of the first level key
 * @param K2 第二级键的类型 / The type of the second level key
 * @param V 值的类型 / The type of the value
 * @param key1 第一级键 / The first level key
 * @param key2 第二级键 / The second level key
 * @param value 要设置的值 / The value to set
 * @return 之前的值，如果不存在则返回 null / The previous value, or null if not found
 */
operator fun <K1, K2, V> MutableMultiMap2<K1, K2, V>.set(
    key1: K1,
    key2: K2,
    value: V
): V? {
    return this.put(key1, key2, value)
}

/**
 * 设置二维可变 Map 中的值
 *
 * Puts a value in a two-level nested mutable Map.
 * 设置二级嵌套可变 Map 中的值。
 *
 * @param K1 第一级键的类型 / The type of the first level key
 * @param K2 第二级键的类型 / The type of the second level key
 * @param V 值的类型 / The type of the value
 * @param key1 第一级键 / The first level key
 * @param key2 第二级键 / The second level key
 * @param value 要设置的值 / The value to set
 * @param defaultContainer 默认容器提供者 / The default container provider
 * @return 之前的值，如果不存在则返回 null / The previous value, or null if not found
 */
fun <K1, K2, V> MutableMultiMap2<K1, K2, V>.put(
    key1: K1,
    key2: K2,
    value: V,
    defaultContainer: () -> MutableMap<K2, V> = { mutableMapOf() }
): V? {
    return this
        .getOrPut(key1, defaultContainer)
        .put(key2, value)
}

/**
 * 从二维 Map（值类型为 List）中获取指定键和索引的元素
 *
 * Gets an element from a two-level nested Map with List values by keys and index.
 * 通过两个键和一个索引从二级嵌套 Map（值类型为 List）中获取元素。
 */
operator fun <K1, K2, T> MultiMap2<K1, K2, List<T>>.get(key1: K1, key2: K2, i: Int): T? {
    return this[key1]?.get(key2)?.get(i)
}

/**
 * 从二维 Map（值类型为 List）中获取指定键和 ULong 索引的元素
 *
 * Gets an element from a two-level nested Map with List values by keys and ULong index.
 * 通过两个键和一个 ULong 索引从二级嵌套 Map（值类型为 List）中获取元素。
 */
operator fun <K1, K2, T> MultiMap2<K1, K2, List<T>>.get(key1: K1, key2: K2, i: ULong): T? {
    return this[key1]?.get(key2)?.get(i.toInt())
}

/**
 * 从三维 Map 中获取指定键的值
 *
 * Gets a value from a three-level nested Map by three keys.
 * 通过三个键从三级嵌套 Map 中获取值。
 */
operator fun <K1, K2, K3, V> MultiMap3<K1, K2, K3, V>.get(key1: K1, key2: K2, key3: K3): V? {
    return this[key1]?.get(key2)?.get(key3)
}

/**
 * 从三维可变 Map 中获取或设置值
 *
 * Gets a value from a three-level nested mutable Map, or puts and returns the default value if not found.
 * 从三级嵌套可变 Map 中获取值，如果未找到则设置并返回默认值。
 */
fun <K1, K2, K3, V> MutableMultiMap3<K1, K2, K3, V>.getOrPut(
    key1: K1,
    key2: K2,
    key3: K3,
    defaultValue: () -> V,
    defaultContainer1: () -> MutableMap<K2, MutableMap<K3, V>> = { mutableMapOf() },
    defaultContainer2: () -> MutableMap<K3, V> = { mutableMapOf() }
): V {
    return this
        .getOrPut(key1) { defaultContainer1() }
        .getOrPut(key2) { defaultContainer2() }
        .getOrPut(key3, defaultValue)
}

/**
 * 设置三维可变 Map 中的值（操作符形式）
 *
 * Sets a value in a three-level nested mutable Map using operator syntax.
 * 使用操作符语法设置三级嵌套可变 Map 中的值。
 */
operator fun <K1, K2, K3, V> MutableMultiMap3<K1, K2, K3, V>.set(
    key1: K1,
    key2: K2,
    key3: K3,
    value: V
): V? {
    return this.put(key1, key2, key3, value)
}

/**
 * 设置三维可变 Map 中的值
 *
 * Puts a value in a three-level nested mutable Map.
 * 设置三级嵌套可变 Map 中的值。
 */
fun <K1, K2, K3, V> MutableMultiMap3<K1, K2, K3, V>.put(
    key1: K1,
    key2: K2,
    key3: K3,
    value: V,
    defaultContainer1: () -> MutableMap<K2, MutableMap<K3, V>> = { mutableMapOf() },
    defaultContainer2: () -> MutableMap<K3, V> = { mutableMapOf() }
): V? {
    return this
        .getOrPut(key1, defaultContainer1)
        .getOrPut(key2, defaultContainer2)
        .put(key3, value)
}

/**
 * 从三维 Map（值类型为 List）中获取指定键和索引的元素
 *
 * Gets an element from a three-level nested Map with List values by keys and index.
 * 通过三个键和一个索引从三级嵌套 Map（值类型为 List）中获取元素。
 */
operator fun <K1, K2, K3, T> MultiMap3<K1, K2, K3, List<T>>.get(key1: K1, key2: K2, key3: K3, i: Int): T? {
    return this[key1]?.get(key2)?.get(key3)?.get(i)
}

/**
 * 从三维 Map（值类型为 List）中获取指定键和 ULong 索引的元素
 *
 * Gets an element from a three-level nested Map with List values by keys and ULong index.
 * 通过三个键和一个 ULong 索引从三级嵌套 Map（值类型为 List）中获取元素。
 */
operator fun <K1, K2, K3, T> MultiMap3<K1, K2, K3, List<T>>.get(key1: K1, key2: K2, key3: K3, i: ULong): T? {
    return this[key1]?.get(key2)?.get(key3)?.get(i.toInt())
}

/**
 * 从四维 Map 中获取指定键的值
 *
 * Gets a value from a four-level nested Map by four keys.
 * 通过四个键从四级嵌套 Map 中获取值。
 */
operator fun <K1, K2, K3, K4, V> MultiMap4<K1, K2, K3, K4, V>.get(key1: K1, key2: K2, key3: K3, key4: K4): V? {
    return this[key1]?.get(key2)?.get(key3)?.get(key4)
}

/**
 * 从四维可变 Map 中获取或设置值
 *
 * Gets a value from a four-level nested mutable Map, or puts and returns the default value if not found.
 * 从四级嵌套可变 Map 中获取值，如果未找到则设置并返回默认值。
 */
fun <K1, K2, K3, K4, V> MutableMultiMap4<K1, K2, K3, K4, V>.getOrPut(
    key1: K1,
    key2: K2,
    key3: K3,
    key4: K4,
    defaultValue: () -> V,
    defaultContainer1: () -> MutableMap<K2, MutableMap<K3, MutableMap<K4, V>>> = { mutableMapOf() },
    defaultContainer2: () -> MutableMap<K3, MutableMap<K4, V>> = { mutableMapOf() },
    defaultContainer3: () -> MutableMap<K4, V> = { mutableMapOf() }
): V {
    return this
        .getOrPut(key1) { defaultContainer1() }
        .getOrPut(key2) { defaultContainer2() }
        .getOrPut(key3) { defaultContainer3() }
        .getOrPut(key4, defaultValue)
}

/**
 * 设置四维可变 Map 中的值（操作符形式）
 *
 * Sets a value in a four-level nested mutable Map using operator syntax.
 * 使用操作符语法设置四级嵌套可变 Map 中的值。
 */
operator fun <K1, K2, K3, K4, V> MutableMultiMap4<K1, K2, K3, K4, V>.set(
    key1: K1,
    key2: K2,
    key3: K3,
    key4: K4,
    value: V
): V? {
    return this.put(key1, key2, key3, key4, value)
}

/**
 * 设置四维可变 Map 中的值
 *
 * Puts a value in a four-level nested mutable Map.
 * 设置四级嵌套可变 Map 中的值。
 */
fun <K1, K2, K3, K4, V> MutableMultiMap4<K1, K2, K3, K4, V>.put(
    key1: K1,
    key2: K2,
    key3: K3,
    key4: K4,
    value: V,
    defaultContainer1: () -> MutableMap<K2, MutableMap<K3, MutableMap<K4, V>>> = { mutableMapOf() },
    defaultContainer2: () -> MutableMap<K3, MutableMap<K4, V>> = { mutableMapOf() },
    defaultContainer3: () -> MutableMap<K4, V> = { mutableMapOf() }
): V? {
    return this
        .getOrPut(key1, defaultContainer1)
        .getOrPut(key2, defaultContainer2)
        .getOrPut(key3, defaultContainer3)
        .put(key4, value)
}

/**
 * 从四维 Map（值类型为 List）中获取指定键和索引的元素
 *
 * Gets an element from a four-level nested Map with List values by keys and index.
 * 通过四个键和一个索引从四级嵌套 Map（值类型为 List）中获取元素。
 */
operator fun <K1, K2, K3, K4, T> MultiMap4<K1, K2, K3, K4, List<T>>.get(key1: K1, key2: K2, key3: K3, key4: K4, i: Int): T? {
    return this[key1]?.get(key2)?.get(key3)?.get(key4)?.get(i)
}

/**
 * 从四维 Map（值类型为 List）中获取指定键和 ULong 索引的元素
 *
 * Gets an element from a four-level nested Map with List values by keys and ULong index.
 * 通过四个键和一个 ULong 索引从四级嵌套 Map（值类型为 List）中获取元素。
 */
operator fun <K1, K2, K3, K4, T> MultiMap4<K1, K2, K3, K4, List<T>>.get(key1: K1, key2: K2, key3: K3, key4: K4, i: ULong): T? {
    return this[key1]?.get(key2)?.get(key3)?.get(key4)?.get(i.toInt())
}