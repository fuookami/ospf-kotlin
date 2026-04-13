package fuookami.ospf.kotlin.math.symbol.serde

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.OwnedSymbol
import fuookami.ospf.kotlin.math.symbol.OwnedSymbolLike
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.SymbolId
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.utils.serialization.writeJson
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SymbolIdentitySerdeTest {
    private fun ByteArray.toHex(): String {
        return joinToString(separator = "") { b -> "%02x".format(b) }
    }

    private fun encodeIdentityIdentifier(identityExpr: SymbolIdentityExpr): String {
        val payload = writeJson(identityExpr).toByteArray(Charsets.UTF_8).toHex()
        return "__ospf_symbol_identity__$payload"
    }

    @Test
    fun linearPolynomialJsonShouldKeepOwnedSymbolId() {
        val ownedSymbol = OwnedSymbol(
            id = SymbolId("symbol-x-id"),
            name = "x",
            displayName = "X"
        )
        val polynomial = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64.two, ownedSymbol)),
            constant = Flt64.one
        )

        val json = polynomial.toJsonString()
        val restored = linearPolynomialFromJson(json)
        val restoredSymbol = restored?.monomials?.firstOrNull()?.symbol

        assertTrue(restoredSymbol is OwnedSymbolLike)
        assertEquals("symbol-x-id", (restoredSymbol as OwnedSymbolLike).id.value)
    }

    @Test
    fun symbolIdentifierParserShouldDecodeCompositeIdentity() {
        val identityExpr = SymbolIdentityExpr.CompositeMulti(
            operator = "join",
            args = listOf(
                SymbolIdentityExpr.Simple(name = "a"),
                SymbolIdentityExpr.WithId(name = "b", id = "b-id")
            ),
            name = "join(a,b)",
            displayName = "join(a,b)"
        )
        val payload = writeJson(identityExpr).toByteArray(Charsets.UTF_8).toHex()
        val encodedIdentifier = "__ospf_symbol_identity__$payload"

        val decodedSymbol: Symbol = symbolOfSerializedIdentifier(encodedIdentifier)
        val decodedIdentity = decodedSymbol.toSymbolIdentityExpr()

        assertEquals(identityExpr, decodedIdentity)
    }

    @Test
    fun symbolIdentifierParserShouldRoundTripCommonIdentityVariants() {
        val identities = listOf(
            SymbolIdentityExpr.Simple(name = "x"),
            SymbolIdentityExpr.WithId(name = "x", id = "x-id"),
            SymbolIdentityExpr.CompositeMulti(
                operator = "join",
                args = listOf(
                    SymbolIdentityExpr.Simple(name = "a"),
                    SymbolIdentityExpr.WithId(name = "b", id = "b-id")
                ),
                name = "join(a,b)",
                displayName = "join(a,b)"
            )
        )

        for (identity in identities) {
            val encodedIdentifier = encodeIdentityIdentifier(identity)
            val decodedIdentity = symbolOfSerializedIdentifier(encodedIdentifier).toSymbolIdentityExpr()
            assertEquals(identity, decodedIdentity)
        }
    }
}
