/**
 * 适应度评估算子 / Fitness evaluation operators
*/
package fuookami.ospf.kotlin.core.solver.heuristic.operator.fitness

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.model.basic.Solution
import fuookami.ospf.kotlin.core.model.callback.AbstractCallBackModelInterface
import fuookami.ospf.kotlin.core.solver.heuristic.Individual
import fuookami.ospf.kotlin.core.solver.heuristic.SolutionWithFitness
import fuookami.ospf.kotlin.core.solver.heuristic.runner.SearchEvaluation

/**
 * 适应度评估算子接口，把「如何给解打分」从算法中独立出来。 / Fitness evaluation interface that separates "how to score a solution" from the algorithm.
*/
interface FitnessEvaluator {

    /**
     * 评估一个解的适应度 / Evaluate the fitness of one solution
     *
     * @param ObjValue 目标值类型 / Objective value type
     * @param V 值类型 / Value type
     * @param model 回调模型接口 / Callback model interface
     * @param solution 待评估解 / Solution to evaluate
     * @return 适应度，无法评估时返回 null / Fitness, or null when it cannot be evaluated
    */
    operator fun <ObjValue, V> invoke(
        model: AbstractCallBackModelInterface<*, ObjValue, V>,
        solution: Solution<V>
    ): ObjValue? where V : RealNumber<V>, V : NumberField<V>
}

/** 直接使用模型目标值 / Uses the model objective value directly */
data object ObjectiveFitnessEvaluator : FitnessEvaluator {
    override operator fun <ObjValue, V> invoke(
        model: AbstractCallBackModelInterface<*, ObjValue, V>,
        solution: Solution<V>
    ): ObjValue? where V : RealNumber<V>, V : NumberField<V> {
        return model.objective(solution)
    }
}

/** 目标值缺失时回退到模型默认目标值 / Falls back to the model default objective when the objective is missing */
data object DefaultOnMissFitnessEvaluator : FitnessEvaluator {
    override operator fun <ObjValue, V> invoke(
        model: AbstractCallBackModelInterface<*, ObjValue, V>,
        solution: Solution<V>
    ): ObjValue? where V : RealNumber<V>, V : NumberField<V> {
        return model.objective(solution) ?: model.defaultObjective
    }
}

/** 先检查约束可行性，不可行直接判为无适应度 / Checks constraint feasibility first and reports no fitness when infeasible */
data object ConstraintAwareFitnessEvaluator : FitnessEvaluator {
    override operator fun <ObjValue, V> invoke(
        model: AbstractCallBackModelInterface<*, ObjValue, V>,
        solution: Solution<V>
    ): ObjValue? where V : RealNumber<V>, V : NumberField<V> {
        if (model is SearchEvaluation) return model.objective(solution)
        return when (model.constraintSatisfied(solution)) {
            false -> null
            else -> model.objective(solution) ?: model.defaultObjective
        }
    }
}

/**
 * 评估解并包装为个体 / Evaluate a solution and wrap it as an individual
 *
 * @param ObjValue 目标值类型 / Objective value type
 * @param V 值类型 / Value type
 * @param model 回调模型接口 / Callback model interface
 * @param solution 待评估解 / Solution to evaluate
 * @return 个体，无法评估时返回 null / Individual, or null when it cannot be evaluated
*/
fun <ObjValue, V> FitnessEvaluator.toIndividual(
    model: AbstractCallBackModelInterface<*, ObjValue, V>,
    solution: Solution<V>
): Individual<ObjValue, V>? where V : RealNumber<V>, V : NumberField<V> {
    val fitness = invoke(model, solution) ?: return null
    if (model is SearchEvaluation && model.rejected) {
        return null
    }

    // Flt64 目标值非有限（NaN / ±∞）时该解不可比较：NaN 与任何值比较都被判为「持平」，
    // 会破坏排序所需的全序，因此必须在入口处丢弃，而不是让它在种群里传播。
    // A non-finite Flt64 objective (NaN / ±∞) makes the solution incomparable: NaN ties with everything, which breaks the total
    // order required by sorting, so it must be dropped at the entry point instead of propagating through the population.
    if (fitness is Flt64 && !fitness.toDouble().isFinite()) {
        return null
    }

    return SolutionWithFitness(
        solution = solution,
        fitness = fitness
    )
}
