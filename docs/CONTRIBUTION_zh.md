# 贡献指南 — VNet

感谢您考虑为 VNet 贡献代码。本文档详细介绍了项目的提交规范、代码风格和工作流程，帮助您快速上手。

## 目录

- [提交信息规范](#提交信息规范)
- [分支策略](#分支策略)
- [代码风格](#代码风格)
- [Pull Request 流程](#pull-request-流程)
- [获取帮助](#获取帮助)

## 提交信息规范

本项目采用 **结构化提交信息格式**。每条提交信息**必须**遵循以下格式：

```
Operation: <操作类型>[, Module: <模块>][: <描述>]
```

### 格式说明

| 部分 | 必需 | 说明 |
|------|------|------|
| `Operation:` | 是 | 固定前缀，表示这是一个操作提交 |
| `<操作类型>` | 是 | 操作类型（见下方表格） |
| `, Module:` | 否 | 影响的模块（如 `server`、`client`、`android`） |
| `: <描述>` | 否 | 变更的简要描述 |

### 操作类型

使用以下关键词之一作为操作类型：

| 操作 | 使用场景 |
|------|----------|
| `Docs` | 新增或更新文档 |
| `Fix` | 修复 Bug |
| `Format` | 代码格式化、空白符或代码风格调整 |
| `Automatic` | 自动化工具变更（CI、工作流等） |
| `Merge` | 合并提交新的模块/功能 |
| `Update` | 更新已有功能或依赖 |

### 模块

| 模块 | 说明 |
|------|------|
| `server` | 服务端代码 |
| `client` | 客户端代码 |
| `android` | Android 应用 |
| `docs` | 文档相关 |
| `ci` | CI/CD 相关 |

### 示例

```
Operation: Fix, Module: client: allocate virtio header headroom for TUN write on Linux
Operation: Merge, Module: android: Add IPv6 address input, multicast route support
Operation: Merge, Module: client: Replace water with wireguard/tun, add cross-platform and IPv6 support
Operation: Merge, Module: server: Add IPv6 packet parsing and broadcast/multicast fan-out support
Operation: Docs: Add contribution guide
Operation: Update, Module: go.mod: Update dependencies
```

### 提交正文（可选）

如需补充更多上下文，请在标题行后空一行，然后撰写正文。正文无严格格式要求，但请保持简洁明了。

## 分支策略

- **`master`**（或 **`main`**）— 主要开发分支。所有 Pull Request 应合并到此分支。
- 功能分支应从 `master` 创建，并使用描述性名称，例如：
  - `feature/add-ipv6-support`
  - `fix/tun-write-headroom`
  - `docs/add-contributing-guide`

## 代码风格

本项目使用 **Go** 语言，遵循 [官方 Go 代码评审建议](https://go.dev/wiki/CodeReviewComments)。

### 通用 Go 规范

- 提交前请运行 `go fmt`，确保代码格式一致。
- 使用有意义的变量和函数名称。
- 保持包的专注性和单一职责。
- 为新功能编写单元测试。
- 导出的函数、类型和常量应附带 Go 风格的注释。

### 平台特定代码

- 使用构建标签（build tags）区分平台特定实现
- 文件命名格式：`<功能>_<平台>.go`（如 `tun_linux.go`）
- 确保所有平台构建通过

## Pull Request 流程

1. Fork 本仓库，从 `master` 创建功能分支。
2. 按照上述代码风格进行修改。
3. 提交信息遵循提交信息规范。
4. 确保所有已有测试通过，并为新功能添加测试。
5. 运行 `go fmt ./...` 格式化代码。
6. 提交 Pull Request 到 `master` 分支。
7. 在 PR 描述中包括：
   - 变更摘要
   - 变更动机
   - 相关的 Issue 编号（如有）

## 获取帮助

如有疑问或需要帮助，请在 GitHub 上提交 Issue。