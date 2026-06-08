# 函数符号 Big-M 优化记录

## 目标

在 `ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/symbol/function` 中，系统性优化函数符号的 Big-M 处理：当输入线性/二次多项式具有有限上下界时，默认 Big-M 应从输入范围推导；仅在无法推导时回退到全局默认值。

## 事项

- 为线性与二次多项式补充统一的有限范围推导工具。
- 将默认 `1e6` 的 Big-M 改为局部、按约束推导的 Big-M。
- 修正 Big-M 指示约束中已发现的语义与代数问题。
- 保留调用方显式传入 Big-M 的优先级。
- 补充或调整相关 KDoc，说明默认 Big-M 的推导逻辑。
- 编译验证核心模块。

## 计划

1. 完善 `BigM.kt` 公共能力：
   - 线性多项式有限上下界推导。
   - 二次多项式有限上下界推导。
   - 绝对值上界、非负最小 Big-M、差值多项式等辅助函数。
   - 按多项式列表和不等式差值生成默认 Big-M。
2. 修正基础指示约束：
   - 明确非零、正数、不等式满足三类语义。
   - 修正 `simpleIndicatorConstraints` 的满足/违反链接逻辑。
3. 优化线性函数：
   - `Abs`、`Binaryzation`、`And/Or/Not/Xor`、`OneOf`、`If`、`IfIn`、`IfThen`、`Imply`。
   - `Inequality`、`SameAs`、`SatisfiedAmount`、`SatisfiedAmountInequality`。
   - `Masking`、`Max/Min`、`MinMax/MaxMin`、`UnivariateLinearPiecewise`、`Sin`、`Cos`、`BalanceTernaryzation`。
4. 优化二次函数：
   - `QuadraticInStepRange`、`QuadraticMaskingRange`、`QuadraticMin`。
5. 清理非 Big-M 但相关的参数残留：
   - `Floor`、`Ceiling`、`Mod` 的 `bigM` 参数保留为兼容参数，但不再保存无效默认 `1e6`。
   - 修正 `Floor`、`Ceiling`、`Rounding` 中小数部分误用二值变量的建模问题。
6. 验证：
   - 运行 `mvn -pl ospf-kotlin-core test-compile`。
   - 必要时运行定向测试，若耗时过长则记录。

## 修改清单

- `BigM.kt`：新增/完善范围推导、局部 Big-M、指示约束工具。
- `Abs.kt`：默认 Big-M 从输入范围推导，并显式强制正负部互补。
- `Binaryzation.kt`：使用输入范围推导 Big-M 并修正二值化约束。
- `And.kt`、`OneOf.kt`：每个输入多项式使用自身 Big-M。
- `If.kt`、`IfIn.kt`、`IfThen.kt`、`Imply.kt`、`Sigmoid.kt`：区分正数/非零指示语义并使用范围推导。
- `Inequality.kt`、`SameAs.kt`、`SatisfiedAmount.kt`、`SatisfiedAmountInequality.kt`：按 `lhs-rhs` 的范围推导每条不等式的 Big-M。
- `Masking.kt`：使用输入多项式上下界重写掩码线性化。
- `Max.kt`、`MinMax.kt`：按候选多项式范围推导选择约束 Big-M。
- `UnivariateLinearPiecewise.kt`、`Sin.kt`、`Cos.kt`、`BalanceTernaryzation.kt`：分段相关 Big-M 改为基于输入/输出范围。
- `QuadraticInStepRange.kt`、`QuadraticMaskingRange.kt`、`QuadraticMin.kt`：使用二次多项式范围推导 Big-M。

## 验收标准

- 未显式传入 Big-M 且输入多项式有有限上下界时，不再默认使用 `1e6`。
- 输入范围无法推导时仍能回退到 `BIG_M_DEFAULT`，不破坏现有调用。
- 显式传入 Big-M 时保持现有行为，不被自动推导覆盖。
- Big-M 指示变量语义与 `evaluate` 逻辑一致。
- `mvn -pl ospf-kotlin-core test-compile` 通过。
- `git diff --check` 无空白错误。

## 执行结果

- 已完成线性与二次多项式有限上下界推导工具，并支持平方项跨 0 区间的更紧范围估计。
- 指示约束内部会按 `tolerance` 或 `strictBoundary` 自动扩展放松 Big-M，避免范围端点上的默认 M 被边界余量意外收紧。
- 已补充零值满足指示约束，修正 `Inequality`、`SameAs`、`SatisfiedAmount` 的 `EQ` 满足/违反方向。
- 已完成 `Abs`、`Binaryzation`、逻辑函数、条件函数、不等式满足类函数、`Masking`、`Max/Min`、分段线性函数、二次步进/掩码/最小值函数的默认 Big-M 优化。
- 已修正 `If`、`Sigmoid`、`Imply`、`IfIn` 中“正数/非负”判断误用“非零”指示的问题。
- 已重写线性 `Masking` 为基于输入上下界的四约束掩码形式，并修正 `QuadraticMin` 精确选择约束方向。
- `Floor`、`Ceiling`、`Mod` 的 `bigM` 参数已收口为兼容保留参数，不再生成无效默认 Big-M。
- `Floor`、`Ceiling` 已移除错误的二值小数变量，`Ceiling` 下界约束已修正为 `x >= k - 1 + eps`。
- `Mod` 已要求除数为正，并将求值逻辑改为 `x - d * floor(x / d)`，与非负余数约束一致。
- `Rounding` 已将小数部分改为连续非负变量，并使用局部小数指示 Big-M（默认 1）处理 0.5 阈值。
- `BalanceTernaryzation` 已支持自定义输入无界时的分段 fallback 范围，默认保持旧的 `[-1e6, 1e6]` 以兼容现有调用。
- `Semi` 已新增从变量有限边界推导半连续上下界的工厂，原无变量信息构造器仍保留兼容回退。

## 验证结果

- `mvn -pl ospf-kotlin-core test-compile`：通过。
- `git diff --check -- ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/symbol/function`：无空白错误，仅有 Git 的 LF/CRLF 提示。
