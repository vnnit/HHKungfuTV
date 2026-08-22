package com.hhkungfu.tv.data.updater

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

data class UpdateInfo(
    @SerializedName("versionCode") val versionCode: Int = 0,
    @SerializedName("versionName") val versionName: String = "",
    @SerializedName("apkUrl") val apkUrl: String = "",
    @SerializedName("changelog") val changelog: String = ""
)

object AppUpdater {
    // Primary: GitHub Raw version.json
    private const val GITHUB_VERSION_JSON = "https://raw.githubusercontent.com/vnnit/HHKungfuTV/main/version.json"
    private const val GITHUB_RELEASE_JSON = "https://github.com/vnnit/HHKungfuTV/releases/latest/download/version.json"
    private const val FALLBACK_JSON = "https://app.plzmail.net/version.json"

    const val DEFAULT_APK_URL = "https://github.com/vnnit/HHKungfuTV/releases/latest/download/HHKungfuTV.apk"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    fun getCurrentVersionCode(context: Context): Int {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0).versionCode
            }
        } catch (_: Exception) {
            1
        }
    }

    suspend fun checkForUpdate(context: Context): UpdateInfo? = withContext(Dispatchers.IO) {
        val currentVersionCode = getCurrentVersionCode(context)

        // 1. Try GitHub Raw, GitHub Release, and fallback
        val jsonUrls = listOf(GITHUB_VERSION_JSON, GITHUB_RELEASE_JSON, FALLBACK_JSON)
        for (url in jsonUrls) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0")
                    .header("Cache-Control", "no-cache")
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrEmpty()) {
                        val updateInfo = Gson().fromJson(body, UpdateInfo::class.java)
                        if (updateInfo != null && updateInfo.versionCode > currentVersionCode) {
                            val apkUrl = if (updateInfo.apkUrl.isNotEmpty()) updateInfo.apkUrl else DEFAULT_APK_URL
                            return@withContext updateInfo.copy(apkUrl = apkUrl)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("AppUpdater", "Error checking update from $url: ${e.message}")
            }
        }

        return@withContext null
    }

    suspend fun downloadAndInstallApk(
        context: Context,
        apkUrl: String,
        onProgress: (Int) -> Unit = {}
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val targetUrl = apkUrl.ifEmpty { DEFAULT_APK_URL }
            val request = Request.Builder()
                .url(targetUrl)
                .header("User-Agent", "Mozilla/5.0")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext false

            val body = response.body ?: return@withContext false
            val contentLength = body.contentLength()

            val downloadsDir = File(context.cacheDir, "updates")
            if (!downloadsDir.exists()) downloadsDir.mkdirs()

            val apkFile = File(downloadsDir, "update.apk")
            if (apkFile.exists()) apkFile.delete()

            body.byteStream().use { input ->
                FileOutputStream(apkFile).use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var bytesRead: Int
                    var totalBytesRead = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead
                        if (contentLength > 0) {
                            val progress = ((totalBytesRead * 100) / contentLength).toInt()
                            withContext(Dispatchers.Main) {
                                onProgress(progress)
                            }
                        }
                    }
                    output.flush()
                }
            }

            withContext(Dispatchers.Main) {
                installApk(context, apkFile)
            }
            return@withContext true
        } catch (e: Exception) {
            Log.e("AppUpdater", "Error downloading APK", e)
            return@withContext false
        }
    }

    private fun installApk(context: Context, apkFile: File) {
        try {
            apkFile.setReadable(true, false)
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val resInfoList = context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            for (resolveInfo in resInfoList) {
                val targetPkg = resolveInfo.activityInfo.packageName
                context.grantUriPermission(targetPkg, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("AppUpdater", "Error starting APK installation intent", e)
        }
    }
}
