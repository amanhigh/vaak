package com.aman.vaak.managers

import com.aman.vaak.repositories.ClipboardRepository
import javax.inject.Inject

/** Interface for managing clipboard operations. */
interface ClipboardManager {
    /**
     * Retrieves the content from the clipboard.
     *
     * @return The clipboard content as a String, or null if the clipboard is empty.
     */
    fun pasteContent(): String?
}

class ClipboardManagerImpl @Inject constructor(private val repository: ClipboardRepository) :
        ClipboardManager {
    override fun pasteContent(): String? = repository.getClipboardText()
}
