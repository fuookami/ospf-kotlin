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

/**
 * 线性约束关系密封接口，封装线性展平数据和比较符号。
 * Sealed interface for linear constraint relations, encapsulating linear flatten data and comparison sign.
 */
sealed interface LinearRelation<V> where V : RealNumber<V>, V : NumberField<V> {
    val flattenData: LinearFlattenData<V>
    val sign: Comparison
    val name: String
    val displayName: String?

    val constraintRelation: ConstraintRelation get() = ConstraintRelation(sign)

    fun normalize(): LinearRelation<V>
}

/**
 * 二次约束关系密封接口，封装二次展平数据和比较符号。
 * Sealed interface for quadratic constraint relations, encapsulating quadratic flatten data and comparison sign.
 */
sealed interface QuadraticRelation<V> where V : RealNumber<V>, V : NumberField<V> {
    val flattenData: QuadraticFlattenData<V>
    val sign: Comparison
    val name: String
    val displayName: String?

    val constraintRelation: ConstraintRelation get() = ConstraintRelation(sign)

    fun normalize(): QuadraticRelation<V>
}

/**
 * 线性约束关系实现。
 * Linear constraint relation implementation.
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
 * 二次约束关系实现。
 * Quadratic constraint relation implementation.
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
