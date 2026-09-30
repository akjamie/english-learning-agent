# Proposal: Refresh the learning experience

## Why
The latest Figma prototype brings the existing learning journeys into one clearer, child-friendly experience, including onboarding, daily learning, planning, practice, progress, and recovery states. Updating the app to match it now will make those journeys feel coherent while keeping learning data and established behavior intact.

## What Changes
- Refresh the app UI to cover every screen and state shown in `prototype/new-ui-prototype-20260927.pdf`, including the onboarding welcome page, Home, lesson activities, Plan, roleplay, vocabulary, Progress, and offline/error/microphone states.
- Keep current domain behavior and navigation outcomes wherever the prototype does not specify them; display live app data rather than embedding the prototype's example values.
- Preserve the existing error-processing actions and outcomes. The recovery screens may adopt the prototype's presentation, but retry, skip, saved-progress, and other existing processing behavior must remain unchanged.
- Define BDD acceptance scenarios for the covered user journeys and organize implementation work into vertical TDD slices at the UI-to-ViewModel seam: one behavior test, the minimum implementation to pass, then the next scenario.
- Add Android simulator regression coverage on a custom Huawei Mate 80 display profile using the Android SDK under `D:\system\android-sdk`.

## Capabilities
- **New Capabilities**: `learning-experience` — user-facing presentation and journeys across onboarding, daily learning, planning, practice, progress, and recovery states.
- **Modified Capabilities**: None.

## Impact
- `:ui` Compose screens and UI resources, including onboarding, dashboard, learning, weekly plan, roleplay, vocabulary/error book, progress/report, and recovery presentation.
- `app` screen routing and navigation presentation where needed to connect the prototype journeys.
- Existing ViewModels and domain/data behavior are reused; no new API, persistence, or AI decision behavior is intended by this proposal.
- Tests will verify observable screen states and user actions through UI-to-ViewModel-facing seams. Each BDD scenario will map to a test-first implementation slice; no tests are added as part of this proposal.
- The simulator profile will match the standard Mate 80 display resolution (1280×2832) for layout and journey regression. It will use an Android system image and does not claim Huawei hardware or HarmonyOS compatibility.

## System Design refs
None. This proposal changes the app's presentation and does not alter the `home-experience@v1` cross-repo contract.
