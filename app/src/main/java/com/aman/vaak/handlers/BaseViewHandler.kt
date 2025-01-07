package com.aman.vaak.handlers

import android.view.View
import androidx.annotation.IdRes
import javax.inject.Singleton

interface BaseViewHandler {
    /**
     * Attaches view for handler operations
     * @param view Parent view containing handler UI elements
     */
    fun attachView(view: View)

    /**
     * Detaches view and cleans up resources
     */
    fun detachView()

    /**
     * Checks if view is currently attached
     * @return true if view is attached, false otherwise
     */
    fun isViewAttached(): Boolean
}

class ViewNotFoundException(message: String) : Exception(message)

sealed class ViewOperationException(message: String) : Exception(message) {
    class ViewNotAttachedException :
        ViewOperationException("No view currently attached")

    class ViewComponentNotFoundException(id: Int) :
        ViewOperationException("Component with id $id not found")
}

@Singleton
abstract class BaseViewHandlerImpl : BaseViewHandler {
    protected var currentView: View? = null

    override fun attachView(view: View) {
        currentView = view
        onViewAttached(view)
    }

    override fun detachView() {
        onViewDetached()
        currentView = null
    }

    override fun isViewAttached(): Boolean = currentView != null

    protected abstract fun handleError(error: Exception)

    /**
     * Called when view is attached, initialize and setup view elements
     */
    protected abstract fun onViewAttached(view: View)

    /**
     * Called before view is detached, cleanup resources
     */
    protected abstract fun onViewDetached()

    /**
     * Safely finds and returns view by ID
     * @throws ViewNotFoundException if view not found
     */
    protected fun <T : View> requireView(
        @IdRes id: Int,
    ): T {
        return currentView?.findViewById(id)
            ?: throw ViewNotFoundException("View with id $id not found")
    }

    /**
     * Safely performs action with view if attached
     * @return true if action was performed, false if view not attached
     */
    protected fun withView(action: (View) -> Unit): Boolean {
        return currentView?.let { view ->
            action(view)
            true
        } ?: false
    }
}
