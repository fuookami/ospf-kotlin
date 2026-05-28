@file:Suppress("DEPRECATION")

package fuookami.ospf.kotlin.framework.bpp3d.domain.item.model

import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.LegacyCuboid
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.ItemModelScalar
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.legacyInfinity
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.legacyNegativeInfinity
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.legacyOne
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.legacyTwo
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.legacyZero
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*
import fuookami.ospf.kotlin.quantities.unit.PhysicalUnit
import fuookami.ospf.kotlin.quantities.unit.QuantityUnit
import fuookami.ospf.kotlin.quantities.quantity.plus
import fuookami.ospf.kotlin.quantities.quantity.toFltX
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.Scale
import fuookami.ospf.kotlin.utils.functional.Eq
import kotlin.math.ceil

data class PackageBottomShape(
    val width: Quantity<ItemModelScalar>,
    val depth: Quantity<ItemModelScalar>,
    val weight: Quantity<ItemModelScalar>,
    val packageType: PackageType,
) : Eq<PackageBottomShape> {
    val packageCategory by packageType::category
    val area: Quantity<ItemModelScalar> = width * depth

    fun new(
        width: Quantity<ItemModelScalar>? = null,
        depth: Quantity<ItemModelScalar>? = null,
        weight: Quantity<ItemModelScalar>? = null,
        packageType: PackageType? = null
    ): PackageBottomShape {
        return PackageBottomShape(
            width = width ?: this.width,
            depth = depth ?: this.depth,
            weight = weight ?: this.weight,
            packageType = packageType ?: this.packageType
        )
    }

    override fun partialEq(rhs: PackageBottomShape): Boolean {
        return packageType == rhs.packageType
    }

    override fun eq(rhs: PackageBottomShape): Boolean {
        if (width neq rhs.width) return false
        if (depth neq rhs.depth) return false
        if ((weight - rhs.weight).abs() gr (legacyTwo() * weight.unit)) return false

        return packageType == rhs.packageType
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PackageBottomShape) return false

        return this eq other
    }

    override fun hashCode(): Int {
        var result = width.hashCode()
        result = 31 * result + depth.hashCode()
        result = 31 * result + weight.hashCode()
        result = 31 * result + packageType.hashCode()
        return result
    }
}

