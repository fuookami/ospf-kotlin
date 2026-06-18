# AGENTS

## 作用范围
本文件适用于当前目录及其所有子目录。

## 规则来源
请遵循 `.rules/` 下实际存在的规则文件：

- `.rules/chore.md`
- `.rules/framework-architecture.md`
- `.rules/error-handling.md`

## 明确优先级顺序（从高到低）
1. `.rules/chore.md`
2. `.rules/framework-architecture.md`
3. `.rules/error-handling.md`

## 冲突处理
当规则冲突时，严格按上述优先级顺序执行。

## 语言要求
始终使用简体中文与用户对话。

## 提交信息要求
进行 `git commit` 或 `git commit --amend` 时，提交信息内容要具体、完整，清晰说明改动目的与关键变更点，避免过于简短或笼统的描述。
提交信息必须包含符合 Conventional Commit 风格的 Header。
