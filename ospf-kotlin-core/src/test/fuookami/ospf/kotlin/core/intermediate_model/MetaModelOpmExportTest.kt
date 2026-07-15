package fuookami.ospf.kotlin.core.intermediate_model

import java.nio.file.Files
import kotlin.io.path.deleteIfExists
import kotlin.test.*
import kotlinx.coroutines.runBlocking
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.RealVar

class MetaModelOpmExportTest {
    @Test
    fun opmExportShouldWriteReadableLinearConstraints() {
        val purchase = RealVar("purchase_1")
        val model = LinearMetaModel(
            name = "opm-readable-constraints",
            objectCategory = ObjectCategory.Minimum,
            converter = IntoValue.fromConverter(Flt64)
        )
        val path = Files.createTempFile("opm-readable-constraints", ".opm")

        try {
            assertTrue(model.add(listOf(purchase)) is Ok)
            assertTrue(
                model.addConstraint(
                    relation = LinearInequality(
                        lhs = LinearPolynomial(
                            monomials = listOf(LinearMonomial(Flt64.one, purchase)),
                            constant = Flt64.zero
                        ),
                        rhs = LinearPolynomial(emptyList(), Flt64(400.0)),
                        comparison = Comparison.GE
                    )
                ) is Ok
            )
            assertTrue(
                model.addConstraint(
                    relation = LinearInequality(
                        lhs = LinearPolynomial(
                            monomials = listOf(LinearMonomial(Flt64.one, purchase)),
                            constant = Flt64.zero
                        ),
                        rhs = LinearPolynomial(emptyList(), Flt64(500.0)),
                        comparison = Comparison.LE
                    ),
                    name = "purchase_upper"
                ) is Ok
            )

            val result = runBlocking {
                model.export(path)
            }

            assertTrue(result is Ok)
            val content = Files.readString(path)
            assertContains(content, "purchase_1 >= 400.0")
            assertContains(content, "purchase_upper: purchase_1 <= 500.0")
            assertFalse(content.contains("LinearInequality(lhs="))
        } finally {
            model.close()
            path.deleteIfExists()
        }
    }
}
