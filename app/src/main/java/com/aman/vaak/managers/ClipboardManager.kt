package com.aman.vaak.managers

import javax.inject.Inject

interface ClipboardManager : BaseInputManager {
    /**
     * Copies the currently selected text to clipboard
     * @return True if the copy was successful, false otherwise
     * @throws InputNotConnectedException if no input connection available
     */
    fun copySelectedText(): Boolean

    /**
     * Pastes text from clipboard at current cursor position
     * @return True if the paste was successful, false otherwise
     * @throws InputNotConnectedException if no input connection available
     */
    fun pasteText(): Boolean
}

class ClipboardManagerImpl
    @Inject
    constructor() : BaseInputManagerImpl(), ClipboardManager {
        override fun copySelectedText(): Boolean {
            return requireInputConnection().performContextMenuAction(android.R.id.copy)
        }

        override fun pasteText(): Boolean {
            return requireInputConnection().performContextMenuAction(android.R.id.paste)
        }
    }
