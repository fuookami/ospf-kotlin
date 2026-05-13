package fuookami.ospf.kotlin.core.intermediate_symbol

import fuookami.ospf.kotlin.core.model.basic.ExpressionRange
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.token.AbstractTokenList
import fuookami.ospf.kotlin.core.token.AbstractTokenTable
import fuookami.ospf.kotlin.core.token.AddableTokenCollection
import fuookami.ospf.kotlin.core.token.LinearFlattenData
import fuookami.ospf.kotlin.core.token.QuadraticFlattenData
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.concept.RealNumberConstants
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.inequality.QuadraticInequalityOf
import fuookami.ospf.kotlin.math.symbol.polynomial.MutableLinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.MutableQuadraticPolynomial
import fuookami.ospf.kotlin.core.intermediate_symbol.function.MathFunctionSymbolBase
import fuookami.ospf.kotlin.core.intermediate_symbol.function.QuadraticMathFunctionSymbolBase
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMechanismModel
import fuookami.ospf.kotlin.core.model.mechanism.AbstractQuadraticMechanismModel
import fuookami.ospf.kotlin.core.model.mechanism.Constraint
import fuookami.ospf.kotlin.core.model.mechanism.Linear
import fuookami.ospf.kotlin.core.model.mechanism.LinearConstraintImpl
import fuookami.ospf.kotlin.core.model.mechanism.LinearMechanismModel
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.model.mechanism.MechanismModel
import fuookami.ospf.kotlin.core.model.mechanism.Quadratic
import fuookami.ospf.kotlin.core.model.mechanism.QuadraticConstraintImpl
import fuookami.ospf.kotlin.core.model.mechanism.QuadraticMechanismModel
import fuookami.ospf.kotlin.core.model.mechanism.QuadraticMetaModel
import fuookami.ospf.kotlin.utils.functional.Try

/**
 * Centralized solver-boundary type casts.
 *
 * At runtime, all V-typed instances use V=Flt64. These casts bridge
 * star-projected (V=*) references back to Flt64 so that V-typed methods
 * can be called. The casts are safe but unchecked due to JVM type erasure.
 *
 * This object is the single location for all UNCHECKED_CAST suppressions
 * in the framework. Do not add @Suppress("UNCHECKED_CAST") elsewhere.
 */
