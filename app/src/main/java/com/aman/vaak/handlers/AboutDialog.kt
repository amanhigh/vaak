package com.aman.vaak.handlers

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import com.aman.vaak.R
import com.aman.vaak.databinding.DialogAboutBinding
import com.aman.vaak.managers.NotifyManager
import javax.inject.Inject

class AboutDialog
    @Inject
    constructor(
        private val notifyManager: NotifyManager,
    ) {
        companion object {
            private const val DOCS_URL = "https://github.com/amanhigh/vaak"
            private const val ISSUES_URL = "https://github.com/amanhigh/vaak/issues"
        }

        private fun openUrl(
            context: Context,
            url: String,
        ) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(intent)
            } catch (e: Exception) {
                notifyManager.showError(
                    title = context.getString(R.string.error_browser_launch),
                    message = e.message ?: "Unknown error",
                )
            }
        }

        fun show(context: Context) {
            val binding = DialogAboutBinding.inflate(LayoutInflater.from(context))

            AlertDialog.Builder(context)
                .setTitle(R.string.about_title)
                .setView(binding.root)
                .setPositiveButton(R.string.btn_done, null)
                .create()
                .also { dialog ->
                    binding.documentationButton.setOnClickListener {
                        openUrl(context, DOCS_URL)
                        dialog.dismiss()
                    }
                    binding.reportIssueButton.setOnClickListener {
                        openUrl(context, ISSUES_URL)
                        dialog.dismiss()
                    }
                    dialog.show()
                }
        }
    }
