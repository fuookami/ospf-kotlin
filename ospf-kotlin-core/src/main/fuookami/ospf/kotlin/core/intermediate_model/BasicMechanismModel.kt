package fuookami.ospf.kotlin.core.intermediate_model

import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64

/**
 * Base layer of the mechanism model hierarchy: expanded variables + constraints (no objective).
 *
 * This corresponds to `BasicMechanismModel<V>` in the Rust implementation.
 * `MechanismModel<V>` extends this with the objective function and Benders cut generation.
 *
 * BasicMechanismModel delegates token storage to an [LegacyAbstractTokenTable] provided
 * at construction time — the same mechanism used by MechanismModelF64.
 */
open class BasicMechanismModel<V : RealNumber<V>>(
    open val name: String,
    open val tokens: LegacyAbstractTokenTable
) {

    // ── Query helpers ────────────────────────────────────────────────

    val numVariables: Int get() = tokens.tokens.size
}

typealias BasicMechanismModelF64 = BasicMechanismModel<Flt64>
