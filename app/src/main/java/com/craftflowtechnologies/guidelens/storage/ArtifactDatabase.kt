package com.craftflowtechnologies.guidelens.storage

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Room Database for the Artifact System
 * Handles local storage of artifacts, user limits, and progress tracking
 */


@Database(
    entities = [
        ArtifactEntity::class,
        UserLimitsEntity::class,
        GeneratedImageEntity::class,
        StageImageEntity::class,
        ProgressNoteEntity::class,
        CachedImageEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(DatabaseConverters::class)
abstract class ArtifactDatabase : RoomDatabase() {
    
    abstract fun artifactDao(): ArtifactDao
    abstract fun userLimitsDao(): UserLimitsDao
    abstract fun imageDao(): ImageDao
    abstract fun cachedImageDao(): CachedImageDao
    
    companion object {
        @Volatile
        private var INSTANCE: ArtifactDatabase? = null
        
        fun getDatabase(context: Context): ArtifactDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ArtifactDatabase::class.java,
                    "artifact_database"
                )
                .addCallback(DatabaseCallback(CoroutineScope(SupervisorJob())))
                .addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigration() // For development - remove in production
                .build()
                INSTANCE = instance
                instance
            }
        }
        
        // Migration example for future schema changes
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Example migration - add new columns, tables, etc.
                // database.execSQL("ALTER TABLE artifacts ADD COLUMN new_field TEXT")
            }
        }
    }
}

/**
 * Database callback for initialization
 */
private class DatabaseCallback(
    private val scope: CoroutineScope
) : RoomDatabase.Callback() {
    
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        // Populate database with initial data if needed
    }
}


/**
 * Type converters for complex data types
 */
class DatabaseConverters {
    private val json = Json { ignoreUnknownKeys = true }
    
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return json.encodeToString(value)
    }
    
    @TypeConverter
    fun toStringList(value: String): List<String> {
        return try {
            json.decodeFromString(value)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    @TypeConverter
    fun fromStringMap(value: Map<String, String>): String {
        return json.encodeToString(value)
    }
    
    @TypeConverter
    fun toStringMap(value: String): Map<String, String> {
        return try {
            json.decodeFromString(value)
        } catch (e: Exception) {
            emptyMap()
        }
    }
    
    @TypeConverter
    fun fromStringSet(value: Set<String>): String {
        return json.encodeToString(value.toList())
    }
    
    @TypeConverter
    fun toStringSet(value: String): Set<String> {
        return try {
            json.decodeFromString<List<String>>(value).toSet()
        } catch (e: Exception) {
            emptySet()
        }
    }
    
    @TypeConverter
    fun fromArtifactType(value: ArtifactType): String {
        return value.name
    }
    
    @TypeConverter
    fun toArtifactType(value: String): ArtifactType {
        return try {
            ArtifactType.valueOf(value)
        } catch (e: Exception) {
            ArtifactType.RECIPE
        }
    }
    
    @TypeConverter
    fun fromUserTier(value: UserTier): String {
        return value.name
    }
    
    @TypeConverter
    fun toUserTier(value: String): UserTier {
        return try {
            UserTier.valueOf(value)
        } catch (e: Exception) {
            UserTier.FREE
        }
    }
    
    @TypeConverter
    fun fromArtifactContent(value: ArtifactContent): String {
        return json.encodeToString(value)
    }
    
    @TypeConverter
    fun toArtifactContent(value: String): ArtifactContent {
        return try {
            json.decodeFromString(value)
        } catch (e: Exception) {
            ArtifactContent.TextContent("")
        }
    }
    
    @TypeConverter
    fun fromGenerationMetadata(value: GenerationMetadata?): String? {
        return value?.let { json.encodeToString(it) }
    }
    
    @TypeConverter
    fun toGenerationMetadata(value: String?): GenerationMetadata? {
        return value?.let {
            try {
                json.decodeFromString(it)
            } catch (e: Exception) {
                null
            }
        }
    }
    
    @TypeConverter
    fun fromArtifactUsageStats(value: ArtifactUsageStats): String {
        return json.encodeToString(value)
    }
    
    @TypeConverter
    fun toArtifactUsageStats(value: String): ArtifactUsageStats {
        return try {
            json.decodeFromString(value)
        } catch (e: Exception) {
            ArtifactUsageStats()
        }
    }
    
    @TypeConverter
    fun fromArtifactProgress(value: ArtifactProgress?): String? {
        return value?.let { json.encodeToString(it) }
    }
    
    @TypeConverter
    fun toArtifactProgress(value: String?): ArtifactProgress? {
        return value?.let {
            try {
                json.decodeFromString(it)
            } catch (e: Exception) {
                null
            }
        }
    }
}

/**
 * Room entity for Artifacts
 */
@Entity(
    tableName = "artifacts",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["type"]),
        Index(value = ["createdAt"]),
        Index(value = ["agentType"])
    ]
)

