# 🔥 Firebase Setup Guide for GuideLens

## Current Status
✅ **Debug SHA-1 Generated**: `AB:D7:E4:47:56:85:CF:CD:A9:6B:AC:B6:6F:30:62:51:CD:0E:66:8B`  
✅ **Build Configuration**: Firebase plugins added to build files  
⏳ **Next Step**: Create Firebase project and download `google-services.json`

---

## Step 1: Create Firebase Project

1. **Go to Firebase Console**: https://console.firebase.google.com/
2. **Click "Add project"**
3. **Project Configuration**:
   - **Project Name**: `GuideLens`
   - **Project ID**: `guidelens-[random]` (Firebase will generate)
   - **Enable Google Analytics**: ✅ Yes (recommended)
   - **Analytics Account**: Create new or use existing
4. **Click "Create project"**

---

## Step 2: Add Android App

1. **In your new Firebase project, click "Add app" → Android**
2. **App Configuration**:
   - **Android package name**: `com.craftflowtechnologies.guidelens`
   - **App nickname**: `GuideLens Debug`
   - **Debug signing certificate SHA-1**: `AB:D7:E4:47:56:85:CF:CD:A9:6B:AC:B6:6F:30:62:51:CD:0E:66:8B`

3. **Click "Register app"**

---

## Step 3: Download google-services.json

1. **Download the `google-services.json` file** from Firebase Console
2. **Move it to your project**:
   ```bash
   # Replace the placeholder file with the real one
   mv ~/Downloads/google-services.json app/google-services.json
   rm app/google-services-placeholder.json
   ```

---

## Step 4: Enable Required Firebase Services

### 🎤 **Firebase AI Logic (Required for Voice Chat)**
1. In Firebase Console → **Build** → **Vertex AI in Firebase**
2. **Enable Vertex AI in Firebase**
3. **Note**: This is in Developer Preview - follow setup instructions

### 🔐 **Authentication (Optional but Recommended)**
1. Firebase Console → **Build** → **Authentication**  
2. **Get started** → **Sign-in method**
3. **Enable providers**:
   - **Google** (for Google Sign-In)
   - **Anonymous** (for guest users)
   - **Email/Password** (optional)

### 📊 **Analytics & Performance (Optional)**
1. **Analytics**: Already enabled if you chose it during project creation
2. **Performance Monitoring**: Build → Performance → Get started
3. **Crashlytics**: Build → Crashlytics → Get started

---

## Step 5: Configure Gemini API Access

### 📋 **Get Your Gemini API Key**
1. Go to [Google AI Studio](https://makersuite.google.com/app/apikey)
2. **Create API key** or use existing
3. **Copy the API key**

### 🔧 **Add to Firebase Project**
1. Firebase Console → **Project Settings** → **General**
2. Scroll to **Your project** section
3. **Add the API key to your app configuration**

### 🔒 **Secure API Key Storage**
For production, store the API key securely:

```kotlin
// In your app - Use Firebase Remote Config or secure storage
// Do NOT hardcode the API key in your app
const val GEMINI_API_KEY = BuildConfig.GEMINI_API_KEY // From build config
```

---

## Step 6: Test Firebase Integration

After completing the setup:

1. **Sync your project**: 
   ```bash
   ./gradlew sync
   ```

2. **Build and run**:
   ```bash
   ./gradlew assembleDebug
   ```

3. **Test Voice Chat**:
   - Open the app
   - Navigate to voice chat
   - Try starting a live session
   - Check Firebase Console for activity

---

## Step 7: Production Setup (When Ready)

### 🚀 **Release SHA-1**
Generate release keystore and SHA-1:
```bash
keytool -genkey -v -keystore release.keystore -alias release -keyalg RSA -keysize 2048 -validity 10000
keytool -list -v -keystore release.keystore -alias release
```

### 🏭 **Production Firebase Project**
1. Create separate Firebase project for production
2. Use different package name: `com.craftflowtechnologies.guidelens`
3. Add release SHA-1 to Firebase Console
4. Download production `google-services.json`

### 🔐 **Security Rules & Rate Limiting**
Configure Firebase security rules and rate limiting for production use.

---

## Troubleshooting

### ❌ **"google-services.json not found"**
- Ensure file is in `app/` directory (not root)
- File name must be exactly `google-services.json`
- Clean and rebuild project

### ❌ **"Firebase AI Logic not available"**
- Check if Vertex AI is enabled in Firebase Console
- Verify your Firebase project supports the feature
- Check if you're in a supported region

### ❌ **"Authentication failed"**
- Verify SHA-1 fingerprint is correctly added
- Check package name matches exactly
- Ensure Google Sign-In is enabled if using auth

---

## Next Steps After Setup

1. **Test voice functionality** with different agents
2. **Monitor usage** in Firebase Analytics  
3. **Set up error tracking** with Crashlytics
4. **Configure performance monitoring**
5. **Plan production deployment** strategy

---

## Support

- **Firebase Documentation**: https://firebase.google.com/docs
- **Vertex AI in Firebase**: https://firebase.google.com/docs/vertex-ai
- **Gemini API Documentation**: https://ai.google.dev/docs

---

**Status**: ⏳ Waiting for `google-services.json` file from Firebase Console