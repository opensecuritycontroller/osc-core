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
