# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

VaaK is an AI-powered Android keyboard app that integrates OpenAI's speech recognition for voice dictation. It's built with Kotlin using modern Android development practices.

## Development Workflow
### Release Management
- `make release ver=X.Y.Z` - Create and push release tag (must be on master branch)
- `make unrelease ver=X.Y.Z` - Remove release tag

## Architecture Overview

### Core Structure
The app follows a clean architecture pattern with these main layers:

**Handlers** (`app/src/main/java/com/aman/vaak/handlers/`)
- UI controllers and input method components
- `VaakInputMethodService.kt` - Main keyboard service
- `DictationHandler.kt` - Voice input processing
- `TextHandler.kt` - Text manipulation
- Activity handlers for settings and setup

**Managers** (`app/src/main/java/com/aman/vaak/managers/`)
- Business logic and system interaction
- `TextManager.kt` - Text input/output operations
- `DictationManager.kt` - Speech-to-text coordination
- `WhisperManager.kt` - OpenAI API integration
- `SettingsManager.kt` - User preferences
- `BackupManager.kt` - Data backup/restore

**Models** (`app/src/main/java/com/aman/vaak/models/`)
- Data classes and state objects
- `DictationState.kt` - Recording state management
- `Language.kt` - Language configuration
- `Prompt.kt` - User text snippets

### Key Technologies
- **Dependency Injection**: Hilt/Dagger
- **HTTP Client**: Ktor (for OpenAI API)
- **JSON**: Moshi
- **Security**: AndroidX Security Crypto
- **Testing**: JUnit 5, Mockito
- **Code Quality**: Spotless (formatting), Detekt (linting)

### Input Method Architecture
The app extends Android's `InputMethodService` through `VaakInputMethodService`, which coordinates:
1. Voice recording via `DictationHandler`
2. Text processing via `TextHandler` 
3. API calls via `WhisperManager`
4. UI state management across keyboard views

### Key Dependencies
- OpenAI client for speech recognition
- AndroidX Security for encrypted API key storage
- Hilt for dependency injection across Android components
- Coroutines for async operations (voice recording, API calls)

## Testing
- Unit tests use JUnit 5 and Mockito
- Tests are located in `app/src/test/java/com/aman/vaak/`
- Current test coverage includes managers: ClipboardManager, NotifyManager, PromptsManager, TextManager
- Use `make test` to run the full test suite

## Code Style
- Kotlin code follows ktlint formatting rules
- Spotless enforces consistent formatting
- Detekt provides static analysis
- All formatting rules are applied via `make format`