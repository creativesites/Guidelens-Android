# xAI API Testing - Usage Example

## Quick Access via Sidebar

The API Test screen is now integrated into the GuideLens app sidebar menu. Here's how to access it:

### 🚀 **How to Access**

1. **Open the App**: Launch GuideLens and log in
2. **Open Sidebar**: Tap the menu button (☰) in the top bar
3. **Find API Test Button**: Look for the green bug report icon (🐛) in the header
4. **Run Tests**: Tap the button to open the API Test screen

### 🧪 **API Test Screen Features**

**Two Testing Options:**

1. **Quick Test** (Green button)
   - ⚡ Fast 2-3 second connectivity check
   - ✅ Tests basic API functionality
   - 🖼️ Generates one test image
   - Perfect for quick verification

2. **Full Test Suite** (Blue button)
   - 🔍 Comprehensive 15-30 second test
   - ✅ Tests all 9 components:
     - API key validation
     - Network connectivity
     - All 3 model versions
     - Simple & complex prompts
     - Multiple image generation
     - Error handling

### 📱 **User Interface**

The test screen includes:
- **Real-time Results**: See test progress as it happens
- **Color-coded Status**: Green for pass, red for fail
- **Detailed Information**: Response times, error codes, success details
- **Summary Card**: Overall pass/fail statistics
- **Back Navigation**: Easy return to main app

### 🎯 **Expected Results**

When everything is working correctly, you should see:

```
✅ API Key Validation Passed
✅ Basic Connectivity Test  
✅ Model Access: grok-2-image-1212 ✅
✅ Model Access: grok-2-image-latest ✅
✅ Model Access: grok-2-image ✅
✅ Simple Image Generation ✅
✅ Complex Prompt Test ✅
✅ Multiple Images Test
✅ Error Handling Test

📈 Summary: 9/9 tests passed (100%)
```

### 🔧 **Integration Benefits**

1. **No Code Changes**: Works with existing app structure
2. **Theme Support**: Adapts to light/dark mode automatically  
3. **Easy Access**: Always available from sidebar
4. **Developer Friendly**: Detailed logs in Android Studio console
5. **User Friendly**: Clear visual feedback for non-technical users

### 💡 **Troubleshooting**

If tests fail:

1. **Check Internet**: Ensure device has network connectivity
2. **Verify API Key**: Make sure the xAI API key is valid
3. **Check Logs**: Review Android Studio console for detailed errors
4. **Try Again**: Some failures may be temporary rate limiting

### 🚀 **Ready for Production**

The API testing integration is complete and ready for use:

- ✅ Integrated into sidebar menu
- ✅ Theme-aware UI design
- ✅ Real-time test execution
- ✅ Detailed result reporting
- ✅ Easy navigation back to main app

**Next Steps:**
1. Build and run the app
2. Access the API test via sidebar menu
3. Run tests to verify xAI integration
4. Start using image generation in conversations!

---

**Note**: The API test feature helps ensure your xAI integration is working properly before users experience any issues in actual conversations.