package com.aman.vaak.repositories

import android.content.ClipboardManager
import javax.inject.Inject

/**
 * Repository interface for accessing system clipboard.
 */
interface ClipboardRepository {
    /**
     * Retrieves text from the system clipboard.
     *
     * @return Current text from clipboard, or null if:
     *         - Clipboard is empty
     *         - Clipboard contains non-text data
     *         - System clipboard is unavailable
     */
    fun getClipboardText(): String?
}

class ClipboardRepositoryImpl
    @Inject
    constructor(
        private val clipboardManager: ClipboardManager,
    ) : ClipboardRepository {
        override fun getClipboardText(): String? {
            return clipboardManager.primaryClip?.getItemAt(0)?.text?.toString()
        }
    }