@Suppress("UNCHECKED_CAST")
internal object SolverBoundaryCasts {
    private val flt64Converter = object : IntoValue<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
        override fun intoValue(value: Flt64) = value
        override val zero get() = Flt64.zero
        override val one get() = Flt64.one
        override fun fromValue(value: Flt64) = value
    }

    fun registerAuxiliaryTokensStar(
        symbol: Any,
        tokens: Any
    ): Try {
        return (symbol as IntermediateSymbol<fuookami.ospf.kotlin.math.algebra.number.Flt64>).registerAuxiliaryTokens(tokens as AddableTokenCollection<fuookami.ospf.kotlin.math.algebra.number.Flt64>)
    }

    fun prepareStar(
        symbol: Any,
        fixedValues: Map<Symbol, Flt64>?,
        tokenTable: AbstractTokenTable<fuookami.ospf.kotlin.math.algebra.number.Flt64>
    ): Flt64? {
        val sym = symbol as IntermediateSymbol<fuookami.ospf.kotlin.math.algebra.number.Flt64>
        return if (fixedValues.isNullOrEmpty()) {
            sym.prepare(null, tokenTable, flt64Converter)
        } else {
            sym.prepare(fixedValues, tokenTable, flt64Converter)
        }
    }

    fun registerConstraintsLinearStar(
        symbol: Any,
        model: Any
    ): Try {
        return (symbol as MathFunctionSymbolBase<fuookami.ospf.kotlin.math.algebra.number.Flt64>).registerConstraints(model as AbstractLinearMechanismModel<fuookami.ospf.kotlin.math.algebra.number.Flt64>)
    }

    fun registerConstraintsQuadraticStar(
        symbol: Any,
        model: Any
    ): Try {
        return (symbol as QuadraticMathFunctionSymbolBase<fuookami.ospf.kotlin.math.algebra.number.Flt64>).registerConstraints(model as AbstractQuadraticMechanismModel<fuookami.ospf.kotlin.math.algebra.number.Flt64>)
    }

    fun <V> castLinearMechanismModelStar(model: MechanismModel<V>): LinearMechanismModel<fuookami.ospf.kotlin.math.algebra.number.Flt64>
        where V : RealNumber<V>, V : NumberField<V> {
        return model as LinearMechanismModel<fuookami.ospf.kotlin.math.algebra.number.Flt64>
    }

    fun <V> castQuadraticMechanismModelStar(model: MechanismModel<V>): QuadraticMechanismModel<fuookami.ospf.kotlin.math.algebra.number.Flt64>
        where V : RealNumber<V>, V : NumberField<V> {
        return model as QuadraticMechanismModel<fuookami.ospf.kotlin.math.algebra.number.Flt64>
    }

    fun <V> castLinearMetaModelStar(model: LinearMetaModel<V>): LinearMetaModel<fuookami.ospf.kotlin.math.algebra.number.Flt64>
        where V : RealNumber<V>, V : NumberField<V> {
        return model as LinearMetaModel<fuookami.ospf.kotlin.math.algebra.number.Flt64>
    }

    fun <V> castQuadraticMetaModelStar(model: QuadraticMetaModel<V>): QuadraticMetaModel<fuookami.ospf.kotlin.math.algebra.number.Flt64>
        where V : RealNumber<V>, V : NumberField<V> {
        return model as QuadraticMetaModel<fuookami.ospf.kotlin.math.algebra.number.Flt64>
    }

    fun <V> linearConstraintAsFlt64(constraint: LinearConstraintImpl<V>): Constraint<fuookami.ospf.kotlin.math.algebra.number.Flt64, Linear>
        where V : RealNumber<V>, V : NumberField<V> {
        return constraint as Constraint<fuookami.ospf.kotlin.math.algebra.number.Flt64, Linear>
    }

    fun <V> linearConstraintsAsFlt64(constraints: List<LinearConstraintImpl<V>>): List<Constraint<fuookami.ospf.kotlin.math.algebra.number.Flt64, Linear>>
        where V : RealNumber<V>, V : NumberField<V> {
        return constraints as List<Constraint<fuookami.ospf.kotlin.math.algebra.number.Flt64, Linear>>
    }

    fun <V> quadraticConstraintAsFlt64(constraint: QuadraticConstraintImpl<V>): Constraint<fuookami.ospf.kotlin.math.algebra.number.Flt64, Quadratic>
        where V : RealNumber<V>, V : NumberField<V> {
        return constraint as Constraint<fuookami.ospf.kotlin.math.algebra.number.Flt64, Quadratic>
    }

    fun <V> quadraticConstraintsAsFlt64(constraints: List<QuadraticConstraintImpl<V>>): List<Constraint<fuookami.ospf.kotlin.math.algebra.number.Flt64, Quadratic>>
        where V : RealNumber<V>, V : NumberField<V> {
        return constraints as List<Constraint<fuookami.ospf.kotlin.math.algebra.number.Flt64, Quadratic>>
    }

    fun <V> linearInequalityAsV(cut: Any): LinearInequality<V>?
        where V : RealNumber<V>, V : NumberField<V> {
        return cut as? LinearInequality<V>
    }

    fun <V> quadraticInequalityAsV(cut: Any): QuadraticInequalityOf<V>?
        where V : RealNumber<V>, V : NumberField<V> {
        return cut as? QuadraticInequalityOf<V>
    }

    fun <V> tokenListAsFlt64(tokenTable: AbstractTokenTable<V>): AbstractTokenList<fuookami.ospf.kotlin.math.algebra.number.Flt64>
        where V : RealNumber<V>, V : NumberField<V> {
        return tokenTable.tokenList as AbstractTokenList<fuookami.ospf.kotlin.math.algebra.number.Flt64>
    }

    fun <V> tokenListAsFlt64OrNull(tokenTable: AbstractTokenTable<V>?): AbstractTokenList<fuookami.ospf.kotlin.math.algebra.number.Flt64>?
        where V : RealNumber<V>, V : NumberField<V> {
        return tokenTable?.let { tokenListAsFlt64(it) }
    }

    fun <V> mapValuesToV(values: Map<Symbol, Flt64>, converter: IntoValue<V>): Map<Symbol, V>
        where V : RealNumber<V>, V : NumberField<V> {
        return values.mapValues { converter.intoValue(it.value) }
    }

    fun <V> dependencyAsIntermediateV(dependency: IntermediateSymbol<*>): IntermediateSymbol<V>
        where V : RealNumber<V>, V : NumberField<V> {
        return dependency as IntermediateSymbol<V>
    }

    fun <V> linearPolynomialAsFlt64(polynomial: MutableLinearPolynomial<V>): MutableLinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>
        where V : RealNumber<V>, V : NumberField<V> {
        return polynomial as MutableLinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>
    }

    fun <V> quadraticPolynomialAsFlt64(polynomial: MutableQuadraticPolynomial<V>): MutableQuadraticPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>
        where V : RealNumber<V>, V : NumberField<V> {
        return polynomial as MutableQuadraticPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>
    }

    fun tokenTableAsFlt64OrNull(tokenTable: Any?): AbstractTokenTable<fuookami.ospf.kotlin.math.algebra.number.Flt64>? {
        return tokenTable as? AbstractTokenTable<fuookami.ospf.kotlin.math.algebra.number.Flt64>
    }

    fun <V> symbolAsIntermediateStar(symbol: Symbol?): IntermediateSymbol<out V>?
        where V : RealNumber<V>, V : NumberField<V> {
        return symbol as? IntermediateSymbol<out V>
    }

    fun linearSolverFlattenedMonomials(symbol: Any): LinearFlattenData<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
        val polynomial = (symbol as LinearIntermediateSymbol<fuookami.ospf.kotlin.math.algebra.number.Flt64>).toLinearPolynomial()
        return LinearFlattenData(polynomial.monomials, polynomial.constant)
    }

    fun quadraticSolverFlattenedMonomials(symbol: Any): QuadraticFlattenData<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
        val polynomial = (symbol as QuadraticIntermediateSymbol<fuookami.ospf.kotlin.math.algebra.number.Flt64>).toQuadraticPolynomial()
        return QuadraticFlattenData(polynomial.monomials, polynomial.constant)
    }

    @Suppress("UNCHECKED_CAST")
    fun <V> expressionRangeVFromFlt64(rangeFlt64: ValueRange<fuookami.ospf.kotlin.math.algebra.number.Flt64>?): ExpressionRange<V>
        where V : RealNumber<V>, V : NumberField<V> {
        return ExpressionRange(rangeFlt64 as ValueRange<V>?, Flt64 as RealNumberConstants<V>)
    }

    @Suppress("UNCHECKED_CAST")
    fun <V> fullExpressionRangeV(): ExpressionRange<V>
        where V : RealNumber<V>, V : NumberField<V> {
        return ExpressionRange(Flt64 as RealNumberConstants<V>) as ExpressionRange<V>
    }

    fun rangeAsFlt64(symbol: Any): ExpressionRange<fuookami.ospf.kotlin.math.algebra.number.Flt64>? {
        val typed = symbol as IntermediateSymbol<fuookami.ospf.kotlin.math.algebra.number.Flt64>
        return typed.range as ExpressionRange<fuookami.ospf.kotlin.math.algebra.number.Flt64>?
    }
}

fun <V> castLinearMetaModelForSolver(model: LinearMetaModel<V>): LinearMetaModel<fuookami.ospf.kotlin.math.algebra.number.Flt64>
        where V : RealNumber<V>, V : NumberField<V> {
    return SolverBoundaryCasts.castLinearMetaModelStar(model)
}

fun <V> castQuadraticMetaModelForSolver(model: QuadraticMetaModel<V>): QuadraticMetaModel<fuookami.ospf.kotlin.math.algebra.number.Flt64>
        where V : RealNumber<V>, V : NumberField<V> {
    return SolverBoundaryCasts.castQuadraticMetaModelStar(model)
}
