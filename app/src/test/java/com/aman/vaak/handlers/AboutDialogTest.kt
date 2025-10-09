package com.aman.vaak.handlers

import com.aman.vaak.managers.NotifyManager
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AboutDialogTest {
    @Mock
    private lateinit var notifyManager: NotifyManager

    private lateinit var dialog: AboutDialog

    @BeforeEach
    fun setup() {
        dialog = AboutDialog(notifyManager)
    }

    @Nested
    inner class DialogCreationTests {
        @Test
        fun `AboutDialog can be instantiated with NotifyManager`() {
            val aboutDialog = AboutDialog(notifyManager)

            assertNotNull(aboutDialog)
        }

        @Test
        fun `dialog instance is created successfully`() {
            assertNotNull(dialog)
        }
    }
}
