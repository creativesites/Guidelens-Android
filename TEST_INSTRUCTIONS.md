# xAI API Testing Instructions

## Overview
The GuideLens app now includes comprehensive xAI API testing capabilities to verify image generation functionality with the latest Grok models.

## Files Created/Updated

### 1. XAIApiTester.kt
**Location**: `app/src/main/java/com/craftflowtechnologies/guidelens/api/XAIApiTester.kt`

**Purpose**: Comprehensive testing utility for xAI API connectivity and functionality

**Features**:
- ✅ API key validation
- ✅ Basic connectivity testing
- ✅ Model access testing for all supported models
- ✅ Simple and complex prompt testing
- ✅ Multiple image generation testing
- ✅ Error handling verification
- ✅ Detailed logging and reporting

### 2. Updated XAIImageClient.kt
**Location**: `app/src/main/java/com/craftflowtechnologies/guidelens/api/XAIImageClient.kt`

**Updates**:
- ✅ Updated to use `grok-2-image-1212` (latest model)
- ✅ Added model fallback system (tries multiple models)
- ✅ Improved error handling and retry logic
- ✅ Support for all model aliases

### 3. ApiTestScreen.kt
**Location**: `app/src/main/java/com/craftflowtechnologies/guidelens/ui/ApiTestScreen.kt`

**Purpose**: UI interface for running API tests

**Features**:
- ✅ Quick connectivity test button
- ✅ Full test suite execution
- ✅ Real-time test results display
- ✅ Dark/light theme support
- ✅ Detailed error reporting

## How to Test

### Method 1: Programmatic Testing
```kotlin
// Add this to any composable or activity
val context = LocalContext.current
val coroutineScope = rememberCoroutineScope()

// Quick test
coroutineScope.launch {
    val tester = XAIApiTester(context)
    val result = tester.quickConnectivityTest()
    Log.d("TEST", "Quick test result: ${result.success} - ${result.message}")
}

// Full test suite
coroutineScope.launch {
    val results = runXAIApiTests(context)
    results.forEach { result ->
        Log.d("TEST", "${result.message}: ${if (result.success) "PASS" else "FAIL"}")
    }
}
```

### Method 2: UI Testing (Recommended)
1. Add the `ApiTestScreen` to your navigation
2. Access it from the main menu or settings
3. Run tests interactively
4. View results in real-time

### Method 3: Logcat Testing
If you want to run tests without UI, add this code to any initialization point:

```kotlin
// In MainActivity or GuideLensApp
lifecycleScope.launch {
    val results = runXAIApiTests(this@MainActivity)
    // Results will be logged automatically
}
```

## Test Results Interpretation

### ✅ Success Indicators
- **API Key Validation**: Key format is correct and configured
- **Basic Connectivity**: Can reach xAI servers  
- **Model Access**: Can successfully use image generation models
- **Image Generation**: Successfully generates images with various prompts
- **Error Handling**: Properly handles and recovers from errors

### ❌ Failure Scenarios
- **API Key Issues**: Invalid, missing, or incorrect format
- **Network Problems**: Connectivity issues or firewall blocks
- **Model Unavailable**: Specific model versions not accessible
- **Rate Limiting**: Too many requests (wait and retry)
- **Authentication**: API key permissions or billing issues

## Expected Test Results

When all tests pass, you should see:
```
📊 xAI API Test Report
==================================================
1. ✅ PASS | API Key Validation Passed
   Details: API key format appears correct: xai-08td...cfuO

2. ✅ PASS | Basic Connectivity Test
   Details: HTTP 200 - Connection established in 234ms

3. ✅ PASS | Model Access: grok-2-image-1212 ✅
   Details: Successfully generated image in 2341ms

4. ✅ PASS | Model Access: grok-2-image-latest ✅  
   Details: Successfully generated image in 1876ms

5. ✅ PASS | Model Access: grok-2-image ✅
   Details: Successfully generated image in 2102ms

6. ✅ PASS | Simple Image Generation ✅
   Details: Generated in 1923ms

7. ✅ PASS | Complex Prompt Test ✅
   Details: Generated in 2456ms, revised prompt available

8. ✅ PASS | Multiple Images Test
   Details: Generated 3 images in 3234ms

9. ✅ PASS | Error Handling Test
   Details: Expected error received: HTTP 404

📈 Summary: 9/9 tests passed (100%)
==================================================
```

## Troubleshooting

### Common Issues

1. **API Key Not Configured**
   - Error: "API key is not configured"
   - Solution: Update `apiKey` in `XAIImageClient.kt`

2. **Network Connectivity**
   - Error: "Network error: timeout"
   - Solution: Check internet connection and firewall settings

3. **Model Not Found**
   - Error: "HTTP 404: Model not found"
   - Solution: xAI automatically falls back to available models

4. **Rate Limiting**
   - Error: "HTTP 429: Too many requests"
   - Solution: Wait a few minutes and retry

5. **Authentication Issues**
   - Error: "HTTP 401: Unauthorized"
   - Solution: Verify API key and billing status

### Debug Tips

1. **Enable Verbose Logging**:
   ```kotlin
   // Add to your test code
   Log.d("xAI", "Starting comprehensive test suite...")
   ```

2. **Check Network Logs**:
   - Use Android Studio's Network Inspector
   - Monitor actual API requests and responses

3. **Test Individual Components**:
   ```kotlin
   // Test just image generation
   val client = XAIImageClient(context)
   val result = client.generateImage("test prompt")
   ```

## Integration with Main App

The image generation will automatically work in chat conversations when:

1. ✅ API tests pass successfully
2. ✅ User mentions cooking, crafting, DIY, or visual topics
3. ✅ Smart prompt detection triggers image generation
4. ✅ Generated images appear in chat messages

## Performance Expectations

- **Quick Test**: ~2-3 seconds
- **Full Test Suite**: ~15-30 seconds  
- **Single Image Generation**: ~2-4 seconds
- **Multiple Images**: ~3-8 seconds
- **Model Fallback**: +1-2 seconds per fallback

## Next Steps

1. **Run Tests**: Execute the test suite to verify everything works
2. **Check Logs**: Review the detailed logs for any issues
3. **Test in Chat**: Try image generation in actual conversations
4. **Monitor Usage**: Keep track of API usage and costs
5. **Optimize Prompts**: Fine-tune prompts based on results

---

**Note**: All image generation features are now fully integrated and ready for testing. The fallback system ensures reliability even if specific models become unavailable.