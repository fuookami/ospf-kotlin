/**
 * 连续半径模型组件，统一管理 native 和 PWL 两条路径的变量创建、注册与结果提取。
 * Continuous radius model component that unifies variable creation, registration,
 * and result extraction for both native and PWL paths.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.item.model

import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.function.LinearFunctionSymbolAdapter
import fuookami.ospf.kotlin.core.symbol.function.UnivariateLinearPiecewiseFunction
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.ConservativeRadiusEnvelope
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.InfraNumber
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PWLRadiusApproximationConfig
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PWLRadiusSquaredApproximation
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.infraScalar
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.utils.functional.Try

/**
 * 连续半径 solver 变量（持有真实 solver 变量和对应的原型）。
 * Continuous-radius solver variable (holds a real solver variable and its prototype).
 *
 * @property prototype 连续半径 solver 变量原型 / continuous-radius solver variable prototype
 * @property variable 真实 solver 连续变量 / real solver continuous variable
 */
data class ContinuousRadiusSolverVariable(
    val prototype: ContinuousCylinderRadiusSolverPrototype,
    val variable: RealVar
)

/**
 * PWL 连续半径 solver 变量（持有 core UnivariateLinearPiecewiseFunction、PWL 近似函数和保守 envelope）。
 * PWL continuous-radius solver variable (holds a core UnivariateLinearPiecewiseFunction, PWL approximation, and conservative envelope).
 *
 * 使用 core 提供的 [UnivariateLinearPiecewiseFunction] 实现分段线性近似 q ≈ r²：
 * - r: solver 连续变量，代表半径
 * - q = f(r): UnivariateLinearPiecewiseFunction 自动创建结果变量、二值选择变量和 Big-M 约束
 * - envelope: 使用 rMax 保守建模所有几何计算
 *
 * @property prototype 连续半径 solver 变量原型 / continuous-radius solver variable prototype
 * @property radiusVariable 半径连续变量 r / radius continuous variable r
 * @property pwlFunction core 单变量分段线性函数 q ≈ r² / core univariate piecewise linear function q ≈ r²
 * @property pwlApproximation PWL 近似函数（断点/误差）/ PWL approximation function (breakpoints/errors)
 * @property envelope 保守半径 envelope / conservative radius envelope
 * @property config PWL 配置 / PWL config
 */
data class PWLContinuousRadiusSolverVariable(
    val prototype: ContinuousCylinderRadiusSolverPrototype,
    val radiusVariable: RealVar,
    val pwlFunction: UnivariateLinearPiecewiseFunction<InfraNumber>,
    val pwlApproximation: PWLRadiusSquaredApproximation,
    val envelope: ConservativeRadiusEnvelope,
    val config: PWLRadiusApproximationConfig
) {
    /** 变量名 / variable name */
    val variableName: String get() = prototype.variableName

    /** PWL 结果变量 q ≈ r² / PWL result variable q ≈ r² */
    val radiusSquaredVariable get() = pwlFunction.resultVar

    /**
     * 生成 PWL 约束描述（用于诊断和调试）。
     * Generate PWL constraint descriptions (for diagnostics and debugging).
     */
    fun constraintDescriptions(): List<String> {
        val descriptions = ArrayList<String>()
        descriptions.add("PWL($variableName): q = f(r) via UnivariateLinearPiecewiseFunction, r in [${envelope.rMin.toDouble()}, ${envelope.rMax.toDouble()}]")
        descriptions.add("PWL($variableName): numSegments=${pwlApproximation.numSegments}, Big-M + binary selector")
        if (config.enableDebugInfo) {
            descriptions.add("PWL($variableName): maxRelError = ${pwlApproximation.maxRelativeError.toDouble()}")
            descriptions.add("PWL($variableName): maxAbsError = ${pwlApproximation.maxAbsoluteError.toDouble()}")
            for (i in 0 until pwlApproximation.numSegments) {
                descriptions.add("PWL($variableName): seg[$i] slope=${pwlApproximation.slopes[i].toDouble()}, intercept=${pwlApproximation.intercepts[i].toDouble()}")
            }
        }
        return descriptions
    }
}

/**
 * 从原型列表创建 solver 变量列表（native path）。
 * Create solver variable list from prototype list (native path).
 *
 * @param prototypes 连续半径 solver 变量原型 / continuous-radius solver variable prototypes
 * @return solver 变量列表 / solver variable list
 */
fun continuousRadiusSolverVariables(
    prototypes: List<ContinuousCylinderRadiusSolverPrototype>
): List<ContinuousRadiusSolverVariable> {
    return prototypes
        .filter { it.isSolverRegisterable }
        .map { prototype ->
            ContinuousRadiusSolverVariable(
                prototype = prototype,
                variable = RealVar(prototype.variableName)
            )
        }
}

