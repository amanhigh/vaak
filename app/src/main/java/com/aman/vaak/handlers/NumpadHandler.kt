package com.aman.vaak.handlers

import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import com.aman.vaak.R
import com.aman.vaak.managers.TextManager
import javax.inject.Inject
import javax.inject.Singleton

interface NumpadHandler {
    /**
     * Sets up numpad buttons on the provided view
     * @param parentView Parent view containing numpad buttons
     */
    fun setupNumpadButtons(parentView: View)

    /**
     * Shows the numpad view
     * @param parentView View containing the numpad layout
     */
    fun showNumpad(parentView: View)

    /**
     * Hides the numpad view
     * @param parentView View containing the numpad layout
     */
    fun hideNumpad(parentView: View)

    /**
     * Returns current visibility state of numpad
     * @return true if numpad is visible, false otherwise
     */
    fun isNumpadVisible(): Boolean
}

@Singleton
class NumpadHandlerImpl
    @Inject
    constructor(
        private val textManager: TextManager,
    ) : NumpadHandler {
        private var numpadVisible = false

        override fun setupNumpadButtons(parentView: View) {
            val numpadButtons =
                listOf(
                    R.id.num0Button, R.id.num1Button, R.id.num2Button,
                    R.id.num3Button, R.id.num4Button, R.id.num5Button,
                    R.id.num6Button, R.id.num7Button, R.id.num8Button,
                    R.id.num9Button,
                )

            numpadButtons.forEach { buttonId ->
                parentView.findViewById<Button>(buttonId)?.setOnClickListener { button ->
                    handleNumInput((button as Button).text.toString())
                }
            }

            parentView.findViewById<Button>(R.id.hideNumpadButton)?.setOnClickListener {
                hideNumpad(parentView)
            }
        }

        override fun showNumpad(parentView: View) {
            parentView.findViewById<LinearLayout>(R.id.numpadRow)?.visibility = View.VISIBLE
            numpadVisible = true
        }

        override fun hideNumpad(parentView: View) {
            parentView.findViewById<LinearLayout>(R.id.numpadRow)?.visibility = View.GONE
            numpadVisible = false
        }

        override fun isNumpadVisible(): Boolean = numpadVisible

        private fun handleNumInput(number: String) {
            try {
                textManager.insertText(number)
            } catch (e: Exception) {
                handleError(e)
            }
        }

        private fun handleError(error: Exception) {
            throw error
        }
    }
