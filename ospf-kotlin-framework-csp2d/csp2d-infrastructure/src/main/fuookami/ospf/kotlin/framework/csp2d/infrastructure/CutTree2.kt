/**
 * 裁切工艺与断头台切分树。 / Cutting process and guillotine cut tree.
 *
 * 二维套裁需要同时支持两种工艺：
 * 1. **任意排样**：只给放置坐标，设备（激光/水刀/CNC）按轮廓切。
 * 2. **断头台**：每一刀必须贯穿当前料块，把料切成两块，递归进行；适用于剪板机/开料锯。
 *
 * 断头台模式下，排样必须能由一个**切分树**产生，且切分树本身就是给车间的下刀顺序，因此必须显式建模，
 * 而不是只存放置坐标。
 * / 2D cutting stock must support two processes: **arbitrary nesting**, where only placement coordinates are given and
 * the machine (laser, waterjet, CNC) cuts along contours; and **guillotine**, where every cut must run through the
 * current piece and split it into two, recursively, as on a shear or panel saw. In guillotine mode a layout must be
 * produced by a **cut tree**, and that tree is itself the cut sequence handed to the workshop, so it has to be
 * modelled explicitly instead of storing placement coordinates only.
*/
package fuookami.ospf.kotlin.framework.csp2d.infrastructure

import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.framework.geometry2d.*

/**
 * 刀线方向。 / Cut line orientation.
*/
enum class CutOrientation2 {
    /** 竖刀：刀线平行于 y 轴，把料分成左右两块 / vertical cut: parallel to the y axis, splitting left and right */
    Vertical,

    /** 横刀：刀线平行于 x 轴，把料分成上下两块 / horizontal cut: parallel to the x axis, splitting bottom and top */
    Horizontal
}

/**
 * 一条贯穿刀线。 / A single through cut.
 *
 * @property V 数值类型 / numeric type
 * @property orientation 刀线方向 / cut line orientation
 * @property position 刀缝起始坐标（竖刀为 x，横刀为 y）/ the coordinate where the kerf strip starts
 * @property stage 切割层号：1 为切条、2 为切块、3 为切件 / cut level: 1 strips, 2 blocks, 3 pieces
*/
data class CutLine2<V : FloatingNumber<V>>(
    val orientation: CutOrientation2,
    val position: Quantity<V>,
    val stage: Int = 1
)

/**
 * 断头台切分树。 / Guillotine cut tree.
 *
 * 叶子是最终的一块料（成品件或余料），内部结点是一刀与它切出的两个子树。树的构造必须与 `Region2.cut` 一致，
 * 这样叶子区域才是真正可由贯穿刀得到的。
 * / Leaves are the final pieces of material (finished parts or remnants) and internal nodes are one cut together with
 * the two subtrees it produces. The tree must be consistent with `Region2.cut`, so that the leaf regions are really
 * obtainable by through cuts.
 *
 * @property V 数值类型 / numeric type
*/
sealed interface CutTree2<V : FloatingNumber<V>> {

    /** 本结点对应的区域 / the region of this node */
    val region: Region2<V>

    /** 本结点的工序级数 / the process stage of this node */
    val stage: Int

    /**
     * 收集全部叶子。 / Collect every leaf.
     *
     * @return 叶子列表 / leaf list
    */
    fun leaves(): List<Leaf2<V>>

    /**
     * 收集全部刀线，按深度优先顺序，即下刀顺序。 / Collect every cut line in depth-first order, i.e. the cut sequence.
     *
     * @return 刀线列表 / cut line list
    */
    fun cutLines(): List<CutLine2<V>>

    /**
     * 本子树的最大工序级数。 / The maximum process stage inside this subtree.
     *
     * @return 最大级数 / maximum stage
    */
    fun maxStage(): Int

    /**
     * 校验结构与本工艺的一致性。 / Validate the structure against the given process.
     *
     * 检查项：区域非退化；级数落在 [1, maxStage]；子结点区域与父区域按刀缝切分的结果一致；子刀线层号恰为父层号 + 1。
     * / Checks: regions are non-degenerate; stages lie in `[1, maxStage]`; child regions match the result of splitting
     * the parent region by the kerf; the stage never decreases.
     *
     * @param process 裁切工艺 / cutting process
     * @param kerf 刀缝宽度 / kerf width
     * @return 校验结果 / validation result
    */
    fun validate(process: CuttingProcess2, kerf: Quantity<V>): Try

