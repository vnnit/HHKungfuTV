package com.hhkungfu.tv.ui.screens.player

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hhkungfu.tv.data.history.HistoryManager
import com.hhkungfu.tv.ui.components.FocusableTvItem
import com.hhkungfu.tv.ui.theme.NetflixGold
import com.hhkungfu.tv.ui.theme.NetflixRed
import com.hhkungfu.tv.ui.theme.TextPrimary
import com.hhkungfu.tv.ui.theme.TextSecondary
import com.hhkungfu.tv.utils.Constants
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TvPlayerScreen(
    postId: String,
    chapterSt: String,
    movieTitle: String,
    episodeName: String,
    serverType: String,
    sv: String = "1",
    onBackClick: () -> Unit,
    viewModel: PlayerViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showControls by remember { mutableStateOf(false) }
    var seekMessage by remember { mutableStateOf<String?>(null) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Injected JavaScript that directly hooks HTML5 video and starts playback via code without user gesture
    val autoPlayScript = """
        (function() {
            function forcePlay() {
                var vids = document.getElementsByTagName('video');
                for (var i = 0; i < vids.length; i++) {
                    var v = vids[i];
                    if (v && v.paused) {
                        v.autoplay = true;
                        v.muted = false;
                        var p = v.play();
                        if (p !== undefined) {
                            p.catch(function() {
                                v.muted = true;
                                v.play().then(function() {
                                    setTimeout(function() { v.muted = false; }, 300);
                                });
                            });
                        }
                    }
                }
                var btn = document.querySelector('button.play, [aria-label*="Play"], .vjs-big-play-button, .jw-display-icon-container, .art-state');
                if (btn) {
                    try { btn.click(); } catch(e) {}
                }
            }
            forcePlay();
            setInterval(forcePlay, 300);
        })();
    """.trimIndent()

    fun pressDownThenEnter() {
        val webView = webViewRef ?: return
        webView.post {
            try {
                webView.requestFocus()
                val now = SystemClock.uptimeMillis()
                webView.dispatchKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_DOWN, 0))
                webView.dispatchKeyEvent(KeyEvent(now, now + 40, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_DOWN, 0))

                webView.postDelayed({
                    try {
                        val enterTime = SystemClock.uptimeMillis()
                        webView.dispatchKeyEvent(KeyEvent(enterTime, enterTime, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER, 0))
                        webView.dispatchKeyEvent(KeyEvent(enterTime, enterTime + 40, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_CENTER, 0))
                        webView.dispatchKeyEvent(KeyEvent(enterTime, enterTime, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER, 0))
                        webView.dispatchKeyEvent(KeyEvent(enterTime, enterTime + 40, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER, 0))
                        webView.dispatchKeyEvent(KeyEvent(enterTime, enterTime, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY, 0))
                        webView.dispatchKeyEvent(KeyEvent(enterTime, enterTime + 40, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY, 0))

                        webView.evaluateJavascript(autoPlayScript, null)
                    } catch (e: Exception) {
                        Log.e("TvPlayer", "Error in postDelayed enter", e)
                    }
                }, 200)
            } catch (e: Exception) {
                Log.e("TvPlayer", "Error pressDownThenEnter", e)
            }
        }
    }

    fun sendKeyEvent(keyCode: Int) {
        val webView = webViewRef ?: return
        val downTime = SystemClock.uptimeMillis()
        val down = KeyEvent(downTime, downTime, KeyEvent.ACTION_DOWN, keyCode, 0)
        val up = KeyEvent(downTime, downTime + 30, KeyEvent.ACTION_UP, keyCode, 0)
        webView.dispatchKeyEvent(down)
        webView.dispatchKeyEvent(up)
    }

    fun seek(seconds: Int) {
        if (seconds > 0) {
            sendKeyEvent(KeyEvent.KEYCODE_DPAD_RIGHT)
            sendKeyEvent(KeyEvent.KEYCODE_MEDIA_FAST_FORWARD)
        } else {
            sendKeyEvent(KeyEvent.KEYCODE_DPAD_LEFT)
            sendKeyEvent(KeyEvent.KEYCODE_MEDIA_REWIND)
        }
        seekMessage = if (seconds > 0) "+${seconds}s ⏩" else "${seconds}s ⏪"
    }

    LaunchedEffect(postId, chapterSt, serverType, sv) {
        viewModel.loadStream(postId, chapterSt, movieTitle, episodeName, serverType, sv)
        HistoryManager.saveWatchHistory(
            context = context,
            movieUrl = postId,
            movieTitle = movieTitle,
            posterUrl = "",
            episodeSlug = chapterSt,
            episodeName = episodeName,
            sv = sv
        )
    }

    // Auto-hide seek message after 1.5s
    LaunchedEffect(seekMessage) {
        if (seekMessage != null) {
            delay(1500)
            seekMessage = null
        }
    }

    // Auto-hide controls after 4 seconds
    LaunchedEffect(showControls) {
        if (showControls) {
            delay(4000)
            showControls = false
        }
    }

    BackHandler {
        onBackClick()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_CENTER,
                        KeyEvent.KEYCODE_ENTER,
                        KeyEvent.KEYCODE_NUMPAD_ENTER,
                        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                        KeyEvent.KEYCODE_MEDIA_PLAY,
                        KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                            pressDownThenEnter()
                            true
                        }

                        KeyEvent.KEYCODE_DPAD_LEFT,
                        KeyEvent.KEYCODE_MEDIA_REWIND -> {
                            seek(-10)
                            true
                        }

                        KeyEvent.KEYCODE_DPAD_RIGHT,
                        KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                            seek(10)
                            true
                        }

                        KeyEvent.KEYCODE_DPAD_UP,
                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                            showControls = !showControls
                            true
                        }

                        else -> false
                    }
                } else false
            }
    ) {
        LaunchedEffect(Unit) {
            try {
                focusRequester.requestFocus()
            } catch (_: Exception) {}
        }

        when (val state = uiState) {
            is PlayerUiState.Loading -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        color = NetflixRed,
                        modifier = Modifier.size(54.dp),
                        strokeWidth = 4.dp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "⚡ Đang kết nối: $episodeName (${if (sv == "2") "Thuyết Minh" else "Việt Sub"})...",
                        color = TextSecondary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            is PlayerUiState.Error -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = state.message,
                        color = NetflixRed,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        FocusableTvItem(
                            onClick = {
                                viewModel.loadStream(postId, chapterSt, movieTitle, episodeName, serverType, sv)
                            }
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(NetflixRed)
                                    .padding(horizontal = 20.dp, vertical = 8.dp)
                            ) {
                                Text(text = "Thử lại", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }

                        FocusableTvItem(onClick = onBackClick) {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF333333))
                                    .padding(horizontal = 20.dp, vertical = 8.dp)
                            ) {
                                Text(text = "Quay lại", color = Color.White)
                            }
                        }
                    }
                }
            }

            is PlayerUiState.Ready -> {
                val stream = state.streamSource

                // Fullscreen TV Native Direct WebView Player (Directly loads embed URL with Referer, no cross-origin iframe barrier)
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            setLayerType(View.LAYER_TYPE_HARDWARE, null)
                            isFocusable = true
                            isFocusableInTouchMode = true
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                databaseEnabled = true
                                mediaPlaybackRequiresUserGesture = false
                                allowFileAccess = true
                                allowContentAccess = true
                                loadWithOverviewMode = true
                                useWideViewPort = true
                                userAgentString = Constants.USER_AGENT
                                cacheMode = WebSettings.LOAD_DEFAULT
                                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                setSupportMultipleWindows(false)
                                javaScriptCanOpenWindowsAutomatically = true
                            }

                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                    return false
                                }

                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    super.onPageStarted(view, url, favicon)
                                    view?.evaluateJavascript(autoPlayScript, null)
                                }

                                override fun onLoadResource(view: WebView?, url: String?) {
                                    super.onLoadResource(view, url)
                                    view?.evaluateJavascript(autoPlayScript, null)
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    view?.evaluateJavascript(autoPlayScript, null)

                                    scope.launch {
                                        delay(800)
                                        pressDownThenEnter()
                                        delay(1200)
                                        pressDownThenEnter()
                                    }
                                }

                                override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                                    super.onReceivedError(view, request, error)
                                    Log.e("TvPlayer", "WebView error: ${error?.description}")
                                }
                            }

                            webChromeClient = object : WebChromeClient() {
                                override fun getDefaultVideoPoster(): Bitmap? {
                                    return Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
                                }
                            }

                            val directUrl = if (stream.embedUrl.contains("?")) "${stream.embedUrl}&autoplay=1" else "${stream.embedUrl}?autoplay=1"
                            val headers = mapOf("Referer" to "${Constants.BASE_URL}/")
                            loadUrl(directUrl, headers)
                            webViewRef = this
                            requestFocus()
                        }
                    },
                    update = { view ->
                        val directUrl = if (stream.embedUrl.contains("?")) "${stream.embedUrl}&autoplay=1" else "${stream.embedUrl}?autoplay=1"
                        val headers = mapOf("Referer" to "${Constants.BASE_URL}/")
                        view.loadUrl(directUrl, headers)
                        view.requestFocus()
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Seek Message Pop-up
                if (seekMessage != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 60.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xEE141414))
                                .padding(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = seekMessage ?: "",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // TV Player Overlay Controls (Top bar & Bottom bar)
                AnimatedVisibility(
                    visible = showControls,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xDD000000),
                                        Color.Transparent,
                                        Color(0xF0000000)
                                    )
                                )
                            )
                    ) {
                        // Top Header Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 36.dp, vertical = 20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                FocusableTvItem(
                                    onClick = onBackClick,
                                    shape = RoundedCornerShape(50),
                                    focusedScale = 1.1f
                                ) { isFocused ->
                                    Box(
                                        modifier = Modifier
                                            .background(if (isFocused) NetflixRed else Color(0x66000000))
                                            .padding(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Quay lại",
                                            tint = Color.White
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column {
                                    Text(
                                        text = state.movieTitle,
                                        color = TextPrimary,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = state.episodeName,
                                            color = NetflixGold,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(if (state.sv == "2") NetflixRed else Color(0xFF3B82F6))
                                                .padding(horizontal = 5.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = if (state.sv == "2") "THUYẾT MINH" else "VIỆT SUB",
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            // Quick Controls on Top Right
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                FocusableTvItem(
                                    onClick = { seek(-10) },
                                    shape = RoundedCornerShape(6.dp),
                                    focusedScale = 1.08f
                                ) { isFocused ->
                                    Box(
                                        modifier = Modifier
                                            .background(if (isFocused) NetflixRed else Color(0x66333333))
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.FastRewind, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("-10s", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                FocusableTvItem(
                                    onClick = { pressDownThenEnter() },
                                    shape = RoundedCornerShape(6.dp),
                                    focusedScale = 1.08f
                                ) { isFocused ->
                                    Box(
                                        modifier = Modifier
                                            .background(if (isFocused) NetflixRed else Color(0x66333333))
                                            .padding(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.PlayArrow,
                                                null,
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                "Phát / Dừng",
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                FocusableTvItem(
                                    onClick = { seek(10) },
                                    shape = RoundedCornerShape(6.dp),
                                    focusedScale = 1.08f
                                ) { isFocused ->
                                    Box(
                                        modifier = Modifier
                                            .background(if (isFocused) NetflixRed else Color(0x66333333))
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("+10s", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Spacer(Modifier.width(4.dp))
                                            Icon(Icons.Default.FastForward, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }

                        // Bottom Server Switcher & Remote Guide
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth()
                                .padding(horizontal = 36.dp, vertical = 20.dp)
                        ) {
                            // Server options
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.padding(bottom = 10.dp)
                            ) {
                                Text(
                                    text = "Đổi Chất Lượng:",
                                    color = TextSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                viewModel.serverOptions.forEach { server ->
                                    val isSelected = server.type == state.currentServer
                                    FocusableTvItem(
                                        onClick = { viewModel.switchServer(server.type) },
                                        shape = RoundedCornerShape(6.dp),
                                        focusedScale = 1.05f
                                    ) { isFocused ->
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    when {
                                                        isFocused -> NetflixRed
                                                        isSelected -> Color(0xFF444444)
                                                        else -> Color(0xFF1E1E1E)
                                                    }
                                                )
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = server.displayName,
                                                color = if (isSelected || isFocused) Color.White else TextSecondary,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    }
                                }
                            }

                            // Remote instructions bar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🎮 Điều khiển Remote: [OK/Center] Tạm dừng/Phát  •  [←/→] Tua ±10s  •  [↑/↓] Ẩn/Hiện thanh điều khiển",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                webViewRef?.stopLoading()
                webViewRef?.destroy()
                webViewRef = null
            } catch (_: Exception) {}
        }
    }
}
