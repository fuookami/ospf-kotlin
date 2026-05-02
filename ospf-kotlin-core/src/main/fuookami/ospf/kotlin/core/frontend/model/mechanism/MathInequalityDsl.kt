@file:Suppress("unused")

package fuookami.ospf.kotlin.core.frontend.model.mechanism

// Frontend inequality DSL has been consolidated into:
// fuookami.ospf.kotlin.core.model.mechanism.MathInequalityDsl
//
// This file re-exports the model/mechanism DSL for backward compatibility.

@Deprecated("Import from fuookami.ospf.kotlin.core.model.mechanism instead", level = DeprecationLevel.HIDDEN)
typealias LinearInequality<V> = fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality<V>

@Deprecated("Import from fuookami.ospf.kotlin.core.model.mechanism instead", level = DeprecationLevel.HIDDEN)
typealias QuadraticInequality<V> = fuookami.ospf.kotlin.math.symbol.inequality.QuadraticInequalityOf<V>
