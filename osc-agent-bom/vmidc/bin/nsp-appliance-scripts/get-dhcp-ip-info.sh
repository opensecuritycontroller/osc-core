#!/bin/sh
cleanup() {
    iptables -F configuration
	cat /tftpboot/vmidc/bin/dhclient.pid | xargs kill
    exit
}
trap cleanup INT TERM

/bin/ip link set dev eth0 up

#Assmue this gets run as part of sensor boot
#iptables -N configuration
#iptables -I INPUT -j configuration

/usr/local/sbin/iptables -F configuration
/usr/local/sbin/iptables -I configuration -i eth0 -p udp -m udp --dport 67 -j ACCEPT

touch /tftpboot/vmidc/bin/dhclient.lease
> /tftpboot/vmidc/bin/dhclient.lease
/usr/local/sbin/dhclient -1 -4 -v -lf /tftpboot/vmidc/bin/dhclient.lease -pf /tftpboot/vmidc/bin/dhclient.pid -sf /tftpboot/vmidc/bin/scripts/dhclient-script.sh eth0

cleanup
