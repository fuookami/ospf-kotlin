@file:Suppress("DEPRECATION")

/**
 * 圆柱形状契约。
 * Cylinder shape contract.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.item.model

import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.geometry.Axis3
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.convertTo
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.CylinderPackingShape3
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Orientation
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.OrientationCategory
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PackingShape3
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.fltX

/**
 * 圆柱能力路径状态。
 * Cylinder capability path status.
 */
enum class CylinderCapabilityStatus {
    /** 仅长方体路径。 / Cuboid-only path. */
    CuboidOnly,

    /** 默认候选生成路径，仅支持竖直圆柱。 / Default candidate path, vertical-cylinder-only. */
    VerticalCandidateOnly,

    /** 轴向感知候选生成路径。 / Axis-aware candidate path. */
    AxisAwareCandidate,

    /** 已验证候选放置适配路径。 / Verified generated-candidate placement adapter path. */
    VerifiedGeneratedPlacement,

    /** 支撑语义路径，仅支持直立竖直圆柱。 / Support semantics path, upright vertical-cylinder-only. */
    UprightVerticalSupportOnly,

    /** 已知坐标终态校验路径。 / Known-coordinate final validation path. */
    KnownCoordinateFinalValidation,

    /** 后验深度边界校验路径。 / Post-solve depth-boundary validation path. */
    DepthBoundaryFinalValidation
}

/**
 * 圆柱能力路径。
 * Cylinder capability path.
 *
 * @property source 调用来源 / call source
 * @property status 能力状态 / capability status
 * @property pathPredicate 仅长方体路径描述 / cuboid-only path predicate
 */
enum class CylinderCapabilityPath(
    val source: String,
    val status: CylinderCapabilityStatus,
    val pathPredicate: String? = null
) {
    DefaultLayerCandidate(
        source = "LayerGeneration.defaultCandidate",
        status = CylinderCapabilityStatus.VerticalCandidateOnly
    ),
    CirclePackingCandidate(
        source = "CirclePackingLayerGenerator",
        status = CylinderCapabilityStatus.AxisAwareCandidate
    ),
    ApplicationLayerPlacementCandidate(
        source = "LayerPlacementAdapter.toLayerPlacement",
        status = CylinderCapabilityStatus.VerifiedGeneratedPlacement
    ),
    PileSupportCandidate(
        source = "PileLayerGenerator",
        status = CylinderCapabilityStatus.UprightVerticalSupportOnly
    ),
    PackageAttributeSupport(
        source = "PackageAttribute.supportPackingShape",
        status = CylinderCapabilityStatus.UprightVerticalSupportOnly
    ),
    SimpleBlockCandidate(
        source = "SimpleBlockGenerator",
        status = CylinderCapabilityStatus.VerticalCandidateOnly
    ),
    DfsMlhsCuboidSearch(
        source = "DFS/MLHS",
        status = CylinderCapabilityStatus.CuboidOnly,
        pathPredicate = "DFS/MLHS space-splitting path is"
    ),
    ItemMerge(
        source = "ItemMerger.merge",
        status = CylinderCapabilityStatus.CuboidOnly,
        pathPredicate = "item merge paths are"
    ),
    ItemMergePiles(
        source = "ItemMerger.mergePiles",
        status = CylinderCapabilityStatus.CuboidOnly,
        pathPredicate = "item merge paths are"
    ),
    ItemMergeBlocks(
        source = "ItemMerger.mergeBlocks",
        status = CylinderCapabilityStatus.CuboidOnly,
        pathPredicate = "item merge paths are"
    ),
    ItemMergePatternBlocks(
        source = "ItemMerger.mergePatternBlocks",
        status = CylinderCapabilityStatus.CuboidOnly,
        pathPredicate = "item merge paths are"
    ),
    ItemMergeHollowSquareBlocks(
        source = "ItemMerger.mergeHollowSquareBlocks",
        status = CylinderCapabilityStatus.CuboidOnly,
        pathPredicate = "item merge paths are"
    ),
    PatternPlacement(
        source = "Pattern",
        status = CylinderCapabilityStatus.CuboidOnly,
        pathPredicate = "pattern placement paths are"
    ),
    KnownCoordinateFinalPacking(
        source = "known-coordinate final packing",
        status = CylinderCapabilityStatus.KnownCoordinateFinalValidation
    ),
    RendererFinalPacking(
        source = "renderer final packing",
        status = CylinderCapabilityStatus.KnownCoordinateFinalValidation
    ),
    DepthBoundaryFinalValidation(
        source = "DepthBoundaryLayerOrientationPolicy",
        status = CylinderCapabilityStatus.DepthBoundaryFinalValidation
    )
}

/**
 * 横向圆柱轴向未开放错误信息。
 * Unsupported horizontal cylinder axis message.
 *
 * @param source 调用来源 / call source
 * @param axis 实际轴向 / actual axis
 * @return 错误信息 / error message
 */
fun unsupportedCylinderAxisMessage(source: String, axis: Axis3): String {
    return "Unsupported cylinder axis in $source: only Axis3.Y is allowed, but got $axis."
}

