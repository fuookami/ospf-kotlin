/**
 * 抽象变量项及其键、类型别名和物理单位扩展。
 * Abstract variable item, its key, type aliases, and physical unit extensions.
 */
package fuookami.ospf.kotlin.core.variable

import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.value_range.Bound
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.utils.functional.*

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
    /**
     * @param rhs 右侧比较对象 / Right-hand comparison object
     * @return 偏序关系 / Partial order result
     */
    override fun partialOrd(rhs: VariableItemKey): Order {
        return if (this.identifier < rhs.identifier) {
            Order.Less()
        } else if (this.identifier > rhs.identifier) {
            Order.Greater()
        } else {
            index ord rhs.index
        }
    }

    /** @return 基于 identifier 和 index 的哈希值 / Hash code based on identifier and index */
    override fun hashCode(): Int {
        return identifier.toInt() * 31 + index
    }

    /**
     * @param other 待比较对象 / Object to compare
     * @return 是否相同 / Whether equal
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VariableItemKey) return false

        if (identifier != other.identifier) return false
        if (index != other.index) return false

        return true
    }
}

/**
 * 抽象变量项基类
 * Abstract base class for variable items
 *
 * 持有变量类型、名称、常量和范围等元数据，是所有变量类型的公共基类。
 * Holds metadata such as type, name, constants, and range; common base for all variable types.
 *
 * @param T 数值类型 / The number type
 * @param Type 变量类型 / The variable type
 * @property type 变量类型 / Variable type
 * @property name 变量名称 / Variable name
 * @property constants 数值类型常量 / Numeric type constants
 * @property dimension 变量维度 / Variable dimension
 * @property identifier 变量标识符 / Variable identifier
 * @property index 变量索引 / Variable index
 * @property vectorView 向量视图 / Vector view
 * @property displayName 显示名称 / Display name
 * @property uindex 无符号索引 / Unsigned index
 * @property uvector 无符号向量 / Unsigned vector
 * @property range 变量范围 / Variable range
 * @property lowerBound Flt64 下界 / Flt64 lower bound
 * @property upperBound Flt64 上界 / Flt64 upper bound
 * @property key 变量唯一键 / Variable unique key
 */
abstract class AbstractVariableItem<T, Type : VariableType<T>>(
    val type: Type,
    override var name: String,
    val constants: RealNumberConstants<T>
) : Symbol
        where T : RealNumber<T>, T : NumberField<T> {
    /** 变量维度 / Variable dimension */
    abstract val dimension: Int
    /** 变量标识符 / Variable identifier */
    abstract val identifier: UInt64
    /** 变量索引 / Variable index */
    abstract val index: Int
    /** 向量视图 / Vector view */
    abstract val vectorView: IntArray
    /** 显示名称 / Display name */
    override val displayName get() = name

    /** 无符号索引 / Unsigned index */
    val uindex get() = UInt64(index)
    /** 无符号向量 / Unsigned vector */
    val uvector by lazy { vectorView.map { UInt64(it) } }

    /** 变量范围 / Variable range */
    val range = Range(type, constants)

    /** Flt64 下界 / Flt64 lower bound */
    val lowerBound: Bound<Flt64>? get() = range.lowerBound?.toFlt64()
    /** Flt64 上界 / Flt64 upper bound */
    val upperBound: Bound<Flt64>? get() = range.upperBound?.toFlt64()

    /** 变量唯一键 / Variable unique key */
    val key get() = VariableItemKey(identifier, index)

    /**
     * 判断是否属于同一变量组
     * Check if this variable belongs to the same group as another
     *
     * @param item 另一个变量项 / Another variable item
     * @return 是否属于同一组 / Whether in the same group
     */
    open infix fun belongsTo(item: AbstractVariableItem<*, *>): Boolean {
        return identifier == item.identifier
    }

    /**
     * 判断是否属于指定组合
     * Check if this variable belongs to the specified combination
     *
     * @param combination 变量组合 / Variable combination
     * @return 是否属于该组合 / Whether belongs to the combination
     */
    open infix fun belongsTo(combination: VariableCombination<*, *, *>): Boolean {
        return identifier == combination.identifier
    }

    /**
     * 转换为线性不等式（变量 = 1）
     * Convert to linear inequality (variable = 1)
     *
     * @return 线性不等式 / Linear inequality
     */
    fun toMathLinearInequality(): LinearInequality<Flt64> {
        val poly = LinearPolynomial(monomials = listOf(LinearMonomial(Flt64.one, this)), constant = Flt64.zero)
        return LinearInequality<Flt64>(poly, LinearPolynomial(emptyList(), Flt64.one), Comparison.EQ)
    }

    /**
     * 转换为二次不等式（变量² = 1）
     * Convert to quadratic inequality (variable² = 1)
     *
     * @return 二次不等式 / Quadratic inequality
     */
    fun toMathQuadraticInequality(): QuadraticInequalityOf<Flt64> {
        val mono = QuadraticMonomial(Flt64.one, this, this)
        val poly = QuadraticPolynomial(monomials = listOf(mono), constant = Flt64.zero)
        return QuadraticInequalityOf(poly, QuadraticPolynomial(emptyList(), Flt64.one), Comparison.EQ)
    }

    /** @return 基于 key 的哈希值 / Hash code based on key */
    override fun hashCode() = key.hashCode()

    /**
     * @param other 待比较对象 / Object to compare
     * @return 是否相同 / Whether equal
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AbstractVariableItem<*, *>) return false

        if (identifier != other.identifier) return false
        if (index != other.index) return false

        return true
    }

    /** @return 变量名称 / Variable name */
    override fun toString() = name
}

