package main

import (
	"flag"
	"log"
	"net/url"
	"strings"
	"sync"
	"time"

	"github.com/gorilla/websocket"
	"golang.zx2c4.com/wireguard/tun"
)

var (
	devPrefix  string
	serverAddr string
	localCIDR  string
	localCIDR6 string
	secretKey  string
	tunDev     tun.Device
	wsConn     *websocket.Conn
	wsMu       sync.Mutex
	once       sync.Once
)

func dial() {
	for {
		u, _ := url.Parse(serverAddr)
		q := u.Query()
		localIP := strings.Split(localCIDR, "/")[0]
		q.Set("ip", localIP)
		q.Set("key", secretKey)
		u.RawQuery = q.Encode()
		log.Printf("[DIAL] server=%s", serverAddr)
		conn, _, err := websocket.DefaultDialer.Dial(u.String(), nil)
		if err != nil {
			log.Printf("[DIAL FAIL] err=%v sleep 2s", err)
			time.Sleep(2 * time.Second)
			continue
		}
		wsMu.Lock()
		wsConn = conn
		wsMu.Unlock()
		log.Printf("[CONNECTED] virtIp=%s", localIP)
		once.Do(func() { go tunWriteLoop() })
		readWsLoop(conn)
		wsMu.Lock()
		wsConn = nil
		wsMu.Unlock()
		conn.Close()
		log.Printf("[DISCONNECT] retry 1s")
		time.Sleep(1 * time.Second)
	}
}

func readWsLoop(conn *websocket.Conn) {
	for {
		_, pkt, err := conn.ReadMessage()
		if err != nil {
			log.Printf("[WS READ EXIT] err=%v", err)
			return
		}
		buf := make([]byte, len(pkt)+12)
		copy(buf[12:], pkt)
		_, err = tunDev.Write([][]byte{buf}, 12)
		if err != nil {
			log.Printf("[TUN WRITE ERR] len=%d err=%v", len(pkt), err)
		} else {
			log.Printf("[WS->TUN] len=%d", len(pkt))
		}
	}
}

func tunWriteLoop() {
	log.Printf("[TUN WRITE LOOP START]")
	for {
		bufs := make([][]byte, 1)
		bufs[0] = make([]byte, 1500)
		sizes := make([]int, 1)
		n, err := tunDev.Read(bufs, sizes, 0)
		if err != nil {
			time.Sleep(10 * time.Millisecond)
			continue
		}
		wsMu.Lock()
		conn := wsConn
		wsMu.Unlock()
		if conn == nil {
			log.Printf("[TUN->WS SKIP] ws offline")
			continue
		}
		for i := 0; i < n; i++ {
			pkt := bufs[i][:sizes[i]]
			err = conn.WriteMessage(websocket.BinaryMessage, pkt)
			if err != nil {
				log.Printf("[TUN->WS SEND ERR] len=%d err=%v", sizes[i], err)
				wsMu.Lock()
				wsConn = nil
				wsMu.Unlock()
				return
			}
			log.Printf("[TUN->WS] len=%d", sizes[i])
		}
	}
}

func main() {
	flag.StringVar(&devPrefix, "prefix", "vnet", "tun prefix")
	flag.StringVar(&serverAddr, "server", "ws://127.0.0.1:8080/ws", "ws server")
	flag.StringVar(&localCIDR, "ip", "10.0.0.2/24", "client cidr (e.g. 10.0.0.2/24)")
	flag.StringVar(&localCIDR6, "ipv6", "", "optional IPv6 cidr (e.g. fd00::2/64)")
	flag.StringVar(&secretKey, "key", "", "auth secret")
	flag.Parse()
	if secretKey == "" {
		log.Fatal("-key required")
	}
	if localCIDR == "" {
		log.Fatal("-ip required")
	}

	devName := devPrefix + "0"
	var err error
	tunDev, err = createTUN(devName)
	if err != nil {
		log.Fatalf("[TUN CREATE FAIL] dev=%s err=%v", devName, err)
	}
	log.Printf("[TUN CREATED] dev=%s", devName)

	if err := configureTUN(devName, localCIDR, localCIDR6); err != nil {
		log.Fatalf("[TUN CONFIG FAIL] err=%v", err)
	}

	localIP := strings.Split(localCIDR, "/")[0]
	log.Printf("[CLIENT START] virtIp=%s", localIP)
	dial()
}
