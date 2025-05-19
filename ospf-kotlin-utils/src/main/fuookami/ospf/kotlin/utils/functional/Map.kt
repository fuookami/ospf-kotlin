package fuookami.ospf.kotlin.utils.functional

import fuookami.ospf.kotlin.utils.math.*

operator fun <K, T> Map<K, List<T>>.get(key: K, i: Int): T? {
    return this[key]?.get(i)
}

operator fun <K, T> Map<K, List<T>>.get(key: K, i: UInt64): T? {
    return this[key]?.get(i)
}

operator fun <K1, K2, V> Map<K1, Map<K2, V>>.get(key1: K1, key2: K2): V? {
    return this[key1]?.get(key2)
}

fun <K1, K2, V> MutableMap<K1, MutableMap<K2, V>>.getOrPut(
    key1: K1,
    key2: K2,
    defaultValue: () -> V,
    defaultContainer: () -> MutableMap<K2, V> = { mutableMapOf() }
): V {
    return this
        .getOrPut(key1) { defaultContainer() }
        .getOrPut(key2, defaultValue)
}

operator fun <K1, K2, V> MutableMap<K1, MutableMap<K2, V>>.set(
    key1: K1,
    key2: K2,
    value: V
): V? {
    return this.put(key1, key2, value)
}

fun <K1, K2, V> MutableMap<K1, MutableMap<K2, V>>.put(
    key1: K1,
    key2: K2,
    value: V,
    defaultContainer: () -> MutableMap<K2, V> = { mutableMapOf() }
): V? {
    return this
        .getOrPut(key1, defaultContainer)
        .put(key2, value)
}

operator fun <K1, K2, T> Map<K1, Map<K2, List<T>>>.get(key1: K1, key2: K2, i: Int): T? {
    return this[key1]?.get(key2)?.get(i)
}

operator fun <K1, K2, T> Map<K1, Map<K2, List<T>>>.get(key1: K1, key2: K2, i: UInt64): T? {
    return this[key1]?.get(key2)?.get(i)
}

operator fun <K1, K2, K3, V> Map<K1, Map<K2, Map<K3, V>>>.get(key1: K1, key2: K2, key3: K3): V? {
    return this[key1]?.get(key2)?.get(key3)
}

fun <K1, K2, K3, V> MutableMap<K1, MutableMap<K2, MutableMap<K3, V>>>.getOrPut(
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

operator fun <K1, K2, K3, V> MutableMap<K1, MutableMap<K2, MutableMap<K3, V>>>.set(
    key1: K1,
    key2: K2,
    key3: K3,
    value: V
): V? {
    return this.put(key1, key2, key3, value)
}

fun <K1, K2, K3, V> MutableMap<K1, MutableMap<K2, MutableMap<K3, V>>>.put(
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

operator fun <K1, K2, K3, T> Map<K1, Map<K2, Map<K3, List<T>>>>.get(key1: K1, key2: K2, key3: K3, i: Int): T? {
    return this[key1]?.get(key2)?.get(key3)?.get(i)
}

operator fun <K1, K2, K3, T> Map<K1, Map<K2, Map<K3, List<T>>>>.get(key1: K1, key2: K2, key3: K3, i: UInt64): T? {
    return this[key1]?.get(key2)?.get(key3)?.get(i)
}

operator fun <K1, K2, K3, K4, V> Map<K1, Map<K2, Map<K3, Map<K4, V>>>>.get(key1: K1, key2: K2, key3: K3, key4: K4): V? {
    return this[key1]?.get(key2)?.get(key3)?.get(key4)
}

fun <K1, K2, K3, K4, V> MutableMap<K1, MutableMap<K2, MutableMap<K3, MutableMap<K4, V>>>>.getOrPut(
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

operator fun <K1, K2, K3, K4, V> MutableMap<K1, MutableMap<K2, MutableMap<K3, MutableMap<K4, V>>>>.set(
    key1: K1,
    key2: K2,
    key3: K3,
    key4: K4,
    value: V
): V? {
    return this.put(key1, key2, key3, key4, value)
}

fun <K1, K2, K3, K4, V> MutableMap<K1, MutableMap<K2, MutableMap<K3, MutableMap<K4, V>>>>.put(
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

operator fun <K1, K2, K3, K4, T> Map<K1, Map<K2, Map<K3, Map<K4, List<T>>>>>.get(key1: K1, key2: K2, key3: K3, key4: K4, i: Int): T? {
    return this[key1]?.get(key2)?.get(key3)?.get(key4)?.get(i)
}

operator fun <K1, K2, K3, K4, T> Map<K1, Map<K2, Map<K3, Map<K4, List<T>>>>>.get(key1: K1, key2: K2, key3: K3, key4: K4, i: UInt64): T? {
    return this[key1]?.get(key2)?.get(key3)?.get(key4)?.get(i)
}
