package fuookami.ospf.kotlin.core.frontend.inequality.adapter

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.expression.adapter.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.utils.math.symbol.monomial.LinearMonomial as UtilsLinearMonomial
import fuookami.ospf.kotlin.utils.math.symbol.monomial.QuadraticMonomial as UtilsQuadraticMonomial
import fuookami.ospf.kotlin.utils.math.symbol.operation.combineTerms

private fun <CoreMonomial, UtilsMonomial> mergeMonomialsByUtils(
    positiveMonomials: List<CoreMonomial>,
    negativeMonomials: List<CoreMonomial>,
    toUtilsMonomial: (CoreMonomial) -> UtilsMonomial,
    negateUtilsMonomial: (UtilsMonomial) -> UtilsMonomial,
    combineMonomials: (List<UtilsMonomial>) -> List<UtilsMonomial>,
    toCoreMonomialRet: (UtilsMonomial) -> Ret<CoreMonomial>,
    errorPrefix: String
): List<CoreMonomial> {
    val allMonomials = ArrayList<UtilsMonomial>(positiveMonomials.size + negativeMonomials.size)
    for (monomial in positiveMonomials) {
        allMonomials.add(toUtilsMonomial(monomial))
    }
    for (monomial in negativeMonomials) {
        val utilsMonomial = toUtilsMonomial(monomial)
        allMonomials.add(negateUtilsMonomial(utilsMonomial))
    }

    val mergedMonomials = combineMonomials(allMonomials)
    val coreMonomials = ArrayList<CoreMonomial>(mergedMonomials.size)
    for (monomial in mergedMonomials) {
        when (val result = toCoreMonomialRet(monomial)) {
            is Ok -> coreMonomials.add(result.value)
            is Failed -> {
                error("$errorPrefix normalize adapter failed unexpectedly: ${result.error}")
            }
        }
    }
    return coreMonomials
}

fun mergeLinearMonomialsByUtils(
    positiveMonomials: List<LinearMonomial>,
    negativeMonomials: List<LinearMonomial>
): List<LinearMonomial> {
    return mergeMonomialsByUtils(
        positiveMonomials = positiveMonomials,
        negativeMonomials = negativeMonomials,
        toUtilsMonomial = { it.toUtilsMonomial() },
        negateUtilsMonomial = { it.copy(coefficient = -it.coefficient) },
        combineMonomials = { source: List<UtilsLinearMonomial> -> source.combineTerms() },
        toCoreMonomialRet = { it.toCoreMonomialRet() },
        errorPrefix = "Linear"
    )
}

fun mergeQuadraticMonomialsByUtils(
    positiveMonomials: List<QuadraticMonomial>,
    negativeMonomials: List<QuadraticMonomial>
): List<QuadraticMonomial> {
    return mergeMonomialsByUtils(
        positiveMonomials = positiveMonomials,
        negativeMonomials = negativeMonomials,
        toUtilsMonomial = { it.toUtilsMonomial() },
        negateUtilsMonomial = { it.copy(coefficient = -it.coefficient) },
        combineMonomials = { source: List<UtilsQuadraticMonomial> -> source.combineTerms() },
        toCoreMonomialRet = { it.toCoreMonomialRet() },
        errorPrefix = "Quadratic"
    )
}
