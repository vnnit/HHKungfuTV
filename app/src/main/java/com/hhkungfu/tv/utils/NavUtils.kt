package com.hhkungfu.tv.utils

import android.util.Base64
import java.nio.charset.StandardCharsets

object NavUtils {
    fun encode(value: String): String {
        return Base64.encodeToString(
            value.toByteArray(StandardCharsets.UTF_8),
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )
    }

    fun decode(value: String): String {
        return try {
            String(
                Base64.decode(value, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING),
                StandardCharsets.UTF_8
            )
        } catch (_: Exception) {
            value
        }
    }
}
