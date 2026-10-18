/**
 * 二维套裁裁切工艺。 / 2D cutting stock process.
 *
 * 工艺与「模式」正交：模式决定料怎么来（卷材条带排样 / 定尺板材），工艺决定怎么下刀。
 * / The process is orthogonal to the mode: the mode decides where the material comes from (coil strip packing or
 * fixed-size sheets) while the process decides how the cuts are made.
*/
package fuookami.ospf.kotlin.framework.csp2d.infrastructure

/**
 * 裁切工艺。 / Cutting process.
 *
 * @property maxStage 允许的最大工序级数；任意排样不受级数约束 / the maximum allowed process stage; arbitrary nesting
 * has no stage limit
 * @property requiresCutTree 是否要求输出断头台切分树 / whether a guillotine cut tree is required
*/
enum class CuttingProcess2(
    val maxStage: Int,
    val requiresCutTree: Boolean
) {
    /**
     * 任意排样：设备按轮廓切（激光/水刀/CNC），只需要放置坐标。
     * / Arbitrary nesting: the machine cuts along contours (laser, waterjet, CNC) and only placement coordinates are
     * needed.
    */
    Freeform(
        maxStage = Int.MAX_VALUE,
        requiresCutTree = false
    ),

    /**
     * 断头台 2 级：先把料切成条，再把条切成件。 / Guillotine with two stages: first cut the material into strips, then
     * cut the strips into pieces.
    */
    GuillotineTwoStage(
        maxStage = 2,
        requiresCutTree = true
    ),

    /**
     * 断头台 3 级：切条 → 切块 → 切件。 / Guillotine with three stages: strips, then blocks, then pieces.
    */
    GuillotineThreeStage(
        maxStage = 3,
        requiresCutTree = true
    );

    /**
     * 判断工序级数是否被本工艺允许。 / Check whether a process stage is allowed by this process.
     *
     * @param stage 工序级数 / process stage
     * @return 是否允许 / whether allowed
    */
    fun allowsStage(stage: Int): Boolean {
        return stage >= 1 && stage <= maxStage
    }

    /**
     * 工序级数的中文/英文标签。 / Bilingual label of a process stage.
     *
     * @param stage 工序级数 / process stage
     * @return 标签 / label
    */
    fun stageLabel(stage: Int): String {
        return when (stage) {
            1 -> "切条 / strip"
            2 -> "切块 / block"
            3 -> "切件 / piece"
            else -> "第 $stage 级 / stage $stage"
        }
    }
}
