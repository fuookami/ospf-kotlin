@file:Suppress("DEPRECATION")

package fuookami.ospf.kotlin.framework.bpp3d.domain.item.api

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.FltX

/**
 * Flt64 兼容别名（路线 A 迁移期）
 * Flt64 compatibility aliases (route A migration phase)
 */
typealias Flt64Material = Material<Flt64>
typealias Flt64PackageShape = PackageShape<Flt64>
typealias Flt64Package = Package<Flt64>
typealias Flt64Item = Item<Flt64>
typealias Flt64ItemPlacement = ItemPlacement<Flt64>
typealias Flt64BinLayer = BinLayer<Flt64>

/**
 * FltX 直连别名（用于 APS/高精度调用）
 * FltX direct aliases (for APS/high precision paths)
 */
typealias FltXMaterial = Material<FltX>
typealias FltXPackageShape = PackageShape<FltX>
typealias FltXPackage = Package<FltX>
typealias FltXItem = Item<FltX>
typealias FltXItemPlacement = ItemPlacement<FltX>
typealias FltXBinLayer = BinLayer<FltX>
