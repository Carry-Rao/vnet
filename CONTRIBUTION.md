# Contributing Guide — VNet

Thank you for considering contributing to VNet. This document details the project's commit conventions, code style, and workflow to help you get started quickly.

## Table of Contents

- [Commit Message Convention](#commit-message-convention)
- [Branch Strategy](#branch-strategy)
- [Code Style](#code-style)
- [Pull Request Process](#pull-request-process)
- [Getting Help](#getting-help)

## Commit Message Convention

This project uses a **structured commit message format**. Each commit message **must** follow this format:

```
Operation: <operation_type>[, Module: <module>][: <description>]
```

### Format Description

| Part | Required | Description |
|------|----------|-------------|
| `Operation:` | Yes | Fixed prefix indicating this is an operation commit |
| `<operation_type>` | Yes | Operation type (see table below) |
| `, Module:` | No | Affected module (e.g., `server`, `client`, `android`) |
| `: <description>` | No | Brief description of changes |

### Operation Types

Use one of the following keywords as the operation type:

| Operation | Use Case |
|-----------|----------|
| `Docs` | Add or update documentation |
| `Fix` | Fix bugs |
| `Format` | Code formatting, whitespace, or code style adjustments |
| `Automatic` | Automation tool changes (CI, workflows, etc.) |
| `Merge` | Merge new module/feature |
| `Update` | Update existing features or dependencies |

### Modules

| Module | Description |
|--------|-------------|
| `server` | Server code |
| `client` | Client code |
| `android` | Android application |
| `docs` | Documentation related |
| `ci` | CI/CD related |

### Examples

```
Operation: Fix, Module: client: allocate virtio header headroom for TUN write on Linux
Operation: Merge, Module: android: Add IPv6 address input, multicast route support
Operation: Merge, Module: client: Replace water with wireguard/tun, add cross-platform and IPv6 support
Operation: Merge, Module: server: Add IPv6 packet parsing and broadcast/multicast fan-out support
Operation: Docs: Add contribution guide
Operation: Update, Module: go.mod: Update dependencies
```

### Commit Body (Optional)

If you need to provide more context, leave a blank line after the subject line and write the body. There are no strict format requirements for the body, but please keep it concise and clear.

## Branch Strategy

- **`master`** (or **`main`**) — Main development branch. All Pull Requests should be merged into this branch.
- Feature branches should be created from `master` with descriptive names, such as:
  - `feature/add-ipv6-support`
  - `fix/tun-write-headroom`
  - `docs/add-contributing-guide`

## Code Style

This project uses **Go** and follows the [Official Go Code Review Comments](https://go.dev/wiki/CodeReviewComments).

### General Go Conventions

- Run `go fmt` before committing to ensure consistent code formatting.
- Use meaningful variable and function names.
- Keep packages focused with single responsibility.
- Write unit tests for new features.
- Exported functions, types, and constants should have Go-style comments.

### Platform-Specific Code

- Use build tags to distinguish platform-specific implementations
- File naming format: `<function>_<platform>.go` (e.g., `tun_linux.go`)
- Ensure all platforms build successfully

## Pull Request Process

1. Fork this repository and create a feature branch from `master`.
2. Make changes according to the above code style.
3. Follow the commit message convention.
4. Ensure all existing tests pass and add tests for new features.
5. Run `go fmt ./...` to format code.
6. Submit a Pull Request to the `master` branch.
7. Include the following in your PR description:
   - Summary of changes
   - Motivation for changes
   - Related Issue numbers (if any)

## Getting Help

If you have questions or need help, please submit an Issue on GitHub.