    /**
     * 叶子：一块最终的料，可对应一个成品件，也可为余料。 / A leaf: one final piece of material, either a finished part
     * or a remnant.
     *
     * @property V 数值类型 / numeric type
     * @property region 区域 / region
     * @property stage 切割层号 / cut level
     * @property pieceId 成品件标识，为空表示为余料 / finished part identifier, null meaning a remnant
     * @property rotated 该件是否旋转 90° / whether the piece is rotated by 90 degrees
    */
    data class Leaf2<V : FloatingNumber<V>>(
        override val region: Region2<V>,
        override val stage: Int = 1,
        val pieceId: String? = null,
        val rotated: Boolean = false
    ) : CutTree2<V> {
        override fun leaves(): List<Leaf2<V>> = listOf(this)

        override fun cutLines(): List<CutLine2<V>> = emptyList()

        override fun maxStage(): Int = stage

        override fun validate(process: CuttingProcess2, kerf: Quantity<V>): Try {
            if (region.isDegenerate()) {
                return Failed(
                    Csp2dValidationError(
                        detail = "叶子区域退化：$region / Degenerate leaf region"
                    )
                )
            }
            if (stage < 1) {
                return Failed(
                    Csp2dValidationError(
                        detail = "叶子工序级数非法：$stage / Invalid leaf stage"
                    )
                )
            }
            if (!process.allowsStage(stage)) {
                return Failed(
                    Csp2dValidationError(
                        detail = "叶子工序级数 $stage 超出工艺 ${process.name} 的上限 ${process.maxStage} / " +
                                "Leaf stage $stage exceeds the process limit"
                    )
                )
            }
            return ok
        }
    }