data class ArtifactEntity(
    @PrimaryKey val id: String,
    val type: ArtifactType,
    val title: String,
    val description: String,
    val agentType: String,
    val userId: String,
    val createdAt: Long,
    val updatedAt: Long,
    val version: Int,
    val tags: List<String>,
    val contentData: ArtifactContent,
    val usageStats: ArtifactUsageStats,
    val currentProgress: ArtifactProgress?,
    val isDownloaded: Boolean,
    val downloadedAt: Long?,
    val fileSizeBytes: Long,
    val isShared: Boolean,
    val shareCode: String?,
    val originalArtifactId: String?,
    val difficulty: String,
    val estimatedDuration: Int?,
    val generationMetadata: GenerationMetadata?
)

/**
 * Room entity for User Limits
 */
@Entity(
    tableName = "user_limits",
    indices = [Index(value = ["userId"], unique = true)]
)
data class UserLimitsEntity(
    @PrimaryKey val userId: String,
    val tier: UserTier,
    val artifactsCreatedToday: Int,
    val artifactsCreatedThisWeek: Int,
    val imagesGeneratedToday: Int,
    val creditsRemaining: Int,
    val lastResetDate: Long,
    val isPremium: Boolean,
    val subscriptionExpiresAt: Long?
)

/**
 * Room entity for Generated Images
 */
@Entity(
    tableName = "generated_images",
    indices = [
        Index(value = ["artifactId"]),
        Index(value = ["generatedAt"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = ArtifactEntity::class,
            parentColumns = ["id"],
            childColumns = ["artifactId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class GeneratedImageEntity(
    @PrimaryKey val id: String,
    val artifactId: String,
    val url: String?,
    val base64Data: String?,
    val prompt: String,
    val model: String,
    val generatedAt: Long,
    val costCredits: Int,
    val isMainImage: Boolean = false,
    val localPath: String? = null
)

/**
 * Room entity for Stage Images
 */
@Entity(
    tableName = "stage_images",
    indices = [
        Index(value = ["artifactId"]),
        Index(value = ["stageNumber"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = ArtifactEntity::class,
            parentColumns = ["id"],
            childColumns = ["artifactId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class StageImageEntity(
    @PrimaryKey val id: String,
    val artifactId: String,
    val stageNumber: Int,
    val stepId: String?,
    val imageId: String, // References GeneratedImageEntity
    val description: String,
    val isKeyMilestone: Boolean
)

/**
 * Room entity for Progress Notes
 */
@Entity(
    tableName = "progress_notes",
    indices = [
        Index(value = ["artifactId"]),
        Index(value = ["timestamp"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = ArtifactEntity::class,
            parentColumns = ["id"],
            childColumns = ["artifactId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ProgressNoteEntity(
    @PrimaryKey val id: String,
    val artifactId: String,
    val stageIndex: Int,
    val note: String,
    val timestamp: Long,
    val imageUrl: String?
)

/**
 * Room entity for Cached Images
 */
@Entity(
    tableName = "cached_images",
    indices = [
        Index(value = ["artifactId"]),
        Index(value = ["downloadedAt"])
    ]
)
data class CachedImageEntity(
    @PrimaryKey val id: String,
    val url: String?,
    val localPath: String,
    val downloadedAt: Long,
    val fileSizeBytes: Long,
    val artifactId: String
)