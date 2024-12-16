package com.aman.vaak.managers

import android.view.inputmethod.InputConnection
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(MockitoJUnitRunner::class)
class TextManagerTest {
    @Mock private lateinit var inputConnection: InputConnection
    private lateinit var manager: TextManagerImpl

    @Before
    fun setup() {
        manager = TextManagerImpl()
    }

    @Test
    fun `attachInputConnection returns true for valid connection`() {
        assertTrue(manager.attachInputConnection(inputConnection))
    }

    @Test
    fun `attachInputConnection returns false for null connection`() {
        assertFalse(manager.attachInputConnection(null))
    }

    @Test
    fun `operations return false without attached connection`() {
        assertFalse(manager.insertSpace())
        assertFalse(manager.handleBackspace())
        assertFalse(manager.insertNewLine())
        assertFalse(manager.selectAll())
    }

    @Test
    fun `operations return false after detaching connection`() {
        manager.attachInputConnection(inputConnection)
        manager.detachInputConnection()
        
        assertFalse(manager.insertSpace())
        assertFalse(manager.handleBackspace())
        assertFalse(manager.insertNewLine())
        assertFalse(manager.selectAll())
    }

    @Test
    fun `insertSpace commits space character when connected`() {
        manager.attachInputConnection(inputConnection)
        whenever(inputConnection.commitText(" ", 1)).thenReturn(true)
        
        assertTrue(manager.insertSpace())
        verify(inputConnection).commitText(" ", 1)
    }

    @Test
    fun `handleBackspace deletes character when connected`() {
        manager.attachInputConnection(inputConnection)
        whenever(inputConnection.deleteSurroundingText(1, 0)).thenReturn(true)
        
        assertTrue(manager.handleBackspace())
        verify(inputConnection).deleteSurroundingText(1, 0)
    }

    @Test
    fun `insertNewLine commits newline when connected`() {
        manager.attachInputConnection(inputConnection)
        whenever(inputConnection.commitText("\n", 1)).thenReturn(true)
        
        assertTrue(manager.insertNewLine())
        verify(inputConnection).commitText("\n", 1)
    }

    @Test
    fun `selectAll performs context menu action when connected`() {
        manager.attachInputConnection(inputConnection)
        whenever(inputConnection.performContextMenuAction(android.R.id.selectAll)).thenReturn(true)
        
        assertTrue(manager.selectAll())
        verify(inputConnection).performContextMenuAction(android.R.id.selectAll)
    }
}
