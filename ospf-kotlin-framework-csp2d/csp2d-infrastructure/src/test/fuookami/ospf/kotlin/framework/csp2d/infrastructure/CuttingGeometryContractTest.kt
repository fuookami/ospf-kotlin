/**
 * 二维套裁几何与断头台切分契约测试。 / 2D cutting geometry and guillotine cut tree contract test.
*/
package fuookami.ospf.kotlin.framework.csp2d.infrastructure

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.quantities.unit.Meter
import fuookami.ospf.kotlin.framework.geometry2d.*

class Region2ContractTest {

    /**
     * 构造米制量值。 / Build a metric quantity.
     *
     * @param value 数值 / value
     * @return 量值 / quantity
    */
    private fun meter(value: Double): Quantity<Flt64> = Flt64(value) * Meter

    @Test
    fun areaAndBoundsAreConsistent() {
        val region = region2Of(x = meter(1.0), y = meter(2.0), width = meter(10.0), height = meter(4.0))
        assertEquals(40.0, region.area.value.toDouble(), 1e-9)
        assertEquals(11.0, region.maxX().value.toDouble(), 1e-9)
        assertEquals(6.0, region.maxY().value.toDouble(), 1e-9)
        assertEquals(10.0, region.toBox().width.value.toDouble(), 1e-9)
        assertEquals(4.0, region.toBox().height.value.toDouble(), 1e-9)
    }

    @Test
    fun containsRequiresFullCoverage() {
        val outer = region2Of(x = meter(0.0), y = meter(0.0), width = meter(10.0), height = meter(10.0))
        val inner = region2Of(x = meter(1.0), y = meter(1.0), width = meter(5.0), height = meter(5.0))
        val crossing = region2Of(x = meter(8.0), y = meter(8.0), width = meter(4.0), height = meter(4.0))
        assertTrue(outer.contains(inner))
        assertTrue(!outer.contains(crossing))
        assertTrue(outer.contains(outer))
    }

    @Test
    fun verticalCutWithoutKerfSplitsExactly() {
        val region = region2Of(x = meter(0.0), y = meter(0.0), width = meter(100.0), height = meter(50.0))
        val (first, second) = region.cut(CutOrientation2.Vertical, meter(40.0), quantityZeroOf(region.width))
            .valueOrFail()
        assertEquals(40.0, first.width.value.toDouble(), 1e-9)
        assertEquals(0.0, first.x.value.toDouble(), 1e-9)
        assertEquals(60.0, second.width.value.toDouble(), 1e-9)
        assertEquals(40.0, second.x.value.toDouble(), 1e-9)
        assertEquals(50.0, first.height.value.toDouble(), 1e-9)
        assertEquals(50.0, second.height.value.toDouble(), 1e-9)
    }

    @Test
    fun verticalCutRemovesKerfStrip() {
        // 刀缝 2：左侧 [0,40)，刀缝 [40,42)，右侧 [42,100)。 / Kerf 2: left [0,40), kerf [40,42), right [42,100).
        val region = region2Of(x = meter(0.0), y = meter(0.0), width = meter(100.0), height = meter(50.0))
        val (first, second) = region.cut(CutOrientation2.Vertical, meter(40.0), meter(2.0)).valueOrFail()
        assertEquals(40.0, first.width.value.toDouble(), 1e-9)
        assertEquals(42.0, second.x.value.toDouble(), 1e-9)
        assertEquals(58.0, second.width.value.toDouble(), 1e-9)
    }

    @Test
    fun horizontalCutSplitsAlongY() {
        val region = region2Of(x = meter(0.0), y = meter(0.0), width = meter(100.0), height = meter(50.0))
        val (first, second) = region.cut(CutOrientation2.Horizontal, meter(20.0), meter(1.0)).valueOrFail()
        assertEquals(20.0, first.height.value.toDouble(), 1e-9)
        assertEquals(21.0, second.y.value.toDouble(), 1e-9)
        assertEquals(29.0, second.height.value.toDouble(), 1e-9)
        assertEquals(100.0, first.width.value.toDouble(), 1e-9)
    }

