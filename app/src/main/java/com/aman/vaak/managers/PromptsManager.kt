package com.aman.vaak.managers

import com.aman.vaak.models.Prompt
import com.aman.vaak.models.PromptLibrary
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

sealed class PromptsException(message: String) : Exception(message) {
    class StorageException(message: String) : PromptsException(message)

    class InvalidDataException : PromptsException("Invalid prompt data")

    class DuplicatePromptException(name: String) :
        PromptsException("Prompt with name '$name' already exists")
}

interface PromptsManager {
    suspend fun getPrompts(): List<Prompt>

    suspend fun getPrompt(id: String): Prompt?

    suspend fun savePrompt(prompt: Prompt): Result<Unit>

    suspend fun deletePrompt(id: String): Result<Unit>
}

class PromptsManagerImpl
    @Inject
    constructor(
        private val fileManager: FileManager,
        private val moshi: Moshi,
    ) : PromptsManager {
        companion object {
            private const val PROMPTS_FILENAME = "prompts.json"
        }

        private val mutex = Mutex()
        private val adapter: JsonAdapter<PromptLibrary> = moshi.adapter(PromptLibrary::class.java)
        private val promptsFile: File = fileManager.getInternalFile(PROMPTS_FILENAME)

        private suspend fun readPromptLibrary(): PromptLibrary =
            withContext(Dispatchers.IO) {
                if (!fileManager.fileExists(promptsFile)) {
                    return@withContext PromptLibrary()
                }

                runCatching {
                    val content = fileManager.read(promptsFile)
                    adapter.fromJson(content) ?: PromptLibrary()
                }.getOrElse { e ->
                    throw PromptsException.StorageException("Failed to read prompts: ${e.message}")
                }
            }

        private suspend fun writePromptLibrary(library: PromptLibrary) =
            withContext(Dispatchers.IO) {
                runCatching {
                    val json = adapter.toJson(library)
                    fileManager.write(promptsFile, json)
                }.getOrElse { e ->
                    throw PromptsException.StorageException("Failed to write prompts: ${e.message}")
                }
            }

        override suspend fun getPrompts(): List<Prompt> =
            mutex.withLock {
                readPromptLibrary().prompts.sortedByDescending { it.updatedAt }
            }

        override suspend fun getPrompt(id: String): Prompt? =
            mutex.withLock {
                readPromptLibrary().prompts.find { it.id == id }
            }

        override suspend fun savePrompt(prompt: Prompt): Result<Unit> =
            runCatching {
                mutex.withLock {
                    val library = readPromptLibrary()
                    val existing = library.prompts.find { it.id == prompt.id }

                    // Check for name duplication for new prompts
                    if (existing == null && library.prompts.any { it.name == prompt.name }) {
                        throw PromptsException.DuplicatePromptException(prompt.name)
                    }

                    val updatedPrompts = library.prompts.filter { it.id != prompt.id } + prompt
                    writePromptLibrary(PromptLibrary(updatedPrompts))
                }
            }

        override suspend fun deletePrompt(id: String): Result<Unit> =
            runCatching {
                mutex.withLock {
                    val library = readPromptLibrary()
                    val updatedPrompts = library.prompts.filter { it.id != id }
                    writePromptLibrary(PromptLibrary(updatedPrompts))
                }
            }
    }
