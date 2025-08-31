package com.craftflowtechnologies.guidelens

import android.app.Application
import android.util.Log
import com.clerk.api.Clerk

class GuideLensApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.d("GuideLensApp", "Application initialized successfully")
        
        // Initialize Clerk authentication
        Clerk.initialize(
            this,
            publishableKey = "pk_test_YWJzb2x1dGUtc25haWwtNC5jbGVyay5hY2NvdW50cy5kZXYk"
        )
        Log.d("GuideLensApp", "Clerk initialized successfully")
        
        // Supabase initialization temporarily disabled for build stability
    }
}