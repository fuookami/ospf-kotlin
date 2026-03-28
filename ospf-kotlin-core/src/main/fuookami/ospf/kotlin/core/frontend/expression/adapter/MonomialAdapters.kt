package fuookami.ospf.kotlin.core.frontend.expression.adapter

import fuookami.ospf.kotlin.core.frontend.expression.symbol.LinearIntermediateSymbol
import fuookami.ospf.kotlin.core.frontend.expression.symbol.QuadraticIntermediateSymbol
import fuookami.ospf.kotlin.core.frontend.variable.AbstractVariableItem
import fuookami.ospf.kotlin.utils.error.Error
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.math.algebra.number.Flt64
import fuookami.ospf.kotlin.utils.math.symbol.Symbol
import fuookami.ospf.kotlin.core.frontend.expression.monomial.LinearMonomial as CoreLinearMonomial
import fuookami.ospf.kotlin.core.frontend.expression.monomial.LinearMonomialSymbol as CoreLinearMonomialSymbol
import fuookami.ospf.kotlin.core.frontend.expression.monomial.QuadraticMonomial as CoreQuadraticMonomial
import fuookami.ospf.kotlin.core.frontend.expression.monomial.QuadraticMonomialSymbolUnit as CoreQuadraticMonomialSymbolUnit
import fuookami.ospf.kotlin.utils.math.symbol.monomial.LinearMonomial as UtilsLinearMonomial
import fuookami.ospf.kotlin.utils.math.symbol.monomial.QuadraticMonomial as UtilsQuadraticMonomial

private fun coreLinearSymbolToCommonSymbol(
    symbol: CoreLinearMonomialSymbol
): Symbol {
    return symbol.variable ?: symbol.exprSymbol!!
}

private fun coreQuadraticUnitToCommonSymbol(
    symbol: CoreQuadraticMonomialSymbolUnit
): Symbol {
    return when (symbol) {
        is Variant3.V1 -> symbol.value
        is Variant3.V2 -> symbol.value
        is Variant3.V3 -> symbol.value
    }
}

private fun unsupportedLinearSymbol(symbol: Symbol): Failed<CoreLinearMonomial, Error> {
    return Failed(
        ErrorCode.IllegalArgument,
        "Unsupported symbol for core linear monomial adapter: ${symbol.name}"
    )
}

private fun unsupportedQuadraticSymbols(
    symbol1: Symbol,
    symbol2: Symbol?
): Failed<CoreQuadraticMonomial, Error> {
    return Failed(
        ErrorCode.IllegalArgument,
        "Unsupported symbols for core quadratic monomial adapter: ${symbol1.name}, ${symbol2?.name}"
    )
}

fun CoreLinearMonomial.toUtilsMonomial(): UtilsLinearMonomial<Flt64> {
    return UtilsLinearMonomial(
        coefficient = coefficient,
        symbol = coreLinearSymbolToCommonSymbol(symbol)
    )
}

fun CoreQuadraticMonomial.toUtilsMonomial(): UtilsQuadraticMonomial<Flt64> {
    return UtilsQuadraticMonomial(
        coefficient = coefficient,
        symbol1 = coreQuadraticUnitToCommonSymbol(symbol.symbol1),
        symbol2 = symbol.symbol2?.let { coreQuadraticUnitToCommonSymbol(it) }
    )
}

fun UtilsLinearMonomial<Flt64>.toCoreMonomialRet(): Ret<CoreLinearMonomial> {
    val coreSymbol = symbol
    return when (coreSymbol) {
        is AbstractVariableItem<*, *> -> {
            Ok(CoreLinearMonomial(coefficient, coreSymbol))
        }

        is LinearIntermediateSymbol -> {
            Ok(CoreLinearMonomial(coefficient, coreSymbol))
        }

        else -> {
            unsupportedLinearSymbol(coreSymbol)
        }
    }
}

@Deprecated(
    message = "Use toCoreMonomialRet() to keep adapter failures explicit."
)
fun UtilsLinearMonomial<Flt64>.toCoreMonomialOrNull(): CoreLinearMonomial? {
    return when (val result = toCoreMonomialRet()) {
        is Ok -> result.value
        is Failed -> null
        is Fatal -> null
    }
}

fun UtilsQuadraticMonomial<Flt64>.toCoreMonomialRet(): Ret<CoreQuadraticMonomial> {
    val coreSymbol1 = symbol1
    val coreSymbol2 = symbol2

    if (coreSymbol2 == null) {
        return when (coreSymbol1) {
            is AbstractVariableItem<*, *> -> Ok(CoreQuadraticMonomial(coefficient, coreSymbol1))
            is LinearIntermediateSymbol -> Ok(CoreQuadraticMonomial(coefficient, coreSymbol1))
            is QuadraticIntermediateSymbol -> Ok(CoreQuadraticMonomial(coefficient, coreSymbol1))
            else -> unsupportedQuadraticSymbols(coreSymbol1, null)
        }
    }

    return when {
        coreSymbol1 is AbstractVariableItem<*, *> && coreSymbol2 is AbstractVariableItem<*, *> -> {
            Ok(CoreQuadraticMonomial(coefficient, coreSymbol1, coreSymbol2))
        }

        coreSymbol1 is AbstractVariableItem<*, *> && coreSymbol2 is LinearIntermediateSymbol -> {
            Ok(CoreQuadraticMonomial(coefficient, coreSymbol1, coreSymbol2))
        }

        coreSymbol1 is LinearIntermediateSymbol && coreSymbol2 is AbstractVariableItem<*, *> -> {
            Ok(CoreQuadraticMonomial(coefficient, coreSymbol2, coreSymbol1))
        }

        coreSymbol1 is AbstractVariableItem<*, *> && coreSymbol2 is QuadraticIntermediateSymbol -> {
            Ok(CoreQuadraticMonomial(coefficient, coreSymbol1, coreSymbol2))
        }

        coreSymbol1 is QuadraticIntermediateSymbol && coreSymbol2 is AbstractVariableItem<*, *> -> {
            Ok(CoreQuadraticMonomial(coefficient, coreSymbol2, coreSymbol1))
        }

        coreSymbol1 is LinearIntermediateSymbol && coreSymbol2 is LinearIntermediateSymbol -> {
            Ok(CoreQuadraticMonomial(coefficient, coreSymbol1, coreSymbol2))
        }

        coreSymbol1 is LinearIntermediateSymbol && coreSymbol2 is QuadraticIntermediateSymbol -> {
            Ok(CoreQuadraticMonomial(coefficient, coreSymbol1, coreSymbol2))
        }

        coreSymbol1 is QuadraticIntermediateSymbol && coreSymbol2 is LinearIntermediateSymbol -> {
            Ok(CoreQuadraticMonomial(coefficient, coreSymbol2, coreSymbol1))
        }

        coreSymbol1 is QuadraticIntermediateSymbol && coreSymbol2 is QuadraticIntermediateSymbol -> {
            Ok(CoreQuadraticMonomial(coefficient, coreSymbol1, coreSymbol2))
        }

        else -> {
            unsupportedQuadraticSymbols(coreSymbol1, coreSymbol2)
        }
    }
}

@Deprecated(
    message = "Use toCoreMonomialRet() to keep adapter failures explicit."
)
fun UtilsQuadraticMonomial<Flt64>.toCoreMonomialOrNull(): CoreQuadraticMonomial? {
    return when (val result = toCoreMonomialRet()) {
        is Ok -> result.value
        is Failed -> null
        is Fatal -> null
    }
}