/**
 * 从原型列表创建 PWL solver 变量列表。
 * Create PWL solver variable list from prototype list.
 *
 * 使用 core [UnivariateLinearPiecewiseFunction] 实现分段线性近似 q ≈ r²。
 * 仅处理 isPWLRegisterable 且非 isSolverRegisterable 的原型。
 *
 * @param prototypes 连续半径 solver 变量原型 / continuous-radius solver variable prototypes
 * @param config PWL 配置 / PWL config
 * @return PWL solver 变量列表 / PWL solver variable list
 */
fun pwlContinuousRadiusSolverVariables(
    prototypes: List<ContinuousCylinderRadiusSolverPrototype>,
    config: PWLRadiusApproximationConfig = PWLRadiusApproximationConfig()
): List<PWLContinuousRadiusSolverVariable> {
    return prototypes
        .filter { it.isPWLRegisterable && !it.isSolverRegisterable }
        .mapNotNull { prototype ->
            val rMin = prototype.radiusLowerBound ?: return@mapNotNull null
            val rMax = prototype.radiusUpperBound ?: return@mapNotNull null
            val rMinValue = rMin.value
            val rMaxValue = rMax.value

            val pwlApproximation = PWLRadiusSquaredApproximation.fromRadiusInterval(
                rMin = rMinValue,
                rMax = rMaxValue,
                config = config
            )

            val envelope = ConservativeRadiusEnvelope(
                rMin = rMinValue,
                rMax = rMaxValue
            )

            val variableName = prototype.variableName
            val radiusVariable = RealVar("${variableName}_r")

            // Build core UnivariateLinearPiecewiseFunction: q ≈ r²
            val x = LinearPolynomial(
                listOf(LinearMonomial(infraScalar(1.0), radiusVariable)),
                infraScalar(0.0)
            )
            val pwlFunction = UnivariateLinearPiecewiseFunction(
                x = x,
                breakpoints = pwlApproximation.breakpoints,
                slopes = pwlApproximation.slopes,
                intercepts = pwlApproximation.intercepts,
                converter = IntoValue.fromConverter(FltX),
                name = "${variableName}_pwl_r_squared"
            )

            PWLContinuousRadiusSolverVariable(
                prototype = prototype,
                radiusVariable = radiusVariable,
                pwlFunction = pwlFunction,
                pwlApproximation = pwlApproximation,
                envelope = envelope,
                config = config
            )
        }
}

/**
 * 连续半径 solver 变量注册计划。
 * Continuous-radius solver variable registration plan.
 *
 * @property variableNames 变量名列表 / variable names
 * @property boundDescriptions 变量上下界描述 / variable bound descriptions
 * @property selectedRadiusDescriptions 已选择半径描述 / selected-radius descriptions
 * @property gapDescriptions 缺口描述 / gap descriptions
 * @property productionReadyVariables 已具备生产回写闭环的变量 / variables with production selection closure
 * @property modelRegistrationBlockedVariables 暂未注册进 solver model 的变量 / variables not yet registered into solver model
 * @property registeredVariables 已注册进 solver model 的变量（native path）/ variables registered into solver model (native path)
 * @property pwlRegisteredVariables 已通过 PWL 路径注册进 solver model 的变量 / variables registered via PWL path
 */
data class ContinuousRadiusSolverVariableRegistrationPlan(
    val variableNames: List<String>,
    val boundDescriptions: List<String>,
    val selectedRadiusDescriptions: List<String>,
    val gapDescriptions: List<String>,
    val productionReadyVariables: List<String>,
    val modelRegistrationBlockedVariables: List<String>,
    val registeredVariables: List<ContinuousRadiusSolverVariable> = emptyList(),
    val pwlRegisteredVariables: List<PWLContinuousRadiusSolverVariable> = emptyList()
) {
    /**
     * 转为诊断信息。
     * Convert to diagnostic info.
     *
     * @return 诊断信息 / diagnostic info
     */
    fun info(): Map<String, String> {
        val blockedReason = if (modelRegistrationBlockedVariables.isEmpty()) {
            ""
        } else {
            "solver numeric variable bound conversion requires explicit constants or core token-bound support; footprint/volume/support/renderer remain unbound"
        }
        val pwlInfo = if (pwlRegisteredVariables.isEmpty()) {
            ""
        } else {
            pwlRegisteredVariables.joinToString("|") { "${it.prototype.variableName}:segments=${it.pwlApproximation.numSegments},maxRelErr=${it.pwlApproximation.maxRelativeError.toDouble().formatScientific()}" }
        }
        // Mutual exclusion classification: each variable belongs to exactly one path
        val nativeCount = registeredVariables.size
        val pwlCount = pwlRegisteredVariables.size
        val productionReadyCount = productionReadyVariables.size
        val blockedCount = modelRegistrationBlockedVariables.size
        val mutualExclusionSummary = "native=$nativeCount, pwl=$pwlCount, productionReady=$productionReadyCount, blocked=$blockedCount"
        return mapOf(
            "continuous_radius_solver_registration_plan_count" to variableNames.size.toString(),
            "continuous_radius_solver_registration_plan_variables" to variableNames.joinToString("|"),
            "continuous_radius_solver_registration_plan_bounds" to boundDescriptions.joinToString("|"),
            "continuous_radius_solver_registration_plan_selected_radii" to selectedRadiusDescriptions.joinToString("|"),
            "continuous_radius_solver_registration_plan_gap_variables" to gapDescriptions.joinToString("|"),
            "continuous_radius_solver_registration_plan_production_ready_variables" to productionReadyVariables.joinToString("|"),
            "continuous_radius_solver_model_registration_blocked_variables" to modelRegistrationBlockedVariables.joinToString("|"),
            "continuous_radius_solver_model_registration_blocked_reason" to blockedReason,
            "continuous_radius_solver_pwl_registered_variables" to pwlInfo,
            "continuous_radius_solver_mutual_exclusion_summary" to mutualExclusionSummary
        )
    }

    private fun Double.formatScientific(): String {
        return if (this == 0.0) "0" else String.format("%.4e", this)
    }
}

