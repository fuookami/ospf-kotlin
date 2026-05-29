/**
 * 线性三元模型弹性构建器
 * Linear triad model elastic builder
 */
package fuookami.ospf.kotlin.core.model.intermediate

import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.LinearConstraintImpl
import fuookami.ospf.kotlin.core.symbol.IntermediateSymbol
import fuookami.ospf.kotlin.core.variable.Binary
import fuookami.ospf.kotlin.core.variable.Continuous
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.operator.abs

/**
 * 构建弹性模型
 * Build elastic model
 *
 * 为线性三元模型添加松弛变量，使其成为弹性模型。
 * Adds slack variables to the linear triad model to make it an elastic model.
 *
 * @param minmaxSlack 是否添加最小最大松弛变量 / Whether to add minmax slack variable
 * @param minSlackAmount 最小松弛量约束（二元变量阈值，松弛量阈值）/ Minimum slack amount constraint (binary threshold, slack threshold)
 * @return 弹性模型 / Elastic model
 */
internal fun LinearTriadModel.buildElasticModel(
    minmaxSlack: Boolean,
    minSlackAmount: Pair<UInt64, Flt64>?
): LinearTriadModel {
    var colIndex = this.variables.size
    val slackVariables = ArrayList<Pair<Variable?, Variable?>>()
    val slackBinVariables = ArrayList<Pair<Variable, Variable>>()
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
                slackVariables.add(
                    null to null
                )
            } else if (variable.positiveFree) {
                // x ??lb
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
                // x ??ub
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
                // lb ??x ??ub
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
    if (minSlackAmount != null) {
            slackBinVariables.addAll(slackVariables.flatMap { it.toList().filterNotNull() }.mapIndexed { j, slack ->
                slack to Variable(
                    index = colIndex + j,
                    lowerBound = Flt64.zero,
                    upperBound = Flt64.one,
                    type = Binary,
                    origin = null,
                    dualOrigin = null,
                    name = "${slack.name}_bin",
                    initialResult = Flt64.zero
                )
            })
            colIndex += slackBinVariables.size
    }
    val minmaxSlackVariable = if (minmaxSlack) {
            val minmax = Variable(
                index = colIndex,
                lowerBound = Flt64.zero,
                upperBound = Flt64.infinity,
                type = Continuous,
                origin = null,
                dualOrigin = null,
                name = "minmax_slack",
                initialResult = Flt64.zero
            )
            colIndex += 1
            minmax
    } else {
            null
    }

    var rowIndex = this.variables.size
    val lhs = this.constraints.indices.map { i ->
                this.constraints.lhs[i] + listOfNotNull(
                    slackVariables[i].first?.let {
                        LinearConstraintCell(
                            rowIndex = i,
                            colIndex = it.index,
                            coefficient = -Flt64.one,
                        )
                    }, slackVariables[i].second?.let {
                        LinearConstraintCell(
                            rowIndex = i,
                            colIndex = it.index,
                            coefficient = Flt64.one,
                        )
                    }
                )
            } + this.variables.indices.flatMap { j ->
                val jp = this.constraints.size + j
                val thisLhs = ArrayList<List<LinearConstraintCell>>()
                if (slackVariables[jp].first != null) {
                    thisLhs.add(
                        listOf(
                            LinearConstraintCell(
                                rowIndex = rowIndex,
                                colIndex = j,
                                coefficient = Flt64.one,
                            ),
                            LinearConstraintCell(
                                rowIndex = rowIndex,
                                colIndex = slackVariables[jp].first!!.index,
                                coefficient = Flt64.one,
                            )
                        )
                    )
                    rowIndex += 1
                }
                if (slackVariables[jp].second != null) {
                    thisLhs.add(
                        listOf(
                            LinearConstraintCell(
                                rowIndex = rowIndex,
                                colIndex = j,
                                coefficient = Flt64.one,
                            ),
                            LinearConstraintCell(
                                rowIndex = rowIndex,
                                colIndex = slackVariables[jp].second!!.index,
                                coefficient = -Flt64.one,
                            )
                        )
                    )
                    rowIndex += 1
                }
                thisLhs
            } + if (minSlackAmount != null) {
                val thisLhs = slackBinVariables.flatMapIndexed { j, (slack, bin) ->
                    listOf(
                        listOf(
                            LinearConstraintCell(
                                rowIndex = rowIndex + 2 * j,
                                colIndex = slack.index,
                                coefficient = Flt64.one,
                            ),
                            LinearConstraintCell(
                                rowIndex = rowIndex + 2 * j,
                                colIndex = bin.index,
                                coefficient = -minSlackAmount.second - Flt64.decimalPrecision
                            )
                        ),
                        listOf(
                            LinearConstraintCell(
                                rowIndex = rowIndex + 2 * j + 1,
                                colIndex = slack.index,
                                coefficient = Flt64.one
                            ),
                            LinearConstraintCell(
                                rowIndex = rowIndex + 2 * j + 1,
                                colIndex = bin.index,
                                coefficient = -Flt64.decimalPrecision.reciprocal()
                            )
                        )
                    )
                } + listOf(
                    slackBinVariables.map { (_, bin) ->
                        LinearConstraintCell(
                            rowIndex = rowIndex + 2 * slackBinVariables.size + 1,
                            colIndex = bin.index,
                            coefficient = Flt64.one
                        )
                    }
                )
                rowIndex += 2 * slackBinVariables.size + 1
                thisLhs
            } else {
                emptyList()
            } + if (minmaxSlack) {
                val thisLhs = ArrayList<List<LinearConstraintCell>>()
                for ((lbSlack, ubSlack) in slackVariables) {
                    if (lbSlack != null) {
                        thisLhs.add(
                            listOf(
                                LinearConstraintCell(
                                    rowIndex = rowIndex,
                                    colIndex = minmaxSlackVariable!!.index,
                                    coefficient = Flt64.one,
                                ),
                                LinearConstraintCell(
                                    rowIndex = rowIndex,
                                    colIndex = lbSlack.index,
                                    coefficient = -Flt64.one,
                                )
                            )
                        )
                        rowIndex += 1
                    }
                    if (ubSlack != null) {
                        thisLhs.add(
                            listOf(
                                LinearConstraintCell(
                                    rowIndex = rowIndex,
                                    colIndex = minmaxSlackVariable!!.index,
                                    coefficient = Flt64.one,
                                ),
                                LinearConstraintCell(
                                    rowIndex = rowIndex,
                                    colIndex = ubSlack.index,
                                    coefficient = -Flt64.one,
                                )
                            )
                        )
                        rowIndex += 1
                    }
                }
                thisLhs
            } else {
                emptyList()
            }
    val constraints = LinearConstraintBatch(
            sparseLhs = buildLinearSparseLhs(lhs),
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
            } + if (minSlackAmount != null) {
                slackBinVariables.flatMap { listOf(ConstraintRelation.GreaterEqual, ConstraintRelation.LessEqual) } + listOf(ConstraintRelation.GreaterEqual)
            } else {
                emptyList()
            } + if (minmaxSlack) {
                slackVariables.flatMap { (lbSlack, ubSlack) ->
                    listOfNotNull(
                        lbSlack?.let { ConstraintRelation.GreaterEqual },
                        ubSlack?.let { ConstraintRelation.GreaterEqual }
                    )
                }
            } else {
                emptyList()
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
            } + if (minSlackAmount != null) {
                slackBinVariables.flatMap { listOf(Flt64.zero, Flt64.zero) } + listOf(minSlackAmount.first.toFlt64())
            } else {
                emptyList()
            } + if (minmaxSlack) {
                slackVariables.flatMap { (lbSlack, ubSlack) ->
                    listOfNotNull(
                        lbSlack?.let { Flt64.zero },
                        ubSlack?.let { Flt64.zero }
                    )
                }
            } else {
                emptyList()
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
            } + if (minSlackAmount != null) {
                slackBinVariables.flatMap { (slack, _) -> listOf("${slack.name}_bin_lb", "${slack.name}_bin_ub") } + listOf("min_slack_amount")
            } else {
                emptyList()
            } + if (minmaxSlack) {
                slackVariables.flatMap { (lbSlack, ubSlack) ->
                    listOfNotNull(
                        lbSlack?.let { "${lbSlack.name}_minmax" },
                        ubSlack?.let { "${ubSlack.name}_minmax" }
                    )
                }
            } else {
                emptyList()
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
            } + if (minSlackAmount != null) {
                slackBinVariables.flatMap { listOf(ConstraintSource.ElasticSlackBinary, ConstraintSource.ElasticSlackBinary) } + listOf(ConstraintSource.ElasticSlackBinary)
            } else {
                emptyList()
            } + if (minmaxSlack) {
                val thisSources = ArrayList<ConstraintSource>()
                for ((lbSlack, ubSlack) in slackVariables) {
                    if (lbSlack != null) {
                        thisSources.add(ConstraintSource.ElasticSlackMinmax)
                    }
                    if (ubSlack != null) {
                        thisSources.add(ConstraintSource.ElasticSlackMinmax)
                    }
                }
                thisSources
            } else {
                emptyList()
            },
            origins = this.constraints.origins + this.variables.indices.flatMap { j ->
                val jp = this.constraints.size + j
                val thisOrigins = ArrayList<LinearConstraintImpl<Flt64>?>()
                if (slackVariables[jp].first != null) {
                    thisOrigins.add(null)
                }
                if (slackVariables[jp].second != null) {
                    thisOrigins.add(null)
                }
                thisOrigins
            } + if (minSlackAmount != null) {
                slackBinVariables.flatMap { listOf(null, null) } + listOf(null)
            } else {
                emptyList()
            } + if (minmaxSlack) {
                val thisOrigins = ArrayList<LinearConstraintImpl<Flt64>?>()
                for ((lbSlack, ubSlack) in slackVariables) {
                    if (lbSlack != null) {
                        thisOrigins.add(null)
                    }
                    if (ubSlack != null) {
                        thisOrigins.add(null)
                    }
                }
                thisOrigins
            } else {
                emptyList()
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
            } + if (minSlackAmount != null) {
                slackBinVariables.flatMap { listOf(null, null) } + listOf(null)
            } else {
                emptyList()
            } + if (minmaxSlack) {
                val thisOrigins = ArrayList<Pair<IntermediateSymbol<Flt64>, Boolean>?>()
                for ((lbSlack, ubSlack) in slackVariables) {
                    if (lbSlack != null) {
                        thisOrigins.add(null)
                    }
                    if (ubSlack != null) {
                        thisOrigins.add(null)
                    }
                }
                thisOrigins
            } else {
                emptyList()
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
            } + if (minSlackAmount != null) {
                slackBinVariables.flatMap { listOf(null, null) } + listOf(null)
            } else {
                emptyList()
            } + if (minmaxSlack) {
                val thisPriorities = ArrayList<Int?>()
                for ((lbSlack, ubSlack) in slackVariables) {
                    if (lbSlack != null) {
                        thisPriorities.add(null)
                    }
                    if (ubSlack != null) {
                        thisPriorities.add(null)
                    }
                }
                thisPriorities
            } else {
                emptyList()
            },
    )

    val objective = slackVariables.flatMap { (posSlack, negSlack) ->
            listOfNotNull(
                posSlack?.let {
                    LinearObjectiveCell(
                        colIndex = it.index,
                        coefficient = Flt64.one
                    )
                },
                negSlack?.let {
                    LinearObjectiveCell(
                        colIndex = it.index,
                        coefficient = Flt64.one
                    )
                }
            )
    } + if (minmaxSlack) {
            listOf(
                LinearObjectiveCell(
                    colIndex = minmaxSlackVariable!!.index,
                    coefficient = Flt64.one
                )
            )
    } else {
            emptyList()
    }

    return LinearTriadModel(
            impl = BasicLinearTriadModel(
                variables = this.variables.map {
                    it.copy().apply {
                        if (!free && !positiveNormalized && !negativeNormalized) {
                            _lowerBound = Flt64.negativeInfinity
                            _upperBound = Flt64.infinity
                        }
                    }
                } + slackVariables.flatMap { it.toList().filterNotNull() }.sortedBy { it.index } + if (minSlackAmount != null) {
                    slackBinVariables.map { it.second }
                } else {
                    emptyList()
                } + if (minmaxSlack) {
                    listOf(minmaxSlackVariable!!)
                } else {
                    emptyList()
                },
                constraints = constraints,
                name = "$name-elastic"
            ),
            tokensInSolver = tokensInSolver,
            objective = LinearObjective(ObjectCategory.Minimum, objective)
    )
}
