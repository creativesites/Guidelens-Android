package com.craftflowtechnologies.guidelens.storage

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Artifact operations
 */



@Dao
interface ArtifactDao {
//
    // ========== INSERT OPERATIONS ==========
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArtifact(artifact: ArtifactEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArtifacts(artifacts: List<ArtifactEntity>)
    
    // ========== QUERY OPERATIONS ==========
    @Query("SELECT * FROM artifacts WHERE id = :artifactId")
    suspend fun getArtifactById(artifactId: String): ArtifactEntity?
    
    @Query("SELECT * FROM artifacts WHERE userId = :userId ORDER BY createdAt DESC")
    fun getUserArtifacts(userId: String): Flow<List<ArtifactEntity>>
    
    @Query("SELECT * FROM artifacts WHERE userId = :userId AND type = :type ORDER BY createdAt DESC")
    suspend fun getUserArtifactsByType(userId: String, type: String): List<ArtifactEntity>
    
    @Query("SELECT * FROM artifacts WHERE userId = :userId AND agentType = :agentType ORDER BY createdAt DESC")
    fun getUserArtifactsByAgent(userId: String, agentType: String): Flow<List<ArtifactEntity>>
    
    @Query("""
        SELECT * FROM artifacts 
        WHERE userId = :userId 
        AND (title LIKE '%' || :searchQuery || '%' OR description LIKE '%' || :searchQuery || '%')
        ORDER BY createdAt DESC
    """)
    suspend fun searchUserArtifacts(userId: String, searchQuery: String): List<ArtifactEntity>
    
    @Query("SELECT * FROM artifacts WHERE userId = :userId AND isDownloaded = 1")
    suspend fun getDownloadedArtifacts(userId: String): List<ArtifactEntity>
    
    @Query("SELECT * FROM artifacts WHERE userId = :userId ORDER BY updatedAt DESC LIMIT :limit")
    suspend fun getRecentArtifacts(userId: String, limit: Int = 10): List<ArtifactEntity>
    
    @Query("""
        SELECT * FROM artifacts 
        WHERE userId = :userId 
        AND createdAt >= :startDate 
        ORDER BY createdAt DESC
    """)
    suspend fun getArtifactsCreatedSince(userId: String, startDate: Long): List<ArtifactEntity>
    
    // ========== UPDATE OPERATIONS ==========
    @Update
    suspend fun updateArtifact(artifact: ArtifactEntity)
    
    @Query("UPDATE artifacts SET currentProgress = :progressJson, updatedAt = :updatedAt WHERE id = :artifactId")
    suspend fun updateArtifactProgress(artifactId: String, progressJson: String?, updatedAt: Long)
    
    @Query("UPDATE artifacts SET usageStats = :statsJson, updatedAt = :updatedAt WHERE id = :artifactId")
    suspend fun updateUsageStats(artifactId: String, statsJson: String, updatedAt: Long)
    
    @Query("UPDATE artifacts SET isDownloaded = :isDownloaded, downloadedAt = :downloadedAt WHERE id = :artifactId")
    suspend fun updateDownloadStatus(artifactId: String, isDownloaded: Boolean, downloadedAt: Long?)
    
    @Query("UPDATE artifacts SET isShared = :isShared, shareCode = :shareCode WHERE id = :artifactId")
    suspend fun updateSharingStatus(artifactId: String, isShared: Boolean, shareCode: String?)
    
    // ========== DELETE OPERATIONS ==========
    @Delete
    suspend fun deleteArtifact(artifact: ArtifactEntity)
    
    @Query("DELETE FROM artifacts WHERE id = :artifactId AND userId = :userId")
    suspend fun deleteArtifactById(artifactId: String, userId: String)
    
    @Query("DELETE FROM artifacts WHERE userId = :userId")
    suspend fun deleteAllUserArtifacts(userId: String)
    
    @Query("DELETE FROM artifacts WHERE createdAt < :cutoffDate")
    suspend fun deleteOldArtifacts(cutoffDate: Long)
    
    // ========== STATISTICS QUERIES ==========
    @Query("SELECT COUNT(*) FROM artifacts WHERE userId = :userId")
    suspend fun getUserArtifactCount(userId: String): Int
    
    @Query("SELECT COUNT(*) FROM artifacts WHERE userId = :userId AND type = :type")
    suspend fun getUserArtifactCountByType(userId: String, type: ArtifactType): Int
    
    @Query("SELECT COUNT(*) FROM artifacts WHERE userId = :userId AND createdAt >= :startDate")
    suspend fun getUserArtifactCountSince(userId: String, startDate: Long): Int
    
    @Query("""
        SELECT type, COUNT(*) as count 
        FROM artifacts 
        WHERE userId = :userId 
        GROUP BY type
    """)
    suspend fun getUserArtifactTypeStats(userId: String): List<ArtifactTypeCount>
    
    @Query("SELECT SUM(fileSizeBytes) FROM artifacts WHERE userId = :userId AND isDownloaded = 1")
    suspend fun getTotalDownloadedSize(userId: String): Long?
}

/**
 * Data Access Object for User Limits operations
 */
@Dao
interface UserLimitsDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserLimits(userLimits: UserLimitsEntity)
    
