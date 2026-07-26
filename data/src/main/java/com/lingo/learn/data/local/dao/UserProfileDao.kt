package com.lingo.learn.data.local.dao

import androidx.room.*
import com.lingo.learn.data.local.entity.UserProfileEntity

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile LIMIT 1")
    suspend fun getUserProfile(): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(userProfile: UserProfileEntity)

    @Delete
    suspend fun delete(userProfile: UserProfileEntity)
}
