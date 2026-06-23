/**
 * 包装模型。
 * Package model.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.item.model

import kotlin.math.*
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.geometry.Axis3
import fuookami.ospf.kotlin.math.Scale
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.quantities.quantity.minus
import fuookami.ospf.kotlin.quantities.quantity.plus
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

/**
 * 包装底面形状，包含宽度、深度、重量和包装类型。
 * Package bottom shape, including width, depth, weight and package type.
 *
 * @param V 数值类型 / numeric type
 */
data class PackageBottomShape<V : FloatingNumber<V>>(
    val width: Quantity<V>,
    val depth: Quantity<V>,
    val weight: Quantity<V>,
    val packageType: fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PackageType,
) : Eq<PackageBottomShape<V>> {
    val packageCategory by packageType::category
    val area: Quantity<V> = width * depth

    fun new(
        width: Quantity<V>? = null,
        depth: Quantity<V>? = null,
        weight: Quantity<V>? = null,
        packageType: fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PackageType? = null
    ): PackageBottomShape<V> {
        return PackageBottomShape(
            width = width ?: this.width,
            depth = depth ?: this.depth,
            weight = weight ?: this.weight,
            packageType = packageType ?: this.packageType
        )
    }

    override fun partialEq(rhs: PackageBottomShape<V>): Boolean {
        return packageType == rhs.packageType
    }

    override fun eq(rhs: PackageBottomShape<V>): Boolean {
        if (width neq rhs.width) return false
        if (depth neq rhs.depth) return false
        if (weight neq rhs.weight) return false

        return packageType == rhs.packageType
    }

    @Suppress("UNCHECKED_CAST")
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PackageBottomShape<*>) return false
        if (weight.value::class != other.weight.value::class) return false

        return this eq (other as PackageBottomShape<V>)
    }

    override fun hashCode(): Int {
        var result = width.hashCode()
        result = 31 * result + depth.hashCode()
        result = 31 * result + weight.hashCode()
        result = 31 * result + packageType.hashCode()
        return result
    }
}

/**
 * 包装形状，包含宽度、高度、深度、重量、包装类型和形状规格。
 * Package shape, including width, height, depth, weight, package type and shape spec.
 *
 * @param V 数值类型 / numeric type
 */
data class PackageShape<V : FloatingNumber<V>>(
    val width: Quantity<V>,
    val height: Quantity<V>,
    val depth: Quantity<V>,
    val weight: Quantity<V>,
    val packageType: fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PackageType,
    val shapeSpec: PackageShapeSpec = PackageShapeSpec.Cuboid
) : Eq<PackageShape<V>> {
    val bottomShape = PackageBottomShape(
        width = width,
        depth = depth,
        weight = weight,
        packageType = packageType
    )
    val packageCategory by packageType::category
    val volume: Quantity<V> = width * height * depth

    fun new(
        width: Quantity<V>? = null,
        height: Quantity<V>? = null,
        depth: Quantity<V>? = null,
        weight: Quantity<V>? = null,
        packageType: fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PackageType? = null,
        shapeSpec: PackageShapeSpec? = null
    ): PackageShape<V> {
        return PackageShape(
            width = width ?: this.width,
            height = height ?: this.height,
            depth = depth ?: this.depth,
            weight = weight ?: this.weight,
            packageType = packageType ?: this.packageType,
            shapeSpec = shapeSpec ?: this.shapeSpec
        )
    }

    override fun partialEq(rhs: PackageShape<V>): Boolean {
        return packageType == rhs.packageType
    }

    override fun eq(rhs: PackageShape<V>): Boolean {
        if (width neq rhs.width) return false
        if (height neq rhs.height) return false
        if (depth neq rhs.depth) return false
        if (weight neq rhs.weight) return false

        return packageType == rhs.packageType
                && shapeSpec == rhs.shapeSpec
    }

    @Suppress("UNCHECKED_CAST")
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PackageShape<*>) return false
        if (weight.value::class != other.weight.value::class) return false

        return this eq (other as PackageShape<V>)
    }

    override fun hashCode(): Int {
        var result = width.hashCode()
        result = 31 * result + height.hashCode()
        result = 31 * result + depth.hashCode()
        result = 31 * result + weight.hashCode()
        result = 31 * result + packageType.hashCode()
        result = 31 * result + shapeSpec.hashCode()
        return result
    }
}

