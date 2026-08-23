/**
 * 二次四元模型弹性构建器 / Quadratic tetrad model elastic builder
*/
package fuookami.ospf.kotlin.core.model.intermediate

import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.QuadraticConstraintImpl
import fuookami.ospf.kotlin.core.solver.report.ConstraintId
import fuookami.ospf.kotlin.core.solver.report.ObjectiveId
import fuookami.ospf.kotlin.core.symbol.IntermediateSymbol
import fuookami.ospf.kotlin.core.variable.Continuous
import fuookami.ospf.kotlin.math.algebra.number.Flt64

/**
 * 构建弹性模型 / Build elastic model
 *
 * 为二次四元模型添加松弛变量，使其成为弹性模型。 / Adds slack variables to the quadratic tetrad model to make it an elastic model.
 *
 * @return 弹性模型 / Elastic model
*/
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

    val derivedSlackVariables = slackVariables.mapIndexed { index, pair ->
        val sourceConstraint = index < this.constraints.size
        val sourceId = if (sourceConstraint) {
            this.constraints.ids.getOrNull(index)?.value
        } else {
            this.variables.getOrNull(index - this.constraints.size)?.id?.value
        }
        val sourceScope = if (sourceConstraint) {
            this.constraints.identityScopeAt(index)
        } else {
            this.variables[index - this.constraints.size].identityScope
        }
        val sourceOrigin = if (sourceConstraint) {
            this.constraints.identityOriginAt(index)
        } else {
            this.variables[index - this.constraints.size].identityOrigin
        }
        val sourceProvenance = if (sourceConstraint) {
            this.constraints.identityProvenanceOrOriginAt(index)
        } else {
            this.variables[index - this.constraints.size].identityProvenanceOrOrigin()
        }
        val namespace = if (sourceConstraint) {
            this.constraints.identityNamespace
        } else {
            this.variables[index - this.constraints.size].identityNamespace ?: this.constraints.identityNamespace
        }
        val schemaVersion = if (sourceConstraint) {
            this.constraints.identitySchemaVersion
        } else {
            this.variables[index - this.constraints.size].identitySchemaVersion ?: this.constraints.identitySchemaVersion
        }
        pair.first?.withDerivedIdentity(
            role = if (sourceConstraint) "elastic-constraint-slack" else "elastic-bound-slack",
            sourceId = sourceId,
            sourceScope = sourceScope,
            sourceOrigin = sourceOrigin,
            sourceProvenance = sourceProvenance,
            namespace = namespace,
            schemaVersion = schemaVersion,
            discriminator = derivedDiscriminator(sourceId, "lower", "$index:lower")
        ) to pair.second?.withDerivedIdentity(
            role = if (sourceConstraint) "elastic-constraint-slack" else "elastic-bound-slack",
            sourceId = sourceId,
            sourceScope = sourceScope,
            sourceOrigin = sourceOrigin,
            sourceProvenance = sourceProvenance,
            namespace = namespace,
            schemaVersion = schemaVersion,
            discriminator = derivedDiscriminator(sourceId, "upper", "$index:upper")
        )
    }
    val derivedSlackList = derivedSlackVariables.flatMap { it.toList().filterNotNull() }
    val constraintIdentities = ArrayList<DerivedIdentityMetadata>()
    this.constraints.indices.forEach { index ->
        val sourceId = this.constraints.ids.getOrNull(index)?.value
        constraintIdentities += derivedConstraintIdentity(
            role = "elastic-constraint",
            sourceId = sourceId,
            sourceScope = this.constraints.identityScopeAt(index),
            sourceOrigin = this.constraints.identityOriginAt(index),
            sourceProvenance = this.constraints.identityProvenanceOrOriginAt(index),
            namespace = this.constraints.identityNamespace,
            schemaVersion = this.constraints.identitySchemaVersion,
            discriminator = derivedDiscriminator(sourceId, "0", index.toString())
        )
    }
    this.variables.indices.forEach { index ->
        val pair = derivedSlackVariables[this.constraints.size + index]
        val source = this.variables[index]
        val sourceId = source.id?.value
        pair.first?.let {
            constraintIdentities += derivedConstraintIdentity(
                role = "elastic-lower-bound",
                sourceId = source.id?.value,
                sourceScope = source.identityScope,
                sourceOrigin = source.identityOrigin,
                sourceProvenance = source.identityProvenanceOrOrigin(),
                namespace = source.identityNamespace ?: this.constraints.identityNamespace,
                schemaVersion = source.identitySchemaVersion ?: this.constraints.identitySchemaVersion,
                discriminator = derivedDiscriminator(sourceId, "lower", "$index:lower")
            )
        }
        pair.second?.let {
            constraintIdentities += derivedConstraintIdentity(
                role = "elastic-upper-bound",
                sourceId = source.id?.value,
                sourceScope = source.identityScope,
                sourceOrigin = source.identityOrigin,
                sourceProvenance = source.identityProvenanceOrOrigin(),
                namespace = source.identityNamespace ?: this.constraints.identityNamespace,
                schemaVersion = source.identitySchemaVersion ?: this.constraints.identitySchemaVersion,
                discriminator = derivedDiscriminator(sourceId, "upper", "$index:upper")
            )
        }
    }
    val objectiveIdentity = derivedObjectiveIdentity(
        role = "elastic-objective",
        sourceId = this.objective.id?.value,
        sourceScope = this.objective.identityScope,
        sourceOrigin = this.objective.identityOrigin,
        sourceProvenance = (
            this.objective.identityProvenanceOrOrigin() +
                this.constraints.indices.flatMap { this.constraints.identityProvenanceOrOriginAt(it) } +
                this.variables.flatMap { it.identityProvenanceOrOrigin() }
            ).distinct(),
        namespace = this.objective.identityNamespace ?: this.constraints.identityNamespace,
        schemaVersion = this.objective.identitySchemaVersion ?: this.constraints.identitySchemaVersion
    )

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
            },
            ids = constraintIdentities.map { ConstraintId(it.id.value) },
            identityNamespace = this.constraints.identityNamespace,
            identitySchemaVersion = this.constraints.identitySchemaVersion,
            identityScopes = constraintIdentities.map { it.scope },
            identityOrigins = constraintIdentities.map { it.origin },
            identityProvenance = constraintIdentities.map { it.provenance }
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
                } + derivedSlackVariables.flatMap { it.toList().filterNotNull() }.sortedBy { it.index },
                constraints = constraints,
                name = "$name-elastic"
            ),
            tokensInSolver = tokensInSolver,
            objective = QuadraticObjective(
                category = ObjectCategory.Minimum,
                objective = objective,
                id = ObjectiveId(objectiveIdentity.id.value),
                identityScope = objectiveIdentity.scope,
                identityOrigin = objectiveIdentity.origin,
                identityNamespace = this.objective.identityNamespace ?: this.constraints.identityNamespace,
                identitySchemaVersion = this.objective.identitySchemaVersion ?: this.constraints.identitySchemaVersion,
                identityProvenance = objectiveIdentity.provenance
            )
    ).withDerivedIdentityValidation()
}
