@file:Suppress("DEPRECATION")

/**
 * 包装模型。
 * Package model.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.item.model

import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.convertTo
import fuookami.ospf.kotlin.quantities.quantity.neq
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.quantities.quantity.toFltX
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.InfraNumber
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*
import fuookami.ospf.kotlin.quantities.unit.PhysicalUnit
import fuookami.ospf.kotlin.quantities.unit.QuantityUnit
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.Scale
import fuookami.ospf.kotlin.math.geometry.Axis3
import fuookami.ospf.kotlin.utils.functional.Eq
import kotlin.math.abs
import kotlin.math.ceil

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

sealed interface PackageShapeSpec {
    data object Cuboid : PackageShapeSpec

    data class VerticalCylinder(
        val radius: Quantity<InfraNumber>,
        val axis: Axis3 = Axis3.Y,
        val radiusCandidates: List<Quantity<InfraNumber>> = emptyList(),
        val radiusMin: Quantity<InfraNumber>? = null,
        val radiusMax: Quantity<InfraNumber>? = null,
        val radiusWeightFunctionKey: String? = null,
        val radiusStep: Quantity<InfraNumber>? = null,
        val diameterMin: Quantity<InfraNumber>? = null,
        val diameterMax: Quantity<InfraNumber>? = null,
        val diameterStep: Quantity<InfraNumber>? = null
    ) : PackageShapeSpec {
        val resolvedRadiusCandidates: List<Quantity<InfraNumber>> = resolveVerticalCylinderRadiusCandidates(
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
            require(radius.value.toDouble() > 0.0) {
                "Vertical cylinder radius must be positive."
            }
            if (radiusCandidates.isNotEmpty()) {
                require(resolvedRadiusCandidates.any { it sameCylinderRadiusValue radius }) {
                    "Resolved radius must be included in radius candidates when candidates are provided."
                }
            }
            radiusMin?.let {
                require(it.value.toDouble() > 0.0) {
                    "Vertical cylinder radiusMin must be positive."
                }
            }
            radiusMax?.let {
                require(it.value.toDouble() > 0.0) {
                    "Vertical cylinder radiusMax must be positive."
                }
            }
            if (radiusMin != null && radiusMax != null) {
                require(radiusMin.convertTo(radius.unit)!!.value.toDouble() <= radiusMax.convertTo(radius.unit)!!.value.toDouble()) {
                    "Vertical cylinder radiusMin must be less than or equal to radiusMax."
                }
            }
            radiusMin?.let {
                require(radius.value.toDouble() >= it.convertTo(radius.unit)!!.value.toDouble()) {
                    "Resolved radius must be greater than or equal to radiusMin."
                }
            }
            radiusMax?.let {
                require(radius.value.toDouble() <= it.convertTo(radius.unit)!!.value.toDouble()) {
                    "Resolved radius must be less than or equal to radiusMax."
                }
            }
            diameterMin?.let {
                val minimumRadius = it.toRadiusQuantity(radius.unit, "diameterMin")
                require(radius.value.toDouble() >= minimumRadius.value.toDouble()) {
                    "Resolved radius diameter must be greater than or equal to diameterMin."
                }
            }
            diameterMax?.let {
                val maximumRadius = it.toRadiusQuantity(radius.unit, "diameterMax")
                require(radius.value.toDouble() <= maximumRadius.value.toDouble()) {
                    "Resolved radius diameter must be less than or equal to diameterMax."
                }
            }
        }
    }
}

private const val CylinderRadiusCandidateTolerance = 1e-9

private infix fun Quantity<InfraNumber>.sameCylinderRadiusValue(rhs: Quantity<InfraNumber>): Boolean {
    val converted = rhs.convertTo(unit) ?: return false
    return abs(value.toDouble() - converted.value.toDouble()) <= CylinderRadiusCandidateTolerance
}

private fun Quantity<InfraNumber>.toPositiveQuantity(
    unit: PhysicalUnit,
    fieldName: String
): Quantity<InfraNumber> {
    val converted = convertTo(unit)
        ?: throw IllegalArgumentException("Vertical cylinder $fieldName must use a length-compatible unit.")
    require(converted.value.toDouble() > 0.0) {
        "Vertical cylinder $fieldName must be positive."
    }
    return converted
}

private fun Quantity<InfraNumber>.toRadiusQuantity(
    unit: PhysicalUnit,
    fieldName: String
): Quantity<InfraNumber> {
    val converted = toPositiveQuantity(unit, fieldName)
    return Quantity(infraScalar(converted.value.toDouble() / 2.0), unit)
}

private fun distinctSortedRadiusCandidates(
    candidates: List<Quantity<InfraNumber>>
): List<Quantity<InfraNumber>> {
    val sorted = candidates.sortedWith { lhs, rhs ->
        lhs.value.toDouble().compareTo(rhs.value.toDouble())
    }
    val distinct = ArrayList<Quantity<InfraNumber>>()
    for (candidate in sorted) {
        if (distinct.none { it sameCylinderRadiusValue candidate }) {
            distinct.add(candidate)
        }
    }
    return distinct
}

private fun intervalCandidates(
    min: Quantity<InfraNumber>,
    max: Quantity<InfraNumber>,
    step: Quantity<InfraNumber>,
    unit: PhysicalUnit,
    fieldPrefix: String
): List<Quantity<InfraNumber>> {
    val normalizedMin = min.toPositiveQuantity(unit, "${fieldPrefix}Min")
    val normalizedMax = max.toPositiveQuantity(unit, "${fieldPrefix}Max")
    val normalizedStep = step.toPositiveQuantity(unit, "${fieldPrefix}Step")
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
    return values.map { Quantity(infraScalar(it), unit) }
}

private fun diameterIntervalRadiusCandidates(
    min: Quantity<InfraNumber>,
    max: Quantity<InfraNumber>,
    step: Quantity<InfraNumber>,
    unit: PhysicalUnit
): List<Quantity<InfraNumber>> {
    return intervalCandidates(
        min = min,
        max = max,
        step = step,
        unit = unit,
        fieldPrefix = "diameter"
    ).map {
        Quantity(infraScalar(it.value.toDouble() / 2.0), unit)
    }
}

private fun resolveVerticalCylinderRadiusCandidates(
    radius: Quantity<InfraNumber>,
    radiusCandidates: List<Quantity<InfraNumber>>,
    radiusMin: Quantity<InfraNumber>?,
    radiusMax: Quantity<InfraNumber>?,
    radiusStep: Quantity<InfraNumber>?,
    diameterMin: Quantity<InfraNumber>?,
    diameterMax: Quantity<InfraNumber>?,
    diameterStep: Quantity<InfraNumber>?
): List<Quantity<InfraNumber>> {
    radius.toPositiveQuantity(radius.unit, "radius")
    radiusMin?.toPositiveQuantity(radius.unit, "radiusMin")
    radiusMax?.toPositiveQuantity(radius.unit, "radiusMax")
    radiusStep?.toPositiveQuantity(radius.unit, "radiusStep")
    diameterMin?.toPositiveQuantity(radius.unit, "diameterMin")
    diameterMax?.toPositiveQuantity(radius.unit, "diameterMax")
    diameterStep?.toPositiveQuantity(radius.unit, "diameterStep")

    if (radiusMin != null && radiusMax != null) {
        require(radiusMin.convertTo(radius.unit)!!.value.toDouble() <= radiusMax.convertTo(radius.unit)!!.value.toDouble()) {
            "Vertical cylinder radiusMin must be less than or equal to radiusMax."
        }
    }
    if (diameterMin != null && diameterMax != null) {
        require(diameterMin.convertTo(radius.unit)!!.value.toDouble() <= diameterMax.convertTo(radius.unit)!!.value.toDouble()) {
            "Vertical cylinder diameterMin must be less than or equal to diameterMax."
        }
    }
    if (radiusStep != null) {
        require(radiusMin != null && radiusMax != null) {
            "Vertical cylinder radiusStep requires radiusMin and radiusMax."
        }
    }
    if (diameterStep != null) {
        require(diameterMin != null && diameterMax != null) {
            "Vertical cylinder diameterStep requires diameterMin and diameterMax."
        }
    }

    val hasExplicitCandidates = radiusCandidates.isNotEmpty()
    val hasRadiusInterval = radiusMin != null && radiusMax != null && radiusStep != null
    val hasDiameterInterval = diameterMin != null && diameterMax != null && diameterStep != null
    require(hasExplicitCandidates || !hasRadiusInterval || !hasDiameterInterval) {
        "Vertical cylinder radius interval and diameter interval cannot both generate candidates."
    }

    return when {
        hasExplicitCandidates -> distinctSortedRadiusCandidates(
            radiusCandidates.map { it.toPositiveQuantity(radius.unit, "radiusCandidates") }
        )

        hasRadiusInterval -> intervalCandidates(
            min = radiusMin,
            max = radiusMax,
            step = radiusStep,
            unit = radius.unit,
            fieldPrefix = "radius"
        )

        hasDiameterInterval -> diameterIntervalRadiusCandidates(
            min = diameterMin,
            max = diameterMax,
            step = diameterStep,
            unit = radius.unit
        )

        else -> listOf(radius.toPositiveQuantity(radius.unit, "radius"))
    }
}

fun PackageShape<InfraNumber>.toPackingShapeOrNull(): PackingShape3<InfraNumber>? {
    val shapeHeight = height
    val shapeWeight = weight
    return when (val spec = shapeSpec) {
        PackageShapeSpec.Cuboid -> null
        is PackageShapeSpec.VerticalCylinder -> {
            CylinderPackingShape3(
                cylinder = object : AbstractCylinder<InfraNumber> {
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
private fun packingProgramWeightToInfraQuantity(value: Quantity<*>): Quantity<InfraNumber> {
    return when (value.value) {
        is InfraNumber -> value as Quantity<InfraNumber>
        is FltX -> {
            val quantity = value as Quantity<FltX>
            Quantity(InfraNumber(quantity.value.toDouble()), quantity.unit)
        }
        else -> throw IllegalArgumentException("Unsupported packing material quantity scalar: ${value.value}")
    }
}

@Suppress("UNCHECKED_CAST")
private fun plusPackingProgramWeight(
    lhs: Quantity<*>?,
    rhs: Quantity<*>?
): Quantity<*>? {
    if (lhs == null) {
        return rhs
    }
    if (rhs == null) {
        return lhs
    }
    return when (lhs.value) {
        is InfraNumber -> (lhs as Quantity<InfraNumber>) + packingProgramWeightToInfraQuantity(rhs)
        is FltX -> {
            val rhsValue = when (rhs.value) {
                is FltX -> rhs as Quantity<FltX>
                is InfraNumber -> packingProgramWeightToInfraQuantity(rhs).toFltX()
                else -> throw IllegalArgumentException("Unsupported packing material quantity scalar: ${rhs.value}")
            }
            (lhs as Quantity<FltX>) + rhsValue
        }

        else -> throw IllegalArgumentException("Unsupported packing material quantity scalar: ${lhs.value}")
    }
}

private fun mergePackingProgramMaterialValue(
    lhs: PackingProgramMaterialValue?,
    rhs: PackingProgramMaterialValue
): PackingProgramMaterialValue {
    val amount = when {
        lhs?.amount == null -> rhs.amount
        rhs.amount == null -> lhs.amount
        else -> lhs.amount + rhs.amount
    }
    val weight = plusPackingProgramWeight(lhs?.weight, rhs.weight)
    return PackingProgramMaterialValue(
        amount = amount,
        weight = weight
    )
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
): PackingProgramMaterialValue {
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
    override val scale = Scale()
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
        is InfraNumber -> value.toDouble()
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

data class PackingProgram<V : FloatingNumber<V>>(
    val shape: PackageShape<V>,
    val pattern: PackagePattern? = null,
    val packages: List<PackingProgram<V>>? = null,
    val materials: Map<MaterialKey, PackingProgramMaterialValue>
) {
    companion object {
        fun <V : FloatingNumber<V>> outerPackage(
            shape: PackageShape<V>,
            packages: List<PackingProgram<V>>
        ): PackingProgram<V> {
            val materials = LinkedHashMap<MaterialKey, PackingProgramMaterialValue>()
            for (pack in packages) {
                for ((materialNo, value) in pack.materials) {
                    materials[materialNo] = mergePackingProgramMaterialValue(
                        lhs = materials[materialNo],
                        rhs = value
                    )
                }
            }
            return PackingProgram(
                shape = shape,
                packages = packages,
                materials = materials,
            )
        }

        fun innerPackage(
            shape: PackageShape<InfraNumber>,
            materials: Map<MaterialKey, UInt64>
        ): PackingProgram<InfraNumber> {
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
            shape: PackageShape<InfraNumber>,
            materials: Map<MaterialKey, PackingProgramMaterialValue>
        ): PackingProgram<InfraNumber> {
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
        materialCatalog: Map<MaterialKey, Material<InfraNumber>> = emptyMap()
    ): Map<MaterialKey, Quantity<InfraNumber>> {
        return materials.mapNotNull { (material, value) ->
            val quantity = value.weight?.let { packingProgramWeightToInfraQuantity(it) } ?: value.amount?.let { amount ->
                val unitWeight = materialCatalog[material]?.weight
                if (unitWeight != null) {
                    unitWeight * InfraNumber(amount.toULong().toDouble())
                } else {
                    InfraNumber(amount.toULong().toDouble()) * amountUnit
                }
            }
            if (quantity == null) {
                null
            } else {
                Pair(material, quantity)
            }
        }.toMap()
    }

    fun materialWeights(materialCatalog: Map<MaterialKey, Material<InfraNumber>> = emptyMap()): Map<MaterialKey, Quantity<InfraNumber>> {
        return materials.mapNotNull { (material, value) ->
            val resolvedWeight = value.weight?.let { packingProgramWeightToInfraQuantity(it) } ?: value.amount?.let { amount ->
                val unitWeight = materialCatalog[material]?.weight ?: return@let null
                unitWeight * InfraNumber(amount.toULong().toDouble())
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

    fun actualPackage(materials: Map<Material<InfraNumber>, UInt64>, pending: Boolean = false): Package<V> {
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
    val materials: Map<Material<InfraNumber>, UInt64>,
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
            shape: PackageShape<InfraNumber>,
            materials: Map<Material<InfraNumber>, UInt64>,
            amount: UInt64 = UInt64.one,
            pending: Boolean = false,
        ): Package<InfraNumber> {
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
            materials: Map<Material<InfraNumber>, UInt64>,
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




