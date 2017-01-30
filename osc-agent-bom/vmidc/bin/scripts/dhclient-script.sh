#!/bin/sh
case "$reason" in

	BOUND|RENEW|REBIND|REBOOT)
		rm -f ipconfig.conf
		echo management.ip0=$new_ip_address >> ipconfig.conf
		echo management.netmask0=$new_subnet_mask >> ipconfig.conf
		echo management.mtu=$new_interface_mtu >> ipconfig.conf
		for router in $new_routers; do	
			echo management.gateway=$router >> ipconfig.conf
			break
		done
	;;
esac
