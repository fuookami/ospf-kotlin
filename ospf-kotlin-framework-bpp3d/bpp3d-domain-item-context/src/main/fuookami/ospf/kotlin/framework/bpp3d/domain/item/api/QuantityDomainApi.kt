@file:Suppress("DEPRECATION")

package fuookami.ospf.kotlin.framework.bpp3d.domain.item.api

import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.AbstractCargoAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.PackageAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.MaterialType
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.BatchNo
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Container3Shape
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.MaterialNo
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Orientation
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PackageCode
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PackagePattern
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PackageType
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.point3
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.quantities.quantity.toFlt64
import kotlin.reflect.KClass
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BinLayer as LegacyBinLayer
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.ActualItem as LegacyItem
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Material as LegacyMaterial
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.ItemPlacement3 as LegacyItemPlacement3
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Package as LegacyPackage
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.PackageShape as LegacyPackageShape
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Placement3 as LegacyPlacement3

data class Material<V : FloatingNumber<V>>(
    val no: MaterialNo,
    val type: MaterialType,
    val cargo: AbstractCargoAttribute,
    val name: String,
    val manufacturer: String? = null,
    val supplier: String? = null,
    val warehouse: String? = null,
    val weight: Quantity<V>
) {
    fun toLegacy(): LegacyMaterial {
        return LegacyMaterial(
            no = no,
            type = type,
            cargo = cargo,
            name = name,
            manufacturer = manufacturer,
            supplier = supplier,
            warehouse = warehouse,
            weight = weight.toFlt64()
        )
    }
}

data class PackageShape<V : FloatingNumber<V>>(
    val width: Quantity<V>,
    val height: Quantity<V>,
    val depth: Quantity<V>,
    val weight: Quantity<V>,
    val packageType: PackageType,
) {
    val volume: Quantity<V> = width * height * depth

    fun toLegacy(): LegacyPackageShape {
        return LegacyPackageShape(
            width = width.toFlt64(),
            height = height.toFlt64(),
            depth = depth.toFlt64(),
            weight = weight.toFlt64(),
            packageType = packageType
        )
    }
}

data class Package<V : FloatingNumber<V>>(
    val code: PackageCode? = null,
    val pattern: PackagePattern? = null,
    val shape: PackageShape<V>,
    val packages: List<Package<V>>? = null,
    val materials: Map<Material<V>, UInt64>,
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
            val materials = LinkedHashMap<Material<V>, UInt64>()
            for (pack in packages) {
                for ((material, materialAmount) in pack.materials) {
                    materials[material] = (materials[material] ?: UInt64.zero) + materialAmount
                }
            }
            return Package(
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
            shape: PackageShape<V>,
            materials: Map<Material<V>, UInt64>,
            amount: UInt64 = UInt64.one,
            pending: Boolean = false,
        ): Package<V> {
            return Package(
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

    fun toLegacy(
        materialCache: MutableMap<Material<V>, LegacyMaterial> = LinkedHashMap()
    ): LegacyPackage {
        return if (packages.isNullOrEmpty()) {
            LegacyPackage.innerPackage(
                code = code,
                pattern = pattern,
                shape = shape.toLegacy(),
                materials = materials.map { (material, amount) ->
                    val legacyMaterial = materialCache.getOrPut(material) { material.toLegacy() }
                    Pair(legacyMaterial, amount)
                }.toMap(),
                amount = amount,
                pending = pending
            )
        } else {
            LegacyPackage.outerPackage(
                code = code,
                pattern = pattern,
                shape = shape.toLegacy(),
                packages = packages.map { it.toLegacy(materialCache) },
                amount = amount,
                pending = pending
            )
        }
    }
}

data class Item<V : FloatingNumber<V>>(
    val id: String,
    val name: String,
    val packageCode: PackageCode? = null,
    val pack: Package<V>? = null,
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
        pack: Package<V>,
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

    fun toLegacy(
        materialCache: MutableMap<Material<V>, LegacyMaterial> = LinkedHashMap()
    ): LegacyItem {
        val legacyPack = pack?.toLegacy(materialCache)
        return LegacyItem(
            id = id,
            name = name,
            packageCode = packageCode ?: legacyPack?.code,
            pack = legacyPack,
            width = width.toFlt64(),
            height = height.toFlt64(),
            depth = depth.toFlt64(),
            weight = weight.toFlt64(),
            enabledOrientations = enabledOrientations,
            batchNo = batchNo,
            warehouse = warehouse,
            packageAttribute = packageAttribute
        )
    }
}

data class ItemPlacement<V : FloatingNumber<V>>(
    val item: Item<V>,
    val x: Quantity<V>,
    val y: Quantity<V>,
    val z: Quantity<V>,
    val orientation: Orientation = Orientation.Upright
) {
    fun toLegacy(
        materialCache: MutableMap<Material<V>, LegacyMaterial> = LinkedHashMap()
    ): LegacyItemPlacement3 {
        val legacyItem = item.toLegacy(materialCache)
        return LegacyPlacement3(
            view = legacyItem.view(orientation),
            position = point3(
                x = x.toFlt64(),
                y = y.toFlt64(),
                z = z.toFlt64()
            )
        )
    }
}

data class BinLayer<V : FloatingNumber<V>>(
    val iteration: Int64,
    val from: KClass<*>,
    val width: Quantity<V>,
    val height: Quantity<V>,
    val depth: Quantity<V>,
    val units: List<ItemPlacement<V>>
) {
    fun toLegacy(
        materialCache: MutableMap<Material<V>, LegacyMaterial> = LinkedHashMap()
    ): LegacyBinLayer {
        return LegacyBinLayer(
            iteration = iteration,
            from = from,
            shape = Container3Shape(
                width = width.toFlt64(),
                height = height.toFlt64(),
                depth = depth.toFlt64()
            ),
            units = units.map { it.toLegacy(materialCache) }
        )
    }
}
