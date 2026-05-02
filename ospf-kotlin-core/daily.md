# OSPF Kotlin Core Refactor Daily

记录日期：2026-05-02

本文件为 P9 执行清单版，目标是让每项任务可直接分配、编码、验收。

---

## 0. 总体目标

1. 对外泛型主线统一：`V : RealNumber<V>, NumberField<V>`。
2. 求解器内部边界显式化：仅在 solver-boundary 使用 Flt64。
3. 删除桥接类型与桥接路径文件，不再保留“旧路径转发壳”。
4. 补齐二次型函数缺口，并接入 register/flatten/dump 全链路。

---

## 1. 任务分解（按工作包）

## WP-1 SubObject 求值主线收口

### 1.1 目标

- `MetaModel.SubObject` 与 `mechanism/SubObject` 的对外求值入口统一为泛型 `evaluate<V>`。
- Flt64 仅保留 solver-boundary 入口，例如 `evaluateAsFlt64` / `evaluateSolver`。

### 1.2 需改文件

1. `ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/model/mechanism/MetaModel.kt`
2. `ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/model/mechanism/SubObject.kt`
3. `ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/model/mechanism/MechanismModel.kt`（调用链对齐）

### 1.3 具体改动

1. `MetaModel.SubObject`：
- 把当前返回 `Flt64?` 的主 `evaluate` 系列改为 `V?` 主线。
- 增加或重命名 solver 专用求值函数，明确 Flt64 语义。

2. `SubObject.kt`：
- 抽象类主接口只保留泛型求值。
- 线性/二次子类同步实现泛型主接口。
- solver 路径统一转到显式 Flt64 边界方法。

3. `MechanismModel.kt`：
- 所有 subobject 聚合计算对外路径调用泛型接口。
- 求解器相关路径调用 solver-boundary 接口。

### 1.4 验收标准

1. `MetaModel.SubObject` 主 `evaluate` 不再返回 `Flt64`。
2. `SubObject.kt` 中 `evaluate` 主签名全为 `V`。
3. 编译通过，线性/二次目标值计算无行为回归。

---

## WP-2 SlackFunction 全链路泛型化

### 2.1 目标

- `SlackFunction<V>` 主属性与主工厂全部泛型化。
- 兼容 Flt64 重载保留，但只能做薄委托。

### 2.2 需改文件

1. `ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/intermediate_symbol/function/Slack.kt`
2. `ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/intermediate_symbol/function/SlackRange.kt`
3. `ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/intermediate_symbol/function/FunctionSymbol.kt`

### 2.3 具体改动

1. `Slack.kt`：
- `polyX: LinearPolynomial<Flt64>` -> `LinearPolynomial<V>`。
- `CompationObject.invoke` 主重载参数改为 `LinearPolynomial<V>`、`LinearIntermediateSymbol<V>`。
- 增加 `ToLinearPolynomial<V>` 工厂重载。
- `LinearIntermediateSymbolFlt64` / `LinearPolynomial<Flt64>` 重载改为一次转换后委托。

2. `SlackRange.kt`：
- `upper/lower` 改为泛型多项式。
- 增加 `ToLinearPolynomial<V>` 泛型工厂。
- Flt64 重载保留为委托壳。

3. `FunctionSymbol.kt`：
- `pos/neg/polyX` 从 Flt64 主暴露改为泛型主暴露。
- Flt64 导出保留为内部转换工具，不作为主接口。

### 2.4 验收标准

1. `SlackFunction` 主属性、主 `invoke` 无 Flt64 绑定。
2. `SlackRange` 主构造无 Flt64 绑定。
3. DSL 可直接传 `ToLinearPolynomial<V>`。
4. 兼容重载仍可用且无重复业务逻辑。

---

## WP-3 MathFunctionSymbol.register 时机修正

### 3.1 目标

- register 从 `MetaModel` 构建期迁移到 `MetaModel -> MechanismModel` 转换期。

### 3.2 需改文件

1. `ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/model/mechanism/MetaModel.kt`
2. `ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/model/mechanism/MechanismModel.kt`
3. `ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/token/TokenTable.kt`

