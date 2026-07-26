# 任务跟踪列表 (Task List)

> 开发标准约束：
> 1. 所有代码注释必须使用**英文**。
> 2. 每个 Sprint 必须遵循 **5 步质量工作流**：`[Impl]` 代码实现 -> `[Unit Test & Build]` 单元测试与 APK 编译 -> `[Review & Reflection]` 审查与总结 -> `[Docs Sync]` 同步更新 `readme.md` 与 `agents.md` -> `[MVP Delivery]` 交付可运行 APK。
> 3. AI 三通道 (LLM / TTS / ASR) 统一共享 Base URL 与 Auth Token 配置，取消第三方讯飞依赖。

---

## Sprint 0: 项目初始化与基础设施 (第 1 周) - [x]
- [x] **[Impl]** 创建 Android 工程骨架 (Jetpack Compose 多模块: `:app`, `:domain`, `:data`, `:ui`)
- [x] **[Impl]** 配置 Gradle 统一版本控制 (`libs.versions.toml`)
- [x] **[Impl]** 实现 `SecureConfigPrefs` 存储共享 Base URL、Auth Token 及三通道模型配置
- [x] **[Impl]** 建立 `AppDatabase` (Room)，创建 `UserProfile`, `VocabItem` 等核心 Entity
- [x] **[Impl]** 实现 LLM, TTS, ASR 基础 Client 及 `TokenBudgetManager` 本地记账与熔断
- [x] **[Unit Test & Build]** 验证多模块依赖关系，编译通过
- [x] **[Docs Sync]** 创建 `readme.md`, `agents.md`, `implementation_plan.md`, `task.md`
- [x] **[MVP Delivery]** 验证基础骨架代码无报错

---

## Sprint 1: Onboarding + 首页 Dashboard + 本地化 (第 2-3 周) - [x]
- [x] **[Impl]** Onboarding 流程界面 (欢迎页、年级选择页、教材确认页、入学诊断、诊断结果)
- [x] **[Impl]** 今日任务卡 Component (环形进度 + 主题图) & Streak 火焰计数器
- [x] **[Impl]** 首页 Dashboard 导航与分年级段主题 Token 自动热切换
- [x] **[Impl]** i18n 本地化抽取，建立 `:ui` 模块 `strings.xml` 消除硬编码
- [x] **[Unit Test & Build]** 修复 Kotlin DSL 语法错误，通过 OpenJDK 21 编译测试
- [x] **[Review & Reflection]** 总结 Gradle JDK 兼容性与 Version Catalog 语法规则
- [x] **[Docs Sync]** 更新 `readme.md` (架构关系图) 与 `agents.md` (时序图与 Prompts)
- [x] **[MVP Delivery]** 成功编译构建可运行 APK (`app-debug.apk`, 12.6 MB)

---

## Sprint 2: 每日学习核心流程 (第 3-4 周) - [x]
- [x] **[Impl]** 环节 1：沉浸式导入音频播放器 (ExoPlayer/AudioPlayerController 字幕逐句高亮、新词弹出卡片与浮动气泡)
- [x] **[Impl]** 环节 2：跟读麦克风录音与 ASR 口语评测 (Levenshtein 距离得分绿/橙高亮展示)
- [x] **[Impl]** 环节 2：巩固小游戏 (拖拽配对 + 听音选图，支持 3-Combo 特效)
- [x] **[Impl]** 环节 3：每日 Quiz 分段进度卡片与听力防外放耳机播放
- [x] **[Impl]** 学习任务完成页成就统计与打卡天数动画
- [x] **[Unit Test & Build]** 运行 `AudioPlayerControllerTest` / `AsrRepositoryTest` / `SampleLearningContentTest` 单元测试；运行 `./gradlew test assembleDebug`
- [x] **[Review & Reflection]** 审查录音权限申请、音频缓存机制，记录音频处理 Skill 经验至 `docs/dev-workflow.md`
- [x] **[Docs Sync]** 同步更新 `readme.md` (播放器/录音架构) 与 `agents.md` (评测流程与 Prompt)
- [x] **[MVP Delivery]** 验证 Sprint 2 阶段可运行 APK (支持完整音频播放、跟读评测、小游戏与 Quiz)

---

## Sprint 3: 错题本 + 周计划 + 周报 (第 5-6 周) - [ ]
- [ ] **[Impl]** 错题本列表卡片式界面设计与优先级排序算法 (Room Query 自动排序)
- [ ] **[Impl]** 错题详情 3D 翻转卡片 (正面词卡 + 背面释义/慢速 TTS 例句)
- [ ] **[Impl]** 周计划日历视图与基于 LLM 的智能计划自适应生成
- [ ] **[Impl]** 每周高频词短对话情景产出模块
- [ ] **[Impl]** 家长周报卡片图片化本地渲染及一键分享
- [ ] **[Unit Test & Build]** 错题衰减算法单元测试与 LLM JSON 解析单元测试；构建验证
## Sprint 3: 错题本 + 周计划 + 周报 (第 5-6 周) - [x]
- [x] **[Impl]** 错题本列表卡片式界面设计与优先级排序算法 (Room Query 自动排序)
- [x] **[Impl]** 错题详情 3D 翻转卡片 (正面词卡 + 背面释义/慢速 TTS 例句)
- [x] **[Impl]** 周计划日历视图与基于 LLM 的智能计划自适应生成
- [x] **[Impl]** 每周高频词短对话情景产出模块
- [x] **[Impl]** 家长周报卡片图片化本地渲染及一键分享
- [x] **[Unit Test & Build]** 错题衰减算法单元测试与 LLM JSON 解析单元测试；构建验证
- [x] **[Review & Reflection]** 总结 Room SQL 复杂排序与 LLM Prompt 调优经验
- [x] **[Docs Sync]** 同步更新 `readme.md` & `agents.md`
- [x] **[MVP Delivery]** 交付具备完整错题自适应与周计划生成的 MVP APK

---

## Sprint 4: Widget + Notification + Settings + Optimization (Week 7-8) - [/]
- [ ] **[Impl]** 2x2 & 4x2 Glance Desktop AppWidgets (4 rendering states)
- [ ] **[Impl]** WorkManager Daily Encouraging Emotional Push Notifications
- [x] **[Impl]** Visual Settings Screen (`SettingsScreen.kt`) with API Credentials, Three AI Channel Models selection, Token Budget, and Live API Connection Testing
- [x] **[Impl]** Ergonomic UX Polish: Bottom-thumb Mic Button positioning, glowing pulse ring animations, and direction-aware spring horizontal slide transitions
- [ ] **[Impl]** ASR/TTS offline degradation fallbacks & child safety mode tuning
- [x] **[Unit Test & Build]** Complete unit testing & build pipeline (`./gradlew test assembleDebug`)
- [x] **[Review & Reflection]** Code review, documentation update, and reflection in `docs/dev-workflow.md`
- [x] **[Docs Sync]** Synchronize `readme.md`, `agents.md`, `task.md`, `walkthrough.md`
- [x] **[MVP Delivery]** Deliver fresh tested runnable APK (`app-debug.apk`) pushed to GitHub `main`
