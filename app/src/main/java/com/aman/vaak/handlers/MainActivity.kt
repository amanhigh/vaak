package com.aman.vaak.handlers

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.aman.vaak.R
import com.aman.vaak.managers.ClipboardManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var clipboardManager: ClipboardManager
    
    private lateinit var pasteButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)
        
        pasteButton = findViewById(R.id.pasteButton)
        pasteButton.setOnClickListener { onPasteClicked() }
    }
    
    private fun onPasteClicked() {
        clipboardManager.pasteContent()?.let { text ->
            // TODO: Handle the pasted text
        }
    }
}