/**
 * 包装形状规格，区分长方体和垂直圆柱。
 * Package shape spec, distinguishing cuboid and vertical cylinder.
 */
sealed interface PackageShapeSpec {
    /** 长方体形状规格 / Cuboid shape spec */
    data object Cuboid : PackageShapeSpec

    /**
     * 垂直圆柱形状规格，支持连续半径选择。
     * Vertical cylinder shape spec, supporting continuous radius selection.
     *
     * @property radius 圆柱半径 / cylinder radius
     * @property axis 圆柱轴向 / cylinder axis
     * @property radiusCandidates 候选半径列表 / candidate radius list
     * @property radiusMin 最小半径 / minimum radius
     * @property radiusMax 最大半径 / maximum radius
     * @property radiusWeightFunctionKey 半径权重函数键 / radius weight function key
     * @property radiusStep 半径步长 / radius step
     * @property diameterMin 最小直径 / minimum diameter
     * @property diameterMax 最大直径 / maximum diameter
     * @property diameterStep 直径步长 / diameter step
     */
    data class VerticalCylinder(
        val radius: Quantity<FltX>,
        val axis: Axis3 = Axis3.Y,
        val radiusCandidates: List<Quantity<FltX>> = emptyList(),
        val radiusMin: Quantity<FltX>? = null,
        val radiusMax: Quantity<FltX>? = null,
        val radiusWeightFunctionKey: String? = null,
        val radiusStep: Quantity<FltX>? = null,
        val diameterMin: Quantity<FltX>? = null,
        val diameterMax: Quantity<FltX>? = null,
        val diameterStep: Quantity<FltX>? = null
    ) : PackageShapeSpec {
        val resolvedRadiusCandidates: List<Quantity<FltX>> = resolveVerticalCylinderRadiusCandidates(
            radius = radius,
            radiusCandidates = radiusCandidates,
            radiusMin = radiusMin,
            radiusMax = radiusMax,
            radiusStep = radiusStep,
            diameterMin = diameterMin,
            diameterMax = diameterMax,
            diameterStep = diameterStep
        )

        init {
            val normalizedRadius = radius.toPositiveQuantityOrRequire(radius.unit, "radius")
            val normalizedRadiusMin = radiusMin?.toPositiveQuantityOrRequire(radius.unit, "radiusMin")
            val normalizedRadiusMax = radiusMax?.toPositiveQuantityOrRequire(radius.unit, "radiusMax")
            if (radiusCandidates.isNotEmpty()) {
                require(resolvedRadiusCandidates.any { it sameCylinderRadiusValue radius }) {
                    "Resolved radius must be included in radius candidates when candidates are provided."
                }
            }
            if (normalizedRadiusMin != null && normalizedRadiusMax != null) {
                require(normalizedRadiusMin.value.toDouble() <= normalizedRadiusMax.value.toDouble()) {
                    "Vertical cylinder radiusMin must be less than or equal to radiusMax."
                }
            }
            normalizedRadiusMin?.let {
                require(normalizedRadius.value.toDouble() >= it.value.toDouble()) {
                    "Resolved radius must be greater than or equal to radiusMin."
                }
            }
            normalizedRadiusMax?.let {
                require(normalizedRadius.value.toDouble() <= it.value.toDouble()) {
                    "Resolved radius must be less than or equal to radiusMax."
                }
            }
            diameterMin?.let {
                val minimumRadius = it.toRadiusQuantity(radius.unit, "diameterMin")
                require(normalizedRadius.value.toDouble() >= minimumRadius.value.toDouble()) {
                    "Resolved radius diameter must be greater than or equal to diameterMin."
                }
            }
            diameterMax?.let {
                val maximumRadius = it.toRadiusQuantity(radius.unit, "diameterMax")
                require(normalizedRadius.value.toDouble() <= maximumRadius.value.toDouble()) {
                    "Resolved radius diameter must be less than or equal to diameterMax."
                }
            }
        }
    }
}

private const val CylinderRadiusCandidateTolerance = 1e-9

private infix fun Quantity<FltX>.sameCylinderRadiusValue(rhs: Quantity<FltX>): Boolean {
    val converted = rhs.convertTo(unit) ?: return false
    return abs(value.toDouble() - converted.value.toDouble()) <= CylinderRadiusCandidateTolerance
}

