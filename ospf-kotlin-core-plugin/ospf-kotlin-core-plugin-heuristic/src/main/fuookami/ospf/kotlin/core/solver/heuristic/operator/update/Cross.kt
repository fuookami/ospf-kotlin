/**
 * 启发式交叉操作接口 / Heuristic crossover operation interface
*/
package fuookami.ospf.kotlin.core.solver.heuristic.operator.update


/**
 * 交叉操作接口，定义从父代个体生成子代解的行为。 / Crossover operation interface, defining how offspring solutions are generated from parents.
 *
 * 交叉是 [SolutionUpdate] 的一种：二者契约完全相同，区别只在语义——交叉组合多个父代的基因，
 * 位置更新则按某种动力学把父代推向新的位置。 / Crossover is a kind of [SolutionUpdate]: the contract is identical and only the semantics differ — crossover recombines the genes of
 * several parents, whereas a position update pushes the parents towards new positions by some dynamics.
*/
interface Cross : SolutionUpdate
