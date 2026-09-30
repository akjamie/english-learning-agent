# Independent Review: refresh-learning-experience

> Reviewer agent (fresh context, independent). Read-only review of proposal, spec, design, tasks, configuration, and relevant current code.

## Verdict

APPROVE

## Dimension Verdicts

| Dimension | Verdict | Notes |
|---|---|---|
| Completeness | PASS | The change covers onboarding, Home and its four destinations, daily activities, vocabulary/error-book, Plan, Progress, roleplay, recovery, and target-display regression. The two gaps from the previous review now have explicit Home navigation AC2 and bounds-measured clipping AC2. |
| Ambiguity | PASS | BDD scenarios use Given/When/Then. The PDF is the visual reference and existing behavior is the behavioral reference. The profile is explicitly Android-only, uses the D-drive SDK/AVD storage, and targets portrait 1280×2832 without claiming Huawei hardware or HarmonyOS coverage. |
| Cross-repo consistency | PASS | The proposal explicitly leaves `home-experience@v1` unchanged and declares no modified capability contracts. The scope is local UI/navigation presentation and current app state. |
| Contract impact | PASS | The design reuses existing ViewModels, repositories, persistence, and handlers; it introduces no API, persistence, scoring, retry, skip, offline-capability, or AI decision changes. |
| Testability | PASS | Every one of the 20 acceptance criteria names a test path. Tasks prescribe controlled Compose state and semantics, per-scenario vertical TDD, failure-before-implementation checks, and running the affected suite. The Mate 80 criterion measures each required action's `boundsInRoot` against the root viewport after scrolling into view where needed. The existing `:ui` configuration lacks Compose UI test dependencies, but task 1.1 explicitly adds only the necessary support. |
| Security | PASS | No authentication or new data/service behavior is introduced. Onboarding explicitly excludes sign-in, while permission recovery retains existing system-settings handling. |

## Requirement -> Implementation -> Test

| Requirement / AC | Implementation | Test |
|---|---|---|
| Onboarding entry / AC1 | Refresh welcome UI; retain Start into existing onboarding progression and omit sign-in (tasks 2.1–2.2) | `ui/src/androidTest/java/com/lingo/learn/ui/onboarding/WelcomeExperienceTest.kt` |
| Home and lesson navigation / AC1 | Render current learner, streak, XP, lesson, completion, and route continue to the saved lesson (tasks 3.1–3.2) | `ui/src/androidTest/java/com/lingo/learn/ui/dashboard/HomeExperienceTest.kt` |
| Home and lesson navigation / AC2 | Connect Start lesson, Plan, Practice, and Progress to their matching destinations (tasks 3.1–3.2) | `ui/src/androidTest/java/com/lingo/learn/ui/dashboard/HomeExperienceTest.kt` |
| Daily lesson activities / AC1 | Render active vocabulary question, existing correctness feedback, and progression (tasks 5.1–5.2) | `ui/src/androidTest/java/com/lingo/learn/ui/learning/LessonExperienceTest.kt` |
| Daily lesson activities / AC2 | Render existing pronunciation score and word feedback; Continue follows existing lesson state (tasks 5.3–5.4) | `ui/src/androidTest/java/com/lingo/learn/ui/learning/LessonExperienceTest.kt` |
| Daily lesson activities / AC3 | Render existing quiz feedback and advance through existing quiz state (tasks 5.5–5.6) | `ui/src/androidTest/java/com/lingo/learn/ui/learning/LessonExperienceTest.kt` |
| Vocabulary and error-book review / AC1 | Render saved entries and matching detail/history; retain explanation, review, and retry actions (tasks 4.1–4.2) | `ui/src/androidTest/java/com/lingo/learn/ui/errorbook/VocabularyExperienceTest.kt` |
| Plan and progress review / AC1 | Render saved plan days/completion and route day selection to existing lesson journey (tasks 6.1–6.2) | `ui/src/androidTest/java/com/lingo/learn/ui/progress/PlanAndProgressExperienceTest.kt` |
| Plan and progress review / AC2 | Render current learning history, skill values, and error-book summary from current state (tasks 6.3–6.4) | `ui/src/androidTest/java/com/lingo/learn/ui/progress/PlanAndProgressExperienceTest.kt` |
| Roleplay and speaking practice / AC1 | Display typed input and continue the current scenario (tasks 7.1–7.2) | `ui/src/androidTest/java/com/lingo/learn/ui/roleplay/RoleplayExperienceTest.kt` |
| Roleplay and speaking practice / AC2 | Submit spoken input through existing voice interaction while retaining scenario state (tasks 7.3–7.4) | `ui/src/androidTest/java/com/lingo/learn/ui/roleplay/RoleplayExperienceTest.kt` |
| Roleplay and speaking practice / AC3 | Show existing hint without resetting the conversation (tasks 7.5–7.6) | `ui/src/androidTest/java/com/lingo/learn/ui/roleplay/RoleplayExperienceTest.kt` |
| Offline and recoverable error presentation / AC1 | Present continue-offline and retry-connection with existing availability and outcomes (tasks 8.1–8.2) | `ui/src/androidTest/java/com/lingo/learn/ui/recovery/RecoveryExperienceTest.kt` |
| Offline and recoverable error presentation / AC2 | Wire system-settings and continue-without-speaking to existing handlers (tasks 8.3–8.4) | `ui/src/androidTest/java/com/lingo/learn/ui/recovery/RecoveryExperienceTest.kt` |
| Offline and recoverable error presentation / AC3 | Retry using saved recording and preserve existing skip progression/state (tasks 8.5–8.6) | `ui/src/androidTest/java/com/lingo/learn/ui/recovery/RecoveryExperienceTest.kt` |
| Offline and recoverable error presentation / AC4 | Preserve AI-service retry/offline-practice outcomes and saved progress (tasks 8.7–8.8) | `ui/src/androidTest/java/com/lingo/learn/ui/recovery/RecoveryExperienceTest.kt` |
| Offline and recoverable error presentation / AC5 | Preserve regenerate/use-last-plan state and fallback behavior (tasks 8.9–8.10) | `ui/src/androidTest/java/com/lingo/learn/ui/recovery/RecoveryExperienceTest.kt` |
| Offline and recoverable error presentation / AC6 | Preserve safe return, checkpoint, and diagnostic-reference behavior (tasks 8.11–8.12) | `ui/src/androidTest/java/com/lingo/learn/ui/recovery/RecoveryExperienceTest.kt` |
| Huawei Mate 80 display regression / AC1 | Run listed journeys and recovery actions on the custom Android AVD at 1280×2832 using the SDK in `D:\system\android-sdk` (tasks 9.1, 9.3) | `ui/src/androidTest/java/com/lingo/learn/ui/regression/HuaweiMate80RegressionTest.kt` |
| Huawei Mate 80 display regression / AC2 | Measure primary action `boundsInRoot` against the root viewport; scroll required controls into view before asserting bounds (task 9.2) | `ui/src/androidTest/java/com/lingo/learn/ui/regression/HuaweiMate80RegressionTest.kt` |

## Blocking Findings

None.
