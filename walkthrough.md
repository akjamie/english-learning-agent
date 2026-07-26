# 开发审计与结构记录 (Walkthrough - Sprint 0)

> 本文档用于记录 **Sprint 0：项目初始化与基础设施** 的开发产出，包含了文件目录树、各模块核心组件功能以及代码结构。

---

## 📂 项目多模块物理目录树

我们按照 **Clean Architecture (整洁架构)** 规范对项目结构进行了模块化隔离，在工作区创建了如下的 Android/Gradle 目录骨架：

```
english-learning-agent/
├── gradle/
│   └── libs.versions.toml       # Version Catalog (版本和依赖集中管理)
├── build.gradle.kts             # 根目录构建脚本
├── settings.gradle.kts          # 注册四个子模块 (:app, :ui, :data, :domain)
├── gradle.properties            # 全局编译参数配置
│
├── domain/                      # 业务逻辑与领域模型 (纯 Kotlin 模块，无 Android 依赖)
│   ├── build.gradle.kts
│   └── src/main/java/com/lingo/learn/domain/
│       ├── model/               # 领域模型 (UserProfile, VocabItem, ErrorBookEntry, etc.)
│       └── repository/          # Repository 仓储接口 (LlmRepository, TtsRepository, etc.)
│
├── data/                        # 数据源与网络层 (Android Library，依赖 :domain)
│   ├── build.gradle.kts
│   └── src/main/java/com/lingo/learn/data/
│       ├── local/
│       │   ├── AppDatabase.kt   # Room 数据库配置 (版本 1)
│       │   ├── converter/       # TypeConverters (AppTypeConverters.kt JSON 序列化)
│       │   ├── dao/             # Room DAO (UserProfileDao, ErrorBookDao, etc.)
│       │   └── entity/          # Room Entity 映射表 (UserProfileEntity, etc.)
│       ├── prefs/
│       │   └── SecureConfigPrefs.kt  # AES256 本地加密 SharedPreferences 存储
│       ├── remote/
│       │   └── minimax/         # MiniMax API (Retrofit 接口与数据类模型)
│       ├── repository/          # Repository 仓储实现类 (LlmRepositoryImpl, etc.)
│       └── di/                  # Hilt 依赖注入模块 (NetworkModule, DatabaseModule, RepositoryModule)
│
├── ui/                          # Compose UI 与界面表现层 (Android Library，依赖 :domain)
│   └── build.gradle.kts
│
└── app/                         # 宿主应用入口 (Android Application，依赖其他三层)
    ├── build.gradle.kts
    └── src/main/
        ├── AndroidManifest.xml  # 应用 Manifest (网络权限 + 录音权限)
        └── java/com/lingo/learn/
            ├── LingoApplication.kt  # Application Hilt 入口
            └── MainActivity.kt      # 主 Activity 入口
```

---

## 🛠️ 各子模块构建与依赖详情

1. **`:domain` (业务与领域层)**
   - 依赖情况：仅依赖官方 `javax.inject:javax.inject:1` 以声明注解，不带任何 Android 框架、Retrofit 或 Room 依赖。
   - 优势：绝对解耦，数据结构极度纯净，可直接打包复用于 HarmonyOS (鸿蒙) 或 Desktop 等多平台。
2. **`:data` (数据与连接层)**
   - 依赖情况：依赖 `:domain` 模块，同时引入 Room (2.6.1)、Hilt (2.50)、Retrofit (2.9.0) + OkHttp (4.12.0)、Gson 以及 Android Jetpack Security。
3. **`:ui` (表现层)**
   - 依赖情况：依赖 `:domain` 模块，启用 Jetpack Compose 并配置兼容 Kotlin 1.9.22 的 Compose Compiler `1.5.8`。
4. **`:app` (宿主装配层)**
   - 依赖情况：依赖 `:domain`, `:data`, `:ui`，承载 Application 的 Hilt 构建逻辑和首屏 MainActivity 路由。

