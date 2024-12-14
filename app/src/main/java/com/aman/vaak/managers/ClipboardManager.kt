package com.aman.vaak.managers

import com.aman.vaak.repositories.ClipboardRepository
import javax.inject.Inject

class ClipboardManager @Inject constructor(
    private val repository: ClipboardRepository
) {
    fun pasteContent(): String? = 
        repository.getClipboardText()
}
