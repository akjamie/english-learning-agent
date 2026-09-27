# Proposal: Add pronunciation scoring for voice answers

## Why
Kids get no feedback on pronunciation today; parents asked for a score to track progress.

## What Changes
- Add on-device pronunciation scoring for recorded answers.
- Show a 0-100 score and per-word highlights on the answer result screen.
- **BREAKING**: `AnswerResult` no longer exposes `rawAudioUrl` to the UI layer.

## Capabilities
- **New Capabilities**: `pronunciation-scoring`
- **Modified Capabilities**: (none)

## Impact
- `domain` (scoring engine), `data` (audio pipeline), `ui` (result screen), `app` (wiring).

## System Design refs
- pronunciation-scoring@v1
