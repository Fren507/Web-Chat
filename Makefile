.PHONY: all build web java dev clean clean-web clean-java help

# ─────────────────────────────────────────────
# Configuration
# ─────────────────────────────────────────────

WEB_DIR := web
WEB_OUT := src/main/resources/web

# ─────────────────────────────────────────────
# Default
# ─────────────────────────────────────────────

all: build

# ─────────────────────────────────────────────
# Build everything
# ─────────────────────────────────────────────

build: web java

# Build the web frontend
web:
	cd $(WEB_DIR) && pnpm build
	rm -rf $(WEB_OUT)
	mkdir -p $(WEB_OUT)
	cp -r $(WEB_DIR)/dist/. $(WEB_OUT)/

# Build the Fabric mod
java:
	./gradlew build

# ─────────────────────────────────────────────
# Development
# ─────────────────────────────────────────────

dev:
	cd $(WEB_DIR) && pnpm dev

run: build
	./gradlew runServer

# ─────────────────────────────────────────────
# Cleaning
# ─────────────────────────────────────────────

clean: clean-web clean-java

clean-web:
	rm -rf $(WEB_DIR)/dist

clean-java:
	./gradlew clean

# ─────────────────────────────────────────────
# Help
# ─────────────────────────────────────────────

help:
	@echo "Web Chat build system"
	@echo
	@echo "Targets:"
	@echo "  make              Build web + Java"
	@echo "  make build        Build web + Java"
	@echo "  make run          Build web + Java and run it afterwords"
	@echo "  make web          Build the Vite frontend"
	@echo "  make java         Build the Fabric mod"
	@echo "  make dev          Start the Vite development server"
	@echo "  make clean        Clean web + Java builds"
	@echo "  make clean-web    Remove web/dist"
	@echo "  make clean-java   Run Gradle clean"
	@echo "  make help         Show this help"
