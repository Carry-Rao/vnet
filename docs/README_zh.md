We provide [English](https://github.com/Carry-Rao/vnet/blob/master/README.md)

# VNet — 轻量级虚拟网络工具

本项目是一个基于 WebSocket 的轻量级虚拟网络解决方案，支持跨平台 TUN 设备创建和 IPv4/IPv6 双栈通信。核心设计理念如下：

- 基于 WebSocket 协议实现虚拟网络隧道，简化 NAT 穿透和防火墙穿越
- 支持多平台（Linux/macOS/Windows/Android）TUN 设备创建和配置
- 提供统一的客户端和服务端接口，支持 IPv4/IPv6 双栈通信
- 内置广播/组播支持，实现局域网内设备发现和通信

+ **注意**：本项目为实验性项目，未经过充分测试，不建议用于生产环境。

## 核心特性

### 跨平台 TUN 支持

VNet 支持在多个操作系统上创建和配置 TUN 虚拟网络设备：

- **Linux**: 使用 `ip` 命令配置网络接口
- **macOS**: 使用 `ifconfig` 命令配置网络接口
- **Windows**: 使用 `netsh` 命令配置网络接口
- **Android**: 通过 Android VpnService API 创建 TUN 设备

### IPv4/IPv6 双栈

VNet 完整支持 IPv4 和 IPv6 双栈通信：

- 客户端可同时配置 IPv4 和 IPv6 地址
- 服务端支持 IPv4/IPv6 数据包解析和转发
- 支持 IPv6 组播地址自动配置

### WebSocket 隧道

使用 WebSocket 协议作为传输层，具有以下优势：

- 穿越 NAT 和防火墙
- 支持 TLS/SSL 加密传输
- 自动重连机制
- 跨平台兼容性

### 广播/组播支持

服务端支持广播和组播数据包的扇出转发：

- 自动识别广播/组播目标地址
- 支持 IPv4 广播地址（255.255.255.255）和组播地址范围（224.0.0.0 - 239.255.255.255）
- 支持 IPv6 组播地址（ff00::/8）

## 快速开始

### 服务端部署

```bash
# 编译服务端
go build -o vnet-server ./vnet-server

# 启动服务端（监听 8080 端口，使用密钥 mysecret）
./vnet-server -listen :8080 -key mysecret
```

### 客户端使用

```bash
# 编译客户端
go build -o vnet ./vnet

# 启动客户端（连接到服务器，配置虚拟 IP）
./vnet -server ws://127.0.0.1:8080/ws -ip 10.0.0.2/24 -key mysecret

# 可选：配置 IPv6 地址
./vnet -server ws://127.0.0.1:8080/ws -ip 10.0.0.2/24 -ipv6 fd00::2/64 -key mysecret
```

### Android 应用

Android 应用提供图形化配置界面：

1. 配置服务器地址
2. 输入虚拟 IP 地址（IPv4/IPv6）
3. 输入认证密钥
4. 点击"连接"按钮

## 架构说明

### 客户端架构

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│   应用层    │    │   应用层    │    │   应用层    │
├─────────────┤    ├─────────────┤    ├─────────────┤
│    TUN      │◄──►│   WebSocket │◄──►│    服务端   │
│  设备驱动   │    │   客户端    │    │             │
└─────────────┘    └─────────────┘    └─────────────┘
```

### 服务端架构

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│  客户端 A   │◄──►│             │◄──►│  客户端 B   │
└─────────────┘    │   服务端    │    └─────────────┘
                   │  WebSocket  │
┌─────────────┐    │   服务器    │    ┌─────────────┐
│  客户端 C   │◄──►│             │◄──►│  客户端 D   │
└─────────────┘    └─────────────┘    └─────────────┘
```

## 配置参数

### 服务端参数

| 参数 | 说明 | 默认值 |
|------|------|--------|
| `-listen` | 监听地址 | `:8080` |
| `-key` | 认证密钥 | 无（必填） |

### 客户端参数

| 参数 | 说明 | 默认值 |
|------|------|--------|
| `-server` | 服务器地址 | `ws://127.0.0.1:8080/ws` |
| `-ip` | IPv4 地址和子网掩码 | 无（必填） |
| `-ipv6` | IPv6 地址和子网掩码 | 空（可选） |
| `-prefix` | TUN 设备前缀 | `vnet` |
| `-key` | 认证密钥 | 无（必填） |

## 协议细节

### 数据包格式

VNet 使用原始 IP 数据包作为 WebSocket 消息的内容：

- **消息类型**: `BinaryMessage`
- **数据格式**: 原始 IP 数据包（包含 IP 头部）
- **最大传输单元**: 1500 字节

### IP 地址解析

服务端通过解析 IP 数据包头部获取目标地址：

- **IPv4**: 从第 16-19 字节提取目标 IP 地址
- **IPv6**: 从第 24-39 字节提取目标 IP 地址

### 认证机制

客户端在 WebSocket 连接时通过 URL 参数传递认证信息：

```
ws://server:port/ws?key=secret&ip=10.0.0.2
```

## 开发说明

### 构建要求

- Go 1.26.4 或更高版本
- Android Studio（用于构建 Android 应用）
- Git

### 项目结构

```
vnet/
├── vnet/                    # 客户端代码
│   ├── main.go             # 客户端主程序
│   ├── tun_linux.go        # Linux TUN 实现
│   ├── tun_darwin.go       # macOS TUN 实现
│   └── tun_windows.go      # Windows TUN 实现
├── vnet-server/            # 服务端代码
│   └── main.go             # 服务端主程序
├── android/                # Android 应用
│   └── app/src/main/java/com/carryrao/vnet/
│       ├── MainActivity.kt        # 主界面
│       ├── VnetVpnService.kt      # VPN 服务
│       ├── VnetConnection.kt      # WebSocket 连接
│       ├── VnetLogger.kt          # 日志记录
│       └── VnetStats.kt           # 统计信息
├── go.mod                  # Go 模块定义
└── go.sum                  # 依赖校验和
```

### 依赖项

- `github.com/gorilla/websocket`: WebSocket 协议实现
- `golang.zx2c4.com/wireguard/tun`: 跨平台 TUN 设备创建

## 使用场景

1. **远程办公**: 安全访问公司内网资源
2. **物联网设备通信**: 设备间直接通信
3. **游戏加速**: 低延迟网络连接
4. **网络调试**: 测试网络应用在不同网络环境下的表现

## 许可证

Apache License 2.0