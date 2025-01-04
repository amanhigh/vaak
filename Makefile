### Variables
.DEFAULT_GOAL := help
OUT := /dev/null

GRADLE := ./gradlew
BUILD_DIR := app/build
APK_SOURCE := $(BUILD_DIR)/outputs/apk/debug/app-debug.apk
APK_TARGET := ./vaak.apk

# Release Management
GITHUB_REPO_URL := https://github.com/amanfdk/vaak
VERSION_PATTERN := ^[0-9]+\.[0-9]+\.[0-9]+$$
VERSION_HELP := "Usage: make [release|unrelease] ver=X.Y.Z\nExample: make release ver=1.0.0"

### Basic
help: ## Show this help
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}'
	printf $(_TITLE) "FirstTime: prepare/all, OUT=/dev/stdout (Debug) "

### Release Management
check-version:
	@if [ -z "$(ver)" ]; then \
		echo "Error: Version not specified"; \
		echo $(VERSION_HELP); \
		exit 1; \
	fi
	@if ! echo "$(ver)" | grep -qE "$(VERSION_PATTERN)"; then \
		echo "Error: Invalid version format. Must be X.Y.Z"; \
		echo $(VERSION_HELP); \
		exit 1; \
	fi

check-branch:
	@if [ "$(shell git branch --show-current)" != "master" ]; then \
		echo "Error: Releases must be created from master branch"; \
		echo "Current branch: $$(git branch --show-current)"; \
		exit 1; \
	fi

check-clean:
	@if [ -n "$(shell git status --porcelain)" ]; then \
		echo "Error: Working directory is not clean"; \
		echo "Please commit or stash changes first"; \
		exit 1; \
	fi

release: check-version check-branch check-clean ## Create and push a new release version tag (ver=X.Y.Z)
	@echo "Creating release v$(ver)..."
	@git tag -a v$(ver) -m "Release v$(ver)"
	@git push origin v$(ver)
	@echo "Release tag v$(ver) created and pushed successfully"

unrelease: check-version ## Delete a specific version tag and show release deletion info (ver=X.Y.Z)
	@if ! git tag | grep -q "v$(ver)"; then \
		echo "Error: Tag v$(ver) does not exist"; \
		exit 1; \
	fi
	@echo "Removing tag v$(ver) locally and remotely..."
	@git tag -d v$(ver)
	@git push origin :refs/tags/v$(ver)
	@echo "Tag removed successfully"
	@echo "Note: To complete cleanup, please delete the release at:"
	@echo "$(GITHUB_REPO_URL)/releases/tag/v$(ver)"

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
	@repomix --style markdown . --ignore "LICENSE,gradlew,app/src/test"

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

lint: ## Run lint checks
	@printf $(_TITLE) "Lint" "Running lint checks"
	@$(GRADLE) detekt

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
