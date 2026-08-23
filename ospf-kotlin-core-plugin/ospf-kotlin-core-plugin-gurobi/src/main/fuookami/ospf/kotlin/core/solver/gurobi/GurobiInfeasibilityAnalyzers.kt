@file:OptIn(kotlin.time.ExperimentalTime::class)

/** Gurobi 原生 IIS/Farkas 诊断适配器。 / Gurobi native IIS/Farkas diagnostic adapters. */
package fuookami.ospf.kotlin.core.solver.gurobi

import kotlin.math.abs
import kotlin.time.Clock
import fuookami.ospf.kotlin.core.model.intermediate.LinearTriadModelView
import fuookami.ospf.kotlin.core.model.intermediate.QuadraticTetradModelView
import fuookami.ospf.kotlin.core.solver.config.GurobiSolverConfig
import fuookami.ospf.kotlin.core.solver.config.SolverConfig
import fuookami.ospf.kotlin.core.solver.iis.FarkasInfeasibilityAnalyzer
import fuookami.ospf.kotlin.core.solver.iis.InfeasibilityAnalyzerCapabilities
import fuookami.ospf.kotlin.core.solver.iis.NativeIISAnalyzer
import fuookami.ospf.kotlin.core.solver.iis.IISConfig
import fuookami.ospf.kotlin.core.solver.report.BoundSide
import fuookami.ospf.kotlin.core.solver.report.ConstraintId
import fuookami.ospf.kotlin.core.solver.report.diagnosticConstraintId
import fuookami.ospf.kotlin.core.solver.report.diagnosticVariableId
import fuookami.ospf.kotlin.core.solver.report.EvidenceCompleteness
import fuookami.ospf.kotlin.core.solver.report.EvidenceExactness
import fuookami.ospf.kotlin.core.solver.report.EvidenceMinimality
import fuookami.ospf.kotlin.core.solver.report.EvidenceValidity
import fuookami.ospf.kotlin.core.solver.report.InfeasibilityEvidence
import fuookami.ospf.kotlin.core.solver.report.InfeasibilityEvidenceSource
import fuookami.ospf.kotlin.core.solver.report.InfeasibilityMember
import fuookami.ospf.kotlin.core.solver.report.SolverModelType
import fuookami.ospf.kotlin.core.solver.report.VariableBoundRef
import fuookami.ospf.kotlin.core.solver.value.toSolverDouble
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.ok
import gurobi.GRB
import gurobi.GRBConstr
import gurobi.GRBException
import gurobi.GRBLinExpr
import gurobi.GRBQConstr
import gurobi.GRBQuadExpr
import gurobi.GRBVar

private const val DIAGNOSTIC_TOLERANCE = 1.0e-9

/** Gurobi native IIS analyzer for linear models. / 面向线性模型的 Gurobi 原生 IIS 分析器。 */
internal class GurobiNativeIISAnalyzer(
    private val solverConfig: SolverConfig,
    private val iisConfig: IISConfig,
    private val callBack: GurobiLinearSolverCallBack?
) : NativeIISAnalyzer<LinearTriadModelView> {
    override val capabilities = InfeasibilityAnalyzerCapabilities(
        modelTypes = setOf(SolverModelType.LP, SolverModelType.MIP),
        exact = true,
        source = InfeasibilityEvidenceSource.NativeIIS
    )

    override suspend fun analyze(model: LinearTriadModelView): Ret<InfeasibilityEvidence> {
        return GurobiLinearDiagnosticRun(solverConfig, iisConfig, callBack).run(model, nativeIIS = true)
    }
}

