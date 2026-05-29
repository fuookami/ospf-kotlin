/**
 * 求解器边界类型转换 / Solver boundary type casts
 *
 * 提供运行时求解器边界的 Flt64 类型转换工具，将星投影引用转换回 Flt64 以调用泛型方法。
 *
 * Provides runtime solver-boundary Flt64 type cast utilities, converting star-projected
 * references back to Flt64 for calling generic methods.
 */
package fuookami.ospf.kotlin.core.symbol

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.core.symbol.function.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.value.*

/**
 * Centralized solver-boundary type casts.
 *
 * 运行时所有求解器边界实例都使用 V=Flt64；这里把星投影引用转换回 Flt64，以便调用泛型方法。
 * At runtime, all solver-boundary instances use V=Flt64. These casts convert
 * star-projected (V=*) references back to Flt64 so that generic methods can be called.
 * The casts are safe but unchecked due to JVM type erasure.
 *
 * This object is the single location for all UNCHECKED_CAST suppressions
 * in the framework. Do not add @Suppress("UNCHECKED_CAST") elsewhere.
 */
@Suppress("UNCHECKED_CAST")
internal object SolverBoundaryCasts {
    private val solverValueConverter = object : IntoValue<Flt64> {
        override fun intoValue(value: Flt64) = value
        override val zero get() = Flt64.zero
        override val one get() = Flt64.one
        override fun fromValue(value: Flt64) = value
    }

    fun registerAuxiliaryTokensStar(
        symbol: Any,
        tokens: Any
    ): Try {
        return (symbol as IntermediateSymbol<Flt64>).registerAuxiliaryTokens(tokens as AddableTokenCollection<Flt64>)
    }

    fun prepareStar(
        symbol: Any,
        fixedValues: Map<Symbol, Flt64>?,
        tokenTable: AbstractTokenTable<Flt64>
    ): Flt64? {
        val sym = symbol as IntermediateSymbol<Flt64>
        return if (fixedValues.isNullOrEmpty()) {
            sym.prepare(null, tokenTable, solverValueConverter)
        } else {
            sym.prepare(fixedValues, tokenTable, solverValueConverter)
        }
    }

    fun registerConstraintsLinearStar(
        symbol: Any,
        model: Any
    ): Try {
        return (symbol as MathFunctionSymbolBase<Flt64>).registerConstraints(model as AbstractLinearMechanismModel<Flt64>)
    }

    fun registerConstraintsQuadraticStar(
        symbol: Any,
        model: Any
    ): Try {
        return (symbol as QuadraticMathFunctionSymbolBase<Flt64>).registerConstraints(model as AbstractQuadraticMechanismModel<Flt64>)
    }

    fun <V> castLinearMechanismModelStar(model: MechanismModel<V>): LinearMechanismModel<Flt64>
        where V : RealNumber<V>, V : NumberField<V> {
        return model as LinearMechanismModel<Flt64>
    }

    fun <V> castQuadraticMechanismModelStar(model: MechanismModel<V>): QuadraticMechanismModel<Flt64>
        where V : RealNumber<V>, V : NumberField<V> {
        return model as QuadraticMechanismModel<Flt64>
    }

    fun <V> castLinearMetaModelStar(model: LinearMetaModel<V>): LinearMetaModel<Flt64>
        where V : RealNumber<V>, V : NumberField<V> {
        return model as LinearMetaModel<Flt64>
    }

    fun <V> castQuadraticMetaModelStar(model: QuadraticMetaModel<V>): QuadraticMetaModel<Flt64>
        where V : RealNumber<V>, V : NumberField<V> {
        return model as QuadraticMetaModel<Flt64>
    }

    fun <V> linearConstraintAsFlt64(constraint: LinearConstraintImpl<V>): Constraint<Flt64, Linear>
        where V : RealNumber<V>, V : NumberField<V> {
        return constraint as Constraint<Flt64, Linear>
    }

    fun <V> linearConstraintsAsFlt64(constraints: List<LinearConstraintImpl<V>>): List<Constraint<Flt64, Linear>>
        where V : RealNumber<V>, V : NumberField<V> {
        return constraints as List<Constraint<Flt64, Linear>>
    }

    fun <V> quadraticConstraintAsFlt64(constraint: QuadraticConstraintImpl<V>): Constraint<Flt64, Quadratic>
        where V : RealNumber<V>, V : NumberField<V> {
        return constraint as Constraint<Flt64, Quadratic>
    }

    fun <V> quadraticConstraintsAsFlt64(constraints: List<QuadraticConstraintImpl<V>>): List<Constraint<Flt64, Quadratic>>
        where V : RealNumber<V>, V : NumberField<V> {
        return constraints as List<Constraint<Flt64, Quadratic>>
    }

    fun <V> linearInequalityAs(cut: Any): LinearInequality<V>?
        where V : RealNumber<V>, V : NumberField<V> {
        return cut as? LinearInequality<V>
    }

    fun <V> quadraticInequalityAs(cut: Any): QuadraticInequalityOf<V>?
        where V : RealNumber<V>, V : NumberField<V> {
        return cut as? QuadraticInequalityOf<V>
    }

