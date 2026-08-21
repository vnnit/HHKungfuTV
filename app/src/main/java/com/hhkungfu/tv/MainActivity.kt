package com.hhkungfu.tv

import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import coil.Coil
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.hhkungfu.tv.ui.navigation.AppNavigation
import com.hhkungfu.tv.ui.theme.HHKungfuTVTheme
import com.hhkungfu.tv.ui.theme.NetflixBlack
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Catch global uncaught exceptions to prevent sudden force closes
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("HHKungfuApp", "Uncaught exception in thread ${thread.name}", throwable)
        }

        // Keep screen on for continuous video watching
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Configure custom high-performance Coil image loader with memory & disk cache
        try {
            val imageLoader = ImageLoader.Builder(this)
                .memoryCache {
                    MemoryCache.Builder(this)
                        .maxSizePercent(0.25)
                        .build()
                }
                .diskCache {
                    DiskCache.Builder()
                        .directory(cacheDir.resolve("image_cache"))
                        .maxSizeBytes(100L * 1024 * 1024) // 100 MB
                        .build()
                }
                .okHttpClient {
                    OkHttpClient.Builder()
                        .connectTimeout(10, TimeUnit.SECONDS)
                        .readTimeout(10, TimeUnit.SECONDS)
                        .build()
                }
                .crossfade(true)
                .build()
            Coil.setImageLoader(imageLoader)
        } catch (e: Exception) {
            Log.e("HHKungfuApp", "Failed to setup Coil ImageLoader", e)
        }

        setContent {
            HHKungfuTVTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = NetflixBlack
                ) {
                    AppNavigation()
                }
            }
        }
    }
}
