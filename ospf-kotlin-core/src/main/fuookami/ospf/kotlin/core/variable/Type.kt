/**
 * 变量类型体系，定义二值、三值、整数、连续等变量类型及其分类接口。
 * Variable type system defining binary, ternary, integer, continuous variable types and their classification interfaces.
*/
package fuookami.ospf.kotlin.core.variable

import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*

/**
 * 变量类型分类接口
 * Variable type kind interface
 *
 * 提供二值/无符号/整数/连续等类型判断属性。
 * Provides binary/unsigned/integer/continuous type classification properties.
 *
 * @property isBinaryType 是否为二值类型 / Whether binary type
 * @property isUnsignedType 是否为无符号类型 / Whether unsigned type
 * @property isIntegerType 是否为整数类型 / Whether integer type
 * @property isUnsignedIntegerType 是否为无符号整数类型 / Whether unsigned integer type
 * @property isContinuousType 是否为连续类型 / Whether continuous type
 * @property isUnsignedContinuousType 是否为无符号连续类型 / Whether unsigned continuous type
 * @property isNotBinaryIntegerType 是否为非二值整数类型 / Whether non-binary integer type
*/
sealed interface VariableTypeKind {

    /** 是否为二值类型 / Whether binary type */
    val isBinaryType get() = false

    /** 是否为无符号类型 / Whether unsigned type */
    val isUnsignedType get() = false

    /** 是否为整数类型 / Whether integer type */
    val isIntegerType get() = false

    /** 是否为无符号整数类型 / Whether unsigned integer type */
    val isUnsignedIntegerType get() = isIntegerType && isUnsignedType

    /** 是否为连续类型 / Whether continuous type */
    val isContinuousType get() = !isIntegerType

    /** 是否为无符号连续类型 / Whether unsigned continuous type */
    val isUnsignedContinuousType get() = isContinuousType && isUnsignedType

    /** 是否为非二值整数类型 / Whether non-binary integer type */
    val isNotBinaryIntegerType get() = !isBinaryType && isIntegerType
}

/**
 * 变量类型完整接口
 * Variable type interface
 *
 * 扩展 VariableTypeKind 并附加名称、常量和值域边界。
 * Extends VariableTypeKind with name, constants, and value range bounds.
 *
 * @param T 数值类型 / The number type
 * @property name 类型全名 / Full type name
 * @property shortName 类型缩写 / Short type name
 * @property constants 数值类型常量 / Numeric type constants
 * @property minimum 最小值 / Minimum value
 * @property maximum 最大值 / Maximum value
*/
sealed interface VariableTypeInterface<T> : VariableTypeKind where T : RealNumber<T>, T : NumberField<T> {

    /** 类型全名 / Full type name */
    val name: String

    /** 类型缩写 / Short type name */
    val shortName: String

    /** 数值类型常量 / Numeric type constants */
    val constants: RealNumberConstants<T>

    /** 最小值 / Minimum value */
    val minimum: T

    /** 最大值 / Maximum value */
    val maximum: T
}

/** 有符号整数变量类型接口。 / Signed integer variable type interface. */
sealed interface IntegerVariableType<T : IntegerNumber<T>> : VariableTypeInterface<T> {
    override val minimum get() = constants.minimum
    override val maximum get() = constants.maximum

    override val isIntegerType get() = true
}

/** 无符号整数变量类型接口。 / Unsigned integer variable type interface. */
sealed interface UIntegerVariableType<T : UIntegerNumber<T>> : VariableTypeInterface<T> {
    override val minimum get() = constants.zero
    override val maximum get() = constants.maximum

    override val isUnsignedType get() = true
    override val isIntegerType get() = true
}

/** 有符号连续变量类型接口。 / Signed continuous variable type interface. */
sealed interface ContinuesVariableType<T : FloatingNumber<T>> : VariableTypeInterface<T> {
    override val minimum get() = -constants.decimalPrecision.reciprocal()
    override val maximum get() = constants.decimalPrecision.reciprocal()
}

/** 无符号连续变量类型接口。 / Unsigned continuous variable type interface. */
sealed interface UContinuesVariableType<T : FloatingNumber<T>> : VariableTypeInterface<T> {
    override val minimum get() = constants.zero
    override val maximum get() = constants.decimalPrecision.reciprocal()

    override val isUnsignedType get() = true
}

/**
 * 变量类型的密封基类。
 * Sealed base class for variable types.
 *
 * @property constants 数值类型常量 / Numeric type constants
*/
sealed class VariableType<T>(
    override val constants: RealNumberConstants<T>
) : VariableTypeInterface<T> where T : RealNumber<T>, T : NumberField<T>

/** 二值变量类型（0/1）。 / Binary variable type (0/1). */
data object Binary : VariableType<UInt8>(UInt8), UIntegerVariableType<UInt8> {
    override val name = "Binary"
    override val shortName = "bin"
    override val maximum by constants::one

    override val isBinaryType get() = true
    override val isUnsignedType get() = true

    /** @return "Binary" */
    override fun toString(): String = "Binary"
}

/** 三值变量类型（0/1/2）。 / Ternary variable type (0/1/2). */
data object Ternary : VariableType<UInt8>(UInt8), UIntegerVariableType<UInt8> {
    override val name = "Ternary"
    override val shortName = "ter"
    override val maximum by constants::two

    /** @return "Ternary" */
    override fun toString(): String = "Ternary"
}

/** 平衡三值变量类型（-1/0/1）。 / Balanced ternary variable type (-1/0/1). */
data object BalancedTernary : VariableType<Int8>(Int8), IntegerVariableType<Int8> {
    override val name = "BalancedTernary"
    override val shortName = "bter"
    override val minimum get() = -constants.one
    override val maximum by constants::one

    /** @return "BalancedTernary" */
    override fun toString(): String = "BalancedTernary"
}

/** 百分比变量类型（[0, 1]）。 / Percentage variable type ([0, 1]). */
data object Percentage : VariableType<Flt64>(Flt64), UContinuesVariableType<Flt64> {
    override val name = "Percentage"
    override val shortName = "pct"
    override val maximum by constants::one

    /** @return "Percentage" */
    override fun toString(): String = "Percentage"
}

/** 有符号整数变量类型。 / Signed integer variable type. */
data object Integer : VariableType<Int64>(Int64), IntegerVariableType<Int64> {
    override val name = "Integer"
    override val shortName = "int"

    /** @return "Integer" */
    override fun toString(): String = "Integer"
}

/** 无符号整数变量类型。 / Unsigned integer variable type. */
data object UInteger : VariableType<UInt64>(UInt64), UIntegerVariableType<UInt64> {
    override val name = "UInteger"
    override val shortName = "uint"

    /** @return "UInteger" */
    override fun toString(): String = "UInteger"
}

/** 有符号连续变量类型。 / Signed continuous variable type. */
data object Continuous : VariableType<Flt64>(Flt64), ContinuesVariableType<Flt64> {
    override val name = "Continuous"
    override val shortName = "real"

    /** @return "Continues" */
    override fun toString(): String = "Continues"
}

/** 无符号连续变量类型。 / Unsigned continuous variable type. */
data object UContinuous : VariableType<Flt64>(Flt64), UContinuesVariableType<Flt64> {
    override val name = "UContinuous"
    override val shortName = "ureal"

    /** @return "UContinues" */
    override fun toString(): String = "UContinues"
}
