package fuookami.ospf.kotlin.core.model.mechanism

import fuookami.ospf.kotlin.core.token.AbstractTokenTable
import fuookami.ospf.kotlin.core.token.LinearFlattenData
import fuookami.ospf.kotlin.core.token.toQuadraticFlattenData
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial

internal fun <V> buildLinearObjectiveSubObjects(
    metaModel: LinearMetaModel<V>,
    tokens: AbstractTokenTable<V>
): List<LinearSubObject<V>> where V : RealNumber<V>, V : NumberField<V> {
    if (metaModel.flattenSubObjects.isNotEmpty()) {
        return metaModel.flattenSubObjects.map { source ->
            LinearSubObject(
                category = source.category,
                cells = ArrayList(
                    createLinearCells(
                        source.linearTerms().map { (coefficient, symbol) ->
                            LinearMonomial(metaModel.converter.fromValue(coefficient), symbol)
                        },
                        tokens,
                        metaModel.converter
                    )
                ),
                _constant = source.constant,
                name = source.name,
            )
        }
    }
    return metaModel._subObjects.map {
        LinearSubObject(
            category = it.category,
            flattenData = LinearFlattenData(
                it.polynomial.monomials.map { m -> LinearMonomial(m.coefficient, m.symbol) },
                it.polynomial.constant
            ),
            tokens = tokens,
            name = it.name,
            converter = metaModel.converter
        )
    }
}

internal fun <V> buildQuadraticObjectiveSubObjects(
    metaModel: QuadraticMetaModel<V>,
    tokens: AbstractTokenTable<V>
): List<QuadraticSubObject<V>> where V : RealNumber<V>, V : NumberField<V> {
    if (metaModel.flattenSubObjects.isNotEmpty()) {
        return metaModel.flattenSubObjects.map { source ->
            QuadraticSubObject(
                category = source.category,
                cells = ArrayList(
                    source.flattenData.monomials.mapNotNull { monomial ->
                        val variable1 = monomial.symbol1 as? AbstractVariableItem<*, *> ?: return@mapNotNull null
                        val token1 = tokens.find(variable1) ?: return@mapNotNull null
                        val token2 = if (monomial.symbol2 != null) {
                            tokens.find(monomial.symbol2 as? AbstractVariableItem<*, *> ?: return@mapNotNull null) ?: return@mapNotNull null
                        } else {
                            null
                        }
                        if (monomial.coefficient eq metaModel.converter.zero) {
                            null
                        } else {
                            fuookami.ospf.kotlin.core.model.intermediate.QuadraticCellImpl(
                                tokenTable = tokens,
                                _coefficientFlt64 = metaModel.converter.fromValue(monomial.coefficient),
                                token1 = token1,
                                token2 = token2,
                                converter = metaModel.converter
                            )
                        }
                    }
                ),
                _constant = source.flattenData.constant,
                name = source.name
            )
        }
    }
    return metaModel._subObjects.map {
        QuadraticSubObject(
            category = it.category,
            flattenData = LinearFlattenData(
                it.polynomial.monomials.map { m -> LinearMonomial(m.coefficient, m.symbol) },
                it.polynomial.constant
            ).toQuadraticFlattenData(),
            tokens = tokens,
            name = it.name,
            converter = metaModel.converter
        )
    }
}
