#!/bin/sh
# Copyright (c) Intel Corporation
# Copyright (c) 2017
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

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
