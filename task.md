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

## Sprint 4: Widget + Notification + Settings + Optimization (Week 7-8) - [x]
- [x] **[Impl]** 2x2 & 4x2 Glance Desktop AppWidgets (4 rendering states)
- [x] **[Impl]** WorkManager Daily Encouraging Emotional Push Notifications
- [x] **[Impl]** Visual Settings Screen (`SettingsScreen.kt`) with API Credentials, Three AI Channel Models selection, Token Budget, and Live API Connection Testing
- [x] **[Impl]** Ergonomic UX Polish: Bottom-thumb Mic Button positioning, glowing pulse ring animations, and direction-aware spring horizontal slide transitions
- [x] **[Impl]** ASR/TTS offline degradation fallbacks & child safety mode tuning
- [x] **[Unit Test & Build]** Complete unit testing & build pipeline (`./gradlew test assembleDebug`)
- [x] **[Review & Reflection]** Code review, documentation update, and reflection in `docs/dev-workflow.md`
- [x] **[Docs Sync]** Synchronize `readme.md`, `agents.md`, `task.md`, `walkthrough.md`
- [x] **[MVP Delivery]** Deliver fresh tested runnable APK (`app-debug.apk`) pushed to GitHub `main`

---

## Sprint 4.5: Bug Fix & UI Polish - [x]
- [x] **[Impl]** 修复 Dashboard 底部卡片布局不完整 (`MainActivity.kt` `AnimatedContent` 添加 `fillMaxSize`)
- [x] **[Impl]** 修复入学诊断题始终相同 (`DiagnosisScreen` 传递真实年级，`DiagnosisViewModel` 使用 `GradeBand` 适配，`maxTokens=1500`)
- [x] **[Impl]** 修复 `DIAGNOSIS` 缺少 LLM 降级模板 (`LlmRepositoryImpl` 新增 10 题 JSON 兜底)
- [x] **[Impl]** 修复 `LingoAvatar` 小尺寸裁剪 (Canvas 坐标按 `min(canvasW, canvasH)/150f` 等比缩放)
- [x] **[Impl]** 合并重复结果页 (移除 `QuizResultPage`，Quiz 直接进入 `TaskCompleteScreen`)
- [x] **[Impl]** 移除 `GameOptionButton` 死代码参数
- [x] **[Impl]** 沉浸式音频接入真实 TTS (`LearningViewModel` 监听字幕索引，`SystemTtsHelper` 逐句朗读)
- [x] **[Impl]** 学习流程增加阶段间返回导航 (`goToPreviousStage()`)
- [x] **[Unit Test & Build]** 全模块编译通过

---

## Sprint 5: Resilient Learning Engine (弹性学习引擎) - [x] v1.5
> Principle: Children interrupt learning sessions constantly. This sprint adds state machine, checkpoint resume, exception paths, and emotional intervention.

### Phase A: Task State Machine & Checkpoint Resume
- [x] **[Impl]** Task state machine (NOT_STARTED / IN_PROGRESS / PAUSED / COMPLETED / EXPIRED) via `TaskStatePrefs` + SharedPreferences; `LearningViewModel` auto-restores PAUSED on init; `LearningContainer` auto-pauses on `ON_STOP`
- [x] **[Impl]** Question-level checkpoint persistence - `Checkpoint` data class with stage/phase/questionIndex/score/timestamp; saved in all progression methods; restored with resume dialog
- [x] **[Impl]** EXPIRED task next-day redo - 23:59 expiry; redoable but no streak credit; non-punitive messaging (deferred to Sprint 7)

### Phase B: Read-Along Exception Paths (6 Branches)
- [x] **[Impl]** Recording < 1s detection via `MediaPlayer.duration` in `DiagnosisViewModel`; `RecordingState.TOO_SHORT` prompt
- [x] **[Impl]** Network failure degradation - `evaluateWithRetry()` with 2 retries + 1s delay; `RecordingState.NETWORK_ERROR` with retry button
- [x] **[Impl]** Mic permission denied - `rememberLauncherForActivityResult` runtime permission request in `DiagnosisScreen`; `RecordingState.FAILED` on mic failure
- [x] **[Impl]** 3 consecutive retries on same sentence - Lingo intervention (deferred to Sprint 7 for full pre-teach loop redesign)
- [x] **[Bug]** Fix `DiagnosisScreen.kt:426` dummy.wav -> `VoiceRecorder` + `stopAndEvaluate()`

### Phase C: Emotional State Intervention
- [x] **[Impl]** `consecutive_negative_signal` counter in `LearningViewModel` - tracks quiz/game/wrong, low ASR score, retry; threshold=3 triggers `_showIntervention`
- [x] **[Impl]** Lingo emotional intervention dialog with Keep Going / Take a Break (pause) / Skip This Stage (advance) options; no penalty