/**
 * 生成连续半径 solver 变量注册计划。
 * Build the continuous-radius solver variable registration plan.
 *
 * @param prototypes 连续半径 solver 变量原型 / continuous-radius solver variable prototypes
 * @param solverVariables solver 变量列表（native path）/ solver variable list (native path)
 * @param pwlSolverVariables PWL solver 变量列表 / PWL solver variable list
 * @return 注册计划 / registration plan
 */
fun continuousRadiusSolverVariableRegistrationPlan(
    prototypes: List<ContinuousCylinderRadiusSolverPrototype>,
    solverVariables: List<ContinuousRadiusSolverVariable> = emptyList(),
    pwlSolverVariables: List<PWLContinuousRadiusSolverVariable> = emptyList()
): ContinuousRadiusSolverVariableRegistrationPlan {
    val variableNames = ArrayList<String>()
    val boundDescriptions = ArrayList<String>()
    val selectedRadiusDescriptions = ArrayList<String>()
    val gapDescriptions = ArrayList<String>()
    val productionReadyVariables = ArrayList<String>()
    val modelRegistrationBlockedVariables = ArrayList<String>()
    val registeredVariables = ArrayList<ContinuousRadiusSolverVariable>()
    val pwlRegisteredVariables = ArrayList<PWLContinuousRadiusSolverVariable>()
    for (prototype in prototypes) {
        variableNames.add(prototype.variableName)
        boundDescriptions.add(prototype.registrationBoundDescription())
        prototype.registrationSelectedRadiusDescription()?.let { description ->
            selectedRadiusDescriptions.add(description)
        }
        if (prototype.gaps.isNotEmpty()) {
            gapDescriptions.add("${prototype.variableName}:${prototype.gaps.joinToString("+") { it.name }}")
        }
        if (prototype.isProductionReady) {
            productionReadyVariables.add(prototype.variableName)
        }
        when {
            prototype.isSolverRegisterable -> {
                val solverVar = solverVariables.firstOrNull { it.prototype.variableName == prototype.variableName }
                if (solverVar != null) {
                    registeredVariables.add(solverVar)
                }
            }
            prototype.isPWLRegisterable -> {
                val pwlVar = pwlSolverVariables.firstOrNull { it.prototype.variableName == prototype.variableName }
                if (pwlVar != null) {
                    pwlRegisteredVariables.add(pwlVar)
                }
            }
            else -> {
                modelRegistrationBlockedVariables.add(prototype.variableName)
            }
        }
    }
    return ContinuousRadiusSolverVariableRegistrationPlan(
        variableNames = variableNames,
        boundDescriptions = boundDescriptions,
        selectedRadiusDescriptions = selectedRadiusDescriptions,
        gapDescriptions = gapDescriptions,
        productionReadyVariables = productionReadyVariables,
        modelRegistrationBlockedVariables = modelRegistrationBlockedVariables,
        registeredVariables = registeredVariables,
        pwlRegisteredVariables = pwlRegisteredVariables
    )
}

private fun ContinuousCylinderRadiusSolverPrototype.registrationBoundDescription(): String {
    val selectedRadius = initialRadius
    if (selectedRadius != null && gaps.isEmpty()) {
        val selectedText = "${selectedRadius.value.toDouble()} ${selectedRadius.unit.symbol}"
        return "$variableName:$selectedText..$selectedText"
    }
    val lowerText = radiusLowerBound
        ?.let { "${it.value.toDouble()} ${it.unit.symbol}" }
        ?: "-inf"
    val upperText = radiusUpperBound
        ?.let { "${it.value.toDouble()} ${it.unit.symbol}" }
        ?: "+inf"
    return "$variableName:$lowerText..$upperText"
}

private fun ContinuousCylinderRadiusSolverPrototype.registrationSelectedRadiusDescription(): String? {
    val selectedRadius = initialRadius ?: return null
    return "$variableName:${selectedRadius.value.toDouble()} ${selectedRadius.unit.symbol}"
}

