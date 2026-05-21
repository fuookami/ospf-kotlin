@file:Suppress("unused")

package fuookami.ospf.kotlin.core.intermediate_symbol

import fuookami.ospf.kotlin.core.model.basic.ExpressionRange
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.IdentifierGenerator
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.utils.functional.Variant3
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.concept.RealNumberConstants
import fuookami.ospf.kotlin.math.algebra.concept.Ring
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.value_range.*
import fuookami.ospf.kotlin.math.symbol.Category
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.math.symbol.Quadratic
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.operation.ToLinearPolynomial
import fuookami.ospf.kotlin.math.symbol.operation.ToQuadraticPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.MutableLinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.MutableQuadraticPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.PhysicalUnit
import fuookami.ospf.kotlin.quantities.unit.reciprocal
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMechanismModel
import fuookami.ospf.kotlin.core.model.mechanism.AbstractQuadraticMechanismModel
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality


interface IntermediateSymbol<V> : Symbol where V : RealNumber<V>, V : NumberField<V> {
    override var name: String
    override var displayName: String?

    val discrete: Boolean get() = false

    val range: ExpressionRange<V>
    val lowerBound: Bound<V>? get() = range.lowerBound
    val upperBound: Bound<V>? get() = range.upperBound
    val fixedValue: V? get() = range.fixedValue

    // --- V-typed primary path (abstract) ---
    fun prepare(values: Map<Symbol, V>?, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>): V?

    fun prepareAndCache(values: Map<Symbol, V>?, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>) {
        if (values.isNullOrEmpty()) {
            prepare(null, tokenTable, converter)?.let {
                tokenTable.cache(cacheKey = this, solution = null, value = it)
            }
        } else {
            prepare(values, tokenTable, converter)?.let {
                tokenTable.cache(cacheKey = this, fixedValues = values, value = it)
            }
        }
    }

    fun evaluate(tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>, zeroIfNone: Boolean = false): V?
    fun evaluate(results: List<V>, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>, zeroIfNone: Boolean = false): V?
    fun evaluate(values: Map<Symbol, V>, tokenTable: AbstractTokenTable<V>?, converter: IntoValue<V>, zeroIfNone: Boolean = false): V?
    fun evaluateFromTokens(tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>, zeroIfNone: Boolean = false): V? =
        evaluate(tokenTable, converter, zeroIfNone)

    val category: Category
    val operationCategory: Category get() = category

    val cached: Boolean
    val parent: IntermediateSymbol<*>? get() = null
    val args: Any? get() = parent?.args
    val dependencies: Set<IntermediateSymbol<*>>

    val identifier: UInt64
    val index: Int

    fun flush(force: Boolean = false)

    fun registerAuxiliaryTokens(tokens: AddableTokenCollection<V>): Try = ok

    fun toRawString(unfold: UInt64 = UInt64.zero): String
}


interface LinearIntermediateSymbol<V> : IntermediateSymbol<V>, ToLinearPolynomial<V> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {
    companion object {
        fun <V> empty(
            constants: RealNumberConstants<V>,
            parent: IntermediateSymbol<*>? = null,
            name: String = "",
            displayName: String? = null
        ): LinearIntermediateSymbol<V> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {
            return LinearExpressionSymbol(
                _utilsPolynomial = MutableLinearPolynomial(
                    monomials = emptyList(),
                    constant = constants.zero
                ),
                parent = parent,
                name = name,
                displayName = displayName
            )
        }
    }

    val polynomial: LinearPolynomial<V>

    fun asMutable(): MutableLinearPolynomial<V>

    override fun toLinearPolynomial(): LinearPolynomial<V> = polynomial
}

interface QuadraticIntermediateSymbol<V> : IntermediateSymbol<V>, ToQuadraticPolynomial<V> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {
    companion object {
        fun <V> empty(
            constants: RealNumberConstants<V>,
            parent: IntermediateSymbol<*>? = null,
            name: String = "",
            displayName: String? = null
        ): QuadraticIntermediateSymbol<V> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {
            return QuadraticExpressionSymbol(
                _utilsPolynomial = MutableQuadraticPolynomial(
                    monomials = emptyList(),
                    constant = constants.zero
                ),
                category = Quadratic,
                parent = parent,
                name = name,
                displayName = displayName
            )
        }
    }

    val polynomial: QuadraticPolynomial<V>

    fun asMutable(): MutableQuadraticPolynomial<V>

    override fun toQuadraticPolynomial(): QuadraticPolynomial<V> = polynomial
}
