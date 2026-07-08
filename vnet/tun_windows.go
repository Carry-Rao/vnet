//go:build windows

package main

import (
	"fmt"
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
	ip, ipnet, err := net.ParseCIDR(cidr)
	if err != nil {
		return err
	}
	mask := net.IP(ipnet.Mask).String()

	args := []string{
		"interface", "ip", "set", "address", name,
		"static", ip.String(), mask,
	}
	cmd := exec.Command("netsh", args...)
	out, err := cmd.CombinedOutput()
	if err != nil {
		log.Printf("[TUN SETUP] cmd=netsh out=%s err=%v", strings.TrimSpace(string(out)), err)
		return err
	}
	log.Printf("[TUN READY] dev=%s cidr=%s mask=%s", name, cidr, mask)

	if cidr6 != "" {
		if err := configureTUNIPv6(name, cidr6); err != nil {
			return err
		}
	}
	return nil
}

func configureTUNIPv6(name string, cidr6 string) error {
	ip6, ipnet6, err := net.ParseCIDR(cidr6)
	if err != nil {
		return err
	}

	args := []string{
		"interface", "ipv6", "set", "address", name,
		"unicast", fmt.Sprintf("%s/%d", ip6.String(), ipnet6.Mask),
	}
	cmd := exec.Command("netsh", args...)
	out, err := cmd.CombinedOutput()
	if err != nil {
		log.Printf("[TUN SETUP IPv6] cmd=netsh out=%s err=%v", strings.TrimSpace(string(out)), err)
		return err
	}
	log.Printf("[TUN READY IPv6] dev=%s cidr6=%s", name, cidr6)
	return nil
}
