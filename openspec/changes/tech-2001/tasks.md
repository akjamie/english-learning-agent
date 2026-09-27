# Tasks

## 1. Domain — scoring engine
- [ ] 1.1 Implement `PronunciationScorer.score(audio, expected): Int` and verify a unit test returns a 0-100 value
- [ ] 1.2 Handle empty/no-speech input and verify it returns 0
- [ ] 1.3 Implement per-word alignment returning correct/mispronounced/missing and verify the alignment test

## 2. Data — audio pipeline
- [ ] 2.1 Stop exposing `rawAudioUrl` from `AnswerResult` and verify the data unit tests still pass

## 3. UI — result screen
- [ ] 3.1 Render the score and per-word highlights and verify with a UI test
- [ ] 3.2 Show a retry prompt on a 0 score and verify with a UI test

## 4. Integration
- [ ] 4.1 Verify offline scoring completes within 2s in airplane mode via an instrumentation test