    fun <V> tokenListAsFlt64(tokenTable: AbstractTokenTable<V>): AbstractTokenList<Flt64>
        where V : RealNumber<V>, V : NumberField<V> {
        return tokenTable.tokenList as AbstractTokenList<Flt64>
    }

    fun <V> tokenListAsFlt64OrNull(tokenTable: AbstractTokenTable<V>?): AbstractTokenList<Flt64>?
        where V : RealNumber<V>, V : NumberField<V> {
        return tokenTable?.let { tokenListAsFlt64(it) }
    }

    fun <V> mapValues(values: Map<Symbol, Flt64>, converter: IntoValue<V>): Map<Symbol, V>
        where V : RealNumber<V>, V : NumberField<V> {
        return values.mapValues { converter.intoValue(it.value) }
    }

    fun <V> dependencyAsIntermediate(dependency: IntermediateSymbol<*>): IntermediateSymbol<V>
        where V : RealNumber<V>, V : NumberField<V> {
        return dependency as IntermediateSymbol<V>
    }

    fun <V> linearPolynomialAsFlt64(polynomial: MutableLinearPolynomial<V>): MutableLinearPolynomial<Flt64>
        where V : RealNumber<V>, V : NumberField<V> {
        return MutableLinearPolynomial(
            monomials = polynomial.monomials.map { monomial ->
                LinearMonomial(
                    coefficient = monomial.coefficient.toFlt64(),
                    symbol = monomial.symbol
                )
            },
            constant = polynomial.constant.toFlt64()
        )
    }

    fun <V> quadraticPolynomialAsFlt64(polynomial: MutableQuadraticPolynomial<V>): MutableQuadraticPolynomial<Flt64>
        where V : RealNumber<V>, V : NumberField<V> {
        return MutableQuadraticPolynomial(
            monomials = polynomial.monomials.map { monomial ->
                QuadraticMonomial(
                    coefficient = monomial.coefficient.toFlt64(),
                    symbol1 = monomial.symbol1,
                    symbol2 = monomial.symbol2
                )
            },
            constant = polynomial.constant.toFlt64()
        )
    }

    fun tokenTableAsFlt64OrNull(tokenTable: Any?): AbstractTokenTable<Flt64>? {
        return tokenTable as? AbstractTokenTable<Flt64>
    }

    fun <V> symbolAsIntermediateStar(symbol: Symbol?): IntermediateSymbol<out V>?
        where V : RealNumber<V>, V : NumberField<V> {
        return symbol as? IntermediateSymbol<out V>
    }

    fun linearSolverFlattenedMonomials(symbol: Any): LinearFlattenData<Flt64> {
        val polynomial = (symbol as LinearIntermediateSymbol<*>).toLinearPolynomial()
        return LinearFlattenData(
            monomials = polynomial.monomials.map { monomial ->
                LinearMonomial(
                    coefficient = monomial.coefficient.toFlt64(),
                    symbol = monomial.symbol
                )
            },
            constant = polynomial.constant.toFlt64()
        )
    }

    fun quadraticSolverFlattenedMonomials(symbol: Any): QuadraticFlattenData<Flt64> {
        val polynomial = (symbol as QuadraticIntermediateSymbol<*>).toQuadraticPolynomial()
        return QuadraticFlattenData(
            monomials = polynomial.monomials.map { monomial ->
                QuadraticMonomial(
                    coefficient = monomial.coefficient.toFlt64(),
                    symbol1 = monomial.symbol1,
                    symbol2 = monomial.symbol2
                )
            },
            constant = polynomial.constant.toFlt64()
        )
    }

    @Suppress("UNCHECKED_CAST")
    fun <V> expressionRangeFromFlt64(rangeFlt64: ValueRange<Flt64>?): ExpressionRange<V>
        where V : RealNumber<V>, V : NumberField<V> {
        return ExpressionRange(rangeFlt64 as ValueRange<V>?, Flt64 as RealNumberConstants<V>)
    }

    @Suppress("UNCHECKED_CAST")
    fun <V> fullExpressionRange(): ExpressionRange<V>
        where V : RealNumber<V>, V : NumberField<V> {
        return ExpressionRange(Flt64 as RealNumberConstants<V>)
    }

    fun rangeAsFlt64(symbol: Any): ExpressionRange<Flt64>? {
        val typed = symbol as IntermediateSymbol<Flt64>
        return typed.range as ExpressionRange<Flt64>?
    }
}

fun <V> castLinearMetaModelForSolver(model: LinearMetaModel<V>): LinearMetaModel<Flt64>
        where V : RealNumber<V>, V : NumberField<V> {
    return SolverBoundaryCasts.castLinearMetaModelStar(model)
}

fun <V> castQuadraticMetaModelForSolver(model: QuadraticMetaModel<V>): QuadraticMetaModel<Flt64>
        where V : RealNumber<V>, V : NumberField<V> {
    return SolverBoundaryCasts.castQuadraticMetaModelStar(model)
}
