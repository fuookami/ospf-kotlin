/**
 * 二次四元模型弹性构建器
 * Quadratic tetrad model elastic builder
 */
package fuookami.ospf.kotlin.core.model.intermediate

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.QuadraticConstraintImpl
import fuookami.ospf.kotlin.core.symbol.IntermediateSymbol
import fuookami.ospf.kotlin.core.variable.Continuous

internal fun QuadraticTetradModel.buildElasticModel(): QuadraticTetradModel {
    var colIndex = this.variables.size
    val slackVariables = ArrayList<Pair<Variable?, Variable?>>()
    for (i in this.constraints.indices) {
        when (this.constraints.signs[i]) {
                ConstraintRelation.LessEqual -> {
                    slackVariables.add(
                        Variable(
                            index = colIndex,
                            lowerBound = Flt64.zero,
                            upperBound = Flt64.infinity,
                            type = Continuous,
                            origin = null,
                            dualOrigin = null,
                            slack = VariableSlack(
                                constraint = this.constraints.origins[i]
                            ),
                            name = "${this.constraints.names[i].ifEmpty { "cons${i}" }}_lb_slack",
                            initialResult = Flt64.zero
                        ) to null
                    )
                    colIndex += 1
                }

                ConstraintRelation.GreaterEqual -> {
                    slackVariables.add(
                        null to Variable(
                            index = colIndex,
                            lowerBound = Flt64.zero,
                            upperBound = Flt64.infinity,
                            type = Continuous,
                            origin = null,
                            dualOrigin = null,
                            slack = VariableSlack(
                                constraint = this.constraints.origins[i]
                            ),
                            name = "${this.constraints.names[i].ifEmpty { "cons${i}" }}_ub_slack",
                            initialResult = Flt64.zero
                        )
                    )
                    colIndex += 1
                }

                ConstraintRelation.Equal -> {
                    slackVariables.add(
                        Variable(
                            index = colIndex,
                            lowerBound = Flt64.zero,
                            upperBound = Flt64.infinity,
                            type = Continuous,
                            origin = null,
                            dualOrigin = null,
                            slack = VariableSlack(
                                constraint = this.constraints.origins[i]
                            ),
                            name = "${this.constraints.names[i].ifEmpty { "cons${i}" }}_lb_slack",
                            initialResult = Flt64.zero
                        ) to Variable(
                            index = colIndex + 1,
                            lowerBound = Flt64.zero,
                            upperBound = Flt64.infinity,
                            type = Continuous,
                            origin = null,
                            dualOrigin = null,
                            slack = VariableSlack(
                                constraint = this.constraints.origins[i]
                            ),
                            name = "${this.constraints.names[i].ifEmpty { "cons${i}" }}_ub_slack",
                            initialResult = Flt64.zero
                        )
                    )
                    colIndex += 2
                }
            }
    }
    for ((_, variable) in this.variables.withIndex()) {
            if (variable.free || variable.positiveNormalized || variable.negativeNormalized) {
                slackVariables.add(null to null)
            } else if (variable.positiveFree) {
                slackVariables.add(
                    Variable(
                        index = colIndex,
                        lowerBound = Flt64.zero,
                        upperBound = Flt64.infinity,
                        type = Continuous,
                        origin = null,
                        dualOrigin = null,
                        slack = VariableSlack(
                            lowerBound = variable
                        ),
                        name = "${variable.name}_lb_slack",
                        initialResult = Flt64.zero
                    ) to null
                )
                colIndex += 1
            } else if (variable.negativeFree) {
                slackVariables.add(
                    null to Variable(
                        index = colIndex,
                        lowerBound = Flt64.zero,
                        upperBound = Flt64.infinity,
                        type = Continuous,
                        origin = null,
                        dualOrigin = null,
                        slack = VariableSlack(
                            upperBound = variable
                        ),
                        name = "${variable.name}_ub_slack",
                        initialResult = Flt64.zero
                    )
                )
                colIndex += 1
            } else {
                slackVariables.add(
                    Variable(
                        index = colIndex,
                        lowerBound = Flt64.zero,
                        upperBound = Flt64.infinity,
                        type = Continuous,
                        origin = null,
                        dualOrigin = null,
                        slack = VariableSlack(
                            lowerBound = variable
                        ),
                        name = "${variable.name}_lb_slack",
                        initialResult = Flt64.zero
                    ) to Variable(
                        index = colIndex + 1,
                        lowerBound = Flt64.zero,
                        upperBound = Flt64.infinity,
                        type = Continuous,
                        origin = null,
                        dualOrigin = null,
                        slack = VariableSlack(
                            upperBound = variable
                        ),
                        name = "${variable.name}_ub_slack",
                        initialResult = Flt64.zero
                    )
                )
                colIndex += 2
            }
    }

    var rowIndex = this.variables.size
    val lhs = this.constraints.indices.map { i ->
                this.constraints.lhs[i] + listOfNotNull(
                    slackVariables[i].first?.let {
                        QuadraticConstraintCell(
                            rowIndex = i,
                            colIndex1 = it.index,
                            colIndex2 = null,
                            coefficient = -Flt64.one,
                        )
                    },
                    slackVariables[i].second?.let {
                        QuadraticConstraintCell(
                            rowIndex = i,
                            colIndex1 = it.index,
                            colIndex2 = null,
                            coefficient = Flt64.one,
                        )
                    }
                )
            } + this.variables.indices.flatMap { j ->
                val jp = this.constraints.size + j
                val thisLhs = ArrayList<List<QuadraticConstraintCell>>()
                if (slackVariables[jp].first != null) {
                    thisLhs.add(
                        listOf(
                            QuadraticConstraintCell(
                                rowIndex = rowIndex,
                                colIndex1 = j,
                                colIndex2 = null,
                                coefficient = Flt64.one,
                            ),
                            QuadraticConstraintCell(
                                rowIndex = rowIndex,
                                colIndex1 = slackVariables[jp].first!!.index,
                                colIndex2 = null,
                                coefficient = Flt64.one,
                            )
                        )
                    )
                    rowIndex += 1
                }
                if (slackVariables[jp].second != null) {
                    thisLhs.add(
                        listOf(
                            QuadraticConstraintCell(
                                rowIndex = rowIndex,
                                colIndex1 = j,
                                colIndex2 = null,
                                coefficient = Flt64.one,
                            ),
                            QuadraticConstraintCell(
                                rowIndex = rowIndex,
                                colIndex1 = slackVariables[jp].second!!.index,
                                colIndex2 = null,
                                coefficient = -Flt64.one,
                            )
                        )
                    )
                    rowIndex += 1
                }
                thisLhs
            }
    val constraints = QuadraticConstraintBatch(
            sparseLhs = buildQuadraticSparseLhs(lhs),
            signs = this.constraints.signs + this.variables.indices.flatMap { j ->
                val jp = this.constraints.size + j
                val thisConstraintRelations = ArrayList<ConstraintRelation>()
                if (slackVariables[jp].first != null) {
                    thisConstraintRelations.add(ConstraintRelation.GreaterEqual)
                }
                if (slackVariables[jp].second != null) {
                    thisConstraintRelations.add(ConstraintRelation.LessEqual)
                }
                thisConstraintRelations
            },
            rhs = this.constraints.rhs + this.variables.flatMapIndexed { j, variable ->
                val jp = this.constraints.size + j
                val thisRhs = ArrayList<Flt64>()
                if (slackVariables[jp].first != null) {
                    thisRhs.add(variable.lowerBound)
                }
                if (slackVariables[jp].second != null) {
                    thisRhs.add(variable.upperBound)
                }
                thisRhs
            },
            names = this.constraints.names.mapIndexed { i, name -> "${name.ifEmpty { "cons${i}" }}_elastic" } + this.variables.flatMapIndexed { j, variable ->
                val jp = this.constraints.size + j
                val thisNames = ArrayList<String>()
                if (slackVariables[jp].first != null) {
                    thisNames.add("${variable.name}_lb_slack")
                }
                if (slackVariables[jp].second != null) {
                    thisNames.add("${variable.name}_ub_slack")
                }
                thisNames
            },
            sources = this.constraints.sources.map { ConstraintSource.Elastic } + this.variables.indices.flatMap { j ->
                val jp = this.constraints.size + j
                val thisSources = ArrayList<ConstraintSource>()
                if (slackVariables[jp].first != null) {
                    thisSources.add(ConstraintSource.ElasticLowerBound)
                }
                if (slackVariables[jp].second != null) {
                    thisSources.add(ConstraintSource.ElasticUpperBound)
                }
                thisSources
            },
            origins = this.constraints.origins + this.variables.indices.flatMap { j ->
                val jp = this.constraints.size + j
                val thisOrigins = ArrayList<QuadraticConstraintImpl<Flt64>?>()
                if (slackVariables[jp].first != null) {
                    thisOrigins.add(null)
                }
                if (slackVariables[jp].second != null) {
                    thisOrigins.add(null)
                }
                thisOrigins
            },
            froms = this.constraints.froms + this.variables.indices.flatMap { j ->
                val jp = this.constraints.size + j
                val thisOrigins = ArrayList<Pair<IntermediateSymbol<Flt64>, Boolean>?>()
                if (slackVariables[jp].first != null) {
                    thisOrigins.add(null)
                }
                if (slackVariables[jp].second != null) {
                    thisOrigins.add(null)
                }
                thisOrigins
            },
            priorities = this.constraints.priorities + this.variables.indices.flatMap { j ->
                val jp = this.constraints.size + j
                val thisPriorities = ArrayList<Int?>()
                if (slackVariables[jp].first != null) {
                    thisPriorities.add(null)
                }
                if (slackVariables[jp].second != null) {
                    thisPriorities.add(null)
                }
                thisPriorities
            }
    )

    val objective = slackVariables.flatMap { (posSlack, negSlack) ->
            listOfNotNull(
                posSlack?.let {
                    QuadraticObjectiveCell(
                        colIndex1 = it.index,
                        colIndex2 = null,
                        coefficient = Flt64.one
                    )
                },
                negSlack?.let {
                    QuadraticObjectiveCell(
                        colIndex1 = it.index,
                        colIndex2 = null,
                        coefficient = Flt64.one
                    )
                }
            )
    }

    return QuadraticTetradModel(
            impl = BasicQuadraticTetradModel(
                variables = this.variables.map {
                    it.copy().apply {
                        if (!free && !positiveNormalized && !negativeNormalized) {
                            _lowerBound = Flt64.negativeInfinity
                            _upperBound = Flt64.infinity
                        }
                    }
                } + slackVariables.flatMap { it.toList().filterNotNull() }.sortedBy { it.index },
                constraints = constraints,
                name = "$name-elastic"
            ),
            tokensInSolver = tokensInSolver,
            objective = QuadraticObjective(ObjectCategory.Minimum, objective)
    )
}
