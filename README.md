We provide [Chinese](https://github.com/Carry-Rao/vnet/blob/master/docs/README_zh.md)

# VNet — Lightweight Virtual Network Tool

This project is a lightweight virtual network solution based on WebSocket, supporting cross-platform TUN device creation and IPv4/IPv6 dual-stack communication. Core design concepts:

- Implement virtual network tunnel using WebSocket protocol, simplifying NAT traversal and firewall traversal
- Support multi-platform (Linux/macOS/Windows/Android) TUN device creation and configuration
- Provide unified client and server interfaces, supporting IPv4/IPv6 dual-stack communication
- Built-in broadcast/multicast support for device discovery and communication within LAN

+ **Note**: This is an experimental project, not fully tested, not recommended for production use.

## Core Features

### Cross-platform TUN Support

VNet supports creating and configuring TUN virtual network devices on multiple operating systems:

- **Linux**: Configure network interface using `ip` command
- **macOS**: Configure network interface using `ifconfig` command
- **Windows**: Configure network interface using `netsh` command
- **Android**: Create TUN device through Android VpnService API

### IPv4/IPv6 Dual Stack

VNet fully supports IPv4 and IPv6 dual-stack communication:

- Client can configure both IPv4 and IPv6 addresses simultaneously
- Server supports IPv4/IPv6 packet parsing and forwarding
- Automatic IPv6 multicast address configuration

### WebSocket Tunnel

Using WebSocket protocol as transport layer, with the following advantages:

- Traverse NAT and firewalls
- Support TLS/SSL encrypted transmission
- Automatic reconnection mechanism
- Cross-platform compatibility

### Broadcast/Multicast Support

Server supports fan-out forwarding of broadcast and multicast packets:

- Automatic identification of broadcast/multicast target addresses
- Support IPv4 broadcast address (255.255.255.255) and multicast address range (224.0.0.0 - 239.255.255.255)
- Support IPv6 multicast address (ff00::/8)

## Quick Start

### Server Deployment

```bash
# Build server
go build -o vnet-server ./vnet-server

# Start server (listening on port 8080, using key mysecret)
./vnet-server -listen :8080 -key mysecret
```

### Client Usage

```bash
# Build client
go build -o vnet ./vnet

# Start client (connect to server, configure virtual IP)
./vnet -server ws://127.0.0.1:8080/ws -ip 10.0.0.2/24 -key mysecret

# Optional: Configure IPv6 address
./vnet -server ws://127.0.0.1:8080/ws -ip 10.0.0.2/24 -ipv6 fd00::2/64 -key mysecret
```

### Android Application

Android application provides graphical configuration interface:

1. Configure server address
2. Enter virtual IP address (IPv4/IPv6)
3. Enter authentication key
4. Click "Connect" button

## Architecture

### Client Architecture

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│  Application│    │  Application│    │  Application│
├─────────────┤    ├─────────────┤    ├─────────────┤
│    TUN      │◄──►│   WebSocket │◄──►│    Server   │
│   Driver    │    │   Client    │    │             │
└─────────────┘    └─────────────┘    └─────────────┘
```

### Server Architecture

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│  Client A   │◄──►│             │◄──►│  Client B   │
└─────────────┘    │   Server    │    └─────────────┘
                   │  WebSocket  │
┌─────────────┐    │   Server    │    ┌─────────────┐
│  Client C   │◄──►│             │◄──►│  Client D   │
└─────────────┘    └─────────────┘    └─────────────┘
```

## Configuration Parameters

### Server Parameters

| Parameter | Description | Default |
|-----------|-------------|---------|
| `-listen` | Listen address | `:8080` |
| `-key` | Authentication key | None (required) |

### Client Parameters

| Parameter | Description | Default |
|-----------|-------------|---------|
| `-server` | Server address | `ws://127.0.0.1:8080/ws` |
| `-ip` | IPv4 address and subnet mask | None (required) |
| `-ipv6` | IPv6 address and subnet mask | Empty (optional) |
| `-prefix` | TUN device prefix | `vnet` |
| `-key` | Authentication key | None (required) |

## Protocol Details

### Packet Format

VNet uses raw IP packets as WebSocket message content:

- **Message Type**: `BinaryMessage`
- **Data Format**: Raw IP packet (including IP header)
- **Maximum Transmission Unit**: 1500 bytes

### IP Address Parsing

Server extracts target address by parsing IP packet header:

- **IPv4**: Extract destination IP address from bytes 16-19
- **IPv6**: Extract destination IP address from bytes 24-39

### Authentication Mechanism

Client passes authentication information through URL parameters during WebSocket connection:

```
ws://server:port/ws?key=secret&ip=10.0.0.2
```

## Development Notes

### Build Requirements

- Go 1.26.4 or higher
- Android Studio (for building Android application)
- Git

### Project Structure

```
vnet/
├── vnet/                    # Client code
│   ├── main.go             # Client main program
│   ├── tun_linux.go        # Linux TUN implementation
│   ├── tun_darwin.go       # macOS TUN implementation
│   └── tun_windows.go      # Windows TUN implementation
├── vnet-server/            # Server code
│   └── main.go             # Server main program
├── android/                # Android application
│   └── app/src/main/java/com/carryrao/vnet/
│       ├── MainActivity.kt        # Main interface
│       ├── VnetVpnService.kt      # VPN service
│       ├── VnetConnection.kt      # WebSocket connection
│       ├── VnetLogger.kt          # Log recording
│       └── VnetStats.kt           # Statistics information
├── go.mod                  # Go module definition
└── go.sum                  # Dependency checksums
```

### Dependencies

- `github.com/gorilla/websocket`: WebSocket protocol implementation
- `golang.zx2c4.com/wireguard/tun`: Cross-platform TUN device creation

## Use Cases

1. **Remote Work**: Securely access company intranet resources
2. **IoT Device Communication**: Direct communication between devices
3. **Game Acceleration**: Low-latency network connection
4. **Network Debugging**: Test network applications in different network environments

## License

Apache License 2.0