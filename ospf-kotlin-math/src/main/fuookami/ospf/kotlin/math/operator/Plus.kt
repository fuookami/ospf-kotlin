/**
 * 加法运算笌
 * Addition Operator
 *
 * 定义加法运算相关接口，包括加法运算和自增运算。
 * 支持运算符重载，允许使用 + 和++ 运算符。
 *
 * Defines interfaces related to addition operations, including addition and increment.
 * Supports operator overloading, allowing the use of + and ++ operators.
 *
 * 数学定义 / Mathematical definition:
 * a + b (加法 / addition)
 * ++a = a + 1 (自增 / increment)
 *
 * 接口说明 / Interface descriptions:
 * - Plus: 加法运算符接口，支持 a + b
 * - PlusTrait: 加法运算符特征接口，用于类型约束
 * - PlusAssign: 加法赋值接口，支持 a += b
 * - Inc: 自增运算符接口，支持 ++a 戌a++
*/
package fuookami.ospf.kotlin.math.operator

/**
 * 加法运算符接双
 * Addition Operator Interface
 *
 * 定义加法运算，支挌+ 运算符。
 * 加法是将两个数合并得到和的基本运算。
 *
 * Defines the addition operation, supporting the + operator.
 * Addition is a basic operation that combines two numbers to get a sum.
 *
 * @param Rhs 右操作数（加数）的类垌
 * @param Ret 返回值（和）的类垌
 *
*/
interface Plus<in Rhs, out Ret> {

    /**
     * 加法运算符，计算和
     * Addition operator, calculates the sum
     *
     * @param rhs 加数
     * @return 和
     *
     * @return Sum
    */
    operator fun plus(rhs: Rhs): Ret
}

/**
 * 加法运算符特征接双
 * Addition Operator Trait Interface
 *
 * 定义加法运算的扩展函数接口，用于在特定类型上添加加法运算功能。
 * 这种设计允许在不修改类本身的情况下添加运算符功能。
 *
 * Defines the extension function interface for addition operations, used to add addition functionality to specific types.
 * This design allows adding operator functionality without modifying the class itself.
 *
 * @param Self 接收者类型（被加数）
 * @param Rhs 右操作数（加数）的类垌
 * @param Ret 返回值（和）的类垌
 *
*/
interface PlusTrait<Self, in Rhs, out Ret> {

    /**
     * 加法运算符扩展函数，计算和
     * Addition operator extension function, calculates the sum
     *
     * @param rhs 加数
     * @return 和
     *
     * @return Sum
    */
    operator fun Self.plus(rhs: Rhs): Ret
}

/**
 * 加法赋值接双
 * Addition Assignment Interface
 *
 * 定义加法赋值运算，支持 += 运算符。
 * 将左操作数加上右操作数，并将结果赋值给左操作数。
 *
 * Defines the addition assignment operation, supporting the += operator.
 * Adds the right operand to the left operand and assigns the result to the left operand.
 *
 * @param Rhs 右操作数（加数）的类垌
 *
*/
interface PlusAssign<in Rhs> {

    /**
     * 加法赋值运算符，将和赋值给左操作数
     * Addition assignment operator, assigns the sum to the left operand
     *
     * @param rhs 加数
     *
    */
    operator fun plusAssign(rhs: Rhs)
}

/**
 * 自增运算符接双
 * Increment Operator Interface
 *
 * 定义自增运算，支挌++ 运算符。
 * 自增将当前值加 1，返回自增后的新实例。
 *
 * Defines the increment operation, supporting the ++ operator.
 * Increment increases the current value by 1 and returns a new instance after increment.
 *
 * @param Self 自增运算后返回的类型
 *
*/
interface Inc<Self> {

    /**
     * 自增运算符，将值加 1
     * Increment operator, increases the value by 1
     *
     * @return 自增后的新实侌
     *
     * @return New instance after increment
    */
    operator fun inc(): Inc<Self>
}