/** 转换为正物理量 / Convert to positive quantity */
private fun Quantity<FltX>.toPositiveQuantity(
    unit: PhysicalUnit,
    fieldName: String
): Ret<Quantity<FltX>> {
    val converted = convertTo(unit)
        ?: return Failed(ErrorCode.IllegalArgument, "Vertical cylinder $fieldName must use a length-compatible unit.")
    if (converted.value.toDouble() <= 0.0) {
        return Failed(ErrorCode.IllegalArgument, "Vertical cylinder $fieldName must be positive.")
    }
    return ok(converted)
}

/**
 * 从 Ret 结果中提取成功值，若为失败则抛出 require 异常。
 * Extract the success value from a Ret result; throws a require exception if it is a failure.
 *
 * @return 成功时的值 / the value on success
 */
private fun <T> Ret<T>.requireValue(): T {
    val result = this
    require(result is Ok) {
        when (result) {
            is Failed -> result.error.message
            is Fatal -> result.errors.joinToString("; ") { it.message }
            else -> "Unexpected failure."
        }
    }
    return result.value
}

/**
 * 将量转换到指定单位并要求为正值，转换或验证失败时抛出 require 异常。
 * Convert a quantity to the specified unit and require it to be positive; throws a require exception on conversion or validation failure.
 *
 * @param unit 目标物理单位 / target physical unit
 * @param fieldName 字段名（用于错误消息） / field name (used in error messages)
 * @return 转换后的正值量 / the converted positive quantity
 */
private fun Quantity<FltX>.toPositiveQuantityOrRequire(
    unit: PhysicalUnit,
    fieldName: String
): Quantity<FltX> {
    return toPositiveQuantity(unit, fieldName).requireValue()
}

private fun Quantity<FltX>.toRadiusQuantity(
    unit: PhysicalUnit,
    fieldName: String
): Quantity<FltX> {
    val converted = toPositiveQuantityOrRequire(unit, fieldName)
    return Quantity(FltX(converted.value.toDouble() / 2.0), unit)
}

private fun distinctSortedRadiusCandidates(
    candidates: List<Quantity<FltX>>
): List<Quantity<FltX>> {
    val sorted = candidates.sortedWith { lhs, rhs ->
        lhs.value.toDouble().compareTo(rhs.value.toDouble())
    }
    val distinct = ArrayList<Quantity<FltX>>()
    for (candidate in sorted) {
        if (distinct.none { it sameCylinderRadiusValue candidate }) {
            distinct.add(candidate)
        }
    }
    return distinct
}

private fun intervalCandidates(
    min: Quantity<FltX>,
    max: Quantity<FltX>,
    step: Quantity<FltX>,
    unit: PhysicalUnit,
    fieldPrefix: String
): List<Quantity<FltX>> {
    val normalizedMin = min.toPositiveQuantityOrRequire(unit, "${fieldPrefix}Min")
    val normalizedMax = max.toPositiveQuantityOrRequire(unit, "${fieldPrefix}Max")
    val normalizedStep = step.toPositiveQuantityOrRequire(unit, "${fieldPrefix}Step")
    val minValue = normalizedMin.value.toDouble()
    val maxValue = normalizedMax.value.toDouble()
    val stepValue = normalizedStep.value.toDouble()
    require(minValue <= maxValue + CylinderRadiusCandidateTolerance) {
        "Vertical cylinder ${fieldPrefix}Min must be less than or equal to ${fieldPrefix}Max."
    }

    val values = ArrayList<Double>()
    values.add(minValue)
    while (values.last() + stepValue < maxValue - CylinderRadiusCandidateTolerance) {
        values.add(values.last() + stepValue)
        require(values.size <= 100000) {
            "Vertical cylinder $fieldPrefix interval generates too many radius candidates."
        }
    }
    if (abs(values.last() - maxValue) > CylinderRadiusCandidateTolerance) {
        values.add(maxValue)
    }
    return values.map { Quantity(FltX(it), unit) }
}

private fun diameterIntervalRadiusCandidates(
    min: Quantity<FltX>,
    max: Quantity<FltX>,
    step: Quantity<FltX>,
    unit: PhysicalUnit
): List<Quantity<FltX>> {
    return intervalCandidates(
        min = min,
        max = max,
        step = step,
        unit = unit,
        fieldPrefix = "diameter"
    ).map {
        Quantity(FltX(it.value.toDouble() / 2.0), unit)
    }
}

