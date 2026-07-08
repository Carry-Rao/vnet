//go:build linux

package main

import (
	"log"
	"net"
	"os/exec"
	"strings"

	"golang.zx2c4.com/wireguard/tun"
)

func createTUN(name string) (tun.Device, error) {
	return tun.CreateTUN(name, 1500)
}

func configureTUN(name string, cidr string, cidr6 string) error {
	_, ipnet, err := net.ParseCIDR(cidr)
	if err != nil {
		return err
	}
	broadcast := broadcastAddr(ipnet).String()

	cmds := [][]string{
		{"ip", "addr", "add", cidr, "dev", name},
		{"ip", "link", "set", name, "up"},
		{"ip", "route", "add", broadcast, "dev", name},
	}
	for _, args := range cmds {
		cmd := exec.Command(args[0], args[1:]...)
		out, err := cmd.CombinedOutput()
		if err != nil {
			log.Printf("[TUN SETUP] cmd=%v out=%s err=%v", args, strings.TrimSpace(string(out)), err)
			return err
		}
	}
	log.Printf("[TUN READY] dev=%s cidr=%s broadcast=%s", name, cidr, broadcast)

	if cidr6 != "" {
		if err := configureTUNIPv6(name, cidr6); err != nil {
			return err
		}
	}
	return nil
}

func configureTUNIPv6(name string, cidr6 string) error {
	_, ipnet6, err := net.ParseCIDR(cidr6)
	if err != nil {
		return err
	}
	multicast := multicastAddr6(ipnet6).String()

	cmds := [][]string{
		{"ip", "-6", "addr", "add", cidr6, "dev", name},
		{"ip", "-6", "route", "add", multicast, "dev", name},
	}
	for _, args := range cmds {
		cmd := exec.Command(args[0], args[1:]...)
		out, err := cmd.CombinedOutput()
		if err != nil {
			log.Printf("[TUN SETUP IPv6] cmd=%v out=%s err=%v", args, strings.TrimSpace(string(out)), err)
			return err
		}
	}
	log.Printf("[TUN READY IPv6] dev=%s cidr6=%s multicast=%s", name, cidr6, multicast)
	return nil
}

func broadcastAddr(n *net.IPNet) net.IP {
	ip := make(net.IP, len(n.IP))
	for i := range ip {
		ip[i] = n.IP[i] | ^n.Mask[i]
	}
	return ip
}

func multicastAddr6(n *net.IPNet) net.IP {
	ip := make(net.IP, len(n.IP))
	copy(ip, n.IP)
	ip[0] = 0xFF
	for i := 1; i < len(ip); i++ {
		ip[i] = 0
	}
	return ip
}