/**
 * 变量标识符生成器（线程不安全）
 * Variable identifier generator (not thread-safe)
 *
 * 通过递增计数器生成唯一的变量标识符。
 * Generates unique variable identifiers via an incrementing counter.
 *
 * @property next 下一个可用标识符 / Next available identifier
 */
internal data object IdentifierGenerator {
    /** 下一个可用标识符 / Next available identifier */
    var next: UInt64 = UInt64.zero

    /**
     * 重置生成器
     * Reset the generator
     */
    fun flush() {
        next = UInt64.zero
    }

    /**
     * 生成下一个标识符
     * Generate the next identifier
     *
     * @return 新的唯一标识符 / New unique identifier
     */
    fun gen(): UInt64 {
        val thisValue = next;
        ++next;
        return thisValue;
    }
}

/** 变量项类型别名（按变量类型参数化）/ Variable item type alias (parameterized by variable type) */
typealias VariableItem<Type> = AbstractVariableItem<*, Type>
/** 二值变量 / Binary variable */
typealias BinVariable = AbstractVariableItem<UInt8, Binary>
/** 三值变量 / Ternary variable */
typealias TernaryVariable = AbstractVariableItem<UInt8, Ternary>
/** 平衡三值变量 / Balanced ternary variable */
typealias BalancedTernaryVariable = AbstractVariableItem<Int8, BalancedTernary>
/** 整数变量 / Integer variable */
typealias IntVariable = AbstractVariableItem<Int64, Integer>
/** 无符号整数变量 / Unsigned integer variable */
typealias UIntVariable = AbstractVariableItem<UInt64, UInteger>

/** 物理量变量项类型别名 / Quantity variable item type alias */
typealias QuantityVariableItem<Type> = Quantity<AbstractVariableItem<*, Type>>
/** 物理量二值变量 / Quantity binary variable */
typealias QuantityBinVariable = Quantity<BinVariable>
/** 物理量三值变量 / Quantity ternary variable */
typealias QuantityTernaryVariable = Quantity<TernaryVariable>
/** 物理量平衡三值变量 / Quantity balanced ternary variable */
typealias QuantityBalancedTernaryVariable = Quantity<BalancedTernaryVariable>
/** 物理量整数变量 / Quantity integer variable */
typealias QuantityIntVariable = Quantity<IntVariable>
/** 物理量无符号整数变量 / Quantity unsigned integer variable */
typealias QuantityUIntVariable = Quantity<UIntVariable>

/**
 * 变量项与物理单位相乘
 * Multiply variable item by physical unit
 *
 * @param rhs 物理单位 / Physical unit
 * @return 物理量变量项 / Quantity variable item
 */
operator fun AbstractVariableItem<*, *>.times(rhs: PhysicalUnit): Quantity<AbstractVariableItem<*, *>> {
    return Quantity(this, rhs)
}

/**
 * 变量项除以物理单位
 * Divide variable item by physical unit
 *
 * @param rhs 物理单位 / Physical unit
 * @return 物理量变量项（单位取倒数）/ Quantity variable item (reciprocal unit)
 */
operator fun AbstractVariableItem<*, *>.div(rhs: PhysicalUnit): Quantity<AbstractVariableItem<*, *>> {
    return Quantity(this, rhs.reciprocal())
}
