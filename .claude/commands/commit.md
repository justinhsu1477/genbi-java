# Bilingual Commit Message (中英雙語 Commit)

Generate a git commit message following these conventions:

## Format
```
<type>(<scope>): <中文摘要>

<English summary>

- <中文細節 1>
- <中文細節 2>
```

## Type
- `feat`: 新功能
- `fix`: 修 bug
- `refactor`: 重構（不改功能）
- `test`: 新增/修改測試
- `docs`: 文件
- `chore`: 雜項（CI, 依賴更新等）
- `style`: 格式調整（不改邏輯）

## Scope
用受影響的模組: `websocket`, `crud`, `bedrock`, `profile`, `statemachine`, `dto`, `rls`, `auth`, `config`

## Rules
1. 第一行不超過 72 字元
2. 中文摘要簡潔，不超過 20 字
3. English summary 用完整句子
4. 細節用 bullet points
5. **不要** 加 `Co-Authored-By` 或任何 AI 相關標記

## Example
```
feat(profile): 新增 Profile CRUD 管理功能

Add Profile CRUD endpoints for database connection management

- 新增 DbProfile Entity + Repository
- 新增 ProfileRequest / ProfileResponse DTO
- 新增 DbProfileService (含 RLS 驗證)
- 新增 ProfileController (5 個 REST 端點)
- 新增 16 個單元測試 + MockMvc 測試
```

Now analyze the staged changes with `git diff --cached` and `git status`, then generate a commit message following the format above.
