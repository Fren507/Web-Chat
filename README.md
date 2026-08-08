# Web Chat

A web-based chat integration for Minecraft, connecting the Minecraft server with a modern web interface.

<div align="center">
  <a href="https://github.com/Fren507/Web-Chat/actions/workflows/release.yml">
    <img src="https://img.shields.io/github/actions/workflow/status/Fren507/Web-Chat/release.yml?label=Release&style=for-the-badge" alt="Release Status">
  </a>
  <img src="https://img.shields.io/badge/Fabric-0.19.3-blue?style=for-the-badge" alt="Fabric">
  <br>
  <img src="https://img.shields.io/badge/Minecraft-26.2.1-green?style=for-the-badge" alt="Minecraft">
  <a href="https://github.com/Fren507">
    <img src="https://img.shields.io/badge/Built%20with%20Love%20by%20Jason%20Frenzel-Fren507-red?style=for-the-badge&logo=github" alt="Built with Love">
  </a>
</div>

## Features

* Web-based Minecraft chat
* Real-time communication between Minecraft and the web
* Player verification
* Custom web interface
* Modern frontend built with Vite and TypeScript
* Fabric mod integration

## Website

The web frontend is located in [`web/`](web/) and is built using Vite and TypeScript.

The production build is automatically included in the Fabric mod under:

```text
src/main/resources/web/
```

## Development

### Requirements

* Java 25
* Node.js 22+
* pnpm
* Gradle Wrapper

### Build

Build the complete project with:

```bash
make build
```

This builds both the web frontend and the Fabric mod.

### Development Server

Start the web development server with:

```bash
make dev
```

### Run the Minecraft Server

Build the project and start the development server with:

```bash
make run
```

### Clean

Remove generated build files with:

```bash
make clean
```

For a list of available Make targets:

```bash
make help
```

## Fonts

The website uses the following fonts:

### Monocraft

**Monocraft**
Copyright © 2022 Idrees Hassan

Licensed under the **SIL Open Font License 1.1**.

https://scripts.sil.org/OFL

### Minecraft Font

**Minecraft Font**
Author: JDGraphics

Licensed under the **Public Domain**.

## License

See the repository for the applicable project license and third-party licenses.
