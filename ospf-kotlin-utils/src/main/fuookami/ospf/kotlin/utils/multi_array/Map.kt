package fuookami.ospf.kotlin.utils.multi_array

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.functional.*

operator fun <K, T : Any> Map<K, T>.get(k: AllDummyIndex): Iterable<T> {
    return this.values
}

operator fun <K, T : Any, S : Shape> Map<K, MultiArray<T, S>>.get(k: K, i: Int): T? {
    return this[k]?.get(i)
}

operator fun <K, T : Any, S : Shape> Map<K, MultiArray<T, S>>.get(k: K, i: UInt64): T? {
    return this[k]?.get(i)
}

operator fun <K, T : Any, S : Shape> Map<K, MultiArray<T, S>>.get(k: K, e: Indexed): T? {
    return this[k]?.get(e)
}

@JvmName("mapGetByIntArray")
operator fun <K, T : Any, S : Shape> Map<K, MultiArray<T, S>>.get(k: K, v: IntArray): T? {
    return this[k]?.get(v)
}

@JvmName("mapGetByInts")
operator fun <K, T : Any, S : Shape> Map<K, MultiArray<T, S>>.get(k: K, vararg v: Int): T? {
    return this[k]?.get(v)
}

operator fun <K, T : Any, S : Shape> Map<K, MultiArray<T, S>>.get(k: K, v: Iterable<UInt64>): T? {
    return this[k]?.get(v)
}

operator fun <K, T : Any, S : Shape> Map<K, MultiArray<T, S>>.get(k: K, vararg v: Indexed): T? {
    return this[k]?.get(v.map { it.index }.toIntArray())
}

operator fun <K, T : Any, S : Shape> Map<K, MultiArray<T, S>>.get(k: K, vararg v: Any): MultiArrayView<T, S>? {
    return this[k]?.get(*v)
}

operator fun <K, T : Any, S : Shape> Map<K, MutableMultiArray<T, S>>.set(k: K, i: Int, value: T) {
    this[k]!![i] = value
}

operator fun <K, T : Any, S : Shape> Map<K, MutableMultiArray<T, S>>.set(k: K, i: UInt64, value: T) {
    this[k]!![i] = value
}

operator fun <K, T : Any, S : Shape> Map<K, MutableMultiArray<T, S>>.set(k: K, e: Indexed, value: T) {
    this[k]!![e] = value
}

@JvmName("setByIntArray")
operator fun <K, T : Any, S : Shape> Map<K, MutableMultiArray<T, S>>.set(k: K, v: IntArray, value: T) {
    this[k]!![v] = value
}

@JvmName("setByInts")
operator fun <K, T : Any, S : Shape> Map<K, MutableMultiArray<T, S>>.set(k: K, vararg v: Int, value: T) {
    this[k]!![v] = value
}

operator fun <K, T : Any, S : Shape> Map<K, MutableMultiArray<T, S>>.set(k: K, v: Iterable<UInt64>, value: T) {
    this[k]!![v] = value
}