/**
 * 横向圆柱候选来源未开放错误信息。
 * Unsupported generated horizontal cylinder source message.
 *
 * @param source 调用来源 / call source
 * @return 错误信息 / error message
 */
fun unsupportedGeneratedHorizontalCylinderSourceMessage(source: String): String {
    return "Unsupported horizontal cylinder in $source: only verified axis-aware generated candidates are allowed."
}

/**
 * 要求横向圆柱候选放置来自已验证的轴向感知生成路径。
 * Require horizontal cylinder candidate placement to come from verified axis-aware generation.
 *
 * @param shape 装箱形状 / packing shape
 * @param verifiedAxisAwareCandidate 是否为已验证轴向感知候选 / whether it is a verified axis-aware candidate
 * @param path 能力路径 / capability path
 */
fun requireVerifiedGeneratedCylinderCandidate(
    shape: PackingShape3<FltX>,
    verifiedAxisAwareCandidate: Boolean,
    path: CylinderCapabilityPath
) {
    require(path.status == CylinderCapabilityStatus.VerifiedGeneratedPlacement) {
        "Cylinder capability path ${path.name} is not a verified generated placement path."
    }
    if (shape is CylinderPackingShape3 && shape.axis != Axis3.Y && !verifiedAxisAwareCandidate) {
        throw IllegalArgumentException(unsupportedGeneratedHorizontalCylinderSourceMessage(source = path.source))
    }
}

/**
 * 圆柱非直立朝向未开放错误信息。
 * Unsupported non-upright cylinder orientation message.
 *
 * @param source 调用来源 / call source
 * @return 错误信息 / error message
 */
fun unsupportedCylinderOrientationMessage(source: String): String {
    return "Unsupported cylinder orientation in $source: only upright orientations are allowed."
}

/**
 * 圆柱侧放/卧放堆叠层策略未开放错误信息。
 * Unsupported side/lie cylinder top-layer policy message.
 *
 * @param source 调用来源 / call source
 * @return 错误信息 / error message
 */
fun unsupportedCylinderTopLayerPolicyMessage(source: String): String {
    return "Unsupported cylinder top-layer policy in $source: side/lie stacking is not allowed."
}

/**
 * 无坐标圆柱堆叠/悬挂支撑未开放错误信息。
 * Unsupported coordinate-less cylinder stacking/hanging support message.
 *
 * @param source 调用来源 / call source
 * @return 错误信息 / error message
 */
fun unsupportedCylinderStackingSupportMessage(source: String): String {
    return "Unsupported coordinate-less cylinder stacking and hanging support in $source: only upright Axis3.Y items are allowed; horizontal cylinders require verified 3D placement support coverage."
}

/**
 * 连续半径优化未开放错误信息。
 * Unsupported continuous radius optimization message.
 *
 * @param source 调用来源 / call source
 * @return 错误信息 / error message
 */
fun unsupportedContinuousCylinderRadiusOptimizationMessage(source: String): String {
    return "Unsupported continuous cylinder radius optimization in $source: radiusWeightFunctionKey requires a concrete selected radius result for final actual-radius validation."
}

/**
 * 连续半径优化缺口。
 * Continuous-radius optimization gap.
 *
 * @property detail 缺口说明 / gap detail
 */
enum class ContinuousCylinderRadiusOptimizationGap(
    val detail: String
) {
    /** 缺失已选择具体半径。 / Missing concrete selected radius. */
    MissingSelectedRadius(
        detail = "field=radius_weight_function_key requires radius_meter or selected radius"
    ),

    /** 连续半径 key 与离散半径元数据冲突。 / Continuous-radius key conflicts with discrete radius metadata. */
    DiscreteRadiusMetadataConflict(
        detail = "radius_weight_function_key cannot be combined with discrete radius candidates or radius/diameter steps"
    ),

    /** 区间型半径仍缺少 solver 原生符号变量闭环。 / Interval radius still lacks solver-native symbolic variable closure. */
    SolverNativeRadiusIntervalUnsupported(
        detail = "continuous radius interval is unsupported: provide radius_step or radius_meter; symbolic radius variables are not wired to layer footprint, volume, support coverage, and renderer actualVolume"
    ),

    /** 区间型直径仍缺少 solver 原生符号变量闭环。 / Interval diameter still lacks solver-native symbolic variable closure. */
    SolverNativeDiameterIntervalUnsupported(
        detail = "continuous diameter interval is unsupported: provide diameter_step or radius_meter; symbolic radius variables are not wired to layer footprint, volume, support coverage, and renderer actualVolume"
    )
}

/**
 * 连续半径优化缺口报告。
 * Continuous-radius optimization gap report.
 *
 * @property source 调用来源 / call source
 * @property radiusWeightFunctionKey 半径权重函数键（可选） / radius weight function key (optional)
 * @property gaps 缺口列表 / gap list
 */