data class PackageShape(
    val width: Quantity<ItemModelScalar>,
    val height: Quantity<ItemModelScalar>,
    val depth: Quantity<ItemModelScalar>,
    val weight: Quantity<ItemModelScalar>,
    val packageType: PackageType,
) : Eq<PackageShape> {
    val bottomShape = PackageBottomShape(
        width = width,
        depth = depth,
        weight = weight,
        packageType = packageType
    )
    val packageCategory by packageType::category
    val volume: Quantity<ItemModelScalar> = width * height * depth

    fun new(
        width: Quantity<ItemModelScalar>? = null,
        height: Quantity<ItemModelScalar>? = null,
        depth: Quantity<ItemModelScalar>? = null,
        weight: Quantity<ItemModelScalar>? = null,
        packageType: PackageType? = null
    ): PackageShape {
        return PackageShape(
            width = width ?: this.width,
            height = height ?: this.height,
            depth = depth ?: this.depth,
            weight = weight ?: this.weight,
            packageType = packageType ?: this.packageType
        )
    }

    override fun partialEq(rhs: PackageShape): Boolean {
        return packageType == rhs.packageType
    }

    override fun eq(rhs: PackageShape): Boolean {
        if (width neq rhs.width) return false
        if (height neq rhs.height) return false
        if (depth neq rhs.depth) return false
        if ((weight - rhs.weight).abs() gr (legacyTwo() * weight.unit)) return false

        return packageType == rhs.packageType
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PackageShape) return false

        return this eq other
    }

    override fun hashCode(): Int {
        var result = width.hashCode()
        result = 31 * result + height.hashCode()
        result = 31 * result + depth.hashCode()
        result = 31 * result + weight.hashCode()
        result = 31 * result + packageType.hashCode()
        return result
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
private fun packingProgramWeightToLegacyScalar(value: Quantity<*>): Quantity<ItemModelScalar> {
    return when (value.value) {
        is ItemModelScalar -> value as Quantity<ItemModelScalar>
        is FltX -> {
            val quantity = value as Quantity<FltX>
            Quantity(ItemModelScalar(quantity.value.toDouble()), quantity.unit)
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
        is ItemModelScalar -> (lhs as Quantity<ItemModelScalar>) + packingProgramWeightToLegacyScalar(rhs)
        is FltX -> {
            val rhsValue = when (rhs.value) {
                is FltX -> rhs as Quantity<FltX>
                is ItemModelScalar -> packingProgramWeightToLegacyScalar(rhs).toFltX()
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
        is ItemModelScalar -> value.toDouble()
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

data class PackingProgram(
    val shape: PackageShape,
    val pattern: PackagePattern? = null,
    val packages: List<PackingProgram>? = null,
    val materials: Map<MaterialKey, PackingProgramMaterialValue>
) {
    companion object {
        fun outerPackage(
            shape: PackageShape,
            packages: List<PackingProgram>
        ): PackingProgram {
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
            shape: PackageShape,
            materials: Map<MaterialKey, UInt64>
        ): PackingProgram {
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
            shape: PackageShape,
            materials: Map<MaterialKey, PackingProgramMaterialValue>
        ): PackingProgram {
            return PackingProgram(
                shape = shape,
                materials = materials
            )
        }

        fun <V : FloatingNumber<V>> innerPackageWithMaterialQuantities(
            shape: PackageShape,
            materials: Map<MaterialKey, Quantity<V>>
        ): PackingProgram {
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
        materialCatalog: Map<MaterialKey, Material> = emptyMap()
    ): Map<MaterialKey, Quantity<ItemModelScalar>> {
        return materials.mapNotNull { (material, value) ->
            val quantity = value.weight?.let { packingProgramWeightToLegacyScalar(it) } ?: value.amount?.let { amount ->
                val unitWeight = materialCatalog[material]?.weight
                if (unitWeight != null) {
                    unitWeight * ItemModelScalar(amount.toULong().toDouble())
                } else {
                    ItemModelScalar(amount.toULong().toDouble()) * amountUnit
                }
            }
            if (quantity == null) {
                null
            } else {
                Pair(material, quantity)
            }
        }.toMap()
    }

    fun materialWeights(materialCatalog: Map<MaterialKey, Material> = emptyMap()): Map<MaterialKey, Quantity<ItemModelScalar>> {
        return materials.mapNotNull { (material, value) ->
            val resolvedWeight = value.weight?.let { packingProgramWeightToLegacyScalar(it) } ?: value.amount?.let { amount ->
                val unitWeight = materialCatalog[material]?.weight ?: return@let null
                unitWeight * ItemModelScalar(amount.toULong().toDouble())
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

    fun actualPackage(materials: Map<Material, UInt64>, pending: Boolean = false): Package {
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
                    val packages = ArrayList<Package>()
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

open class Package(
    val code: PackageCode? = null,
    val pattern: PackagePattern? = null,
    val program: PackingProgram? = null,
    val shape: PackageShape,
    val packages: List<Package>? = null,
    val materials: Map<Material, UInt64>,
    val amount: UInt64 = UInt64.one,
    val pending: Boolean = false,
) {
    companion object {
        fun outerPackage(
            code: PackageCode? = null,
            pattern: PackagePattern? = null,
            shape: PackageShape,
            packages: List<Package>,
            amount: UInt64 = UInt64.one,
            pending: Boolean = false,
        ): Package {
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

        fun outerPackage(
            code: PackageCode? = null,
            pattern: PackagePattern? = null,
            program: PackingProgram,
            packages: List<Package>,
            amount: UInt64 = UInt64.one,
            pending: Boolean = false,
        ): Package {
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
            shape: PackageShape,
            materials: Map<Material, UInt64>,
            amount: UInt64 = UInt64.one,
            pending: Boolean = false,
        ): Package {
            return Package(
                code = code,
                pattern = pattern,
                shape = shape,
                materials = materials,
                amount = amount,
                pending = pending,
            )
        }

        fun innerPackage(
            code: PackageCode? = null,
            pattern: PackagePattern? = null,
            program: PackingProgram,
            materials: Map<Material, UInt64>,
            amount: UInt64 = UInt64.one,
            pending: Boolean = false,
        ): Package {
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

    open fun enabledHoldingAmount(packingProgram: PackingProgram): Map<MaterialKey, UInt64>? {
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

    open fun full(packingProgram: PackingProgram): Boolean {
        return enabledHoldingAmount(packingProgram)?.let {
            it.isEmpty() || it.values.all { amount -> amount == UInt64.zero }
        } == true
    }

    open fun contain(materialType: MaterialType?): Boolean {
        return materialType != null && materials.keys.any { it.type == materialType }
    }
}


