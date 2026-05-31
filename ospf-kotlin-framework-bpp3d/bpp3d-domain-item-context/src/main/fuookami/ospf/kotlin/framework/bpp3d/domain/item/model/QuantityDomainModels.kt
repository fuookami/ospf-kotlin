@file:Suppress("DEPRECATION")

/**
 * 量纲域模型。
 * Quantity domain models.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.item.model

import kotlin.reflect.KClass
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.BatchNo
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Container3Shape
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.InfraNumber
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.MaterialNo
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Orientation
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PackageCode
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PackagePattern
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PackageType
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.infraScalar
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.point3
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.QuantityPlacement3
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.math.algebra.number.UInt64

private fun <V : FloatingNumber<V>> Quantity<V>.toInfraQuantity(): Quantity<InfraNumber> {
    return Quantity(infraScalar(this.value.toString().toDouble()), this.unit)
}

data class GenericMaterial<V : FloatingNumber<V>>(
    val no: MaterialNo,
    val type: MaterialType,
    val cargo: AbstractCargoAttribute,
    val name: String,
    val manufacturer: String? = null,
    val supplier: String? = null,
    val warehouse: String? = null,
    val weight: Quantity<V>
) {
    fun toModel(): Material<InfraNumber> {
        return Material(
            no = no,
            type = type,
            cargo = cargo,
            name = name,
            manufacturer = manufacturer,
            supplier = supplier,
            warehouse = warehouse,
            weight = weight.toInfraQuantity()
        )
    }
}

data class GenericPackageShape<V : FloatingNumber<V>>(
    val width: Quantity<V>,
    val height: Quantity<V>,
    val depth: Quantity<V>,
    val weight: Quantity<V>,
    val packageType: fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PackageType,
) {
    val volume: Quantity<V> = width * height * depth

    fun toModel(): PackageShape<InfraNumber> {
        return PackageShape(
            width = width.toInfraQuantity(),
            height = height.toInfraQuantity(),
            depth = depth.toInfraQuantity(),
            weight = weight.toInfraQuantity(),
            packageType = packageType
        )
    }
}

data class GenericPackage<V : FloatingNumber<V>>(
    val code: PackageCode? = null,
    val pattern: PackagePattern? = null,
    val shape: GenericPackageShape<V>,
    val packages: List<GenericPackage<V>>? = null,
    val materials: Map<GenericMaterial<V>, UInt64>,
    val amount: UInt64 = UInt64.one,
    val pending: Boolean = false,
) {
    companion object {
        fun <V : FloatingNumber<V>> outerPackage(
            code: PackageCode? = null,
            pattern: PackagePattern? = null,
            shape: GenericPackageShape<V>,
            packages: List<GenericPackage<V>>,
            amount: UInt64 = UInt64.one,
            pending: Boolean = false,
        ): GenericPackage<V> {
            val materials = LinkedHashMap<GenericMaterial<V>, UInt64>()
            for (pack in packages) {
                for ((material, materialAmount) in pack.materials) {
                    materials[material] = (materials[material] ?: UInt64.zero) + materialAmount
                }
            }
            return GenericPackage(
                code = code,
                pattern = pattern,
                shape = shape,
                packages = packages,
                materials = materials,
                amount = amount,
                pending = pending
            )
        }

        fun <V : FloatingNumber<V>> innerPackage(
            code: PackageCode? = null,
            pattern: PackagePattern? = null,
            shape: GenericPackageShape<V>,
            materials: Map<GenericMaterial<V>, UInt64>,
            amount: UInt64 = UInt64.one,
            pending: Boolean = false,
        ): GenericPackage<V> {
            return GenericPackage(
                code = code,
                pattern = pattern,
                shape = shape,
                materials = materials,
                amount = amount,
                pending = pending
            )
        }
    }

    val width by shape::width
    val height by shape::height
    val depth by shape::depth
    val weight by shape::weight
    val packageType by shape::packageType
    val volume by shape::volume

    fun toModel(
        materialCache: MutableMap<GenericMaterial<V>, Material<InfraNumber>> = LinkedHashMap()
    ): Package<InfraNumber> {
        return if (packages.isNullOrEmpty()) {
            Package.innerPackage(
                code = code,
                pattern = pattern,
                shape = shape.toModel(),
                materials = materials.map { (material, amount) ->
                    val modelMaterial = materialCache.getOrPut(material) { material.toModel() }
                    Pair(modelMaterial, amount)
                }.toMap(),
                amount = amount,
                pending = pending
            )
        } else {
            Package.outerPackage(
                code = code,
                pattern = pattern,
                shape = shape.toModel(),
                packages = packages.map { it.toModel(materialCache) },
                amount = amount,
                pending = pending
            )
        }
    }
}

data class GenericItem<V : FloatingNumber<V>>(
    val id: String,
    val name: String,
    val packageCode: PackageCode? = null,
    val pack: GenericPackage<V>? = null,
    val width: Quantity<V>,
    val height: Quantity<V>,
    val depth: Quantity<V>,
    val weight: Quantity<V>,
    val enabledOrientations: List<Orientation>,
    val batchNo: BatchNo? = null,
    val warehouse: String? = null,
    val packageAttribute: PackageAttribute,
) {
    constructor(
        id: String,
        name: String,
        pack: GenericPackage<V>,
        enabledOrientations: List<Orientation>,
        batchNo: BatchNo? = null,
        warehouse: String? = null,
        packageAttribute: PackageAttribute,
    ) : this(
        id = id,
        name = name,
        packageCode = pack.code,
        pack = pack,
        width = pack.width,
        height = pack.height,
        depth = pack.depth,
        weight = pack.weight,
        enabledOrientations = enabledOrientations,
        batchNo = batchNo,
        warehouse = warehouse,
        packageAttribute = packageAttribute
    )

    fun toModel(
        materialCache: MutableMap<GenericMaterial<V>, Material<InfraNumber>> = LinkedHashMap(),
        itemCache: MutableMap<GenericItem<V>, ActualItem> = LinkedHashMap()
    ): ActualItem {
        return itemCache.getOrPut(this) {
            val modelPack = pack?.toModel(materialCache)
            ActualItem(
                id = id,
                name = name,
                packageCode = packageCode ?: modelPack?.code,
                pack = modelPack,
                width = width.toInfraQuantity(),
                height = height.toInfraQuantity(),
                depth = depth.toInfraQuantity(),
                weight = weight.toInfraQuantity(),
                enabledOrientations = enabledOrientations,
                batchNo = batchNo,
                warehouse = warehouse,
                packageAttribute = packageAttribute
            )
        }
    }
}

data class GenericItemPlacement<V : FloatingNumber<V>>(
    val item: GenericItem<V>,
    val x: Quantity<V>,
    val y: Quantity<V>,
    val z: Quantity<V>,
    val orientation: Orientation = Orientation.Upright
) {
    fun toModel(
        materialCache: MutableMap<GenericMaterial<V>, Material<InfraNumber>> = LinkedHashMap(),
        itemCache: MutableMap<GenericItem<V>, ActualItem> = LinkedHashMap()
    ): QuantityPlacement3<Item> {
        val modelItem = item.toModel(materialCache, itemCache)
        return QuantityPlacement3(
            view = modelItem.view(orientation),
            position = point3(
                x = x.toInfraQuantity().value,
                y = y.toInfraQuantity().value,
                z = z.toInfraQuantity().value
            )
        )
    }
}

data class GenericBinLayer<V : FloatingNumber<V>>(
    val iteration: Int64,
    val from: KClass<*>,
    val width: Quantity<V>,
    val height: Quantity<V>,
    val depth: Quantity<V>,
    val units: List<GenericItemPlacement<V>>
) {
    fun toModel(
        materialCache: MutableMap<GenericMaterial<V>, Material<InfraNumber>> = LinkedHashMap(),
        itemCache: MutableMap<GenericItem<V>, ActualItem> = LinkedHashMap()
    ): BinLayer {
        return BinLayer(
            iteration = iteration,
            from = from,
            shape = Container3Shape(
                width = width.toInfraQuantity(),
                height = height.toInfraQuantity(),
                depth = depth.toInfraQuantity()
            ),
            units = units.map { it.toModel(materialCache, itemCache) }
        )
    }
}