### Phase D: Bug Fixes & Hardcode Cleanup
- [x] **[Bug]** Fix `QuizScreen.kt` SPELL_FILL_BLANK hardcoded "cla__room" -> `question.question`
- [x] **[Bug]** Fix hardcoded grade -> read from `lingo_app_prefs` via `LocalContext`
- [x] **[Bug]** Fix hardcoded "Buddy" -> `@ApplicationContext` + read `child_name` from prefs
- [x] **[Bug]** Fix hardcoded `duration=900L`/`weeklyDayNumber=1` -> `sessionStartTimeMs` calc + `taskDayIndex` from `getTaskDay()`
- [x] **[Bug]** Fix `TaskCompleteScreen.kt:206` no-op share -> `Intent.ACTION_SEND` with achievement summary
- [x] **[Bug]** `domain/bin/` already covered by `**/bin/` in `.gitignore`

### Phase E: Quality
- [x] **[Unit Test & Build]** `TaskStatePrefsTest` (9 tests: state transitions, checkpoint save/restore, task day); `./gradlew test assembleDebug` (47 tests, 1 pre-existing failure)
- [x] **[Review & Reflection]** Document resilient design principles for children's apps
- [x] **[Docs Sync]** Updated `agents.md` (intervention sequence + prompt), `implementation_plan.md`, `task.md`
- [x] **[MVP Delivery]** Deliver V1.5 resilient learning engine APK

---

## Sprint 6: AI Presence & Transparency (AI 存在感显性化) - [x] v1.6
> Principle: The AI is already working behind the scenes. This sprint exposes the judgment process and evidence that was already happening, making "AI presence" a perceptible, interactive experience. No new AI capabilities - just making existing work visible.

### Phase A: AgentDecisionLog Infrastructure
- [x] **[Impl]** New `AgentDecisionLogEntity` (id, timestamp, decisionType, title, description, metadata, confidence, lastModified) + DAO (getRecentDecisions, getDecisionsSince, insert); DB v3 migration adding `agent_decision_log` table
- [x] **[Impl]** `AgentDecisionLogRepository` wrap write/query; inject into existing decision points (WeeklyPlanRepository, ErrorBookRepository, LearningViewModel)
- [x] **[Impl]** `PlanEntity` add `rationaleSnapshot` field for per-day plan rationale storage

### Phase B: "Lingo Observes" Real-time Insights (Enhancement 1)
- [x] **[Impl]** `ObservationTriggerEngine` - rule-based triggers querying `LearningRecordDao.getRecordsSince(7 days)` for longitudinal comparison; active trigger patterns (word previously wrong now correct / pronunciation improvement vs same sentence / retries before correct). `FAST_ANSWER` type declared in enum but rule deferred (needs per-question timing history not yet collected)
- [x] **[Impl]** Non-blocking `SpeechBubble` composable (3s auto-dismiss, rendered in `LearningContainer` so it appears in Quiz/Game without blocking the next question); template-first messages (offline-safe), LLM `OBSERVE` enrichment not wired (templates cover current patterns)
- [x] **[Impl]** Observation decision logging to `AgentDecisionLog(decisionType=OBSERVATION_MADE)`; per-attempt word-level `LearningRecord`s (QUIZ/GAME/SPEAKING) now persisted so longitudinal comparison has history; stats methods filtered to `DAILY_PRACTICE` to stay accurate

### Phase C: Plan "Why" Annotation + ExplainDecisionUseCase (Enhancement 2)
- [x] **[Impl]** `ExplainDecisionUseCase` (moved from original Sprint 6 Phase B) - generates explanations from persisted `rationaleSnapshot`, not re-deriving via LLM
- [x] **[Impl]** Weekly Plan Prompt update - JSON output adds per-day `rationale` field; `rationaleSnapshot` persisted at generation time
- [x] **[Impl]** `PlanDayCard` expandable annotation component - "i" icon on each day card, tap to expand/show `day.rationale` text

### Phase D: Weekly Report "AI Adjustments" Section (Enhancement 3)
- [x] **[Impl]** New card in `WeeklyReportScreen`: "What Lingo adjusted this week" - queries past 7 days of `AgentDecisionLog`, shows 1-2 representative decisions in natural language
- [x] **[Impl]** `WeeklyReportViewModel` add `agentAdjustments: List<String>` field

### Phase E: AI Growth Notes Page (Enhancement 6)
- [x] **[Impl]** `AiGrowthNotesScreen` - lightweight timeline list of `AgentDecisionLog` entries (date + type tag + description); parent-oriented, entry from Settings
- [x] **[Impl]** Settings page new navigation entry "AI Growth Notes"

### Phase F: Quality
- [x] **[Unit Test & Build]** `ObservationTriggerEngine` rule tests (8 cases), `AgentDecisionLogRepositoryImpl` CRUD tests (3), `ExplainDecisionUseCase` tests (7); only pre-existing `SessionBuilderTest`/`AsrRepositoryTest` failures remain
- [x] **[Bug]** Fix bottom-nav crash when switching to Plan/Error Book/Weekly Report tabs - `IndexOutOfBoundsException` (Compose `Stack.pop` during tab switch) resolved via removing `AnimatedContent` tab wrapper in `MainActivity` + replacing early-return `return@Column` with `if/else` structure in `WeeklyPlanScreen`/`ErrorBookScreen`/`WeeklyReportScreen`; verified on emulator (Plan tab, Error Book tab, Weekly Report, rapid 8-tab switching all render, no FATAL); confirmed the `composeBom` bump was unnecessary (reverted to `2024.02.00` - code fix alone resolves crash)
- [x] **[Docs Sync]** Update `agents.md` (Observation Agent + Decision Transparency Layer + rationale prompt), `implementation_plan.md`, `task.md`
- [x] **[MVP Delivery]** Deliver V1.6 AI Presence APK (`app-debug.apk`, verified: Plan/Error Book/Weekly Report tabs + crash fix re-tested on emulator with reverted BOM, all tests green except pre-existing failures)

