# Spec Delta

## Purpose
Gives learners immediate, actionable pronunciation feedback on spoken answers.

## ADDED Requirements

### Requirement: Pronunciation score
The system SHALL compute a 0-100 pronunciation score for each recorded answer and expose it on the answer result.

#### Scenario: Scored answer
- **WHEN** a learner records an answer with intelligible speech
- **THEN** the result screen shows a 0-100 pronunciation score

#### Scenario: Empty recording
- **WHEN** a learner submits a recording with no detected speech
- **THEN** the system returns a score of 0 and shows a retry prompt

### Requirement: Per-word highlights
The system SHALL mark each word in the expected sentence as correct, mispronounced, or missing.

#### Scenario: Mispronounced word
- **WHEN** the learner mispronounces exactly one word
- **THEN** that word is marked mispronounced and the remaining words are marked correct

### Requirement: Offline scoring
The system MUST compute the score on-device without any network call.

#### Scenario: Airplane mode
- **WHEN** the device has no network connectivity
- **THEN** scoring still returns a score within 2 seconds
