package fuookami.ospf.kotlin.utils.multi_array

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.math.value_range.*

operator fun <K, T : Any> Map<K, T>.get(k: AllDummyIndex): Iterable<T> {
    return this.values
}

operator fun <K1, K2, T: Any> Map<K1, Map<K2, T>>.get(k1: K1, k2: AllDummyIndex): Iterable<T> {
    return this[k1]?.values ?: emptyList()
}

operator fun <K1, K2, T: Any> Map<K1, Map<K2, T>>.get(k1: AllDummyIndex, k2: AllDummyIndex): Iterable<T> {
    return this.values.flatMap { it.values }
}

operator fun <K1, K2, T: Any> Map<K1, Map<K2, T>>.get(k1: AllDummyIndex, k2: K2): Iterable<T> {
    return this.values.mapNotNull { it[k2] }
}

operator fun <K1, K2, K3, T: Any> Map<K1, Map<K2, Map<K3, T>>>.get(k1: AllDummyIndex, k2: K2, k3: K3): Iterable<T> {
    return this.values.mapNotNull { it[k2] }.mapNotNull { it[k3] }
}

operator fun <K1, K2, K3, T: Any> Map<K1, Map<K2, Map<K3, T>>>.get(k1: K1, k2: AllDummyIndex, k3: K3): Iterable<T> {
    return this[k1]?.values?.mapNotNull { it[k3] } ?: emptyList()
}

operator fun <K1, K2, K3, T: Any> Map<K1, Map<K2, Map<K3, T>>>.get(k1: K1, k2: K2, k3: AllDummyIndex): Iterable<T> {
    return this[k1]?.get(k2)?.values ?: emptyList()
}

operator fun <K1, K2, K3, T: Any> Map<K1, Map<K2, Map<K3, T>>>.get(k1: K1, k2: AllDummyIndex, k3: AllDummyIndex): Iterable<T> {
    return this.values.flatMap { it[k2, k3] }
}

operator fun <K1, K2, K3, T: Any> Map<K1, Map<K2, Map<K3, T>>>.get(k1: AllDummyIndex, k2: K2, k3: AllDummyIndex): Iterable<T> {
    return this.values.mapNotNull { it[k2] }.flatMap { it.values }
}

operator fun <K1, K2, K3, T: Any> Map<K1, Map<K2, Map<K3, T>>>.get(k1: AllDummyIndex, k2: AllDummyIndex, k3: K3): Iterable<T> {
    return this.values.flatMap { it.values }.mapNotNull { it[k3] }
}

operator fun <K1, K2, K3, T: Any> Map<K1, Map<K2, Map<K3, T>>>.get(k1: AllDummyIndex, k2: AllDummyIndex, k3: AllDummyIndex): Iterable<T> {
    return this.values.flatMap { it.values }.flatMap { it.values }
}