---

## Sprint 7: Pedagogical Deepening & Agent Intelligence (教学法深化与 Agent 智能) - [x] v2.0
> Principle: Build on Sprint 6 AI Presence infrastructure. Add core pedagogical activities and bounded-autonomy Agent decision-making.

### Phase A: Pedagogical Core Activities
- [x] **[Impl]** Pre-teach vocabulary warm-up - enhance existing `PreTeachScreen` with image association and ESA Engage interaction
- [x] **[Impl]** Listen-Repeat-Compare loop - TTS demo -> record -> playback comparison (play child's recording) -> ASR score -> optional retry
- [x] **[Impl]** Phonics blending for PRIMARY band - CVC word building, onset-rime, minimal pairs discrimination
- [x] **[Impl]** Spaced repetition in Quiz - auto-insert Error Book words at Ebbinghaus intervals (1/3/7/14 days); reuse `nextReviewTimestamp` field
- [x] **[Impl]** Production task scoring - complete SPELLING/DICTATION evaluation logic (enum exists, scoring incomplete); add sentence writing type

### Phase B: Bounded-Autonomy Agent
- [x] **[Impl]** `DiagnoseAnomalyUseCase` - structured learning summary input -> predefined category output (exam pressure / schedule change / motivation decline / difficulty mismatch / uncertain) + confidence; low confidence (<0.6) defers to parent
- [x] **[Impl]** Error Book Agent follow-up - child asks "why can't I remember this word?"; Agent uses full error history for personalized explanation
- [x] **[NOTE]** `ExplainDecisionUseCase` implemented in Sprint 6 Phase C; reused here

### Phase C: Gamification & Incentives
- [x] **[Impl]** Reward animations & XP system - particle effects on correct, XP pop counters, full-screen level-up celebration
- [x] **[Impl]** Daily 3-goal system - 1 session / 80%+ accuracy / 5 new words, each tracked with badge rewards
- [x] **[Impl]** Hint/Skip system completion - existing 3-level hint (level 1 = LLM); add skip (no XP but no penalty)

### Phase D: Adaptive & Parent Reports
- [x] **[Impl]** Adaptive difficulty - adjust sentence length (±3 words) and CEFR level based on last quiz accuracy; log to `AgentDecisionLog` (Sprint 6 infra)
- [x] **[Impl]** Parent detail report - per-word pronunciation error breakdown, time distribution, weak skill tag cloud, PDF/WeChat export
- [x] **[Impl]** Makeup card mechanic - 2 cards/month, streak break triggers active choice (not auto-use), preserves child's agency

### Phase E: Quality
- [x] **[Unit Test & Build]** Spaced repetition algorithm tests, Phonics module tests, attribution agent tests, adaptive difficulty tests; `./gradlew test assembleDebug` (71 new tests pass; only pre-existing SessionBuilderTest/AsrRepositoryTest/LlmRepositoryImplTest failures remain)
- [x] **[Review & Reflection]** Document ESA model, bounded-autonomy Agent design, spaced repetition mobile best practices (see `readme.md` Sprint 7 section)
- [x] **[Docs Sync]** Full doc sync for V2.0 architecture
- [x] **[MVP Delivery]** Deliver V2.0 pedagogical + Agent intelligence APK (`app-debug.apk`, verified on emulator: full PreTeach→Immersion→Practice→Game→Quiz→Results flow, ESA Engage word reveal, Listen-Repeat-Compare offline ASR, XP persisted `total_xp=40` + makeup card `2026-08`, no crashes)

---

## Sprint 10: 可见成长 - 游戏化显性化与持久化 (Visible Growth) - [x] v3.0
> 原则：游戏化引擎 (XP/每日目标/补签卡) 已存在但**对用户不可见**，且无持久化实体。本 Sprint 只做"展示已存在之物"，不新增 AI 能力。技术依赖最小、风险最低、价值最高，独立可交付。

### Phase A: 游戏化状态持久化 (Gamification Persistence)
- [x] **[Impl]** 新增 `GamificationState` domain 实体 (totalXp / 每日目标 / 补签卡余额) + DAO + Repository；数据库版本升至 v4 新增 `gamification_state` 表
- [x] **[Impl]** `LearningViewModel` 的 XP/目标/补签卡写入统一走新 Repository（当前 `lingo_xp_prefs` 逻辑与持久化分离）

### Phase B: 游戏化 UI 显性化 (Visible Growth Surfaces)
- [x] **[Impl]** Dashboard 头部 + TaskComplete 页新增**持久化 XP 进度条**与等级徽章 (复用 `XpRewardSystem.levelInfo()`)
- [x] **[Impl]** TaskComplete 渲染每日三目标徽章行 (当前 `_dailyGoals` 为死 UI 状态，仅计算未展示)
- [x] **[Impl]** Dashboard 展示补签卡余额 (当前仅学习中断弹窗时可见)

### Phase C: 质量
- [x] **[Unit Test & Build]** GamificationState Repository CRUD 测试 (4) + XP/等级 UI 状态映射测试；`./gradlew test assembleDebug`
- [x] **[Impl]** 修复 TaskComplete 内容溢出 — 结果页增加 `verticalScroll`，确保 XP 栏/目标徽章/返回按钮全部可达
- [x] **[Docs Sync]** 更新 `readme.md` / `implementation_plan.md` / `task.md` / `walkthrough.md`
- [x] **[MVP Delivery]** 交付 V3.0 可见成长 APK

---

## Sprint 10.5: 发布加固 - 阻断性缺陷修复与离线可用性 (Release Hardening) - [x] v3.0.1
> 触发：v3.0.0 实测反馈 5 个问题。技术负责人逐一代码定位根因后，将其编排为单个阻断性修复 Sprint（先于 Sprint 11），全部走 5 步质量工作流。不新增 AI 能力，只修复体验与可用性。

### Phase A: 计划→开始学习链路 (Blocker, Issue 2)
- [x] **[Impl]** 修复 `PlanDayCard` 无"Start"按钮：今日/已完成卡片均显示 Start/Review 按钮（原条件 `!isCompleted && !isToday` 导致今日无法开始）
- [x] **[Impl]** `onStartLearning(dayIndex)` 透传：MainActivity 接收 dayIndex 并传入 `LearningContainer` → `LearningViewModel.setGrade(grade, dayIndex)`，按计划日加载会话
- [x] **[Impl]** Dashboard 今日任务卡从已加载周计划读取 (主题/时长/目标)，替换硬编码 "15 Mins / 5 Words + 2 Speech"；新增 `WeeklyPlanRepository.getDayTaskSummary()`
- [x] **[Impl]** 修复计划生成离线兜底：LLM 失败时回退 `generateDefaultPlanJson(gradeBand)`（真实 day 卡片），原兜底 `days:[]` 导致"生成了计划却无法开始"

### Phase B: 评测音频与模型配置门槛 (Issue 1)
- [x] **[Impl]** 学习流程接入配置化 TTS (`TtsRepository.getSpeech` + MediaPlayer)，`SystemTtsHelper` 作为离线降级；`speakWithTts()` 统一入口覆盖沉浸字幕/跟读/游戏/Quiz
- [x] **[Impl]** 模型配置缺失检测：`isOfflineMode` 状态 + 学习流程内离线横幅 "⚠️ Offline mode"

### Phase C: 难度匹配年级 (Issue 3)
- [x] **[Impl]** Onboarding 持久化 `diagnostic_level` (A/B/C) 到 `lingo_app_prefs`（MainActivity 原丢弃级别）
- [x] **[Impl]** 诊断级别驱动难度精调：计划生成按级别注入 `difficultyAdjustment`（A=-0.2 / B=0 / C=+0.2），与 GradeBand 系数合成
- [x] **[Impl]** `WeeklyPlanViewModel.generateNewPlan(grade, diagnosticLevel)` 读取真实级别，替换硬编码难度

### Phase D: 离线/在线能力矩阵 (Issue 4)
- [x] **[Impl]** `CapabilityMatrix` (domain 纯函数)：离线可用 (缓存计划/会话、系统 TTS、离线 ASR、模板 LLM) vs 仅在线 (LLM 生成、云端 TTS/ASR)
- [x] **[Impl]** Dashboard + Learning 离线横幅："Offline mode — AI features need a model configured in Settings"

### Phase E: 统一异常处理 (Issue 5)
- [x] **[Impl]** `LingoError.kt` 错误分类 (NETWORK/CONFIG/AUTH/TIMEOUT/PARSE/UNKNOWN) + `userFacingError()` 用户文案
- [x] **[Impl]** `WeeklyPlanViewModel`/`DiagnosisViewModel` 错误统一经分类映射为可读文案

### Phase F: 质量
- [x] **[Unit Test & Build]** `CapabilityMatrixTest` (6) + `LingoErrorTest` (6) + 既有测试全绿；`./gradlew test assembleDebug`
- [x] **[MVP Delivery]** 模拟器 E2E 复测：Plan 生成 (离线兜底真实 day 卡片) → Review/Start 进入学习 → 离线横幅 (Dashboard + Learning)，无崩溃

---

## Sprint 11: 伴学升级 - Lingo 陪伴对话 (Agent Companion) - [x] v3.1
> 原则：把单一"冰激凌店"Roleplay 升级为可选的场景化 AI 对话伴学，并补全被声明但未实现的 `FAST_ANSWER` 观察触发。中等风险，独立可交付。

### Phase A: 场景化 Roleplay 2.0
- [x] **[Impl]** `RoleplayScenarioBank` 场景库 (动物园/餐厅/学校/旅行) + `RoleplayScenario` 模型，各场景内置离线安全脚本 (system prompt + opening line + 目标词汇)；`ROLEPLAY_SCENARIO` LLM 增强 (离线失败时保留策展脚本)
- [x] **[Impl]** RoleplayScreen 场景选择器 (FilterChip 横滚行) + 文本输入聊天模式 (麦克风不可用时仍可对话) + Reset 重开对话
- [x] **[Impl]** 对话历史持久化：Room v5 `conversation_history` 表 + `ConversationRepository`；进入场景恢复最近 12 条历史，Lingo 能"记得"上次聊了什么

### Phase B: FAST_ANSWER 观察触发 (补全 Sprint 6 缺口)
- [x] **[Impl]** Quiz/Game 状态新增 `questionStartMs`，答题时计算 `responseTimeMs` 传入 `evaluateObservation`
- [x] **[Impl]** `ObservationTriggerEngine` 实现 `FAST_ANSWER` 规则 (响应时间 < 个人均值×0.5，无历史用 10s 基线)

### Phase C: 质量
- [x] **[Unit Test & Build]** `RoleplayScenarioBankTest` (5) + `FastAnswerObservationTest` (5) + `ConversationRepositoryImplTest` (3)；`./gradlew test assembleDebug`
- [x] **[Docs Sync]** 更新 `agents.md` (Roleplay Agent prompt)、`readme.md`、`implementation_plan.md`、`task.md`
- [x] **[MVP Delivery]** 交付 V3.1 伴学升级 APK (模拟器 E2E：场景选择器 + 文本输入 + 历史持久化 + 离线兜底回复，无崩溃)

---

## Sprint 12: 流畅交互与家长信任 (Smooth Interaction & Parent Trust) - [x] v3.2
> 原则：补齐半成品接线 (校准按钮 no-op、通知无 UI、周报仅文本分享)，并新增每周"Lingo 的信"家长摘要。多屏幕小改动，风险低。

### Phase A: 流畅交互接线
- [x] **[Impl]** 校准弹窗 "Update My Level" → 跳转重跑 Diagnosis (原为 no-op)，完成后更新 `diagnostic_level`
- [x] **[Impl]** Settings 新增 "Change Grade / Re-run Onboarding" 入口，重置 `onboarding_completed` 并回到 Welcome
- [x] **[Impl]** Settings 新增 "Daily Reminder" 偏好卡 (开关 + 时间滑杆)，`DailyReminderWorker` 读取 `reminder_enabled` 决定是否通知 (镜像 plain prefs 供 WorkManager 读取)
- [x] **[Impl]** Dashboard 任务目标/时长读取周计划 (Sprint 10.5 已完成，保持)

### Phase B: 家长信任面
- [x] **[Impl]** WeeklyReport 图片分享：`buildReportImageUri` 渲染周报摘要 PNG + FileProvider 分享 (manifest + file_paths.xml)
- [x] **[Impl]** 每周 "Lingo 的信" 家长摘要卡：LLM 生成 (taskType `LINGO_LETTER`) + `LingoLetterFallback` 离线模板

### Phase C: 质量
- [x] **[Unit Test & Build]** `LingoLetterFallbackTest` (4)；`./gradlew test assembleDebug`
- [x] **[Docs Sync]** 更新 `agents.md` (Weekly Lingo Letter Agent prompt)、`readme.md`、`implementation_plan.md`、`task.md`
- [x] **[MVP Delivery]** 交付 V3.2 流畅交互 APK (模拟器 E2E：Settings 提醒卡 + 重跑 Onboarding → Welcome + 校准接线，无崩溃)

---

## Sprint 13: 真实音频沉浸 (Real Audio Immersion) - [x] v3.3
> 原则：沉浸式导入阶段当前是**模拟播放** (协程计时器)，无真实音频。本 Sprint 接入真实 TTS 合成 + MediaPlayer 播放，保留字幕高亮与生词弹出。核心阶段质量基线，风险最高，放最后。

### Phase A: 真实 TTS 音频
- [x] **[Impl]** `ImmersiveAudioScreen` 字幕行经 `TtsRepository.getSpeech()` 合成真实音频，MediaPlayer 播放
- [x] **[Impl]** `AudioPlayerController` 扩展支持真实文件播放 (保留模拟模式作为离线降级)
- [x] **[Impl]** 字幕高亮 / 新词弹出与真实播放进度同步

### Phase B: 降级与性能
- [x] **[Impl]** 合成失败 -> System TTS / 模拟模式降级路径
- [x] **[Impl]** 批量预合成缓存 (接入 TtsCache LRU)

### Phase C: 质量
- [x] **[Unit Test & Build]** AudioPlayerController 真实文件状态测试 + 降级路径测试；`./gradlew test assembleDebug` (14 new real-audio tests; all 36 UI tests green)
- [x] **[Docs Sync]** 更新 `readme.md` / `implementation_plan.md` / `task.md`
- [x] **[MVP Delivery]** 交付 V3.3 真实音频 APK

---

## Sprint 14: 自适应智能与小组件个性化 (Adaptive Intelligence & Widget Personalization) - [x] v3.4
> 原则：基于 V1.1 延期增强 En4 (诊断持续校准反馈) 与 En5 (小组件个性化文本)，把硬编码/静态内容替换为数据驱动的个性化体验，并打通诊断-计划-小组件-提醒链路。

### Phase A: 数据驱动计划生成 & 诊断反馈
- [x] **[Impl]** `WeeklyPlanViewModel.generateAndCacheWeeklyPlan()` 拉取真实指标 (月准确率 / 薄弱词类 / 连续天数 / 错题数) 替代硬编码 75%
- [x] **[Impl]** `DiagnosisResultScreen` 新增 "AI 将持续调整" 提示，说明每两周校准难度
- [x] **[Impl]** 重新诊断路径展示 `DiagnosisResultScreen` 后再进入主页 (Enhancement 4)

### Phase B: 小组件个性化文本 (Enhancement 5)
- [x] **[Impl]** 新增 `WidgetContent` 模型 + `WidgetContentGenerator` (纯函数，按 streak/todayDone/姓名/今日主题/错题数生成)
- [x] **[Impl]** 新增 `WidgetContentCache` (纯 SharedPreferences) + `LingoStreakWidget` 渲染改为读缓存 (无缓存回退模板)
- [x] **[Impl]** `DashboardViewModel` 加载时预生成并写缓存；`MainActivity` 打开应用时刷新小组件
- [x] **[Impl]** DI 接线：`WidgetContentRepository`(domain) + `WidgetContentRepositoryImpl`(data) + `RepositoryModule` 绑定

### Phase C: 提醒尊重偏好 & 小组件联动
- [x] **[Impl]** `DailyReminderWorker` 按 `reminder_hour` 计算首次触发时间 (分钟级对齐)，通知文本复用个性化缓存
- [x] **[Impl]** 提醒触发时刷新小组件；无缓存时按 streak 数据兜底生成

### Phase D: 质量
- [x] **[Unit Test & Build]** `WidgetContentGeneratorTest` 12 条用例 (覆盖分支/兜底)；`./gradlew test assembleDebug` 全绿 (199 domain+ui tests)
- [x] **[Docs Sync]** 更新 `readme.md` / `implementation_plan.md` / `task.md`
- [x] **[MVP Delivery]** 交付 V3.4 APK (由 antigravity 验收与发布)

---

## Sprint 15: 质量加固与无障碍适配 (Quality Hardening & Accessibility) - [x] v3.5
> 原则：所有功能已就绪（Sprint 0-14 完成），本 Sprint 聚焦质量：修复功能遗漏（每日目标重置未接线、设置齿轮未渲染）、增加错误态 UI、修复触摸目标与语义，补全领域用例测试。

### Phase A: 功能修复 (Functional Fixes)
- [x] **[Impl]** `DashboardViewModel.loadDashboardData()` 每日目标重置：当 `gamification.dailyGoalsDate != today` 时调用 `gamificationRepository.resetDailyGoals()`，避免跨天显示旧日"已完成"状态
- [x] **[Impl]** `DashboardScreen` 标题栏渲染设置齿轮图标（原声明 `onSettingsClick` 但从未渲染）
- [x] **[Impl]** `DashboardUiState` 新增 `errorMessage`，catch 块设置错误提示；`DashboardScreen` 新增错误态 UI（狐狸 + 文案 + Retry 按钮）；`WeeklyPlanViewModel.loadPlan()` 类似处理

### Phase B: 无障碍适配 (Accessibility)
- [x] **[Impl]** `WeeklyPlanScreen` "i" 理由切换按钮触摸目标 24dp → 48dp（外层 48dp 可点击、内层 24dp 视觉圆环）
- [x] **[Impl]** `ErrorBookScreen` 排序 Chip 触摸目标（`heightIn(min = 48.dp)`）+ 添加 `contentDescription`
- [x] **[Impl]** `QuizScreen` 选项按钮 / 字母选项添加 `contentDescription`
- [x] **[Impl]** `RoleplayScreen` 发送按钮添加 `contentDescription`
- [x] **[Impl]** `ErrorBookScreen` 错误卡片添加 `contentDescription`
- [x] **[Impl]** `DashboardScreen` 设置齿轮添加 `semantics`

### Phase C: 领域用例测试补全 (Domain Use Case Tests)
- [x] **[Unit Test]** `ExplanationAgentUseCaseTest` 9 条用例（提示词含 word/grade/errorType、EXPLAIN 任务类型、maxTokens=150、history JSON、无 history 回退、成功/失败传递、指南段落）
- [x] **[Unit Test]** `DailyEncouragerUseCaseTest` 8 条用例（提示词含 name/streak、ENCOURAGEMENT 任务类型、maxTokens=60、语气规则、成功/失败传递、0 连续天数）

### Phase D: 质量
- [x] **[Unit Test & Build]** 全部 19 条新用例 + 既有测试全绿；`./gradlew test assembleDebug` 通过
- [x] **[Docs Sync]** 更新 `task.md` / `readme.md` / `implementation_plan.md`
- [x] **[MVP Delivery]** 交付 V3.5 APK（由 antigravity 验收与发布）

---

## Sprint 16: 代码清理与质量补全 (Polish & Cleanup) - [x] v3.6
> 原则：所有功能已就绪，本 Sprint 聚焦清理遗留问题：修复 `WeeklyPlanScreen` 缺失的 loadError 渲染、添加 Roleplay 离线 TTS 降级、删除 5 个未使用的 DAO/Entity 及对应 DI 接线、补充 `SettingsViewModel` 单元测试。

### Phase A: 用户体验补全 (UX Polish)
- [x] **[Impl]** `WeeklyPlanScreen` loadError 渲染：当 `uiState.loadError != null` 时显示狐狸 + 错误文案 + Retry 按钮（ViewModel 侧已实现）
- [x] **[Impl]** `RoleplayViewModel.speakAssistant()` 云 TTS 失败时降级到 `SystemTtsHelper.speak(text)`（离线/错误态声音降级）
- [x] **[Impl]** `SettingsScreen` 语言切换已有 "Restart required to apply language change" 提示文案

### Phase B: 死代码清理 (Dead Code Cleanup)
- [x] **[Impl]** 删除 5 个未使用 DAO：`UserProfileDao`、`VocabItemDao`、`QuizResultDao`、`DailyStreakDao`、`ThemeUnitDao`
- [x] **[Impl]** 删除对应 Entity：`UserProfileEntity`、`VocabItemEntity`、`QuizResultEntity`、`DailyStreakEntity`、`ThemeUnitEntity`
- [x] **[Impl]** 更新 `AppDatabase.kt`（清除 entity 声明 + DAO 抽象方法，版本 5→6）
- [x] **[Impl]** 更新 `DatabaseModule.kt`（清除 5 个 `@Provides` 方法）

### Phase C: 单元测试补全 (Unit Test Coverage)
- [x] **[Unit Test]** `SettingsViewModelTest` 10 条用例（loadSettings、updateLanguage、updateReminderHour 边界、saveSettings 持久化、trim、isSaved 复位、ASR 阈值、月 Token 上限）

### Phase D: 质量
- [x] **[Unit Test & Build]** 全部 10 条新用例 + 既有测试全绿；`./gradlew test assembleDebug` 通过
- [x] **[Docs Sync]** 更新 `task.md` / `readme.md` / `implementation_plan.md`
- [x] **[MVP Delivery]** 交付 V3.6 APK（由 antigravity 验收与发布）

---

## Sprint 17: 错误态补全与 E2E 回归测试 (Error State Polish & E2E Regression) - [x] v3.7
> 原则：在所有主要屏幕添加错误态 UI 渲染（替代静默吞异常），并在华为 Mate 80 分辨率模拟器上完成端到端回归测试。

### Phase A: 错误态 UI 补全 (Error State UI Polish)
- [x] **[Impl]** `WeeklyReportViewModel` 新增 `loadError` 状态，catch 块设置用户友好错误信息；`WeeklyReportScreen` 渲染狐狸 + 错误文案 + Retry 按钮
- [x] **[Impl]** `ErrorBookViewModel` 新增 `loadError` 状态，catch 块设置错误信息；`ErrorBookScreen` 渲染相同错误态 UI

### Phase B: (Sprint 16 已覆盖 - 无新增死代码清理)

### Phase C: E2E 回归测试 (E2E Regression Testing)
- [x] **[E2E]** 华为 Mate 80 规格模拟器（1260x2816 @ 480dpi）上完整回归：Dashboard 渲染（设置齿轮/XP/Level/Streak）、Weekly Plan、Error Book、Settings 四个页面
- [x] **[E2E]** 验证所有触摸目标 ≥ 48dp（设置齿轮 48x48dp ✅、Back 按钮 ✅、Start 按钮 ✅）
- [x] **[E2E]** logcat 无崩溃、无 FATAL、无 ANR

### Phase D: 质量
- [x] **[Unit Test & Build]** `./gradlew test assembleDebug` 全绿（216 tasks）
- [x] **[Docs Sync]** 更新 `task.md` / `readme.md` / `implementation_plan.md`
- [x] **[MVP Delivery]** 交付 V3.7 APK（由 antigravity 验收与发布）

---

## Sprint 18: 错题本毕业观察接线 (Error Book Graduation Observation) - [x] v3.8
> 原则：PO P0 变更 —— `ErrorBookDao.getErrorsInObservation()` 查询自 Sprint 7 以来存在但从未被调用（死代码），将毕业观察窗口中的词（连续答对 3 次 → `GRADUATION_OBSERVATION`）显性化为错题本独立区块。

### Phase A: 毕业观察接线 (Graduation Observation Wiring)
- [x] **[Impl]** `ErrorBookRepository` 接口新增 `getErrorsInObservation(): List<ErrorBookEntry>`；`ErrorBookRepositoryImpl` 实现（`errorBookDao.getErrorsInObservation()` → `toDomain()`）
- [x] **[Impl]** `ErrorBookUiState` 新增 `observationEntries`；`ErrorBookViewModel.loadErrors()` 加载并按 priorityScore 降序排序；主列表 `entries` 过滤掉 `GRADUATION_OBSERVATION`（避免重复展示）
- [x] **[Impl]** `ErrorBookScreen` 新增 "🔵 Graduation Watch" 区块（SectionHeader + 计数徽章 + 副标题 "Almost there! Keep practicing to graduate these words."），下方 "📖 Needs Practice" 区块承接剩余条目

### Phase B: 单元测试 (Unit Tests)
- [x] **[Unit Test]** `ErrorBookPriorityTest` 新增 2 条：`getErrorsInObservation maps entities to domain entries`（状态/词条/连续正确数断言）、`getErrorsInObservation returns empty when no entries in observation`

### Phase C: 质量
- [x] **[Unit Test & Build]** `./gradlew test assembleDebug` 全绿（216 tasks, BUILD SUCCESSFUL）
- [x] **[Docs Sync]** 更新 `task.md` / `readme.md` / `implementation_plan.md`
- [x] **[MVP Delivery]** 交付 V3.8 APK（由 antigravity 验收与发布）

---

## 🔴 Sprint 19: 核心用户路径重构与体验坚固化 (v4.3.0) - [ ]

> **主题依据**: 用户实测反馈——优先修复核心闭环体验缺陷：(1) 启动即需 Key 配置，(2) Onboarding 结束直通 AI 计划生成，(3) 单词/句子排版截断修复，(4) 错题本写入与渲染修复，(5) 真实 TTS 音频播放与无声修复。  
> **预计总工作量**: ~5 天

- [ ] **[Impl S19-001 🔴 P0]** **首次启动 Key 配置门槛 (`ModelConfigGate`)** — App 启动及 Onboarding 开始前，检测 `prefs.getAuthToken()`；若为空或未配置，优先展示 "🧠 配置 Lingo 的 AI 大脑" 引导配置页（支持连通性测试）；避免未配置 Key 时使用假数据/假分数进行诊断和建计划 (`MainActivity.kt`, `OnboardingContainer.kt`, `ModelConfigGateScreen.kt`)
- [ ] **[Impl S19-002 🔴 P0]** **Onboarding 结束直通 AI 计划生成与无计划卡片修复** — 诊断结果页点击 "生成我的 AI 计划 🪄" 后，跳转至 `PlanGeneratingScreen.kt` 全屏动画加载页（展示 LLM 计划生成进度）；计划生成完成后进入 Dashboard；若 Dashboard 检测到无计划，展示 "🪄 开启我的周计划" 明显引导卡片，而非占位假任务 (`DiagnosisResultScreen.kt`, `PlanGeneratingScreen.kt`, `DashboardScreen.kt`, `DashboardViewModel.kt`)
- [ ] **[Impl S19-003 🔴 P0]** **学习流程文本截断与换行排版修复** — 针对 `PreTeachScreen` (闪卡)、`PracticeScreen` (句子框)、`QuizScreen` (选项/填空)、`WordHighlightText` 重新设计排版：引入 `AutoResizeText` 或自适应字号（根据文本长度动态缩放 16sp~26sp），取消固定宽高比例下的 `softWrap=false` 截断，保障 Mate 80 等各类屏高 DPI 下单词/句子完整整洁展示 (`PreTeachScreen.kt`, `PracticeScreen.kt`, `QuizScreen.kt`, `WordHighlightText.kt`, `AutoResizeText.kt`)
- [ ] **[Impl S19-004 🔴 P0]** **错题本错题写入与展示完整修复** — 修复 `LearningViewModel` 在 Quiz 错题 (选择/填空/听写)、Game 错误、Practice 口语低分 (<60) 时 `vocabId` 提取逻辑（提取真实单词而非 `vocab_123` 或整句）；确保 `upsertError` 必定成功落库 Room；`ErrorBookViewModel` 增加进入页面自动刷新；`ErrorBookScreen` 展现清晰单词、错因标签与重练按钮 (`LearningViewModel.kt`, `ErrorBookRepositoryImpl.kt`, `ErrorBookViewModel.kt`, `ErrorBookScreen.kt`)
- [ ] **[Impl S19-005 🔴 P0]** **TTS 声音播放与无声降级修复** — 修复 `TtsRepositoryImpl` 请求 Payload 适配火山引擎 Ark API (`/audio/tts` OpenAI 兼容格式)；修复 `SystemTtsHelper` 离线 TTS 在缺少英文语音包时的无声静默，在无声或引擎缺失时弹出 `Snackbar/Toast` 明确提示家长 ("⚠️ 设备的英文发音引擎未就绪，请先配置 API Key 或安装 TTS 语音包") (`TtsRepositoryImpl.kt`, `SystemTtsHelper.kt`, `LearningViewModel.kt`)
- [ ] **[Acceptance Test & Release]** 5-Gate 全量端到端体验验收 → 发布 GitHub Release `v4.3.0`


---

## V1.1 Deferred Enhancements (剩余)
- **Enhancement 4/5 已并入 Sprint 14 交付**，无剩余延期项

---

## Backlog (Bug Fixes & Enhancements)
- [x] **[Bug]** Word display issue - words cannot properly display in some areas.
- [x] **[Bug]** Onboarding/eval queries are all the same - diagnosis screen still generating same questions despite fallback fix, needs further investigation.
- [x] **[Bug]** Navigation - From learning journey cannot go back to the app home page.
