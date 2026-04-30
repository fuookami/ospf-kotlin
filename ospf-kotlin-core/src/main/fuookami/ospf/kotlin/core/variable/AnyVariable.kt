package fuookami.ospf.kotlin.core.variable

import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64

/**
 * Type-erased variable wrapper (aligns with Rust AnyVariable<V>).
 * Unifies all VariableTypeTrait variants into a single V-parametric type.
 *
 * In Rust, AnyVariable<V> wraps VariableData<V> with bounds in V.
 * In Kotlin, we delegate to AbstractVariableItem which already holds all metadata,
 * and provide V-typed accessors via IntoValue<V> conversion.
 */
class AnyVariable<V : RealNumber<V>>(
    val data: AbstractVariableItem<*, *>
) {
    val id: VariableItemKey get() = data.key
    val index: Int get() = data.index
    val name: String get() = data.name
    val displayName: String? get() = data.displayName
    val varType: VariableType<*> get() = data.type

    val lowerBoundFlt64: Flt64? get() = data.lowerBound?.value?.toFlt64()
    val upperBoundFlt64: Flt64? get() = data.upperBound?.value?.toFlt64()

    fun lowerBound(converter: IntoValue<V>): V? = lowerBoundFlt64?.let { converter.intoValue(it) }
    fun upperBound(converter: IntoValue<V>): V? = upperBoundFlt64?.let { converter.intoValue(it) }

    fun isValidValue(value: Flt64): Boolean {
        val lb = lowerBoundFlt64
        val ub = upperBoundFlt64
        if (lb != null && value < lb) return false
        if (ub != null && value > ub) return false
        return true
    }

    fun isValidValue(value: V): Boolean {
        val f64 = value.toFlt64()
        return isValidValue(f64)
    }

    override fun hashCode(): Int = data.hashCode()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AnyVariable<*>) return false
        return data == other.data
    }
    override fun toString(): String = data.toString()

    companion object {
        fun <V : RealNumber<V>> from(item: AbstractVariableItem<*, *>): AnyVariable<V> {
            return AnyVariable(item)
        }
    }
}

typealias AnyVariableF64 = AnyVariable<F64>
