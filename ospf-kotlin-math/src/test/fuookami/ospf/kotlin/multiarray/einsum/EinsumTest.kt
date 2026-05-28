/**
 * Einstein notation tests
 *
 * Test contents:
 * - Index labels and index lists
 * - Matrix multiplication matmul
 * - Dot product dot
 * - Trace trace
 * - Outer product outer
 * - Transpose transpose
 * - Tensor contraction contract
 * - String notation einsum
 * - DSL API
 */
package fuookami.ospf.kotlin.multiarray.einsum

import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.multiarray.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.operator.abs

/**
 * Einstein notation tests
 *
 * Aligned with Rust implementation: ospf-rust-multiarray/src/einsum/tests.rs
 */
class EinsumTest {

    // ========================================================================
    // Index label tests
    // ========================================================================

    @Test
    fun testIndexLabels() {
        // Test index label properties
        assertEquals("i", IndexLabel.I.labelName)
        assertEquals("j", IndexLabel.J.labelName)
        assertEquals("k", IndexLabel.K.labelName)

        assertEquals(0, IndexLabel.I.id)
        assertEquals(1, IndexLabel.J.id)
        assertEquals(2, IndexLabel.K.id)

        // Test finding by name
        assertEquals(IndexLabel.I, IndexLabel.fromName("i"))
        assertEquals(IndexLabel.J, IndexLabel.fromName("j"))
        assertEquals(null, IndexLabel.fromName("x"))

        // Test finding by ID
        assertEquals(IndexLabel.I, IndexLabel.fromId(0))
        assertEquals(IndexLabel.J, IndexLabel.fromId(1))
        assertEquals(null, IndexLabel.fromId(100))
    }

    @Test
    fun testIndexLists() {
        // Test empty list
        val empty = IndexList.Empty
        assertEquals(0, empty.length)
        assertTrue(empty.isEmpty())

        // Test single element list
        val single = IndexList.of(IndexLabel.I)
        assertEquals(1, single.length)
        assertEquals(listOf(0), single.ids)
        assertEquals("i", single.names)

        // Test double element list
        val double = IndexList.of(IndexLabel.I, IndexLabel.J)
        assertEquals(2, double.length)
        assertEquals(listOf(0, 1), double.ids)
        assertEquals("i, j", double.names)

        // Test triple element list
        val triple = IndexList.of(IndexLabel.I, IndexLabel.J, IndexLabel.K)
        assertEquals(3, triple.length)
        assertEquals(listOf(0, 1, 2), triple.ids)

        // Test parsing from names
        val parsed = IndexList.fromNames("i, j, k")
        assertEquals(3, parsed.length)
        assertEquals(listOf(IndexLabel.I, IndexLabel.J, IndexLabel.K), parsed.indices)
    }

    @Test
    fun testFindCommonIndices() {
        // Test finding common indices
        val lhs = IndexList.of(IndexLabel.I, IndexLabel.J, IndexLabel.K)  // i, j, k
        val rhs = IndexList.of(IndexLabel.J, IndexLabel.K, IndexLabel.L)  // j, k, l

        val common = findCommonIndices(lhs, rhs)

        // Result should be j, k
        assertEquals(2, common.length)
        assertTrue(common.contains(IndexLabel.J))
        assertTrue(common.contains(IndexLabel.K))
    }

    @Test
    fun testRemoveIndices() {
        // Test removing indices
        val indices = IndexList.of(IndexLabel.I, IndexLabel.J, IndexLabel.K, IndexLabel.L)
        val toRemove = IndexList.of(IndexLabel.J, IndexLabel.L)

        val result = removeIndices(indices, toRemove)

        // Result should be i, k
        assertEquals(2, result.length)
        assertTrue(result.contains(IndexLabel.I))
        assertTrue(result.contains(IndexLabel.K))
    }

    // ========================================================================
    // Tensor expression tests
    // ========================================================================