### 3.3 具体改动

1. `MetaModel.kt`：
- 去除最终 register 副作用逻辑，仅保留符号收集与声明。

2. `MechanismModel.kt`：
- dump 时复制 `MetaModel.tokenTable`。
- 在复制表上执行 `MathFunctionSymbol.register` 及依赖注册。
- 将注册完成 tokenTable 注入 MechanismModel。
- 同步修正同步/异步 dump 两条路径。

3. `TokenTable.kt`：
- 保证注释与行为一致：register 在机制模型构建阶段完成。
- 移除可绕过 dump 的旁路注册入口（若存在）。

### 3.4 验收标准

1. register 不再在 MetaModel 构建时完成闭包。
2. MechanismModel 注入 tokenTable 后可完整解析函数依赖。
3. 并发/非并发 dump 结果一致。

---

## WP-4 删除桥接层

### 4.1 目标

- 删除桥接路径与桥接 DSL，不保留“旧包名转发壳”。

### 4.2 需改文件

1. `ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/frontend/model/mechanism/MathInequalityDsl.kt`（删除）
2. `ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/model/mechanism/MathInequalityBridge.kt`（删除或内联后删除）
3. `ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/model/mechanism/MathInequalityDsl.kt`（仅保留主 DSL）
4. 全仓 import 调整（core/framework/example）

### 4.3 具体改动

1. 删除 bridge 文件，并把调用迁移到主实现路径。
2. 清理所有旧路径 import。
3. 若存在名字冲突，优先修改调用方全限定名，不恢复别名导入。

### 4.4 验收标准

1. 指定桥接文件不存在。
2. 全仓 `import ... as ...` 命中为 0（已完成，持续门禁）。
3. 无旧路径 import 残留。

---

## WP-5 二次型函数缺口补齐

### 5.1 目标

对齐 `ospf-kotlin-main` quadratic_function 缺口，补齐当前 function 目录能力。

### 5.2 参考与差集

对照目录：
`E:\workspace\ospf-kotlin-main\ospf-kotlin-core\src\main\fuookami\ospf\kotlin\core\frontend\expression\symbol\quadratic_function`

当前缺口文件：
1. `Linear.kt`
2. `Min.kt`
3. `MaskingRange.kt`
4. `InStepRangeFunction.kt`

### 5.3 新增文件

新增到：
`ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/intermediate_symbol/function/`

### 5.4 具体改动

1. 每个新函数符号提供：
- 泛型主构造。
- `ToLinearPolynomial<V>` 工厂重载。
- solver-boundary 显式转换参数（`IntoValue<V>` 或同等 converter）。

2. 对接流程：
- register 到 tokenTable。
- flatten 到 linear/quadratic 数据。
- dump 到 mechanism model。

### 5.5 验收标准

1. 四个缺口文件全部新增并编译通过。
2. 每个函数具备 register + flatten + dump 路径。
3. 无桥接壳写法。

---

## WP-6 泛型入口清理（防回流）

### 6.1 目标

把高频函数中的 Flt64 主入口下沉为兼容入口，避免后续继续默认 Flt64。

### 6.2 需改文件

1. `function/Masking.kt`
2. `function/Max.kt`
3. `function/MinMax.kt`
4. `function/Bridge.kt`
5. `function/QuadraticBridge.kt`

### 6.3 具体改动

1. 主 `invoke` 改为泛型签名。
2. Flt64 特化重载仅保留委托。
3. 保持行为不变，不重写约束语义。

### 6.4 验收标准

1. 主 `invoke` 签名不再写死 `LinearPolynomial<Flt64>` 或 `LinearIntermediateSymbolFlt64`。
2. 现有 Flt64 调用样例继续可编译。

---


## WP-7 heuristic/callback 泛型主链清零

### 7.1 目标

- `solver/heuristic` 与 `model/callback` 主链完成泛型化。
- 移除 `evaluateAsFlt64`（及等价 Flt64 专用 evaluate 入口）。
- 主链不再依赖 Flt64 硬编码类型。

