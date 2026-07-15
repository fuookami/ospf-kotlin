/**
 * Quantity domain models.
 * 量纲域模型。
*/
package fuookami.ospf.kotlin.framework.bpp3d.domain.item.model

import kotlin.reflect.KClass
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.geometry.Axis3
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*

/**
 * 转换为 FltX 量纲。
 * Convert to FltX quantity.
 * @return FltX 量纲。
*/
private fun <V : FloatingNumber<V>> Quantity<V>.toFltXQuantity(): Quantity<FltX> {
    return Quantity(FltX(this.value.toString().toDouble()), this.unit)
}

/**
 * 不安全地转换为 FltX 量纲。
 * Unsafely convert to FltX quantity.
 * @return FltX 量纲。
*/
private fun Quantity<*>.toFltXQuantityUnsafe(): Quantity<FltX> {
    return Quantity(FltX(this.value.toString().toDouble()), this.unit)
}

/**
 * 量纲包裹形状规格。
 * Quantity package shape specification.
*/
sealed interface QuantityPackageShapeSpec {

    /** 长方体。 / Cuboid. */
    data object Cuboid : QuantityPackageShapeSpec

    /**
     * 竖直圆柱体。
     * Vertical cylinder.
     * @property radius 半径。
     * @property axis 轴线方向。
     * @property radiusCandidates 半径候选值。
     * @property radiusMin 半径最小值。
     * @property radiusMax 半径最大值。
     * @property radiusWeightFunctionKey 半径权重函数键。
     * @property radiusStep 半径步长。
     * @property diameterMin 直径最小值。
     * @property diameterMax 直径最大值。
     * @property diameterStep 直径步长。
    */
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

/**
 * 转换为领域模型。
 * Convert to the domain model.
 * @return 包裹形状规格模型。
*/
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

/**
 * 量纲材料。
 * Quantity material.
 * @property no 材料编号。
 * @property type 材料类型。
 * @property cargo 货物属性。
 * @property name 名称。
 * @property manufacturer 制造商。
 * @property supplier 供应商。
 * @property warehouse 仓库。
 * @property weight 重量。
*/
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

    /**
     * 转换为领域模型。
     * Convert to the domain model.
     * @return 材料模型。
    */
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

/**
 * 量纲包裹形状。
 * Quantity package shape.
 * @property width 宽度。
 * @property height 高度。
 * @property depth 深度。
 * @property weight 重量。
 * @property packageType 包裹类型。
 * @property shapeSpec 形状规格。
*/
data class QuantityPackageShape<V : FloatingNumber<V>>(
    val width: Quantity<V>,
    val height: Quantity<V>,
    val depth: Quantity<V>,
    val weight: Quantity<V>,
    val packageType: fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PackageType,
    val shapeSpec: QuantityPackageShapeSpec = QuantityPackageShapeSpec.Cuboid
) {
    val volume: Quantity<V> = width * height * depth

    /**
     * 转换为领域模型。
     * Convert to the domain model.
     * @return 包裹形状模型。
    */
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

/**
 * 量纲包裹。
 * Quantity package.
 * @property code 包裹编码。
 * @property pattern 包裹模式。
 * @property shape 包裹形状。
 * @property packages 子包裹列表。
 * @property materials 材料映射。
 * @property amount 数量。
 * @property pending 是否待定。
*/
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
        /**
         * 创建外层包裹。
         * Create an outer package.
         * @param code 包裹编码。
         * @param pattern 包裹模式。
         * @param shape 包裹形状。
         * @param packages 子包裹列表。
         * @param amount 数量。
         * @param pending 是否待定。
         * @return 外层包裹。
        */
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

        /**
         * 创建内层包裹。
         * Create an inner package.
         * @param code 包裹编码。
         * @param pattern 包裹模式。
         * @param shape 包裹形状。
         * @param materials 材料映射。
         * @param amount 数量。
         * @param pending 是否待定。
         * @return 内层包裹。
        */
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

    /**
     * 转换为领域模型。
     * Convert to the domain model.
     * @param materialCache 材料缓存，用于避免重复创建。
     * @return 包裹模型。
    */
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

/**
 * 量纲物品。
 * Quantity item.
 * @property packageCode 包裹编码。
 * @property pack 包裹。
 * @property width 宽度。
 * @property height 高度。
 * @property depth 深度。
 * @property weight 重量。
 * @property enabledOrientations 允许的朝向。
 * @property batchNo 批号。
 * @property warehouse 仓库。
 * @property packageAttribute 包裹属性。
*/
data class QuantityItem<V : FloatingNumber<V>>(
    val id: ItemId,
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
        id: ItemId,
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

    /**
     * 转换为领域模型。
     * Convert to the domain model.
     * @param materialCache 材料缓存，用于避免重复创建。
     * @param itemCache 物品缓存，用于避免重复创建。
     * @return 实际物品模型。
    */
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

/**
 * 量纲物品放置。
 * Quantity item placement.
 * @property item 物品。
 * @property x X 坐标。
 * @property y Y 坐标。
 * @property z Z 坐标。
 * @property orientation 朝向。
*/
data class QuantityItemPlacement<V : FloatingNumber<V>>(
    val item: QuantityItem<V>,
    val x: Quantity<V>,
    val y: Quantity<V>,
    val z: Quantity<V>,
    val orientation: Orientation = Orientation.Upright
) {

    /**
     * 转换为领域模型。
     * Convert to the domain model.
     * @param materialCache 材料缓存，用于避免重复创建。
     * @param itemCache 物品缓存，用于避免重复创建。
     * @return 物品放置模型。
    */
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

/**
 * 量纲箱子层。
 * Quantity bin layer.
*/
data class QuantityBinLayer<V : FloatingNumber<V>>(
    val iteration: Int64,
    val from: KClass<*>,
    val width: Quantity<V>,
    val height: Quantity<V>,
    val depth: Quantity<V>,
    val units: List<QuantityItemPlacement<V>>
) {

    /**
     * 转换为领域模型。
     * Convert to the domain model.
     * @param materialCache 材料缓存，用于避免重复创建。
     * @param itemCache 物品缓存，用于避免重复创建。
     * @return 箱子层模型。
    */
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