    @Test
    fun testTensorExprCreation() {
        // Create 2x3 matrix
        val matrix = MultiArray.newWith(Shape2(2, 3), Flt64.one)

        // Create tensor expression
        val expr = TensorExpr(matrix, IndexList.of(IndexLabel.I, IndexLabel.J))

        assertEquals("i, j", expr.indexNames)
        assertEquals(listOf(0, 1), expr.indexIds)
        assertEquals(6, expr.size)
        assertEquals(2, expr.dimension)
    }

    @Test
    fun testTensorExprWithDefaultIndices() {
        // Test auto-assigning default indices
        val matrix = MultiArray.newWith(Shape2(2, 3), Flt64.one)
        val expr = TensorExpr.withDefaultIndices(matrix)

        assertEquals(2, expr.indices.length)
        assertEquals(IndexLabel.I, expr.indices.indices[0])
        assertEquals(IndexLabel.J, expr.indices.indices[1])
    }

    @Test
    fun testTensorExprDimensionMismatch() {
        // Test throwing exception when dimensions don't match
        val matrix = MultiArray.newWith(Shape2(2, 3), Flt64.one)

        assertFailsWith<EinsumError.IndexListLengthMismatch> {
            TensorExpr(matrix, IndexList.of(IndexLabel.I))  // Expected 2 indices, but only 1
        }
    }

    // ========================================================================
    // Matrix multiplication tests
    // ========================================================================

    @Test
    fun testMatmulBasic() {
        // Create simple 2x2 matrices
        val a = MultiArray.newBy(Shape2(2, 2)) { i, _ -> Flt64(i + 1.0) }
        val b = MultiArray.newBy(Shape2(2, 2)) { i, _ -> Flt64(i + 5.0) }

        // Matrix multiplication
        val c = matmul(a, b, Flt64.zero)

        // Result shape should be 2x2
        assertEquals(2, c.shape.dimension)
        assertEquals(4, c.size)
    }

    @Test
    fun testMatmulShapeMismatch() {
        // Test error handling for shape mismatch
        val a = MultiArray.newWith(Shape2(2, 3), Flt64.one)
        val b = MultiArray.newWith(Shape2(4, 5), Flt64.one)

        // This matrix multiplication should fail (dimension mismatch)
        assertFailsWith<EinsumError.IncompatibleShapes> {
            matmul(a, b, Flt64.zero)
        }
    }

    @Test
    fun testMatmulCorrectness() {
        // Test matrix multiplication correctness
        // A = [[1, 2], [3, 4]]
        val a = MultiArray.newBy(Shape2(2, 2)) { _, vec ->
            Flt64(vec[0] * 2 + vec[1] + 1)
        }
        // B = [[5, 6], [7, 8]]
        val b = MultiArray.newBy(Shape2(2, 2)) { _, vec ->
            Flt64(vec[0] * 2 + vec[1] + 5)
        }

        // C = A @ B
        val c = matmul(a, b, Flt64.zero)

        // Verify C[0,0] = 1*5 + 2*7 = 19
        assertTrue((c[intArrayOf(0, 0)] - Flt64(19.0)).abs() < Flt64(1e-10))
    }

    // ========================================================================
    // Dot product tests
    // ========================================================================

    @Test
    fun testDotProduct() {
        // Create vectors
        val a = MultiArray.newBy(Shape1(3)) { i, _ -> Flt64(i + 1.0) }
        val b = MultiArray.newBy(Shape1(3)) { i, _ -> Flt64(i + 1.0) }

        // Dot product: 1*1 + 2*2 + 3*3 = 14
        val result = dot(a, b, Flt64.zero)
        assertTrue((result - Flt64(14.0)).abs() < Flt64(1e-10))
    }

    @Test
    fun testDotProductLengthMismatch() {
        // Test length mismatch
        val a = MultiArray.newWith(Shape1(3), Flt64.one)
        val b = MultiArray.newWith(Shape1(4), Flt64.one)

        assertFailsWith<EinsumError.IncompatibleShapes> {
            dot(a, b, Flt64.zero)
        }
    }

    // ========================================================================
    // Trace tests
    // ========================================================================