/** 解析垂直圆柱半径候选值 / Resolve vertical cylinder radius candidates */
private fun resolveVerticalCylinderRadiusCandidates(
    radius: Quantity<FltX>,
    radiusCandidates: List<Quantity<FltX>>,
    radiusMin: Quantity<FltX>?,
    radiusMax: Quantity<FltX>?,
    radiusStep: Quantity<FltX>?,
    diameterMin: Quantity<FltX>?,
    diameterMax: Quantity<FltX>?,
    diameterStep: Quantity<FltX>?
): List<Quantity<FltX>> {
    val normalizedRadius = radius.toPositiveQuantityOrRequire(radius.unit, "radius")
    val normalizedRadiusMin = radiusMin?.toPositiveQuantityOrRequire(radius.unit, "radiusMin")
    val normalizedRadiusMax = radiusMax?.toPositiveQuantityOrRequire(radius.unit, "radiusMax")
    val normalizedRadiusStep = radiusStep?.toPositiveQuantityOrRequire(radius.unit, "radiusStep")
    val normalizedDiameterMin = diameterMin?.toPositiveQuantityOrRequire(radius.unit, "diameterMin")
    val normalizedDiameterMax = diameterMax?.toPositiveQuantityOrRequire(radius.unit, "diameterMax")
    val normalizedDiameterStep = diameterStep?.toPositiveQuantityOrRequire(radius.unit, "diameterStep")

    if (normalizedRadiusMin != null && normalizedRadiusMax != null) {
        require(normalizedRadiusMin.value.toDouble() <= normalizedRadiusMax.value.toDouble()) {
            "Vertical cylinder radiusMin must be less than or equal to radiusMax."
        }
    }
    if (normalizedDiameterMin != null && normalizedDiameterMax != null) {
        require(normalizedDiameterMin.value.toDouble() <= normalizedDiameterMax.value.toDouble()) {
            "Vertical cylinder diameterMin must be less than or equal to diameterMax."
        }
    }
    if (normalizedRadiusStep != null) {
        require(normalizedRadiusMin != null && normalizedRadiusMax != null) {
            "Vertical cylinder radiusStep requires radiusMin and radiusMax."
        }
    }
    if (normalizedDiameterStep != null) {
        require(normalizedDiameterMin != null && normalizedDiameterMax != null) {
            "Vertical cylinder diameterStep requires diameterMin and diameterMax."
        }
    }

    val hasExplicitCandidates = radiusCandidates.isNotEmpty()
    val hasRadiusInterval = normalizedRadiusMin != null && normalizedRadiusMax != null && normalizedRadiusStep != null
    val hasDiameterInterval = normalizedDiameterMin != null && normalizedDiameterMax != null && normalizedDiameterStep != null
    require(hasExplicitCandidates || !hasRadiusInterval || !hasDiameterInterval) {
        "Vertical cylinder radius interval and diameter interval cannot both generate candidates."
    }

    return when {
        hasExplicitCandidates -> distinctSortedRadiusCandidates(
            radiusCandidates.map { it.toPositiveQuantityOrRequire(radius.unit, "radiusCandidates") }
        )

        normalizedRadiusMin != null && normalizedRadiusMax != null && normalizedRadiusStep != null -> intervalCandidates(
            min = normalizedRadiusMin,
            max = normalizedRadiusMax,
            step = normalizedRadiusStep,
            unit = radius.unit,
            fieldPrefix = "radius"
        )

        normalizedDiameterMin != null && normalizedDiameterMax != null && normalizedDiameterStep != null -> diameterIntervalRadiusCandidates(
            min = normalizedDiameterMin,
            max = normalizedDiameterMax,
            step = normalizedDiameterStep,
            unit = radius.unit
        )

        else -> listOf(normalizedRadius)
    }
}

private fun PackageShape<FltX>.cylinderAxisLength(axis: Axis3): Quantity<FltX> {
    return when (axis) {
        Axis3.X -> width
        Axis3.Y -> height
        Axis3.Z -> depth
    }
}

