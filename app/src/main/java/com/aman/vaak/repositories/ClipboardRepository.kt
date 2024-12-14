package com.aman.vaak.repositories

import android.content.ClipboardManager
import android.content.Context
import javax.inject.Inject
import dagger.hilt.android.qualifiers.ApplicationContext

class ClipboardRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val clipboard: ClipboardManager = 
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    fun getClipboardText(): String? =
        clipboard.primaryClip?.getItemAt(0)?.text?.toString()
}
