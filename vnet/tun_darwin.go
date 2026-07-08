//go:build darwin

package main

import (
	"log"
	"net"
	"os/exec"
	"strings"

	"golang.zx2c4.com/wireguard/tun"
)

func createTUN(name string) (tun.Device, error) {
	return tun.CreateTUNByName(name, 1500)
}

func configureTUN(name string, cidr string, cidr6 string) error {
	ip, ipnet, err := net.ParseCIDR(cidr)
	if err != nil {
		return err
	}
	peer := peerAddr(ip, ipnet)
	mask := net.IP(ipnet.Mask).String()

	cmds := [][]string{
		{"ifconfig", name, "inet", ip.String(), peer.String(), "netmask", mask},
		{"ifconfig", name, "up"},
	}
	for _, args := range cmds {
		cmd := exec.Command(args[0], args[1:]...)
		out, err := cmd.CombinedOutput()
		if err != nil {
			log.Printf("[TUN SETUP] cmd=%v out=%s err=%v", args, strings.TrimSpace(string(out)), err)
			return err
		}
	}
	log.Printf("[TUN READY] dev=%s cidr=%s peer=%s mask=%s", name, cidr, peer, mask)

	if cidr6 != "" {
		if err := configureTUNIPv6(name, cidr6); err != nil {
			return err
		}
	}
	return nil
}

func configureTUNIPv6(name string, cidr6 string) error {
	_, _, err := net.ParseCIDR(cidr6)
	if err != nil {
		return err
	}

	cmds := [][]string{
		{"ifconfig", name, "inet6", cidr6},
		{"ifconfig", name, "up"},
	}
	for _, args := range cmds {
		cmd := exec.Command(args[0], args[1:]...)
		out, err := cmd.CombinedOutput()
		if err != nil {
			log.Printf("[TUN SETUP IPv6] cmd=%v out=%s err=%v", args, strings.TrimSpace(string(out)), err)
			return err
		}
	}
	log.Printf("[TUN READY IPv6] dev=%s cidr6=%s", name, cidr6)
	return nil
}

func peerAddr(ip net.IP, ipnet *net.IPNet) net.IP {
	peer := make(net.IP, len(ip))
	copy(peer, ip)
	peer[len(peer)-1]++
	return peer
}
