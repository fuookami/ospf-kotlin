/**
 * 抽象变量项及其键、类型别名和物理单位扩展。
 * Abstract variable item, its key, type aliases, and physical unit extensions.
 */
package fuookami.ospf.kotlin.core.variable

import fuookami.ospf.kotlin.utils.functional.Ord
import fuookami.ospf.kotlin.utils.functional.Order
import fuookami.ospf.kotlin.utils.functional.ord
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.inequality.QuadraticInequalityOf
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.concept.RealNumberConstants
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.value_range.Bound
import fuookami.ospf.kotlin.quantities.unit.PhysicalUnit
import fuookami.ospf.kotlin.quantities.unit.reciprocal
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.core.model.mechanism.eq
import fuookami.ospf.kotlin.core.model.mechanism.geq
import fuookami.ospf.kotlin.core.model.mechanism.leq

/**
 * 变量项键，由标识符和索引唯一确定一个变量项。
 * Variable item key uniquely identifying a variable item by identifier and index.
 *
 * @property identifier 变量标识符 / Variable identifier
 * @property index 变量索引 / Variable index
 */
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

/**
 * 抽象变量项基类，持有变量类型、名称、常量和范围等元数据。
 * Abstract base class for variable items, holding metadata such as type, name, constants, and range.
 *
 * @property type 变量类型 / Variable type
 * @property name 变量名称 / Variable name
 * @property constants 数值类型常量 / Numeric type constants
 */
abstract class AbstractVariableItem<T, Type : VariableType<T>>(
    val type: Type,
    override var name: String,
    val constants: RealNumberConstants<T>
) : Symbol
        where T : RealNumber<T>, T : NumberField<T> {
    abstract val dimension: Int
    abstract val identifier: UInt64
    abstract val index: Int
    abstract val vectorView: IntArray
    override val displayName get() = name

    val uindex get() = UInt64(index)
    val uvector by lazy { vectorView.map { UInt64(it) } }

    val range = Range(type, constants)

    val lowerBound: Bound<fuookami.ospf.kotlin.math.algebra.number.Flt64>? get() = range.lowerBound?.toFlt64()
    val upperBound: Bound<fuookami.ospf.kotlin.math.algebra.number.Flt64>? get() = range.upperBound?.toFlt64()

    val key get() = VariableItemKey(identifier, index)

    open infix fun belongsTo(item: AbstractVariableItem<*, *>): Boolean {
        return identifier == item.identifier
    }

    open infix fun belongsTo(combination: VariableCombination<*, *, *>): Boolean {
        return identifier == combination.identifier
    }

    fun toMathLinearInequality(): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
        val poly = LinearPolynomial(monomials = listOf(LinearMonomial(Flt64.one, this)), constant = Flt64.zero)
        return LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(poly, LinearPolynomial(emptyList(), Flt64.one), Comparison.EQ)
    }

    fun toMathQuadraticInequality(): QuadraticInequalityOf<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
        val mono = QuadraticMonomial(Flt64.one, this, this)
        val poly = QuadraticPolynomial(monomials = listOf(mono), constant = Flt64.zero)
        return QuadraticInequalityOf<fuookami.ospf.kotlin.math.algebra.number.Flt64>(poly, QuadraticPolynomial(emptyList(), Flt64.one), Comparison.EQ)
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
typealias IntVariable = AbstractVariableItem<Int64, Integer>
typealias UIntVariable = AbstractVariableItem<UInt64, UInteger>

typealias QuantityVariableItem<Type> = Quantity<AbstractVariableItem<*, Type>>
typealias QuantityBinVariable = Quantity<BinVariable>
typealias QuantityTernaryVariable = Quantity<TernaryVariable>
typealias QuantityBalancedTernaryVariable = Quantity<BalancedTernaryVariable>
typealias QuantityIntVariable = Quantity<IntVariable>
typealias QuantityUIntVariable = Quantity<UIntVariable>

operator fun AbstractVariableItem<*, *>.times(rhs: PhysicalUnit): Quantity<AbstractVariableItem<*, *>> {
    return Quantity(this, rhs)
}

operator fun AbstractVariableItem<*, *>.div(rhs: PhysicalUnit): Quantity<AbstractVariableItem<*, *>> {
    return Quantity(this, rhs.reciprocal())
}
