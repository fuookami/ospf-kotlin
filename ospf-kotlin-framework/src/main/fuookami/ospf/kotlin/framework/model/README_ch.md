# Model Package

:us: [English](README.md) | :cn: 简体中文

求解管线与影子价格抽象层，为领域框架提供约束注册、对偶信息管理和启发式分析的可扩展接口。

## 管线层次

```
MetaConstraintGroup
  └── Pipeline<in M : Model<*>>
        ├── register(model: M)           注册约束组到元模型
        ├── invoke(model: M): Try        执行管线
        ├── infeasibleReasons(iis)       获取不可行原因（线性/二次）
        │
        ├── CGPipeline<Args, Model, Map>
        │     ├── extractor()            获取影子价格提取器
        │     └── refresh(map, model, dualSolution)  刷新影子价格
        │
        └── HAPipeline<in M : Model<*>>
              ├── calculate(model, solution)  计算启发式目标值
              └── check(model, solution)      检查解有效性
```

### Pipeline<M>

约束管线接口。实现类负责向 `MetaModel` 注册一组约束，并在求解时执行。

典型用法：

```kotlin
class DemandPipeline : Pipeline<LinearMetaModel<Flt64>> {
    override fun register(model: LinearMetaModel<Flt64>) {
        // 注册约束组
    }

    override fun invoke(model: LinearMetaModel<Flt64>): Try {
        // 向模型添加约束和目标
        return ok
    }
}
```

### CGPipeline<Args, Model, Map>

列生成管线接口。扩展 `Pipeline`，增加影子价格提取和刷新能力，用于列生成定价阶段。

- `extractor()` — 返回 `ShadowPriceExtractor<Args, Map>`，通过参数计算影子价格。
- `refresh(shadowPriceMap, model, shadowPrices)` — 从对偶解刷新影子价格映射。
- 伴生对象提供 `refreshByKeyAsArgs()` 方法，按键从约束参数提取影子价格。

### HAPipeline<M>

启发式分析管线接口。扩展 `Pipeline`，增加启发式目标计算和解有效性检查。

- `calculate(model, solution)` — 计算启发式目标值，返回 `Ret<Flt64?>`。
- `check(model, solution)` — 检查解是否满足启发式条件。
- `invoke(model, solution)` — 执行分析并返回 `Ret<Obj>`，其中 `Obj` 包含标签和值。

## 管线列表

类型别名和批量执行扩展：

| 类型别名 | 说明 |
| --- | --- |
| `PipelineList<M>` | `List<Pipeline<M>>` |
| `CGPipelineList<Args, Model, Map>` | `List<CGPipeline<Args, Model, Map>>` |
| `HAPipelineList<M>` | `List<HAPipeline<M>>` |

`PipelineList<M>.invoke(model: M): Try` 依次注册并执行所有管线，首个失败即短路返回。

## 影子价格体系

```
ShadowPriceKey(limit: KClass<*>)
  ↓ 影子价格键，标识约束来源类型
ShadowPrice(key: ShadowPriceKey, price: Flt64)
  ↓ 影子价格，键值对
AbstractShadowPriceMap<in Args, in M>
  ├── map: Map<ShadowPriceKey, ShadowPrice>    影子价格映射表
  ├── invoke(arg: Args): Flt64                 按参数计算影子价格总和
  ├── get(key): ShadowPrice?                   按键获取
  ├── set(key, value)                          按键设置
  ├── put(price)                               放置影子价格
  ├── putOrAdd(price)                          放置或累加
  ├── put(extractor)                           注册提取器
  ├── remove(key)                              移除
  └── shrink()                                 收缩（移除零值）
  ↓
ShadowPriceExtractor<Args, M> = (AbstractShadowPriceMap<Args, M>, Args) -> Flt64
```

### 核心函数

- `extractShadowPrice(shadowPriceMap, pipelineList, model, shadowPrices)` — 从 `CGPipelineList` 提取影子价格：遍历管线列表，逐个刷新影子价格并注册提取器。
- `IntermediateSymbol<*>.refresh(shadowPriceMap, shadowPrices)` — 刷新中间符号的影子价格：如果符号参数是 `ShadowPriceKey`，则从对偶解提取值并放入映射。

### 使用模式

```kotlin
// 定义自定义影子价格映射
class MyShadowPriceMap : AbstractShadowPriceMap<MyArg, MyShadowPriceMap>()

// 从管线列表提取影子价格
val ret = extractShadowPrice(
    shadowPriceMap = shadowPriceMap,
    pipelineList = cgPipelines,
    model = metaModel,
    shadowPrices = lpResult.dualSolution
)

// 通过参数查询影子价格
val price = shadowPriceMap(myArg)
```

## 与领域框架集成

领域框架（CSP1D、BPP3D 等）通过以下方式使用管线体系：

1. **约束管线** — 领域建模阶段，通过 `Pipeline` 注册约束组到 `MetaModel`。
2. **列生成管线** — 列生成迭代中，通过 `CGPipeline` 从 LP 对偶解提取影子价格，驱动定价问题。
3. **启发式分析管线** — 求解后分析阶段，通过 `HAPipeline` 评估解质量和启发式指标。
4. **扩展管线** — 下游业务通过 `extra pipeline` 注入自定义约束和目标，无需修改框架核心。
