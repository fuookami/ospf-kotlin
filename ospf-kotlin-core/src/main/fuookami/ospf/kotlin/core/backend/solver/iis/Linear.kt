package fuookami.ospf.kotlin.core.backend.solver.iis

import java.io.*
import kotlinx.datetime.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.core.backend.solver.*
import fuookami.ospf.kotlin.core.backend.intermediate_model.*
import fuookami.ospf.kotlin.core.backend.intermediate_model.LinearConstraint

data class LinearIISModel(
    private val impl: BasicLinearTriadModel,
    val guardConstraints: LinearConstraint?,
    val origin: LinearTriadModelView
) : BasicLinearTriadModelView {
    override val variables: List<Variable> by impl::variables
    override val constraints: LinearConstraint by impl::constraints
    override val name: String by impl::name
    val relaxedFeasible: Boolean get() = guardConstraints == null

    override fun exportLP(writer: OutputStreamWriter): Try {
        return impl.exportLP(writer)
    }
}

suspend fun computeIIS(
    model: LinearTriadModelView,
    solver: AbstractLinearSolver,
    config: IISConfig
): Ret<LinearIISModel> {
    val startTime = Clock.System.now()
    val elasticModel = model.elastic()
    val boundAmount = UInt64(elasticModel.constraints.sources.count { it == ConstraintSource.ElasticLowerBound || it == ConstraintSource.ElasticUpperBound })
    val constraintAmount = UInt64(elasticModel.constraints.sources.count { it == ConstraintSource.Elastic })

    when (val result = performElasticFiltering(
        elasticModel = elasticModel,
        solver = solver,
        config = config
    )) {
        is Ok -> {
            if (result.value.first) {
                when (val result = config.computingStatusCallBack?.invoke(
                    true,
                    Clock.System.now() - startTime,
                    IISComputingStatus(
                        restBoundAmount = UInt64(result.value.second.keys.count { it.slack?.lowerBound != null || it.slack?.upperBound != null }),
                        totalBoundAmount = boundAmount,
                        restConstraintAmount = UInt64(result.value.second.keys.count { it.slack?.constraint != null }),
                        totalConstraintAmount = constraintAmount,
                    )
                )) {
                    is Failed -> {
                        return Failed(result.error)
                    }

                    else -> {}
                }
                return Ok(dump(model, result.value.second))
            }
            result.value.second
        }

        is Failed -> {
            return Failed(result.error)
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

private fun dump(
    model: LinearTriadModelView,
    elasticFilter: Map<Variable, Flt64>
): LinearIISModel {
    val relatedConstraints = getRelatedConstraints(model, elasticFilter.keys)
    val relatedVariables = (elasticFilter.keys.mapNotNull { it.slack?.lowerBound ?: it.slack?.upperBound }
        + relatedConstraints.flatMap { i -> model.constraints.lhs[i].map { cell -> model.variables[cell.colIndex] } }
    ).distinct().sortedBy { it.index }

    return LinearIISModel(
        impl = BasicLinearTriadModel(
            variables = relatedVariables,
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
    val relatedMISConstraints = getRelatedConstraints(model, misConstraints)
    val relatedGuardConstraints = getRelatedConstraints(model, guardConstraints)
    val relatedConstraints = (relatedMISConstraints + relatedGuardConstraints).distinct().sorted()
    val relatedVariables = ((misConstraints + guardConstraints).mapNotNull { it.slack?.lowerBound ?: it.slack?.upperBound }
        + relatedConstraints.flatMap { i -> model.constraints.lhs[i].map { cell -> model.variables[cell.colIndex] } }
    ).distinct().sortedBy { it.index }

    return LinearIISModel(
        impl = BasicLinearTriadModel(
            variables = relatedVariables,
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
            Sign.LessEqual, Sign.GreaterEqual -> {
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
    }
    if (relaxInequalitiesAndVariableBoundsResult.first) {
        return Ok(true to relaxVariableBoundsResult.second)
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
    }
    if (relaxAllResult.first) {
        return Ok(true to relaxAllResult.second)
    }

    return Ok(false to emptyMap())
}

private suspend fun performDeletionFiltering(
    elasticModel: LinearTriadModelView,
    solver: AbstractLinearSolver,
    startTime: Instant,
    boundAmount: UInt64,
    constraintAmount: UInt64,
    config: IISConfig
): Ret<Pair<Set<Variable>, Set<Variable>>> {
    TODO("not implemented yet")
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
                Flt64.decimalPrecision.reciprocal()
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
    }

    val relaxedComponents = elasticModel.variables.associateNotNull { variable ->
        if (variable.slack != null && result.solution[variable.index] gr tolerance) {
            variable to result.solution[variable.index]
        } else {
            null
        }
    }
    return Ok(true to relaxedComponents)
}
