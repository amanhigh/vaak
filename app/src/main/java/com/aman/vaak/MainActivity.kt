package com.aman.vaak

import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private val clipboard by lazy { getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }
    private lateinit var pasteButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        pasteButton = findViewById(R.id.pasteButton)
        pasteButton.setOnClickListener { onPasteClicked() }
    }
    
    private fun onPasteClicked() {
        clipboard.primaryClip?.getItemAt(0)?.text?.let { text ->
            // For now, just add clipboard text
            // TODO: Handle the pasted text
        }
    }
}