    /**
     * 内部结点：一刀与它切出的两个子树。 / An internal node: one cut together with the two subtrees it produces.
     *
     * @property V 数值类型 / numeric type
     * @property region 被切的区域 / the region being cut
     * @property line 刀线 / the cut line
     * @property first 左/下侧子树 / the left or bottom subtree
     * @property second 右/上侧子树 / the right or top subtree
    */
    data class Split2<V : FloatingNumber<V>>(
        override val region: Region2<V>,
        val line: CutLine2<V>,
        val first: CutTree2<V>,
        val second: CutTree2<V>
    ) : CutTree2<V> {
        override val stage: Int get() = line.stage

        override fun leaves(): List<Leaf2<V>> = first.leaves() + second.leaves()

        override fun cutLines(): List<CutLine2<V>> = listOf(line) + first.cutLines() + second.cutLines()

        override fun maxStage(): Int = maxOf(line.stage, first.maxStage(), second.maxStage())

        override fun validate(process: CuttingProcess2, kerf: Quantity<V>): Try {
            if (region.isDegenerate()) {
                return Failed(
                    Csp2dValidationError(
                        detail = "结点区域退化：$region / Degenerate split region"
                    )
                )
            }
            if (!process.allowsStage(line.stage)) {
                return Failed(
                    Csp2dValidationError(
                        detail = "刀线级数 ${line.stage} 超出工艺 ${process.name} 的上限 ${process.maxStage} / " +
                                "Cut stage ${line.stage} exceeds the process limit"
                    )
                )
            }
            // k 级断头台的定义：级数是「切割层号」，根刀线为第 1 级，其下的刀线必须恰为第 2 级，依此类推。
            // 这样 2 级工艺恰好等于「先切条、再切件」，3 级等于「切条 → 切块 → 切件」，与剪板机工艺一致。
            // Definition of k-stage guillotine: the stage is the cut level, the root cut is level 1, cuts below it must be
            // exactly level 2, and so on. A two-stage process then means exactly "strips then pieces", and a three-stage
            // process means "strips, blocks, pieces", matching shear workflows.
            // 级数与刀线方向联动：同向刀属于同一级（同一批平行刀），换向才进入下一级。
            // 因此期望级数为「同向 → 同级；换向 → 父级 + 1」，而不是一律 +1：否则同一级的平行刀（例如把整板切成
            // 多条同向条带）无法表达。
            // The stage is coupled to the cut orientation: cuts in the same direction belong to the same stage (one
            // family of parallel cuts), and only a direction change advances the stage. The expected stage is therefore
            // "same direction keeps the stage, a direction change adds one" rather than always adding one; otherwise
            // same-stage parallel cuts (for example cutting a sheet into several strips) could not be expressed.
            for ((name, child) in listOf("左/下侧 / first" to first, "右/上侧 / second" to second)) {
                if (child !is Split2) {
                    continue
                }
                val expected = if (child.line.orientation == line.orientation) line.stage else line.stage + 1
                if (child.line.stage != expected) {
                    return Failed(
                        Csp2dValidationError(
                            detail = "$name 子刀线级数应为 $expected，实际 ${child.line.stage} / " +
                                    "A child cut must use the expected stage for its orientation"
                        )
                    )
                }
            }
            if (quantityLessOrEqual(line.position, startOf(region, line.orientation))) {
                return Failed(
                    Csp2dValidationError(
                        detail = "刀线不在区域内部：$line / The cut line lies outside the region"
                    )
                )
            }

            val expected = when (val result = region.cut(line.orientation, line.position, kerf)) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            if (expected.first != first.region) {
                return Failed(
                    Csp2dValidationError(
                        detail = "左/下侧子区域与切分结果不一致：期望 ${expected.first}，实际 ${first.region} / " +
                                "First child region does not match the split result"
                    )
                )
            }
            if (expected.second != second.region) {
                return Failed(
                    Csp2dValidationError(
                        detail = "右/上侧子区域与切分结果不一致：期望 ${expected.second}，实际 ${second.region} / " +
                                "Second child region does not match the split result"
                    )
                )
            }

            for (child in listOf(first, second)) {
                when (val result = child.validate(process, kerf)) {
                    is Ok -> {}
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
            }
            return ok
        }
    }

    companion object {

        /**
         * 由刀线构造内部结点，子区域按刀缝自动推导。 / Build an internal node from a cut line, deriving the child
         * regions from the kerf automatically.
         *
         * 这是推荐的构造方式：它保证树与 `Region2.cut` 一致，不会出现子区域与刀线矛盾的手工错误。
         * / This is the recommended construction: it guarantees consistency with `Region2.cut` and avoids hand-built
         * mismatches between the cut line and the child regions.
         *
         * @param V 数值类型 / numeric type
         * @param region 被切的区域 / the region being cut
         * @param orientation 刀线方向 / cut line orientation
         * @param position 刀缝起始坐标 / the coordinate where the kerf strip starts
         * @param stage 工序级数 / process stage
         * @param first 左/下侧子树 / the left or bottom subtree
         * @param second 右/上侧子树 / the right or top subtree
         * @param kerf 刀缝宽度 / kerf width
         * @return 内部结点或失败 / the internal node, or failure
        */
        fun <V : FloatingNumber<V>> split2Of(
            region: Region2<V>,
            orientation: CutOrientation2,
            position: Quantity<V>,
            stage: Int,
            first: CutTree2<V>,
            second: CutTree2<V>,
            kerf: Quantity<V> = quantityZeroOf(region.width)
        ): Ret<Split2<V>> {
            return when (val result = region.cut(orientation, position, kerf)) {
                is Ok -> {
                    val (expectedFirst, expectedSecond) = result.value
                    if (expectedFirst != first.region) {
                        return Failed(
                            Csp2dValidationError(
                                detail = "左/下侧子区域与刀线矛盾：期望 $expectedFirst，实际 ${first.region} / " +
                                        "First child region contradicts the cut line"
                            )
                        )
                    }
                    if (expectedSecond != second.region) {
                        return Failed(
                            Csp2dValidationError(
                                detail = "右/上侧子区域与刀线矛盾：期望 $expectedSecond，实际 ${second.region} / " +
                                        "Second child region contradicts the cut line"
                            )
                        )
                    }
                    ok(
                        Split2(
                            region = region,
                            line = CutLine2(orientation = orientation, position = position, stage = stage),
                            first = first,
                            second = second
                        )
                    )
                }

                is Failed -> Failed(result.error)
                is Fatal -> Fatal(result.errors)
            }
        }

        /**
         * 读取区域在指定方向上的起始坐标。 / Read the starting coordinate of a region along the given orientation.
         *
         * @param V 数值类型 / numeric type
         * @param region 区域 / region
         * @param orientation 刀线方向 / cut line orientation
         * @return 起始坐标 / starting coordinate
        */
        private fun <V : FloatingNumber<V>> startOf(region: Region2<V>, orientation: CutOrientation2): Quantity<V> {
            return when (orientation) {
                CutOrientation2.Vertical -> region.x
                CutOrientation2.Horizontal -> region.y
            }
        }
    }
}

/**
 * 判断坐标是否严格落在区域内部。 / Check whether a coordinate lies strictly inside a region.
 *
 * @param V 数值类型 / numeric type
 * @param region 区域 / region
 * @param orientation 刀线方向 / cut line orientation
 * @param position 坐标 / coordinate
 * @return 是否在内部 / whether strictly inside
*/
fun <V : FloatingNumber<V>> isInside(
    region: Region2<V>,
    orientation: CutOrientation2,
    position: Quantity<V>
): Boolean {
    return when (orientation) {
        CutOrientation2.Vertical -> quantityLess(region.x, position) && quantityLess(position, region.maxX())
        CutOrientation2.Horizontal -> quantityLess(region.y, position) && quantityLess(position, region.maxY())
    }
}

/**
 * 判断区域是否非退化且面积为正。 / Check whether a region is non-degenerate with positive area.
 *
 * @param V 数值类型 / numeric type
 * @param region 区域 / region
 * @param sample 面积样本，用于提供零值单位 / area sample providing the zero unit
 * @return 是否有效 / whether valid
*/
fun <V : FloatingNumber<V>> hasPositiveArea(region: Region2<V>, sample: Quantity<V>): Boolean {
    return !region.isDegenerate() && quantityGreaterOrEqual(region.area, quantityZeroOf(sample))
}