data class ContinuousCylinderRadiusOptimizationGapReport(
    val source: String,
    val radiusWeightFunctionKey: String?,
    val gaps: List<ContinuousCylinderRadiusOptimizationGap>
) {
    init {
        require(source.isNotBlank()) {
            "Continuous cylinder radius optimization gap source must not be blank."
        }
        require(gaps.isNotEmpty()) {
            "Continuous cylinder radius optimization gap report must contain at least one gap."
        }
    }

    /**
     * 转为错误信息。
     * Convert to error message.
     *
     * @param rowDescription 行描述（可选） / row description (optional)
     * @return 错误信息 / error message
     */
    fun message(rowDescription: String? = null): String {
        val keyText = radiusWeightFunctionKey
            ?.takeIf { it.isNotBlank() }
            ?.let { " key=$it;" }
            ?: ""
        val gapText = gaps.joinToString(separator = "; ") { gap -> gap.detail }
        val rowText = rowDescription
            ?.takeIf { it.isNotBlank() }
            ?.let { ", $it" }
            ?: ""
        return "${unsupportedContinuousCylinderRadiusOptimizationMessage(source)}$keyText gaps=$gapText$rowText"
    }
}

/**
 * 构建连续半径优化缺口报告。
 * Build continuous-radius optimization gap report.
 *
 * @param source 调用来源 / call source
 * @param radiusWeightFunctionKey 半径权重函数键（可选） / radius weight function key (optional)
 * @param hasConcreteSelectedRadius 是否已提供具体选择半径 / whether a concrete selected radius is provided
 * @param hasDiscreteRadiusCandidates 是否存在离散半径候选 / whether discrete radius candidates exist
 * @param hasDiscreteRadiusStep 是否存在离散半径或直径步长 / whether discrete radius or diameter step exists
 * @param hasContinuousRadiusInterval 是否存在未离散化的半径区间 / whether an undiscretized radius interval exists
 * @param hasContinuousDiameterInterval 是否存在未离散化的直径区间 / whether an undiscretized diameter interval exists
 * @return 缺口报告；无缺口时返回 null / gap report, or null when there is no gap
 */
fun continuousCylinderRadiusOptimizationGapReport(
    source: String,
    radiusWeightFunctionKey: String?,
    hasConcreteSelectedRadius: Boolean,
    hasDiscreteRadiusCandidates: Boolean = false,
    hasDiscreteRadiusStep: Boolean = false,
    hasContinuousRadiusInterval: Boolean = false,
    hasContinuousDiameterInterval: Boolean = false
): ContinuousCylinderRadiusOptimizationGapReport? {
    val key = radiusWeightFunctionKey?.takeIf { it.isNotBlank() }
    val gaps = ArrayList<ContinuousCylinderRadiusOptimizationGap>()
    if (key != null && !hasConcreteSelectedRadius) {
        gaps.add(ContinuousCylinderRadiusOptimizationGap.MissingSelectedRadius)
    }
    if (key != null && (hasDiscreteRadiusCandidates || hasDiscreteRadiusStep)) {
        gaps.add(ContinuousCylinderRadiusOptimizationGap.DiscreteRadiusMetadataConflict)
    }
    if (!hasConcreteSelectedRadius && hasContinuousRadiusInterval) {
        gaps.add(ContinuousCylinderRadiusOptimizationGap.SolverNativeRadiusIntervalUnsupported)
    }
    if (!hasConcreteSelectedRadius && hasContinuousDiameterInterval) {
        gaps.add(ContinuousCylinderRadiusOptimizationGap.SolverNativeDiameterIntervalUnsupported)
    }
    if (gaps.isEmpty()) {
        return null
    }
    return ContinuousCylinderRadiusOptimizationGapReport(
        source = source,
        radiusWeightFunctionKey = key,
        gaps = gaps
    )
}

private val ContinuousRadiusVariableTokenRegex = Regex("[^A-Za-z0-9]+")

private fun String.continuousRadiusVariableToken(fallback: String): String {
    val token = trim()
        .replace(ContinuousRadiusVariableTokenRegex, "_")
        .trim('_')
    return token.ifEmpty { fallback }
}

private fun continuousRadiusVariableName(
    source: String,
    radiusWeightFunctionKey: String?,
    axis: Axis3
): String {
    val sourceToken = source.continuousRadiusVariableToken("source")
    val keyToken = radiusWeightFunctionKey
        ?.continuousRadiusVariableToken("anonymous")
        ?: "anonymous"
    return "cylinder_radius_${sourceToken}_${keyToken}_${axis.name}"
}

private fun Quantity<FltX>.toContinuousRadiusBoundFromDiameter(
    radiusUnitSource: Quantity<FltX>?
): Quantity<FltX> {
    val targetUnit = radiusUnitSource?.unit ?: unit
    val converted = convertTo(targetUnit)
        ?: throw IllegalArgumentException("Cylinder diameter bound must use a length-compatible unit.")
    return Quantity(fltX(converted.value.toDouble() / 2.0), targetUnit)
}

private fun Quantity<FltX>.radiusValueText(): String {
    return "${value.toDouble()} ${unit.symbol}"
}