fun PackageShape<FltX>.toPackingShapeOrNull(): PackingShape3<FltX>? {
    val shapeWeight = weight
    return when (val spec = shapeSpec) {
        PackageShapeSpec.Cuboid -> null
        is PackageShapeSpec.VerticalCylinder -> {
            requireConcreteCylinderRadiusProductionMetadata(
                spec = spec,
                source = "PackageShape.toPackingShapeOrNull"
            )!!
            val shapeHeight = cylinderAxisLength(spec.axis)
            CylinderPackingShape3(
                cylinder = object : AbstractCylinder<FltX> {
                    override val radius = spec.radius
                    override val height = shapeHeight
                    override val axis = spec.axis
                    override val weight = shapeWeight
                }
            )
        }
    }
}

data class PackingProgramMaterialValue(
    val amount: UInt64? = null,
    val weight: Quantity<*>? = null
) {
    init {
        require(amount != null || weight != null) { "amount and weight cannot both be null." }
    }
}

@Suppress("UNCHECKED_CAST")
/** 将装箱程序重量转换为 FltX 物理量 / Convert packing program weight to FltX quantity */
private fun packingProgramWeightToFltXQuantity(value: Quantity<*>): Ret<Quantity<FltX>> {
    return when (value.value) {
        is FltX -> ok(value as Quantity<FltX>)
        else -> Failed(ErrorCode.IllegalArgument, "Unsupported packing material quantity scalar: ${value.value}")
    }
}

@Suppress("UNCHECKED_CAST")
/** 加上装箱程序重量 / Add packing program weight */
private fun plusPackingProgramWeight(
    lhs: Quantity<*>?,
    rhs: Quantity<*>?
): Ret<Quantity<*>?> {
    if (lhs == null) {
        return ok(rhs)
    }
    if (rhs == null) {
        return ok(lhs)
    }
    return when (lhs.value) {
        is FltX -> {
            when (val rhsQuantity = packingProgramWeightToFltXQuantity(rhs)) {
                is Ok -> {
                    when (val sum = (lhs as Quantity<FltX>).plusSafe(rhsQuantity.value)) {
                        is Ok -> ok(sum.value)
                        is Failed -> Failed(sum.error)
                        is Fatal -> Fatal(sum.errors)
                    }
                }
                is Failed -> Failed(rhsQuantity.error)
                is Fatal -> Fatal(rhsQuantity.errors)
            }
        }
        else -> Failed(ErrorCode.IllegalArgument, "Unsupported packing material quantity scalar: ${lhs.value}")
    }
}

/** 合并装箱程序材料值 / Merge packing program material value */
private fun mergePackingProgramMaterialValue(
    lhs: PackingProgramMaterialValue?,
    rhs: PackingProgramMaterialValue
): Ret<PackingProgramMaterialValue> {
    val amount = when {
        lhs?.amount == null -> rhs.amount
        rhs.amount == null -> lhs.amount
        else -> lhs.amount + rhs.amount
    }
    val weight = when (val result = plusPackingProgramWeight(lhs?.weight, rhs.weight)) {
        is Ok -> result.value
        is Failed -> return Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }
    return ok(PackingProgramMaterialValue(
        amount = amount,
        weight = weight
    ))
}

/**
 * 合并包装方案的物料贡献值。
 * Merge material contribution values for packing programs.
 *
 * @param lhs existing contribution, nullable when the material is first seen.
 * @param rhs contribution to append.
 * @return merged contribution.
 */
fun mergePackingProgramMaterialValues(
    lhs: PackingProgramMaterialValue?,
    rhs: PackingProgramMaterialValue
): Ret<PackingProgramMaterialValue> {
    return mergePackingProgramMaterialValue(lhs, rhs)
}

private enum class PackingProgramMaterialDomain {
    Discrete,
    Continuous
}

private object PackingProgramCountUnit : PhysicalUnit() {
    @Suppress("unused")
    fun getDomain(): String = "Discrete"

    override val name = "count"
    override val symbol = "cnt"
    override val quantity = QuantityUnit(name = "count", symbol = "cnt").quantity
    override val conversionRule = UnitConversionRule.Linear(Scale())
}

private fun resolvePackingProgramDomain(unit: PhysicalUnit): PackingProgramMaterialDomain {
    val domainRaw = runCatching {
        unit.javaClass.methods
            .firstOrNull { it.name == "getDomain" && it.parameterCount == 0 }
            ?.invoke(unit)
            ?.toString()
    }.getOrNull()
    return when {
        domainRaw.equals("Discrete", ignoreCase = true) -> PackingProgramMaterialDomain.Discrete
        else -> PackingProgramMaterialDomain.Continuous
    }
}