/**
 * PWL 提取的半径结果。
 * PWL extracted radius result.
 *
 * @property variableName 变量名 / variable name
 * @property solverRadius solver 选择的半径 r / solver-selected radius r
 * @property solverRadiusSquared solver 的近似 r² 值 q / solver's approximate r² value q
 * @property actualRadiusSquared 真实 r² 值 / actual r² value
 * @property pwlAbsoluteError PWL 绝对误差 |q - r²| / PWL absolute error
 * @property pwlRelativeError PWL 相对误差 |q - r²| / r² / PWL relative error
 * @property isWithinEnvelope 是否在 envelope 范围内 / whether within envelope range
 * @property envelope 保守半径 envelope / conservative radius envelope
 * @property pwlApproximation PWL 近似函数 / PWL approximation function
 */
data class PWLExtractedRadius(
    val variableName: String,
    val solverRadius: InfraNumber,
    val solverRadiusSquared: InfraNumber,
    val actualRadiusSquared: InfraNumber,
    val pwlAbsoluteError: InfraNumber,
    val pwlRelativeError: InfraNumber,
    val isWithinEnvelope: Boolean,
    val envelope: ConservativeRadiusEnvelope,
    val pwlApproximation: PWLRadiusSquaredApproximation
) {
    /**
     * 计算真实圆柱体积（使用 solver 选择的半径）。
     * Compute actual cylinder volume using solver-selected radius.
     */
    fun actualVolume(height: InfraNumber, pi: InfraNumber): InfraNumber {
        return pi * actualRadiusSquared * height
    }

    /**
     * 计算 PWL 近似体积（使用 q ≈ r²）。
     * Compute PWL approximate volume using q ≈ r².
     */
    fun pwlVolume(height: InfraNumber, pi: InfraNumber): InfraNumber {
        return pi * solverRadiusSquared * height
    }

    /**
     * 转为诊断信息。
     * Convert to diagnostic info.
     */
    fun info(): Map<String, String> {
        return mapOf(
            "pwl_radius_${variableName}_r" to solverRadius.toDouble().toString(),
            "pwl_radius_${variableName}_q" to solverRadiusSquared.toDouble().toString(),
            "pwl_radius_${variableName}_r_squared" to actualRadiusSquared.toDouble().toString(),
            "pwl_radius_${variableName}_abs_error" to pwlAbsoluteError.toDouble().toString(),
            "pwl_radius_${variableName}_rel_error" to pwlRelativeError.toDouble().toString(),
            "pwl_radius_${variableName}_within_envelope" to isWithinEnvelope.toString(),
            "pwl_radius_${variableName}_pwl_max_rel_error" to pwlApproximation.maxRelativeError.toDouble().toString()
        )
    }
}

/**
 * 连续半径模型组件，统一管理 native 和 PWL 两条路径的变量创建、约束注册和结果提取。
 * Continuous radius model component that unifies variable creation, constraint registration,
 * and result extraction for both native and PWL paths.
 *
 * 使用方式 / Usage:
 * 1. 创建组件: `val component = ContinuousRadiusModelComponent(prototypes, config)`
 * 2. 注册到模型: `component.register(model)`
 * 3. 求解后提取结果: `val results = component.extractResults(model)`
 *
 * @property prototypes 连续半径 solver 变量原型列表 / continuous-radius solver variable prototypes
 * @property config PWL 近似配置 / PWL approximation config
 */