/**
 * 连续半径 solver 原生变量原型。
 * Solver-native continuous-radius variable prototype.
 *
 * @property source 调用来源 / call source
 * @property radiusWeightFunctionKey 半径权重函数键（可选） / radius weight function key (optional)
 * @property axis 圆柱轴向 / cylinder axis
 * @property variableName solver 半径变量名 / solver radius variable name
 * @property radiusLowerBound 半径下界（可选） / radius lower bound (optional)
 * @property radiusUpperBound 半径上界（可选） / radius upper bound (optional)
 * @property initialRadius 初始或已选择半径（可选） / initial or selected radius (optional)
 * @property gaps 当前生产闭环缺口 / current production closure gaps
 */
data class ContinuousCylinderRadiusSolverPrototype(
    val source: String,
    val radiusWeightFunctionKey: String?,
    val axis: Axis3,
    val variableName: String,
    val radiusLowerBound: Quantity<FltX>? = null,
    val radiusUpperBound: Quantity<FltX>? = null,
    val initialRadius: Quantity<FltX>? = null,
    val gaps: List<ContinuousCylinderRadiusOptimizationGap> = emptyList()
) {
    init {
        require(source.isNotBlank()) {
            "Continuous cylinder radius solver prototype source must not be blank."
        }
        radiusWeightFunctionKey?.let {
            require(it.isNotBlank()) {
                "Continuous cylinder radius solver prototype key must not be blank."
            }
        }
        require(variableName.isNotBlank()) {
            "Continuous cylinder radius solver prototype variable name must not be blank."
        }
        radiusLowerBound?.let {
            require(it.value.toDouble() > 0.0) {
                "Continuous cylinder radius solver prototype lower bound must be positive."
            }
        }
        radiusUpperBound?.let {
            require(it.value.toDouble() > 0.0) {
                "Continuous cylinder radius solver prototype upper bound must be positive."
            }
        }
        initialRadius?.let {
            require(it.value.toDouble() > 0.0) {
                "Continuous cylinder radius solver prototype initial radius must be positive."
            }
        }
        if (initialRadius != null && radiusLowerBound != null) {
            val lower = radiusLowerBound.convertTo(initialRadius.unit)
                ?: throw IllegalArgumentException("Continuous cylinder radius solver prototype bounds must be length-compatible.")
            require(initialRadius.value.toDouble() >= lower.value.toDouble()) {
                "Continuous cylinder radius solver prototype selected or initial radius must be greater than or equal to lower bound."
            }
        }
        if (initialRadius != null && radiusUpperBound != null) {
            val upper = radiusUpperBound.convertTo(initialRadius.unit)
                ?: throw IllegalArgumentException("Continuous cylinder radius solver prototype bounds must be length-compatible.")
            require(initialRadius.value.toDouble() <= upper.value.toDouble()) {
                "Continuous cylinder radius solver prototype selected or initial radius must be less than or equal to upper bound."
            }
        }
        if (radiusLowerBound != null && radiusUpperBound != null) {
            val upper = radiusUpperBound.convertTo(radiusLowerBound.unit)
                ?: throw IllegalArgumentException("Continuous cylinder radius solver prototype bounds must be length-compatible.")
            require(radiusLowerBound.value.toDouble() <= upper.value.toDouble()) {
                "Continuous cylinder radius solver prototype lower bound must be less than or equal to upper bound."
            }
        }
    }

    /** 是否已经具备生产级回写闭环。 / Whether the production selection closure is available. */
    val isProductionReady: Boolean get() = initialRadius != null && gaps.isEmpty()

    /** 是否可以注册到 solver model（允许 solver 自由选择半径范围内的最优值）。 / Whether the variable can be registered into the solver model (solver may freely choose the optimal value within bounds). */
    val isSolverRegisterable: Boolean get() = gaps.none {
        it == ContinuousCylinderRadiusOptimizationGap.SolverNativeRadiusIntervalUnsupported
                || it == ContinuousCylinderRadiusOptimizationGap.SolverNativeDiameterIntervalUnsupported
    }

    /** 是否可以通过 PWL 近似路径注册到 solver model（interval-only 变量使用分段线性近似表达 r²）。
     * PWL 路径允许 MissingSelectedRadius gap（PWL 就是由 solver 选择半径，不需要预先选定的半径），
     * 但必须提供 radiusWeightFunctionKey 以确保生产回写路径可用。
     *
     * Whether the variable can be registered into the solver model via the PWL approximation path
     * (interval-only variables use piecewise linear approximation for r²).
     * PWL path allows MissingSelectedRadius gap (PWL is about letting the solver choose the radius,
     * no pre-selected radius needed), but requires radiusWeightFunctionKey to ensure the production
     * writeback path is available.
     */
    val isPWLRegisterable: Boolean get() = gaps.all {
        it == ContinuousCylinderRadiusOptimizationGap.SolverNativeRadiusIntervalUnsupported
                || it == ContinuousCylinderRadiusOptimizationGap.SolverNativeDiameterIntervalUnsupported
                || it == ContinuousCylinderRadiusOptimizationGap.MissingSelectedRadius
    } && radiusLowerBound != null && radiusUpperBound != null && initialRadius == null
            && radiusWeightFunctionKey != null

    /**
     * 转为错误信息后缀。
     * Convert to error message suffix.
     *
     * @return 错误信息后缀 / error message suffix
     */
    fun messageSuffix(): String {
        val keyText = radiusWeightFunctionKey
            ?.let { ", key=$it" }
            ?: ""
        val lowerText = radiusLowerBound
            ?.radiusValueText()
            ?: "unbounded"
        val upperText = radiusUpperBound
            ?.radiusValueText()
            ?: "unbounded"
        val initialText = initialRadius
            ?.radiusValueText()
            ?: "unselected"
        return " solverPrototype(variable=$variableName, axis=$axis$keyText, radiusBounds=[$lowerText, $upperText], initialRadius=$initialText, productionReady=$isProductionReady)"
    }
}

