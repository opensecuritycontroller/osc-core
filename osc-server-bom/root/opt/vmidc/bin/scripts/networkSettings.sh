#!/bin/sh

#Log current settings
ip -o addr > log/networkSettings.log

#Remove all network settings
ip addr flush dev eth0 >> log/networkSettings.log

if [ $1 = dhcp ]; then

	sed -i -e '/BOOTPROTO/d' /etc/sysconfig/network-scripts/ifcfg-eth0
	sed -i -e "$ a\\BOOTPROTO=dhcp" /etc/sysconfig/network-scripts/ifcfg-eth0
	sed -i -e '/GATEWAY/d' /etc/sysconfig/network
else

	sed -i -e '/IPADDR/d' -e '/NETMASK/d' -e '/BOOTPROTO/d' /etc/sysconfig/network-scripts/ifcfg-eth0
	sed -i -e '/GATEWAY/d' /etc/sysconfig/network
	sed -i -e "$ a\\IPADDR=$2\\
NETMASK=$3\\
BOOTPROTO=static" /etc/sysconfig/network-scripts/ifcfg-eth0
	sed -i -e "$ a\\GATEWAY=$4" /etc/sysconfig/network
	rm -rf /etc/resolv.conf
	echo "nameserver $5
nameserver $6" > /etc/resolv.conf
fi

#Restart network
/etc/init.d/network restart >> log/networkSettings.log 2>&1

#Log settings after change
ip -o addr >> log/networkSettings.log