@file:Suppress("DEPRECATION")

/**
 * 泛型量纲域别名。
 * Generic quantity domain aliases.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.item.model

import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.InfraNumber

/** InfraNumber 物料别名。InfraNumber material alias. */
typealias InfraNumberMaterial = GenericMaterial<InfraNumber>

/** InfraNumber 包装形状别名。InfraNumber package-shape alias. */
typealias InfraNumberPackageShape = GenericPackageShape<InfraNumber>

/** InfraNumber 包装别名。InfraNumber package alias. */
typealias InfraNumberPackage = GenericPackage<InfraNumber>

/** InfraNumber 货物别名。InfraNumber item alias. */
typealias InfraNumberItem = GenericItem<InfraNumber>

/** InfraNumber 货物摆放别名。InfraNumber item-placement alias. */
typealias InfraNumberItemPlacement = GenericItemPlacement<InfraNumber>

/** InfraNumber 层别名。InfraNumber bin-layer alias. */
typealias InfraNumberBinLayer = GenericBinLayer<InfraNumber>

/** FltX 物料别名。FltX material alias. */
typealias FltXMaterial = GenericMaterial<FltX>

/** FltX 包装形状别名。FltX package-shape alias. */
typealias FltXPackageShape = GenericPackageShape<FltX>

/** FltX 包装别名。FltX package alias. */
typealias FltXPackage = GenericPackage<FltX>

/** FltX 货物别名。FltX item alias. */
typealias FltXItem = GenericItem<FltX>

/** FltX 货物摆放别名。FltX item-placement alias. */
typealias FltXItemPlacement = GenericItemPlacement<FltX>

/** FltX 层别名。FltX bin-layer alias. */
typealias FltXBinLayer = GenericBinLayer<FltX>
