package fuookami.ospf.kotlin.core.frontend.inequality.adapter

import fuookami.ospf.kotlin.core.frontend.expression.adapter.toCoreMonomialRet
import fuookami.ospf.kotlin.core.frontend.expression.adapter.toUtilsMonomial
import fuookami.ospf.kotlin.core.frontend.expression.monomial.LinearMonomial
import fuookami.ospf.kotlin.core.frontend.expression.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.operation.combineTerms
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial as UtilsLinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial as UtilsQuadraticMonomial

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

            is Fatal -> {
                error("$errorPrefix normalize adapter failed unexpectedly: ${result.errors}")
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
        combineMonomials = { source: List<UtilsLinearMonomial<Flt64>> -> source.combineTerms() },
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
        combineMonomials = { source: List<UtilsQuadraticMonomial<Flt64>> -> source.combineTerms() },
        toCoreMonomialRet = { it.toCoreMonomialRet() },
        errorPrefix = "Quadratic"
    )
}