/**
 * 构建连续半径 solver 原生变量原型。
 * Build a solver-native continuous-radius variable prototype.
 *
 * @param source 调用来源 / call source
 * @param radiusWeightFunctionKey 半径权重函数键（可选） / radius weight function key (optional)
 * @param axis 圆柱轴向 / cylinder axis
 * @param selectedRadius 已选择半径（可选） / selected radius (optional)
 * @param radiusMin 半径下界（可选） / radius lower bound (optional)
 * @param radiusMax 半径上界（可选） / radius upper bound (optional)
 * @param diameterMin 直径下界（可选） / diameter lower bound (optional)
 * @param diameterMax 直径上界（可选） / diameter upper bound (optional)
 * @param hasDiscreteRadiusCandidates 是否存在离散半径候选 / whether discrete radius candidates exist
 * @param hasDiscreteRadiusStep 是否存在离散半径或直径步长 / whether discrete radius or diameter step exists
 * @return solver 原生变量原型；未请求连续半径语义时返回 null / solver-native variable prototype, or null when no continuous-radius semantics are requested
 */
fun continuousCylinderRadiusSolverPrototype(
    source: String,
    radiusWeightFunctionKey: String?,
    axis: Axis3,
    selectedRadius: Quantity<FltX>? = null,
    radiusMin: Quantity<FltX>? = null,
    radiusMax: Quantity<FltX>? = null,
    diameterMin: Quantity<FltX>? = null,
    diameterMax: Quantity<FltX>? = null,
    hasDiscreteRadiusCandidates: Boolean = false,
    hasDiscreteRadiusStep: Boolean = false
): ContinuousCylinderRadiusSolverPrototype? {
    val key = radiusWeightFunctionKey?.takeIf { it.isNotBlank() }
    val hasRadiusInterval = selectedRadius == null && radiusMin != null && radiusMax != null && !hasDiscreteRadiusStep
    val hasDiameterInterval = selectedRadius == null && diameterMin != null && diameterMax != null && !hasDiscreteRadiusStep
    if (key == null && !hasRadiusInterval && !hasDiameterInterval) {
        return null
    }
    val gapReport = continuousCylinderRadiusOptimizationGapReport(
        source = source,
        radiusWeightFunctionKey = key,
        hasConcreteSelectedRadius = selectedRadius?.value?.toDouble()?.let { it > 0.0 } == true,
        hasDiscreteRadiusCandidates = hasDiscreteRadiusCandidates,
        hasDiscreteRadiusStep = hasDiscreteRadiusStep,
        hasContinuousRadiusInterval = hasRadiusInterval,
        hasContinuousDiameterInterval = hasDiameterInterval
    )
    val referenceRadius = selectedRadius ?: radiusMin ?: radiusMax
    val lowerBound = radiusMin ?: diameterMin?.toContinuousRadiusBoundFromDiameter(referenceRadius)
    val upperBound = radiusMax ?: diameterMax?.toContinuousRadiusBoundFromDiameter(referenceRadius ?: lowerBound)
    return ContinuousCylinderRadiusSolverPrototype(
        source = source,
        radiusWeightFunctionKey = key,
        axis = axis,
        variableName = continuousRadiusVariableName(
            source = source,
            radiusWeightFunctionKey = key,
            axis = axis
        ),
        radiusLowerBound = lowerBound,
        radiusUpperBound = upperBound,
        initialRadius = selectedRadius,
        gaps = gapReport?.gaps ?: emptyList()
    )
}

/**
 * PWL 半径选择元数据。
 * PWL radius selection metadata.
 *
 * 记录 solver 通过 PWL 近似路径选择半径时的诊断信息。
 * Records diagnostic information when the solver selects a radius via the PWL approximation path.
 *
 * @property solverRadiusSquared solver 的近似 r² 值 q / solver's approximate r² value q
 * @property actualRadiusSquared 真实 r² 值 / actual r² value
 * @property pwlAbsoluteError PWL 绝对误差 |q - r²| / PWL absolute error
 * @property pwlRelativeError PWL 相对误差 |q - r²| / r² / PWL relative error
 * @property maxPWLRelativeError PWL 函数的最大相对误差 / maximum relative error of the PWL function
 * @property numSegments PWL 分段数 / number of PWL segments
 * @property isWithinEnvelope 是否在保守 envelope 范围内 / whether within conservative envelope range
 * @property selectionSource 选择来源（"pwl"）/ selection source ("pwl")
 */
