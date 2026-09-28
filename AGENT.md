# Multi-Agent Collaboration & Documentation Governance Guide

> **适用范围**：所有参与本项目（Kids Language Companion）分析、设计、编码、测试与维护的 AI Agent 及协同开发人员。

---

## 1. 核心铁律：文档只增不删不改 (Immutable Docs Rule)

为了防止多 Agent 协同工作时发生**需求漂移（Requirements Drift）**、**设计基准被覆盖**或**历史上下文丢失**，制定以下强制约束：

### 🚫 禁止行为 (Strictly Prohibited)
1. **禁止修改既有文档**：不得修改 `docs/` 目录下任何已存在的规范文件（包括但不限于 `01_Product_Requirements.md`、`02_Technical_Design.md`、`03_UI_UX_Design.md`、`04_Mascot_Design.md`、`05_Mascot_Asset_Spec.md`、`06_Wireframes_and_Page_Prototype.md`、`07_High_Fidelity_Figma_Design_Spec.md`、`08_Android_Project_Initialization_and_Task_Plan.md`、`09_Development_Backlog.md` 等）。
2. **禁止删除任何既有文档与素材**：不得删除 `docs/` 下的任何历史文件、原型线框、Token 配置或资源图片。
3. **禁止篡改既有需求 ID 与验收基线**：需求 ID（如 `FR-xxx`, `G-xxx`）一经建立即永久绑定其原始语义，不得复用或篡改含义。

### ✅ 允许与推荐流程 (Additive-Only Workflow)
1. **增量新增新文件**：
   - 若出现新功能、新架构设计、新规则或对既有设计的补充/演化，必须以**新文件**形式存放于 `docs/` 目录中。
   - 命名格式建议遵循顺延编号或模块化命名（如 `docs/10_Offline_ASR_Vosk_Integration.md`、`docs/adrs/ADR-001-xxx.md` 等）。
2. **工作日志追加记录**：
   - 每次完成一轮研发、测试或重构后，在 `docs/WORKLOG.md` 末尾**追加（Append-Only）**本次工作记录，不得修改或覆盖过往历史日志。
3. **补充而非覆盖**：
   - 如果新方案推翻了旧方案，请在新文档中明确声明：“本方案基于 `docs/xx` 进行增量补充/替代，自 vX.X 起生效”，保留完整决策链与演进可追溯性。

---

## 2. 署名与追溯规则 (Signature & Attribution Rule)

为保证所有文档变更来源清晰、意图可回溯、多 Agent 协作时权责明确，强制执行以下署名规范：

### ✍️ 署名要求
所有在 `docs/` 下**新建文档**，或在允许追加的文档（如 `docs/WORKLOG.md`）中**追加内容**的 Agent，**必须明确署上自己的 Agent 名字与操作日期**。

### 📋 格式建议

1. **新建文档（在文档头部元数据块中署名）**：
   ```markdown
   # 文档标题

   > Author / Agent: [Agent Name / Role / Model]  
   > Date: YYYY-MM-DD  
   > Status: Draft / Baseline  
   > Scope: [简要说明]
   ```

2. **追加工作日志或记录（在日志标题中署名）**：
   ```markdown
   ## YYYY-MM-DD | [模块或任务名称] | By: [Agent Name]
   ```

---

## 3. 每轮对话本地提交规则 (Per-Turn Git Commit Rule)

为了确保工作进度的原子性、可回溯性并防止意外丢失改动：

1. **变动必提交**：
   在**每一轮对话**中，只要工作区发生了**任何文件变更**（代码编写、文档增补、配置调整等），在结束本轮回复前**都必须在本地进行一次 Git 提交 (`git commit`)**。
2. **提交信息规范**：
   提交信息必须遵循 Conventional Commits 规范（如 `feat:`, `fix:`, `docs:`, `test:`, `refactor:` 等），简要准确概括本轮改动的核心内容与目的。
3. **推送策略**：
   每轮必须完成**本地提交**；当用户明确要求 `push` 或特定里程碑阶段完成时，再推送到远端仓库。

---

## 4. 需求与实现对齐原则 (Alignment Principles)

1. **以 `docs/` 为唯一真理来源**：
   - 所有功能的开发实现必须回溯至 `docs/` 中的 PRD、UI 设计与技术规范。
   - 不得凭借 Agent 自行推测而脱离既定目标（例如：严禁自行增加复杂的文字阅读测试、账户系统或高压力打分）。
2. **儿童友好体验守则 (Child Mode Guardrails)**：
   - **零文字依赖**：确保核心练习闭环即使遮挡文字，幼儿也能通过看图、听音和触控自主完成；
   - **无惩罚机制**：禁止使用大红叉、扣分、蜂鸣等负反馈，错误仅使用温和提示并允许继续尝试；
   - **防卡题机制**：多次失败自动触发辅助通过（SoftPass），杜绝挫败感。
3. **构建与测试防线**：
   - 任何代码改动提交前，必须运行本地验证（单元测试 `./gradlew.bat test` 或构建 `./gradlew.bat assembleDebug`），确保编译与逻辑无破坏。