---

## 💾 1. 本地 Room 数据库设计与映射

共建立了 **10 张表** 及其对应的本地 Entity 与 10 个 DAO 数据库访问接口，用于实现 **Local-First (本地优先)** 的持久化存储：

| 数据表 (`TableName`) | Entity 类 | DAO 接口 | 核心作用及转换关系 |
|---|---|---|---|
| `user_profile` | `UserProfileEntity` | `UserProfileDao` | 存储孩子档案信息，支持与 domain `UserProfile` 互转 |
| `vocab_item` | `VocabItemEntity` | `VocabItemDao` | 存储内置/更新词汇主表，含音标、例句及 TTS 音频本地路径 |
| `error_book` | `ErrorBookEntity` | `ErrorBookDao` | 存储错题本数据，含答错题型、错误次数权重及 3D 卡片历史 |
| `plan` | `PlanEntity` | `PlanDao` | 存储周/月计划快照，以及 Agent 周末生成的短对话 JSON |
| `learning_record` | `LearningRecordEntity` | `LearningRecordDao` | 存储每天具体环节的学习耗时、正确率和 Streak 打卡数 |
| `quiz_result` | `QuizResultEntity` | `QuizResultDao` | 存储每日 micro-quiz / 周测 / 诊断测的详细得分与答错 ID |
| `token_usage_log` | `TokenUsageLogEntity` | `TokenUsageLogDao` | 存储 LLM 调用的 Token 记账日志，支持按月份查询总消耗 |
| `tts_cache` | `TtsCacheEntity` | `TtsCacheDao` | 存储 TTS 本地 MP3 文件路径映射，用于 LRU 淘汰定位 |
| `daily_streak` | `DailyStreakEntity` | `DailyStreakDao` | 记录每天的打卡状态以及补签卡消耗记录 |
| `theme_unit` | `ThemeUnitEntity` | `ThemeUnitDao` | 存储单元主题库，使用 `AppTypeConverters` 存储 List<String> |

---

## 🔒 2. 安全加密配置层 (SecureConfigPrefs)

我们通过 Hilt 统一向应用注入 `SecureConfigPrefs.kt`，利用 `EncryptedSharedPreferences`（基于 AES256 加密标准）在安卓设备中安全地保存自填式的 MiniMax 凭证和科大讯飞评测密钥：

- **加密保存字段**：`MINIMAX_GROUP_ID`、`MINIMAX_API_KEY` (LLM & TTS 共享)、`ASR_AUTH_APPID`、`ASR_AUTH_SECRET`。
- **系统安全防爆**：在读取时对敏感 Key 提供 Masked 显隐展示支持，明文仅在注入 HTTP Header 调用时使用，内存释放即销毁。

---

## 🌐 3. MiniMax 统一网络通道实现

根据“**使用 MiniMax 统一套餐，不需要引入火山引擎等额外特定 TTS SDK 依赖**”的决定，我们将 LLM 和 TTS 进行了网络层统一合并：

### 接口声明 (`MinimaxService.kt`)
通过 Retrofit 规范了两个核心端点：
1. **统一对话 (Chat Completion V2)**：`POST v1/text/chatcompletion_v2`，支持流式或非流式对话。
2. **语音合成 (t2a_v2)**：`POST v1/t2a_v2`，直接获取二进制音频数据 ResponseBody，写入本地文件。

### 降级与兜底设计 (`LlmRepositoryImpl.kt`)
- **双模型降级**：调用时主模型（例如 `abab6.5s-chat`）超时 (8s) 或返回 429 时，自动降级至备用模型（如 `abab6.5t-chat`）。
- **Token 本地控制**：每次请求前计算本月累计 Token，如超限则触发本地模板兜底；请求成功后通过 `TokenUsageLogDao` 自动记账。

