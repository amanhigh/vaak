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
	printf $(_TITLE) "FirstTime: prepare/all, OUT=/dev/stdout (Debug) "

### APK Setup
.PHONY: build copy-apk remove-apk
build: format ## Build the APK
	@printf $(_TITLE) "Build" "Building APK"
	@$(GRADLE) build

format: ## Format all Kotlin files
	@printf $(_TITLE) "Format" "Formatting Kotlin files"
	@$(GRADLE) spotlessApply

pack: ## Repomix Packing
	@printf $(_TITLE) "Pack" "Repository"
	@repomix --style markdown .

copy-apk:
	@printf $(_TITLE) "Copy" "Copying APK to Root"
	@cp $(APK_SOURCE) $(APK_TARGET)

### Clean
remove-apk:
	@printf $(_TITLE) "Remove" "Removing APK from Root"
	@rm $(APK_TARGET)

clean-gradle: ## Clean Gradle
	@printf $(_TITLE) "Clean" "Cleaning Gradle"
	@$(GRADLE) clean

### Testing
test: ## Run Unit Tests
	@printf $(_TITLE) "Test" "Running Unit Tests"
	@$(GRADLE) test

### Workflows
info: ## Info
infos: info ## Extended Info
prepare: ## Onetime Setup
setup: test build copy-apk ## Setup
install: setup adb-install ## Build and install APK to emulator
clean: remove-apk clean-gradle ## Clean
reset: clean setup info ## Reset
all:prepare reset ## Run All Targets

### Formatting
_INFO := "\033[33m[%s]\033[0m %s\n"  # Yellow text for "printf"
_DETAIL := "\033[34m[%s]\033[0m %s\n"  # Blue text for "printf"
_TITLE := "\033[32m[%s]\033[0m %s\n" # Green text for "printf"
_WARN := "\033[31m[%s]\033[0m %s\n" # Red text for "printf"
