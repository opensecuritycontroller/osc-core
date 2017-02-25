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

import pexpect
import sys

child = pexpect.spawn("/usr/local/bin/idscli\r", timeout=60);
child.timeout = 60
child.logfile = sys.stdout

# Skip first time factory default prompt
index = child.expect( [ ".*>", ".*later:"] )
if index == 1:
    child.sendline("N\r")
    print("Wizard prompt identified")
    child.expect(".*>")

child.sendline("debug\r")

child.expect(".*>")
child.sendline("set sensor ip " + sys.argv[1] + " " + sys.argv[2] + "\r")

child.expect(".*>")
child.sendline("set sensor gateway " + sys.argv[3] + "\r")

n = int (sys.argv[4])
if n > 0:
    child.expect(".*>")
    child.sendline("set mgmtport mtu " + sys.argv[4] + "\r")

child.expect(".*>")
child.sendline("show\r")

child.expect(".*>")
child.close()
