package fuookami.ospf.kotlin.math.symbol.serde

import kotlinx.serialization.Serializable

@Serializable
data class CanonicalInequalityData(
    val lhs: CanonicalPolynomialData,
    val rhs: CanonicalPolynomialData,
    val comparison: String
)

@Serializable
data class LinearInequalityData(
    val lhs: LinearPolynomialData,
    val rhs: LinearPolynomialData,
    val comparison: String,
    val name: String = "",
    val displayName: String = ""
)

@Serializable
data class QuadraticInequalityData(
    val lhs: QuadraticPolynomialData,
    val rhs: QuadraticPolynomialData,
    val comparison: String,
    val name: String = "",
    val displayName: String = ""
)
