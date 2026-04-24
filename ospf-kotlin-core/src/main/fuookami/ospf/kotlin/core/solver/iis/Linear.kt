@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.core.solver.iis

import fuookami.ospf.kotlin.core.model.mechanism.geq
import fuookami.ospf.kotlin.core.model.mechanism.leq
import fuookami.ospf.kotlin.core.model.mechanism.eq
import fuookami.ospf.kotlin.core.solver.AbstractLinearSolver
import fuookami.ospf.kotlin.core.model.basic.ConstraintRelation
import fuookami.ospf.kotlin.core.model.basic.ConstraintSource
import fuookami.ospf.kotlin.core.model.basic.Variable
import fuookami.ospf.kotlin.core.model.intermediate.BasicLinearTriadModel
import fuookami.ospf.kotlin.core.model.intermediate.BasicLinearTriadModelView
import fuookami.ospf.kotlin.core.model.intermediate.LinearConstraintBatch
import fuookami.ospf.kotlin.core.model.intermediate.LinearTriadModelView
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import java.io.OutputStreamWriter
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

data class LinearIISModel(
    private val impl: BasicLinearTriadModel,
    val guardConstraints: LinearConstraintBatch?,
    val origin: LinearTriadModelView
) : BasicLinearTriadModelView {
    override val variables: List<Variable> by impl::variables
    override val constraints: LinearConstraintBatch by impl::constraints
    override val name: String by impl::name
    val relaxedFeasible: Boolean get() = guardConstraints == null

    override fun exportLP(writer: OutputStreamWriter): Try {
        return impl.exportLP(writer)
    }
}

@OptIn(ExperimentalTime::class)
suspend fun computeIIS(
    model: LinearTriadModelView,
    solver: AbstractLinearSolver,
    config: IISConfig
): Ret<LinearIISModel> {
    val startTime = Clock.System.now()
    val elasticModel = model.elastic(minSlackAmount = UInt64.two to config.slackTolerance)
    val boundAmount = UInt64(elasticModel.constraints.sources.count {
        it == ConstraintSource.ElasticLowerBound || it == ConstraintSource.ElasticUpperBound
    })
    val constraintAmount = UInt64(elasticModel.constraints.sources.count {
        it == ConstraintSource.Elastic
    })

    // todo: find impossible constraints

    when (val result = performElasticFiltering(
        elasticModel = elasticModel,
        solver = solver,
        config = config
    )) {
        is Ok -> {
            if (result.value.first) {
                when (val callbackResult = config.computingStatusCallBack?.invoke(
                    true,
                    Clock.System.now() - startTime,
                    IISComputingStatus(
                        restBoundAmount = UInt64(result.value.second.keys.count {
                            it.slack?.lowerBound != null || it.slack?.upperBound != null
                        }),
                        totalBoundAmount = boundAmount,
                        restConstraintAmount = UInt64(result.value.second.keys.count {
                            it.slack?.constraint != null
                        }),
                        totalConstraintAmount = constraintAmount,
                    )
                )) {
                    null -> {}
                    is Ok -> {}
                    is Failed -> {
                        return Failed(callbackResult.error)
                    }

                    is Fatal -> {
                        return Fatal(callbackResult.errors)
                    }
                }
                return Ok(dump(model, result.value.second))
            }
            result.value.second
        }

        is Failed -> {
            return Failed(result.error)
        }

        is Fatal -> {
            return Fatal(result.errors)
        }
    }

    config.computingStatusCallBack?.invoke(
        true,
        Clock.System.now() - startTime,
        IISComputingStatus(
            restBoundAmount = boundAmount,
            totalBoundAmount = boundAmount,
            restConstraintAmount = constraintAmount,
            totalConstraintAmount = constraintAmount,
        )
    )
    val (misConstraints, guardConstraints) = when (val result = performDeletionFiltering(
        elasticModel = elasticModel,
        solver = solver,
        startTime = startTime,
        boundAmount = boundAmount,
        constraintAmount = constraintAmount,
        config = config
    )) {
        is Ok -> {
            result.value
        }

        is Failed -> {
            return Failed(result.error)
        }

        is Fatal -> {
            return Fatal(result.errors)
        }
    }
    return Ok(dump(model, misConstraints, guardConstraints))
}

