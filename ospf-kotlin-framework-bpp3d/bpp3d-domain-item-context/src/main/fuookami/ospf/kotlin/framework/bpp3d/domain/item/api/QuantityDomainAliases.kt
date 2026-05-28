@file:Suppress("DEPRECATION")

package fuookami.ospf.kotlin.framework.bpp3d.domain.item.api

import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.InfraNumber
import fuookami.ospf.kotlin.math.algebra.number.FltX





/**
 * InfraNumber 兼容别名（路线 A 迁移期）
 * InfraNumber compatibility aliases (route A migration phase)
 */
typealias InfraNumberMaterial = Material<InfraNumber>
typealias InfraNumberPackageShape = PackageShape<InfraNumber>
typealias InfraNumberPackage = Package<InfraNumber>
typealias InfraNumberItem = Item<InfraNumber>
typealias InfraNumberItemPlacement = ItemPlacement<InfraNumber>
typealias InfraNumberBinLayer = BinLayer<InfraNumber>

/**
 * 旧命名别名（测试与迁移兼容）
 * Legacy-named aliases (test/migration compatibility)
 */
typealias Flt64Material = InfraNumberMaterial
typealias Flt64PackageShape = InfraNumberPackageShape
typealias Flt64Package = InfraNumberPackage
typealias Flt64Item = InfraNumberItem
typealias Flt64ItemPlacement = InfraNumberItemPlacement
typealias Flt64BinLayer = InfraNumberBinLayer

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

