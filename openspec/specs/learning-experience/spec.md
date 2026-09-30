# learning-experience Specification

## Purpose
Define a coherent, child-friendly experience matching the supplied prototype across onboarding, daily learning, planning, practice, progress, and recovery, while preserving established learning and error-processing behavior.

## Requirements

### Requirement: Onboarding entry
The app MUST present the prototype-aligned onboarding welcome experience to a new learner and preserve the existing onboarding progression when the learner chooses to begin. It MUST NOT introduce sign-in behavior as part of this presentation change.

#### Scenario: New learner starts onboarding
- **GIVEN** onboarding has not been completed
- **WHEN** the learner opens the app and chooses to start their journey
- **THEN** the prototype-aligned welcome page is shown and the learner continues into the existing onboarding flow

**Acceptance criteria**
- [ ] AC1: A new learner sees the prototype-aligned welcome page, and its existing primary action enters onboarding (test: `ui/src/androidTest/java/com/lingo/learn/ui/onboarding/WelcomeExperienceTest.kt`)

### Requirement: Home and lesson navigation
The app MUST present the prototype-aligned Home experience with current learner and lesson data and provide clear routes into continuing or starting the lesson, Plan, Practice, and Progress.

#### Scenario: Learner resumes today's lesson
- **GIVEN** a learner has an in-progress lesson
- **WHEN** the learner opens Home and selects the continue action
- **THEN** the lesson resumes at the saved place with its current progress

#### Scenario: Learner opens the main destinations
- **GIVEN** the learner is on Home
- **WHEN** the learner chooses Start lesson, Plan, Practice, and Progress
- **THEN** each action opens its matching existing or refreshed destination

**Acceptance criteria**
- [ ] AC1: Home renders learner, streak, XP, lesson, and completion values from current app state and routes the continue action to the saved lesson (test: `ui/src/androidTest/java/com/lingo/learn/ui/dashboard/HomeExperienceTest.kt`)
- [ ] AC2: Start lesson, Plan, Practice, and Progress actions each open the matching destination (test: `ui/src/androidTest/java/com/lingo/learn/ui/dashboard/HomeExperienceTest.kt`)

### Requirement: Daily lesson activities
The app MUST present the prototype-aligned vocabulary, read-aloud, and quiz activities, show feedback from existing evaluation behavior, and let the learner proceed through the lesson.

#### Scenario: Learner answers a vocabulary question
- **GIVEN** a vocabulary question is active
- **WHEN** the learner selects an answer
- **THEN** the app shows the existing correctness feedback and allows the learner to continue

#### Scenario: Learner completes a read-aloud evaluation
- **GIVEN** the learner has recorded a response to the displayed sentence
- **WHEN** pronunciation evaluation completes
- **THEN** the app presents the existing score and word feedback in the prototype-aligned result screen

**Acceptance criteria**
- [ ] AC1: Vocabulary selection displays the current question and existing correctness feedback, then advances through the existing progression (test: `ui/src/androidTest/java/com/lingo/learn/ui/learning/LessonExperienceTest.kt`)
- [ ] AC2: Read-aloud completion displays the existing score and word feedback, and Continue advances using the existing lesson state (test: `ui/src/androidTest/java/com/lingo/learn/ui/learning/LessonExperienceTest.kt`)
- [ ] AC3: Quiz selection displays the existing feedback and Next question advances using the existing quiz state (test: `ui/src/androidTest/java/com/lingo/learn/ui/learning/LessonExperienceTest.kt`)

### Requirement: Vocabulary and error-book review
The app MUST present the learner's saved vocabulary and error-book entries and preserve existing detail, explanation, review, and retry actions.

#### Scenario: Learner opens a saved word
- **GIVEN** the learner has saved error-book entries
- **WHEN** the learner opens vocabulary review and selects an entry
- **THEN** the app shows that entry's existing detail, history, explanation, and review actions

**Acceptance criteria**
- [ ] AC1: The vocabulary list displays current saved entries and selecting one opens its matching detail; existing explanation/review/retry actions remain available (test: `ui/src/androidTest/java/com/lingo/learn/ui/errorbook/VocabularyExperienceTest.kt`)

### Requirement: Plan and progress review
The app MUST present weekly plan and learner progress information using current saved data and provide the existing navigation between the two surfaces.

#### Scenario: Parent reviews the weekly plan
- **GIVEN** a weekly plan is available
- **WHEN** the parent opens Plan
- **THEN** the app presents the plan days, completion states, and available continue action using the saved plan

#### Scenario: Learner reviews growth
- **GIVEN** learning history is available
- **WHEN** the learner opens Progress
- **THEN** the app presents current learning time, activity, skill progress, and error-book summary

