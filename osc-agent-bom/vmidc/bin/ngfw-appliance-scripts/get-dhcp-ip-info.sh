#!/bin/sh
cleanup() {
    cat /spool/cpa/bin/dhclient.pid | xargs kill
    exit
}
trap cleanup INT TERM

/bin/ip link set dev eth0 up

touch /spool/cpa/bin/dhclient.lease
> /spool/cpa/bin/dhclient.lease
/sbin/dhclient -1 -4 -v -lf /spool/cpa/bin/dhclient.lease -pf /spool/cpa/bin/dhclient.pid -sf /spool/cpa/bin/scripts/dhclient-script.sh eth0

cleanup
