package main

import (
	"flag"
	"log"
	"net"
	"net/http"
	"os"
	"sync"

	"github.com/gorilla/websocket"
)

var (
	listenAddr string
	secretKey  string
	upgrader   = websocket.Upgrader{CheckOrigin: func(r *http.Request) bool { return true }}
	clients    sync.Map
)

type clientEntry struct {
	ip  string
	ws  *websocket.Conn
	mu  sync.Mutex
}

func handleWs(w http.ResponseWriter, r *http.Request) {
	ck := r.URL.Query().Get("key")
	remote := r.RemoteAddr
	if ck != secretKey {
		log.Printf("[AUTH FAIL] remote=%s key mismatch", remote)
		w.WriteHeader(http.StatusForbidden)
		return
	}
	clientIP := r.URL.Query().Get("ip")
	if clientIP == "" {
		log.Printf("[BAD REQ] remote=%s empty client ip", remote)
		w.WriteHeader(http.StatusBadRequest)
		return
	}
	conn, err := upgrader.Upgrade(w, r, nil)
	if err != nil {
		log.Printf("[UPGRADE ERR] remote=%s err=%v", remote, err)
		return
	}
	ce := &clientEntry{ip: clientIP, ws: conn}
	clients.Store(clientIP, ce)
	log.Printf("[ONLINE] virtIp=%s remote=%s total=%d", clientIP, remote, countClients())
	defer func() {
		clients.Delete(clientIP)
		conn.Close()
		log.Printf("[OFFLINE] virtIp=%s remote=%s total=%d", clientIP, remote, countClients())
	}()
	readLoop(conn, clientIP)
}

func countClients() int {
	cnt := 0
	clients.Range(func(k, v any) bool {
		cnt++
		return true
	})
	return cnt
}

func readLoop(conn *websocket.Conn, selfIP string) {
	for {
		_, pkt, err := conn.ReadMessage()
		if err != nil {
			log.Printf("[READ LOOP EXIT] virtIp=%s err=%v", selfIP, err)
			return
		}
		dst := parseDstIP(pkt)
		if dst == "" {
			log.Printf("[DROP] virtIp=%s invalid ip packet len=%d", selfIP, len(pkt))
			continue
		}
		if isBroadcastOrMulticast(pkt) {
			fanOut(selfIP, pkt)
		} else {
			unicast(selfIP, dst, pkt)
		}
	}
}

func unicast(srcIP, dstIP string, pkt []byte) {
	val, ok := clients.Load(dstIP)
	if !ok {
		log.Printf("[NO PEER] src=%s dst=%s", srcIP, dstIP)
		return
	}
	peer := val.(*clientEntry)
	peer.mu.Lock()
	err := peer.ws.WriteMessage(websocket.BinaryMessage, pkt)
	peer.mu.Unlock()
	if err != nil {
		log.Printf("[SEND FAIL] dst=%s err=%v", dstIP, err)
		clients.Delete(dstIP)
		peer.ws.Close()
	} else {
		log.Printf("[FORWARD] src=%s dst=%s len=%d", srcIP, dstIP, len(pkt))
	}
}

func fanOut(srcIP string, pkt []byte) {
	sent := 0
	clients.Range(func(key, value any) bool {
		peer := value.(*clientEntry)
		if peer.ip == srcIP {
			return true
		}
		peer.mu.Lock()
		err := peer.ws.WriteMessage(websocket.BinaryMessage, pkt)
		peer.mu.Unlock()
		if err != nil {
			log.Printf("[BROADCAST FAIL] dst=%s err=%v", peer.ip, err)
			clients.Delete(peer.ip)
			peer.ws.Close()
		} else {
			sent++
		}
		return true
	})
	log.Printf("[BROADCAST] src=%s len=%d sent=%d", srcIP, len(pkt), sent)
}

func parseDstIP(pkt []byte) string {
	if len(pkt) < 1 {
		return ""
	}
	ver := pkt[0] >> 4
	switch ver {
	case 4:
		if len(pkt) < 20 {
			return ""
		}
		return net.IPv4(pkt[16], pkt[17], pkt[18], pkt[19]).String()
	case 6:
		if len(pkt) < 40 {
			return ""
		}
		return net.IP(pkt[24:40]).String()
	default:
		return ""
	}
}

func isBroadcastOrMulticast(pkt []byte) bool {
	if len(pkt) < 1 {
		return false
	}
	ver := pkt[0] >> 4
	switch ver {
	case 4:
		if len(pkt) < 20 {
			return false
		}
		if pkt[16] == 0xFF {
			return true
		}
		if pkt[16] >= 0xE0 && pkt[16] <= 0xEF {
			return true
		}
		return false
	case 6:
		if len(pkt) < 40 {
			return false
		}
		return pkt[24] == 0xFF
	default:
		return false
	}
}

func main() {
	flag.StringVar(&listenAddr, "listen", ":8080", "listen addr")
	flag.StringVar(&secretKey, "key", "", "auth secret")
	flag.Parse()
	if secretKey == "" {
		secretKey = os.Getenv("key")
		if secretKey == "" {
			log.Fatal("-key required")
		}
	}
	log.Printf("[START] listen=%s", listenAddr)
	http.HandleFunc("/ws", handleWs)
	log.Fatal(http.ListenAndServe(listenAddr, nil))
}
