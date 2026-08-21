package com.hhkungfu.tv.utils

import android.view.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.NativeKeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.type

object DpadHelper {
    fun isDpadEvent(event: androidx.compose.ui.input.key.KeyEvent): Boolean {
        val keyCode = event.nativeKeyEvent.keyCode
        return keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
               keyCode == KeyEvent.KEYCODE_DPAD_UP ||
               keyCode == KeyEvent.KEYCODE_DPAD_DOWN ||
               keyCode == KeyEvent.KEYCODE_DPAD_LEFT ||
               keyCode == KeyEvent.KEYCODE_DPAD_RIGHT ||
               keyCode == KeyEvent.KEYCODE_ENTER ||
               keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER ||
               keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE ||
               keyCode == KeyEvent.KEYCODE_MEDIA_PLAY ||
               keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE
    }

    fun isSelectKey(keyCode: Int): Boolean {
        return keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
               keyCode == KeyEvent.KEYCODE_ENTER ||
               keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER
    }
}