private fun toDiscreteAmount(value: Any): UInt64 {
    val numericValue = when (value) {
        is FltX -> value.toDouble()
        else -> value.toString().toDouble()
    }
    val rounded = ceil(numericValue).toLong()
    return if (rounded <= 0L) {
        UInt64.zero
    } else {
        UInt64(rounded.toULong())
    }
}

/** 装箱程序数据 / Packing program data */
data class PackingProgram<V : FloatingNumber<V>>(
    val shape: PackageShape<V>,
    val pattern: PackagePattern? = null,
    val packages: List<PackingProgram<V>>? = null,
    val materials: Map<MaterialKey, PackingProgramMaterialValue>
) {
    /** 伴生对象 / Companion object */
    companion object {
        fun <V : FloatingNumber<V>> outerPackage(
            shape: PackageShape<V>,
            packages: List<PackingProgram<V>>
        ): Ret<PackingProgram<V>> {
            val materials = LinkedHashMap<MaterialKey, PackingProgramMaterialValue>()
            for (pack in packages) {
                for ((materialNo, value) in pack.materials) {
                    materials[materialNo] = when (val result = mergePackingProgramMaterialValue(
                        lhs = materials[materialNo],
                        rhs = value
                    )) {
                        is Ok -> result.value
                        is Failed -> return Failed(result.error)
                        is Fatal -> return Fatal(result.errors)
                    }
                }
            }
            return ok(PackingProgram(
                shape = shape,
                packages = packages,
                materials = materials,
            ))
        }

        fun innerPackage(
            shape: PackageShape<FltX>,
            materials: Map<MaterialKey, UInt64>
        ): PackingProgram<FltX> {
            return PackingProgram(
                shape = shape,
                materials = materials.mapValues { (_, amount) ->
                    PackingProgramMaterialValue(
                        amount = amount
                    )
                }
            )
        }

        fun innerPackageWithMaterialValues(
            shape: PackageShape<FltX>,
            materials: Map<MaterialKey, PackingProgramMaterialValue>
        ): PackingProgram<FltX> {
            return PackingProgram(
                shape = shape,
                materials = materials
            )
        }

        fun <V : FloatingNumber<V>> innerPackageWithMaterialQuantities(
            shape: PackageShape<V>,
            materials: Map<MaterialKey, Quantity<V>>
        ): PackingProgram<V> {
            return PackingProgram(
                shape = shape,
                materials = materials.mapValues { (_, quantity) ->
                    when (resolvePackingProgramDomain(quantity.unit)) {
                        PackingProgramMaterialDomain.Discrete -> PackingProgramMaterialValue(
                            amount = toDiscreteAmount(quantity.value)
                        )

                        PackingProgramMaterialDomain.Continuous -> PackingProgramMaterialValue(
                            weight = quantity
                        )
                    }
                }
            )
        }
    }

    val classification = if (packages.isNullOrEmpty()) {
        PackageClassification.Inner
    } else {
        PackageClassification.Outer
    }
    val width by shape::width
    val height by shape::height
    val depth by shape::depth
    val weight by shape::weight
    val packageType by shape::packageType
    val packageCategory by shape::packageCategory
    val volume by shape::volume

    fun materialAmounts(): Map<MaterialKey, UInt64> {
        return materials.mapNotNull { (material, value) ->
            val amount = value.amount
            if (amount == null || amount == UInt64.zero) {
                null
            } else {
                Pair(material, amount)
            }
        }.toMap()
    }

    fun materialQuantities(
        amountUnit: PhysicalUnit = PackingProgramCountUnit,
        materialCatalog: Map<MaterialKey, Material<FltX>> = emptyMap()
    ): Map<MaterialKey, Quantity<FltX>> {
        return materials.mapNotNull { (material, value) ->
            val quantity = value.weight?.let { packingProgramWeightToFltXQuantity(it).value!! } ?: value.amount?.let { amount ->
                val unitWeight = materialCatalog[material]?.weight
                if (unitWeight != null) {
                    unitWeight * FltX(amount.toULong().toDouble())
                } else {
                    FltX(amount.toULong().toDouble()) * amountUnit
                }
            }
            if (quantity == null) {
                null
            } else {
                Pair(material, quantity)
            }
        }.toMap()
    }

    fun materialWeights(materialCatalog: Map<MaterialKey, Material<FltX>> = emptyMap()): Map<MaterialKey, Quantity<FltX>> {
        return materials.mapNotNull { (material, value) ->
            val resolvedWeight = value.weight?.let { packingProgramWeightToFltXQuantity(it).value!! } ?: value.amount?.let { amount ->
                val unitWeight = materialCatalog[material]?.weight ?: return@let null
                unitWeight * FltX(amount.toULong().toDouble())
            }
            if (resolvedWeight == null) {
                null
            } else {
                Pair(material, resolvedWeight)
            }
        }.toMap()
    }

    fun materialAmount(material: MaterialKey): UInt64 {
        return materials[material]?.amount ?: UInt64.zero
    }

    fun actualPackage(materials: Map<Material<FltX>, UInt64>, pending: Boolean = false): Package<V> {
        val requiredAmounts = this.materialAmounts()
        return when (classification) {
            PackageClassification.Outer -> {
                val maxPackage = requiredAmounts.minOfOrNull {
                    val material = materials.filterKeys { material -> material.key == it.key }.entries.firstOrNull()
                    (material?.value ?: UInt64.zero) / it.value
                } ?: UInt64.zero
                if (maxPackage != UInt64.zero) {
                    Package.outerPackage(
                        program = this,
                        packages = this.packages!!.map {
                            Package.innerPackage(
                                program = it,
                                materials = it.materialAmounts().map { (materialKey, amount) ->
                                    val material = materials.filterKeys { material -> material.key == materialKey }.entries.firstOrNull()!!
                                    Pair(material.key, amount)
                                }.toMap(),
                                amount = UInt64.one,
                                pending = pending
                            )
                        },
                        amount = maxPackage,
                        pending = pending
                    )
                } else {
                    val packages = ArrayList<Package<V>>()
                    val restMaterials = materials.toMutableMap()
                    for (pack in this.packages!!) {
                        val thisPackageMaterials = pack.materialAmounts().mapNotNull {
                            val material = restMaterials.filterKeys { material -> material.key == it.key }.entries.firstOrNull()
                                ?: return@mapNotNull null
                            Pair(material.key, maxOf(material.value, it.value))
                        }.toMap()

                        if (thisPackageMaterials.values.all { it == UInt64.zero }) {
                            continue
                        }

                        val subPackage = Package.innerPackage(
                            program = pack,
                            materials = thisPackageMaterials,
                            amount = UInt64.one,
                            pending = pending
                        )
                        if (subPackage.materials.values.any { it != UInt64.zero }) {
                            packages.add(subPackage)
                            for ((material, amount) in subPackage.materials) {
                                restMaterials[material] = (restMaterials[material] ?: UInt64.zero) - amount
                            }
                        }
                    }
                    Package.outerPackage(
                        program = this,
                        packages = packages,
                        amount = UInt64.one,
                        pending = pending
                    )
                }
            }

            PackageClassification.Inner -> {
                val maxPackage = requiredAmounts.minOfOrNull {
                    val material = materials.filterKeys { material -> material.key == it.key }.entries.firstOrNull()
                    (material?.value ?: UInt64.zero) / it.value
                } ?: UInt64.zero
                if (maxPackage != UInt64.zero) {
                    Package.innerPackage(
                        program = this,
                        materials = requiredAmounts.map {
                            val material = materials.filterKeys { material -> material.key == it.key }.entries.firstOrNull()!!
                            Pair(material.key, it.value)
                        }.toMap(),
                        amount = maxPackage,
                        pending = pending
                    )
                } else {
                    Package.innerPackage(
                        program = this,
                        materials = requiredAmounts.mapNotNull {
                            val material = materials.filterKeys { material -> material.key == it.key }.entries.firstOrNull()
                                ?: return@mapNotNull null
                            Pair(material.key, maxOf(material.value, it.value))
                        }.toMap(),
                        amount = UInt64.one,
                        pending = pending
                    )
                }
            }
        }
    }
}