class ContinuousRadiusModelComponent(
    val prototypes: List<ContinuousCylinderRadiusSolverPrototype>,
    val config: PWLRadiusApproximationConfig = PWLRadiusApproximationConfig()
) {
    /** native 路径 solver 变量 / native path solver variables */
    val nativeVariables: List<ContinuousRadiusSolverVariable> by lazy {
        continuousRadiusSolverVariables(prototypes)
    }

    /** PWL 路径 solver 变量 / PWL path solver variables */
    val pwlVariables: List<PWLContinuousRadiusSolverVariable> by lazy {
        pwlContinuousRadiusSolverVariables(prototypes, config)
    }

    /** 注册计划（含诊断信息） / registration plan (with diagnostics) */
    val registrationPlan: ContinuousRadiusSolverVariableRegistrationPlan by lazy {
        continuousRadiusSolverVariableRegistrationPlan(
            prototypes = prototypes,
            solverVariables = nativeVariables,
            pwlSolverVariables = pwlVariables
        )
    }

    /**
     * 将所有连续半径变量和约束注册到模型。
     * Register all continuous-radius variables and constraints into the model.
     *
     * 包括：
     * - native 路径：注册 RealVar + 上下界约束 + target 约束
     * - PWL 路径：注册 RealVar + 上下界约束 + PWL 函数符号 + helper variables + Big-M 约束（在 LinearMetaModel 上注册，因 InfraNumber 不走 core mechanism model 展开）
     *
     * Includes:
     * - native path: register RealVar + bound constraints + target constraint
     * - PWL path: register RealVar + bound constraints + PWL function symbol + helper variables + Big-M constraints (registered on LinearMetaModel, as InfraNumber bypasses core mechanism model expansion)
     *
     * @param model 线性元模型 / linear meta model
     * @param ensureTry 错误处理函数 / error handling function
     */
    fun register(
        model: LinearMetaModel<InfraNumber>,
        ensureTry: (Try, String) -> Unit
    ) {
        // Register native variables / 注册 native 变量
        for (solverVar in nativeVariables) {
            val proto = solverVar.prototype
            model.add(solverVar.variable)
            // Lower bound / 下界
            proto.radiusLowerBound?.let { lb ->
                val lhs = LinearPolynomial(
                    listOf(LinearMonomial(InfraNumber.one, solverVar.variable)),
                    InfraNumber.zero
                )
                val rhs = LinearPolynomial(emptyList(), lb.value)
                ensureTry(
                    model.addConstraint(
                        relation = LinearInequality(lhs, rhs, Comparison.GE),
                        name = "${proto.variableName}_lb"
                    ),
                    "register continuous radius lower bound for ${proto.variableName}"
                )
            }
            // Upper bound / 上界
            proto.radiusUpperBound?.let { ub ->
                val lhs = LinearPolynomial(
                    listOf(LinearMonomial(InfraNumber.one, solverVar.variable)),
                    InfraNumber.zero
                )
                val rhs = LinearPolynomial(emptyList(), ub.value)
                ensureTry(
                    model.addConstraint(
                        relation = LinearInequality(lhs, rhs, Comparison.LE),
                        name = "${proto.variableName}_ub"
                    ),
                    "register continuous radius upper bound for ${proto.variableName}"
                )
            }
            // Target equality for production-ready / 生产就绪变量的目标等式约束
            if (proto.isProductionReady) {
                proto.initialRadius?.let { ir ->
                    val lhs = LinearPolynomial(
                        listOf(LinearMonomial(InfraNumber.one, solverVar.variable)),
                        InfraNumber.zero
                    )
                    val rhs = LinearPolynomial(emptyList(), ir.value)
                    ensureTry(
                        model.addConstraint(
                            relation = LinearInequality(lhs, rhs, Comparison.EQ),
                            name = "${proto.variableName}_target"
                        ),
                        "register continuous radius target for ${proto.variableName}"
                    )
                }
            }
        }

        // Register PWL variables / 注册 PWL 变量
        for (pwlVar in pwlVariables) {
            val variableName = pwlVar.variableName
            val r = pwlVar.radiusVariable
            val envelope = pwlVar.envelope
            val pwlFunction = pwlVar.pwlFunction

            // Register radius variable / 注册半径变量
            model.add(r)

            // Lower bound: r >= rMin / 半径下界
            ensureTry(
                model.addConstraint(
                    relation = LinearInequality(
                        LinearPolynomial(listOf(LinearMonomial(InfraNumber.one, r)), InfraNumber.zero),
                        LinearPolynomial(emptyList(), envelope.rMin),
                        Comparison.GE
                    ),
                    name = "${variableName}_pwl_r_lb"
                ),
                "register PWL r lower bound for $variableName"
            )

            // Upper bound: r <= rMax / 半径上界
            ensureTry(
                model.addConstraint(
                    relation = LinearInequality(
                        LinearPolynomial(listOf(LinearMonomial(InfraNumber.one, r)), InfraNumber.zero),
                        LinearPolynomial(emptyList(), envelope.rMax),
                        Comparison.LE
                    ),
                    name = "${variableName}_pwl_r_ub"
                ),
                "register PWL r upper bound for $variableName"
            )

            // Register PWL function symbol via core intermediate symbol lifecycle.
            // The LinearFunctionSymbolAdapter wraps the MathFunctionSymbol as an
            // IntermediateSymbol so it can be registered via model.add(symbol).
            //
            // Note: Core token registration (registerAuxiliaryTokens) only happens during
            // the LinearMechanismModel construction phase, which operates on solver-precision
            // floating-point numbers. Since BPP3D uses InfraNumber, helper variables must be
            // manually registered on LinearMetaModel.tokens, and Big-M constraints are
            // registered directly on LinearMetaModel (via registerPWLFunctionConstraints).
            // 通过 core 中间符号生命周期注册 PWL 函数符号。
            // LinearFunctionSymbolAdapter 将 MathFunctionSymbol 包装为 IntermediateSymbol，
            // 以便通过 model.add(symbol) 注册。
            //
            // 注意：core token 注册（registerAuxiliaryTokens）仅在 LinearMechanismModel
            // 构建阶段发生，该阶段操作求解器精度浮点数。
            // 由于 BPP3D 使用 InfraNumber，辅助变量需手动注册到 LinearMetaModel.tokens，
            // Big-M 约束直接在 LinearMetaModel 上注册（通过 registerPWLFunctionConstraints）。
            val pwlSymbol = LinearFunctionSymbolAdapter(pwlFunction, IntoValue.fromConverter(FltX))
            ensureTry(
                model.add(pwlSymbol),
                "register PWL function symbol for $variableName"
            )

            // Manually register PWL helper variables into LinearMetaModel.tokens.
            // Core auxiliary token registration only happens during LinearMechanismModel construction,
            // which unfolds tokens to solver-precision floating-point and registers auxiliary tokens
            // on the solver-precision token table. Since BPP3D operates on InfraNumber and registers
            // constraints on LinearMetaModel, helper variables (selector vars, result var) must be
            // registered on LinearMetaModel.tokens so that constraint symbol references can be resolved
            // during mechanism model construction.
            // 手动将 PWL 辅助变量注册到 LinearMetaModel.tokens。
            // core auxiliary token 注册仅在 LinearMechanismModel 构建阶段发生，
            // 该阶段将 tokens 展开到求解器精度浮点数并在求解器精度 token table 上注册辅助 token。
            // 由于 BPP3D 操作 InfraNumber 并在 LinearMetaModel 上注册约束，
            // 辅助变量（选择变量、结果变量）必须注册到 LinearMetaModel.tokens，
            // 以便 mechanism model 构建时约束符号引用能被解析。
            for (helperVar in pwlFunction.helperVariables) {
                ensureTry(
                    model.add(helperVar),
                    "register PWL helper variable ${helperVar.name} for $variableName"
                )
            }

            // Register PWL Big-M constraints on LinearMetaModel.
            // This is necessary because core mechanism model constraint expansion
            // only supports solver-precision floating-point, but BPP3D operates on InfraNumber.
            // 在 LinearMetaModel 上注册 PWL Big-M 约束。
            // 这是必要的，因为 core mechanism model 约束展开仅支持求解器精度浮点数，
            // 而 BPP3D 操作的是 InfraNumber。
            registerPWLFunctionConstraints(model, pwlFunction, variableName, ensureTry)
        }
    }

    /**
     * 从 solver 模型中提取 native 连续半径结果。
     * Extract native continuous-radius results from solver model.
     *
     * @param model 线性元模型（求解后）/ linear meta model (after solving)
     * @return 变量名到 solver 选择值的映射 / variable name to solver-selected value map
     */
    fun extractNativeResults(model: LinearMetaModel<InfraNumber>): Map<String, InfraNumber> {
        val results = LinkedHashMap<String, InfraNumber>()
        for (solverVar in nativeVariables) {
            val proto = solverVar.prototype
            val token = model.tokens.find(solverVar.variable) ?: continue
            val value = token.doubleResult?.let { InfraNumber(it) } ?: continue
            results[proto.variableName] = value
        }
        return results
    }

    /**
     * 从 solver 模型中提取 PWL 连续半径结果（opaque Map，用于 ColumnGenerationFinalResult）。
     * Extract PWL continuous-radius results from solver model (opaque Map for ColumnGenerationFinalResult).
     *
     * @param model 线性元模型（求解后）/ linear meta model (after solving)
     * @return opaque Map，外层 key 为变量名，内层 key 为结果字段 / opaque Map, outer key = variable name, inner key = result field
     */
    fun extractPWLResults(model: LinearMetaModel<InfraNumber>): Map<String, Map<String, InfraNumber>> {
        if (pwlVariables.isEmpty()) return emptyMap()

        val pwlResultsMap = LinkedHashMap<String, Map<String, InfraNumber>>()
        for (pwlVar in pwlVariables) {
            val rToken = model.tokens.find(pwlVar.radiusVariable)
            val rValue = rToken?.doubleResult?.let { InfraNumber(it) } ?: continue

            val qToken = model.tokens.find(pwlVar.pwlFunction.resultVar)
            val qValue = qToken?.doubleResult?.let { InfraNumber(it) } ?: InfraNumber.zero

            val actualRSquared = rValue * rValue
            val pwlError = (qValue - actualRSquared).abs()
            val pwlRelativeError = if (actualRSquared > infraScalar(1e-12)) pwlError / actualRSquared else InfraNumber.zero
            val isWithinEnvelope = pwlVar.envelope.isRadiusValid(rValue)

            pwlResultsMap[pwlVar.variableName] = mapOf(
                "solverRadius" to rValue,
                "solverRadiusSquared" to qValue,
                "actualRadiusSquared" to actualRSquared,
                "pwlAbsoluteError" to pwlError,
                "pwlRelativeError" to pwlRelativeError,
                "isWithinEnvelope" to InfraNumber(if (isWithinEnvelope) 1.0 else 0.0),
                "maxPWLRelativeError" to pwlVar.pwlApproximation.maxRelativeError,
                "numSegments" to InfraNumber(pwlVar.pwlApproximation.numSegments.toDouble())
            )
        }
        return pwlResultsMap
    }

    /**
     * 获取注册计划诊断信息。
     * Get registration plan diagnostic info.
     *
     * @return 诊断信息 / diagnostic info
     */
    fun info(): Map<String, String> = registrationPlan.info()

    /**
     * 获取 PWL 模型规模诊断信息。
     * Get PWL model scale diagnostic info.
     *
     * 包括所有 PWL 变量的段数、选择变量数、辅助变量数、约束数和误差统计汇总。
     * Includes summary of segment counts, selector variables, helper variables,
     * constraint counts, and error statistics across all PWL variables.
     *
     * @return 模型规模诊断信息 / model scale diagnostic info
     */
    fun modelScaleInfo(): Map<String, String> {
        if (pwlVariables.isEmpty()) {
            return mapOf(
                "pwl_total_prototypes" to "0",
                "pwl_total_segments" to "0",
                "pwl_total_selector_vars" to "0",
                "pwl_total_helper_vars" to "0",
                "pwl_total_constraints" to "0",
                "pwl_max_segments" to "0",
                "pwl_avg_segments" to "0.0",
                "pwl_max_relative_error" to "0.0",
                "pwl_avg_relative_error" to "0.0"
            )
        }
        val totalPrototypes = pwlVariables.size
        val segmentCounts = pwlVariables.map { it.pwlApproximation.numSegments }
        val totalSegments = segmentCounts.sum()
        val totalSelectorVars = pwlVariables.sumOf { it.pwlFunction.selectorVars.size }
        val totalHelperVars = pwlVariables.sumOf { it.pwlFunction.helperVariables.size }
        // Per PWL variable: 1 select-one constraint + 4 Big-M constraints per segment
        // These constraints are registered on LinearMetaModel by registerPWLFunctionConstraints,
        // because core LinearMechanismModel constraint expansion only supports solver-precision
        // floating-point. BPP3D uses InfraNumber, so PWL Big-M constraints are registered directly.
        // 每个 PWL 变量：1 个 select-one 约束 + 4 个 Big-M 约束/线段
        // 这些约束通过 registerPWLFunctionConstraints 在 LinearMetaModel 上注册，
        // 因为 core LinearMechanismModel 约束展开仅支持求解器精度浮点数。
        // BPP3D 使用 InfraNumber，因此 PWL Big-M 约束直接注册。
        val totalConstraints = pwlVariables.sumOf { 1 + 4 * it.pwlApproximation.numSegments }
        val maxSegments = segmentCounts.maxOrNull() ?: 0
        val avgSegments = segmentCounts.average()
        val relativeErrors = pwlVariables.map { it.pwlApproximation.maxRelativeError.toDouble() }
        val maxRelError = relativeErrors.maxOrNull() ?: 0.0
        val avgRelError = relativeErrors.average()

        return mapOf(
            "pwl_total_prototypes" to totalPrototypes.toString(),
            "pwl_total_segments" to totalSegments.toString(),
            "pwl_total_selector_vars" to totalSelectorVars.toString(),
            "pwl_total_helper_vars" to totalHelperVars.toString(),
            "pwl_total_constraints" to totalConstraints.toString(),
            "pwl_max_segments" to maxSegments.toString(),
            "pwl_avg_segments" to String.format("%.1f", avgSegments),
            "pwl_max_relative_error" to String.format("%.4e", maxRelError),
            "pwl_avg_relative_error" to String.format("%.4e", avgRelError)
        )
    }

    /**
     * 将 UnivariateLinearPiecewiseFunction 的 Big-M 约束注册到 LinearMetaModel。
     * Register UnivariateLinearPiecewiseFunction's Big-M constraints into LinearMetaModel.
     *
     * This mirrors the logic in UnivariateLinearPiecewiseFunction.registerConstraints
     * but targets LinearMetaModel.addConstraint instead of AbstractLinearMechanismModel.addConstraint.
     *
     * This is necessary because core LinearMechanismModel constraint expansion only
     * supports solver-precision floating-point models, but BPP3D operates on InfraNumber.
     * 这是因为 core LinearMechanismModel 约束展开仅支持求解器精度浮点数模型，
     * 而 BPP3D 操作的是 InfraNumber。
     */
    private fun registerPWLFunctionConstraints(
        model: LinearMetaModel<InfraNumber>,
        pwlFunction: UnivariateLinearPiecewiseFunction<InfraNumber>,
        variableName: String,
        ensureTry: (Try, String) -> Unit
    ) {
        val name = pwlFunction.name
        val breakpoints = pwlFunction.breakpoints
        val slopes = pwlFunction.slopes
        val intercepts = pwlFunction.intercepts
        val numSegments = breakpoints.size - 1
        val selectorVars = pwlFunction.selectorVars
        val resultVar = pwlFunction.resultVar
        val x = pwlFunction.x

        val one = InfraNumber.one
        val zero = InfraNumber.zero

        // Compute output range for Big-M calculation
        // 计算输出范围用于 Big-M 计算
        val outputValues = (0 until numSegments).flatMap { i ->
            listOf(
                slopes[i] * breakpoints[i] + intercepts[i],
                slopes[i] * breakpoints[i + 1] + intercepts[i]
            )
        }
        val outputLower = outputValues.reduce { acc, value -> if (value ls acc) value else acc }
        val outputUpper = outputValues.reduce { acc, value -> if (value gr acc) value else acc }

        // Exactly one segment must be active: sum(s[i]) = 1
        // 恰好一个线段激活：sum(s[i]) = 1
        val sumMonos = selectorVars.map { LinearMonomial(one, it) }
        ensureTry(
            model.addConstraint(
                relation = LinearInequality(
                    LinearPolynomial(sumMonos, zero),
                    LinearPolynomial(emptyList(), one),
                    Comparison.EQ
                ),
                name = "${name}_select_one"
            ),
            "register PWL select-one constraint for $variableName"
        )

        for (i in 0 until numSegments) {
            val sVar = selectorVars[i]
            val bpLow = breakpoints[i]
            val bpHigh = breakpoints[i + 1]
            val slope = slopes[i]
            val intercept = intercepts[i]

            // Compute per-segment Big-M aligned with core logic
            // For each segment, Big-M is derived from the x-bounds relaxation and
            // output-range relaxation, matching UnivariateLinearPiecewiseFunction.registerConstraints.
            // 每个 segment 独立计算 Big-M，与 core 逻辑对齐。
            // Big-M 由 x-bounds 松弛和输出范围松弛推导，匹配
            // UnivariateLinearPiecewiseFunction.registerConstraints。
            val xLower = breakpoints.first()
            val xUpper = breakpoints.last()
            val lineAtLower = slope * xLower + intercept
            val lineAtUpper = slope * xUpper + intercept
            val lineLower = if (lineAtLower ls lineAtUpper) lineAtLower else lineAtUpper
            val lineUpper = if (lineAtLower gr lineAtUpper) lineAtLower else lineAtUpper
            val xLowerRelax = if (bpLow gr xLower) bpLow - xLower else zero
            val xUpperRelax = if (xUpper gr bpHigh) xUpper - bpHigh else zero
            val eqUpperRelax = (outputUpper - lineLower).abs()
            val eqLowerRelax = (outputLower - lineUpper).abs()
            val bigMValue = listOf(
                xLowerRelax,
                xUpperRelax,
                eqUpperRelax,
                eqLowerRelax
            ).reduce { acc, value -> if (value gr acc) value else acc }
            // Ensure Big-M is positive (matches core ensurePositiveBigM behavior)
            // 确保 Big-M 为正值（匹配 core ensurePositiveBigM 行为）
            val safeBigM = if (bigMValue ls infraScalar(1e-6)) infraScalar(1e6) else bigMValue

            // Lower bound: x >= bpLow - M*(1 - s[i])
            ensureTry(
                model.addConstraint(
                    relation = LinearInequality(
                        LinearPolynomial(
                            x.monomials.map { LinearMonomial(it.coefficient, it.symbol) } + LinearMonomial(-safeBigM, sVar),
                            x.constant + safeBigM
                        ),
                        LinearPolynomial(emptyList(), bpLow),
                        Comparison.GE
                    ),
                    name = "${name}_seg_${i}_lb"
                ),
                "register PWL segment $i lower bound for $variableName"
            )

            // Upper bound: x <= bpHigh + M*(1 - s[i])
            ensureTry(
                model.addConstraint(
                    relation = LinearInequality(
                        LinearPolynomial(
                            x.monomials.map { LinearMonomial(it.coefficient, it.symbol) } + LinearMonomial(safeBigM, sVar),
                            x.constant
                        ),
                        LinearPolynomial(emptyList(), bpHigh + safeBigM),
                        Comparison.LE
                    ),
                    name = "${name}_seg_${i}_ub"
                ),
                "register PWL segment $i upper bound for $variableName"
            )

            // y - slope*x - intercept <= M*(1 - s[i])
            val negSlopeXMonos = x.monomials.map { LinearMonomial(-it.coefficient * slope, it.symbol) }
            ensureTry(
                model.addConstraint(
                    relation = LinearInequality(
                        LinearPolynomial(
                            listOf(LinearMonomial(one, resultVar)) + negSlopeXMonos + LinearMonomial(safeBigM, sVar),
                            -intercept
                        ),
                        LinearPolynomial(emptyList(), safeBigM),
                        Comparison.LE
                    ),
                    name = "${name}_seg_${i}_eq_ub"
                ),
                "register PWL segment $i equality upper for $variableName"
            )

            // y - slope*x - intercept >= -M*(1 - s[i])
            ensureTry(
                model.addConstraint(
                    relation = LinearInequality(
                        LinearPolynomial(
                            listOf(LinearMonomial(one, resultVar)) + negSlopeXMonos + LinearMonomial(-safeBigM, sVar),
                            -intercept
                        ),
                        LinearPolynomial(emptyList(), -safeBigM),
                        Comparison.GE
                    ),
                    name = "${name}_seg_${i}_eq_lb"
                ),
                "register PWL segment $i equality lower for $variableName"
            )
        }
    }
}