### TTS 缓存与 LRU 淘汰 (`TtsRepositoryImpl.kt`)
- **缓存计算**：缓存文件名根据 `hash(text + speed + voiceId)` 生成，并在数据库 `tts_cache` 建立索引。
- **物理驱逐**：当 `context.cacheDir/tts/` 目录大小超出 **200MB** 限制时，读取最旧的访问条目，同时清除物理文件和数据库映射，保障低端设备的存储空间。

---

## 🏆 Sprint 0 交付审计结果
- **Gradle 编译配置**：**已通过**
- **多模块分层规范**：**已通过**
- **Room 核心数据库物理落盘**：**已通过**
- **三通道 Repository 接口与 Hilt DI 自动装配**：**已通过**

下一步，我们将正式启动 **Sprint 2：每日学习核心流程（听/跟读/Quiz）的 Compose 开发**！

---

## 🎨 4. Onboarding 流程与动效微交互实现 (Sprint 1)

我们通过 **Jetpack Compose UI** 实现了极高品质的 5 屏线性 Onboarding (入学引导) 流程与首屏 Dashboard：

### 🦊 Lingo 陪伴形象与 6 种表情动效 (`LingoAvatar.kt`)
- **图形绘制**：使用 Compose `Canvas` 自定义 Path 动态绘制小狐狸灵灵。无任何外部位图依赖，矢量无缝缩放，极低渲染开销。
- **微动呼吸**：采用 `InfiniteTransition` 结合 `EaseInOutQuad` 插值器，实现了 1200ms 的面部上下起伏呼吸动效。
- **动态表情切换**：利用 `updateTransition` 管理 `HAPPY`, `CELEBRATING`, `SAD`, `THINKING`, `SLEEPY`, `EXCITED` 表情间的插值切换。例如当答对题目时自动过渡到眯眼大笑（`CELEBRATING`）并伴随 `EaseOutBounce` 的 400ms 垂直跳动动效。

### 📲 线性引导与左右滑屏物理过渡 (`OnboardingContainer.kt`)
- **状态机控制**：使用单状态机管理 `Welcome` $\rightarrow$ `GradeSelect` $\rightarrow$ `TextbookConfirm` $\rightarrow$ `Diagnosis` $\rightarrow$ `DiagnosisResult` 步骤流动。
- **物理滑屏动效**：使用 Compose `AnimatedContent` 判断当前跳转的 ordinal 方向，自动匹配相应的滑动过渡：
  - **前进**：`slideInHorizontally { width -> width }` (新页面从右滑入，老页面向左滑出)
  - **后退**：`slideInHorizontally { width -> -width }` (新页面从左滑入，老页面向右滑出)

### 📊 年级选择 Spring 阻尼动效 (`GradeSelectScreen.kt`)
- **二级网格展开**：点击三大阶段卡片（小学/初中/高中）后，子年级选项采用 `expandVertically(spring(stiffness = Spring.StiffnessLow))` 弹性自然向下铺展。
- **触觉与视觉反馈**：
  - 点击时应用 `HapticFeedbackType.LongPress`（长震动）和 `TextHandleMove`（轻量位移动作）。
  - 选中卡片自动呈现 `1.04f` 的 Scale Bounce 膨胀，未选中卡片下压降为 `0.96f` 比例且透明度降低，操作感知非常强烈。

### 📝 5题自适应诊断测验 (`DiagnosisScreen.kt`)
- **答题闭环**：支持听力单选、反义词选择、自然拼读辨析、单词拖拽排序、跟读录音打分 5 类测试。
- **拟真反馈**：跟读测试中，用户长按麦克风触发 `isRecording`，图标自带缩放膨胀，松开后自动进行模拟跟读评分，达标后 Lingo 转为庆祝跳跃状态。

---

## 🏠 5. 首页 Dashboard 极简化界面与视觉 Token 热切换

首页 Dashboard 连通了学情数据，并应用了年级段的**视觉 Token 自动热切换系统** (`DashboardScreen.kt`)：