    @Test
    fun cutOutsideOrOnBoundaryFails() {
        val region = region2Of(x = meter(0.0), y = meter(0.0), width = meter(100.0), height = meter(50.0))
        assertTrue(
            region.cut(CutOrientation2.Vertical, meter(0.0), quantityZeroOf(region.width)) is Failed,
            "刀线落在左边界应失败 / a cut on the left edge must fail"
        )
        assertTrue(
            region.cut(CutOrientation2.Vertical, meter(100.0), quantityZeroOf(region.width)) is Failed,
            "刀线落在右边界应失败 / a cut on the right edge must fail"
        )
        assertTrue(
            region.cut(CutOrientation2.Vertical, meter(99.5), meter(2.0)) is Failed,
            "刀缝吃掉整个右侧应失败 / a kerf consuming the whole right side must fail"
        )
    }

    @Test
    fun degenerateRegionIsDetected() {
        assertTrue(region2Of(x = meter(0.0), y = meter(0.0), width = meter(0.0), height = meter(5.0)).isDegenerate())
        assertTrue(region2Of(x = meter(0.0), y = meter(0.0), width = meter(5.0), height = meter(0.0)).isDegenerate())
        assertTrue(!region2Of(x = meter(0.0), y = meter(0.0), width = meter(1.0), height = meter(1.0)).isDegenerate())
    }

    @Test
    fun isInsideIsStrict() {
        val region = region2Of(x = meter(0.0), y = meter(0.0), width = meter(10.0), height = meter(10.0))
        assertTrue(isInside(region, CutOrientation2.Vertical, meter(5.0)))
        assertTrue(!isInside(region, CutOrientation2.Vertical, meter(0.0)))
        assertTrue(!isInside(region, CutOrientation2.Vertical, meter(10.0)))
        assertTrue(isInside(region, CutOrientation2.Horizontal, meter(5.0)))
    }
}

class GuillotineCutTreeContractTest {

    /**
     * 构造米制量值。 / Build a metric quantity.
     *
     * @param value 数值 / value
     * @return 量值 / quantity
    */
    private fun meter(value: Double): Quantity<Flt64> = Flt64(value) * Meter

    /**
     * 构造 100×60 的板材区域。 / Build a 100 by 60 sheet region.
     *
     * @return 区域 / region
    */
    private fun sheet(): Region2<Flt64> = region2Of(
        x = meter(0.0),
        y = meter(0.0),
        width = meter(100.0),
        height = meter(60.0)
    )

    /**
     * 构造 2 级断头台树：竖切成 40×60 与 60×60，再把右侧横切成两块 60×30。
     * / Build a two-stage guillotine tree: cut vertically into 40×60 and 60×60, then cut the right part horizontally
     * into two 60×30 pieces.
     *
     * @param kerf 刀缝 / kerf
     * @return 切分树 / cut tree
    */
    private fun twoStageTree(kerf: Quantity<Flt64>): CutTree2.Split2<Flt64> {
        val root = sheet()
        val rootCutPosition = meter(40.0)
        val leftRegion = region2Of(
            x = meter(0.0),
            y = meter(0.0),
            width = meter(40.0),
            height = meter(60.0)
        )
        val rightRegion = region2Of(
            x = rootCutPosition + kerf,
            y = meter(0.0),
            width = meter(100.0) - rootCutPosition - kerf,
            height = meter(60.0)
        )
        val rightCutPosition = meter(30.0)
        val bottomRegion = region2Of(
            x = rightRegion.x,
            y = meter(0.0),
            width = rightRegion.width,
            height = meter(30.0)
        )
        val topRegion = region2Of(
            x = rightRegion.x,
            y = rightCutPosition + kerf,
            width = rightRegion.width,
            height = meter(60.0) - rightCutPosition - kerf
        )
        val rightSubtree = CutTree2.split2Of(
            region = rightRegion,
            orientation = CutOrientation2.Horizontal,
            position = rightCutPosition,
            stage = 2,
            first = CutTree2.Leaf2(region = bottomRegion, stage = 2, pieceId = "B"),
            second = CutTree2.Leaf2(region = topRegion, stage = 2, pieceId = "C"),
            kerf = kerf
        ).valueOrFail()
        return CutTree2.split2Of(
            region = root,
            orientation = CutOrientation2.Vertical,
            position = rootCutPosition,
            stage = 1,
            first = CutTree2.Leaf2(region = leftRegion, stage = 1, pieceId = "A"),
            second = rightSubtree,
            kerf = kerf
        ).valueOrFail()
    }

