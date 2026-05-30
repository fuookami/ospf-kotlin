package fuookami.ospf.kotlin.framework.bpp3d.domain.packing.model

import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.InfraNumber
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.infraOne
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.infraScalar
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.infraZero
import fuookami.ospf.kotlin.quantities.quantity.Quantity

/**
 * 鐗╂枡鍖呰涓昏矾寰勬爣閲忓埆鍚嶏紙鍏煎杈圭晫锛夈€?
 * Material packing main-path scalar alias (compatibility boundary).
 */
typealias MaterialPackingNumber = InfraNumber

/**
 * 鐗╂枡鍖呰涓昏矾寰勬暟閲忓埆鍚嶏紙鍏煎杈圭晫锛夈€?
 * Material packing main-path quantity alias (compatibility boundary).
 */
typealias MaterialPackingQuantity = Quantity<MaterialPackingNumber>

/**
 * 鐗╂枡鍖呰鏍囬噺闆跺€笺€?
 * Zero value for material packing scalar.
 */
fun materialPackingZero(): MaterialPackingNumber = infraZero()

/**
 * 鐗╂枡鍖呰鏍囬噺涓€鍊笺€?
 * One value for material packing scalar.
 */
fun materialPackingOne(): MaterialPackingNumber = infraOne()

/**
 * 浠?double 鏋勯€犵墿鏂欏寘瑁呮爣閲忋€?
 * Build material packing scalar from double.
 */
fun materialPackingScalar(value: Double): MaterialPackingNumber = infraScalar(value)