    @Test
    fun testTrace() {
        // Create 3x3 identity matrix
        val a = MultiArray.newBy(Shape2(3, 3)) { _, vec ->
            if (vec[0] == vec[1]) Flt64.one else Flt64.zero
        }

        // Trace = 3
        val result = trace(a, Flt64.zero)
        assertTrue((result - Flt64(3.0)).abs() < Flt64(1e-10))
    }

    @Test
    fun testTraceNonSquare() {
        // Test trace of non-square matrix
        val a = MultiArray.newWith(Shape2(2, 3), Flt64.one)

        assertFailsWith<EinsumError.UnsupportedOperation> {
            trace(a, Flt64.zero)
        }
    }

    // ========================================================================
    // Outer product tests
    // ========================================================================

    @Test
    fun testOuterProduct() {
        // Create vectors
        val a = MultiArray.newBy(Shape1(2)) { i, _ -> Flt64(i + 1.0) }  // [1, 2]
        val b = MultiArray.newBy(Shape1(3)) { i, _ -> Flt64(i + 1.0) }  // [1, 2, 3]

        // Outer product
        val result = outer(a, b, Flt64.zero)

        // Result shape should be 2x3
        assertEquals(2, result.shape.dimension)
        assertEquals(2, result.shape[0])
        assertEquals(3, result.shape[1])

        // Verify values: result[i,j] = a[i] * b[j]
        assertTrue((result[intArrayOf(0, 0)] - Flt64(1.0)).abs() < Flt64(1e-10))  // 1*1
        assertTrue((result[intArrayOf(0, 1)] - Flt64(2.0)).abs() < Flt64(1e-10))  // 1*2
        assertTrue((result[intArrayOf(0, 2)] - Flt64(3.0)).abs() < Flt64(1e-10))  // 1*3
        assertTrue((result[intArrayOf(1, 0)] - Flt64(2.0)).abs() < Flt64(1e-10))  // 2*1
        assertTrue((result[intArrayOf(1, 1)] - Flt64(4.0)).abs() < Flt64(1e-10))  // 2*2
        assertTrue((result[intArrayOf(1, 2)] - Flt64(6.0)).abs() < Flt64(1e-10))  // 2*3
    }

    // ========================================================================
    // Transpose tests
    // ========================================================================

    @Test
    fun testTranspose() {
        // Create 2x3 matrix
        val a = MultiArray.newBy(Shape2(2, 3)) { i, _ -> Flt64(i) }

        val result = transpose(a)

        // Shape after transpose should be 3x2
        assertEquals(3, result.shape[0])
        assertEquals(2, result.shape[1])
    }

    @Test
    fun testTransposeCorrectness() {
        // Test transpose correctness
        val a = MultiArray.newBy(Shape2(2, 3)) { _, vec ->
            Flt64(vec[0] * 10 + vec[1])
        }

        val result = transpose(a)

        // Verify values
        // a[0,2] = 2 -> result[2,0] = 2
        assertTrue((a[intArrayOf(0, 2)] - result[intArrayOf(2, 0)]).abs() < Flt64(1e-10))
        // a[1,0] = 10 -> result[0,1] = 10
        assertTrue((a[intArrayOf(1, 0)] - result[intArrayOf(0, 1)]).abs() < Flt64(1e-10))
    }

    // ========================================================================
    // Empty matrix tests
    // ========================================================================

    @Test
    fun testTransposeEmptyRowMatrix() {
        // shape [0, 3] -> [3, 0]
        val a = MultiArray.newWith(Shape2(0, 3), Flt64.one)
        val result = transpose(a)

        assertEquals(3, result.shape[0])
        assertEquals(0, result.shape[1])
    }

    @Test
    fun testTransposeEmptyColMatrix() {
        // shape [2, 0] -> [0, 2]
        val a = MultiArray.newWith(Shape2(2, 0), Flt64.one)
        val result = transpose(a)

        assertEquals(0, result.shape[0])
        assertEquals(2, result.shape[1])
    }

    // ========================================================================
    // Tensor contraction tests
    // ========================================================================

