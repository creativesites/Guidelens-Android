package com.craftflowtechnologies.guidelens.config

/**
 * Configuration constants for voice and video modes
 * Based on CLAUDE.md specifications and Gemini API requirements
 */
object VoiceVideoConfig {
    
    // Audio Configuration
    object Audio {
        const val SAMPLE_RATE = 16000 // Hz - Optimal for speech recognition
        const val CHANNELS = 1 // Mono
        const val BIT_DEPTH = 16 // 16-bit PCM
        const val BUFFER_DURATION_MS = 100 // Milliseconds
        const val CHUNK_SIZE_MS = 50 // Send audio in 50ms chunks
        
        // Quality thresholds
        const val MIN_VOLUME_THRESHOLD = 0.01f
        const val MAX_VOLUME_THRESHOLD = 0.95f
        const val GOOD_VOLUME_MIN = 0.05f
        const val GOOD_VOLUME_MAX = 0.8f
        
        // Noise reduction
        const val NOISE_GATE_THRESHOLD = 0.02f
        const val AUTO_GAIN_CONTROL = true
        const val ECHO_CANCELLATION = true
    }
    
    // Video Configuration
    object Video {
        const val DEFAULT_WIDTH = 1280
        const val DEFAULT_HEIGHT = 720
        const val DEFAULT_FPS = 15
        const val MAX_FPS = 30
        const val MIN_FPS = 5
        
        // Frame processing
        const val ANALYSIS_FRAME_RATE = 5 // Analyze 5 frames per second
        const val GEMINI_FRAME_RATE = 2  // Send 2 frames per second to Gemini
        const val JPEG_QUALITY = 80
        
        // Quality settings
        object Quality {
            const val LOW_WIDTH = 640
            const val LOW_HEIGHT = 480
            const val LOW_BITRATE = 500_000 // 500 kbps
            
            const val MEDIUM_WIDTH = 1280
            const val MEDIUM_HEIGHT = 720
            const val MEDIUM_BITRATE = 1_000_000 // 1 Mbps
            
            const val HIGH_WIDTH = 1920
            const val HIGH_HEIGHT = 1080
            const val HIGH_BITRATE = 2_000_000 // 2 Mbps
        }
    }
    
    // Session Configuration (from CLAUDE.md)
    object Session {
        object Limits {
            // Free tier
            const val FREE_TEXT_UNLIMITED = -1
            const val FREE_VOICE_SESSIONS_PER_DAY = 10
            const val FREE_VOICE_DURATION_MINUTES = 5
            const val FREE_VIDEO_SESSIONS_PER_WEEK = 3
            const val FREE_VIDEO_DURATION_MINUTES = 10
            const val FREE_MAX_DURATION_SECONDS = 1800 // 30 minutes
            const val FREE_COST_CAP = 0.05 // USD
            const val FREE_TOKEN_LIMIT = 100_000
            
            // Basic tier
            const val BASIC_VOICE_SESSIONS_PER_DAY = 50
            const val BASIC_VOICE_DURATION_MINUTES = 10
            const val BASIC_VIDEO_SESSIONS_PER_DAY = 2
            const val BASIC_VIDEO_DURATION_MINUTES = 15
            const val BASIC_MAX_DURATION_SECONDS = 3600 // 60 minutes
            const val BASIC_COST_CAP = 0.10 // USD
            const val BASIC_TOKEN_LIMIT = 200_000
            
            // Pro tier
            const val PRO_VOICE_UNLIMITED = -1
            const val PRO_VIDEO_UNLIMITED = -1
            const val PRO_VIDEO_DURATION_MINUTES = 30
            const val PRO_MAX_DURATION_SECONDS = -1 // Unlimited
            const val PRO_COST_CAP = 2.00 // USD
            const val PRO_TOKEN_LIMIT = 500_000
        }
        
        // Session timeouts and warnings
        const val WARNING_THRESHOLD_PERCENT = 80 // Warn at 80% of limit
        const val SESSION_TIMEOUT_WARNING_MS = 60_000 // 1 minute warning
        const val MAX_IDLE_TIME_MS = 300_000 // 5 minutes before auto-disconnect
    }
    
    // Network Configuration
    object Network {
        const val CONNECTION_TIMEOUT_MS = 30_000 // 30 seconds
        const val READ_TIMEOUT_MS = 30_000
        const val WRITE_TIMEOUT_MS = 30_000
        const val MAX_RETRIES = 3
        const val RETRY_DELAY_MS = 1000
        const val EXPONENTIAL_BACKOFF_MULTIPLIER = 2
        
        // Bandwidth requirements (kbps)
        const val MIN_AUDIO_BANDWIDTH = 32
        const val RECOMMENDED_AUDIO_BANDWIDTH = 64
        const val MIN_VIDEO_BANDWIDTH = 500
        const val RECOMMENDED_VIDEO_BANDWIDTH = 1000
        