/** Gurobi Farkas analyzer for continuous linear models. / 面向连续线性模型的 Gurobi Farkas 分析器。 */
internal class GurobiFarkasAnalyzer(
    private val solverConfig: SolverConfig,
    private val iisConfig: IISConfig,
    private val callBack: GurobiLinearSolverCallBack?
) : FarkasInfeasibilityAnalyzer<LinearTriadModelView> {
    override val capabilities = InfeasibilityAnalyzerCapabilities(
        modelTypes = setOf(SolverModelType.LP),
        exact = true,
        source = InfeasibilityEvidenceSource.Farkas
    )

    override suspend fun analyze(model: LinearTriadModelView): Ret<InfeasibilityEvidence> {
        if (model.variables.any { it.type.isIntegerType }) {
            return Failed(
                ErrorCode.IllegalArgument,
                "Gurobi Farkas 证书仅适用于连续线性模型 / Gurobi Farkas certificates require a continuous linear model"
            )
        }
        return GurobiLinearDiagnosticRun(solverConfig, iisConfig, callBack).run(model, nativeIIS = false)
    }
}

/** Gurobi native IIS analyzer for quadratic models. / 面向二次模型的 Gurobi 原生 IIS 分析器。 */
internal class GurobiQuadraticNativeIISAnalyzer(
    private val solverConfig: SolverConfig,
    private val iisConfig: IISConfig,
    private val callBack: GurobiQuadraticSolverCallBack?
) : NativeIISAnalyzer<QuadraticTetradModelView> {
    override val capabilities = InfeasibilityAnalyzerCapabilities(
        modelTypes = setOf(SolverModelType.QP, SolverModelType.QCP),
        exact = true,
        source = InfeasibilityEvidenceSource.NativeIIS
    )

    override suspend fun analyze(model: QuadraticTetradModelView): Ret<InfeasibilityEvidence> {
        return GurobiQuadraticDiagnosticRun(solverConfig, iisConfig, callBack).run(model)
    }
}

