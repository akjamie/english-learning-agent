# Lingo English — AI Agent Design & Sequence Workflows

This document outlines the architecture, prompts, and sequence flows of the AI agents integrated into Lingo English. 

---

## 🤖 Agent System Overview

Lingo English operates six distinct agent roles to support the student's learning lifecycle:
1. **Weekly Plan Generator**: Analyzes last week's accuracy and vocabulary milestones to output a structured weekly curriculum with per-day rationale.
2. **Pronunciation Evaluator (ASR-driven)**: Transcribes the child's recorded voice and compares it against standard reference sentences using Levenshtein distance matching.
3. **Explanation Agent (Error Book Guide)**: Provides friendly, bite-sized contextual explanations for mistakes stored in the Error Book.
4. **Daily Encourager & Notification Trigger**: Generates personalized push alerts and daily completion badges to boost motivation.
5. **Observation Agent (Lingo Observes)**: Monitors the child's real-time learning event stream and surfaces cross-time, cross-task insights (e.g., "this word you missed last week — you got it right now!") via non-blocking speech bubbles during Quiz/Game stages. Differs from per-question feedback by performing **longitudinal comparison** against `LearningRecord` history.
6. **Decision Transparency Layer**: Persists every significant agent judgment (plan generated, difficulty adjusted, observation made, error pattern detected) into an `AgentDecisionLog` table, enabling three user-facing surfaces: (a) Plan page "Why this arrangement?" expandable annotation, (b) Weekly Report "What Lingo adjusted this week" section, (c) AI Growth Notes timeline page. This is not a new AI capability — it exposes the reasoning that was already happening but invisible to users.

> **Design Principle (Sprint 6)**: "Making AI presence visible" ≠ "adding spinning animations pretending to think." True visibility means exposing the judgment process and evidence that was already happening behind the scenes — letting users see not just "the result" but "how the result came to be" and "what the AI noticed that others wouldn't."

---

## 📊 Core Sequence Flows

### 1. Daily Oral Learning & Assessment Flow
This sequence illustrates how a user listens to a TTS model demonstration, records their voice, triggers the ASR transcriber, performs Levenshtein evaluation, and receives interactive feedback.

```mermaid
sequenceDiagram
    autonumber
    actor User as Child User
    participant UI as :ui Module (Compose)
    participant Prefs as SecureConfigPrefs
    participant ASR as AsrRepositoryImpl
    participant Net as MinimaxService (Retrofit)

    User->>UI: Tap "Listen"
    UI->>Prefs: Retrieve TTS Model & Base URL
    Prefs-->>UI: Return Config
    UI->>UI: Play Cached/Synthesized TTS Audio
    
    User->>UI: Long press Mic & speak sentence
    UI->>UI: Record and save audio to temporary .wav
    
    UI->>ASR: evaluatePronunciation(audioFile, referenceText)
    ASR->>Prefs: Retrieve ASR Model, Token, and Base URL
    Prefs-->>ASR: Return Configs
    
    ASR->>Net: audioToText(url, filePart, modelPart)
    Net-->>ASR: Return MinimaxAsrResponse(transcribedText)
    
    Note over ASR: Sanitize texts (lowercase, strip punctuations)<br/>Calculate Levenshtein distance similarity %<br/>Apply loose children threshold (min 60 points)
    
    ASR-->>UI: Return PronunciationResult(score, wordScores, feedback)
    UI->>UI: Render results (Green/Orange highlight words)
    UI->>UI: Update LingoAvatar expression (HAPPY / CELEBRATING)
```

### 2. Weekly Plan Generation & Sync Flow
This sequence details how study records are collected, formatted into a prompt, evaluated by the LLM, parsed into a structured JSON plan, and written to the local Room database.

```mermaid
sequenceDiagram
    autonumber
    actor Parent as Parent / System
    participant UI as :ui Module
    participant LLM as LlmRepositoryImpl
    participant DB as AppDatabase (Room)
    
    Parent->>UI: Request new Weekly Plan
    UI->>DB: Fetch last week's accuracy & weak words
    DB-->>UI: Return study metrics
    
    Note over UI: Build Weekly Plan Prompt with student metrics
    
    UI->>LLM: complete(planPrompt, taskType="PLAN")
    Note over LLM: Check monthly token budget
    LLM->>LLM: Append custom JSON constraints
    LLM->>LLM: Call Primary Model (glm-5.2) with fallback
    LLM-->>UI: Return LLM JSON String
    
    Note over UI: Parse JSON plan list into PlanEntity
    UI->>DB: Save/Sync PlanEntity to 'plan' table
    DB-->>UI: Write Success
    
    UI->>Parent: Display daily task cards in DashboardScreen
```