    @Query("SELECT * FROM user_limits WHERE userId = :userId")
    suspend fun getUserLimits(userId: String): UserLimitsEntity?
    
    @Query("SELECT * FROM user_limits WHERE userId = :userId")
    fun getUserLimitsFlow(userId: String): Flow<UserLimitsEntity?>
    
    @Update
    suspend fun updateUserLimits(userLimits: UserLimitsEntity)
    
    @Query("""
        UPDATE user_limits 
        SET artifactsCreatedToday = :count, lastResetDate = :resetDate 
        WHERE userId = :userId
    """)
    suspend fun updateDailyArtifactCount(userId: String, count: Int, resetDate: Long)
    
    @Query("""
        UPDATE user_limits 
        SET imagesGeneratedToday = :count, lastResetDate = :resetDate 
        WHERE userId = :userId
    """)
    suspend fun updateDailyImageCount(userId: String, count: Int, resetDate: Long)
    
    @Query("UPDATE user_limits SET creditsRemaining = :credits WHERE userId = :userId")
    suspend fun updateCredits(userId: String, credits: Int)
    
    @Query("UPDATE user_limits SET tier = :tier WHERE userId = :userId")
    suspend fun updateUserTier(userId: String, tier: UserTier)
    
    @Delete
    suspend fun deleteUserLimits(userLimits: UserLimitsEntity)
}

/**
 * Data Access Object for Image operations
 */
@Dao
interface ImageDao {
//
    // ========== GENERATED IMAGES ==========
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGeneratedImage(image: GeneratedImageEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGeneratedImages(images: List<GeneratedImageEntity>)
    
    @Query("SELECT * FROM generated_images WHERE id = :imageId")
    suspend fun getGeneratedImageById(imageId: String): GeneratedImageEntity?
    
    @Query("SELECT * FROM generated_images WHERE artifactId = :artifactId ORDER BY generatedAt DESC")
    suspend fun getArtifactImages(artifactId: String): List<GeneratedImageEntity>
    
    @Query("SELECT * FROM generated_images WHERE artifactId = :artifactId AND isMainImage = 1")
    suspend fun getMainImage(artifactId: String): GeneratedImageEntity?
    
    @Query("DELETE FROM generated_images WHERE id = :imageId")
    suspend fun deleteGeneratedImage(imageId: String)
    
    // ========== STAGE IMAGES ==========
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStageImage(stageImage: StageImageEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStageImages(stageImages: List<StageImageEntity>)
    
    @Query("SELECT * FROM stage_images WHERE artifactId = :artifactId ORDER BY stageNumber ASC")
    suspend fun getArtifactStageImages(artifactId: String): List<StageImageEntity>
    
    @Query("SELECT * FROM stage_images WHERE artifactId = :artifactId AND stageNumber = :stageNumber")
    suspend fun getStageImage(artifactId: String, stageNumber: Int): StageImageEntity?
    
    @Query("DELETE FROM stage_images WHERE id = :stageImageId")
    suspend fun deleteStageImage(stageImageId: String)
    
    // ========== PROGRESS NOTES ==========
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgressNote(note: ProgressNoteEntity)
    
    @Query("SELECT * FROM progress_notes WHERE artifactId = :artifactId ORDER BY timestamp DESC")
    suspend fun getArtifactProgressNotes(artifactId: String): List<ProgressNoteEntity>
    
    @Query("SELECT * FROM progress_notes WHERE artifactId = :artifactId AND stageIndex = :stageIndex ORDER BY timestamp DESC")
    suspend fun getStageProgressNotes(artifactId: String, stageIndex: Int): List<ProgressNoteEntity>
    
    @Query("DELETE FROM progress_notes WHERE id = :noteId")
    suspend fun deleteProgressNote(noteId: String)
}

/**
 * Data Access Object for Cached Image operations
 */
@Dao
interface CachedImageDao {
    @Query("SELECT * FROM cached_images WHERE artifactId = :artifactId")
    suspend fun getImagesForArtifact(artifactId: String): List<CachedImageEntity>

    @Query("SELECT * FROM cached_images WHERE id = :id")
    suspend fun getCachedImage(id: String): CachedImageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCachedImage(image: CachedImageEntity)

    @Delete
    suspend fun deleteCachedImage(image: CachedImageEntity)

    @Query("DELETE FROM cached_images WHERE artifactId = :artifactId")
    suspend fun deleteImagesForArtifact(artifactId: String)

    @Query("SELECT SUM(fileSizeBytes) FROM cached_images")
    suspend fun getTotalCacheSize(): Long?
}

/**
 * Data class for artifact type statistics
 */
data class ArtifactTypeCount(
    val type: ArtifactType,
    val count: Int
)