/** One isolated Gurobi diagnostic model. / 一次隔离的 Gurobi 诊断模型。 */
private class GurobiLinearDiagnosticRun(
    private val solverConfig: SolverConfig,
    private val iisConfig: IISConfig,
    private val callBack: GurobiLinearSolverCallBack?
) : GurobiSolver() {
    private lateinit var variables: List<GRBVar>
    private lateinit var constraints: List<GRBConstr>

    /** Run a linear Gurobi IIS or Farkas diagnostic. / 执行线性 Gurobi IIS 或 Farkas 诊断。
     *
     * @param model Linear model to analyze. / 待分析的线性模型。
     * @param nativeIIS Whether to request native IIS extraction. / 是否请求原生 IIS 提取。
     * @return Structured infeasibility evidence or an analysis error. / 结构化不可行证据或分析错误。
     */
    suspend fun run(model: LinearTriadModelView, nativeIIS: Boolean): Ret<InfeasibilityEvidence> {
        val started = Clock.System.now()
        val initialized = initialize(model.name)
        when (initialized) {
            is Failed -> return Failed(initialized.error)
            is Fatal -> return Fatal(initialized.errors)
            else -> {}
        }

        return try {
            when (val dumped = dump(model)) {
                is Failed -> return Failed(dumped.error)
                is Fatal -> return Fatal(dumped.errors)
                else -> {}
            }
            if (nativeIIS) {
                analyzeIIS(model, started)
            } else {
                analyzeFarkas(model, started)
            }
        } catch (error: GRBException) {
            Failed(
                ErrorCode.OREngineSolvingException,
                "Gurobi 原生不可行诊断失败：${error.message ?: "GRBException"} / " +
                    "Gurobi native infeasibility diagnosis failed: ${error.message ?: "GRBException"}"
            )
        } catch (error: Throwable) {
            Failed(
                ErrorCode.OREngineSolvingException,
                "Gurobi 原生不可行诊断失败：${error.message ?: error::class.simpleName} / " +
                    "Gurobi native infeasibility diagnosis failed: ${error.message ?: error::class.simpleName}"
            )
        } finally {
            close()
        }
    }

    private suspend fun initialize(name: String): Try {
        val gurobiConfig = solverConfig.backendConfiguration as? GurobiSolverConfig
        val server = gurobiConfig?.server
        val password = gurobiConfig?.password
        val connectionTime = gurobiConfig?.connectionTime
        return if (server != null && password != null && connectionTime != null) {
            init(
                server = server,
                password = password,
                connectionTime = connectionTime,
                name = name,
                callBack = callBack?.creatingEnvironmentFunction
            )
        } else {
            init(name = name, callBack = callBack?.creatingEnvironmentFunction)
        }
    }

    private fun dump(model: LinearTriadModelView): Try {
        variables = model.variables.mapIndexed { index, variable ->
            grbModel.addVar(
                variable.lowerBound.toSolverDouble("variable[$index].lowerBound"),
                variable.upperBound.toSolverDouble("variable[$index].upperBound"),
                0.0,
                GurobiVariable(variable.type).toGurobiVar(),
                "var-$index"
            )
        }
        constraints = model.constraints.indices.map { row ->
            val lhs = GRBLinExpr()
            model.constraints.sparseLhs.forEachEntry(row) { column, coefficient ->
                lhs.addTerm(
                    coefficient.toSolverDouble("diagnostic.linear.constraints.lhs[$row].coefficient[$column]"),
                    variables[column]
                )
            }
            grbModel.addConstr(
                lhs,
                GurobiConstraintSign(model.constraints.signs[row]).toGurobiConstraintSign(),
                model.constraints.rhs[row].toSolverDouble("diagnostic.linear.constraints.rhs[$row]"),
                "row-$row"
            )
        }
        grbModel.update()
        grbModel.set(GRB.DoubleParam.TimeLimit, iisConfig.time.toDouble(kotlin.time.DurationUnit.SECONDS))
        grbModel.set(GRB.IntParam.Threads, iisConfig.threadNum.toInt())
        grbModel.set(GRB.IntParam.DualReductions, 0)
        grbModel.set(GRB.IntParam.InfUnbdInfo, 1)
        return ok
    }

    private fun analyzeIIS(
        model: LinearTriadModelView,
        started: kotlin.time.Instant
    ): Ret<InfeasibilityEvidence> {
        grbModel.optimize()
        if (grbModel.get(GRB.IntAttr.Status) != GRB.INFEASIBLE) {
            return Failed(
                ErrorCode.ORModelInfeasible,
                "Gurobi 原生 IIS 要求模型状态为不可行 / Gurobi native IIS requires an infeasible model"
            )
        }
        grbModel.computeIIS()
        val iisMinimal = grbModel.get(GRB.IntAttr.IISMinimal) != 0
        val rowIds = linkedSetOf<ConstraintId>()
        val members = linkedSetOf<InfeasibilityMember>()
        constraints.forEachIndexed { index, constraint ->
            if (constraint.get(GRB.IntAttr.IISConstr) != 0) {
                val id = model.diagnosticConstraintId(index)
                rowIds += id
                members += InfeasibilityMember.Constraint(id)
            }
        }
        val bounds = selectedBounds(model)
        bounds.forEach { ref ->
            members += InfeasibilityMember.VariableBound(ref)
        }
        return ok(
            InfeasibilityEvidence(
                source = InfeasibilityEvidenceSource.NativeIIS,
                exactness = if (iisMinimal) EvidenceExactness.Irreducible else EvidenceExactness.Heuristic,
                completeness = EvidenceCompleteness.Complete,
                constraintIds = rowIds,
                variableBoundIds = bounds.mapTo(linkedSetOf()) { it.variableId },
                elapsed = Clock.System.now() - started,
                validity = EvidenceValidity.Verified,
                minimality = if (iisMinimal) EvidenceMinimality.Irreducible else EvidenceMinimality.Partial,
                variableBoundRefs = bounds,
                members = members,
                reference = "gurobi.computeIIS"
            )
        )
    }

    private fun analyzeFarkas(
        model: LinearTriadModelView,
        started: kotlin.time.Instant
    ): Ret<InfeasibilityEvidence> {
        grbModel.optimize()
        if (grbModel.get(GRB.IntAttr.Status) != GRB.INFEASIBLE) {
            return Failed(
                ErrorCode.ORModelInfeasible,
                "Gurobi Farkas 要求模型状态为不可行 / Gurobi Farkas requires an infeasible model"
            )
        }
        if (grbModel.get(GRB.DoubleAttr.FarkasProof) <= DIAGNOSTIC_TOLERANCE) {
            return Failed(
                ErrorCode.OREngineSolvingException,
                "Gurobi 未返回有效 FarkasProof / Gurobi did not return a valid FarkasProof"
            )
        }
        val rowIds = linkedSetOf<ConstraintId>()
        val members = linkedSetOf<InfeasibilityMember>()
        constraints.forEachIndexed { index, constraint ->
            if (abs(constraint.get(GRB.DoubleAttr.FarkasDual)) > DIAGNOSTIC_TOLERANCE) {
                val id = model.diagnosticConstraintId(index)
                rowIds += id
                members += InfeasibilityMember.Constraint(id)
            }
        }
        val boundsResult = selectedFarkasBounds(model)
        if (boundsResult.failed) {
            return when (boundsResult) {
                is Failed -> Failed(boundsResult.error)
                is Fatal -> Fatal(boundsResult.errors)
                else -> Failed(
                    ErrorCode.OREngineSolvingException,
                    "Gurobi Farkas 变量界提取返回了无效状态 / Gurobi Farkas bound extraction returned an invalid state"
                )
            }
        }
        val bounds = boundsResult.value!!
        bounds.forEach { ref ->
            members += InfeasibilityMember.VariableBound(ref)
        }
        if (members.isEmpty()) {
            return Failed(
                ErrorCode.OREngineSolvingException,
                "Gurobi 未返回非零 Farkas 成分 / Gurobi returned no nonzero Farkas component"
            )
        }
        return ok(
            InfeasibilityEvidence(
                source = InfeasibilityEvidenceSource.Farkas,
                exactness = EvidenceExactness.Exact,
                completeness = EvidenceCompleteness.Complete,
                constraintIds = rowIds,
                variableBoundIds = bounds.mapTo(linkedSetOf()) { it.variableId },
                elapsed = Clock.System.now() - started,
                validity = EvidenceValidity.Verified,
                minimality = EvidenceMinimality.NotChecked,
                variableBoundRefs = bounds,
                members = members,
                reference = "gurobi.farkas"
            )
        )
    }

    private fun selectedBounds(model: LinearTriadModelView): Set<VariableBoundRef> {
        return variables.indices.flatMapTo(linkedSetOf()) { index ->
            val variable = variables[index]
            buildList {
                if (variable.get(GRB.IntAttr.IISLB) != 0) {
                    add(VariableBoundRef(model.variables[index].diagnosticVariableId(), BoundSide.Lower))
                }
                if (variable.get(GRB.IntAttr.IISUB) != 0) {
                    add(VariableBoundRef(model.variables[index].diagnosticVariableId(), BoundSide.Upper))
                }
            }
        }
    }

    private fun selectedFarkasBounds(model: LinearTriadModelView): Ret<Set<VariableBoundRef>> {
        val aggregatedCoefficients = DoubleArray(model.variables.size)
        constraints.forEachIndexed { row, constraint ->
            val multiplier = constraint.get(GRB.DoubleAttr.FarkasDual)
            model.constraints.sparseLhs.forEachEntry(row) { column, coefficient ->
                aggregatedCoefficients[column] += multiplier * coefficient.toSolverDouble(
                    "diagnostic.linear.constraints.lhs[$row].coefficient[$column]"
                )
            }
        }
        val bounds = linkedSetOf<VariableBoundRef>()
        aggregatedCoefficients.forEachIndexed { index, coefficient ->
            val variable = model.variables[index]
            when {
                coefficient > DIAGNOSTIC_TOLERANCE -> {
                    if (variable.lowerBound == fuookami.ospf.kotlin.math.algebra.number.Flt64.negativeInfinity) {
                        return Failed(
                            ErrorCode.OREngineSolvingException,
                            "Gurobi Farkas 证书需要有限下界但模型变量无下界 / " +
                                "Gurobi Farkas certificate requires a finite lower bound"
                        )
                    }
                    bounds += VariableBoundRef(variable.diagnosticVariableId(), BoundSide.Lower)
                }

                coefficient < -DIAGNOSTIC_TOLERANCE -> {
                    if (variable.upperBound == fuookami.ospf.kotlin.math.algebra.number.Flt64.infinity) {
                        return Failed(
                            ErrorCode.OREngineSolvingException,
                            "Gurobi Farkas 证书需要有限上界但模型变量无上界 / " +
                                "Gurobi Farkas certificate requires a finite upper bound"
                        )
                    }
                    bounds += VariableBoundRef(variable.diagnosticVariableId(), BoundSide.Upper)
                }
            }
        }
        return ok(bounds)
    }
}