### 4. Emotional Intervention Flow
Triggered after 3 consecutive negative signals (wrong quiz answer, low ASR score, retry, recording failure) within a single stage. Resets on correct answer or stage transition.

```mermaid
sequenceDiagram
    autonumber
    actor User as Child User
    participant VM as LearningViewModel
    participant UI as :ui Module

    User->>UI: Submit wrong answer / Retry / Low score
    UI->>VM: submitQuizAnswer() / submitGameAnswer()<br/>stopRecording() / retryReadAlong()
    Note over VM: incrementNegativeSignal()
    alt consecutiveNegativeSignals >= 3
        VM->>VM: _showIntervention = true
        VM-->>UI: showIntervention dialog
        UI->>UI: Lingo: "Need a break?"
        alt User taps "Keep Going"
            UI->>VM: acceptContinue()
            Note over VM: resetNegativeSignal()
        else User taps "Take a Break"
            UI->>VM: acceptRest()
            Note over VM: saveCheckpoint() + pauseTask()
        else User taps "Skip This Stage"
            UI->>VM: acceptSkipStage()
            Note over VM: advance to next stage + resetNegativeSignal()
        end
    else < 3
        VM-->>UI: Continue normal flow
    end
```

### 5. Lingo Observation Flow (Sprint 6 - AI Presence)
Triggered during Quiz/Game stages when a meaningful pattern is detected via longitudinal comparison against `LearningRecord` history. Not every question triggers an observation - only specific, personalized patterns to avoid noise.

```mermaid
sequenceDiagram
    autonumber
    actor User as Child User
    participant UI as :ui Module (Quiz/Game)
    participant Engine as ObservationTriggerEngine
    participant DB as LearningRecordDao
    participant Log as AgentDecisionLogDao

    User->>UI: Submit quiz/game answer
    UI->>Engine: checkObservationTrigger(currentAnswer, questionContext)
    Engine->>DB: getRecordsSince(7 days ago)
    DB-->>Engine: Return historical LearningRecords
    
    Note over Engine: Pattern matching rules:<br/>1. Word was wrong last week, correct now<br/>2. Pronunciation score improved vs same sentence<br/>3. Question type answered faster than average<br/>4. Multiple retries before correct (encouragement)
    
    alt Pattern matched (not every question)
        Engine->>Engine: Select observation template + fill variables
        Engine->>Log: Persist observation decision
        Engine-->>UI: Return ObservationMessage(text, expression)
        UI->>UI: Show non-blocking Lingo speech bubble (3s auto-dismiss)
        Note over UI: Bubble does NOT block next question<br/>Appears alongside normal feedback
    else No meaningful pattern
        Engine-->>UI: Return null (no observation)
        UI->>UI: Continue normal feedback flow
    end
```

**Key distinction from per-question feedback**: Per-question feedback (correct/wrong animation) is already built into Sprint 2. Observations are **cross-time, cross-task** comparisons that only a system with memory of this child's history can produce. They are deliberately sparse (triggered by rules, not every question) to avoid becoming noise.

### 6. Plan Rationale & "Why" Annotation Flow (Sprint 6 - AI Presence)
When a weekly plan is generated, the LLM now outputs a per-day `rationale` field. This is displayed as an expandable "Why this arrangement?" annotation on each plan day card.

```mermaid
sequenceDiagram
    autonumber
    actor Parent as Parent / Child
    participant UI as WeeklyPlanScreen
    participant LLM as LlmRepositoryImpl
    participant DB as PlanDao + AgentDecisionLogDao
    
    Parent->>UI: Tap "Generate Plan"
    UI->>LLM: complete(planPrompt with rationale constraint)
    Note over LLM: LLM outputs JSON with per-day<br/>"rationale" field explaining arrangement
    LLM-->>UI: Return plan JSON (theme, days[], each day has rationale)
    UI->>DB: Save PlanEntity (snapshotData includes rationale)
    UI->>DB: Log AgentDecisionLog(PLAN_GENERATED, rationale summary)
    
    Parent->>UI: Tap "Why?" icon on a day card
    UI->>UI: Expand annotation card
    Note over UI: Display day.rationale:<br/>"Your listening accuracy was 72% last week,<br/>below other areas at 85%+. Added extra<br/>listening practice on Wednesday."
    UI-->>Parent: Show rationale (collapse on tap away)
```

