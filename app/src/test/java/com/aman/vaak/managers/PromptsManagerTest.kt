package com.aman.vaak.managers

import com.aman.vaak.models.Prompt
import com.aman.vaak.models.PromptLibrary
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class PromptsManagerTest {
    private lateinit var fileManager: FileManager
    private lateinit var moshi: Moshi
    private lateinit var promptsManager: PromptsManager
    private lateinit var mockFile: File

    @BeforeEach
    fun setup() {
        mockFile = mock()
        fileManager = mock()
        whenever(fileManager.getInternalFile(any())).thenReturn(mockFile)
        moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        promptsManager = PromptsManagerImpl(fileManager, moshi)
    }

    @Nested
    @DisplayName("Given Empty Prompts File")
    inner class EmptyPromptsFile {
        @BeforeEach
        fun setup() {
            whenever(fileManager.fileExists(any())).thenReturn(false)
        }

        @Test
        fun `When Getting Prompts Then Returns Empty List`() =
            runTest {
                val prompts = promptsManager.getPrompts()
                assertTrue(prompts.isEmpty())
            }

        @Test
        fun `When Saving First Prompt Then Generates ID And Saves`() =
            runTest {
                val prompt = Prompt(name = "Test", content = "Content")
                whenever(fileManager.write(any(), any())).then { }

                val result = promptsManager.savePrompt(prompt)
                assertTrue(result.isSuccess)
                verify(fileManager).write(any(), any())
            }
    }

    @Nested
    @DisplayName("Given Existing Prompts")
    inner class ExistingPrompts {
        private val existingPrompt =
            Prompt(
                id = "test-id",
                name = "Existing",
                content = "Content",
                createdAt = 1000L,
                updatedAt = 2000L,
            )
        private val promptLibrary = PromptLibrary(listOf(existingPrompt))

        @BeforeEach
        fun setup() {
            whenever(fileManager.fileExists(any())).thenReturn(true)
            whenever(fileManager.read(any())).thenReturn(
                moshi.adapter(PromptLibrary::class.java).toJson(promptLibrary),
            )
        }

        @Test
        fun `When Getting All Prompts Then Returns Sorted List`() =
            runTest {
                val prompts = promptsManager.getPrompts()
                assertEquals(1, prompts.size)
                assertEquals(existingPrompt.content, prompts.first().content)
                assertEquals(existingPrompt.name, prompts.first().name)
                assertNotNull(prompts.first().id)
            }

        @Test
        fun `When Getting Single Prompt Then Returns Correct Content`() =
            runTest {
                val prompt = promptsManager.getPrompt(existingPrompt.id)
                assertNotNull(prompt)
                assertEquals(existingPrompt.content, prompt!!.content)
                assertEquals(existingPrompt.name, prompt!!.name)
                assertNotNull(prompt!!.id)
            }

        @Test
        fun `When Getting Non-existent Prompt Then Returns Null`() =
            runTest {
                val prompt = promptsManager.getPrompt("non-existent")
                assertNull(prompt)
            }

        @Test
        fun `When Adding Prompt With Duplicate Name Then Throws Exception`() =
            runTest {
                val duplicatePrompt = Prompt(name = "Existing", content = "New Content")

                assertThrows<PromptsException.DuplicatePromptException> {
                    promptsManager.savePrompt(duplicatePrompt).getOrThrow()
                }
            }

        @Test
        fun `When Updating Existing Prompt Then Successfully Updates`() =
            runTest {
                val updatedPrompt = existingPrompt.copy(content = "Updated Content")
                whenever(fileManager.write(any(), any())).then { }

                val result = promptsManager.savePrompt(updatedPrompt)
                assertTrue(result.isSuccess)
            }

        @Test
        fun `When Deleting Prompt Then Successfully Deletes`() =
            runTest {
                whenever(fileManager.write(any(), any())).then { }

                val result = promptsManager.deletePrompt(existingPrompt.id)
                assertTrue(result.isSuccess)
            }
    }

    @Nested
    @DisplayName("Given File Operation Errors")
    inner class FileOperationErrors {
        @Test
        fun `When Reading File Fails Then Throws StorageException`() =
            runTest {
                whenever(fileManager.fileExists(any())).thenReturn(true)
                whenever(fileManager.read(any())).thenAnswer { throw IOException("Read failed") }

                assertThrows<PromptsException.StorageException> {
                    promptsManager.getPrompts()
                }
            }

        @Test
        fun `When Writing File Fails Then Returns Failure`() =
            runTest {
                whenever(fileManager.fileExists(any())).thenReturn(false)
                whenever(fileManager.write(any(), any())).thenAnswer { throw IOException("Write failed") }

                val prompt = Prompt(name = "Test", content = "Content")
                val result = promptsManager.savePrompt(prompt)
                assertTrue(result.isFailure)
            }
    }
}
