# Spec Delta

## Purpose
Gives learners a single, motivating home screen — their streak, XP and today's
lesson — with one clear next action, and honest handling of offline and
not-ready states.

## ADDED Requirements

### Requirement: Home dashboard summary
The Home screen SHALL show the learner's current day streak, total XP, and today's
lesson summary (title, CEFR level, planned minutes).

#### Scenario: Returning learner
- **WHEN** a learner with an active streak opens Home
- **THEN** Home shows the streak, total XP, and today's lesson card

#### Scenario: First-time learner
- **WHEN** a learner with no streak opens Home
- **THEN** Home shows a zero streak and the first lesson, with no error

### Requirement: Continue lesson action
Home SHALL present a single primary action that resumes the day's lesson and
reflects how many activities are already complete.

#### Scenario: Lesson partly done
- **WHEN** 2 of 5 activities are complete
- **THEN** the primary action reads "Continue lesson" and shows 2 of 5 complete

#### Scenario: Lesson complete
- **WHEN** all of today's activities are complete
- **THEN** Home reflects completion instead of offering to continue

### Requirement: Bottom navigation shell
The app SHALL provide a bottom navigation shell with Home, Plan, Practice, and
Progress destinations.

#### Scenario: Switch destination
- **WHEN** the learner taps Plan
- **THEN** the Plan destination is shown and Home is marked unselected

### Requirement: Offline state on Home
When the device is offline, Home SHALL remain usable with downloaded content and
SHALL NOT present being offline as an error.

#### Scenario: Offline with cached lesson
- **WHEN** the device has no connectivity and today's lesson is cached
- **THEN** Home shows an offline indicator and still offers to continue the cached lesson

### Requirement: Plan-not-ready state on Home
When this week's plan has not been generated yet, Home SHALL preserve completed
lessons and the streak, and SHALL offer to regenerate or reuse last week's plan.

#### Scenario: Plan generation failed
- **WHEN** plan generation has not completed
- **THEN** Home preserves completed lessons and the streak, and offers
  "Regenerate weekly plan" and "Use last week's plan"
