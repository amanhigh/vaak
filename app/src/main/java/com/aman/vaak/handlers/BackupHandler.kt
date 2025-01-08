package com.aman.vaak.handlers

import android.content.Context
import android.net.Uri
import android.view.View
import android.widget.Button
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.aman.vaak.R
import com.aman.vaak.managers.BackupManager
import com.aman.vaak.managers.NotifyManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

interface BackupHandler : BaseViewHandler {
    fun handleCreateBackup()

    fun handleRestoreBackup()
}

@Singleton
class BackupHandlerImpl
    @Inject
    constructor(
        private val backupManager: BackupManager,
        private val notifyManager: NotifyManager,
        private val scope: CoroutineScope,
    ) : BaseViewHandlerImpl(), BackupHandler {
        private var activityResultLauncher: ActivityResultLauncher<String>? = null

        override fun onViewAttached(view: View) {
            setupBackupButtons(view)
            registerForActivityResult(view.context)
        }

        override fun onViewDetached() {
            activityResultLauncher = null
        }

        private fun registerForActivityResult(context: Context) {
            val activity = context as? AppCompatActivity ?: return

            activityResultLauncher =
                activity.registerForActivityResult(
                    ActivityResultContracts.GetContent(),
                ) { uri ->
                    uri?.let { handleBackupFileSelection(it) }
                }
        }

        private fun setupBackupButtons(view: View) {
            view.findViewById<Button>(R.id.backupButton)?.setOnClickListener {
                handleCreateBackup()
            }

            view.findViewById<Button>(R.id.restoreButton)?.setOnClickListener {
                handleRestoreBackup()
            }
        }

        override fun handleCreateBackup() {
            scope.launch {
                backupManager.createBackup()
                    .onSuccess { file ->
                        val filename = file.name
                        currentView?.context?.let { context ->
                            notifyManager.showInfo(
                                title = context.getString(R.string.settings_backup_title),
                                message = context.getString(R.string.backup_created, filename),
                            )
                        }
                    }
                    .onFailure { e -> handleError(Exception(e)) }
            }
        }

        override fun handleRestoreBackup() {
            activityResultLauncher?.launch("application/json")
        }

        private fun handleBackupFileSelection(uri: Uri) {
            val context = currentView?.context ?: return

            scope.launch {
                try {
                    // Get file from URI
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        val tempFile = File.createTempFile("backup", ".json", context.cacheDir)
                        tempFile.outputStream().use { output ->
                            input.copyTo(output)
                        }

                        backupManager.restoreBackup(tempFile)
                            .onSuccess {
                                notifyManager.showInfo(
                                    title = context.getString(R.string.settings_backup_title),
                                    message = context.getString(R.string.backup_restored),
                                )
                            }
                            .onFailure { e -> handleError(Exception(e)) }

                        tempFile.delete()
                    }
                } catch (e: Exception) {
                    handleError(e)
                }
            }
        }

        override fun handleError(error: Exception) {
            currentView?.context?.let { context ->
                notifyManager.showError(
                    title = context.getString(R.string.settings_backup_title),
                    message =
                        when (error) {
                            is SecurityException -> context.getString(R.string.error_file_select)
                            else -> error.message ?: context.getString(R.string.error_backup_restore)
                        },
                )
            }
        }
    }
