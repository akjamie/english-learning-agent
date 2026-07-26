package com.lingo.learn.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.lingo.learn.data.local.converter.AppTypeConverters
import com.lingo.learn.domain.model.ThemeUnit

@Entity(tableName = "theme_unit")
@TypeConverters(AppTypeConverters::class)
data class ThemeUnitEntity(
    @PrimaryKey val id: String,
    val name: String,
    val gradeBand: String,
    val durationDays: Int,
    val vocabIds: List<String>,
    val listeningMaterialsJson: String
) {
    fun toDomain() = ThemeUnit(
        id = id,
        name = name,
        gradeBand = gradeBand,
        durationDays = durationDays,
        vocabIds = vocabIds,
        listeningMaterialsJson = listeningMaterialsJson
    )

    companion object {
        fun fromDomain(domain: ThemeUnit) = ThemeUnitEntity(
            id = domain.id,
            name = domain.name,
            gradeBand = domain.gradeBand,
            durationDays = domain.durationDays,
            vocabIds = domain.vocabIds,
            listeningMaterialsJson = domain.listeningMaterialsJson
        )
    }
}
