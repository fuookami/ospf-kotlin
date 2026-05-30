/**
 * 货物模型类型别名。
 * Item model type aliases.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.item.model

import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.AbstractCuboid
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.InfraNumber

/**
 * 物料/载具统一立方体别名。
 * Unified cuboid alias for item-domain model.
 */
typealias ItemCuboid = AbstractCuboid<InfraNumber>