package fuookami.ospf.kotlin.math.symbol.serde

import kotlinx.serialization.Serializable

@Serializable
data class CanonicalPolynomialData(
    val monomials: List<CanonicalMonomialData> = emptyList(),
    val constant: Double
)

@Serializable
data class CanonicalMonomialData(
    val coefficient: Double,
    val powers: Map<String, Int> = emptyMap()
)

@Serializable
data class LinearPolynomialData(
    val monomials: List<LinearMonomialData> = emptyList(),
    val constant: Double
)

@Serializable
data class LinearMonomialData(
    val coefficient: Double,
    val symbol: String
)

@Serializable
data class QuadraticPolynomialData(
    val monomials: List<QuadraticMonomialData> = emptyList(),
    val constant: Double
)

@Serializable
data class QuadraticMonomialData(
    val coefficient: Double,
    val symbol1: String,
    val symbol2: String? = null
)