package com.aman.vaak.managers

import android.content.ContentResolver
import android.provider.Settings
import javax.inject.Inject

interface SystemManager {
    /**
     * Gets currently selected input method from system settings
     * @return Package name of current input method or null if none selected
     */
    fun getDefaultInputMethod(): String?
}

class SystemManagerImpl @Inject constructor(
    private val contentResolver: ContentResolver
) : SystemManager {
    override fun getDefaultInputMethod(): String? =
        Settings.Secure.getString(
            contentResolver,
            Settings.Secure.DEFAULT_INPUT_METHOD
        )
}