- **今日任务卡**：以大卡片展示“🎯 今日学习任务”，使用 `ProgressRing` 绘制圆环进度条，支持基于 `animateFloatAsState` 的 800ms 顺时针扇形渲染。
- **打卡计数器**：橘红色 Streak 火焰 Emoji，包含 1000ms 的正弦抖动呼吸动效，milestone (7/30天) 自动渲染彩虹亮色背景。
- **视觉主题 Token 热切换**：
  - 首页根据传入的年级段自动调用 `getThemeForGrade(grade)` 匹配视觉配色。
  - **小学段**：马卡龙黄主题色 (`Color(0xFFFFD449)`) 与暖黄背景，调性活泼。
  - **初中段**：蓝紫科技感主题色 (`Color(0xFF5C6FF2)`) 与淡蓝背景，调性理智。
  - **高中段**：深蓝金色主题色 (`Color(0xFF1C2F5E)`) 与灰色背景，专业高效。
  - 主题色会自动灌入 `ProgressRing` 前景色与“开始学习”主按钮中，完成一键全屏主题渲染。

---

## 🏆 Sprint 1 交付审计结果
- **Onboarding 流线性滑屏**：**已通过** (MainActivity.kt 连接无缝)
- **多学段主题 Token 热切换**：**已通过**
- **Lingo Mascot 自定义 Canvas 微动效**：**已通过**
- **Streak 7/30天计数组件与 800ms 进度圆环**：**已通过**
- **ASR & TTS & LLM 统一鉴权热配置底座**：**已通过** (移除科大讯飞，实现全渠道共享鉴权)

---

## ⚡ 6. ASR 统一服务底座与发音评估逻辑重构

为了精简技术栈，我们完全移除了科大讯飞依赖，使整个应用基于 **MiniMax 统一接口底座** 进行 ASR、TTS 与 LLMs 的多端调用：

### 🔑 统一热配置设计 (`SecureConfigPrefs.kt` & `NetworkModule.kt`)
- **字段合并**：移除了原有的科大讯飞 AppID/Secret 等冗余字段，取而代之的是全局共享的 `BASE_URL` (支持自填以适应内网或反向代理网关) 以及统一的鉴权 `AUTH_TOKEN` (即 API 密钥) 和 `GROUP_ID`。
- **接口动态化**：通过 Retrofit 的 `@Url` 注解，重写了 `MinimaxService.kt` 中的全部请求函数。这允许在运行时直接从本地加密配置动态组装请求端点（如 `${BASE_URL}/v1/audio_to_text`），避免了硬编码问题，且无需重启服务或重新构建 Hilt 注入项。

### 🎙️ Multipart 语音识别与 Levenshtein 评估算法 (`AsrRepositoryImpl.kt` & `MinimaxModel.kt`)
- **文件直传**：新增 `MinimaxAsrResponse` 等 ASR 服务专用数据模型。录音文件直接通过 `MultipartBody.Part` 上传至 MiniMax 语音识别接口（默认使用 `asr-01` 语音识别模型）。
- **编辑距离相似度打分**：语音转文本后，在客户端使用 **Levenshtein 距离算法** 实现口语比对：
  - 自动对识别文本与标准 reference 文本执行预处理（转化为全小写、剔除所有标点符号）。
  - 动态计算两串文本的编辑距离，依公式 $Similarity = (1 - \frac{Distance}{MaxLength}) \times 100$ 将相似度映射为百分制分数。
  - 接入本地设置中的宽松评分阈值（默认 60 分）。若得分低于达标阈值，自动触发温和鼓励策略，为儿童提供 60 分的最低达标“阳光分”保障。
  - 逐字执行编辑距离模糊搜索（距离 $\le 1$ 判定为读对），生成词级发音正确性映射（`WordScore`），并在弱网或配置缺失时自动平滑降级至 Offline 离线模式，保障儿童的口语学习信心。
