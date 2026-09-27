# Tasks

## 1. Home data
- [ ] 1.1 Expose streak, total XP and today's lesson summary to the UI and verify with a unit test
- [ ] 1.2 Expose activity progress (completed/total) for the day and verify with a unit test

## 2. Home screen
- [ ] 2.1 Build the Home dashboard (greeting, streak, XP, lesson card) and verify with a UI test
- [ ] 2.2 Add the primary "Continue lesson" action with progress and verify both partly-done and complete cases
- [ ] 2.3 Handle the first-time learner (zero streak) and verify no error state

## 3. Navigation shell
- [ ] 3.1 Add the bottom navigation shell (Home / Plan / Practice / Progress) and verify switching destinations

## 4. Resilience states
- [ ] 4.1 Show the offline indicator and keep the cached lesson usable, and verify with a UI test
- [ ] 4.2 Show the plan-not-ready state with regenerate / reuse-last-week actions and verify with a UI test

## 5. Integration
- [ ] 5.1 Verify the app launches to the new Home and the old Dashboard route is gone (instrumentation test)
