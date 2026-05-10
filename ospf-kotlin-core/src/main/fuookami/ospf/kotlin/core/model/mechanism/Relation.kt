package fuookami.ospf.kotlin.core.model.mechanism

import fuookami.ospf.kotlin.core.model.basic.ConstraintRelation
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.concept.Ring
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.core.token.LinearFlattenData
import fuookami.ospf.kotlin.core.token.QuadraticFlattenData

sealed interface LinearRelation<V> where V : RealNumber<V>, V : NumberField<V> {
    val flattenData: LinearFlattenData<V>
    val sign: Comparison
    val name: String
    val displayName: String?

    val constraintRelation: ConstraintRelation get() = ConstraintRelation(sign)

    fun normalize(): LinearRelation<V>
}

sealed interface QuadraticRelation<V> where V : RealNumber<V>, V : NumberField<V> {
    val flattenData: QuadraticFlattenData<V>
    val sign: Comparison
    val name: String
    val displayName: String?

    val constraintRelation: ConstraintRelation get() = ConstraintRelation(sign)

    fun normalize(): QuadraticRelation<V>
}

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
