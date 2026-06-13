@file:Suppress("DEPRECATION")
/**
 * 量纲域模型。
 * Quantity domain models.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.item.model

import kotlin.reflect.KClass
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.geometry.Axis3
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*

private fun <V : FloatingNumber<V>> Quantity<V>.toFltXQuantity(): Quantity<FltX> {
    return Quantity(fltX(this.value.toString().toDouble()), this.unit)
}

private fun Quantity<*>.toFltXQuantityUnsafe(): Quantity<FltX> {
    return Quantity(fltX(this.value.toString().toDouble()), this.unit)
}

sealed interface QuantityPackageShapeSpec {
    data object Cuboid : QuantityPackageShapeSpec

    data class VerticalCylinder<V : FloatingNumber<V>>(
        val radius: Quantity<V>,
        val axis: Axis3 = Axis3.Y,
        val radiusCandidates: List<Quantity<V>> = emptyList(),
        val radiusMin: Quantity<V>? = null,
        val radiusMax: Quantity<V>? = null,
        val radiusWeightFunctionKey: String? = null,
        val radiusStep: Quantity<V>? = null,
        val diameterMin: Quantity<V>? = null,
        val diameterMax: Quantity<V>? = null,
        val diameterStep: Quantity<V>? = null
    ) : QuantityPackageShapeSpec
}

private fun QuantityPackageShapeSpec.toModel(): PackageShapeSpec {
    return when (this) {
        QuantityPackageShapeSpec.Cuboid -> PackageShapeSpec.Cuboid
        is QuantityPackageShapeSpec.VerticalCylinder<*> -> {
            PackageShapeSpec.VerticalCylinder(
                radius = radius.toFltXQuantityUnsafe(),
                axis = axis,
                radiusCandidates = radiusCandidates.map { it.toFltXQuantityUnsafe() },
                radiusMin = radiusMin?.toFltXQuantityUnsafe(),
                radiusMax = radiusMax?.toFltXQuantityUnsafe(),
                radiusWeightFunctionKey = radiusWeightFunctionKey,
                radiusStep = radiusStep?.toFltXQuantityUnsafe(),
                diameterMin = diameterMin?.toFltXQuantityUnsafe(),
                diameterMax = diameterMax?.toFltXQuantityUnsafe(),
                diameterStep = diameterStep?.toFltXQuantityUnsafe()
            )
        }
    }
}

data class QuantityMaterial<V : FloatingNumber<V>>(
    val no: MaterialNo,
    val type: MaterialType,
    val cargo: AbstractCargoAttribute,
    val name: String,
    val manufacturer: String? = null,
    val supplier: String? = null,
    val warehouse: String? = null,
    val weight: Quantity<V>
) {
    fun toModel(): Material<FltX> {
        return Material(
            no = no,
            type = type,
            cargo = cargo,
            name = name,
            manufacturer = manufacturer,
            supplier = supplier,
            warehouse = warehouse,
            weight = weight.toFltXQuantity()
        )
    }
}

data class QuantityPackageShape<V : FloatingNumber<V>>(
    val width: Quantity<V>,
    val height: Quantity<V>,
    val depth: Quantity<V>,
    val weight: Quantity<V>,
    val packageType: fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PackageType,
    val shapeSpec: QuantityPackageShapeSpec = QuantityPackageShapeSpec.Cuboid
) {
    val volume: Quantity<V> = width * height * depth

    fun toModel(): PackageShape<FltX> {
        return PackageShape(
            width = width.toFltXQuantity(),
            height = height.toFltXQuantity(),
            depth = depth.toFltXQuantity(),
            weight = weight.toFltXQuantity(),
            packageType = packageType,
            shapeSpec = shapeSpec.toModel()
        )
    }
}

data class QuantityPackage<V : FloatingNumber<V>>(
    val code: PackageCode? = null,
    val pattern: PackagePattern? = null,
    val shape: QuantityPackageShape<V>,
    val packages: List<QuantityPackage<V>>? = null,
    val materials: Map<QuantityMaterial<V>, UInt64>,
    val amount: UInt64 = UInt64.one,
    val pending: Boolean = false,
) {
    companion object {
        fun <V : FloatingNumber<V>> outerPackage(
            code: PackageCode? = null,
            pattern: PackagePattern? = null,
            shape: QuantityPackageShape<V>,
            packages: List<QuantityPackage<V>>,
            amount: UInt64 = UInt64.one,
            pending: Boolean = false,
        ): QuantityPackage<V> {
            val materials = LinkedHashMap<QuantityMaterial<V>, UInt64>()
            for (pack in packages) {
                for ((material, materialAmount) in pack.materials) {
                    materials[material] = (materials[material] ?: UInt64.zero) + materialAmount
                }
            }
            return QuantityPackage(
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
            shape: QuantityPackageShape<V>,
            materials: Map<QuantityMaterial<V>, UInt64>,
            amount: UInt64 = UInt64.one,
            pending: Boolean = false,
        ): QuantityPackage<V> {
            return QuantityPackage(
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
        materialCache: MutableMap<QuantityMaterial<V>, Material<FltX>> = LinkedHashMap()
    ): Package<FltX> {
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

data class QuantityItem<V : FloatingNumber<V>>(
    val id: String,
    val name: String,
    val packageCode: PackageCode? = null,
    val pack: QuantityPackage<V>? = null,
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
        pack: QuantityPackage<V>,
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
        materialCache: MutableMap<QuantityMaterial<V>, Material<FltX>> = LinkedHashMap(),
        itemCache: MutableMap<QuantityItem<V>, ActualItem> = LinkedHashMap()
    ): ActualItem {
        return itemCache.getOrPut(this) {
            val modelPack = pack?.toModel(materialCache)
            ActualItem(
                id = id,
                name = name,
                packageCode = packageCode ?: modelPack?.code,
                pack = modelPack,
                width = width.toFltXQuantity(),
                height = height.toFltXQuantity(),
                depth = depth.toFltXQuantity(),
                weight = weight.toFltXQuantity(),
                enabledOrientations = enabledOrientations,
                batchNo = batchNo,
                warehouse = warehouse,
                packageAttribute = packageAttribute
            )
        }
    }
}

data class QuantityItemPlacement<V : FloatingNumber<V>>(
    val item: QuantityItem<V>,
    val x: Quantity<V>,
    val y: Quantity<V>,
    val z: Quantity<V>,
    val orientation: Orientation = Orientation.Upright
) {
    fun toModel(
        materialCache: MutableMap<QuantityMaterial<V>, Material<FltX>> = LinkedHashMap(),
        itemCache: MutableMap<QuantityItem<V>, ActualItem> = LinkedHashMap()
    ): QuantityPlacement3<Item, FltX> {
        val modelItem = item.toModel(materialCache, itemCache)
        return itemPlacement3Of(
            item = modelItem,
            position = point3FltX(
                x = x.toFltXQuantity().value,
                y = y.toFltXQuantity().value,
                z = z.toFltXQuantity().value
            ),
            orientation = orientation
        )
    }
}

data class QuantityBinLayer<V : FloatingNumber<V>>(
    val iteration: Int64,
    val from: KClass<*>,
    val width: Quantity<V>,
    val height: Quantity<V>,
    val depth: Quantity<V>,
    val units: List<QuantityItemPlacement<V>>
) {
    fun toModel(
        materialCache: MutableMap<QuantityMaterial<V>, Material<FltX>> = LinkedHashMap(),
        itemCache: MutableMap<QuantityItem<V>, ActualItem> = LinkedHashMap()
    ): BinLayer {
        return BinLayer(
            iteration = iteration,
            from = from,
            shape = Container3Shape(
                width = width.toFltXQuantity(),
                height = height.toFltXQuantity(),
                depth = depth.toFltXQuantity()
            ),
            units = units.map { it.toModel(materialCache, itemCache) }
        )
    }
}
