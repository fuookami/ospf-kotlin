# ospf-kotlin-quantities 单位转换规则升级计划

## 目标

将 Kotlin quantities 的单位转换从单一比例 `Scale` 升级为支持更一般的转换规则，覆盖普通线性单位和带 offset 的仿射单位。

## 原则

1. 本计划由其它会话执行；当前 Rust 会话不修改 Kotlin 代码。
2. offset 是单位转换规则的一部分，但会改变普通物理量代数语义。
3. 普通单位继续使用线性转换。
4. 摄氏度、华氏度、兰氏度等绝对温标使用仿射转换。
5. 仿射单位不能被普通乘除幂误用。

## 事项

### 1. 新增转换规则模型

- 新增类似结构：
  - `Linear(scale: Scale)`
  - `Affine(scale: Scale, offset: FltX)`
- 普通单位迁移为 `Linear(scale)`。
- `PhysicalUnit.scale` 可保留为线性兼容入口，或改为从 conversion rule 读取线性 scale。

### 2. 改造单位转换

- 将 `PhysicalUnit.to(unit)` 从仅返回比例因子升级为值转换能力。
- 保留线性比例因子接口用于普通单位。
- 绝对温标使用：
  - `standard = value * scale + offset`
  - `target = (standard - targetOffset) / targetScale`

### 3. 温度语义

- 明确区分绝对温度点和温差。
- 至少补充测试：
  - `0 °C -> 273.15 K`
  - `32 °F -> 273.15 K`
  - `100 °C -> 212 °F`
  - 普通温差转换仍按线性比例处理。

### 4. 运算规则

- `Affine` 单位禁止普通乘除幂。
- `Affine + Affine` 不应作为普通物理量加法成立。
- `Affine - Affine` 应得到线性温差。
- `Affine ± LinearDifference` 可得到新的绝对温度。

### 5. 验收标准

1. 所有普通单位转换结果保持兼容。
2. 绝对温标转换正确。
3. 仿射单位不会参与错误的普通代数运算。
4. 现有单位制和自定义单位仍可工作。
5. Kotlin quantities 测试通过。