data class PWLRadiusSelectionMetadata(
    val solverRadiusSquared: FltX,
    val actualRadiusSquared: FltX,
    val pwlAbsoluteError: FltX,
    val pwlRelativeError: FltX,
    val maxPWLRelativeError: FltX,
    val numSegments: Int,
    val isWithinEnvelope: Boolean,
    val selectionSource: String = "pwl"
) {
    init {
        require(numSegments >= 1) { "numSegments must be at least 1" }
        require(selectionSource.isNotBlank()) { "selectionSource must not be blank" }
    }

    /**
     * 计算真实圆柱体积（使用 solver 选择的半径）。
     * Compute actual cylinder volume using solver-selected radius.
     */
    fun actualVolume(height: FltX, pi: FltX): FltX {
        return pi * actualRadiusSquared * height
    }

    /**
     * 计算 PWL 近似体积（使用 q ≈ r²）。
     * Compute PWL approximate volume using q ≈ r².
     */
    fun pwlVolume(height: FltX, pi: FltX): FltX {
        return pi * solverRadiusSquared * height
    }
}

/**
 * 连续半径已选择结果。
 * Selected continuous-radius result.
 *
 * @property key 半径权重函数键 / radius weight function key
 * @property variableName solver 半径变量名（可选） / solver radius variable name (optional)
 * @property source solver 原型来源（可选） / solver prototype source (optional)
 * @property selectedRadius 已选择半径 / selected radius
 * @property axis 圆柱轴向 / cylinder axis
 * @property radiusMin 半径下界（可选） / radius lower bound (optional)
 * @property radiusMax 半径上界（可选） / radius upper bound (optional)
 */
data class CylinderRadiusSelectionResult(
    val key: String,
    val selectedRadius: Quantity<FltX>,
    val axis: Axis3,
    val radiusMin: Quantity<FltX>? = null,
    val radiusMax: Quantity<FltX>? = null,
    val variableName: String? = null,
    val source: String? = null,
    /** PWL 近似元数据（仅当通过 PWL 路径选择半径时非空） / PWL approximation metadata (non-null only when radius is selected via PWL path) */
    val pwlMetadata: PWLRadiusSelectionMetadata? = null
) {
    init {
        require(key.isNotBlank()) {
            "Cylinder radius selection key must not be blank."
        }
        variableName?.let {
            require(it.isNotBlank()) {
                "Cylinder radius selection variable name must not be blank."
            }
        }
        source?.let {
            require(it.isNotBlank()) {
                "Cylinder radius selection source must not be blank."
            }
        }
        require(selectedRadius.value.toDouble() > 0.0) {
            "Cylinder selected radius must be positive."
        }
    }
}

/**
 * 构建连续半径 solver 原型的稳定 item 来源。
 * Build a stable item source for continuous-radius solver prototypes.
 *
 * @param item 货物 / item
 * @return solver 原型来源 / solver prototype source
 */
fun continuousCylinderRadiusSolverSource(item: Item): String {
    val itemId = when (item) {
        is ActualItem -> item.id
        else -> if (item.indexed) {
            "indexed_${item.index}"
        } else {
            item.toString()
        }
    }
    return "ColumnGenerationState.item.$itemId"
}

/**
 * 获取连续半径已选择结果。
 * Get selected continuous-radius result.
 *
 * @return 已选择半径结果；未使用连续半径 key 时返回 null / selected radius result, or null when no continuous-radius key is used
 */
fun PackageShapeSpec.VerticalCylinder.continuousRadiusSelectionResult(): CylinderRadiusSelectionResult? {
    val key = radiusWeightFunctionKey ?: return null
    return CylinderRadiusSelectionResult(
        key = key,
        selectedRadius = radius,
        axis = axis,
        radiusMin = radiusMin,
        radiusMax = radiusMax
    )
}

/**
 * 获取连续半径 solver 原生变量原型。
 * Get the solver-native continuous-radius variable prototype.
 *
 * @param source 调用来源 / call source
 * @return solver 原生变量原型；未使用连续半径 key 时返回 null / solver-native variable prototype, or null when no continuous-radius key is used
 */
fun PackageShapeSpec.VerticalCylinder.continuousRadiusSolverPrototype(
    source: String = "PackageShapeSpec.VerticalCylinder"
): ContinuousCylinderRadiusSolverPrototype? {
    val selection = continuousRadiusSelectionResult()
    return continuousCylinderRadiusSolverPrototype(
        source = source,
        radiusWeightFunctionKey = radiusWeightFunctionKey,
        axis = axis,
        selectedRadius = selection?.selectedRadius,
        radiusMin = radiusMin,
        radiusMax = radiusMax,
        diameterMin = diameterMin,
        diameterMax = diameterMax,
        hasDiscreteRadiusCandidates = radiusCandidates.isNotEmpty(),
        hasDiscreteRadiusStep = radiusStep != null || diameterStep != null
    )
}

/**
 * 圆柱进入仅长方体路径未开放错误信息。
 * Unsupported cylinder message for cuboid-only paths.
 *
 * @param source 调用来源 / call source
 * @param pathPredicate 路径谓词描述 / path predicate description
 * @return 错误信息 / error message
 */
fun unsupportedCylinderCuboidOnlyPathMessage(source: String, pathPredicate: String): String {
    return "Unsupported cylinder in $source: $pathPredicate cuboid-only and does not provide verified cylinder geometry yet."
}

