# Bilingual PR Description (中英雙語 PR)

Generate a Pull Request description for the current branch.

## Format
```markdown
## Summary / 摘要
<1-3 bullet points in English>

## Changes / 變更內容
| 檔案/模組 | 變更類型 | 說明 |
|-----------|---------|------|
| ... | 新增/修改/刪除 | ... |

## Python Source Reference / Python 原始碼對照
| Java 檔案 | 對應 Python 檔案 | 備註 |
|-----------|------------------|------|
| ... | ... | ... |

## Test Plan / 測試計畫
- [ ] Unit tests pass (`mvn test`)
- [ ] ...

## Notes / 備註
...
```

## Steps
1. Run `git log main..HEAD --oneline` to see all commits in this branch
2. Run `git diff main...HEAD --stat` to see changed files
3. Generate the PR description following the format above
4. Keep it concise — reviewers should understand the changes in 30 seconds
