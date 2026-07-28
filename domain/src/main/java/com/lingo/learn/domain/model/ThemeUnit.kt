package org.akj.lingo.learn.domain.model

data class ThemeUnit(
    val id: String,
    val name: String,
    val gradeBand: String,             // Primary / Junior / Senior
    val durationDays: Int,
    val vocabIds: List<String>,        // List of all vocabulary IDs in this unit
    val listeningMaterialsJson: String // JSON description of extensive/intensive listening materials
)