private fun getRelatedConstraints(
    model: LinearTriadModelView,
    slackVariables: Set<Variable>
): List<Int> {
    return slackVariables
        .mapNotNull { it.slack?.constraint }
        .let { originConstraints ->
            model.constraints.indices.filter { i ->
                model.constraints.origins[i]?.let { it in originConstraints } == true
            }
        }
}

private fun getRelatedVariables(
    model: LinearTriadModelView,
    filter: Set<Variable>,
    relatedConstraints: List<Int>
): List<Triple<Variable, Flt64?, Flt64?>> {
    return (filter.mapNotNull { variable ->
        if (variable.slack?.lowerBound != null) {
            Triple(variable.slack.lowerBound, true, false)
        } else if (variable.slack?.upperBound != null) {
            Triple(variable.slack.upperBound, false, true)
        } else {
            null
        }
    } + relatedConstraints.flatMap { i ->
        @Suppress("DEPRECATION")
        model.constraints.lhs[i].map { cell ->
            Triple(model.variables[cell.colIndex], false, false)
        }
    })
        .groupBy { it.first }
        .toList()
        .sortedBy { it.first.index }
        .map { (variable, bounds) ->
            val lowerBound = if (bounds.any { it.second }) {
                variable.lowerBound
            } else {
                null
            }
            val upperBound = if (bounds.any { it.third }) {
                variable.upperBound
            } else {
                null
            }
            Triple(variable, lowerBound, upperBound)
        }
}

private fun dump(
    model: LinearTriadModelView,
    elasticFilter: Map<Variable, Flt64>
): LinearIISModel {
    val relatedConstraints = getRelatedConstraints(
        model = model,
        slackVariables = elasticFilter.keys
    )
    val relatedVariables = getRelatedVariables(
        model = model,
        filter = elasticFilter.keys,
        relatedConstraints = relatedConstraints
    )

    return LinearIISModel(
        impl = BasicLinearTriadModel(
            variables = relatedVariables.map {
                it.first.copy().apply {
                    _lowerBound = it.second ?: Flt64.negativeInfinity
                    _upperBound = it.third ?: Flt64.infinity
                }
            },
            constraints = model.constraints.filter { row -> row in relatedConstraints },
            name = "${model.name}_iis"
        ),
        guardConstraints = null,
        origin = model
    )
}

private fun dump(
    model: LinearTriadModelView,
    misConstraints: Set<Variable>,
    guardConstraints: Set<Variable>
): LinearIISModel {
    val relatedMISConstraints = getRelatedConstraints(
        model = model,
        slackVariables = misConstraints
    )
    val relatedGuardConstraints = getRelatedConstraints(
        model = model,
        slackVariables = guardConstraints
    )
    val relatedConstraints = (relatedMISConstraints + relatedGuardConstraints).distinct().sorted()
    val relatedVariables = getRelatedVariables(
        model = model,
        filter = misConstraints + guardConstraints,
        relatedConstraints = relatedConstraints
    )

    return LinearIISModel(
        impl = BasicLinearTriadModel(
            variables = relatedVariables.map {
                it.first.copy().apply {
                    _lowerBound = it.second ?: Flt64.negativeInfinity
                    _upperBound = it.third ?: Flt64.infinity
                }
            },
            constraints = model.constraints.filter { row -> row in relatedMISConstraints },
            name = "${model.name}_iis"
        ),
        guardConstraints = model.constraints.filter { row -> row in relatedGuardConstraints },
        origin = model
    )
}