/**
 * 判断物品集合是否包含圆柱。
 * Return whether the item collection contains a cylinder.
 *
 * @param items 待检查物品 / items to check
 * @return 是否包含圆柱 / whether a cylinder exists
 */
fun hasCylinderItem(items: Iterable<Item>): Boolean {
    return items.any { item ->
        item.packingShape is CylinderPackingShape3
    }
}

/**
 * 判断形状规格是否请求连续半径优化。
 * Return whether shape spec requests continuous radius optimization.
 *
 * @param spec 包装形状规格 / package shape spec
 * @return 是否请求连续半径优化 / whether continuous radius optimization is requested
 */
fun hasContinuousCylinderRadiusOptimization(spec: PackageShapeSpec): Boolean {
    return spec is PackageShapeSpec.VerticalCylinder && spec.radiusWeightFunctionKey != null
}

/**
 * 要求生产装箱形状具备确定的圆柱半径。
 * Require production packing shape to have a concrete cylinder radius.
 *
 * @param spec 包装形状规格 / package shape spec
 * @param source 调用来源 / call source
 */
fun requireConcreteCylinderRadiusProductionMetadata(
    spec: PackageShapeSpec,
    source: String
) {
    if (spec is PackageShapeSpec.VerticalCylinder && hasContinuousCylinderRadiusOptimization(spec)) {
        val selection = spec.continuousRadiusSelectionResult()
        val gapReport = continuousCylinderRadiusOptimizationGapReport(
            source = source,
            radiusWeightFunctionKey = spec.radiusWeightFunctionKey,
            hasConcreteSelectedRadius = selection?.selectedRadius?.value?.toDouble()?.let { it > 0.0 } == true
        )
        if (gapReport != null) {
            throw IllegalArgumentException(gapReport.message())
        }
    }
}

/**
 * 要求圆柱为当前默认候选路径支持的竖直轴向。
 * Require the cylinder axis supported by current default candidate paths.
 *
 * @param shape 装箱形状 / packing shape
 * @param source 调用来源 / call source
 */
fun requireVerticalCylinderAxis(
    shape: PackingShape3<FltX>,
    source: String
) {
    if (shape is CylinderPackingShape3 && shape.axis != Axis3.Y) {
        throw IllegalArgumentException(unsupportedCylinderAxisMessage(source = source, axis = shape.axis))
    }
}

/**
 * 要求圆柱为当前默认候选路径支持的竖直轴向。
 * Require the cylinder axis supported by current default candidate paths.
 *
 * @param shape 装箱形状 / packing shape
 * @param path 能力路径 / capability path
 */
fun requireVerticalCylinderAxis(
    shape: PackingShape3<FltX>,
    path: CylinderCapabilityPath
) {
    require(path.status == CylinderCapabilityStatus.VerticalCandidateOnly) {
        "Cylinder capability path ${path.name} is not a vertical candidate path."
    }
    requireVerticalCylinderAxis(
        shape = shape,
        source = path.source
    )
}

/**
 * 要求圆柱为轴向感知候选路径支持的轴向。
 * Require the cylinder axis supported by axis-aware candidate paths.
 *
 * @param shape 装箱形状 / packing shape
 * @param path 能力路径 / capability path
 */
fun requireAxisAwareCylinderCandidate(
    shape: PackingShape3<FltX>,
    path: CylinderCapabilityPath
) {
    require(path.status == CylinderCapabilityStatus.AxisAwareCandidate) {
        "Cylinder capability path ${path.name} is not an axis-aware candidate path."
    }
    if (shape is CylinderPackingShape3) {
        Axis3.entries.firstOrNull { axis -> axis == shape.axis }
            ?: throw IllegalArgumentException("Unsupported cylinder axis in ${path.source}: got ${shape.axis}.")
    }
}

/**
 * 要求圆柱支撑语义只使用直立 Y 轴圆柱。
 * Require cylinder support semantics to use upright Y-axis cylinders only.
 *
 * @param shape 装箱形状 / packing shape
 * @param orientation 物品朝向 / item orientation
 * @param source 调用来源 / call source
 */
fun requireUprightVerticalCylinderSupport(
    shape: PackingShape3<FltX>,
    orientation: Orientation,
    source: String
) {
    if (shape is CylinderPackingShape3 && (shape.axis != Axis3.Y || orientation != Orientation.Upright)) {
        throw IllegalArgumentException(unsupportedCylinderStackingSupportMessage(source))
    }
}

/**
 * 要求圆柱支撑语义只使用直立 Y 轴圆柱。
 * Require cylinder support semantics to use upright Y-axis cylinders only.
 *
 * @param shape 装箱形状 / packing shape
 * @param orientation 物品朝向 / item orientation
 * @param path 能力路径 / capability path
 */
fun requireUprightVerticalCylinderSupport(
    shape: PackingShape3<FltX>,
    orientation: Orientation,
    path: CylinderCapabilityPath
) {
    require(path.status == CylinderCapabilityStatus.UprightVerticalSupportOnly) {
        "Cylinder capability path ${path.name} is not an upright vertical support path."
    }
    requireUprightVerticalCylinderSupport(
        shape = shape,
        orientation = orientation,
        source = path.source
    )
}

