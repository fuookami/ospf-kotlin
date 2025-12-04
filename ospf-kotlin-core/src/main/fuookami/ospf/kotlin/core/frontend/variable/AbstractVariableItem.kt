package fuookami.ospf.kotlin.core.frontend.variable

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.symbol.*
import fuookami.ospf.kotlin.utils.math.value_range.*
import fuookami.ospf.kotlin.utils.operator.*
import fuookami.ospf.kotlin.utils.physics.unit.*
import fuookami.ospf.kotlin.utils.physics.quantity.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*

data class VariableItemKey(
    val identifier: UInt64,
    val index: Int
) : Ord<VariableItemKey> {
    override fun partialOrd(rhs: VariableItemKey): Order {
        return if (this.identifier < rhs.identifier) {
            Order.Less()
        } else if (this.identifier > rhs.identifier) {
            Order.Greater()
        } else {
            index ord rhs.index
        }
    }

    override fun hashCode(): Int {
        return identifier.toInt() * 31 + index
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VariableItemKey) return false

        if (identifier != other.identifier) return false
        if (index != other.index) return false

        return true
    }
}

abstract class AbstractVariableItem<T, Type : VariableType<T>>(
    val type: Type,
    override var name: String,
    val constants: RealNumberConstants<T>
): Symbol, ToLinearPolynomial<LinearPolynomial>, ToQuadraticPolynomial<QuadraticPolynomial>
        where T : RealNumber<T>, T : NumberField<T> {
    abstract val dimension: Int
    abstract val identifier: UInt64
    abstract val index: Int
    abstract val vectorView: IntArray
    override val displayName get() = name

    val uindex get() = UInt64(index)
    val uvector by lazy { vectorView.map { UInt64(it) } }

    val range = Range(type, constants)

    val lowerBound: Bound<Flt64>? get() = range.lowerBound?.toFlt64()
    val upperBound: Bound<Flt64>? get() = range.upperBound?.toFlt64()

    val key get() = VariableItemKey(identifier, index)

    open infix fun belongsTo(item: AbstractVariableItem<*, *>): Boolean {
        return identifier == item.identifier
    }

    open infix fun belongsTo(combination: VariableCombination<*, *, *>): Boolean {
        return identifier == combination.identifier
    }

    override fun toLinearPolynomial(): LinearPolynomial {
        return LinearPolynomial(this)
    }

    override fun toQuadraticPolynomial(): QuadraticPolynomial {
        return QuadraticPolynomial(this)
    }

    override fun hashCode() = key.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AbstractVariableItem<*, *>) return false

        if (identifier != other.identifier) return false
        if (index != other.index) return false

        return true
    }

    override fun toString() = name
}

internal data object IdentifierGenerator {
    var next: UInt64 = UInt64.zero

    fun flush() {
        next = UInt64.zero
    }

    fun gen(): UInt64 {
        val thisValue = next;
        ++next;
        return thisValue;
    }
}

typealias VariableItem<Type> = AbstractVariableItem<*, Type>
typealias BinVariable = AbstractVariableItem<UInt8, Binary>
typealias TernaryVariable = AbstractVariableItem<UInt8, Ternary>
typealias BalancedTernaryVariable = AbstractVariableItem<Int8, BalancedTernary>
typealias PercentageVariable = AbstractVariableItem<Flt64, Percentage>
typealias IntVariable = AbstractVariableItem<Int64, Integer>
typealias UIntVariable = AbstractVariableItem<UInt64, UInteger>
typealias RealVariable = AbstractVariableItem<Flt64, Continuous>
typealias URealVariable = AbstractVariableItem<Flt64, UContinuous>

typealias QuantityVariableItem<Type> = Quantity<AbstractVariableItem<*, Type>>
typealias QuantityBinVariable = Quantity<BinVariable>
typealias QuantityTernaryVariable = Quantity<TernaryVariable>
typealias QuantityBalancedTernaryVariable = Quantity<BalancedTernaryVariable>
typealias QuantityPercentageVariable = Quantity<PercentageVariable>
typealias QuantityIntVariable = Quantity<IntVariable>
typealias QuantityUIntVariable = Quantity<UIntVariable>
typealias QuantityRealVariable = Quantity<RealVariable>
typealias QuantityURealVariable = Quantity<URealVariable>

operator fun AbstractVariableItem<*, *>.times(rhs: PhysicalUnit): Quantity<AbstractVariableItem<*, *>> {
    return Quantity(this, rhs)
}

operator fun AbstractVariableItem<*, *>.div(rhs: PhysicalUnit): Quantity<AbstractVariableItem<*, *>> {
    return Quantity(this, rhs.reciprocal())
}