private suspend fun performElasticFiltering(
    elasticModel: LinearTriadModelView,
    solver: AbstractLinearSolver,
    config: IISConfig
): Ret<Pair<Boolean, Map<Variable, Flt64>>> {
    val relaxVariableBoundsResult = when (val result = relaxSpecificComponents(
        elasticModel = elasticModel,
        solver = solver,
        tolerance = config.slackTolerance
    ) { variable ->
        variable.slack?.lowerBound != null || variable.slack?.upperBound != null
    }) {
        is Ok -> {
            result.value
        }

        is Failed -> {
            return Failed(result.error)
        }

        is Fatal -> {
            return Fatal(result.errors)
        }
    }
    if (relaxVariableBoundsResult.first) {
        return Ok(true to relaxVariableBoundsResult.second)
    }

    val relaxInequalitiesAndVariableBoundsResult = when (val result = relaxSpecificComponents(
        elasticModel = elasticModel,
        solver = solver,
        tolerance = config.slackTolerance
    ) { variable ->
        when (variable.slack?.constraint?.sign) {
            ConstraintRelation.LessEqual, ConstraintRelation.GreaterEqual -> {
                true
            }

            else -> {
                false
            }
        } || variable.slack?.lowerBound != null || variable.slack?.upperBound != null
    }) {
        is Ok -> {
            result.value
        }

        is Failed -> {
            return Failed(result.error)
        }

        is Fatal -> {
            return Fatal(result.errors)
        }
    }
    if (relaxInequalitiesAndVariableBoundsResult.first) {
        return Ok(true to relaxInequalitiesAndVariableBoundsResult.second)
    }

    val relaxAllResult = when (val result = relaxSpecificComponents(
        elasticModel = elasticModel,
        solver = solver,
        tolerance = config.slackTolerance
    ) { variable -> variable.slack != null }) {
        is Ok -> {
            result.value
        }

        is Failed -> {
            return Failed(result.error)
        }

        is Fatal -> {
            return Fatal(result.errors)
        }
    }
    if (relaxAllResult.first) {
        return Ok(true to relaxAllResult.second)
    }

    return Ok(false to emptyMap())
}

@OptIn(ExperimentalTime::class)
private suspend fun performDeletionFiltering(
    elasticModel: LinearTriadModelView,
    solver: AbstractLinearSolver,
    startTime: Instant,
    boundAmount: UInt64,
    constraintAmount: UInt64,
    config: IISConfig
): Ret<Pair<Set<Variable>, Set<Variable>>> {
    val slackVariables = elasticModel.variables.filter { it.slack != null }.sortedBy { it.index }
    if (slackVariables.isEmpty()) {
        return Ok(emptySet<Variable>() to emptySet())
    }

    val activeRelaxedComponents = slackVariables.toMutableSet()
    for (candidate in slackVariables) {
        if (candidate !in activeRelaxedComponents) {
            continue
        }

        candidate._upperBound = Flt64.zero
        val feasible = when (val result = solver(elasticModel)) {
            is Ok -> {
                true
            }

            is Failed -> {
                if (result.error.code == ErrorCode.ORModelInfeasible || result.error.code == ErrorCode.ORModelInfeasibleOrUnbounded) {
                    false
                } else {
                    return Failed(result.error)
                }
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        if (feasible) {
            activeRelaxedComponents.remove(candidate)
        } else {
            candidate._upperBound = Flt64.infinity
        }

        when (val callbackResult = config.computingStatusCallBack?.invoke(
            true,
            Clock.System.now() - startTime,
            IISComputingStatus(
                restBoundAmount = UInt64(activeRelaxedComponents.count {
                    it.slack?.lowerBound != null || it.slack?.upperBound != null
                }),
                totalBoundAmount = boundAmount,
                restConstraintAmount = UInt64(activeRelaxedComponents.count {
                    it.slack?.constraint != null
                }),
                totalConstraintAmount = constraintAmount,
            )
        )) {
            null -> {}
            is Ok -> {}
            is Failed -> {
                return Failed(callbackResult.error)
            }

            is Fatal -> {
                return Fatal(callbackResult.errors)
            }
        }
    }

    return Ok(activeRelaxedComponents to emptySet())
}

private suspend fun relaxSpecificComponents(
    elasticModel: LinearTriadModelView,
    solver: AbstractLinearSolver,
    tolerance: Flt64,
    relaxCondition: (Variable) -> Boolean
): Ret<Pair<Boolean, Map<Variable, Flt64>>> {
    elasticModel.variables.forEach { variable ->
        if (variable.slack != null) {
            variable._upperBound = if (relaxCondition(variable)) {
                Flt64.infinity
            } else {
                Flt64.zero
            }
        }
    }

    val result = when (val result = solver(elasticModel)) {
        is Ok -> {
            result.value
        }

        is Failed -> {
            return if (result.error.code == ErrorCode.ORModelInfeasible || result.error.code == ErrorCode.ORModelInfeasibleOrUnbounded) {
                Ok(false to emptyMap())
            } else {
                Failed(result.error)
            }
        }

        is Fatal -> {
            return Fatal(result.errors)
        }
    }

    val relaxedComponents = elasticModel.variables.associateNotNull { variable ->
        if (variable.slack != null && result.solution[variable.index] geq tolerance) {
            variable to result.solution[variable.index]
        } else {
            null
        }
    }
    return Ok(true to relaxedComponents)
}