/**
 * 要求简单块生成只接收当前已验证的圆柱能力。
 * Require simple block generation to accept only currently verified cylinder capability.
 *
 * @param item 物品 / item
 * @param source 调用来源 / call source
 */
fun requireSupportedCylinderItemForSimpleBlock(item: Item, source: String) {
    val shape = item.packingShape
    if (shape !is CylinderPackingShape3) {
        return
    }
    requireVerticalCylinderAxis(shape = shape, source = source)
    val unsupportedOrientations = item.enabledOrientations.filter { it.category != OrientationCategory.Upright }
    if (unsupportedOrientations.isNotEmpty()) {
        throw IllegalArgumentException(unsupportedCylinderOrientationMessage(source))
    }
    if (item.enabledSideOnTop || item.enabledLieOnTop) {
        throw IllegalArgumentException(unsupportedCylinderTopLayerPolicyMessage(source))
    }
}

/**
 * 要求简单块生成只接收当前已验证的圆柱能力。
 * Require simple block generation to accept only currently verified cylinder capability.
 *
 * @param item 物品 / item
 * @param path 能力路径 / capability path
 */
fun requireSupportedCylinderItemForSimpleBlock(item: Item, path: CylinderCapabilityPath) {
    require(path == CylinderCapabilityPath.SimpleBlockCandidate) {
        "Cylinder capability path ${path.name} is not the simple block candidate path."
    }
    requireSupportedCylinderItemForSimpleBlock(
        item = item,
        source = path.source
    )
}

/**
 * 要求仅长方体路径不接收圆柱物品。
 * Require cuboid-only paths to reject cylinder items.
 *
 * @param items 待检查物品 / items to check
 * @param source 调用来源 / call source
 * @param pathPredicate 路径谓词描述 / path predicate description
 */
fun requireNoCylinderItemsForCuboidOnlyPath(
    items: Iterable<Item>,
    source: String,
    pathPredicate: String
) {
    if (hasCylinderItem(items)) {
        throw IllegalArgumentException(
            unsupportedCylinderCuboidOnlyPathMessage(
                source = source,
                pathPredicate = pathPredicate
            )
        )
    }
}

/**
 * 要求仅长方体路径不接收圆柱物品。
 * Require cuboid-only paths to reject cylinder items.
 *
 * @param items 待检查物品 / items to check
 * @param path 能力路径 / capability path
 */
fun requireNoCylinderItemsForCuboidOnlyPath(
    items: Iterable<Item>,
    path: CylinderCapabilityPath
) {
    require(path.status == CylinderCapabilityStatus.CuboidOnly && path.pathPredicate != null) {
        "Cylinder capability path ${path.name} is not a cuboid-only path."
    }
    requireNoCylinderItemsForCuboidOnlyPath(
        items = items,
        source = path.source,
        pathPredicate = path.pathPredicate
    )
}

/**
 * 从 solver 结果构建连续半径已选择结果。
 * Build selected continuous-radius result from solver output.
 *
 * @receiver 连续半径 solver 原生变量原型 / continuous-radius solver variable prototype
 * @param solverRadius solver 选出的半径 / solver-selected radius
 * @return 已选择半径结果 / selected radius result
 */
fun ContinuousCylinderRadiusSolverPrototype.withSolverSelectedRadius(
    solverRadius: Quantity<FltX>
): CylinderRadiusSelectionResult {
    require(radiusWeightFunctionKey != null) {
        "Continuous cylinder radius solver prototype must have a radius weight function key to build a selection result."
    }
    return CylinderRadiusSelectionResult(
        key = radiusWeightFunctionKey,
        variableName = variableName,
        source = source,
        selectedRadius = solverRadius,
        axis = axis,
        radiusMin = radiusLowerBound,
        radiusMax = radiusUpperBound
    )
}

/**
 * 从 PWL solver 结果构建连续半径已选择结果。
 * Build selected continuous-radius result from PWL solver output.
 *
 * @receiver 连续半径 solver 原生变量原型 / continuous-radius solver variable prototype
 * @param solverRadius solver 选出的半径 r / solver-selected radius r
 * @param pwlMetadata PWL 近似元数据 / PWL approximation metadata
 * @return 已选择半径结果（包含 PWL 元数据）/ selected radius result (with PWL metadata)
 */
fun ContinuousCylinderRadiusSolverPrototype.withPWLSolverSelectedRadius(
    solverRadius: Quantity<FltX>,
    pwlMetadata: PWLRadiusSelectionMetadata
): CylinderRadiusSelectionResult {
    require(radiusWeightFunctionKey != null) {
        "Continuous cylinder radius solver prototype must have a radius weight function key to build a selection result."
    }
    return CylinderRadiusSelectionResult(
        key = radiusWeightFunctionKey,
        variableName = variableName,
        source = source,
        selectedRadius = solverRadius,
        axis = axis,
        radiusMin = radiusLowerBound,
        radiusMax = radiusUpperBound,
        pwlMetadata = pwlMetadata
    )
}
