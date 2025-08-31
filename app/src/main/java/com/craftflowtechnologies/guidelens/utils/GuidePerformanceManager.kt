package com.craftflowtechnologies.guidelens.utils

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import kotlin.system.measureTimeMillis

/**
 * Enterprise-grade performance management system for GuideLens
 * Handles memory optimization, caching, background processing, and performance monitoring
 */
class GuidePerformanceManager private constructor() {
    
    companion object {
        @JvmStatic
        val instance = GuidePerformanceManager()
        
        private const val MAX_CACHE_SIZE = 50 // Maximum cached items
        const val CACHE_EXPIRY_MS = 300_000L // 5 minutes
        private const val MEMORY_THRESHOLD = 0.8f // 80% memory usage threshold
    }
    
    // Performance monitoring
    private val _performanceMetrics = MutableStateFlow(GuidePerformanceMetrics())
    val performanceMetrics: StateFlow<GuidePerformanceMetrics> = _performanceMetrics.asStateFlow()
    
    // Memory management
    private val imageCache = ConcurrentHashMap<String, GuideCacheItem<ImageBitmap>>()
    private val dataCache = ConcurrentHashMap<String, GuideCacheItem<Any>>()
    
    // Background processing
    private val backgroundScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val mainScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    // Custom thread pool for heavy operations
    private val heavyOperationDispatcher = Executors.newFixedThreadPool(2).asCoroutineDispatcher()
    
    // Performance tracking
    private val operationTimes = ConcurrentHashMap<String, MutableList<Long>>()
    
    init {
        startPerformanceMonitoring()
        startCacheCleanup()
    }
    
    // Memory Management
    fun cacheImage(key: String, image: ImageBitmap, priority: GuideCachePriority = GuideCachePriority.NORMAL) {
        if (shouldCache() && imageCache.size < MAX_CACHE_SIZE) {
            imageCache[key] = GuideCacheItem(
                data = image,
                timestamp = System.currentTimeMillis(),
                priority = priority,
                accessCount = 0
            )
        }
    }
    
    fun getCachedImage(key: String): ImageBitmap? {
        val cacheItem = imageCache[key]
        return if (cacheItem?.isValid() == true) {
            cacheItem.accessCount++
            cacheItem.data
        } else {
            imageCache.remove(key)
            null
        }
    }
    
    fun cacheData(key: String, data: Any, priority: GuideCachePriority = GuideCachePriority.NORMAL) {
        if (shouldCache() && dataCache.size < MAX_CACHE_SIZE) {
            dataCache[key] = GuideCacheItem(
                data = data,
                timestamp = System.currentTimeMillis(),
                priority = priority,
                accessCount = 0
            )
        }
    }
    
    @Suppress("UNCHECKED_CAST")
    fun <T> getCachedData(key: String): T? {
        val cacheItem = dataCache[key]
        return if (cacheItem?.isValid() == true) {
            cacheItem.accessCount++
            cacheItem.data as? T
        } else {
            dataCache.remove(key)
            null
        }
    }
    
    // Background Processing
    fun executeInBackground(
        operation: suspend () -> Unit,
        priority: GuideTaskPriority = GuideTaskPriority.NORMAL
    ): Job {
        val dispatcher = when (priority) {
            GuideTaskPriority.LOW -> Dispatchers.Default
            GuideTaskPriority.NORMAL -> Dispatchers.Default
            GuideTaskPriority.HIGH -> heavyOperationDispatcher
            GuideTaskPriority.CRITICAL -> Dispatchers.Default
        }
        
        return CoroutineScope(dispatcher).launch {
            try {
                operation()
            } catch (e: Exception) {
                e.reportToGuide(
                    context = "Background operation failed",
                    category = GuideErrorCategory.GENERAL
                )
            }
        }
    }
    
    fun executeIO(operation: suspend () -> Unit): Job {
        return ioScope.launch {
            try {
                operation()
            } catch (e: Exception) {
                e.reportToGuide(
                    context = "IO operation failed",
                    category = GuideErrorCategory.GENERAL
                )
            }
        }
    }
    
    fun executeOnMain(operation: suspend () -> Unit): Job {
        return mainScope.launch {
            try {
                operation()
            } catch (e: Exception) {
                e.reportToGuide(
                    context = "Main thread operation failed",
                    category = GuideErrorCategory.GENERAL
                )
            }
        }
    }
    