    @Test
    fun testContractBasic() {
        // Test basic contraction
        val a = MultiArray.newWith(Shape2(2, 3), Flt64.one)
        val b = MultiArray.newWith(Shape2(3, 4), Flt64(2.0))

        // Contract axis 1 of a with axis 0 of b, equivalent to matrix multiplication
        val result = contract(a, 1, b, 0, Flt64.zero)

        // Result shape should be 2x4
        assertEquals(2, result.shape[0])
        assertEquals(4, result.shape[1])
    }

    @Test
    fun testContractHighDimCorrectness() {
        val a = MultiArray.newBy(Shape3(2, 2, 3)) { _, v ->
            Flt64(v[0] * 100 + v[1] * 10 + v[2] + 1)
        }
        val b = MultiArray.newBy(Shape3(3, 2, 2)) { _, v ->
            Flt64(v[0] * 100 + v[1] * 10 + v[2] + 1)
        }

        val result = contract(a, 2, b, 0, Flt64.zero)

        assertEquals(4, result.shape.dimension)
        assertEquals(2, result.shape[0])
        assertEquals(2, result.shape[1])
        assertEquals(2, result.shape[2])
        assertEquals(2, result.shape[3])

        for (i in 0 until 2) {
            for (j in 0 until 2) {
                for (m in 0 until 2) {
                    for (n in 0 until 2) {
                        var expected = Flt64.zero
                        for (k in 0 until 3) {
                            expected = expected + a[intArrayOf(i, j, k)] * b[intArrayOf(k, m, n)]
                        }
                        val actual = result[intArrayOf(i, j, m, n)]
                        assertTrue(
                            (actual - expected).abs() < Flt64(1e-10),
                            "mismatch at [$i,$j,$m,$n], expected=$expected, actual=$actual"
                        )
                    }
                }
            }
        }
    }

    // ========================================================================
    // Negative axis validation tests
    // ========================================================================

    @Test
    fun testContractNegativeAxisA() {
        val a = MultiArray.newWith(Shape2(2, 3), Flt64.one)
        val b = MultiArray.newWith(Shape2(3, 4), Flt64(2.0))

        assertFailsWith<EinsumError.IndexOutOfBounds> {
            contract(a, -1, b, 0, Flt64.zero)
        }
    }

    @Test
    fun testContractNegativeAxisB() {
        val a = MultiArray.newWith(Shape2(2, 3), Flt64.one)
        val b = MultiArray.newWith(Shape2(3, 4), Flt64(2.0))

        assertFailsWith<EinsumError.IndexOutOfBounds> {
            contract(a, 0, b, -1, Flt64.zero)
        }
    }

    // ========================================================================
    // einsum string notation tests
    // ========================================================================

    @Test
    fun testEinsumStringMatmul() {
        // Test string notation matrix multiplication
        val a = MultiArray.newWith(Shape2(2, 3), Flt64.one)
        val b = MultiArray.newWith(Shape2(3, 4), Flt64(2.0))

        val result = einsum(a, b, "ij,jk->ik", Flt64.zero)

        // Verify shape
        assertTrue(result is MultiArray<*, *>)
        val resultArray = result as MultiArray<*, *>
        assertEquals(2, resultArray.shape[0])
        assertEquals(4, resultArray.shape[1])
    }

    @Test
    fun testEinsumStringDot() {
        // Test string notation dot product
        val a = MultiArray.newBy(Shape1(3)) { i, _ -> Flt64(i + 1.0) }
        val b = MultiArray.newBy(Shape1(3)) { i, _ -> Flt64(i + 1.0) }

        val result = einsum(a, b, "i,i->", Flt64.zero)

        // Dot product result should be scalar
        assertTrue(result is Flt64)
        assertTrue((result - Flt64(14.0)).abs() < Flt64(1e-10))
    }

    @Test
    fun testEinsumStringOuter() {
        // Test string notation outer product
        val a = MultiArray.newWith(Shape1(2), Flt64.one)
        val b = MultiArray.newWith(Shape1(3), Flt64(2.0))

        val result = einsum(a, b, "i,j->ij", Flt64.zero)

        // Verify shape
        assertTrue(result is MultiArray<*, *>)
        val resultArray = result as MultiArray<*, *>
        assertEquals(2, resultArray.shape[0])
        assertEquals(3, resultArray.shape[1])
    }

