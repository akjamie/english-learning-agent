# Proposal: New Lingo Home experience

## Why
The 2026-09-27 UI prototype defines a warmer, clearer first screen: a study-buddy
greeting, streak and XP at a glance, and one obvious next action. The current Home
is a functional but flat dashboard that does not match the new design direction.

## What Changes
- New Home dashboard: greeting, day streak, total XP, today's lesson card
  (title, CEFR level, planned minutes), activity progress, and a primary
  "Continue lesson" action.
- Add a bottom navigation shell: Home / Plan / Practice / Progress.
- Show honest, non-blocking states on Home when the learner is offline, or when
  this week's plan is not ready yet.
- **BREAKING**: the existing `Dashboard` route is replaced by the new Home screen.

## Capabilities
- **New Capabilities**: `home-experience`
- **Modified Capabilities**: (none)

## Impact
- `ui` (new Home screen + navigation host), `app` (nav graph wiring),
  `domain`/`data` (read streak / XP / plan state for Home).

## System Design refs
- (none — single repo, no cross-repo contract)