    // Performance Monitoring
    fun <T> measureOperation(operationName: String, operation: () -> T): T {
        val result: T
        val timeMs = measureTimeMillis {
            result = operation()
        }
        
        recordOperationTime(operationName, timeMs)
        return result
    }
    
    suspend fun <T> measureSuspendOperation(operationName: String, operation: suspend () -> T): T {
        val result: T
        val timeMs = measureTimeMillis {
            result = operation()
        }
        
        recordOperationTime(operationName, timeMs)
        return result
    }
    
    private fun recordOperationTime(operationName: String, timeMs: Long) {
        val times = operationTimes.getOrPut(operationName) { mutableListOf() }
        times.add(timeMs)
        
        // Keep only last 100 measurements
        if (times.size > 100) {
            times.removeAt(0)
        }
        
        // Update metrics if it's a significant operation (>100ms)
        if (timeMs > 100) {
            updatePerformanceMetrics()
        }
    }
    
    // Memory Optimization
    fun optimizeMemory() {
        executeInBackground({
            clearExpiredCache()
            trimLowPriorityCache()
            System.gc() // Suggest garbage collection
        }, GuideTaskPriority.LOW)
    }
    
    fun clearCache() {
        imageCache.clear()
        dataCache.clear()
    }
    
    private fun clearExpiredCache() {
        val currentTime = System.currentTimeMillis()
        
        imageCache.entries.removeIf { (_, item) ->
            currentTime - item.timestamp > CACHE_EXPIRY_MS
        }
        
        dataCache.entries.removeIf { (_, item) ->
            currentTime - item.timestamp > CACHE_EXPIRY_MS
        }
    }
    
    private fun trimLowPriorityCache() {
        if (isMemoryPressure()) {
            // Remove low priority items first
            imageCache.entries.removeIf { (_, item) ->
                item.priority == GuideCachePriority.LOW
            }
            
            dataCache.entries.removeIf { (_, item) ->
                item.priority == GuideCachePriority.LOW
            }
        }
    }
    
    private fun shouldCache(): Boolean {
        return !isMemoryPressure()
    }
    
    private fun isMemoryPressure(): Boolean {
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val maxMemory = runtime.maxMemory()
        
        return (usedMemory.toFloat() / maxMemory.toFloat()) > MEMORY_THRESHOLD
    }
    
    // Performance Monitoring
    private fun startPerformanceMonitoring() {
        backgroundScope.launch {
            while (true) {
                delay(30000) // Update every 30 seconds
                updatePerformanceMetrics()
            }
        }
    }
    
    private fun startCacheCleanup() {
        backgroundScope.launch {
            while (true) {
                delay(60000) // Cleanup every minute
                clearExpiredCache()
                if (isMemoryPressure()) {
                    trimLowPriorityCache()
                }
            }
        }
    }
    
    private fun updatePerformanceMetrics() {
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val maxMemory = runtime.maxMemory()
        val memoryUsagePercent = (usedMemory.toFloat() / maxMemory.toFloat()) * 100
        
        val averageOperationTimes = operationTimes.mapValues { (_, times) ->
            if (times.isNotEmpty()) times.average() else 0.0
        }
        
        _performanceMetrics.value = GuidePerformanceMetrics(
            memoryUsageMB = usedMemory / (1024 * 1024),
            maxMemoryMB = maxMemory / (1024 * 1024),
            memoryUsagePercent = memoryUsagePercent,
            cacheSize = imageCache.size + dataCache.size,
            averageOperationTimes = averageOperationTimes,
            isMemoryPressure = isMemoryPressure(),
            timestamp = System.currentTimeMillis()
        )
    }
    
    fun getPerformanceReport(): GuidePerformanceReport {
        val metrics = _performanceMetrics.value
        val slowOperations = metrics.averageOperationTimes
            .filter { it.value > 500 } // Operations taking more than 500ms
            .toMap()
        
        return GuidePerformanceReport(
            currentMetrics = metrics,
            slowOperations = slowOperations,
            cacheHitRate = calculateCacheHitRate(),
            memoryRecommendations = getMemoryRecommendations(),
            performanceScore = calculatePerformanceScore()
        )
    }
    
    private fun calculateCacheHitRate(): Float {
        val totalItems = imageCache.size + dataCache.size
        val totalAccesses = (imageCache.values.sumOf { it.accessCount } + 
                           dataCache.values.sumOf { it.accessCount })
        
        return if (totalAccesses > 0) {
            totalItems.toFloat() / totalAccesses.toFloat()
        } else 0f
    }
    
