/**
 * 约束关系
 * Constraint relation
 */
package fuookami.ospf.kotlin.core.model.mechanism

import fuookami.ospf.kotlin.core.model.basic.ConstraintRelation
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.utils.functional.*

/**
 * 线性约束关系密封接口，封装线性展平数据和比较符号。
 * Sealed interface for linear constraint relations, encapsulating linear flatten data and comparison sign.
 */
sealed interface LinearRelation<V> where V : RealNumber<V>, V : NumberField<V> {
    /** 扁平化的线性数据 / Flattened linear data */
    val flattenData: LinearFlattenData<V>
    /** 比较符号 / Comparison sign */
    val sign: Comparison
    /** 约束名称 / Constraint name */
    val name: String
    /** 显示名称 / Display name */
    val displayName: String?

    /** 转换为可空 ConstraintRelation 枚举 / Convert to nullable ConstraintRelation enum */
    val constraintRelationOrNull: ConstraintRelation? get() = ConstraintRelation.ofOrNull(sign)

    /** 转换为 ConstraintRelation 枚举，失败时返回 Ret 失败 / Convert to ConstraintRelation enum, returning Ret failure when invalid
     * @return 包含 ConstraintRelation 的 Ret，或失败信息 / Ret containing ConstraintRelation, or failure
     */
    fun constraintRelation(): Ret<ConstraintRelation> = ConstraintRelation.ofSafe(sign)

    /** 归一化（将 GT/GE 转换为 LT/LE）/ Normalize (convert GT/GE to LT/LE)
     * @return 归一化后的线性约束关系 / The normalized linear constraint relation
     */
    fun normalize(): LinearRelation<V>
}

/**
 * 二次约束关系密封接口，封装二次展平数据和比较符号。
 * Sealed interface for quadratic constraint relations, encapsulating quadratic flatten data and comparison sign.
 */
sealed interface QuadraticRelation<V> where V : RealNumber<V>, V : NumberField<V> {
    /** 扁平化的二次数据 / Flattened quadratic data */
    val flattenData: QuadraticFlattenData<V>
    /** 比较符号 / Comparison sign */
    val sign: Comparison
    /** 约束名称 / Constraint name */
    val name: String
    /** 显示名称 / Display name */
    val displayName: String?

    /** 转换为可空 ConstraintRelation 枚举 / Convert to nullable ConstraintRelation enum */
    val constraintRelationOrNull: ConstraintRelation? get() = ConstraintRelation.ofOrNull(sign)

    /** 转换为 ConstraintRelation 枚举，失败时返回 Ret 失败 / Convert to ConstraintRelation enum, returning Ret failure when invalid
     * @return 包含 ConstraintRelation 的 Ret，或失败信息 / Ret containing ConstraintRelation, or failure
     */
    fun constraintRelation(): Ret<ConstraintRelation> = ConstraintRelation.ofSafe(sign)

    /** 归一化（将 GT/GE 转换为 LT/LE）/ Normalize (convert GT/GE to LT/LE)
     * @return 归一化后的二次约束关系 / The normalized quadratic constraint relation
     */
    fun normalize(): QuadraticRelation<V>
}

/**
 * 线性约束关系实现
 * Linear constraint relation implementation
 *
 * @param V 数值类型 / The number type
 * @property flattenData 扁平化的线性数据 / Flattened linear data
 * @property sign 比较符号 / Comparison sign
 * @property name 约束名称 / Constraint name
 * @property displayName 显示名称 / Display name
 */
data class LinearRelationImpl<V>(
    override val flattenData: LinearFlattenData<V>,
    override val sign: Comparison,
    override val name: String = "",
    override val displayName: String? = null
) : LinearRelation<V> where V : RealNumber<V>, V : NumberField<V> {

    override fun normalize(): LinearRelation<V> {
        return when (sign) {
            Comparison.GT -> LinearRelationImpl(
                flattenData = LinearFlattenData<V>(
                    monomials = flattenData.monomials.map { LinearMonomial(-it.coefficient, it.symbol) },
                    constant = -flattenData.constant
                ),
                sign = Comparison.LT,
                name = name,
                displayName = displayName
            )
            Comparison.GE -> LinearRelationImpl(
                flattenData = LinearFlattenData<V>(
                    monomials = flattenData.monomials.map { LinearMonomial(-it.coefficient, it.symbol) },
                    constant = -flattenData.constant
                ),
                sign = Comparison.LE,
                name = name,
                displayName = displayName
            )
            else -> this
        }
    }
}

/**
 * 二次约束关系实现
 * Quadratic constraint relation implementation
 *
 * @param V 数值类型 / The number type
 * @property flattenData 扁平化的二次数据 / Flattened quadratic data
 * @property sign 比较符号 / Comparison sign
 * @property name 约束名称 / Constraint name
 * @property displayName 显示名称 / Display name
 */
data class QuadraticRelationImpl<V>(
    override val flattenData: QuadraticFlattenData<V>,
    override val sign: Comparison,
    override val name: String = "",
    override val displayName: String? = null
) : QuadraticRelation<V> where V : RealNumber<V>, V : NumberField<V> {

    override fun normalize(): QuadraticRelation<V> {
        return when (sign) {
            Comparison.GT -> QuadraticRelationImpl(
                flattenData = QuadraticFlattenData<V>(
                    monomials = flattenData.monomials.map { QuadraticMonomial(-it.coefficient, it.symbol1, it.symbol2) },
                    constant = -flattenData.constant
                ),
                sign = Comparison.LT,
                name = name,
                displayName = displayName
            )
            Comparison.GE -> QuadraticRelationImpl(
                flattenData = QuadraticFlattenData<V>(
                    monomials = flattenData.monomials.map { QuadraticMonomial(-it.coefficient, it.symbol1, it.symbol2) },
                    constant = -flattenData.constant
                ),
                sign = Comparison.LE,
                name = name,
                displayName = displayName
            )
            else -> this
        }
    }
}
