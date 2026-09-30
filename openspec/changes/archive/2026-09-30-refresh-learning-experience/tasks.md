# Tasks

<!-- Mutable execution state, generated AFTER approval. Not part of approved intent. -->

## 1. Compose UI test seam
- [x] 1.1 Add the minimal Compose UI test dependencies and reusable instrumentation setup needed to host screens with controlled ViewModel state; verify the UI test source set compiles and a small semantics assertion runs on the configured test target.
- [x] 1.2 Define stable user-visible semantics for refreshed interactive controls as each screen is touched; verify the relevant UI test finds controls by role/label and does not depend on private composable structure.
- [x] 1.3 Document the Compose UI test command and seam conventions in `docs/dev-workflow.md`; verify a developer can follow the documented command to run the onboarding UI test.

## 2. Onboarding welcome
- [x] 2.1 Write the `WelcomeExperienceTest` for a new learner seeing the refreshed welcome page and activating Start; verify it fails against the current presentation before changing production UI.
- [x] 2.2 Refresh the welcome presentation while retaining existing Start and Settings actions and excluding sign-in; verify `WelcomeExperienceTest` passes and existing onboarding progression remains reachable.
- [x] 2.3 Update onboarding documentation in `readme.md`; verify it describes the welcome entry and existing onboarding progression without promising account sign-in.

## 3. Home and lesson navigation
- [x] 3.1 Write `HomeExperienceTest` for current learner/lesson values, saved lesson continuation, and Start lesson/Plan/Practice/Progress destinations; verify each route assertion fails against the current screen before implementation.
- [x] 3.2 Refresh Home and connect its continue/start, Plan, Practice, and Progress actions to their destinations; verify every `HomeExperienceTest` route and live-state assertion passes.
- [x] 3.3 Update the Home journey documentation in `readme.md`; verify the documented routes match the tested actions.

## 4. Vocabulary and error-book review
- [x] 4.1 Write `VocabularyExperienceTest` for opening a saved word and reaching its existing detail/history/explanation/review/retry actions; verify it fails before the screen refresh.
- [x] 4.2 Refresh the vocabulary list and detail presentation using existing entry state and handlers; verify `VocabularyExperienceTest` passes without changing error-book processing behavior.
- [x] 4.3 Update error-book documentation in `readme.md`; verify it describes the tested list-to-detail journey and existing actions.

## 5. Daily learning activities
- [x] 5.1 Write the vocabulary-question case in `LessonExperienceTest`; verify it fails before implementation and asserts existing feedback and advancement through visible state.
- [x] 5.2 Refresh the vocabulary activity presentation; verify the vocabulary-question case passes and existing scoring/progression tests remain green.
- [x] 5.3 Write the read-aloud result case in `LessonExperienceTest`; verify it fails before implementation and asserts the displayed score, word feedback, and Continue action.
- [x] 5.4 Refresh the read-aloud presentation; verify the read-aloud case passes using existing evaluation results and lesson state.
- [x] 5.5 Write the quiz feedback/next-question case in `LessonExperienceTest`; verify it fails before implementation and asserts the learner-visible answer feedback and progression.
- [x] 5.6 Refresh the quiz presentation; verify the quiz case passes without changing answer evaluation.
- [x] 5.7 Update lesson activity documentation in `readme.md`; verify it describes the tested vocabulary, read-aloud, and quiz flow.

## 6. Plan and Progress
- [x] 6.1 Write the Plan case in `PlanAndProgressExperienceTest` for saved days, completion state, and selecting a day; verify it fails against the current presentation.
- [x] 6.2 Refresh Plan using saved plan state and existing navigation; verify the Plan case passes and day selection reaches the existing lesson flow.
- [x] 6.3 Write the Progress case in `PlanAndProgressExperienceTest` for current learning history, skill values, and error-book summary; verify it fails before implementation.
- [x] 6.4 Refresh Progress using current ViewModel/repository data; verify the Progress case passes without hardcoded prototype metrics.
- [x] 6.5 Update Plan and Progress documentation in `readme.md`; verify it matches the tested data sources and available actions.

## 7. Roleplay and speaking practice
- [x] 7.1 Write the typed response case in `RoleplayExperienceTest`; verify it fails before implementation and asserts the submitted response and scenario continuation.
- [x] 7.2 Refresh typed roleplay input presentation; verify the typed response case passes while retaining the current conversation state.
- [x] 7.3 Write the spoken response case in `RoleplayExperienceTest`; verify it fails before implementation and uses a controlled permission/recording outcome.
- [x] 7.4 Refresh spoken roleplay presentation; verify the spoken response case passes through the existing voice interaction.
- [x] 7.5 The approved code-first scope excludes a Roleplay hint because no such behavior exists in the app; no hint UI or behavior was added.
- [x] 7.6 No Roleplay help presentation change was needed; the existing voice and typed paths remain intact and pass `RoleplayExperienceTest`.
- [x] 7.7 Update roleplay documentation in `readme.md`; verify the documented typed and spoken journeys match the tested flow.

## 8. Offline and recovery states
- [x] 8.1 Write the offline-mode case in `RecoveryExperienceTest`; assert the existing local voice/scoring disclosure without adding network retry or lesson selection behavior.
- [x] 8.2 Characterize the existing offline-mode lesson presentation and actions; preserve all current local capability limits.
- [x] 8.3 Write read-aloud failure cases in `RecoveryExperienceTest`; assert the existing microphone retry and skip actions from controlled state.
- [x] 8.4 Verify read-aloud failure keeps the existing retry/skip handlers and never fabricates an evaluation score.
- [x] 8.5 Write the saved-checkpoint case in `RecoveryExperienceTest`; assert the existing Continue and Start Over choices.
- [x] 8.6 Verify checkpoint choices preserve the current saved-progress behavior.
- [x] 8.7 Write the weekly-plan load-error case in `RecoveryExperienceTest`; assert the existing Retry action.
- [x] 8.8 Verify retry invokes the existing plan load handler without replacing saved-plan behavior.
- [x] 8.9 Update offline/recovery documentation in `readme.md`; document only the tested current actions and outcomes.

## 9. Huawei Mate 80 simulator regression
- [x] 9.1 Configure an Android AVD named `HuaweiMate80` with its AVD data at `D:\system\android-sdk\avd\HuaweiMate80.avd` and the standard Mate 80 portrait resolution of 1280×2832; verify `emulator -list-avds` lists it, its registry points to the D-drive AVD data, it boots from the D-drive SDK, and `adb shell wm size` reports the configured resolution.
- [x] 9.2 Write `HuaweiMate80RegressionTest` for the completed journeys and existing recovery states using observable screen behavior; assert each primary visible action's `boundsInRoot` stays within the root viewport after any required scroll, and verify the test fails for a missing journey/action or clipped control.
- [x] 9.3 Run the affected UI instrumentation suite on the booted `HuaweiMate80` AVD and capture the test report; verify no navigation, clipping, crash, or recovery-outcome regressions across onboarding, Home, lesson activities, vocabulary, Plan, Progress, roleplay, and recovery.
- [x] 9.4 Document the D-drive AVD setup, resolution, and regression command in `docs/dev-workflow.md`; verify the documented procedure boots `HuaweiMate80` and runs the same suite.
