package com.aman.vaak.models

import java.util.UUID

data class Prompt(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val content: String,
    // New field
    val priority: Int = 10,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

data class PromptLibrary(
    val prompts: List<Prompt> = emptyList(),
)