/** One isolated Gurobi quadratic diagnostic model. / 一次隔离的 Gurobi 二次诊断模型。 */
private class GurobiQuadraticDiagnosticRun(
    private val solverConfig: SolverConfig,
    private val iisConfig: IISConfig,
    private val callBack: GurobiQuadraticSolverCallBack?
) : GurobiSolver() {
    private lateinit var variables: List<GRBVar>
    private lateinit var constraints: List<GRBQConstr>

    /** Run a quadratic Gurobi native IIS diagnostic. / 执行二次 Gurobi 原生 IIS 诊断。
     *
     * @param model Quadratic model to analyze. / 待分析的二次模型。
     * @return Structured infeasibility evidence or an analysis error. / 结构化不可行证据或分析错误。
     */
    suspend fun run(model: QuadraticTetradModelView): Ret<InfeasibilityEvidence> {
        val started = Clock.System.now()
        val initialized = initialize(model.name)
        when (initialized) {
            is Failed -> return Failed(initialized.error)
            is Fatal -> return Fatal(initialized.errors)
            else -> {}
        }

        return try {
            when (val dumped = dump(model)) {
                is Failed -> return Failed(dumped.error)
                is Fatal -> return Fatal(dumped.errors)
                else -> {}
            }
            grbModel.optimize()
            if (grbModel.get(GRB.IntAttr.Status) != GRB.INFEASIBLE) {
                return Failed(
                    ErrorCode.ORModelInfeasible,
                    "Gurobi 原生 IIS 要求模型状态为不可行 / Gurobi native IIS requires an infeasible model"
                )
            }
            grbModel.computeIIS()
            val iisMinimal = grbModel.get(GRB.IntAttr.IISMinimal) != 0
            val rowIds = linkedSetOf<ConstraintId>()
            val members = linkedSetOf<InfeasibilityMember>()
            constraints.forEachIndexed { index, constraint ->
                if (constraint.get(GRB.IntAttr.IISQConstr) != 0) {
                    val id = model.diagnosticConstraintId(index)
                    rowIds += id
                    members += InfeasibilityMember.Constraint(id)
                }
            }
            val bounds = selectedBounds(model)
            bounds.forEach { members += InfeasibilityMember.VariableBound(it) }
            ok(
                InfeasibilityEvidence(
                    source = InfeasibilityEvidenceSource.NativeIIS,
                    exactness = if (iisMinimal) EvidenceExactness.Irreducible else EvidenceExactness.Heuristic,
                    completeness = EvidenceCompleteness.Complete,
                    constraintIds = rowIds,
                    variableBoundIds = bounds.mapTo(linkedSetOf()) { it.variableId },
                    elapsed = Clock.System.now() - started,
                    validity = EvidenceValidity.Verified,
                    minimality = if (iisMinimal) EvidenceMinimality.Irreducible else EvidenceMinimality.Partial,
                    variableBoundRefs = bounds,
                    members = members,
                    reference = "gurobi.computeIIS"
                )
            )
        } catch (error: GRBException) {
            Failed(
                ErrorCode.OREngineSolvingException,
                "Gurobi 二次原生 IIS 诊断失败：${error.message ?: "GRBException"} / " +
                    "Gurobi quadratic native IIS diagnosis failed: ${error.message ?: "GRBException"}"
            )
        } catch (error: Throwable) {
            Failed(
                ErrorCode.OREngineSolvingException,
                "Gurobi 二次原生 IIS 诊断失败：${error.message ?: error::class.simpleName} / " +
                    "Gurobi quadratic native IIS diagnosis failed: ${error.message ?: error::class.simpleName}"
            )
        } finally {
            close()
        }
    }

    private suspend fun initialize(name: String): Try {
        val gurobiConfig = solverConfig.backendConfiguration as? GurobiSolverConfig
        val server = gurobiConfig?.server
        val password = gurobiConfig?.password
        val connectionTime = gurobiConfig?.connectionTime
        return if (server != null && password != null && connectionTime != null) {
            init(
                server = server,
                password = password,
                connectionTime = connectionTime,
                name = name,
                callBack = callBack?.creatingEnvironmentFunction
            )
        } else {
            init(name = name, callBack = callBack?.creatingEnvironmentFunction)
        }
    }

    private fun dump(model: QuadraticTetradModelView): Try {
        variables = model.variables.mapIndexed { index, variable ->
            grbModel.addVar(
                variable.lowerBound.toSolverDouble("variable[$index].lowerBound"),
                variable.upperBound.toSolverDouble("variable[$index].upperBound"),
                0.0,
                GurobiVariable(variable.type).toGurobiVar(),
                "var-$index"
            )
        }
        constraints = model.constraints.indices.map { row ->
            val lhs = GRBQuadExpr()
            model.constraints.sparseLhs.forEachEntry(row) { column1, column2, coefficient ->
                if (column2 == null) {
                    lhs.addTerm(coefficient.toSolverDouble("quadratic.constraints.lhs[$row].coefficient"), variables[column1])
                } else {
                    lhs.addTerm(
                        coefficient.toSolverDouble("quadratic.constraints.lhs[$row].coefficient"),
                        variables[column1],
                        variables[column2]
                    )
                }
            }
            grbModel.addQConstr(
                lhs,
                GurobiConstraintSign(model.constraints.signs[row]).toGurobiConstraintSign(),
                model.constraints.rhs[row].toSolverDouble("quadratic.constraints.rhs[$row]"),
                "row-$row"
            )
        }
        grbModel.update()
        grbModel.set(GRB.DoubleParam.TimeLimit, iisConfig.time.toDouble(kotlin.time.DurationUnit.SECONDS))
        grbModel.set(GRB.IntParam.Threads, iisConfig.threadNum.toInt())
        grbModel.set(GRB.IntParam.DualReductions, 0)
        return ok
    }

    private fun selectedBounds(model: QuadraticTetradModelView): Set<VariableBoundRef> {
        return variables.indices.flatMapTo(linkedSetOf()) { index ->
            val variable = variables[index]
            buildList {
                if (variable.get(GRB.IntAttr.IISLB) != 0) {
                    add(VariableBoundRef(model.variables[index].diagnosticVariableId(), BoundSide.Lower))
                }
                if (variable.get(GRB.IntAttr.IISUB) != 0) {
                    add(VariableBoundRef(model.variables[index].diagnosticVariableId(), BoundSide.Upper))
                }
            }
        }
    }
}
