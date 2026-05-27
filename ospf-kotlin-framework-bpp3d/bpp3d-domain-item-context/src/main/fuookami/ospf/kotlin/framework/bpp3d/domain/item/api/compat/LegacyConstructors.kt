@file:Suppress("DEPRECATION")

package fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.compat

import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.BinLayer
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.Item
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.ItemPlacement
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.Material
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.Package
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.PackageShape
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.Flt64BinLayer
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.Flt64Item
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.Flt64ItemPlacement
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.Flt64Material
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.Flt64Package
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.Flt64PackageShape
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.PackageAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.AbstractCargoAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.MaterialType
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.BatchNo
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.MaterialNo
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Orientation
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PackageCode
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PackagePattern
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PackageType
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import kotlin.reflect.KClass





/**
 * Flt64 兼容构造 facade。
 * Flt64 compatibility constructor facade.
 */
object LegacyConstructors {
    fun material(
        no: MaterialNo,
        type: MaterialType,
        cargo: AbstractCargoAttribute,
        name: String,
        manufacturer: String? = null,
        supplier: String? = null,
        warehouse: String? = null,
        weight: Quantity<Flt64>
    ): Flt64Material {
        return Material(
            no = no,
            type = type,
            cargo = cargo,
            name = name,
            manufacturer = manufacturer,
            supplier = supplier,
            warehouse = warehouse,
            weight = weight
        )
    }

    fun packageShape(
        width: Quantity<Flt64>,
        height: Quantity<Flt64>,
        depth: Quantity<Flt64>,
        weight: Quantity<Flt64>,
        packageType: PackageType
    ): Flt64PackageShape {
        return PackageShape(
            width = width,
            height = height,
            depth = depth,
            weight = weight,
            packageType = packageType
        )
    }

    fun innerPackage(
        code: PackageCode? = null,
        pattern: PackagePattern? = null,
        shape: Flt64PackageShape,
        materials: Map<Flt64Material, UInt64>,
        amount: UInt64 = UInt64.one,
        pending: Boolean = false
    ): Flt64Package {
        return Package.innerPackage(
            code = code,
            pattern = pattern,
            shape = shape,
            materials = materials,
            amount = amount,
            pending = pending
        )
    }

    fun outerPackage(
        code: PackageCode? = null,
        pattern: PackagePattern? = null,
        shape: Flt64PackageShape,
        packages: List<Flt64Package>,
        amount: UInt64 = UInt64.one,
        pending: Boolean = false
    ): Flt64Package {
        return Package.outerPackage(
            code = code,
            pattern = pattern,
            shape = shape,
            packages = packages,
            amount = amount,
            pending = pending
        )
    }

    fun item(
        id: String,
        name: String,
        packageCode: PackageCode? = null,
        pack: Flt64Package? = null,
        width: Quantity<Flt64>,
        height: Quantity<Flt64>,
        depth: Quantity<Flt64>,
        weight: Quantity<Flt64>,
        enabledOrientations: List<Orientation>,
        batchNo: BatchNo? = null,
        warehouse: String? = null,
        packageAttribute: PackageAttribute
    ): Flt64Item {
        return Item(
            id = id,
            name = name,
            packageCode = packageCode,
            pack = pack,
            width = width,
            height = height,
            depth = depth,
            weight = weight,
            enabledOrientations = enabledOrientations,
            batchNo = batchNo,
            warehouse = warehouse,
            packageAttribute = packageAttribute
        )
    }

    fun item(
        id: String,
        name: String,
        pack: Flt64Package,
        enabledOrientations: List<Orientation>,
        batchNo: BatchNo? = null,
        warehouse: String? = null,
        packageAttribute: PackageAttribute
    ): Flt64Item {
        return Item(
            id = id,
            name = name,
            pack = pack,
            enabledOrientations = enabledOrientations,
            batchNo = batchNo,
            warehouse = warehouse,
            packageAttribute = packageAttribute
        )
    }

    fun itemPlacement(
        item: Flt64Item,
        x: Quantity<Flt64>,
        y: Quantity<Flt64>,
        z: Quantity<Flt64>,
        orientation: Orientation = Orientation.Upright
    ): Flt64ItemPlacement {
        return ItemPlacement(
            item = item,
            x = x,
            y = y,
            z = z,
            orientation = orientation
        )
    }

    fun binLayer(
        iteration: Int64,
        from: KClass<*>,
        width: Quantity<Flt64>,
        height: Quantity<Flt64>,
        depth: Quantity<Flt64>,
        units: List<Flt64ItemPlacement>
    ): Flt64BinLayer {
        return BinLayer(
            iteration = iteration,
            from = from,
            width = width,
            height = height,
            depth = depth,
            units = units
        )
    }
}
