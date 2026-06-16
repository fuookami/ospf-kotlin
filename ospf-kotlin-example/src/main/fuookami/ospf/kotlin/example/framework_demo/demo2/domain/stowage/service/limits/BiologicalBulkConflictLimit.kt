package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.service.limits

import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*

import fuookami.ospf.kotlin.utils.functional.*

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*

import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.token.*

import fuookami.ospf.kotlin.framework.model.*

class BiologicalBulkConflictLimit(
    private val items: List<Item>,
    private val positions: List<Position>,
    private val biologicalLimit: BiologicalLimit,
    private val stowage: Stowage,
    override val name: String = "biological_bulk_conflict_limit"
): Pipeline<AbstractLinearMetaModel<Flt64>> {
    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        for ((j, position) in positions.withIndex()) {
            if (position.status.unavailable || !position.location.bulk) {
                continue
            }

            for ((type1, type2) in biologicalLimit.bulkConflictLimit) {
                if (position.loadedItems.any { it.cargo.contains(type1) } && !position.loadedItems.none { it.cargo.contains(type2) }) {
                    for ((i, item) in items.withIndex()) {
                        if (Stowage.stowageNeeded(item, position) && item.cargo.contains(type2)) {
                            when (val result = model.addConstraint(
            relation = LinearPolynomial(stowage.stowage[i, j]) eq Flt64.zero,
            name = "${name}_${item}_${position}"
                            )) {
                                is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}

                                is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                                    return Failed(result.error)
                                }

                                is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                                    return Fatal(result.errors)
                                }
                            }
                        }
                    }
                }
                if (position.loadedItems.any { it.cargo.contains(type2) } && !position.loadedItems.none { it.cargo.contains(type1) }) {
                    for ((i, item) in items.withIndex()) {
                        if (Stowage.stowageNeeded(item, position) && item.cargo.contains(type1)) {
                            when (val result = model.addConstraint(
            relation = LinearPolynomial(stowage.stowage[i, j]) eq Flt64.zero,
            name = "${name}_${item}_${position}"
                            )) {
                                is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}

                                is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                                    return Failed(result.error)
                                }

                                is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                                    return Fatal(result.errors)
                                }
                            }
                        }
                    }
                }
                if (position.loadedItems.none { it.cargo.contains(type1) || it.cargo.contains(type2) }) {
                    for ((i1, item1) in items.withIndex()) {
                        if (!Stowage.stowageNeeded(item1, position)) {
                            continue
                        }

                        for (i2 in (i1 + 1) until items.size) {
                            val item2 = items[i2]
                            if (!Stowage.stowageNeeded(item2, position)) {
                                continue
                            }

                            if ((item1.cargo.contains(type1) && item2.cargo.contains(type2))
                                || (item1.cargo.contains(type2) && item2.cargo.contains(type1))
                            ) {
                                when (val result = model.addConstraint(
            relation = run {
                val poly = MutableLinearPolynomial()
                poly += LinearMonomial(Flt64.one, stowage.stowage[i1, j])
                poly += LinearMonomial(Flt64.one, stowage.stowage[i2, j])
                LinearPolynomial(poly) leq Flt64.one
            },
            name = "${name}_${item1}_${item2}_${position}"
                                )) {
                                    is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}

                                    is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                                        return Failed(result.error)
                                    }

                                    is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                                        return Fatal(result.errors)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return ok
    }
}
