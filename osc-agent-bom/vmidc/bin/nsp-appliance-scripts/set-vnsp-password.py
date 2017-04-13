#!/usr/bin/python
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
child.sendline("passwd\r")

child.expect(".*:")
child.sendline(sys.argv[1] + "\r")

#If we've encountered a password error
index = child.expect( [ ".*:", ".*>"] )
if index == 1:
    sys.exit(-2)

child.sendline(sys.argv[2] + "\r")

child.expect(".*:")
child.sendline(sys.argv[2] + "\r")

child.close()
