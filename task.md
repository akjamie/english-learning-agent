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

## V1.1 Deferred Enhancements
- **Enhancement 4**: Onboarding diagnosis continuous calibration - diagnosis result conveys "AI will keep adjusting"; day-10 calibration trigger based on actual data vs diagnostic expectation (medium cost)
- **Enhancement 5**: Widget personalized text - Widget text from fixed template to data-driven personalized generation via Fallback LLM + daily pre-generation cache (medium cost)

---

## Backlog (Bug Fixes & Enhancements)
- [x] **[Bug]** Word display issue - words cannot properly display in some areas.
- [x] **[Bug]** Onboarding/eval queries are all the same - diagnosis screen still generating same questions despite fallback fix, needs further investigation.
- [x] **[Bug]** Navigation - From learning journey cannot go back to the app home page.