---

## 📝 Prompt Templates

### 1. Weekly Plan Generator Prompt (JSON output constraint)
Used to generate a structured 7-day study plan with per-day rationale.

```
You are the curriculum planner for Lingo English. 
Your task is to generate a personalized 7-day English learning plan for a student in {grade} using {textbook} textbook.

--- Student Learning History ---
- Average Accuracy: {accuracy}%
- Weak Word Categories: {weak_categories}
- Completed Milestones: {completed_milestones}

--- Constraints & Output Format ---
You must output a raw, valid JSON object ONLY. Do not write markdown blocks like ```json or any prefix text. The JSON must match the following structure:
{
  "theme": "Unit theme name",
  "difficulty_coefficient": 1.2,
  "days": [
    {
      "day": 1,
      "focus": "Vocabulary / Grammar / Dialogue",
      "target_words": ["word1", "word2"],
      "reference_sentence": "Standard practice sentence of the day",
      "duration_minutes": 15,
      "rationale": "One sentence explaining WHY this day's content was arranged this way, referencing the student's specific learning history (e.g., 'Your listening accuracy was 72% last week, so today focuses on listening-intensive vocabulary')"
    }
  ]
}
```

### 2. Explanation Agent Prompt (Error Book Guide)
Generates simple, kid-friendly explanations for misspelled or mispronounced words.

```
You are Lingo, a friendly fox tutor. Explain the word "{word}" to a {grade} child.
The child got this word wrong in a quiz due to: {error_type}.

--- Guidelines ---
1. Use encouraging, warm, and simple English.
2. Provide one clear example sentence suitable for children.
3. Highlight a quick mnemonic trick or spelling tip (e.g., "hear has an 'ear' inside it").
4. Keep the explanation under 100 words. Do not be overly academic.
```

### 3. Emotional Intervention Prompt (Resilient Learning Engine)
Sprint 5 — triggered after 3 consecutive negative signals (wrong answers, low ASR scores, retries).

```
You are Lingo, a compassionate fox tutor. A student named {name} has encountered several difficulties in a row.

--- Guidelines ---
1. Acknowledge the frustration without dwelling on it — be warm and matter-of-fact.
2. Offer three choices: (a) take a short break, (b) keep going, or (c) skip this part.
3. Use encouraging, pressure-free language.
4. Keep the message under 60 words.
5. Do NOT mention scores, grades, or performance.
```

### 4. Daily Encourager Prompt
Creates badges and warm greetings.

```
You are the motivational assistant for Lingo English.
Generate a short push notification or banner greeting for a student named {name} who is on a {streak} day streak.

--- Tone and Rules ---
- Enthusiastic, warm, and gamified.
- Must be less than 40 characters.
- Use exactly 1 appropriate emoji.
- Do not mention tasks as "unfinished" or "obligations". Use invitations instead.
```

### 5. Observation Agent Prompt (Sprint 6 - AI Presence)
Generates personalized, cross-time insights during Quiz/Game stages. Uses template-first approach with LLM enrichment only for complex patterns.

```
You are Lingo, a fox tutor who remembers everything about your student {name}.
The student just answered a question and a meaningful pattern was detected:

--- Pattern Detected ---
{pattern_description}
Example: "The word 'classroom' was answered wrong in last week's quiz, but the student just answered it correctly today."

--- Student Context ---
- Word/Question: {current_word}
- Previous record: {previous_record}
- Current performance: {current_performance}

--- Guidelines ---
1. Acknowledge the improvement or pattern in ONE short sentence (max 20 words).
2. Be warm and specific - reference the actual word or skill, not generic praise.
3. Do NOT use phrases like "AI noticed" or "the system detected" - speak as Lingo naturally.
4. Use child-friendly language with at most 1 emoji.
5. If the pattern is about struggle (multiple retries), be encouraging, never judgmental.
```
