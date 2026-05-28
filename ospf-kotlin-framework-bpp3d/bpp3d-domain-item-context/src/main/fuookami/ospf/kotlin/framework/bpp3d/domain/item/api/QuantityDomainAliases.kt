@file:Suppress("DEPRECATION")

package fuookami.ospf.kotlin.framework.bpp3d.domain.item.api

import fuookami.ospf.kotlin.math.algebra.number.FltX





/**
 * Flt64 兼容别名（路线 A 迁移期）
 * Flt64 compatibility aliases (route A migration phase)
 */
typealias Flt64Material = Material<LegacyScalar>
typealias Flt64PackageShape = PackageShape<LegacyScalar>
typealias Flt64Package = Package<LegacyScalar>
typealias Flt64Item = Item<LegacyScalar>
typealias Flt64ItemPlacement = ItemPlacement<LegacyScalar>
typealias Flt64BinLayer = BinLayer<LegacyScalar>

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