    @Test
    fun twoStageTreeWithoutKerfIsValid() {
        val tree = twoStageTree(quantityZeroOf(meter(1.0)))
        val kerf = quantityZeroOf(meter(1.0))
        assertTrue(tree.validate(CuttingProcess2.GuillotineTwoStage, kerf) is Ok)
        assertEquals(3, tree.leaves().size)
        assertEquals(2, tree.cutLines().size)
        assertEquals(2, tree.maxStage())
        assertEquals(listOf("A", "B", "C"), tree.leaves().map { it.pieceId })
        // 下刀顺序：先竖切，再横切。 / Cut order: the vertical cut first, then the horizontal one.
        assertEquals(
            listOf(CutOrientation2.Vertical, CutOrientation2.Horizontal),
            tree.cutLines().map { it.orientation }
        )
    }

    @Test
    fun twoStageTreeWithKerfIsValid() {
        val kerf = meter(2.0)
        val tree = twoStageTree(kerf)
        assertTrue(tree.validate(CuttingProcess2.GuillotineTwoStage, kerf) is Ok)
        val leaves = tree.leaves()
        // 右侧两块宽度 = 100 − 40 − 2 = 58。 / Both right pieces are 100 minus 40 minus 2 wide.
        assertEquals(58.0, leaves[1].region.width.value.toDouble(), 1e-9)
        // 上方那块高度 = 60 − 30 − 2 = 28。 / The top piece is 60 minus 30 minus 2 high.
        assertEquals(28.0, leaves[2].region.height.value.toDouble(), 1e-9)
    }

    @Test
    fun stageBeyondProcessLimitIsRejected() {
        val kerf = quantityZeroOf(meter(1.0))
        val tree = twoStageTree(kerf)
        assertTrue(
            tree.validate(CuttingProcess2.GuillotineTwoStage, kerf) is Ok,
            "2 级工艺应接受该树 / a two-stage process must accept this tree"
        )
        // 把根刀线标为第 3 级即超出 2 级工艺。 / Marking the root cut as stage 3 exceeds a two-stage process.
        val overStaged = CutTree2.Split2(
            region = tree.region,
            line = tree.line.copy(stage = 3),
            first = tree.first,
            second = tree.second
        )
        assertTrue(overStaged.validate(CuttingProcess2.GuillotineTwoStage, kerf) is Failed)
    }

    @Test
    fun childCutMustBeExactlyOneLevelBelowParent() {
        val kerf = quantityZeroOf(meter(1.0))
        val base = twoStageTree(kerf)
        val right = base.second as CutTree2.Split2
        // 父刀线为第 1 级时，其下的刀线必须恰为第 2 级；这里把子刀线也标为第 1 级。
        // When the parent cut is level 1, a cut below it must be exactly level 2; here the child cut is also level 1.
        val wrong = CutTree2.Split2(
            region = right.region,
            line = right.line.copy(stage = 1),
            first = right.first,
            second = right.second
        )
        val tree = CutTree2.Split2(
            region = base.region,
            line = base.line,
            first = base.first,
            second = wrong
        )
        assertTrue(tree.validate(CuttingProcess2.GuillotineThreeStage, kerf) is Failed)
    }

