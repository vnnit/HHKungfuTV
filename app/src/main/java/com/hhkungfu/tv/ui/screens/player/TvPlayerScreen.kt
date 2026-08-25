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

    fun generateEmbedHtml(embedUrl: String): String {
        val fullUrl = if (embedUrl.contains("?")) {
            "$embedUrl&autoplay=1&autoPlay=true"
        } else {
            "$embedUrl?autoplay=1&autoPlay=true"
        }

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                <style>
                    * { box-sizing: border-box; margin: 0; padding: 0; }
                    html, body {
                        width: 100vw;
                        height: 100vh;
                        background-color: #000000;
                        overflow: hidden;
                    }
                    iframe {
                        position: absolute;
                        top: 0;
                        left: 0;
                        width: 100%;
                        height: 100%;
                        border: 0;
                    }
                </style>
            </head>
            <body>
                <iframe 
                    id="player-iframe" 
                    src="$fullUrl" 
                    referrerPolicy="unsafe-url" 
                    scrolling="no" 
                    frameborder="0" 
                    width="100%" 
                    height="100%" 
                    allowfullscreen="true" 
                    webkitallowfullscreen="true" 
                    mozallowfullscreen="true" 
                    allow="autoplay *; fullscreen *; encrypted-media *; picture-in-picture *">
                </iframe>
                <script>
                    function enforceMaxVolume() {
                        try {
                            localStorage.setItem('volume', '1');
                            localStorage.setItem('player_volume', '1');
                            localStorage.setItem('artplayer_volume', '1');
                            localStorage.setItem('artplayer_settings', JSON.stringify({ volume: 1 }));
                            localStorage.setItem('jwplayer.volume', '100');
                            localStorage.setItem('plyr_volume', '1');
                            localStorage.setItem('dplayer-volume', '1');
                        } catch(e) {}

                        var vids = document.getElementsByTagName('video');
                        for (var i = 0; i < vids.length; i++) {
                            try {
                                if (vids[i].volume < 1.0) vids[i].volume = 1.0;
                                if (vids[i].muted) vids[i].muted = false;
                            } catch(e) {}
                        }

                        var ifr = document.getElementById('player-iframe');
                        if (ifr && ifr.contentWindow) {
                            try {
                                var doc = ifr.contentDocument || ifr.contentWindow.document;
                                if (doc) {
                                    var subVids = doc.getElementsByTagName('video');
                                    for (var j = 0; j < subVids.length; j++) {
                                        if (subVids[j].volume < 1.0) subVids[j].volume = 1.0;
                                        if (subVids[j].muted) subVids[j].muted = false;
                                    }
                                }
                            } catch(e) {}
                        }
                    }

                    window.addEventListener('load', function() {
                        enforceMaxVolume();
                        setInterval(enforceMaxVolume, 500);
                    });
                    document.addEventListener('DOMContentLoaded', function() {
                        enforceMaxVolume();
                        setInterval(enforceMaxVolume, 500);
                    });
                    setInterval(enforceMaxVolume, 500);
                </script>
            </body>
            </html>
        """.trimIndent()
    }

    // Exact Remote TV Sequence: Press DOWN to move focus onto Play Triangle -> Wait 250ms -> Press ENTER to Play
    fun pressDownThenEnter() {
        val webView = webViewRef ?: return
        webView.post {
            try {
                webView.requestFocus()

                // 1. Send DPAD_DOWN to move focus from top text down onto the Play triangle
                val now = SystemClock.uptimeMillis()
                webView.dispatchKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_DOWN, 0))
                webView.dispatchKeyEvent(KeyEvent(now, now + 40, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_DOWN, 0))

                // 2. Wait 250ms for Android TV focus box to settle on the Play button, then press ENTER / DPAD_CENTER
                webView.postDelayed({
                    try {
                        val enterTime = SystemClock.uptimeMillis()
                        webView.dispatchKeyEvent(KeyEvent(enterTime, enterTime, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER, 0))
                        webView.dispatchKeyEvent(KeyEvent(enterTime, enterTime + 40, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_CENTER, 0))
                        webView.dispatchKeyEvent(KeyEvent(enterTime, enterTime, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER, 0))
                        webView.dispatchKeyEvent(KeyEvent(enterTime, enterTime + 40, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER, 0))
                        webView.dispatchKeyEvent(KeyEvent(enterTime, enterTime, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY, 0))
                        webView.dispatchKeyEvent(KeyEvent(enterTime, enterTime + 40, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY, 0))

                        // Touch center fallback
                        val width = if (webView.width > 0) webView.width.toFloat() else webView.resources.displayMetrics.widthPixels.toFloat()
                        val height = if (webView.height > 0) webView.height.toFloat() else webView.resources.displayMetrics.heightPixels.toFloat()
                        val down = MotionEvent.obtain(enterTime, enterTime, MotionEvent.ACTION_DOWN, width / 2f, height / 2f, 0)
                        val up = MotionEvent.obtain(enterTime, enterTime + 40, MotionEvent.ACTION_UP, width / 2f, height / 2f, 0)
                        webView.dispatchTouchEvent(down)
                        webView.dispatchTouchEvent(up)
                        down.recycle()
                        up.recycle()

                        // Direct JS play & Volume 100% Lock
                        val js = """
                            (function() {
                                try {
                                    localStorage.setItem('volume', '1');
                                    localStorage.setItem('artplayer_volume', '1');
                                    localStorage.setItem('artplayer_settings', JSON.stringify({ volume: 1 }));
                                    localStorage.setItem('jwplayer.volume', '100');
                                    localStorage.setItem('plyr_volume', '1');
                                } catch(e) {}
                                var vids = document.querySelectorAll('video');
                                for (var i = 0; i < vids.length; i++) {
                                    try { 
                                        vids[i].muted = false; 
                                        vids[i].volume = 1.0;
                                        vids[i].play(); 
                                    } catch(e) {}
                                }
                            })();
                        """.trimIndent()
                        webView.evaluateJavascript(js, null)
                    } catch (e: Exception) {
                        Log.e("TvPlayer", "Error in postDelayed enter", e)
                    }
                }, 250)
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

                // Fullscreen TV WebView Player
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

                                override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                                    super.onReceivedError(view, request, error)
                                    Log.e("TvPlayer", "WebView error: ${error?.description}")
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    val lockVolJs = """
                                        (function() {
                                            try {
                                                localStorage.setItem('volume', '1');
                                                localStorage.setItem('player_volume', '1');
                                                localStorage.setItem('artplayer_volume', '1');
                                                localStorage.setItem('artplayer_settings', JSON.stringify({ volume: 1 }));
                                                localStorage.setItem('jwplayer.volume', '100');
                                                localStorage.setItem('plyr_volume', '1');
                                            } catch(e) {}
                                            var vids = document.querySelectorAll('video');
                                            for (var i = 0; i < vids.length; i++) {
                                                try {
                                                    vids[i].volume = 1.0;
                                                    vids[i].muted = false;
                                                } catch(e) {}
                                            }
                                        })();
                                    """.trimIndent()
                                    view?.evaluateJavascript(lockVolJs, null)
                                }
                            }

                            webChromeClient = object : WebChromeClient() {
                                override fun getDefaultVideoPoster(): Bitmap? {
                                    return Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
                                }
                            }

                            val embedHtml = generateEmbedHtml(stream.embedUrl)
                            loadDataWithBaseURL(
                                Constants.BASE_URL + "/",
                                embedHtml,
                                "text/html",
                                "UTF-8",
                                Constants.BASE_URL + "/"
                            )
                            webViewRef = this
                            requestFocus()
                        }
                    },
                    update = { view ->
                        val embedHtml = generateEmbedHtml(stream.embedUrl)
                        view.loadDataWithBaseURL(
                            Constants.BASE_URL + "/",
                            embedHtml,
                            "text/html",
                            "UTF-8",
                            Constants.BASE_URL + "/"
                        )
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