**Acceptance criteria**
- [ ] AC1: Plan displays saved days, completion states, and current available actions; selecting a day continues the existing lesson journey (test: `ui/src/androidTest/java/com/lingo/learn/ui/progress/PlanAndProgressExperienceTest.kt`)
- [ ] AC2: Progress displays current learning history, skill values, and error-book summary (test: `ui/src/androidTest/java/com/lingo/learn/ui/progress/PlanAndProgressExperienceTest.kt`)

### Requirement: Roleplay and speaking practice
The app MUST present the prototype-aligned roleplay conversation and speaking prompt, retaining the existing text and voice interaction options. It MUST NOT add a roleplay hint flow that does not exist in the current product behavior.

#### Scenario: Learner sends a typed roleplay response
- **GIVEN** a roleplay conversation is active
- **WHEN** the learner submits a text response
- **THEN** the response appears in the conversation and the existing scenario advances

#### Scenario: Learner sends a spoken roleplay response
- **GIVEN** microphone access is available during roleplay
- **WHEN** the learner records and submits a spoken response
- **THEN** the app processes it through the existing voice interaction and retains the conversation state

**Acceptance criteria**
- [ ] AC1: Submitting typed input displays it in the active conversation and continues the current scenario (test: `ui/src/androidTest/java/com/lingo/learn/ui/roleplay/RoleplayExperienceTest.kt`)
- [ ] AC2: Submitting spoken input follows the existing voice interaction and preserves the current scenario state (test: `ui/src/androidTest/java/com/lingo/learn/ui/roleplay/RoleplayExperienceTest.kt`)

### Requirement: Offline and recoverable error presentation
The app MUST present the offline and recoverable states that exist in the current application and preserve their existing processing actions and outcomes. It MUST NOT introduce system-settings, offline-retry, last-plan fallback, or diagnostic-reference actions that are not implemented by the existing flows.

#### Scenario: Learner uses an existing lesson while cloud AI is unavailable
- **GIVEN** cloud AI is unavailable for a lesson
- **WHEN** the learner continues through the existing lesson flow
- **THEN** the app identifies offline mode and retains the existing device voice and local scoring behavior

#### Scenario: Read-aloud recording cannot be evaluated
- **GIVEN** a recording was saved but could not be evaluated
- **WHEN** the learner chooses Check recording again
- **THEN** the app retries the existing evaluation using the saved recording

#### Scenario: Saved learning checkpoint is restored
- **GIVEN** a paused learning checkpoint exists
- **WHEN** the learner returns to the lesson
- **THEN** the app offers the existing Continue and Start Over actions

**Acceptance criteria**
- [ ] AC1: Offline mode is identified while existing device voice and local scoring remain available (test: `ui/src/androidTest/java/com/lingo/learn/ui/recovery/RecoveryExperienceTest.kt`)
- [ ] AC2: Read-aloud failure presents the existing microphone retry and skip actions without a fabricated score (test: `ui/src/androidTest/java/com/lingo/learn/ui/recovery/RecoveryExperienceTest.kt`)
- [ ] AC3: Saved checkpoint restoration exposes Continue and Start Over while retaining the existing saved checkpoint behavior (test: `ui/src/androidTest/java/com/lingo/learn/ui/recovery/RecoveryExperienceTest.kt`)

### Requirement: Huawei Mate 80 display regression
The refreshed app MUST pass Android simulator regression on a custom Huawei Mate 80 display profile using the Android SDK installed under `D:\system\android-sdk`. The profile MUST use the standard Mate 80 portrait display resolution of 1280×2832 pixels. This regression verifies app layout and journeys at that display size; it does not claim to emulate Huawei hardware or HarmonyOS.

#### Scenario: Refreshed journeys pass on the Mate 80 display profile
- **GIVEN** the custom Mate 80 display profile is booted in the D-drive Android simulator
- **WHEN** the app's UI regression suite exercises onboarding, Home, daily learning, vocabulary, Plan, Progress, roleplay, and recovery states
- **THEN** the journeys remain navigable, visible controls are not clipped, recovery outcomes remain unchanged, and the tests pass

**Acceptance criteria**
- [ ] AC1: The UI regression suite passes on an AVD configured at 1280×2832 using the SDK under `D:\system\android-sdk`, covering the listed journeys and existing recovery actions (test: `ui/src/androidTest/java/com/lingo/learn/ui/regression/HuaweiMate80RegressionTest.kt`)
- [ ] AC2: The regression test measures primary visible action bounds against the root viewport and fails if a required action is clipped; scrollable screens are checked after scrolling the action into view (test: `ui/src/androidTest/java/com/lingo/learn/ui/regression/HuaweiMate80RegressionTest.kt`)
