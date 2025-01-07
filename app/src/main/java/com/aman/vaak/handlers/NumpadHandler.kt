package com.aman.vaak.handlers

import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import com.aman.vaak.R
import com.aman.vaak.managers.TextManager
import javax.inject.Inject
import javax.inject.Singleton

interface NumpadHandler : BaseViewHandler {
    /**
     * Shows the numpad view
     */
    fun showNumpad()

    /**
     * Hides the numpad view
     */
    fun hideNumpad()

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
    ) : BaseViewHandlerImpl(), NumpadHandler {
        private var numpadVisible = false

        override fun onViewAttached(view: View) {
            setupNumpadButtons(view)
        }

        override fun onViewDetached() {
            // No cleanup needed
        }

        private fun setupNumpadButtons(view: View) {
            val numpadButtons =
                listOf(
                    R.id.num0Button, R.id.num1Button, R.id.num2Button,
                    R.id.num3Button, R.id.num4Button, R.id.num5Button,
                    R.id.num6Button, R.id.num7Button, R.id.num8Button,
                    R.id.num9Button,
                )

            numpadButtons.forEach { buttonId ->
                view.findViewById<Button>(buttonId)?.setOnClickListener { button ->
                    handleNumInput((button as Button).text.toString())
                }
            }

            view.findViewById<Button>(R.id.hideNumpadButton)?.setOnClickListener {
                hideNumpad()
            }
        }

        override fun showNumpad() {
            withView { view ->
                view.findViewById<LinearLayout>(R.id.numpadRow)?.visibility = View.VISIBLE
                numpadVisible = true
            }
        }

        override fun hideNumpad() {
            withView { view ->
                view.findViewById<LinearLayout>(R.id.numpadRow)?.visibility = View.GONE
                numpadVisible = false
            }
        }

        override fun isNumpadVisible(): Boolean = numpadVisible

        private fun handleNumInput(number: String) {
            try {
                textManager.insertText(number)
            } catch (e: Exception) {
                handleError(e)
            }
        }

        override fun handleError(error: Exception) {
            throw error
        }
    }