    private fun getMemoryRecommendations(): List<String> {
        val recommendations = mutableListOf<String>()
        val metrics = _performanceMetrics.value
        
        if (metrics.memoryUsagePercent > 80) {
            recommendations.add("High memory usage detected. Consider clearing cache.")
        }
        
        if (metrics.cacheSize > MAX_CACHE_SIZE * 0.8) {
            recommendations.add("Cache is near capacity. Automatic cleanup will occur soon.")
        }
        
        if (metrics.averageOperationTimes.any { it.value > 1000 }) {
            recommendations.add("Some operations are taking longer than expected.")
        }
        
        return recommendations
    }
    
    private fun calculatePerformanceScore(): Int {
        val metrics = _performanceMetrics.value
        var score = 100
        
        // Deduct points for high memory usage
        if (metrics.memoryUsagePercent > 80) score -= 20
        else if (metrics.memoryUsagePercent > 60) score -= 10
        
        // Deduct points for slow operations
        val slowOperations = metrics.averageOperationTimes.count { it.value > 500 }
        score -= slowOperations * 5
        
        // Deduct points for cache pressure
        if (metrics.cacheSize > MAX_CACHE_SIZE * 0.8) score -= 10
        
        return score.coerceAtLeast(0)
    }
    
    // Cleanup
    fun destroy() {
        backgroundScope.cancel()
        ioScope.cancel()
        mainScope.cancel()
        heavyOperationDispatcher.close()
        clearCache()
    }
}

// Data classes
data class GuideCacheItem<T>(
    val data: T,
    val timestamp: Long,
    val priority: GuideCachePriority,
    var accessCount: Int = 0
) {
    fun isValid(): Boolean {
        return System.currentTimeMillis() - timestamp < GuidePerformanceManager.CACHE_EXPIRY_MS
    }
}

enum class GuideCachePriority {
    LOW,
    NORMAL,
    HIGH,
    CRITICAL
}

enum class GuideTaskPriority {
    LOW,
    NORMAL,
    HIGH,
    CRITICAL
}

data class GuidePerformanceMetrics(
    val memoryUsageMB: Long = 0,
    val maxMemoryMB: Long = 0,
    val memoryUsagePercent: Float = 0f,
    val cacheSize: Int = 0,
    val averageOperationTimes: Map<String, Double> = emptyMap(),
    val isMemoryPressure: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

data class GuidePerformanceReport(
    val currentMetrics: GuidePerformanceMetrics,
    val slowOperations: Map<String, Double>,
    val cacheHitRate: Float,
    val memoryRecommendations: List<String>,
    val performanceScore: Int
)

// Composable utilities
@Composable
fun rememberGuidePerformanceMetrics(): State<GuidePerformanceMetrics> {
    return GuidePerformanceManager.instance.performanceMetrics.collectAsState()
}

@Composable
fun <T> rememberCachedValue(
    key: String,
    computation: @Composable () -> T
): T {
    var cachedValue by remember { mutableStateOf<T?>(null) }
    
    LaunchedEffect(key) {
        val cached = GuidePerformanceManager.instance.getCachedData<T>(key)
        if (cached != null) {
            cachedValue = cached
        }
    }
    
    return cachedValue ?: computation().also { result ->
        GuidePerformanceManager.instance.cacheData(key, result as Any)
        cachedValue = result
    }
}

// Extension functions
fun <T> T.cacheAsGuideData(key: String, priority: GuideCachePriority = GuideCachePriority.NORMAL): T {
    GuidePerformanceManager.instance.cacheData(key, this as Any, priority)
    return this
}

// Performance monitoring helpers
inline fun <T> withGuidePerformanceTracking(
    operationName: String,
    noinline operation: () -> T
): T {
    return GuidePerformanceManager.instance.measureOperation(operationName, operation)
}

suspend fun <T> withGuideSuspendPerformanceTracking(
    operationName: String,
    operation: suspend () -> T
): T {
    return GuidePerformanceManager.instance.measureSuspendOperation(operationName, operation)
}

// Memory optimization helpers
fun optimizeGuideMemory() {
    GuidePerformanceManager.instance.optimizeMemory()
}

fun clearGuideCache() {
    GuidePerformanceManager.instance.clearCache()
}