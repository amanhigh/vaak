### Variables
.DEFAULT_GOAL := help
OUT := /dev/null

GRADLE := ./gradlew
BUILD_DIR := app/build
APK_SOURCE := $(BUILD_DIR)/outputs/apk/debug/app-debug.apk
APK_TARGET := ./vaak.apk

### Basic
help: ## Show this help
    @grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}'

### APK Setup
.PHONY: build copy-apk remove-apk clean
build: ## Build the APK
    @printf $(_TITLE) "Build" "Building APK"
    @$(GRADLE) build

copy-apk:
    @printf $(_TITLE) "Copy" "Copying APK to Root"
    @cp $(APK_SOURCE) $(APK_TARGET)

clean: ## Clean build files
    @printf $(_TITLE) "Clean" "Cleaning project"
    @$(GRADLE) clean
    @rm -f $(APK_TARGET)

### Setup
setup: build copy-apk ## Setup initial build

### Formatting
_INFO := "\033[33m[%s]\033[0m %s\n"
_TITLE := "\033[32m[%s]\033[0m %s\n"
_WARN := "\033[31m[%s]\033[0m %s\n"
