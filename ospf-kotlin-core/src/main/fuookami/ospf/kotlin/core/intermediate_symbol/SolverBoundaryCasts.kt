package fuookami.ospf.kotlin.core.intermediate_symbol

import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.token.AbstractTokenTable
import fuookami.ospf.kotlin.core.token.AddableTokenCollection
import fuookami.ospf.kotlin.core.token.LinearFlattenData
import fuookami.ospf.kotlin.core.token.QuadraticFlattenData
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.core.intermediate_symbol.function.MathFunctionSymbolBase
import fuookami.ospf.kotlin.core.intermediate_symbol.function.QuadraticMathFunctionSymbolBase
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMechanismModel
import fuookami.ospf.kotlin.core.model.mechanism.AbstractQuadraticMechanismModel
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
    private val flt64Converter = object : IntoValue<Flt64> {
        override fun intoValue(value: Flt64) = value
        override val zero get() = Flt64.zero
        override val one get() = Flt64.one
        override fun fromValue(value: Flt64) = value
    }

    fun registerAuxiliaryTokensStar(
        symbol: IntermediateSymbol<*>,
        tokens: AddableTokenCollection<*>
    ): Try {
        return (symbol as IntermediateSymbol<Flt64>).registerAuxiliaryTokens(tokens as AddableTokenCollection<Flt64>)
    }

    fun prepareStar(
        symbol: IntermediateSymbol<*>,
        fixedValues: Map<Symbol, Flt64>?,
        tokenTable: AbstractTokenTable<*>
    ): Flt64? {
        val sym = symbol as IntermediateSymbol<Flt64>
        val tt = tokenTable as AbstractTokenTable<Flt64>
        return if (fixedValues.isNullOrEmpty()) {
            sym.prepare(null, tt, flt64Converter)
        } else {
            sym.prepare(fixedValues, tt, flt64Converter)
        }
    }

    fun registerConstraintsLinearStar(
        symbol: MathFunctionSymbolBase<*>,
        model: AbstractLinearMechanismModel<*>
    ): Try {
        return (symbol as MathFunctionSymbolBase<Flt64>).registerConstraints(model as AbstractLinearMechanismModel<Flt64>)
    }

    fun registerConstraintsQuadraticStar(
        symbol: QuadraticMathFunctionSymbolBase<*>,
        model: AbstractQuadraticMechanismModel<*>
    ): Try {
        return (symbol as QuadraticMathFunctionSymbolBase<Flt64>).registerConstraints(model as AbstractQuadraticMechanismModel<Flt64>)
    }

    val LinearIntermediateSymbol<*>.solverFlattenedMonomials: LinearFlattenData<Flt64>
        get() = (this as LinearExpressionSymbol<Flt64>).flattenedMonomials

    val QuadraticIntermediateSymbol<*>.solverFlattenedMonomials: QuadraticFlattenData<Flt64>
        get() = (this as QuadraticExpressionSymbol<Flt64>).flattenedMonomials
}