    @Test
    fun inconsistentChildRegionIsRejected() {
        val kerf = quantityZeroOf(meter(1.0))
        val base = twoStageTree(kerf)
        // 手工构造一个与刀线矛盾的子区域。 / Hand-build a child region that contradicts the cut line.
        val wrongRegion = region2Of(
            x = meter(0.0),
            y = meter(0.0),
            width = meter(30.0),
            height = meter(60.0)
        )
        assertTrue(
            CutTree2.split2Of(
                region = sheet(),
                orientation = CutOrientation2.Vertical,
                position = meter(40.0),
                stage = 1,
                first = CutTree2.Leaf2(region = wrongRegion, stage = 1, pieceId = "A"),
                second = base.second,
                kerf = kerf
            ) is Failed,
            "子区域与刀线矛盾必须被拒绝 / a child region contradicting the cut line must be rejected"
        )
    }

    @Test
    fun freeformProcessAcceptsTreeAndDoesNotRequireCutTree() {
        val kerf = quantityZeroOf(meter(1.0))
        val tree = twoStageTree(kerf)
        assertTrue(!CuttingProcess2.Freeform.requiresCutTree)
        assertTrue(CuttingProcess2.Freeform.allowsStage(1))
        assertTrue(CuttingProcess2.Freeform.allowsStage(9))
        assertTrue(tree.validate(CuttingProcess2.Freeform, kerf) is Ok)
    }

    @Test
    fun processStageSemantics() {
        assertTrue(CuttingProcess2.GuillotineTwoStage.requiresCutTree)
        assertTrue(CuttingProcess2.GuillotineThreeStage.allowsStage(3))
        assertTrue(!CuttingProcess2.GuillotineThreeStage.allowsStage(4))
        assertTrue(!CuttingProcess2.GuillotineTwoStage.allowsStage(3))
        assertTrue(!CuttingProcess2.GuillotineTwoStage.allowsStage(0))
        assertEquals("切条 / strip", CuttingProcess2.GuillotineThreeStage.stageLabel(1))
        assertEquals("切件 / piece", CuttingProcess2.GuillotineThreeStage.stageLabel(3))
    }

    @Test
    fun degenerateLeafIsRejected() {
        val kerf = quantityZeroOf(meter(1.0))
        val tree = CutTree2.Leaf2(
            region = region2Of(x = meter(0.0), y = meter(0.0), width = meter(0.0), height = meter(1.0)),
            stage = 1
        )
        assertTrue(tree.validate(CuttingProcess2.GuillotineTwoStage, kerf) is Failed)
    }

    @Test
    fun geometryWorksForFltXToo() {
        // 泛型门禁：同一套几何必须同时支持 Flt64 与 FltX。 / Genericity: the same geometry must support both Flt64 and FltX.
        val region = region2Of<FltX>(
            x = FltX(0.0) * Meter,
            y = FltX(0.0) * Meter,
            width = FltX(10.0) * Meter,
            height = FltX(10.0) * Meter
        )
        val (first, second) = region.cut(
            CutOrientation2.Vertical,
            FltX(4.0) * Meter,
            quantityZeroOf(region.width)
        ).valueOrFail()
        assertEquals(4.0, first.width.value.toDouble(), 1e-9)
        assertEquals(6.0, second.width.value.toDouble(), 1e-9)
    }
}

/**
 * 解包 Ret 结果。 / Unwrap a Ret result.
 *
 * @param T 结果类型 / result type
 * @param message 失败信息 / failure message
 * @return 成功值 / the successful value
*/
private fun <T> Ret<T>.valueOrFail(message: String = "result should succeed"): T {
    return when (this) {
        is Ok -> value
        is Failed -> kotlin.test.fail("$message: ${error.message}")
        is Fatal -> kotlin.test.fail("$message: ${errors.joinToString { it.message }}")
    }
}