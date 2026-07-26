# Lingo English — AI Agent Design & Sequence Workflows

This document outlines the architecture, prompts, and sequence flows of the AI agents integrated into Lingo English. 

---

## 🤖 Agent System Overview

Lingo English operates four distinct agent roles to support the student's learning lifecycle:
1. **Weekly Plan Generator**: Analyzes last week's accuracy and vocabulary milestones to output a structured weekly curriculum.
2. **Pronunciation Evaluator (ASR-driven)**: Transcribes the child's recorded voice and compares it against standard reference sentences using Levenshtein distance matching.
3. **Explanation Agent (Error Book Guide)**: Provides friendly, bite-sized contextual explanations for mistakes stored in the Error Book.
4. **Daily Encourager & Notification Trigger**: Generates personalized push alerts and daily completion badges to boost motivation.

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

---

## 📝 Prompt Templates

### 1. Weekly Plan Generator Prompt (JSON output constraint)
Used to generate a structured 7-day study plan.

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
      "duration_minutes": 15
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

### 3. Daily Encourager Prompt
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
