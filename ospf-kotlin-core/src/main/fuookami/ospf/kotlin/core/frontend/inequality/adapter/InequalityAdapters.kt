package fuookami.ospf.kotlin.core.frontend.inequality.adapter

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.inequality.Sign as CoreSign
import fuookami.ospf.kotlin.core.frontend.inequality.LinearInequality as CoreLinearInequality
import fuookami.ospf.kotlin.core.frontend.inequality.QuadraticInequality as CoreQuadraticInequality
import fuookami.ospf.kotlin.core.frontend.expression.adapter.*
import fuookami.ospf.kotlin.utils.math.symbol.inequality.Comparison as UtilsComparison
import fuookami.ospf.kotlin.utils.math.symbol.inequality.LinearInequality as UtilsLinearInequality
import fuookami.ospf.kotlin.utils.math.symbol.inequality.QuadraticInequality as UtilsQuadraticInequality

fun CoreSign.toUtilsComparison(): UtilsComparison {
    return when (this) {
        CoreSign.Less -> UtilsComparison.LT
        CoreSign.LessEqual -> UtilsComparison.LE
        CoreSign.Equal -> UtilsComparison.EQ
        CoreSign.Unequal -> UtilsComparison.NE
        CoreSign.GreaterEqual -> UtilsComparison.GE
        CoreSign.Greater -> UtilsComparison.GT
    }
}

fun UtilsComparison.toCoreSign(): CoreSign {
    return when (this) {
        UtilsComparison.LT -> CoreSign.Less
        UtilsComparison.LE -> CoreSign.LessEqual
        UtilsComparison.EQ -> CoreSign.Equal
        UtilsComparison.NE -> CoreSign.Unequal
        UtilsComparison.GE -> CoreSign.GreaterEqual
        UtilsComparison.GT -> CoreSign.Greater
    }
}

fun CoreLinearInequality.toUtilsInequality(): UtilsLinearInequality {
    return UtilsLinearInequality(
        lhs = lhs.toUtilsPolynomial(),
        rhs = rhs.toUtilsPolynomial(),
        comparison = sign.toUtilsComparison()
    )
}

fun CoreQuadraticInequality.toUtilsInequality(): UtilsQuadraticInequality {
    return UtilsQuadraticInequality(
        lhs = lhs.toUtilsPolynomial(),
        rhs = rhs.toUtilsPolynomial(),
        comparison = sign.toUtilsComparison()
    )
}

fun UtilsLinearInequality.toCoreInequalityRet(): Ret<CoreLinearInequality> {
    val coreLhs = when (val result = lhs.toCorePolynomialRet()) {
        is Ok -> result.value
        is Failed -> return Failed(result.error)
    }
    val coreRhs = when (val result = rhs.toCorePolynomialRet()) {
        is Ok -> result.value
        is Failed -> return Failed(result.error)
    }
    return Ok(
        CoreLinearInequality(
            lhs = coreLhs,
            rhs = coreRhs,
            sign = comparison.toCoreSign()
        )
    )
}

fun UtilsLinearInequality.toCoreInequalityOrNull(): CoreLinearInequality? {
    return when (val result = toCoreInequalityRet()) {
        is Ok -> result.value
        is Failed -> null
    }
}

fun UtilsQuadraticInequality.toCoreInequalityRet(): Ret<CoreQuadraticInequality> {
    val coreLhs = when (val result = lhs.toCorePolynomialRet()) {
        is Ok -> result.value
        is Failed -> return Failed(result.error)
    }
    val coreRhs = when (val result = rhs.toCorePolynomialRet()) {
        is Ok -> result.value
        is Failed -> return Failed(result.error)
    }
    return Ok(
        CoreQuadraticInequality(
            lhs = coreLhs,
            rhs = coreRhs,
            sign = comparison.toCoreSign()
        )
    )
}

fun UtilsQuadraticInequality.toCoreInequalityOrNull(): CoreQuadraticInequality? {
    return when (val result = toCoreInequalityRet()) {
        is Ok -> result.value
        is Failed -> null
    }
}