        // Buffer sizes
        const val AUDIO_BUFFER_SIZE_MS = 500
        const val VIDEO_BUFFER_SIZE_FRAMES = 5
    }
    
    // Error Handling
    object ErrorHandling {
        const val MAX_CONSECUTIVE_ERRORS = 5
        const val ERROR_RECOVERY_DELAY_MS = 2000
        const val GRACEFUL_DEGRADATION_ENABLED = true
        
        // Retry strategies
        object Retry {
            const val AUDIO_RECORDING_RETRIES = 3
            const val VIDEO_CAPTURE_RETRIES = 3
            const val NETWORK_REQUEST_RETRIES = 3
            const val PERMISSION_REQUEST_RETRIES = 2
        }
    }
    
    // Performance Configuration
    object Performance {
        // Threading
        const val AUDIO_THREAD_PRIORITY = Thread.MAX_PRIORITY - 1
        const val VIDEO_THREAD_PRIORITY = Thread.NORM_PRIORITY + 1
        const val NETWORK_THREAD_PRIORITY = Thread.NORM_PRIORITY
        
        // Memory management
        const val MAX_AUDIO_BUFFER_SIZE_MB = 10
        const val MAX_VIDEO_BUFFER_SIZE_MB = 50
        const val GARBAGE_COLLECTION_INTERVAL_MS = 30_000
        
        // CPU usage limits
        const val MAX_CPU_USAGE_PERCENT = 70
        const val THERMAL_THROTTLING_ENABLED = true
    }
    
    // UI Configuration
    object UI {
        // Animation durations
        const val MODE_SWITCH_ANIMATION_MS = 300
        const val PULSE_ANIMATION_MS = 1000
        const val WAVE_ANIMATION_MS = 1500
        
        // Visual feedback
        const val VOICE_LEVEL_UPDATE_MS = 50
        const val TRANSCRIPTION_TYPING_DELAY_MS = 30
        const val ERROR_MESSAGE_DURATION_MS = 5000
        
        // Colors and themes
        const val LISTENING_INDICATOR_COLOR = 0xFF10B981 // Green
        const val SPEAKING_INDICATOR_COLOR = 0xFF3B82F6 // Blue
        const val ERROR_INDICATOR_COLOR = 0xFFEF4444 // Red
        const val WARNING_INDICATOR_COLOR = 0xFFFBBF24 // Yellow
    }
    
    // Gemini API Configuration
    object GeminiApi {
        const val WEBSOCKET_PING_INTERVAL_MS = 30_000
        const val MAX_MESSAGE_SIZE_BYTES = 1_048_576 // 1 MB
        const val RESPONSE_TIMEOUT_MS = 30_000
        
        // Content limits
        const val MAX_AUDIO_CHUNK_SIZE_BYTES = 65_536 // 64 KB
        const val MAX_IMAGE_SIZE_BYTES = 524_288 // 512 KB
        const val MAX_TEXT_LENGTH = 4096
        
        // Model selection based on user tier
        fun getModelForTier(userTier: String, mode: String): String {
            return when (userTier) {
                "free" -> when (mode) {
                    "voice", "video" -> "gemini-1.5-flash"
                    else -> "gemini-1.5-flash"
                }
                "basic" -> when (mode) {
                    "voice" -> "gemini-1.5-flash"
                    "video" -> "gemini-1.5-flash"
                    else -> "gemini-1.5-flash"
                }
                "pro" -> when (mode) {
                    "voice" -> "gemini-1.5-flash-native-audio"
                    "video" -> "gemini-1.5-flash-live"
                    else -> "gemini-1.5-pro"
                }
                else -> "gemini-1.5-flash"
            }
        }
        
        fun getCostPerToken(userTier: String, mode: String): Double {
            return when (mode) {
                "voice" -> when (userTier) {
                    "pro" -> 0.50 // per 1M input tokens
                    else -> 0.10
                }
                "video" -> when (userTier) {
                    "pro" -> 3.00 // per 1M input tokens
                    else -> 0.50
                }
                else -> 0.10
            }
        }
    }
    
    // Feature Flags
    object FeatureFlags {
        const val ENABLE_NOISE_REDUCTION = true
        const val ENABLE_ECHO_CANCELLATION = true
        const val ENABLE_AUTO_GAIN_CONTROL = true
        const val ENABLE_VIDEO_ANALYSIS = true
        const val ENABLE_BANDWIDTH_ADAPTATION = true
        const val ENABLE_OFFLINE_FALLBACK = true
        const val ENABLE_ANALYTICS = true
        const val ENABLE_DEBUG_LOGGING = true
    }
}