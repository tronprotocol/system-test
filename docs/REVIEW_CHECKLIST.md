# 文档人工校对清单

AI 生成的 .md 文件整体框架正确，但细节处可能存在幻觉。以下列出每个文件的重点校对项。

---

## 高优先级（对外可见/影响构建）

### README.md
- [ ] 项目描述是否准确反映当前状态
- [ ] CI badge 链接是否正确（需要实际仓库地址后更新）
- [ ] 依赖版本号（JDK 8/17、Gradle 6.3）是否与实际一致
- [ ] "Test Network Options" 章节的命令是否可执行

### CONTRIBUTING.md
- [ ] ~~solc 下载链接~~ ✅ 已修复为 tronprotocol/solidity
- [ ] Conventional Commits 规范示例是否与项目实际 commit 风格匹配
- [ ] 分支命名规范是否是你想要的
- [ ] PR 模板中的 checklist 是否合理

### docs/SETUP.md
- [ ] ~~solc 下载地址~~ ✅ 已修复
- [ ] 私有网络启动命令 `bin/FullNode -c config.conf --witness --es` 是否与实际 java-tron 版本匹配
- [ ] config.conf 中的 genesis 地址、witness 配置是否与 testng.conf 一致
- [ ] 端口号是否与实际测试环境一致
- [ ] troubleshooting 章节的解决方案是否真实有效

### docs/NETWORK.md
- [ ] 端口映射表是否与 testng.conf 中的实际配置完全一致
- [ ] config-node1.conf / config-node2.conf 中的 localwitness 私钥是否正确
- [ ] genesis.block 中的地址和余额是否与 testng.conf 一致
- [ ] Docker 相关命令是否可实际执行
- [ ] DPoS 出块流程描述是否准确

---

## 中优先级（文档质量）

### docs/ARCHITECTURE.md
- [ ] 基类层次图是否与实际代码匹配
- [ ] Helper 分解描述是否反映当前代码结构
- [ ] 提到的类名/方法名是否存在（重命名后可能遗漏更新）

### docs/STYLE_GUIDE.md
- [ ] 命名规范示例是否与项目实际一致
- [ ] 断言最佳实践是否合理
- [ ] 代码格式要求是否与 checkstyle 配置一致

### docs/TIP-TEST-MAPPING.md
- [ ] TIP 编号与实际 TIP 是否对应
- [ ] 测试类名是否存在（重命名后可能遗漏）
- [ ] 映射关系是否准确

### docs/GOOD_FIRST_ISSUES.md
- [ ] 24 个任务是否都还有效（有些可能已被完成）
- [ ] 提到的文件路径是否正确
- [ ] 难度评估是否合理

---

## 低优先级（内部参考）

### CONTRIBUTORS.md
- [ ] 模板格式是否合适

### .github/ISSUE_TEMPLATE/good_first_issue.md
- [ ] 模板字段是否合理

### docs/REFACTORING_REPORT.md / .html
- [ ] 数据统计是否准确
- [ ] 版本号、日期是否正确

### docs/worklog-2026-04-04.md
- [ ] 内部工作日志，可保留或删除

---

## 校对方法建议

1. **链接验证**：`grep -rn 'http' docs/ *.md | grep -v node_modules` 逐个点击验证
2. **类名验证**：文档中提到的类名用 `grep -r "ClassName"` 确认存在
3. **端口验证**：对照 `testng.conf` 检查所有端口号
4. **命令验证**：文档中的 shell 命令实际执行一遍
