package com.hhkungfu.tv.data.updater

import android.content.Context
import android.content.Intent
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
    private const val UPDATE_JSON_URL = "https://app.plzmail.net/version.json"
    private const val UPDATE_JSON_ALT = "https://app.plzmail.net/update.json"
    const val DEFAULT_APK_URL = "https://app.plzmail.net/HHKungfuTV.apk"

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

        // 1. Try version.json or update.json
        val jsonUrls = listOf(UPDATE_JSON_URL, UPDATE_JSON_ALT)
        for (url in jsonUrls) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0")
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val updateInfo = Gson().fromJson(body, UpdateInfo::class.java)
                    if (updateInfo != null && updateInfo.versionCode > currentVersionCode) {
                        val finalApkUrl = if (updateInfo.apkUrl.isNotEmpty()) updateInfo.apkUrl else DEFAULT_APK_URL
                        return@withContext updateInfo.copy(apkUrl = finalApkUrl)
                    }
                }
            } catch (e: Exception) {
                Log.d("AppUpdater", "Check json update from $url failed: ${e.message}")
            }
        }

        // 2. Fallback: check if direct APK file exists on server
        val directApkUrls = listOf(
            "https://app.plzmail.net/HHKungfuTV.apk",
            "https://app.plzmail.net/app.apk",
            "https://app.plzmail.net/app-release.apk"
        )
        for (apkUrl in directApkUrls) {
            try {
                val request = Request.Builder()
                    .url(apkUrl)
                    .head()
                    .header("User-Agent", "Mozilla/5.0")
                    .build()
                val response = client.newCall(request).execute()
                if (response.isSuccessful && (response.body?.contentLength() ?: 0) > 1000000) {
                    return@withContext UpdateInfo(
                        versionCode = currentVersionCode + 1,
                        versionName = "Bản mới nhất",
                        apkUrl = apkUrl,
                        changelog = "Cập nhật ứng dụng từ server plzmail"
                    )
                }
            } catch (_: Exception) {}
        }

        return@withContext null
    }

    suspend fun downloadAndInstallApk(
        context: Context,
        apkUrl: String = DEFAULT_APK_URL,
        onProgress: (Int) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(apkUrl)
                .header("User-Agent", "Mozilla/5.0")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext false

            val body = response.body ?: return@withContext false
            val contentLength = body.contentLength()

            val apkFile = File(context.cacheDir, "update.apk")
            if (apkFile.exists()) apkFile.delete()

            val inputStream = body.byteStream()
            val outputStream = FileOutputStream(apkFile)

            val buffer = ByteArray(16 * 1024)
            var bytesRead: Int
            var totalBytesRead = 0L

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead
                if (contentLength > 0) {
                    val progress = ((totalBytesRead * 100) / contentLength).toInt()
                    withContext(Dispatchers.Main) {
                        onProgress(progress)
                    }
                }
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()

            // Trigger installation
            withContext(Dispatchers.Main) {
                installApk(context, apkFile)
            }
            return@withContext true
        } catch (e: Exception) {
            Log.e("AppUpdater", "Download update failed", e)
            return@withContext false
        }
    }

    fun installApk(context: Context, apkFile: File) {
        try {
            val apkUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("AppUpdater", "Install APK failed", e)
        }
    }
}
