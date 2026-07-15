package fuookami.ospf.kotlin.math.algebra.concept

import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.concept.*

class ConceptCompileTest {
    private interface TestMonoid : Monoid<TestMonoid>
    private interface TestGroup : Group<TestGroup>
    private interface TestAbelianGroup : AbelianGroup<TestAbelianGroup>
    private interface TestRing : Ring<TestRing>
    private interface TestCommutativeRing : CommutativeRing<TestCommutativeRing>
    private interface TestField : Field<TestField>

    private interface TestPlusSemigroup : PlusSemiGroup<TestPlusSemigroup>
    private interface TestPlusGroup : PlusGroup<TestPlusGroup>
    private interface TestNumberRing : NumberRing<TestNumberRing>
    private interface TestNumberField : NumberField<TestNumberField>

    private fun <T> requireSemigroup() where T : Semigroup<T> = Unit
    private fun <T> requireMonoid() where T : Monoid<T> = Unit
    private fun <T> requireGroup() where T : Group<T> = Unit
    private fun <T> requireAbelianGroup() where T : AbelianGroup<T> = Unit
    private fun <T> requireRing() where T : Ring<T> = Unit
    private fun <T> requireCommutativeRing() where T : CommutativeRing<T> = Unit
    private fun <T> requireField() where T : Field<T> = Unit

    @Test
    fun conceptHierarchyShouldCompile() {
        requireSemigroup<TestMonoid>()
        requireMonoid<TestGroup>()
        requireGroup<TestAbelianGroup>()
        requireAbelianGroup<TestRing>()
        requireRing<TestCommutativeRing>()
        requireCommutativeRing<TestField>()
        requireField<TestField>()
    }

    @Test
    fun arithmeticConceptsShouldCompile() {
        requireSemigroup<TestPlusSemigroup>()
        requireAbelianGroup<TestPlusGroup>()
        requireRing<TestNumberRing>()
        requireField<TestNumberField>()
    }
}
