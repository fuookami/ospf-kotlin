package fuookami.ospf.kotlin.utils.multi_array

import fuookami.ospf.kotlin.utils.functional.*

operator fun <K, T : Any> Map<K, T>.get(k: AllDummyIndex): Iterable<T> {
    return this.values
}

operator fun <K1, K2, T: Any> MultiMap2<K1, K2, T>.get(k1: AllDummyIndex, k2: K2): Iterable<T> {
    return this.values.mapNotNull { it[k2] }
}

operator fun <K1, K2, T: Any> MultiMap2<K1, K2, T>.get(k1: K1, k2: AllDummyIndex): Iterable<T> {
    return this[k1]?.values ?: emptyList()
}

operator fun <K1, K2, T: Any> MultiMap2<K1, K2, T>.get(k1: AllDummyIndex, k2: AllDummyIndex): Iterable<T> {
    return this.values.flatMap { it.values }
}

operator fun <K1, K2, K3, T: Any> MultiMap3<K1, K2, K3, T>.get(k1: AllDummyIndex, k2: K2, k3: K3): Iterable<T> {
    return this.values.mapNotNull { it[k2, k3] }
}

operator fun <K1, K2, K3, T: Any> MultiMap3<K1, K2, K3, T>.get(k1: K1, k2: AllDummyIndex, k3: K3): Iterable<T> {
    return this[k1]?.get(k2, k3) ?: emptyList()
}

operator fun <K1, K2, K3, T: Any> MultiMap3<K1, K2, K3, T>.get(k1: K1, k2: K2, k3: AllDummyIndex): Iterable<T> {
    return this[k1]?.get(k2, k3) ?: emptyList()
}

operator fun <K1, K2, K3, T: Any> MultiMap3<K1, K2, K3, T>.get(k1: AllDummyIndex, k2: AllDummyIndex, k3: K3): Iterable<T> {
    return this.values.flatMap { it[k2, k3] }
}

operator fun <K1, K2, K3, T: Any> MultiMap3<K1, K2, K3, T>.get(k1: AllDummyIndex, k2: K2, k3: AllDummyIndex): Iterable<T> {
    return this.values.flatMap { it[k2, k3] }
}

operator fun <K1, K2, K3, T: Any> MultiMap3<K1, K2, K3, T>.get(k1: K1, k2: AllDummyIndex, k3: AllDummyIndex): Iterable<T> {
    return this[k1]?.get(k2, k3) ?: emptyList()
}

operator fun <K1, K2, K3, T: Any> MultiMap3<K1, K2, K3, T>.get(k1: AllDummyIndex, k2: AllDummyIndex, k3: AllDummyIndex): Iterable<T> {
    return this.values.flatMap { it[k2, k3] }
}

operator fun <K1, K2, K3, K4, T: Any> MultiMap4<K1, K2, K3, K4, T>.get(k1: AllDummyIndex, k2: K2, k3: K3, k4: K4): Iterable<T> {
    return this.values.mapNotNull { it[k2, k3, k4] }
}

operator fun <K1, K2, K3, K4, T : Any> MultiMap4<K1, K2, K3, K4, T>.get(k1: K1, k2: AllDummyIndex, k3: K3, k4: K4): Iterable<T> {
    return this[k1]?.get(k2, k3, k4) ?: emptyList()
}

operator fun <K1, K2, K3, K4, T: Any> MultiMap4<K1, K2, K3, K4, T>.get(k1: K1, k2: K2, k3: AllDummyIndex, k4: K4): Iterable<T> {
    return this[k1]?.get(k2, k3, k4) ?: emptyList()
}

operator fun <K1, K2, K3, K4, T: Any> MultiMap4<K1, K2, K3, K4, T>.get(k1: K1, k2: K2, k3: K3, k4: AllDummyIndex): Iterable<T> {
    return this[k1]?.get(k2, k3, k4) ?: emptyList()
}

operator fun <K1, K2, K3, K4, T: Any> MultiMap4<K1, K2, K3, K4, T>.get(k1: AllDummyIndex, k2: AllDummyIndex, k3: K3, k4: K4): Iterable<T> {
    return this.values.flatMap { it[k2, k3, k4] }
}

operator fun <K1, K2, K3, K4, T: Any> MultiMap4<K1, K2, K3, K4, T>.get(k1: AllDummyIndex, k2: K2, k3: AllDummyIndex, k4: K4): Iterable<T> {
    return this.values.flatMap { it[k2, k3, k4] }
}

operator fun <K1, K2, K3, K4, T: Any> MultiMap4<K1, K2, K3, K4, T>.get(k1: AllDummyIndex, k2: K2, k3: K3, k4: AllDummyIndex): Iterable<T> {
    return this.values.flatMap { it[k2, k3, k4] }
}

operator fun <K1, K2, K3, K4, T: Any> MultiMap4<K1, K2, K3, K4, T>.get(k1: K1, k2: AllDummyIndex, k3: AllDummyIndex, k4: K4): Iterable<T> {
    return this[k1]?.get(k2, k3, k4) ?: emptyList()
}

operator fun <K1, K2, K3, K4, T: Any> MultiMap4<K1, K2, K3, K4, T>.get(k1: K1, k2: AllDummyIndex, k3: K3, k4: AllDummyIndex): Iterable<T> {
    return this[k1]?.get(k2, k3, k4) ?: emptyList()
}

operator fun <K1, K2, K3, K4, T: Any> MultiMap4<K1, K2, K3, K4, T>.get(k1: K1, k2: K2, k3: AllDummyIndex, k4: AllDummyIndex): Iterable<T> {
    return this[k1]?.get(k2, k3, k4) ?: emptyList()
}

operator fun <K1, K2, K3, K4, T: Any> MultiMap4<K1, K2, K3, K4, T>.get(k1: AllDummyIndex, k2: AllDummyIndex, k3: AllDummyIndex, k4: K4): Iterable<T> {
    return this.values.flatMap { it[k2, k3, k4] }
}

operator fun <K1, K2, K3, K4, T: Any> MultiMap4<K1, K2, K3, K4, T>.get(k1: AllDummyIndex, k2: AllDummyIndex, k3: K3, k4: AllDummyIndex): Iterable<T> {
    return this.values.flatMap { it[k2, k3, k4] }
}

operator fun <K1, K2, K3, K4, T: Any> MultiMap4<K1, K2, K3, K4, T>.get(k1: AllDummyIndex, k2: K2, k3: AllDummyIndex, k4: AllDummyIndex): Iterable<T> {
    return this.values.flatMap { it[k2, k3, k4] }
}

operator fun <K1, K2, K3, K4, T: Any> MultiMap4<K1, K2, K3, K4, T>.get(k1: K1, k2: AllDummyIndex, k3: AllDummyIndex, k4: AllDummyIndex): Iterable<T> {
    return this[k1]?.get(k2, k3, k4) ?: emptyList()
}

operator fun <K1, K2, K3, K4, T: Any> MultiMap4<K1, K2, K3, K4, T>.get(k1: AllDummyIndex, k2: AllDummyIndex, k3: AllDummyIndex, k4: AllDummyIndex): Iterable<T> {
    return this.values.flatMap { it[k2, k3, k4] }
}