open class Package<V : FloatingNumber<V>>(
    val code: PackageCode? = null,
    val pattern: PackagePattern? = null,
    val program: PackingProgram<V>? = null,
    val shape: PackageShape<V>,
    val packages: List<Package<V>>? = null,
    val materials: Map<Material<FltX>, UInt64>,
    val amount: UInt64 = UInt64.one,
    val pending: Boolean = false,
) {
    companion object {
        fun <V : FloatingNumber<V>> outerPackage(
            code: PackageCode? = null,
            pattern: PackagePattern? = null,
            shape: PackageShape<V>,
            packages: List<Package<V>>,
            amount: UInt64 = UInt64.one,
            pending: Boolean = false,
        ): Package<V> {
            val materials = packages.flatMap { it.materials.keys }.associateWith { UInt64.zero }.toMutableMap()
            for (pack in packages) {
                for ((material, materialAmount) in pack.materials) {
                    materials[material] = (materials[material] ?: UInt64.zero) + materialAmount
                }
            }
            return Package(
                code = code,
                pattern = pattern,
                program = null,
                shape = shape,
                packages = packages,
                materials = materials,
                amount = amount,
                pending = pending,
            )
        }

        fun <V : FloatingNumber<V>> outerPackage(
            code: PackageCode? = null,
            pattern: PackagePattern? = null,
            program: PackingProgram<V>,
            packages: List<Package<V>>,
            amount: UInt64 = UInt64.one,
            pending: Boolean = false,
        ): Package<V> {
            assert(pattern == null || program.pattern == null || pattern belong program.pattern)
            val materials = packages.flatMap { it.materials.keys }.associateWith { UInt64.zero }.toMutableMap()
            for (pack in packages) {
                for ((material, materialAmount) in pack.materials) {
                    materials[material] = (materials[material] ?: UInt64.zero) + materialAmount
                }
            }
            return Package(
                code = code,
                pattern = pattern ?: program.pattern,
                program = program,
                shape = program.shape,
                packages = packages,
                materials = materials,
                amount = amount,
                pending = pending,
            )
        }

        fun innerPackage(
            code: PackageCode? = null,
            pattern: PackagePattern? = null,
            shape: PackageShape<FltX>,
            materials: Map<Material<FltX>, UInt64>,
            amount: UInt64 = UInt64.one,
            pending: Boolean = false,
        ): Package<FltX> {
            return Package(
                code = code,
                pattern = pattern,
                shape = shape,
                materials = materials,
                amount = amount,
                pending = pending,
            )
        }

        fun <V : FloatingNumber<V>> innerPackage(
            code: PackageCode? = null,
            pattern: PackagePattern? = null,
            program: PackingProgram<V>,
            materials: Map<Material<FltX>, UInt64>,
            amount: UInt64 = UInt64.one,
            pending: Boolean = false,
        ): Package<V> {
            return Package(
                code = code,
                pattern = pattern ?: program.pattern,
                program = program,
                shape = program.shape,
                materials = materials,
                amount = amount,
                pending = pending,
            )
        }
    }

    val width by shape::width
    val height by shape::height
    val depth by shape::depth
    val weight by shape::weight
    val packageType by shape::packageType
    val packageCategory by shape::packageCategory
    val volume by shape::volume

    open val enabledHoldingAmount: Map<MaterialKey, UInt64>? by lazy {
        program?.let { enabledHoldingAmount(it) }
    }

    open fun enabledHoldingAmount(packingProgram: PackingProgram<*>): Map<MaterialKey, UInt64>? {
        val requiredAmounts = packingProgram.materialAmounts()
        if (!(pattern == null || packingProgram.pattern == null || pattern belong packingProgram.pattern)) {
            return null
        }
        if (materials.keys.any { !requiredAmounts.containsKey(it.key) }) {
            return null
        }
        return requiredAmounts.mapNotNull {
            val amount = materials.entries.find { entry -> entry.key.key == it.key }?.value ?: UInt64.zero
            if (amount >= it.value) {
                null
            } else {
                Pair(it.key, it.value - amount)
            }
        }.toMap()
    }

    open val full by lazy {
        program?.let {
            enabledHoldingAmount(it)?.let { materials ->
                materials.isEmpty() || materials.values.all { amount -> amount == UInt64.zero }
            }
        } == true
    }

    open fun full(packingProgram: PackingProgram<*>): Boolean {
        return enabledHoldingAmount(packingProgram)?.let {
            it.isEmpty() || it.values.all { amount -> amount == UInt64.zero }
        } == true
    }

    open fun contain(materialType: MaterialType?): Boolean {
        return materialType != null && materials.keys.any { it.type == materialType }
    }
}
