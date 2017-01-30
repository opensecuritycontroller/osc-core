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
