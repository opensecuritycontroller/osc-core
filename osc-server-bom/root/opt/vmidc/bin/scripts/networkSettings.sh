#!/bin/sh
# Copyright (c) 2017 Intel Corporation
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