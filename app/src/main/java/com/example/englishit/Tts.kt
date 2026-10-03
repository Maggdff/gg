package com.example.englishit

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

object Tts {
    private var tts: TextToSpeech? = null
    private var ready = false

    fun init(ctx: Context) {
        if (tts != null) return
        tts = TextToSpeech(ctx.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                ready = true
            }
        }
    }

    fun speak(text: String) {
        if (ready && text.isNotBlank()) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "u")
        }
    }
}
