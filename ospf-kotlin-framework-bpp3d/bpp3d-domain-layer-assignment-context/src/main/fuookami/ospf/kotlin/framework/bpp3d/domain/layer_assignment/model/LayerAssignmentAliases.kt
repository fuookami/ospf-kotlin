/**
 * 层分配标量别名。
 * Layer assignment scalar aliases.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model

import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*

fun layerAssignmentOne(): FltX = FltX.one
fun layerAssignmentZero(): FltX = FltX.zero
fun layerAssignmentScalarProvider() = FltX