operator fun <K, T : Any, S : Shape> Map<K, MutableMultiArray<T, S>>.set(k: K, vararg v: Indexed, value: T) {
    this[k]!![v.map { it.index }.toIntArray()] = value
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

operator fun <K1, K2, T : Any, S : Shape> MultiMap2<K1, K2, MultiArray<T, S>>.get(k1: K1, k2: K2, i: Int): T? {
    return this[k1]?.get(k2)?.get(i)
}

operator fun <K1, K2, T : Any, S : Shape> MultiMap2<K1, K2, MultiArray<T, S>>.get(k1: K1, k2: K2, i: UInt64): T? {
    return this[k1]?.get(k2)?.get(i)
}

operator fun <K1, K2, T : Any, S : Shape> MultiMap2<K1, K2, MultiArray<T, S>>.get(k1: K1, k2: K2, e: Indexed): T? {
    return this[k1]?.get(k2)?.get(e)
}

@JvmName("multiMap2GetByIntArray")
operator fun <K1, K2, T : Any, S : Shape> MultiMap2<K1, K2, MultiArray<T, S>>.get(k1: K1, k2: K2, v: IntArray): T? {
    return this[k1]?.get(k2)?.get(v)
}

@JvmName("multiMap2GetByInts")
operator fun <K1, K2, T : Any, S : Shape> MultiMap2<K1, K2, MultiArray<T, S>>.get(k1: K1, k2: K2, vararg v: Int): T? {
    return this[k1]?.get(k2)?.get(v)
}

operator fun <K1, K2, T : Any, S : Shape> MultiMap2<K1, K2, MultiArray<T, S>>.get(k1: K1, k2: K2, v: Iterable<UInt64>): T? {
    return this[k1]?.get(k2)?.get(v)
}

operator fun <K1, K2, T : Any, S : Shape> MultiMap2<K1, K2, MultiArray<T, S>>.get(k1: K1, k2: K2, vararg v: Indexed): T? {
    return this[k1]?.get(k2)?.get(v.map { it.index }.toIntArray())
}

operator fun <K1, K2, T : Any, S : Shape> MultiMap2<K1, K2, MultiArray<T, S>>.get(k1: K1, k2: K2, vararg v: Any): MultiArrayView<T, S>? {
    return this[k1]?.get(k2)?.get(*v)
}

operator fun <K1, K2, T : Any, S : Shape> MultiMap2<K1, K2, MutableMultiArray<T, S>>.set(k1: K1, k2: K2, i: Int, value: T) {
    this[k1, k2]!![i] = value
}

operator fun <K1, K2, T : Any, S : Shape> MultiMap2<K1, K2, MutableMultiArray<T, S>>.set(k1: K1, k2: K2, i: UInt64, value: T) {
    this[k1, k2]!![i] = value
}

operator fun <K1, K2, T : Any, S : Shape> MultiMap2<K1, K2, MutableMultiArray<T, S>>.set(k1: K1, k2: K2, e: Indexed, value: T) {
    this[k1, k2]!![e] = value
}

@JvmName("multiMap2SetByIntArray")
operator fun <K1, K2, T : Any, S : Shape> MultiMap2<K1, K2, MutableMultiArray<T, S>>.set(k1: K1, k2: K2, v: IntArray, value: T) {
    this[k1, k2]!![v] = value
}

@JvmName("multiMap2SetByInt")
operator fun <K1, K2, T : Any, S : Shape> MultiMap2<K1, K2, MutableMultiArray<T, S>>.set(k1: K1, k2: K2, vararg v: Int, value: T) {
    this[k1, k2]!![v] = value
}

operator fun <K1, K2, T : Any, S : Shape> MultiMap2<K1, K2, MutableMultiArray<T, S>>.set(k1: K1, k2: K2, v: Iterable<UInt64>, value: T) {
    this[k1, k2]!![v] = value
}

operator fun <K1, K2, T : Any, S : Shape> MultiMap2<K1, K2, MutableMultiArray<T, S>>.set(k1: K1, k2: K2, vararg v: Indexed, value: T) {
    this[k1, k2]!![v.map { it.index }.toIntArray()] = value
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

operator fun <K1, K2, K3, T : Any, S : Shape> MultiMap3<K1, K2, K3, MultiArray<T, S>>.get(k1: K1, k2: K2, k3: K3, i: Int): T? {
    return this[k1]?.get(k2)?.get(k3)?.get(i)
}

operator fun <K1, K2, K3, T : Any, S : Shape> MultiMap3<K1, K2, K3, MultiArray<T, S>>.get(k1: K1, k2: K2, k3: K3, i: UInt64): T? {
    return this[k1]?.get(k2)?.get(k3)?.get(i)
}

operator fun <K1, K2, K3, T : Any, S : Shape> MultiMap3<K1, K2, K3, MultiArray<T, S>>.get(k1: K1, k2: K2, k3: K3, e: Indexed): T? {
    return this[k1]?.get(k2)?.get(k3)?.get(e)
}

@JvmName("multiMap3GetByIntArray")
operator fun <K1, K2, K3, T : Any, S : Shape> MultiMap3<K1, K2, K3, MultiArray<T, S>>.get(k1: K1, k2: K2, k3: K3, v: IntArray): T? {
    return this[k1]?.get(k2)?.get(k3)?.get(v)
}

@JvmName("multiMap3GetByInts")
operator fun <K1, K2, K3, T : Any, S : Shape> MultiMap3<K1, K2, K3, MultiArray<T, S>>.get(k1: K1, k2: K2, k3: K3, vararg v: Int): T? {
    return this[k1]?.get(k2)?.get(k3)?.get(v)
}

operator fun <K1, K2, K3, T : Any, S : Shape> MultiMap3<K1, K2, K3, MultiArray<T, S>>.get(k1: K1, k2: K2, k3: K3, v: Iterable<UInt64>): T? {
    return this[k1]?.get(k2)?.get(k3)?.get(v)
}

operator fun <K1, K2, K3, T : Any, S : Shape> MultiMap3<K1, K2, K3, MultiArray<T, S>>.get(k1: K1, k2: K2, k3: K3, vararg v: Indexed): T? {
    return this[k1]?.get(k2)?.get(k3)?.get(v.map { it.index }.toIntArray())
}

operator fun <K1, K2, K3, T : Any, S : Shape> MultiMap3<K1, K2, K3, MultiArray<T, S>>.get(k1: K1, k2: K2, k3: K3, vararg v: Any): MultiArrayView<T, S>? {
    return this[k1]?.get(k2)?.get(k3)?.get(*v)
}

operator fun <K1, K2, K3, T : Any, S : Shape> MultiMap3<K1, K2, K3, MutableMultiArray<T, S>>.set(k1: K1, k2: K2, k3: K3, i: Int, value: T) {
    this[k1, k2, k3]!![i] = value
}

operator fun <K1, K2, K3, T : Any, S : Shape> MultiMap3<K1, K2, K3, MutableMultiArray<T, S>>.set(k1: K1, k2: K2, k3: K3, i: UInt64, value: T) {
    this[k1, k2, k3]!![i] = value
}

operator fun <K1, K2, K3, T : Any, S : Shape> MultiMap3<K1, K2, K3, MutableMultiArray<T, S>>.set(k1: K1, k2: K2, k3: K3, e: Indexed, value: T) {
    this[k1, k2, k3]!![e] = value
}

@JvmName("multiMap3SetByIntArray")
operator fun <K1, K2, K3, T : Any, S : Shape> MultiMap3<K1, K2, K3, MutableMultiArray<T, S>>.set(k1: K1, k2: K2, k3: K3, v: IntArray, value: T) {
    this[k1, k2, k3]!![v] = value
}

@JvmName("multiMap3SetByInts")
operator fun <K1, K2, K3, T : Any, S : Shape> MultiMap3<K1, K2, K3, MutableMultiArray<T, S>>.set(k1: K1, k2: K2, k3: K3, vararg v: Int, value: T) {
    this[k1, k2, k3]!![v] = value
}

operator fun <K1, K2, K3, T : Any, S : Shape> MultiMap3<K1, K2, K3, MutableMultiArray<T, S>>.set(k1: K1, k2: K2, k3: K3, v: Iterable<UInt64>, value: T) {
    this[k1, k2, k3]!![v] = value
}

operator fun <K1, K2, K3, T : Any, S : Shape> MultiMap3<K1, K2, K3, MutableMultiArray<T, S>>.set(k1: K1, k2: K2, k3: K3, vararg v: Indexed, value: T) {
    this[k1, k2, k3]!![v.map { it.index }.toIntArray()] = value
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

operator fun <K1, K2, K3, K4, T : Any, S : Shape> MultiMap4<K1, K2, K3, K4, MultiArray<T, S>>.get(k1: K1, k2: K2, k3: K3, k4: K4, i: Int): T? {
    return this[k1]?.get(k2)?.get(k3)?.get(k4)?.get(i)
}

operator fun <K1, K2, K3, K4, T : Any, S : Shape> MultiMap4<K1, K2, K3, K4, MultiArray<T, S>>.get(k1: K1, k2: K2, k3: K3, k4: K4, i: UInt64): T? {
    return this[k1]?.get(k2)?.get(k3)?.get(k4)?.get(i)
}

operator fun <K1, K2, K3, K4, T : Any, S : Shape> MultiMap4<K1, K2, K3, K4, MultiArray<T, S>>.get(k1: K1, k2: K2, k3: K3, k4: K4, e: Indexed): T? {
    return this[k1]?.get(k2)?.get(k3)?.get(k4)?.get(e)
}

@JvmName("multiMap3GetByIntArray")
operator fun <K1, K2, K3, K4, T : Any, S : Shape> MultiMap4<K1, K2, K3, K4, MultiArray<T, S>>.get(k1: K1, k2: K2, k3: K3, k4: K4, v: IntArray): T? {
    return this[k1]?.get(k2)?.get(k3)?.get(k4)?.get(v)
}

@JvmName("multiMap3GetByInts")
operator fun <K1, K2, K3, K4, T : Any, S : Shape> MultiMap4<K1, K2, K3, K4, MultiArray<T, S>>.get(k1: K1, k2: K2, k3: K3, k4: K4, vararg v: Int): T? {
    return this[k1]?.get(k2)?.get(k3)?.get(k4)?.get(v)
}

operator fun <K1, K2, K3, K4, T : Any, S : Shape> MultiMap4<K1, K2, K3, K4, MultiArray<T, S>>.get(k1: K1, k2: K2, k3: K3, k4: K4, v: Iterable<UInt64>): T? {
    return this[k1]?.get(k2)?.get(k3)?.get(k4)?.get(v)
}

operator fun <K1, K2, K3, K4, T : Any, S : Shape> MultiMap4<K1, K2, K3, K4, MultiArray<T, S>>.get(k1: K1, k2: K2, k3: K3, k4: K4, vararg v: Indexed): T? {
    return this[k1]?.get(k2)?.get(k3)?.get(k4)?.get(v.map { it.index }.toIntArray())
}

operator fun <K1, K2, K3, K4, T : Any, S : Shape> MultiMap4<K1, K2, K3, K4, MultiArray<T, S>>.get(k1: K1, k2: K2, k3: K3, k4: K4, vararg v: Any): MultiArrayView<T, S>? {
    return this[k1]?.get(k2)?.get(k3)?.get(k4)?.get(*v)
}

operator fun <K1, K2, K3, K4, T : Any, S : Shape> MultiMap4<K1, K2, K3, K4, MutableMultiArray<T, S>>.set(k1: K1, k2: K2, k3: K3, k4: K4, i: Int, value: T) {
    this[k1, k2, k3, k4]!![i] = value
}

operator fun <K1, K2, K3, K4, T : Any, S : Shape> MultiMap4<K1, K2, K3, K4, MutableMultiArray<T, S>>.set(k1: K1, k2: K2, k3: K3, k4: K4, i: UInt64, value: T) {
    this[k1, k2, k3, k4]!![i] = value
}

operator fun <K1, K2, K3, K4, T : Any, S : Shape> MultiMap4<K1, K2, K3, K4, MutableMultiArray<T, S>>.set(k1: K1, k2: K2, k3: K3, k4: K4, e: Indexed, value: T) {
    this[k1, k2, k3, k4]!![e] = value
}

@JvmName("multiMap3SetByIntArray")
operator fun <K1, K2, K3, K4, T : Any, S : Shape> MultiMap4<K1, K2, K3, K4, MutableMultiArray<T, S>>.set(k1: K1, k2: K2, k3: K3, k4: K4, v: IntArray, value: T) {
    this[k1, k2, k3, k4]!![v] = value
}

@JvmName("multiMap3SetByInts")
operator fun <K1, K2, K3, K4, T : Any, S : Shape> MultiMap4<K1, K2, K3, K4, MutableMultiArray<T, S>>.set(k1: K1, k2: K2, k3: K3, k4: K4, vararg v: Int, value: T) {
    this[k1, k2, k3, k4]!![v] = value
}

operator fun <K1, K2, K3, K4, T : Any, S : Shape> MultiMap4<K1, K2, K3, K4, MutableMultiArray<T, S>>.set(k1: K1, k2: K2, k3: K3, k4: K4, v: Iterable<UInt64>, value: T) {
    this[k1, k2, k3, k4]!![v] = value
}

operator fun <K1, K2, K3, K4, T : Any, S : Shape> MultiMap4<K1, K2, K3, K4, MutableMultiArray<T, S>>.set(k1: K1, k2: K2, k3: K3, k4: K4, vararg v: Indexed, value: T) {
    this[k1, k2, k3, k4]!![v.map { it.index }.toIntArray()] = value
}
