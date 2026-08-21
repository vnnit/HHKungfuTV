package com.hhkungfu.tv.utils

object Constants {
    const val BASE_URL = "https://hhkungfu.ee"
    const val USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    
    // Server types
    const val SERVER_PRO = "pro"       // 1080P V2
    const val SERVER_TIKTIK = "tiktik" // 1080P V1
    const val SERVER_VIP4K = "vip4k"   // 4K V1
    const val SERVER_VIP4KV2 = "vip4kv2" // 4K V2
    
    // Seek time in milliseconds
    const val SEEK_STEP_MS = 10000L
}
