package com.aritr.zinely.render.android

import android.content.Context
import androidx.emoji2.bundled.BundledEmojiCompatConfig
import androidx.emoji2.text.EmojiCompat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Zinely-owned emoji rendering (ADR-112).
 *
 * The bundled Emoji2 font is local and fixed; [setReplaceAll] prevents a newer OEM font from silently
 * changing preview or print output. [SharedTextLayout] is the only consumer, so the same processed spans
 * reach editor preview, PNG and vector PDF through the existing shared Canvas replay path.
 */
public object EmojiRendering {
    @Volatile
    private var initialized: Boolean = false

    /** Starts the local font load once. Safe and cheap to call from [android.app.Application.onCreate]. */
    public fun initialize(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            val executor = Executors.newSingleThreadExecutor { task ->
                Thread(task, "zinely-emoji-font").apply { isDaemon = true }
            }
            val config = BundledEmojiCompatConfig(context.applicationContext, executor)
                .setReplaceAll(true)
                .registerInitCallback(ShutdownExecutorCallback(executor))
            EmojiCompat.init(config)
            initialized = true
        }
    }

    /**
     * Adds bundled Emoji spans once the local metadata is ready. Loading/failed states return the source
     * unchanged instead of throwing; startup begins loading before the first activity, and export is
     * exercised only after the application has reached an interactive state.
     */
    public fun process(text: String): CharSequence {
        if (!initialized || !EmojiCompat.isConfigured()) return text
        val compat = EmojiCompat.get()
        return if (compat.loadState == EmojiCompat.LOAD_STATE_SUCCEEDED) {
            compat.process(text, 0, text.length, Int.MAX_VALUE, EmojiCompat.REPLACE_STRATEGY_ALL) ?: text
        } else {
            text
        }
    }

    private class ShutdownExecutorCallback(
        private val executor: ExecutorService,
    ) : EmojiCompat.InitCallback() {
        override fun onInitialized() {
            executor.shutdown()
        }

        override fun onFailed(throwable: Throwable?) {
            executor.shutdown()
        }
    }
}
