package com.aman.vaak.models

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.UUID

class PromptTest {
    @Nested
    inner class DefaultValues {
        @Test
        fun `default constructor generates valid UUID`() {
            val prompt = Prompt(name = "Test", content = "Content")

            assertNotNull(prompt.id)
            assertTrue(prompt.id.isNotBlank())
            // Validate UUID format
            UUID.fromString(prompt.id) // Should not throw exception
        }

        @Test
        fun `default constructor sets priority to 10`() {
            val prompt = Prompt(name = "Test", content = "Content")

            assertEquals(10, prompt.priority)
        }

        @Test
        fun `default constructor sets timestamps to current time`() {
            val beforeCreation = System.currentTimeMillis()
            val prompt = Prompt(name = "Test", content = "Content")
            val afterCreation = System.currentTimeMillis()

            assertTrue(prompt.createdAt >= beforeCreation)
            assertTrue(prompt.createdAt <= afterCreation)
            assertTrue(prompt.updatedAt >= beforeCreation)
            assertTrue(prompt.updatedAt <= afterCreation)
        }

        @Test
        fun `createdAt and updatedAt are same for new prompt`() {
            val prompt = Prompt(name = "Test", content = "Content")

            assertEquals(prompt.createdAt, prompt.updatedAt)
        }
    }

    @Nested
    inner class IdGeneration {
        @Test
        fun `different prompts get different IDs`() {
            val prompt1 = Prompt(name = "Test1", content = "Content1")
            val prompt2 = Prompt(name = "Test2", content = "Content2")

            assertNotEquals(prompt1.id, prompt2.id)
        }

        @Test
        fun `custom ID is preserved`() {
            val customId = "custom-test-id"
            val prompt = Prompt(id = customId, name = "Test", content = "Content")

            assertEquals(customId, prompt.id)
        }

        @Test
        fun `generated IDs follow UUID format`() {
            repeat(10) {
                val prompt = Prompt(name = "Test", content = "Content")
                // Should not throw exception if valid UUID
                UUID.fromString(prompt.id)
            }
        }

        @Test
        fun `ID uniqueness across multiple creations`() {
            val ids = mutableSetOf<String>()
            repeat(100) {
                val prompt = Prompt(name = "Test$it", content = "Content$it")
                assertTrue(ids.add(prompt.id), "Duplicate ID generated: ${prompt.id}")
            }
            assertEquals(100, ids.size)
        }
    }

    @Nested
    inner class PromptProperties {
        @Test
        fun `all properties are correctly set`() {
            val id = "test-id"
            val name = "Test Prompt"
            val content = "This is test content"
            val priority = 5
            val createdAt = 1000L
            val updatedAt = 2000L

            val prompt =
                Prompt(
                    id = id,
                    name = name,
                    content = content,
                    priority = priority,
                    createdAt = createdAt,
                    updatedAt = updatedAt,
                )

            assertEquals(id, prompt.id)
            assertEquals(name, prompt.name)
            assertEquals(content, prompt.content)
            assertEquals(priority, prompt.priority)
            assertEquals(createdAt, prompt.createdAt)
            assertEquals(updatedAt, prompt.updatedAt)
        }

        @Test
        fun `copy function works correctly`() {
            val original =
                Prompt(
                    name = "Original",
                    content = "Original content",
                    priority = 8,
                )

            val copied = original.copy()

            assertEquals(original.id, copied.id)
            assertEquals(original.name, copied.name)
            assertEquals(original.content, copied.content)
            assertEquals(original.priority, copied.priority)
            assertEquals(original.createdAt, copied.createdAt)
            assertEquals(original.updatedAt, copied.updatedAt)
        }

        @Test
        fun `copy with modifications works correctly`() {
            val original = Prompt(name = "Original", content = "Original content")
            val newUpdateTime = System.currentTimeMillis() + 1000

            val modified =
                original.copy(
                    name = "Modified",
                    content = "Modified content",
                    priority = 5,
                    updatedAt = newUpdateTime,
                )

            assertEquals(original.id, modified.id) // ID should remain same
            assertEquals("Modified", modified.name)
            assertEquals("Modified content", modified.content)
            assertEquals(5, modified.priority)
            assertEquals(original.createdAt, modified.createdAt) // Created time should remain same
            assertEquals(newUpdateTime, modified.updatedAt)
        }
    }

