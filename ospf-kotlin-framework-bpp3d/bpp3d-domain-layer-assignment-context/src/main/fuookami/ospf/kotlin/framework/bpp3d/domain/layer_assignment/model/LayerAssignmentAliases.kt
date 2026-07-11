/**
 * Layer assignment scalar aliases.
 * 层分配标量别名。
*/
package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model

import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*

/**
 * Returns the scalar one value for layer assignment.
 * 返回层分配的标量一值。
 *
 * @return The FltX one value.
 * 返回 FltX 的一值。
*/
fun layerAssignmentOne(): FltX = FltX.one

/**
 * Returns the scalar zero value for layer assignment.
 * 返回层分配的标量零值。
 *
 * @return The FltX zero value.
 * 返回 FltX 的零值。
*/
fun layerAssignmentZero(): FltX = FltX.zero

/**
 * Provides the scalar provider for layer assignment.
 * 提供层分配的标量提供者。
 *
 * @return The FltX scalar provider.
 * 返回 FltX 标量提供者。
*/
fun layerAssignmentScalarProvider() = FltX