    @Test
    fun testEinsumStringTrace() {
        // Test string notation trace
        val a = MultiArray.newBy(Shape2(3, 3)) { _, vec ->
            if (vec[0] == vec[1]) Flt64.one else Flt64.zero
        }

        val result = einsum(a, "ii->", Flt64.zero)

        assertTrue((result as Flt64 - Flt64(3.0)).abs() < Flt64(1e-10))
    }

    @Test
    fun testEinsumStringTranspose() {
        // Test string notation transpose
        val a = MultiArray.newBy(Shape2(2, 3)) { i, _ -> Flt64(i) }

        val result = einsum(a, "ij->ji", Flt64.zero)

        // Verify shape
        assertTrue(result is MultiArray<*, *>)
        val resultArray = result as MultiArray<*, *>
        assertEquals(3, resultArray.shape[0])
        assertEquals(2, resultArray.shape[1])
    }

    // ========================================================================
    // DSL API tests
    // ========================================================================

    @Test
    fun testEinsteinDslMatmul() {
        // Test DSL matrix multiplication
        val a = MultiArray.newWith(Shape2(2, 3), Flt64.one)
        val b = MultiArray.newWith(Shape2(3, 4), Flt64(2.0))

        val result = einstein(a, b, Flt64.zero).matmul()

        assertEquals(2, result.shape[0])
        assertEquals(4, result.shape[1])
    }

    @Test
    fun testEinsteinDslDot() {
        // Test DSL dot product
        val a = MultiArray.newBy(Shape1(3)) { i, _ -> Flt64(i + 1.0) }
        val b = MultiArray.newBy(Shape1(3)) { i, _ -> Flt64(i + 1.0) }

        val result = einstein(a, b, Flt64.zero).dot()

        assertTrue((result - Flt64(14.0)).abs() < Flt64(1e-10))
    }

    @Test
    fun testEinsteinDslOuter() {
        // Test DSL outer product
        val a = MultiArray.newWith(Shape1(2), Flt64.one)
        val b = MultiArray.newWith(Shape1(3), Flt64(2.0))

        val result = einstein(a, b, Flt64.zero).outer()

        assertEquals(2, result.shape[0])
        assertEquals(3, result.shape[1])
    }

    @Test
    fun testEinsteinDslTrace() {
        // Test DSL trace
        val a = MultiArray.newBy(Shape2(3, 3)) { _, vec ->
            if (vec[0] == vec[1]) Flt64.one else Flt64.zero
        }

        val result = einstein(a, Flt64.zero).trace()

        assertTrue((result - Flt64(3.0)).abs() < Flt64(1e-10))
    }

    @Test
    fun testEinsteinDslTranspose() {
        // Test DSL transpose
        val a = MultiArray.newBy(Shape2(2, 3)) { i, _ -> Flt64(i) }

        val result = einstein(a, Flt64.zero).transpose()

        assertEquals(3, result.shape[0])
        assertEquals(2, result.shape[1])
    }

    // ========================================================================
    // Error handling tests
    // ========================================================================

    @Test
    fun testEinsumErrorMessages() {
        // Test error message format

        val dimError = EinsumError.DimensionMismatch(2, 3, "test")
        assertTrue(dimError.message!!.contains("expected 2"))
        assertTrue(dimError.message!!.contains("got 3"))

        val shapeError = EinsumError.IncompatibleShapes(listOf(2, 3), listOf(3, 4), "test")
        assertTrue(shapeError.message!!.contains("[2, 3]"))
        assertTrue(shapeError.message!!.contains("[3, 4]"))

        val dupError = EinsumError.DuplicateIndices(5)
        assertTrue(dupError.message!!.contains("5"))

        val unsupportedError = EinsumError.UnsupportedOperation("test operation")
        assertTrue(unsupportedError.message!!.contains("test operation"))
    }
}