    @Nested
    inner class PriorityValidation {
        @Test
        fun `accepts valid priority values`() {
            val validPriorities = listOf(1, 5, 10, 15, 100)

            validPriorities.forEach { priority ->
                val prompt =
                    Prompt(
                        name = "Test",
                        content = "Content",
                        priority = priority,
                    )
                assertEquals(priority, prompt.priority)
            }
        }

        @Test
        fun `accepts edge priority values`() {
            val prompt1 = Prompt(name = "Test", content = "Content", priority = Int.MIN_VALUE)
            val prompt2 = Prompt(name = "Test", content = "Content", priority = Int.MAX_VALUE)

            assertEquals(Int.MIN_VALUE, prompt1.priority)
            assertEquals(Int.MAX_VALUE, prompt2.priority)
        }

        @Test
        fun `priority zero is valid`() {
            val prompt = Prompt(name = "Test", content = "Content", priority = 0)
            assertEquals(0, prompt.priority)
        }
    }

    @Nested
    inner class DataClassBehavior {
        @Test
        fun `equals works correctly`() {
            val prompt1 =
                Prompt(
                    id = "same-id",
                    name = "Test",
                    content = "Content",
                    priority = 5,
                    createdAt = 1000L,
                    updatedAt = 2000L,
                )

            val prompt2 =
                Prompt(
                    id = "same-id",
                    name = "Test",
                    content = "Content",
                    priority = 5,
                    createdAt = 1000L,
                    updatedAt = 2000L,
                )

            assertEquals(prompt1, prompt2)
        }

        @Test
        fun `different IDs make prompts unequal`() {
            val prompt1 = Prompt(id = "id1", name = "Test", content = "Content")
            val prompt2 = Prompt(id = "id2", name = "Test", content = "Content")

            assertNotEquals(prompt1, prompt2)
        }

        @Test
        fun `different names make prompts unequal`() {
            val prompt1 = Prompt(id = "same", name = "Name1", content = "Content")
            val prompt2 = Prompt(id = "same", name = "Name2", content = "Content")

            assertNotEquals(prompt1, prompt2)
        }

        @Test
        fun `toString includes key information`() {
            val prompt = Prompt(name = "TestPrompt", content = "TestContent", priority = 7)
            val string = prompt.toString()

            assertTrue(string.contains("TestPrompt"))
            assertTrue(string.contains("TestContent"))
            assertTrue(string.contains("7"))
        }

        @Test
        fun `hashCode is consistent`() {
            val prompt1 =
                Prompt(
                    id = "test-id",
                    name = "Test",
                    content = "Content",
                )
            val prompt2 =
                Prompt(
                    id = "test-id",
                    name = "Test",
                    content = "Content",
                )

            assertEquals(prompt1.hashCode(), prompt2.hashCode())
        }
    }

    @Nested
    inner class EdgeCases {
        @Test
        fun `handles empty name and content`() {
            val prompt = Prompt(name = "", content = "")

            assertEquals("", prompt.name)
            assertEquals("", prompt.content)
            assertNotNull(prompt.id)
        }

        @Test
        fun `handles very long name and content`() {
            val longString = "a".repeat(10000)
            val prompt = Prompt(name = longString, content = longString)

            assertEquals(longString, prompt.name)
            assertEquals(longString, prompt.content)
        }

        @Test
        fun `handles special characters in name and content`() {
            val specialName = "Test!@#$%^&*()_+-={}[]|\\:;\"'<>?,./"
            val specialContent = "Content with 中文 and émojis 🎉"

            val prompt = Prompt(name = specialName, content = specialContent)

            assertEquals(specialName, prompt.name)
            assertEquals(specialContent, prompt.content)
        }

        @Test
        fun `handles negative timestamps`() {
            val prompt =
                Prompt(
                    name = "Test",
                    content = "Content",
                    createdAt = -1000L,
                    updatedAt = -500L,
                )

            assertEquals(-1000L, prompt.createdAt)
            assertEquals(-500L, prompt.updatedAt)
        }
    }
}

class PromptLibraryTest {
    @Test
    fun `default constructor creates empty library`() {
        val library = PromptLibrary()

        assertTrue(library.prompts.isEmpty())
    }

    @Test
    fun `constructor with prompts works correctly`() {
        val prompts =
            listOf(
                Prompt(name = "Test1", content = "Content1"),
                Prompt(name = "Test2", content = "Content2"),
            )

        val library = PromptLibrary(prompts)

        assertEquals(2, library.prompts.size)
        assertEquals(prompts, library.prompts)
    }

    @Test
    fun `copy function works correctly`() {
        val prompts = listOf(Prompt(name = "Test", content = "Content"))
        val library = PromptLibrary(prompts)

        val copied = library.copy()

        assertEquals(library.prompts, copied.prompts)
    }

    @Test
    fun `handles large number of prompts`() {
        val prompts =
            (1..1000).map {
                Prompt(name = "Test$it", content = "Content$it")
            }

        val library = PromptLibrary(prompts)

        assertEquals(1000, library.prompts.size)
    }
}
