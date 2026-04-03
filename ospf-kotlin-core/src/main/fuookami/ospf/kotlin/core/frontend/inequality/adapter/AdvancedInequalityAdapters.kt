package fuookami.ospf.kotlin.core.frontend.inequality.adapter

import fuookami.ospf.kotlin.core.frontend.inequality.LinearInequality
import fuookami.ospf.kotlin.core.frontend.inequality.QuadraticInequality
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.parser.Expr
import fuookami.ospf.kotlin.math.symbol.parser.parseSymbolInequality
import fuookami.ospf.kotlin.math.symbol.serde.symbolExprFromJson
import fuookami.ospf.kotlin.math.symbol.serde.toCanonicalInequality
import fuookami.ospf.kotlin.math.symbol.serde.toExpr
import fuookami.ospf.kotlin.math.symbol.serde.toJsonString
import fuookami.ospf.kotlin.math.symbol.serde.toLinearInequalityOrNull
import fuookami.ospf.kotlin.math.symbol.serde.toQuadraticInequalityOrNull
import fuookami.ospf.kotlin.math.symbol.operation.toLatex

fun LinearInequality.toLatex(): String {
    return toUtilsInequality().toLatex()
}

fun QuadraticInequality.toLatex(): String {
    return toUtilsInequality().toLatex()
}

fun LinearInequality.toSymbolExpr(): Expr.Comparison {
    return toUtilsInequality().toExpr()
}

fun QuadraticInequality.toSymbolExpr(): Expr.Comparison {
    return toUtilsInequality().toExpr()
}

fun LinearInequality.toSymbolExprJson(): String {
    return toSymbolExpr().toJsonString()
}

fun QuadraticInequality.toSymbolExprJson(): String {
    return toSymbolExpr().toJsonString()
}

fun Expr.Comparison.toCoreLinearInequalityRet(
    symbolOf: (String) -> Symbol
): Ret<LinearInequality> {
    val utilsInequality = toLinearInequalityOrNull(symbolOf)
        ?: return Failed(ErrorCode.IllegalArgument, "Cannot convert expression to linear inequality.")
    return utilsInequality.toCoreInequalityRet()
}

fun Expr.Comparison.toCoreQuadraticInequalityRet(
    symbolOf: (String) -> Symbol
): Ret<QuadraticInequality> {
    val utilsInequality = toQuadraticInequalityOrNull(symbolOf)
        ?: return Failed(ErrorCode.IllegalArgument, "Cannot convert expression to quadratic inequality.")
    return utilsInequality.toCoreInequalityRet()
}

fun parseCoreLinearInequalityRet(
    input: String,
    symbolOf: (String) -> Symbol
): Ret<LinearInequality> {
    return parseSymbolInequality(input).toCoreLinearInequalityRet(symbolOf)
}

fun parseCoreQuadraticInequalityRet(
    input: String,
    symbolOf: (String) -> Symbol
): Ret<QuadraticInequality> {
    return parseSymbolInequality(input).toCoreQuadraticInequalityRet(symbolOf)
}

fun linearCoreInequalityFromExprJsonRet(
    json: String,
    symbolOf: (String) -> Symbol
): Ret<LinearInequality> {
    val expr = symbolExprFromJson(json) as? Expr.Comparison
        ?: return Failed(ErrorCode.IllegalArgument, "Expression json is not a comparison.")
    return expr.toCoreLinearInequalityRet(symbolOf)
}

fun quadraticCoreInequalityFromExprJsonRet(
    json: String,
    symbolOf: (String) -> Symbol
): Ret<QuadraticInequality> {
    val expr = symbolExprFromJson(json) as? Expr.Comparison
        ?: return Failed(ErrorCode.IllegalArgument, "Expression json is not a comparison.")
    return expr.toCoreQuadraticInequalityRet(symbolOf)
}
