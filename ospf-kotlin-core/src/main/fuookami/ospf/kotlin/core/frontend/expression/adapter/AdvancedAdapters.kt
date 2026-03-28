package fuookami.ospf.kotlin.core.frontend.expression.adapter

import fuookami.ospf.kotlin.core.frontend.expression.polynomial.AbstractLinearPolynomial
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.AbstractQuadraticPolynomial
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.math.algebra.number.Flt64
import fuookami.ospf.kotlin.utils.math.symbol.Symbol
import fuookami.ospf.kotlin.utils.math.symbol.parser.parseSymbolExpression
import fuookami.ospf.kotlin.utils.math.symbol.serde.SymbolExpr
import fuookami.ospf.kotlin.utils.math.symbol.serde.symbolExprFromJson
import fuookami.ospf.kotlin.utils.math.symbol.serde.toExpr
import fuookami.ospf.kotlin.utils.math.symbol.serde.toJsonString
import fuookami.ospf.kotlin.utils.math.symbol.serde.toLinearPolynomialOrNull
import fuookami.ospf.kotlin.utils.math.symbol.serde.toQuadraticPolynomialOrNull
import fuookami.ospf.kotlin.utils.math.symbol.operation.compileEval
import fuookami.ospf.kotlin.utils.math.symbol.operation.compileGradient
import fuookami.ospf.kotlin.utils.math.symbol.operation.toLatex

fun AbstractLinearPolynomial<*>.compileEval(
    order: List<Symbol>,
    combineTerms: Boolean = true
): (List<Flt64>) -> Flt64 {
    return toUtilsPolynomial().compileEval(order, combineTerms)
}

fun AbstractQuadraticPolynomial<*>.compileEval(
    order: List<Symbol>,
    combineTerms: Boolean = true
): (List<Flt64>) -> Flt64 {
    return toUtilsPolynomial().compileEval(order, combineTerms)
}

fun AbstractLinearPolynomial<*>.compileGradient(
    order: List<Symbol>,
    combineTerms: Boolean = true
): (List<Flt64>) -> List<Flt64> {
    return toUtilsPolynomial().compileGradient(order, combineTerms)
}

fun AbstractQuadraticPolynomial<*>.compileGradient(
    order: List<Symbol>,
    combineTerms: Boolean = true
): (List<Flt64>) -> List<Flt64> {
    return toUtilsPolynomial().compileGradient(order, combineTerms)
}

fun AbstractLinearPolynomial<*>.toLatex(): String {
    return toUtilsPolynomial().toLatex()
}

fun AbstractQuadraticPolynomial<*>.toLatex(): String {
    return toUtilsPolynomial().toLatex()
}

fun AbstractLinearPolynomial<*>.toSymbolExpr(): SymbolExpr {
    return toUtilsPolynomial().toExpr()
}

fun AbstractQuadraticPolynomial<*>.toSymbolExpr(): SymbolExpr {
    return toUtilsPolynomial().toExpr()
}

fun AbstractLinearPolynomial<*>.toSymbolExprJson(): String {
    return toSymbolExpr().toJsonString()
}

fun AbstractQuadraticPolynomial<*>.toSymbolExprJson(): String {
    return toSymbolExpr().toJsonString()
}

fun SymbolExpr.toCoreLinearPolynomialRet(
    symbolOf: (String) -> Symbol
): Ret<LinearPolynomial> {
    val utilsPolynomial = toLinearPolynomialOrNull(symbolOf)
        ?: return Failed(ErrorCode.IllegalArgument, "Cannot convert expression to linear polynomial.")
    return utilsPolynomial.toCorePolynomialRet()
}

fun SymbolExpr.toCoreQuadraticPolynomialRet(
    symbolOf: (String) -> Symbol
): Ret<QuadraticPolynomial> {
    val utilsPolynomial = toQuadraticPolynomialOrNull(symbolOf)
        ?: return Failed(ErrorCode.IllegalArgument, "Cannot convert expression to quadratic polynomial.")
    return utilsPolynomial.toCorePolynomialRet()
}

fun parseCoreLinearPolynomialRet(
    input: String,
    symbolOf: (String) -> Symbol
): Ret<LinearPolynomial> {
    return parseSymbolExpression(input).toCoreLinearPolynomialRet(symbolOf)
}

fun parseCoreQuadraticPolynomialRet(
    input: String,
    symbolOf: (String) -> Symbol
): Ret<QuadraticPolynomial> {
    return parseSymbolExpression(input).toCoreQuadraticPolynomialRet(symbolOf)
}

fun linearCorePolynomialFromExprJsonRet(
    json: String,
    symbolOf: (String) -> Symbol
): Ret<LinearPolynomial> {
    return symbolExprFromJson(json).toCoreLinearPolynomialRet(symbolOf)
}

fun quadraticCorePolynomialFromExprJsonRet(
    json: String,
    symbolOf: (String) -> Symbol
): Ret<QuadraticPolynomial> {
    return symbolExprFromJson(json).toCoreQuadraticPolynomialRet(symbolOf)
}

fun <T> Ret<T>.requireOk(): T {
    return when (this) {
        is Ok -> value
        is Failed -> throw IllegalArgumentException(error.message)
        is Fatal -> throw IllegalArgumentException(errors.joinToString { it.message })
    }
}




