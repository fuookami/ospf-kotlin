package fuookami.ospf.kotlin.core.solver

import kotlin.test.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.basic.Variable
import fuookami.ospf.kotlin.core.solver.report.ModelElementScope
import fuookami.ospf.kotlin.core.variable.*

class ModelingPreparationTest {
    @Test
    fun shouldPrepareVariableDumpingDataWithBoundsNamesAndInitialResults() {
        val variable0 = Variable(
            index = 0,
            lowerBound = Flt64(-3.0),
            upperBound = Flt64(7.0),
            type = Binary,
            origin = null,
            name = "x0",
            initialResult = Flt64(1.0)
        )
        val variable1 = Variable(
            index = 1,
            lowerBound = Flt64.zero,
            upperBound = Flt64(10.0),
            type = Integer,
            origin = null,
            name = "x1",
            initialResult = null
        )

        val dumpingData = prepareVariableDumpingData(
            variables = listOf(variable0, variable1),
            scopeName = "linear"
        )

        assertContentEquals(doubleArrayOf(-3.0, 0.0), dumpingData.lowerBounds)
        assertContentEquals(doubleArrayOf(7.0, 10.0), dumpingData.upperBounds)
        assertContentEquals(arrayOf("x0", "x1"), dumpingData.names)
        assertEquals(1, dumpingData.initialResults.size)
        assertEquals(0, dumpingData.initialResults[0].first)
        assertEquals(1.0, dumpingData.initialResults[0].second)
    }

    @Test
    fun shouldComputeReasonableConstraintSegmentSize() {
        assertEquals(10, computeConstraintSegmentSize(0, 8))
        assertEquals(10, computeConstraintSegmentSize(8, 8))
        assertEquals(10, computeConstraintSegmentSize(50, 8))
        assertEquals(100, computeConstraintSegmentSize(1000, 8))
        assertTrue(computeConstraintSegmentSize(50000, 8) >= 1000)
    }

    @Test
    fun shouldProjectStableIdentityIntoNamespacedNativeName() {
        assertEquals(
            "ospf-variable-fixture_h2f_business-x",
            nativeElementName(
                identityId = "fixture/business-x",
                fallbackName = "x0",
                category = "variable",
                identityScope = ModelElementScope.Stable
            )
        )
        assertEquals(
            "ospf-constraint-non-negative",
            nativeElementName(
                identityId = "non-negative",
                fallbackName = "c0",
                category = "constraint",
                identityScope = ModelElementScope.Stable
            )
        )
        assertEquals(
            "ospf-variable-stable:a:1",
            nativeElementName(
                identityId = "stable:a:1",
                fallbackName = "x1",
                category = "variable",
                identityScope = ModelElementScope.Stable
            )
        )
    }

    @Test
    fun shouldKeepDisplayNameForBlankOrModelLocalIdentity() {
        assertEquals("x0", nativeElementName(null, "x0", "variable"))
        assertEquals("x1", nativeElementName("", "x1", "variable", ModelElementScope.Stable))
        assertEquals("x2", nativeElementName("artifact:generated", "x2", "variable", ModelElementScope.ModelLocal))
    }

    @Test
    fun shouldSanitizeUnsupportedNativeNameCharacters() {
        assertEquals("a_h20_b_h20_c", sanitizeNativeName("a b c"))
        assertEquals("stable_h20_id", sanitizeNativeName("stable id"))
        assertEquals("x_h2c_1", sanitizeNativeName("x,1"))
        assertEquals("dotted.name", sanitizeNativeName("dotted.name"))
        assertEquals("a_h2f_b", sanitizeNativeName("a/b"))
        assertEquals("a_h5f_b", sanitizeNativeName("a_b"))
    }

    @Test
    fun shouldDistinguishPreviouslyCollidingSanitizedNames() {
        val slash = sanitizeNativeName("a/b")
        val space = sanitizeNativeName("a b")
        val underscore = sanitizeNativeName("a_b")
        val comma = sanitizeNativeName("a,b")
        val colon = sanitizeNativeName("a:b")
        val hyphen = sanitizeNativeName("a-b")
        assertEquals(setOf(slash, space, underscore, comma, colon, hyphen).size, 6)
        assertNotEquals(slash, space)
        assertNotEquals(slash, underscore)
        assertNotEquals(space, underscore)
        assertNotEquals(colon, underscore)
        assertNotEquals(hyphen, underscore)
    }

    @Test
    fun shouldRoundTripSanitizedNativeNames() {
        val samples = listOf(
            "a/b",
            "a b",
            "a_b",
            "a,b",
            "a:b",
            "a-b",
            "a.b",
            "稳定:id/1",
            "x,y z_1",
            "a\tb\nc",
            "a_h2f_b"
        )
        for (sample in samples) {
            val encoded = sanitizeNativeName(sample)
            assertEquals(sample, decodeNativeName(encoded), "round trip failed for '$sample'")
        }
    }

    @Test
    fun shouldTruncateOverlyLongSanitizedNameWithDeterministicDigest() {
        val longA = "v" + "长".repeat(200) + "/x"
        val longB = "v" + "长".repeat(200) + "/y"
        val encodedA = sanitizeNativeName(longA)
        val encodedB = sanitizeNativeName(longB)
        assertTrue(encodedA.length <= 180, "sanitized name exceeds native length limit")
        assertEquals(encodedA.length, encodedB.length)
        assertNotEquals(encodedA, encodedB)
        assertEquals(encodedA, sanitizeNativeName(longA))
    }

    private fun decodeNativeName(value: String): String {
        val escape = Regex("_h([0-9a-fA-F]{2})_")
        val bytes = ArrayList<Byte>()
        var index = 0
        while (index < value.length) {
            val match = escape.find(value, index)
            if (match != null && match.range.first == index) {
                bytes.add(match.groupValues[1].toInt(16).toByte())
                index = match.range.last + 1
            } else {
                bytes.add(value[index].code.toByte())
                index += 1
            }
        }
        return bytes.toByteArray().toString(Charsets.UTF_8)
    }
}
