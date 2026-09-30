# Design: refresh-learning-experience

## Context
The prototype is a single-page PDF board with no SVG in `prototype/`. It shows a visual system and representative states, but not every transition or data rule. The existing app already has Compose screens, ViewModels, navigation in `MainActivity`, localization resources, and established lesson/recovery behavior. The change is therefore a presentation and navigation integration across existing flows, not a replacement of the learning domain.

The `:ui` module has JUnit/coroutines unit testing and AndroidX JUnit/Espresso instrumentation support. It does not currently declare Compose UI testing dependencies. The affected UI flows span multiple screens and state holders, so tests should exercise the Compose boundary with controlled screen state rather than assert private composable structure.

The Android SDK is installed at `D:\system\android-sdk`. No AVD is currently listed there; the existing `LingoTest` AVD config is a Pixel 6 and is not a valid Huawei Mate 80 profile. The standard Mate 80 display is 1280×2832 at 6.75 inches, while its OS is HarmonyOS 6.0. Android's emulator can be configured to match the display for layout and journey regression, but it cannot validate Huawei hardware or HarmonyOS-specific behavior.

## Goals / Non-Goals
- Reuse existing domain models, repositories, ViewModels, persisted data, and business transitions.
- Make prototype screen states reachable through the existing app journeys and render dynamic values from app state.
- Give each BDD scenario an observable UI assertion and a named test file before its implementation slice.
- Do not redesign AI prompts, scoring, retries, skip semantics, persistence, or offline capability rules.
- Do not infer unshown behaviors from the static prototype; preserve established behavior in those cases.

## Decisions
1. **Treat the PDF as the visual reference and existing app behavior as the behavioral reference.** The PDF provides no detailed interaction specification for many screens and the expected SVG is absent. Use the shown copy, hierarchy, screen groupings, and recovery presentation as design intent; derive actions and transitions from current behavior unless explicitly represented.
   - Alternative: recreate each screen as a static mock matching the PDF. Rejected because it would disconnect screens from live learner data and existing flows.
2. **Keep presentation state at existing UI/ViewModel boundaries.** Adapt Compose screens and navigation to map current state into the prototype's presentation. Add a small presentation model only where several existing values need a stable screen contract; do not move domain rules into composables.
   - Alternative: consolidate all screens into a new navigation/state framework. Rejected for this change because existing navigation and ViewModels can support the journeys without a framework migration.
3. **Introduce Compose UI test support only as required for behavior tests.** Use Compose semantics and user actions at the screen-to-ViewModel boundary with deterministic state. Keep domain and processing assertions in their existing unit tests. Follow vertical TDD: for each scenario, add one failing UI test, implement the smallest passing slice, and continue.
   - Alternative: rely on screenshot comparison alone. Rejected because screenshots do not establish navigation, dynamic data, or preserved recovery actions.
4. **Preserve recovery action wiring.** Update visual state and copy while retaining the current handlers and their resulting state transitions. Add characterization assertions for retry, skip, saved recording, saved lesson progress, offline practice, and plan fallback before touching recovery presentation.
   - Alternative: simplify recovery into one generic error flow. Rejected because the prototype exposes distinct actionable states and existing behavior differs by failure type.
5. **Use current localization conventions for new or revised visible copy.** Maintain English and Simplified Chinese resources in the UI module; keep layout tests based on stable semantics rather than copy alone where possible.
6. **Do not add an account/sign-in journey to onboarding.** The current welcome screen has no sign-in destination or authentication contract. Keep its existing Settings and Start actions; omit the prototype's returning-account action until a separate account feature is defined.
7. **Run device-size regression on a custom Mate 80 display AVD.** Store the AVD data under `D:\system\android-sdk\avd` (set `ANDROID_AVD_HOME` accordingly), set its portrait resolution to 1280×2832, and run the UI regression suite against it. Compare required visible action bounds with the Compose root viewport, scrolling actions into view before measurement. Treat this as Android layout and journey coverage only; do not describe it as Huawei/HarmonyOS compatibility validation.

## Risks / Trade-offs
- [The static PDF leaves interaction and responsive-layout details unspecified] -> Use current app behavior for interactions and test representative supported device sizes; document any unresolved visual mismatch for design review.
- [A broad screen refresh can accidentally change recovery behavior] -> Characterize affected actions first and verify unchanged outcomes at the same UI-to-ViewModel seam.
- [Adding Compose UI test dependencies can increase build and instrumentation setup] -> Add only the test artifacts needed by the chosen test APIs and keep ViewModel logic coverage in fast JVM tests.
- [Prototype example values can be mistaken for fixed content] -> Bind labels and metrics to existing profile, session, plan, and progress state; reserve literals for instructional copy and sample prompts already defined by product behavior.
- [Large scope can obscure regressions] -> Implement and review by journey-sized vertical slices, retaining a green affected-test baseline between slices.
- [A custom Android AVD only matches Mate 80 display dimensions, not its HarmonyOS runtime or vendor services] -> Limit the claim to resolution/layout and Android app journey regression; use a physical Huawei device or Huawei-provided runtime if OS-specific validation becomes required.

## Migration Plan
No data migration or backend rollout is expected. Land the refresh incrementally by journey, keeping existing state models and navigation actions intact. If a screen slice causes a regression, revert that slice or restore its prior composable while retaining the characterization tests. Remove any temporary compatibility UI only after the full affected journey passes its acceptance tests.
