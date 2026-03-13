package fuookami.ospf.kotlin.core.frontend.inequality.adapter

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.expression.adapter.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.utils.math.symbol.monomial.LinearMonomial as UtilsLinearMonomial
import fuookami.ospf.kotlin.utils.math.symbol.monomial.QuadraticMonomial as UtilsQuadraticMonomial
import fuookami.ospf.kotlin.utils.math.symbol.operation.combineTerms

private fun legacyMergeLinearMonomials(
    positiveMonomials: List<LinearMonomial>,
    negativeMonomials: List<LinearMonomial>
): List<LinearMonomial> {
    return (positiveMonomials.map { it to true } + negativeMonomials.map { it to false })
        .groupBy { it.first.symbol }
        .map { (symbol, monomials) ->
            LinearMonomial(
                monomials.sumOf { if (it.second) { it.first.coefficient } else { -it.first.coefficient } },
                symbol
            )
        }
}

private fun legacyMergeQuadraticMonomials(
    positiveMonomials: List<QuadraticMonomial>,
    negativeMonomials: List<QuadraticMonomial>
): List<QuadraticMonomial> {
    return (positiveMonomials.map { it to true } + negativeMonomials.map { it to false })
        .groupBy { it.first.symbol }
        .map { (symbol, monomials) ->
            QuadraticMonomial(
                monomials.sumOf { if (it.second) { it.first.coefficient } else { -it.first.coefficient } },
                symbol
            )
        }
}

fun mergeLinearMonomialsByUtils(
    positiveMonomials: List<LinearMonomial>,
    negativeMonomials: List<LinearMonomial>
): List<LinearMonomial> {
    val allMonomials = ArrayList<UtilsLinearMonomial>(positiveMonomials.size + negativeMonomials.size)
    for (monomial in positiveMonomials) {
        allMonomials.add(monomial.toUtilsMonomial())
    }
    for (monomial in negativeMonomials) {
        val utilsMonomial = monomial.toUtilsMonomial()
        allMonomials.add(utilsMonomial.copy(coefficient = -utilsMonomial.coefficient))
    }

    val mergedMonomials = allMonomials.combineTerms()
    val coreMonomials = ArrayList<LinearMonomial>(mergedMonomials.size)
    for (monomial in mergedMonomials) {
        when (val result = monomial.toCoreMonomialRet()) {
            is Ok -> coreMonomials.add(result.value)
            is Failed -> return legacyMergeLinearMonomials(positiveMonomials, negativeMonomials)
        }
    }
    return coreMonomials
}

fun mergeQuadraticMonomialsByUtils(
    positiveMonomials: List<QuadraticMonomial>,
    negativeMonomials: List<QuadraticMonomial>
): List<QuadraticMonomial> {
    val allMonomials = ArrayList<UtilsQuadraticMonomial>(positiveMonomials.size + negativeMonomials.size)
    for (monomial in positiveMonomials) {
        allMonomials.add(monomial.toUtilsMonomial())
    }
    for (monomial in negativeMonomials) {
        val utilsMonomial = monomial.toUtilsMonomial()
        allMonomials.add(utilsMonomial.copy(coefficient = -utilsMonomial.coefficient))
    }

    val mergedMonomials = allMonomials.combineTerms()
    val coreMonomials = ArrayList<QuadraticMonomial>(mergedMonomials.size)
    for (monomial in mergedMonomials) {
        when (val result = monomial.toCoreMonomialRet()) {
            is Ok -> coreMonomials.add(result.value)
            is Failed -> return legacyMergeQuadraticMonomials(positiveMonomials, negativeMonomials)
        }
    }
    return coreMonomials
}
