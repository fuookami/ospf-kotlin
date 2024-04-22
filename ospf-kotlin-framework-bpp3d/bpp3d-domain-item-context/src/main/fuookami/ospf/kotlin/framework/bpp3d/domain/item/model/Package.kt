package fuookami.ospf.kotlin.framework.bpp3d.domain.item.model

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.operator.*
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*

enum class PackageClassification {
    Outer,
    Inner
}

enum class PackageCategory {
    HardBox,
    Pallet,
    SoftBox,
    Filler
}

enum class PackageType {
    DutyCorrugatedBoardPedal {
        override val category = PackageCategory.HardBox
    },

    WoodenContainer {
        override val category = PackageCategory.HardBox
    },

    HoneycombBox {
        override val category = PackageCategory.HardBox
    },

    Pallet {
        override val category = PackageCategory.Pallet
    },

    CartonPallet {
        override val category = PackageCategory.Pallet
    },

    CartonContainer {
        override val category = PackageCategory.SoftBox
    },

    PackingFoam {
        override val category = PackageCategory.Filler
    };

    abstract val category: PackageCategory
}

data class PackageBottomShape(
    val width: Flt64,
    val depth: Flt64,
    val weight: Flt64,
    val packageType: PackageType,
) : Eq<PackageBottomShape> {
    val packageCategory by packageType::category
    val area: Flt64 = width * depth

    fun new(
        width: Flt64? = null,
        depth: Flt64? = null,
        weight: Flt64? = null,
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
        if (abs(weight - rhs.weight) gr Flt64.two) return false

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
    val width: Flt64,
    val height: Flt64,
    val depth: Flt64,
    val weight: Flt64,
    val packageType: PackageType,
) : Eq<PackageShape> {
    val bottomShape = PackageBottomShape(
        width = width,
        depth = depth,
        weight = weight,
        packageType = packageType
    )
    val packageCategory by packageType::category
    val volume: Flt64 = width * height * depth

    fun new(
        width: Flt64? = null,
        height: Flt64? = null,
        depth: Flt64? = null,
        weight: Flt64? = null,
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
        if (abs(weight - rhs.weight) gr Flt64.two) return false

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

data class PackingProgram(
    val shape: PackageShape,
    val pattern: PackagePattern? = null,
    val packages: List<PackingProgram>? = null,
    val materials: Map<MaterialKey, UInt64>
) {
    companion object {
        fun outerPackage(
            shape: PackageShape,
            packages: List<PackingProgram>
        ): PackingProgram {
            val materials = packages
                .flatMap { it.materials.keys }
                .toSet()
                .associateWith { UInt64.zero }
                .toMutableMap()
            for (pack in packages) {
                for ((materialNo, amount) in pack.materials) {
                    materials[materialNo] = (materials[materialNo] ?: UInt64.zero) + amount
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
                materials = materials,
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

    fun actualPackage(materials: Map<Material, UInt64>, pending: Boolean = false): Package {
        return when (classification) {
            PackageClassification.Outer -> {
                val maxPackage = this.materials.minOf {
                    val material = materials.filterKeys { material -> material.key == it.key }.entries.firstOrNull()
                    (material?.value ?: UInt64.zero) / it.value
                }
                if (maxPackage != UInt64.zero) {
                    Package.outerPackage(
                        program = this,
                        packages = this.packages!!.map {
                            Package.innerPackage(
                                program = it,
                                materials = it.materials.map { (materialKey, amount) ->
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
                        val thisPackageMaterials = pack.materials.mapNotNull {
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
                val maxPackage = this.materials.minOf {
                    val material = materials.filterKeys { material -> material.key == it.key }.entries.firstOrNull()
                    (material?.value ?: UInt64.zero) / it.value
                }
                if (maxPackage != UInt64.zero) {
                    Package.innerPackage(
                        program = this,
                        materials = this.materials.map {
                            val material = materials.filterKeys { material -> material.key == it.key }.entries.firstOrNull()!!
                            Pair(material.key, it.value)
                        }.toMap(),
                        amount = maxPackage,
                        pending = pending
                    )
                } else {
                    Package.innerPackage(
                        program = this,
                        materials = this.materials.mapNotNull {
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
        if (!(pattern == null || packingProgram.pattern == null || pattern belong packingProgram.pattern)) {
            return null
        }
        if (materials.keys.any { !packingProgram.materials.containsKey(it.key) }) {
            return null
        }
        return packingProgram.materials.mapNotNull {
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
