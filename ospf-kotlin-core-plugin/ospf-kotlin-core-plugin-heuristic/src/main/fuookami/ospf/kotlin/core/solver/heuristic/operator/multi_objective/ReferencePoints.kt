/**
 * 参考点生成 / Reference point generation
*/
package fuookami.ospf.kotlin.core.solver.heuristic.operator.multi_objective

/**
 * NSGA-III 的参考点（Das–Dennis 构造）。 / Reference points of NSGA-III (Das–Dennis construction).
 *
 * 在 `m` 维单位单纯形上均匀取点：每个目标的坐标非负且和为 1。这些点定义了「理想前沿应当朝哪些方向均匀铺开」，
 * 因此选择时按「哪个参考方向的成员最少」来补位，前沿的**分布性**由参考点直接控制，
 * 而不是像 NSGA-II 那样靠拥挤距离隐式维持。 / Points are spread uniformly over the `m`-dimensional unit simplex: coordinates are non-negative and sum to one. These
 * points define "which directions the ideal front should cover", so selection fills the reference direction with the fewest
 * members; the **spread** of the front is controlled directly by the reference points instead of being maintained implicitly
 * through crowding distance as in NSGA-II.
*/
data object ReferencePoints {

    /**
     * 生成 Das–Dennis 参考点 / Generate Das–Dennis reference points
     *
     * @param objectiveAmount 目标数量 / Objective amount
     * @param divisions 每个目标上的分割数；越大越密 / Divisions per objective; larger means denser
     * @return 参考点列表；参数非法时返回空列表（不抛异常）/ Reference points, or an empty list for invalid arguments (never throws)
    */
    fun dasDennis(objectiveAmount: Int, divisions: Int): List<List<Double>> {
        if (objectiveAmount < 1 || divisions < 1) {
            return emptyList()
        }

        val points = ArrayList<List<Double>>()
        val current = ArrayList<Double>(objectiveAmount)

        fun generate(remaining: Int, objectivesLeft: Int) {
            if (objectivesLeft == 1) {
                current.add(remaining.toDouble() / divisions.toDouble())
                points.add(current.toList())
                current.removeAt(current.size - 1)
                return
            }
            for (share in 0..remaining) {
                current.add(share.toDouble() / divisions.toDouble())
                generate(remaining - share, objectivesLeft - 1)
                current.removeAt(current.size - 1)
            }
        }

        generate(divisions, objectiveAmount)
        return points
    }

    /**
     * 计算个体到参考方向的垂直距离 / Perpendicular distance from a point to a reference direction
     *
     * 参考方向取自原点，因此垂直距离等于「个体向量在参考方向上的正交分量长度」。 /
     * The reference direction starts at the origin, so the perpendicular distance equals the length of the component of the
     * individual vector orthogonal to that direction.
     *
     * @param objectives 归一化后的目标向量 / Normalised objective vector
     * @param reference 参考方向 / Reference direction
     * @return 垂直距离；退化情形返回正无穷 / Perpendicular distance, or positive infinity in degenerate cases
    */
    fun perpendicularDistance(objectives: List<Double>, reference: List<Double>): Double {
        var projection = 0.0
        var referenceNorm = 0.0
        for (index in reference.indices) {
            projection += objectives[index] * reference[index]
            referenceNorm += reference[index] * reference[index]
        }
        if (referenceNorm <= 1.0e-12) {
            return Double.POSITIVE_INFINITY
        }

        var squared = 0.0
        for (index in reference.indices) {
            val component = objectives[index] - projection * reference[index] / referenceNorm
            squared += component * component
        }
        return kotlin.math.sqrt(squared)
    }
}