### 7.2 需改文件

1. `ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/solver/heuristic/*.kt`
2. `ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/model/callback/CallBackModelInterface.kt`
3. `ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/model/callback/CallBackModel.kt`
4. 所有调用 callback/heuristic 的 solver 入口文件（按编译报错收敛）

### 7.3 具体改动

1. `solver/heuristic`：
- 把 `Flt64` 写死的接口参数、返回值、策略参数改为泛型类型参数。
- 仅在必要的数值策略处保留可注入 converter，不允许写死 `Flt64`。

2. `model/callback`：
- `AbstractCallBackModelInterface`、`CallBackModel`、`MultiObjectCallBackModel` 主路径改为泛型。
- 现有 `Flt64` 专用 typealias/实现下沉为兼容层，禁止主调用链依赖。

3. 求值入口：
- 删除 `evaluateAsFlt64` 与等价专用入口。
- 所有 evaluate 调用统一走泛型主接口。

### 7.4 验收标准

1. `solver/heuristic` 主链无 Flt64 硬编码。
2. `model/callback` 主链无 Flt64 硬编码。
3. 全仓 `evaluateAsFlt64`（及等价专用 evaluate）命中为 0。
4. 三段 Maven 编译门禁全部通过。
## 2. 执行顺序与依赖

1. 第一步：WP-1（SubObject）
2. 第二步：WP-2（Slack 主链）
3. 第三步：WP-3（register 时机）
4. 第四步：WP-4（桥接删除）
5. 第五步：WP-5（二次型补齐）
6. 第六步：WP-6（泛型入口防回流）
7. 第七步：WP-7（heuristic/callback 泛型主链清零）

依赖关系：
1. WP-3 依赖 WP-1/2 的接口稳定。
2. WP-4 需在 WP-3 后执行，避免删桥接后链路不完整。
3. WP-5/6 可并行，但最终在同一轮编译门禁统一验收。

---

## 3. 回归测试清单

### 3.1 单测新增/更新

1. `SlackFunction`：
- 泛型 `LinearPolynomial<V>` 构造。
- `ToLinearPolynomial<V>` 构造。
- Flt64 兼容重载委托正确性。

2. `SubObject`：
- `evaluate<V>` 与 solver-boundary 结果一致性。
- 线性/二次子对象分别覆盖。

3. `register` 时机：
- MetaModel 阶段无最终副作用。
- dump 后 tokenTable 依赖闭包完整。

4. 二次型新增函数：
- register 成功。
- flatten 结构正确。
- dump 可进入机制模型。

### 3.2 编译门禁

```powershell
mvn -pl ospf-kotlin-core -DskipTests compile
mvn -pl ospf-kotlin-framework -am -DskipTests compile
mvn -pl ospf-kotlin-example -am -DskipTests compile
```

---

## 4. 量化验收口径

1. `import ... as ...`：命中 0（持续检查）。
2. `Slack.kt` 主签名中的 `LinearPolynomial<Flt64>`：命中 0。
3. `Slack.kt` 主签名中的 `LinearIntermediateSymbolFlt64`：命中 0。
4. `MetaModel.SubObject` 主 `evaluate` 返回 Flt64：命中 0。
5. 指定桥接文件存在性：0 文件。
6. 二次型差集文件：4/4 已新增。
7. 三段 Maven 编译：全部 SUCCESS。

---

## 5. 风险与回滚策略

1. register 时机变更导致依赖顺序问题：
- 先补测试再迁移；失败时仅回滚 register 触发点，不回滚泛型签名改动。

2. 删除桥接文件导致外部 import 断裂：
- 同提交完成 import 迁移；若影响范围扩大，先引入迁移说明再删文件。

3. 二次型补齐引入行为偏差：
- 先上线最小闭环（register/flatten/dump），再扩高级工厂重载。

---

## 6. 完成定义（DoD）

1. WP-1 ~ WP-7 全部完成。
2. 量化验收口径全部达标。
3. 编译门禁全部通过。
4. daily 与实际代码状态一致，无“文档完成/代码未完成”项。

