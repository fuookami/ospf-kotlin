package fuookami.ospf.kotlin.example.framework_demo.demo1.bandwidth_context.service

import fuookami.ospf.kotlin.framework.statistical_model.interfaces.AbstractStatisticalModel
import fuookami.ospf.kotlin.framework.statistical_model.infrastructure.StatisticalSolution
import fuookami.ospf.kotlin.framework.statistical_model.infrastructure.StatisticalSolutionAnalyzer

// 定义数据结构
// Define data structures

// 统计解的分析器
// Statistical solution analyzer
class SolutionAnalyzer(
    private val model: AbstractStatisticalModel<BandwidthContext, BandwidthVariable, BandwidthConstraints, BandwidthCost>
) : StatisticalSolutionAnalyzer<BandwidthContext, BandwidthVariable, BandwidthConstraints, BandwidthCost> {
    override fun analyze(
        solutions: List<StatisticalSolution<BandwidthContext, BandwidthVariable, BandwidthConstraints, BandwidthCost>>
    ): List<StatisticalSolution<BandwidthContext, BandwidthVariable, BandwidthConstraints, BandwidthCost>> {
        return solutions.map { solution ->
            val context = solution.context
            /** 所有线路的最大总容量 / Maximum total capacity across all lines */
            val maxCapacity = context.dimensions.sumOf { dimension ->
                dimension.lines.sumOf { line ->
                    line.maxCapacity.toLong()
                }
            }
            /** 所有线路的当前总容量 / Current total capacity across all lines */
            val currentCapacity = context.dimensions.sumOf { dimension ->
                dimension.lines.sumOf { line ->
                    line.currentCapacity.toLong()
                }
            }
            /** 所有线路的目标总容量 / Target total capacity across all lines */
            val targetCapacity = context.dimensions.sumOf { dimension ->
                dimension.lines.sumOf { line ->
                    line.targetCapacity.toLong()
                }
            }
            solution
        }
    }
}
